UPDATE users SET password_hash = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG' WHERE user_id = 1;

INSERT INTO users (full_name, email, phone, password_hash, status, email_verified)
VALUES ('Admin Tester', 'admin@gmail.com', '0989999999', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'active', 1)
ON DUPLICATE KEY UPDATE password_hash = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', status = 'active';

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT user_id, 1 FROM users WHERE email = 'admin@gmail.com';
