-- =============================================================
-- V3__add_sepay_payment_transaction.sql
-- Add SePay bank transfer payment support:
-- - orders table columns for payment reference, polling token hash, paid amount, reconciliation status
-- - payment_transactions table with unique constraint on (provider, external_transaction_id)
-- =============================================================

ALTER TABLE `orders`
  ADD COLUMN `payment_reference` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `payment_status`,
  ADD COLUMN `payment_polling_token_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `payment_reference`,
  ADD COLUMN `payment_polling_expires_at` datetime DEFAULT NULL AFTER `payment_polling_token_hash`,
  ADD COLUMN `paid_amount` decimal(15,2) NOT NULL DEFAULT '0.00' AFTER `payment_polling_expires_at`,
  ADD COLUMN `reconciliation_status` enum('PENDING','PARTIAL','OVERPAID','MATCHED_EXACT') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' AFTER `paid_amount`,
  ADD UNIQUE KEY `uk_orders_payment_reference` (`payment_reference`),
  ADD UNIQUE KEY `uk_orders_polling_token_hash` (`payment_polling_token_hash`);

CREATE TABLE `payment_transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `external_transaction_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `transfer_type` enum('in','out') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `gateway` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `account_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `transfer_amount` decimal(15,2) NOT NULL,
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reference_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order_id` bigint DEFAULT NULL,
  `received_at` datetime NOT NULL,
  `processed_at` datetime DEFAULT NULL,
  `reconciliation_result` enum('MATCHED_EXACT','PARTIAL','OVERPAID','UNMATCHED','IGNORED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `processing_status` enum('RECEIVED','PROCESSED','REVIEW_REQUIRED','RESOLVED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `failure_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `raw_payload` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_provider_transaction` (`provider`,`external_transaction_id`),
  KEY `idx_payment_trans_order` (`order_id`),
  CONSTRAINT `fk_payment_trans_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
