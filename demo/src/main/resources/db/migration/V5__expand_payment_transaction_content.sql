-- Keep the persisted bank transfer content aligned with the validated DTO limit.
-- A dedicated migration is used because previously applied Flyway migrations are immutable.
ALTER TABLE `payment_transactions`
    MODIFY COLUMN `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL;
