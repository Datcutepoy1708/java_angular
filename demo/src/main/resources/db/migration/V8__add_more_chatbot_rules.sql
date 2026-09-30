-- =============================================================
-- V8: Add more default chatbot rules for PC building, promotions,
-- showrooms, order status tracking, and 0% installments.
-- =============================================================

INSERT INTO `chat_bot_rules` (`rule_name`, `keywords`, `match_type`, `response_message`, `quick_replies`, `action_type`, `priority`, `is_active`)
VALUES
  (
    'Tư vấn cấu hình PC',
    'build pc,cấu hình,cau hinh,tư vấn pc,tu van pc,case,lắp máy,lap may,pc gaming,pc đồ họa,pc văn phòng,pc van phong',
    'CONTAINS',
    'Complexus hỗ trợ tư vấn và lắp ráp PC theo yêu cầu và ngân sách của bạn (Gaming, Đồ họa 3D, AI, Văn phòng). Linh kiện chính hãng 100%, bảo hành 12-36 tháng theo từng linh kiện, miễn phí công lắp đặt và cài đặt! Bạn muốn tư vấn cấu hình ở mức ngân sách khoảng bao nhiêu triệu?',
    '["PC Gaming 15 - 20 triệu", "PC Gaming 20 - 30 triệu", "PC Đồ họa - Render", "Gặp kỹ thuật viên build PC"]',
    'REPLY',
    88,
    1
  ),
  (
    'Khuyến mãi & Giảm giá',
    'khuyến mãi,khuyen mai,mã giảm giá,ma giam gia,voucher,coupon,ưu đãi,uu dai,giảm giá,giam gia,sale',
    'CONTAINS',
    'Complexus đang áp dụng nhiều ưu đãi: Giảm trực tiếp cho thành viên mới, Voucher giảm 5% - 10% cho combo linh kiện, và miễn phí vận chuyển cho đơn hàng từ 500.000đ. Bạn có thể kiểm tra danh sách mã giảm giá tại trang Giỏ hàng hoặc hỏi nhân viên nhé!',
    '["Xem mã giảm giá", "Kiểm tra ưu đãi", "Gặp nhân viên tư vấn"]',
    'REPLY',
    82,
    1
  ),
  (
    'Địa chỉ & Giờ mở cửa',
    'địa chỉ,dia chi,ở đâu,o dau,cửa hàng,cua hang,showroom,giờ mở cửa,gio mo cua,hotline,số điện thoại,so dien thoai,sdt,chi nhánh',
    'CONTAINS',
    'Hệ thống Showroom Complexus mở cửa phục vụ từ 08:00 - 21:30 hàng ngày (kể cả Thứ 7, Chủ Nhật và ngày Lễ). Hotline tư vấn và hỗ trợ kỹ thuật: 1900 xxxx (08:00 - 21:00). Bạn cần tìm địa chỉ Showroom gần bạn nhất hay cần hướng dẫn đường đi?',
    '["Xem danh sách Showroom", "Hotline tư vấn", "Gặp nhân viên"]',
    'REPLY',
    78,
    1
  ),
  (
    'Tra cứu đơn hàng',
    'đơn hàng,don hang,tình trạng đơn,tinh trang don,tra cứu đơn,tra cuu don,đơn của tôi,vận đơn,tracking',
    'CONTAINS',
    'Để tra cứu tình trạng đơn hàng, bạn có thể bấm vào mục "Tra cứu đơn hàng" trên menu và nhập mã đơn hàng hoặc số điện thoại đặt mua. Bạn cũng có thể nhắn mã đơn hàng vào đây để được kiểm tra trực tiếp!',
    '["Tra cứu đơn hàng", "Gặp nhân viên hỗ trợ"]',
    'REPLY',
    84,
    1
  ),
  (
    'Trả góp 0% lãi suất',
    'trả góp,tra gop,thu tuc tra gop,lãi suất,lai suat,hồ sơ trả góp,thẻ tín dụng,the tin dung',
    'CONTAINS',
    'Complexus hỗ trợ trả góp 0% lãi suất qua thẻ tín dụng của hơn 25 ngân hàng liên kết (kỳ hạn 3, 6, 9, 12 tháng). Ngoài ra có hỗ trợ trả góp qua công ty tài chính (thủ tục đơn giản chỉ cần CCCD gắn chip, duyệt nhanh 15 phút). Bạn muốn đăng ký trả góp theo hình thức nào?',
    '["Trả góp qua thẻ tín dụng", "Trả góp qua CCCD", "Gặp nhân viên tư vấn"]',
    'REPLY',
    86,
    1
  )
AS new_vals
ON DUPLICATE KEY UPDATE
  `keywords` = new_vals.keywords,
  `response_message` = new_vals.response_message,
  `quick_replies` = new_vals.quick_replies,
  `action_type` = new_vals.action_type,
  `priority` = new_vals.priority,
  `is_active` = new_vals.is_active;
