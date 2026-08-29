-- Update chatbot rules with proper UTF-8 Vietnamese strings
USE computer_store_db;
SET NAMES utf8mb4;

DELETE FROM chat_bot_rules;

INSERT INTO chat_bot_rules (rule_id, rule_name, keywords, match_type, response_message, quick_replies, action_type, priority, is_active) VALUES
(1, 'Lời chào tự động', 'xin chào|chào|hi|hello|alo|start', 'CONTAINS', 
 'Xin chào! Tôi là trợ lý ảo của Complexus Store. Tôi có thể giúp gì cho bạn hôm nay?', 
 '["Tư vấn cấu hình PC", "Chính sách bảo hành", "Tra cứu đơn hàng", "Gặp nhân viên tư vấn"]', 
 'REPLY', 100, 1),

(2, 'Chính sách bảo hành', 'bảo hành|doi tra|đổi trả|loi|hỏng', 'CONTAINS', 
 'Complexus cam kết bảo hành chính hãng 1 đổi 1 trong 30 ngày đầu cho sản phẩm lỗi từ nhà sản xuất, và hỗ trợ bảo hành chính hãng từ 12 - 36 tháng tùy linh kiện.', 
 '["Xem chính sách đầy đủ", "Gặp nhân viên tư vấn"]', 
 'REPLY', 90, 1),

(3, 'Chuyển nhân viên tư vấn', 'nhân viên|gap nhan vien|tư vấn viên|người thật|ho tro truc tiep', 'CONTAINS', 
 'Yêu cầu của bạn đã được ghi nhận. Hệ thống đang kết nối bạn với chuyên viên tư vấn của Complexus, vui lòng chờ trong giây lát...', 
 '[]', 
 'HANDOVER_STAFF', 95, 1),

(4, 'Tra cứu đơn hàng', 'tra cứu|don hang|kiem tra don|van chuyen|giao hang', 'CONTAINS', 
 'Bạn có thể tra cứu tình trạng đơn hàng trực tiếp tại trang /orders bằng cách nhập mã đơn hàng hoặc số điện thoại đặt hàng.', 
 '["Đến trang tra cứu", "Gặp nhân viên"]', 
 'REPLY', 85, 1);
