# Tài liệu Quy trình & Kiến trúc Xác thực Zalo OAuth PKCE

Tài liệu này mô tả chi tiết quy trình đăng nhập và đồng bộ tài khoản mạng xã hội qua Zalo OAuth v4 sử dụng chuẩn **PKCE (Proof Key for Code Exchange)** trên nền tảng Computer Store (Angular 18+ & Spring Boot 3.x).

---

## 1. Tổng quan Kiến trúc

```
+-------------------+             +-----------------------+             +----------------------+
|  Angular Frontend |             |  Zalo OAuth v4 Server |             |  Spring Boot Backend |
+-------------------+             +-----------------------+             +----------------------+
          |                                   |                                    |
          | 1. Tạo PKCE code_verifier         |                                    |
          |    & code_challenge (SHA-256)     |                                    |
          |    Tạo CSRF state                 |                                    |
          |---------------------------------->|                                    |
          | Mở popup tới Zalo OAuth           |                                    |
          |                                   |                                    |
          | 2. Khách hàng xác thực quyền      |                                    |
          |    Zalo chuyển hướng về:          |                                    |
          |    /auth/zalo/callback?code=...   |                                    |
          |<----------------------------------|                                    |
          |                                                                        |
          | 3. Route Callback kiểm tra state,                                      |
          |    gửi postMessage(code, state)                                        |
          |    về cửa sổ chính (window.opener)                                     |
          |                                                                        |
          | 4. Cửa sổ chính kiểm tra:                                              |
          |    - event.origin === window.location.origin                           |
          |    - event.source === popup                                            |
          |    - event.data.state === savedState                                   |
          |    Dọn dẹp popup, interval, listener, storage                          |
          |                                                                        |
          | 5. POST /api/v1/auth/social/zalo                                       |
          |    Body: { code, codeVerifier }                                        |
          |----------------------------------------------------------------------->|
          |                                                                        | 6. Backend đổi code
          |                                                                        |    + codeVerifier với
          |                                                                        |    Zalo Server lấy
          |                                                                        |    access_token & user info
          |                                                                        |    (offline verification)
          |                                                                        |
          |                                                                        | 7. Đối chiếu tài khoản,
          |                                                                        |    bảo vệ tài khoản thật,
          |                                                                        |    phát hiện squatter/hijack
          |                                                                        |
          | 8. Trả về AuthResponse { accessToken, refreshToken, user }             |
          |<-----------------------------------------------------------------------|
          | Lưu token, cập nhật Signal state, chuyển hướng Dashboard/Trang chủ     |
```

---

## 2. Các Biện pháp Bảo mật & Code Hardening

### 2.1. Phía Frontend (Angular)
1. **Single-Popup Lock**:
   - Sử dụng cờ `zaloLoginInProgress` bọc trong khối `try ... finally`.
   - Chặn hoàn toàn việc click đúp hoặc click liên tục mở nhiều popup cùng lúc.
2. **Xác thực Nguồn Tin nhắn (`postMessage`) Hai Lớp**:
   - Xác thực nguồn gốc origin: `event.origin === window.location.origin`.
   - Xác thực cửa sổ gửi: `event.source === popup`.
3. **Phòng chống CSRF State**:
   - Sinh chuỗi ngẫu nhiên 32 ký tự hex lưu trong `sessionStorage`.
   - Đối chiếu state trả về từ callback với state đã lưu; xóa state ngay sau khi đối chiếu.
4. **Dọn dẹp Tài nguyên Triệt để (Cleanup)**:
   - Dù đăng nhập thành công, thất bại, gặp lỗi CSRF hay người dùng chủ động đóng popup:
     - Gỡ bỏ `window.removeEventListener('message', messageHandler)`.
     - Hủy `clearInterval(checkClosedInterval)`.
     - Xóa `sessionStorage.removeItem('zalo_oauth_state')`.
     - Tự động đóng popup (`popup.close()`).
     - Reset cờ `zaloLoginInProgress` về `false`.

### 2.2. Phía Backend (Spring Boot)
1. **Chống IP Spoofing & Header Injection**:
   - Sử dụng `ClientIpResolver` dựa trên Tomcat `RemoteIpValve`.
   - Chỉ chấp nhận IP từ proxy nội bộ đáng tin cậy đã cấu hình trong `server.tomcat.remoteip.*`.
   - Không tự ý phân tích chuỗi header `X-Forwarded-For` hoặc `X-Real-IP`.
2. **Bảo vệ Phân loại Tài khoản 3 Lớp (Pre-Hijacking Defense)**:
   - Nếu email đã tồn tại và là khách hàng thực sự (đã từng đặt hàng hoặc có lịch sử đăng nhập trước đó): Bảo tồn mật khẩu hiện tại, chỉ liên kết `provider_id`.
   - Nếu tài khoản có dấu hiệu bị chiếm trước (chưa từng mua hàng, chưa kích hoạt email): Đặt lại mật khẩu ngẫu nhiên và hủy toàn bộ phiên làm việc cũ.
3. **Giới hạn Tần suất (Rate Limiting)**:
   - `LoginRateLimiter` áp dụng giới hạn theo địa chỉ IP giải quyết từ `ClientIpResolver`.

---

## 3. Cấu hình Môi trường (.env & application.yml)

### Frontend
- Không lưu `app_secret` ở frontend.
- Cấu hình API URL trong `src/environments/environment.ts` hoặc tự động nạp từ `GET /api/v1/auth/oauth2/config`.

### Backend (`application.yml`)
```yaml
app:
  oauth2:
    zalo:
      app-id: ${ZALO_APP_ID:}
      secret-key: ${ZALO_SECRET_KEY:}
```

### Zalo Developer Console
- Thêm URL Callback hợp lệ: `https://your-domain.com/auth/zalo/callback` (và `http://localhost:4200/auth/zalo/callback` cho môi trường dev).
- Cấp quyền truy cập: `get_user_profile` và `get_user_id`.

---

## 4. Kịch bản Kiểm thử

| STT | Kịch bản | Kỳ vọng |
|:---|:---|:---|
| 1 | Click nút Zalo đăng nhập 2 lần liên tục | Chỉ mở đúng 1 popup; lần click thứ 2 báo lỗi chờ hoàn tất |
| 2 | Người dùng đóng popup trước khi đăng nhập | Promise reject `'Cửa sổ đăng nhập Zalo đã bị đóng'`, dọn dẹp sạch listener và storage |
| 3 | Nhận postMessage từ origin khác hoặc iframe khác | Bỏ qua, không xử lý |
| 4 | State trả về không khớp state ban đầu | Báo lỗi CSRF, từ chối cấp token |
| 5 | Đăng nhập hợp lệ | Trả về Authorization Code & PKCE Verifier, backend trả JWT thành công |
