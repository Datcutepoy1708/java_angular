-- =============================================================
-- V2__reference_data.sql
-- Computer & PC Components E-commerce Platform - Reference Data
-- Natural key inserts with dynamic role_permissions mapping.
-- ZERO foreign-key disabling, ZERO hardcoded IDs.
-- =============================================================

-- 1. Roles (Natural Key: role_name)
INSERT INTO `roles` (`role_name`, `description`)
VALUES
  ('ROLE_ADMIN', 'Quản trị viên toàn quyền hệ thống'),
  ('ROLE_STAFF', 'Nhân viên quản lý cửa hàng và xử lý đơn hàng'),
  ('ROLE_CUSTOMER', 'Khách hàng mua sắm')
AS new_vals
ON DUPLICATE KEY UPDATE `description` = new_vals.description;

-- 2. Permissions (Natural Key: permission_code)
INSERT INTO `permissions` (`permission_code`, `description`)
VALUES
  ('ROLE_ADMIN', 'Quản trị viên - toàn quyền hệ thống'),
  ('ROLE_STAFF', 'Nhân viên - quản lý sản phẩm, đơn hàng, kho'),
  ('ROLE_CUSTOMER', 'Khách hàng - mua sắm trên website'),
  ('PRODUCT_VIEW', 'Xem danh sách và chi tiết sản phẩm'),
  ('PRODUCT_CREATE', 'Thêm mới sản phẩm'),
  ('PRODUCT_UPDATE', 'Cập nhật sản phẩm'),
  ('PRODUCT_DELETE', 'Xóa sản phẩm'),
  ('CATEGORY_VIEW', 'Xem danh mục sản phẩm'),
  ('CATEGORY_CREATE', 'Thêm mới danh mục'),
  ('CATEGORY_UPDATE', 'Cập nhật danh mục'),
  ('CATEGORY_DELETE', 'Xóa danh mục'),
  ('CATEGORY_MANAGE', 'Quản lý danh mục sản phẩm'),
  ('BRAND_VIEW', 'Xem danh sách thương hiệu'),
  ('BRAND_CREATE', 'Thêm mới thương hiệu'),
  ('BRAND_UPDATE', 'Cập nhật thương hiệu'),
  ('BRAND_DELETE', 'Xóa thương hiệu'),
  ('BRAND_MANAGE', 'Quản lý thương hiệu sản phẩm'),
  ('ATTRIBUTE_VIEW', 'Xem danh mục thuộc tính (EAV)'),
  ('ATTRIBUTE_MANAGE', 'Quản lý thuộc tính và thông số kỹ thuật'),
  ('INVENTORY_VIEW', 'Xem tồn kho và lịch sử xuất nhập'),
  ('INVENTORY_IMPORT', 'Nhập hàng vào kho (phiếu nhập)'),
  ('INVENTORY_TRANSFER', 'Điều chuyển hàng giữa các kho'),
  ('INVENTORY_MANAGE', 'Quản lý tồn kho và kho bãi'),
  ('WAREHOUSE_VIEW', 'Xem danh sách kho hàng'),
  ('WAREHOUSE_MANAGE', 'Quản lý danh sách kho hàng'),
  ('SUPPLIER_VIEW', 'Xem danh sách nhà cung cấp'),
  ('SUPPLIER_CREATE', 'Thêm mới nhà cung cấp'),
  ('SUPPLIER_UPDATE', 'Cập nhật nhà cung cấp'),
  ('SUPPLIER_DELETE', 'Xóa nhà cung cấp'),
  ('SUPPLIER_MANAGE', 'Quản lý nhà cung cấp'),
  ('ORDER_VIEW', 'Xem danh sách và chi tiết đơn hàng'),
  ('ORDER_UPDATE_STATUS', 'Cập nhật trạng thái đơn hàng'),
  ('ORDER_CANCEL', 'Hủy đơn hàng'),
  ('ORDER_MANAGE', 'Quản lý toàn diện đơn hàng'),
  ('REVIEW_VIEW', 'Xem danh sách đánh giá sản phẩm'),
  ('REVIEW_REPLY', 'Phản hồi đánh giá khách hàng'),
  ('REVIEW_DELETE', 'Xóa / ẩn đánh giá vi phạm'),
  ('DISCOUNT_VIEW', 'Xem danh sách mã giảm giá'),
  ('DISCOUNT_CREATE', 'Tạo mới mã giảm giá'),
  ('DISCOUNT_UPDATE', 'Cập nhật mã giảm giá'),
  ('DISCOUNT_DELETE', 'Xóa mã giảm giá'),
  ('DISCOUNT_MANAGE', 'Quản lý mã giảm giá'),
  ('BANNER_VIEW', 'Xem danh sách banner quảng cáo'),
  ('BANNER_CREATE', 'Thêm mới banner quảng cáo'),
  ('BANNER_UPDATE', 'Cập nhật banner quảng cáo'),
  ('BANNER_DELETE', 'Xóa banner quảng cáo'),
  ('BANNER_MANAGE', 'Quản lý banner quảng cáo'),
  ('NEWS_VIEW', 'Xem danh sách bài viết tin tức'),
  ('NEWS_CREATE', 'Soạn thảo bài viết mới'),
  ('NEWS_UPDATE', 'Chỉnh sửa bài viết tin tức'),
  ('NEWS_DELETE', 'Xóa bài viết tin tức'),
  ('NEWS_MANAGE', 'Quản lý tin tức và bài viết'),
  ('ROLE_VIEW', 'Xem danh sách chức vụ & vai trò'),
  ('ROLE_CREATE', 'Tạo chức vụ mới'),
  ('ROLE_UPDATE', 'Cập nhật chức vụ & phân quyền'),
  ('ROLE_DELETE', 'Xóa chức vụ'),
  ('ROLE_MANAGE', 'Toàn quyền quản lý chức vụ'),
  ('STAFF_VIEW', 'Xem danh sách nhân viên'),
  ('STAFF_CREATE', 'Thêm nhân viên mới'),
  ('STAFF_UPDATE', 'Cập nhật thông tin nhân sự'),
  ('STAFF_DELETE', 'Xóa tài khoản nhân viên'),
  ('STAFF_RESET_PWD', 'Đặt lại mật khẩu nhân viên'),
  ('STAFF_MANAGE', 'Toàn quyền quản lý nhân sự'),
  ('CUSTOMER_VIEW', 'Xem danh sách khách hàng & chi tiêu'),
  ('CUSTOMER_UPDATE', 'Cập nhật hồ sơ khách hàng'),
  ('CUSTOMER_STATUS', 'Khóa / Mở khóa tài khoản khách hàng'),
  ('CUSTOMER_RESET_PWD', 'Đặt lại mật khẩu khách hàng'),
  ('USER_VIEW', 'Xem danh sách người dùng hệ thống'),
  ('USER_MANAGE', 'Quản lý người dùng toàn hệ thống'),
  ('STATISTIC_VIEW', 'Xem báo cáo doanh thu & thống kê'),
  ('STATISTICS_VIEW', 'Xem biểu đồ phân tích kinh doanh'),
  ('SETTING_VIEW', 'Xem thông tin cấu hình hệ thống'),
  ('SETTING_UPDATE', 'Cập nhật cài đặt website & thanh toán'),
  ('SETTING_MANAGE', 'Quản trị toàn diện cấu hình hệ thống'),
  ('CHAT_VIEW', 'Xem danh sách và tin nhắn Live Chat'),
  ('CHAT_RESPOND', 'Tiếp nhận và phản hồi tin nhắn Live Chat'),
  ('CHAT_MANAGE', 'Toàn quyền quản trị Live Chat'),
  ('CHAT_BOT_VIEW', 'Xem kịch bản Chatbot (Bot Rules)'),
  ('CHAT_BOT_CREATE', 'Tạo mới kịch bản Chatbot'),
  ('CHAT_BOT_UPDATE', 'Cập nhật kịch bản Chatbot'),
  ('CHAT_BOT_DELETE', 'Xóa kịch bản Chatbot'),
  ('CHAT_BOT_MANAGE', 'Toàn quyền quản lý Chatbot')
