-- ============================================================
-- V4 — Soft-delete columns: deleted_at (tách biệt status)
-- Chạy thủ công vào DB computer_store_db
-- ddl-auto: validate — không có Flyway
-- ============================================================

-- 1. brands: thêm status + deleted_at (hiện chưa có)
ALTER TABLE brands
  ADD COLUMN status ENUM('active','inactive') NOT NULL DEFAULT 'active',
  ADD COLUMN deleted_at DATETIME NULL;

-- 2. categories: thêm deleted_at (status enum đã có)
ALTER TABLE categories
  ADD COLUMN deleted_at DATETIME NULL;

-- 3. products: thêm deleted_at (status enum đã có)
ALTER TABLE products
  ADD COLUMN deleted_at DATETIME NULL;

-- 4. product_variants: thêm deleted_at
ALTER TABLE product_variants
  ADD COLUMN deleted_at DATETIME NULL;

-- 5. product_images: thêm deleted_at
ALTER TABLE product_images
  ADD COLUMN deleted_at DATETIME NULL;

-- 6. Indexes cho filter thùng rác (tất cả 5 bảng)
CREATE INDEX idx_brands_deleted_at     ON brands(deleted_at);
CREATE INDEX idx_categories_deleted_at ON categories(deleted_at);
CREATE INDEX idx_products_deleted_at   ON products(deleted_at);
CREATE INDEX idx_variants_deleted_at   ON product_variants(deleted_at);
CREATE INDEX idx_images_deleted_at     ON product_images(deleted_at);
