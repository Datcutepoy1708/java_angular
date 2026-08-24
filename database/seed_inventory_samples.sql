-- ========================================================
-- Seed & Refine Sample Inventory Data & Audit Logs
-- ========================================================

-- 1. Refine Specific Stock Scenarios (In Stock, Low Stock <= 10, Out of Stock = 0)

-- Variant 4: Low stock across branches
UPDATE `inventory` SET `quantity` = 4, `reserved_qty` = 1, `updated_at` = NOW() WHERE `variant_id` = 4 AND `warehouse_id` = 1; -- Avail: 3
UPDATE `inventory` SET `quantity` = 5, `reserved_qty` = 2, `updated_at` = NOW() WHERE `variant_id` = 4 AND `warehouse_id` = 2; -- Avail: 3
UPDATE `inventory` SET `quantity` = 2, `reserved_qty` = 0, `updated_at` = NOW() WHERE `variant_id` = 4 AND `warehouse_id` = 3; -- Avail: 2

-- Variant 6: Low stock in Da Nang
UPDATE `inventory` SET `quantity` = 5, `reserved_qty` = 2, `updated_at` = NOW() WHERE `variant_id` = 6 AND `warehouse_id` = 3; -- Avail: 3

-- Variant 10: Low stock in HCM
UPDATE `inventory` SET `quantity` = 4, `reserved_qty` = 1, `updated_at` = NOW() WHERE `variant_id` = 10 AND `warehouse_id` = 2; -- Avail: 3

-- Variant 15: Out of stock completely across ALL branches
UPDATE `inventory` SET `quantity` = 0, `reserved_qty` = 0, `updated_at` = NOW() WHERE `variant_id` = 15 AND `warehouse_id` = 1;
UPDATE `inventory` SET `quantity` = 0, `reserved_qty` = 0, `updated_at` = NOW() WHERE `variant_id` = 15 AND `warehouse_id` = 2;
UPDATE `inventory` SET `quantity` = 0, `reserved_qty` = 0, `updated_at` = NOW() WHERE `variant_id` = 15 AND `warehouse_id` = 3;

-- Variant 30: Out of stock in Hanoi & Da Nang, available in HCM
UPDATE `inventory` SET `quantity` = 0, `reserved_qty` = 0, `updated_at` = NOW() WHERE `variant_id` = 30 AND `warehouse_id` = 1;
UPDATE `inventory` SET `quantity` = 18, `reserved_qty` = 2, `updated_at` = NOW() WHERE `variant_id` = 30 AND `warehouse_id` = 2;
UPDATE `inventory` SET `quantity` = 0, `reserved_qty` = 0, `updated_at` = NOW() WHERE `variant_id` = 30 AND `warehouse_id` = 3;

-- Variant 44: Out of stock in HCM
UPDATE `inventory` SET `quantity` = 0, `reserved_qty` = 0, `updated_at` = NOW() WHERE `variant_id` = 44 AND `warehouse_id` = 2;

-- 2. Clear old test logs and insert realistic, rich Inventory Audit Logs
DELETE FROM `inventory_logs`;

INSERT INTO `inventory_logs` (`log_id`, `variant_id`, `warehouse_id`, `change_type`, `quantity_change`, `reference_type`, `reference_id`, `note`, `created_by`, `created_at`) VALUES
(1, 1, 1, 'import', 50, 'purchase_order', 1, 'Nhập lô 50 máy MacBook Air M5 từ Nhà phân phối Synnex FPT (PO-2026-0801)', 1, '2026-08-20 09:15:00'),
(2, 2, 1, 'import', 30, 'purchase_order', 1, 'Nhập lô cấu hình nâng cấp MacBook Air M5 16GB/512GB', 1, '2026-08-20 09:30:00'),
(3, 1, 1, 'transfer', -15, 'warehouse_transfer', 2, 'Điều chuyển xuất từ Kho Tổng Miền Bắc sang Kho Tổng Miền Nam (Mã TR-2026-0802)', 1, '2026-08-21 10:00:00'),
(4, 1, 2, 'transfer', 15, 'warehouse_transfer', 1, 'Điều chuyển nhập từ Kho Tổng Miền Bắc vào Kho Tổng Miền Nam (Mã TR-2026-0802)', 1, '2026-08-21 14:30:00'),
(5, 1, 1, 'sale', -2, 'ORDER', 10024, 'Xuất kho giao thành công đơn hàng online #10024', 2, '2026-08-21 16:45:00'),
(6, 19, 2, 'import', 40, 'purchase_order', 2, 'Nhập lô 40 Laptop Lenovo LOQ 15IRX10 từ Lenovo Việt Nam (PO-2026-0805)', 2, '2026-08-22 08:30:00'),
(7, 19, 2, 'transfer', -10, 'warehouse_transfer', 3, 'Điều phối 10 Laptop Lenovo LOQ từ TP.HCM ra chi nhánh Đà Nẵng (TR-2026-0806)', 1, '2026-08-22 11:15:00'),
(8, 19, 3, 'transfer', 10, 'warehouse_transfer', 2, 'Nhập kho điều phối 10 Laptop Lenovo LOQ từ TP.HCM (TR-2026-0806)', 2, '2026-08-22 15:20:00'),
(9, 1, 1, 'adjust', 2, 'inventory_audit', NULL, 'Kiểm kê định kỳ tháng 8: Bù thừa 2 sản phẩm do sai lệch tem kiểm kê', 1, '2026-08-23 17:00:00'),
(10, 4, 1, 'adjust', -1, 'inventory_audit', NULL, 'Kiểm kê định kỳ tháng 8: Giảm 1 sản phẩm vỏ hộp bị móp chuyển vào kho bảo hành', 1, '2026-08-23 17:15:00'),
(11, 2, 2, 'sale', -1, 'ORDER', 10058, 'Xuất kho giao đơn hàng #10058 cho khách hàng tại Quận 1, TP.HCM', 2, '2026-08-24 10:20:00'),
(12, 11, 1, 'import', 25, 'purchase_order', 3, 'Nhập lô 25 máy iMac 24 inch M1 từ kho phân phối Apple', 1, '2026-08-24 14:00:00'),
(13, 11, 1, 'return', 1, 'RETURN_RMA', 5012, 'Khách đổi trả nâng cấp cấu hình, hoàn kho 1 iMac 24 inch (RMA-5012)', 2, '2026-08-24 16:30:00'),
(14, 13, 1, 'sale', -3, 'ORDER', 10089, 'Xuất kho hoàn tất đơn hàng doanh nghiệp #10089 (3 máy MacBook Pro 14 M5)', 1, '2026-08-24 18:00:00');