AS new_vals
ON DUPLICATE KEY UPDATE `description` = new_vals.description;

-- 3. Role Permissions (Dynamic natural key mapping without hardcoded IDs)

-- 3.1. ROLE_ADMIN gets all permissions
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.role_id, p.permission_id
FROM `roles` r
CROSS JOIN `permissions` p
WHERE r.role_name = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE `role_permissions`.`role_id` = `role_permissions`.`role_id`;

-- 3.2. ROLE_STAFF operational permissions
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.role_id, p.permission_id
FROM `roles` r
JOIN `permissions` p ON p.permission_code IN (
  'PRODUCT_VIEW',
  'PRODUCT_CREATE',
  'PRODUCT_UPDATE',
  'CATEGORY_VIEW',
  'BRAND_VIEW',
  'ATTRIBUTE_VIEW',
  'INVENTORY_VIEW',
  'INVENTORY_IMPORT',
  'INVENTORY_TRANSFER',
  'INVENTORY_MANAGE',
  'WAREHOUSE_VIEW',
  'SUPPLIER_VIEW',
  'ORDER_VIEW',
  'ORDER_UPDATE_STATUS',
  'ORDER_CANCEL',
  'ORDER_MANAGE',
  'REVIEW_VIEW',
  'REVIEW_REPLY',
  'DISCOUNT_VIEW',
  'BANNER_VIEW',
  'NEWS_VIEW',
  'NEWS_CREATE',
  'NEWS_UPDATE',
  'CUSTOMER_VIEW',
  'CHAT_VIEW',
  'CHAT_RESPOND'
)
WHERE r.role_name = 'ROLE_STAFF'
ON DUPLICATE KEY UPDATE `role_permissions`.`role_id` = `role_permissions`.`role_id`;

