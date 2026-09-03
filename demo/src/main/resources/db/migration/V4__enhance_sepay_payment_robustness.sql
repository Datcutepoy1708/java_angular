-- V4__enhance_sepay_payment_robustness.sql
-- Enhance payment robustness: paid_at, longer payment_reference, transaction_date, attempt tracking, and expanded processing statuses

-- 1. Orders table adjustments
ALTER TABLE `orders`
    ADD COLUMN `paid_at` datetime NULL AFTER `paid_amount`,
    MODIFY COLUMN `payment_reference` varchar(20) NULL;

-- 2. Payment transactions table adjustments
ALTER TABLE `payment_transactions`
    ADD COLUMN `transaction_date` datetime NULL AFTER `received_at`,
    ADD COLUMN `attempt_count` int NOT NULL DEFAULT 0 AFTER `raw_payload`,
    ADD COLUMN `last_attempt_at` datetime NULL AFTER `attempt_count`,
    MODIFY COLUMN `transfer_type` enum('in','out','unknown') NOT NULL DEFAULT 'unknown',
    MODIFY COLUMN `processing_status` enum('RECEIVED','PROCESSING','PROCESSED','FAILED_RETRYABLE','REVIEW_REQUIRED','RESOLVED') NOT NULL DEFAULT 'RECEIVED';
