-- ============================================================
-- Chat Feature Tables
-- Chạy script này vào MySQL database: computer_store_db
-- ============================================================

-- 1. Bảng chat_conversations
CREATE TABLE IF NOT EXISTS `chat_conversations` (
  `conversation_id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id`      VARCHAR(100) NOT NULL,
  `user_id`         BIGINT DEFAULT NULL,
  `staff_id`        BIGINT DEFAULT NULL,
  `customer_name`   VARCHAR(150) DEFAULT NULL,
  `customer_email`  VARCHAR(150) DEFAULT NULL,
  `customer_phone`  VARCHAR(30) DEFAULT NULL,
  `status`          ENUM('BOT_ACTIVE','WAITING_STAFF','STAFF_ACTIVE','CLOSED') NOT NULL DEFAULT 'BOT_ACTIVE',
  `bot_unmatched_count`   INT NOT NULL DEFAULT 0,
  `unread_staff_count`    INT NOT NULL DEFAULT 0,
  `unread_customer_count` INT NOT NULL DEFAULT 0,
  `last_message`    TEXT DEFAULT NULL,
  `last_message_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`conversation_id`),
  KEY `idx_chat_session`      (`session_id`),
  KEY `idx_chat_user`         (`user_id`),
  KEY `idx_chat_staff`        (`staff_id`),
  KEY `idx_chat_status`       (`status`),
  KEY `idx_chat_last_message` (`last_message_at`),
  CONSTRAINT `fk_chat_user`  FOREIGN KEY (`user_id`)  REFERENCES `users`(`user_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_chat_staff` FOREIGN KEY (`staff_id`) REFERENCES `users`(`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bảng chat_messages
CREATE TABLE IF NOT EXISTS `chat_messages` (
  `message_id`      BIGINT NOT NULL AUTO_INCREMENT,
  `conversation_id` BIGINT NOT NULL,
  `sender_type`     ENUM('CUSTOMER','BOT','STAFF','SYSTEM') NOT NULL,
  `sender_id`       BIGINT DEFAULT NULL,
  `sender_name`     VARCHAR(150) NOT NULL,
  `content`         TEXT NOT NULL,
  `attachment_url`  VARCHAR(500) DEFAULT NULL,
  `metadata`        JSON DEFAULT NULL,
  `is_read`         TINYINT(1) NOT NULL DEFAULT 0,
  `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`message_id`),
  KEY `idx_msg_conversation` (`conversation_id`, `created_at`),
  CONSTRAINT `fk_msg_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `chat_conversations`(`conversation_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Bảng chat_bot_rules
CREATE TABLE IF NOT EXISTS `chat_bot_rules` (
  `rule_id`          INT NOT NULL AUTO_INCREMENT,
  `rule_name`        VARCHAR(150) NOT NULL,
  `keywords`         TEXT NOT NULL,
  `match_type`       ENUM('CONTAINS','EXACT','REGEX') NOT NULL DEFAULT 'CONTAINS',
  `response_message` TEXT NOT NULL,
  `quick_replies`    JSON DEFAULT NULL,
  `action_type`      ENUM('REPLY','HANDOVER_STAFF') NOT NULL DEFAULT 'REPLY',
  `priority`         INT NOT NULL DEFAULT 0,
  `is_active`        TINYINT(1) NOT NULL DEFAULT 1,
  `created_at`       DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`rule_id`),
  KEY `idx_rule_active_priority` (`is_active`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Seed data: Bot rules mẫu cho Complexus
INSERT INTO `chat_bot_rules` (`rule_name`, `keywords`, `match_type`, `response_message`, `quick_replies`, `action_type`, `priority`, `is_active`) VALUES
('Chào hỏi',
 'xin chào,chào,hello,hi,hey,alo',
 'CONTAINS',
 'Xin chào! Tôi là trợ lý ảo của Complexus. Tôi có thể giúp bạn tư vấn về sản phẩm, bảo hành, đổi trả và nhiều hơn nữa. Bạn cần hỗ trợ gì hôm nay?',
 '["Tư vấn sản phẩm", "Kiểm tra bảo hành", "Chính sách đổi trả", "Gặp nhân viên"]',
 'REPLY', 100, 1),

('Bảo hành',
 'bảo hành,bao hanh,warranty,hỏng,lỗi,sửa chữa',
 'CONTAINS',
 'Complexus bảo hành chính hãng 12-36 tháng tùy sản phẩm. Để kiểm tra tình trạng bảo hành, bạn vui lòng cung cấp số đơn hàng hoặc IMEI/Serial Number của sản phẩm. Bạn có muốn được kết nối với nhân viên kỹ thuật không?',
 '["Kiểm tra đơn hàng", "Gặp kỹ thuật viên"]',
 'REPLY', 90, 1),

('Đổi trả',
 'đổi trả,doi tra,hoàn tiền,hoan tien,trả hàng,tra hang,return,refund',
 'CONTAINS',
 'Chính sách đổi trả của Complexus: Đổi mới trong 7 ngày nếu lỗi nhà sản xuất, hoàn tiền trong 30 ngày với sản phẩm còn nguyên hộp. Bạn muốn bắt đầu yêu cầu đổi trả ngay không?',
 '["Tạo yêu cầu đổi trả", "Gặp nhân viên hỗ trợ"]',
 'REPLY', 85, 1),

('Giá cả - Báo giá',
 'giá,gia,bao nhiêu,bao nhieu,price,báo giá,bao gia,cost,phí',
 'CONTAINS',
 'Để được báo giá chính xác nhất, bạn vui lòng cho biết tên sản phẩm hoặc cấu hình bạn quan tâm. Nhân viên tư vấn sẽ báo giá ngay trong vài phút!',
 '["Xem danh sách sản phẩm", "Gặp tư vấn viên"]',
 'REPLY', 80, 1),

('Giao hàng',
 'giao hàng,giao hang,ship,shipping,vận chuyển,van chuyen,nhận hàng,nhan hang,khi nào,khi nao',
 'CONTAINS',
 'Complexus giao hàng toàn quốc trong 2-5 ngày làm việc. Nội thành Hà Nội và TP.HCM giao trong ngày (đặt trước 12h). Miễn phí giao hàng đơn từ 500.000đ. Bạn cần theo dõi đơn hàng không?',
 '["Tra cứu đơn hàng", "Gặp nhân viên"]',
 'REPLY', 75, 1),

('Thanh toán',
 'thanh toán,thanh toan,payment,chuyển khoản,chuyen khoan,trả góp,tra gop,installment,visa,mastercard,momo,vnpay,zalopay',
 'CONTAINS',
 'Complexus hỗ trợ: Tiền mặt, Chuyển khoản ngân hàng, Thẻ tín dụng/ghi nợ (Visa/Mastercard), Ví điện tử (MoMo, VNPay, ZaloPay), và Trả góp 0% lãi suất qua thẻ tín dụng. Bạn muốn biết thêm về hình thức nào?',
 '["Trả góp 0%", "Gặp nhân viên tư vấn"]',
 'REPLY', 70, 1),

('Gặp nhân viên - Chuyển giao',
 'gặp nhân viên,gap nhan vien,tư vấn viên,tu van vien,gặp người thật,gap nguoi that,hỗ trợ trực tiếp,ho tro truc tiep,kết nối nhân viên,ket noi nhan vien,gặp người,gap nguoi,nhân viên',
 'CONTAINS',
 'Đang kết nối bạn với nhân viên tư vấn của Complexus. Vui lòng chờ trong giây lát, nhân viên sẽ tiếp nhận ngay!',
 NULL,
 'HANDOVER_STAFF', 95, 1);
