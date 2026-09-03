# Hướng dẫn cấu hình GitHub Actions Secrets

Để quy trình CI/CD ([.github/workflows/backend-ci.yml](file:///.github/workflows/backend-ci.yml)) chạy bảo mật và không bị lộ bất kỳ thông tin nhạy cảm nào trong mã nguồn Git, các credentials đã được chuyển sang sử dụng **GitHub Actions Secrets**.

---

## 1. Danh sách Secrets cần cấu hình

Vào GitHub repository: **Settings** $\rightarrow$ **Secrets and variables** $\rightarrow$ **Actions** $\rightarrow$ **New repository secret**.

| Tên Secret | Bắt buộc | Giá trị mẫu / Mô tả |
|:---|:---:|:---|
| `JWT_SECRET` | **Có** | Chuỗi bí mật HMAC-SHA256 (tối thiểu 32 ký tự / 256 bits), ví dụ: `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` |
| `DB_USERNAME` | Tùy chọn (Mặc định: `root`) | Tài khoản MySQL cho runner CI |
| `DB_PASSWORD` | Tùy chọn (Mặc định: `root`) | Mật khẩu MySQL cho runner CI |
| `MAIL_HOST` | Tùy chọn (Mặc định: `smtp.gmail.com`) | Địa chỉ SMTP server gửi mail |
| `MAIL_PORT` | Tùy chọn (Mặc định: `587`) | Cổng SMTP server |
| `MAIL_USERNAME` | Tùy chọn | Email tài khoản SMTP dùng trong test/production |
| `MAIL_PASSWORD` | Tùy chọn | App password / token của email SMTP |
| `ZALO_APP_ID` | Tùy chọn | App ID của ứng dụng Zalo Developer |
| `ZALO_SECRET_KEY` | Tùy chọn | Secret Key của ứng dụng Zalo Developer |

---

## 2. Thiết lập nhanh qua GitHub CLI (gh)

Nếu bạn sử dụng `gh` CLI trên máy cá nhân, bạn có thể thiết lập nhanh bằng các lệnh:

```bash
# JWT Secret (Bắt buộc cho JWT Token Provider)
gh secret set JWT_SECRET -b "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"

# Mail SMTP Credentials (nếu có)
gh secret set MAIL_HOST -b "smtp.gmail.com"
gh secret set MAIL_PORT -b "587"
gh secret set MAIL_USERNAME -b "your-email@gmail.com"
gh secret set MAIL_PASSWORD -b "your-app-password"

# Zalo OAuth Credentials (nếu có)
gh secret set ZALO_APP_ID -b "your-zalo-app-id"
gh secret set ZALO_SECRET_KEY -b "your-zalo-secret-key"

# Database (nếu muốn override giá trị mặc định của CI container)
gh secret set DB_USERNAME -b "root"
gh secret set DB_PASSWORD -b "root"
```

---

## 3. Cách CI Workflow tiêu thụ Secrets

Trong file [.github/workflows/backend-ci.yml](file:///.github/workflows/backend-ci.yml):

```yaml
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: ${{ secrets.DB_PASSWORD || 'root' }}
          MYSQL_DATABASE: computer_store_db

...

        env:
          DB_HOST: 127.0.0.1
          DB_PORT: 3306
          DB_USERNAME: ${{ secrets.DB_USERNAME || 'root' }}
          DB_PASSWORD: ${{ secrets.DB_PASSWORD || 'root' }}
          SPRING_DATASOURCE_URL: jdbc:mysql://127.0.0.1:3306/computer_store_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
          SPRING_DATASOURCE_USERNAME: ${{ secrets.DB_USERNAME || 'root' }}
          SPRING_DATASOURCE_PASSWORD: ${{ secrets.DB_PASSWORD || 'root' }}
          SPRING_DATA_REDIS_HOST: 127.0.0.1
          SPRING_DATA_REDIS_PORT: 6379
          JWT_SECRET: ${{ secrets.JWT_SECRET }}
          MAIL_HOST: ${{ secrets.MAIL_HOST }}
          MAIL_PORT: ${{ secrets.MAIL_PORT }}
          MAIL_USERNAME: ${{ secrets.MAIL_USERNAME }}
          MAIL_PASSWORD: ${{ secrets.MAIL_PASSWORD }}
          ZALO_APP_ID: ${{ secrets.ZALO_APP_ID }}
          ZALO_SECRET_KEY: ${{ secrets.ZALO_SECRET_KEY }}
```

- Không còn bất kỳ plaintext secret, token hay password nào được lưu trong Git.
- Khi workflow chạy, GitHub Actions tự động mask (che dấu `***`) tất cả các giá trị lấy từ secrets trong log console.