-- 3.3. ROLE_CUSTOMER read/customer-facing permissions
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.role_id, p.permission_id
FROM `roles` r
JOIN `permissions` p ON p.permission_code IN (
  'PRODUCT_VIEW',
  'CATEGORY_VIEW',
  'BRAND_VIEW',
  'ATTRIBUTE_VIEW',
  'REVIEW_VIEW',
  'BANNER_VIEW',
  'NEWS_VIEW'
)
WHERE r.role_name = 'ROLE_CUSTOMER'
ON DUPLICATE KEY UPDATE `role_permissions`.`role_id` = `role_permissions`.`role_id`;

-- 4. Default Chatbot Rules (Natural Key: rule_name)
INSERT INTO `chat_bot_rules` (`rule_name`, `keywords`, `match_type`, `response_message`, `quick_replies`, `action_type`, `priority`, `is_active`)
VALUES
  ('Chào hỏi', 'xin chào,chào,hello,hi,hey,alo', 'CONTAINS', 'Xin chào! Tôi là trợ lý ảo của Complexus. Tôi có thể giúp bạn tư vấn về sản phẩm, bảo hành, đổi trả và nhiều hơn nữa. Bạn cần hỗ trợ gì hôm nay?', '["Tư vấn sản phẩm", "Kiểm tra bảo hành", "Chính sách đổi trả", "Gặp nhân viên"]', 'REPLY', 100, 1),
  ('Bảo hành', 'bảo hành,bao hanh,warranty,hỏng,lỗi,sửa chữa', 'CONTAINS', 'Complexus bảo hành chính hãng 12-36 tháng tùy sản phẩm. Để kiểm tra tình trạng bảo hành, bạn vui lòng cung cấp số đơn hàng hoặc IMEI/Serial Number của sản phẩm. Bạn có muốn được kết nối với nhân viên kỹ thuật không?', '["Kiểm tra đơn hàng", "Gặp kỹ thuật viên"]', 'REPLY', 90, 1),
  ('Đổi trả', 'đổi trả,doi tra,hoàn tiền,hoan tien,trả hàng,tra hang,return,refund', 'CONTAINS', 'Chính sách đổi trả của Complexus: Đổi mới trong 7 ngày nếu lỗi nhà sản xuất, hoàn tiền trong 30 ngày với sản phẩm còn nguyên hộp. Bạn muốn bắt đầu yêu cầu đổi trả ngay không?', '["Tạo yêu cầu đổi trả", "Gặp nhân viên hỗ trợ"]', 'REPLY', 85, 1),
  ('Giá cả - Báo giá', 'giá,gia,bao nhiêu,bao nhieu,price,báo giá,bao gia,cost,phí', 'CONTAINS', 'Để được báo giá chính xác nhất, bạn vui lòng cho biết tên sản phẩm hoặc cấu hình bạn quan tâm. Nhân viên tư vấn sẽ báo giá ngay trong vài phút!', '["Xem danh sách sản phẩm", "Gặp tư vấn viên"]', 'REPLY', 80, 1),
  ('Giao hàng', 'giao hàng,giao hang,ship,shipping,vận chuyển,van chuyen,nhận hàng,nhan hang,khi nào,khi nao', 'CONTAINS', 'Complexus giao hàng toàn quốc trong 2-5 ngày làm việc. Nội thành Hà Nội và TP.HCM giao trong ngày (đặt trước 12h). Miễn phí giao hàng đơn từ 500.000đ. Bạn cần theo dõi đơn hàng không?', '["Tra cứu đơn hàng", "Gặp nhân viên"]', 'REPLY', 75, 1),
  ('Thanh toán', 'thanh toán,thanh toan,payment,chuyển khoản,chuyen khoan,trả góp,tra gop,installment,visa,mastercard,momo,vnpay,zalopay', 'CONTAINS', 'Complexus hỗ trợ: Tiền mặt, Chuyển khoản ngân hàng, Thẻ tín dụng/ghi nợ (Visa/Mastercard), Ví điện tử (MoMo, VNPay, ZaloPay), và Trả góp 0% lãi suất qua thẻ tín dụng. Bạn muốn biết thêm về hình thức nào?', '["Trả góp 0%", "Gặp nhân viên tư vấn"]', 'REPLY', 70, 1),
  ('Gặp nhân viên - Chuyển giao', 'gặp nhân viên,gap nhan vien,tư vấn viên,tu van vien,gặp người thật,gap nguoi that,hỗ trợ trực tiếp,ho tro truc tiep,kết nối nhân viên,ket noi nhan vien,gặp người,gap nguoi,nhân viên', 'CONTAINS', 'Đang kết nối bạn với nhân viên tư vấn của Complexus. Vui lòng chờ trong giây lát, nhân viên sẽ tiếp nhận ngay!', NULL, 'HANDOVER_STAFF', 95, 1)
AS new_vals
ON DUPLICATE KEY UPDATE
  `keywords` = new_vals.keywords,
  `response_message` = new_vals.response_message,
  `quick_replies` = new_vals.quick_replies,
  `action_type` = new_vals.action_type,
  `priority` = new_vals.priority,
  `is_active` = new_vals.is_active;
