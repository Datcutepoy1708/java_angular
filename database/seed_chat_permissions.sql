-- ==============================================================================
-- Migration / Seed Script: Add Chat & Chatbot Permissions
-- ==============================================================================
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

USE computer_store_db;

-- 1. Insert permissions for Live Chat & Bot Rules if not already present
INSERT INTO `permissions` (`permission_code`, `description`)
SELECT 'CHAT_VIEW', 'Xem danh sách và tin nhắn Live Chat'
WHERE NOT EXISTS (SELECT 1 FROM `permissions` WHERE `permission_code` = 'CHAT_VIEW');

INSERT INTO `permissions` (`permission_code`, `description`)
SELECT 'CHAT_RESPOND', 'Tiếp nhận và phản hồi tin nhắn Live Chat'
WHERE NOT EXISTS (SELECT 1 FROM `permissions` WHERE `permission_code` = 'CHAT_RESPOND');

INSERT INTO `permissions` (`permission_code`, `description`)
SELECT 'CHAT_MANAGE', 'Toàn quyền quản trị Live Chat'
WHERE NOT EXISTS (SELECT 1 FROM `permissions` WHERE `permission_code` = 'CHAT_MANAGE');

INSERT INTO `permissions` (`permission_code`, `description`)
SELECT 'CHAT_BOT_VIEW', 'Xem kịch bản Chatbot (Bot Rules)'
WHERE NOT EXISTS (SELECT 1 FROM `permissions` WHERE `permission_code` = 'CHAT_BOT_VIEW');

INSERT INTO `permissions` (`permission_code`, `description`)
SELECT 'CHAT_BOT_CREATE', 'Tạo mới kịch bản Chatbot'
WHERE NOT EXISTS (SELECT 1 FROM `permissions` WHERE `permission_code` = 'CHAT_BOT_CREATE');

INSERT INTO `permissions` (`permission_code`, `description`)
SELECT 'CHAT_BOT_UPDATE', 'Cập nhật kịch bản Chatbot'
WHERE NOT EXISTS (SELECT 1 FROM `permissions` WHERE `permission_code` = 'CHAT_BOT_UPDATE');

INSERT INTO `permissions` (`permission_code`, `description`)
SELECT 'CHAT_BOT_DELETE', 'Xóa kịch bản Chatbot'
WHERE NOT EXISTS (SELECT 1 FROM `permissions` WHERE `permission_code` = 'CHAT_BOT_DELETE');

INSERT INTO `permissions` (`permission_code`, `description`)
SELECT 'CHAT_BOT_MANAGE', 'Toàn quyền quản lý Chatbot'
WHERE NOT EXISTS (SELECT 1 FROM `permissions` WHERE `permission_code` = 'CHAT_BOT_MANAGE');

-- 2. Assign all permissions to ROLE_ADMIN (role_id = 1)
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 1, `permission_id` FROM `permissions`
WHERE `permission_code` IN (
    'CHAT_VIEW', 'CHAT_RESPOND', 'CHAT_MANAGE',
    'CHAT_BOT_VIEW', 'CHAT_BOT_CREATE', 'CHAT_BOT_UPDATE', 'CHAT_BOT_DELETE', 'CHAT_BOT_MANAGE'
);

-- 3. Assign CHAT_VIEW and CHAT_RESPOND to ROLE_STAFF (role_id = 2)
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 2, `permission_id` FROM `permissions`
WHERE `permission_code` IN ('CHAT_VIEW', 'CHAT_RESPOND');
