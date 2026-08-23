-- =====================================================================
-- CSDL QUẢN LÝ WEBSITE BÁN MÁY TÍNH & LINH KIỆN
-- Stack: Java (Spring Boot) + Angular + MySQL
-- Charset: utf8mb4 (hỗ trợ tiếng Việt đầy đủ + emoji)
-- =====================================================================

CREATE DATABASE IF NOT EXISTS computer_store_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE computer_store_db;

SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- 1. QUẢN LÝ NGƯỜI DÙNG & PHÂN QUYỀN (RBAC)
-- =====================================================================

-- Vai trò: admin, staff, customer... (mở rộng dễ dàng, không hardcode)
CREATE TABLE roles (
    role_id       INT PRIMARY KEY AUTO_INCREMENT,
    role_name     VARCHAR(50) NOT NULL UNIQUE,   -- ROLE_ADMIN, ROLE_STAFF, ROLE_CUSTOMER
    description   VARCHAR(255),
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Quyền chi tiết (nếu cần phân quyền mịn: xem đơn, sửa sp, xóa user...)
CREATE TABLE permissions (
    permission_id   INT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(100) NOT NULL UNIQUE, -- PRODUCT_CREATE, ORDER_VIEW, USER_DELETE...
    description     VARCHAR(255)
) ENGINE=InnoDB;

-- Quan hệ n-n: 1 role có nhiều quyền
CREATE TABLE role_permissions (
    role_id        INT NOT NULL,
    permission_id  INT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Người dùng
CREATE TABLE users (
    user_id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name      VARCHAR(150) NOT NULL,
    email          VARCHAR(150) NOT NULL UNIQUE,
    phone          VARCHAR(20) UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,          -- BCrypt hash (Spring Security)
    avatar_url     VARCHAR(500),
    gender         ENUM('male','female','other'),
    birth_date     DATE,
    status         ENUM('active','inactive','banned') DEFAULT 'active',
    email_verified TINYINT(1) DEFAULT 0,
    provider       ENUM('local','google','facebook') DEFAULT 'local', -- hỗ trợ OAuth2 login
    provider_id    VARCHAR(255),
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at  DATETIME
) ENGINE=InnoDB;

-- Quan hệ n-n: 1 user có thể có nhiều role (thường 1, nhưng để mở rộng)
CREATE TABLE user_roles (
    user_id   BIGINT NOT NULL,
    role_id   INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Địa chỉ giao hàng (1 user nhiều địa chỉ)
CREATE TABLE addresses (
    address_id     BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id        BIGINT NOT NULL,
    receiver_name  VARCHAR(150) NOT NULL,
    phone          VARCHAR(20) NOT NULL,
    province       VARCHAR(100),
    district       VARCHAR(100),
    ward           VARCHAR(100),
    detail_address VARCHAR(255),
    is_default     TINYINT(1) DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Token cho refresh token / quên mật khẩu / verify email (JWT refresh - Spring Security)
CREATE TABLE auth_tokens (
    token_id     BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT NOT NULL,
    token        VARCHAR(500) NOT NULL,
    token_type   ENUM('refresh_token','reset_password','verify_email') NOT NULL,
    expires_at   DATETIME NOT NULL,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================================
-- 2. DANH MỤC, THƯƠNG HIỆU, SẢN PHẨM
-- =====================================================================

CREATE TABLE categories (
    category_id   INT PRIMARY KEY AUTO_INCREMENT,
    parent_id     INT NULL,                        -- danh mục cha (self-reference)
    name          VARCHAR(150) NOT NULL,
    slug          VARCHAR(180) NOT NULL UNIQUE,
    icon_url      VARCHAR(500),
    description   VARCHAR(500),
    sort_order    INT DEFAULT 0,
    status        ENUM('active','inactive') DEFAULT 'active',
    deleted_at    DATETIME NULL,
    FOREIGN KEY (parent_id) REFERENCES categories(category_id) ON DELETE SET NULL,
    INDEX idx_categories_deleted_at (deleted_at)
) ENGINE=InnoDB;

CREATE TABLE brands (
    brand_id     INT PRIMARY KEY AUTO_INCREMENT,
    name         VARCHAR(150) NOT NULL UNIQUE,
    slug         VARCHAR(180) NOT NULL UNIQUE,
    logo_url     VARCHAR(500),
    country      VARCHAR(100),
    description  VARCHAR(500),
    status       ENUM('active','inactive') DEFAULT 'active',
    deleted_at   DATETIME NULL,
    INDEX idx_brands_deleted_at (deleted_at)
) ENGINE=InnoDB;

CREATE TABLE suppliers (
    supplier_id   INT PRIMARY KEY AUTO_INCREMENT,
    name          VARCHAR(200) NOT NULL,
    contact_name  VARCHAR(150),
    phone         VARCHAR(20),
    email         VARCHAR(150),
    address       VARCHAR(300),
    status        ENUM('active','inactive') DEFAULT 'active',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE products (
    product_id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id     INT NOT NULL,
    brand_id        INT,
    supplier_id     INT,
    name            VARCHAR(250) NOT NULL,
    slug            VARCHAR(280) NOT NULL UNIQUE,
    sku             VARCHAR(100) UNIQUE,
    short_desc      VARCHAR(500),
    description     LONGTEXT,
    warranty_months INT DEFAULT 12,
    status          ENUM('active','inactive','discontinued') DEFAULT 'active',
    deleted_at      DATETIME NULL,
    view_count      INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(category_id),
    FOREIGN KEY (brand_id) REFERENCES brands(brand_id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),
    INDEX idx_products_category (category_id),
    INDEX idx_products_slug (slug),
    INDEX idx_products_deleted_at (deleted_at)
) ENGINE=InnoDB;

-- Biến thể sản phẩm (giá, dung lượng, màu... khác nhau)
CREATE TABLE product_variants (
    variant_id    BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id    BIGINT NOT NULL,
    variant_name  VARCHAR(200) NOT NULL,   -- vd: "16GB - Đen", "1TB - Trắng"
    sku_variant   VARCHAR(100) UNIQUE,
    price         DECIMAL(15,2) NOT NULL,
    sale_price    DECIMAL(15,2),
    cost_price    DECIMAL(15,2),           -- giá vốn, phục vụ thống kê lợi nhuận
    status        ENUM('active','inactive') DEFAULT 'active',
    deleted_at    DATETIME NULL,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    INDEX idx_variants_deleted_at (deleted_at)
) ENGINE=InnoDB;

-- Ảnh sản phẩm: ảnh chính & ảnh phụ
CREATE TABLE product_images (
    image_id     BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id   BIGINT NOT NULL,
    variant_id   BIGINT NULL,              -- NULL nếu ảnh dùng chung cho sản phẩm
    image_url    VARCHAR(500) NOT NULL,
    image_type   ENUM('main','sub') DEFAULT 'sub',
    sort_order   INT DEFAULT 0,
    alt_text     VARCHAR(255),
    deleted_at   DATETIME NULL,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    FOREIGN KEY (variant_id) REFERENCES product_variants(variant_id) ON DELETE CASCADE,
    INDEX idx_images_deleted_at (deleted_at)
) ENGINE=InnoDB;

-- Thuộc tính kỹ thuật theo từng danh mục (EAV - linh hoạt cho CPU/RAM/VGA...)
CREATE TABLE attributes (
    attribute_id   INT PRIMARY KEY AUTO_INCREMENT,
    category_id    INT NOT NULL,
    name           VARCHAR(150) NOT NULL,     -- "Số nhân", "Socket", "Bus RAM"...
    data_type      ENUM('text','number','boolean') DEFAULT 'text',
    unit           VARCHAR(50),               -- GHz, GB, W...
    sort_order     INT DEFAULT 0,
    FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE product_attribute_values (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id     BIGINT NOT NULL,
    attribute_id   INT NOT NULL,
    value          VARCHAR(255) NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    FOREIGN KEY (attribute_id) REFERENCES attributes(attribute_id) ON DELETE CASCADE,
    UNIQUE KEY uq_product_attribute (product_id, attribute_id)
) ENGINE=InnoDB;

-- Đánh giá sản phẩm
CREATE TABLE product_reviews (
    review_id    BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id   BIGINT NOT NULL,
    user_id      BIGINT NOT NULL,
    order_item_id BIGINT NULL,       -- chỉ cho review nếu đã mua (optional)
    rating       TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment      TEXT,
    status       ENUM('pending','approved','hidden') DEFAULT 'pending',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================================
-- 3. KHO HÀNG & NHẬP KHO
-- =====================================================================

CREATE TABLE warehouses (
    warehouse_id  INT PRIMARY KEY AUTO_INCREMENT,
    name          VARCHAR(150) NOT NULL,
    address       VARCHAR(300),
    phone         VARCHAR(20)
) ENGINE=InnoDB;

CREATE TABLE inventory (
    inventory_id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    variant_id     BIGINT NOT NULL,
    warehouse_id   INT NOT NULL,
    quantity       INT NOT NULL DEFAULT 0,
    reserved_qty   INT NOT NULL DEFAULT 0,   -- đã giữ cho đơn chưa hoàn tất
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (variant_id) REFERENCES product_variants(variant_id) ON DELETE CASCADE,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id),
    UNIQUE KEY uq_variant_warehouse (variant_id, warehouse_id)
) ENGINE=InnoDB;

CREATE TABLE stock_imports (
    import_id     BIGINT PRIMARY KEY AUTO_INCREMENT,
    supplier_id   INT NOT NULL,
    warehouse_id  INT NOT NULL,
    import_code   VARCHAR(50) UNIQUE,
    total_cost    DECIMAL(15,2) DEFAULT 0,
    note          VARCHAR(500),
    created_by    BIGINT,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id),
    FOREIGN KEY (created_by) REFERENCES users(user_id)
) ENGINE=InnoDB;

CREATE TABLE stock_import_items (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_id      BIGINT NOT NULL,
    variant_id     BIGINT NOT NULL,
    quantity       INT NOT NULL,
    import_price   DECIMAL(15,2) NOT NULL,
    FOREIGN KEY (import_id) REFERENCES stock_imports(import_id) ON DELETE CASCADE,
    FOREIGN KEY (variant_id) REFERENCES product_variants(variant_id)
) ENGINE=InnoDB;

-- Lịch sử biến động kho (truy vết mọi thay đổi số lượng)
CREATE TABLE inventory_logs (
    log_id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    variant_id      BIGINT NOT NULL,
    warehouse_id    INT NOT NULL,
    change_type     ENUM('import','sale','return','adjust','transfer') NOT NULL,
    quantity_change INT NOT NULL,       -- âm hoặc dương
    reference_type  VARCHAR(50),        -- 'order', 'stock_import'...
    reference_id    BIGINT,
    note            VARCHAR(255),
    created_by      BIGINT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (variant_id) REFERENCES product_variants(variant_id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id),
    FOREIGN KEY (created_by) REFERENCES users(user_id)
) ENGINE=InnoDB;

-- =====================================================================
-- 4. GIỎ HÀNG, ĐƠN HÀNG, THANH TOÁN
-- =====================================================================

CREATE TABLE cart_items (
    cart_id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT NOT NULL,
    variant_id   BIGINT NOT NULL,
    quantity     INT NOT NULL DEFAULT 1,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (variant_id) REFERENCES product_variants(variant_id) ON DELETE CASCADE,
    UNIQUE KEY uq_user_variant (user_id, variant_id)
) ENGINE=InnoDB;

CREATE TABLE orders (
    order_id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_code       VARCHAR(50) UNIQUE NOT NULL,
    user_id          BIGINT NOT NULL,
    address_id       BIGINT,
    -- snapshot địa chỉ tại thời điểm đặt (đề phòng user sửa/xóa địa chỉ sau này)
    receiver_name    VARCHAR(150),
    receiver_phone   VARCHAR(20),
    shipping_address VARCHAR(500),
    subtotal         DECIMAL(15,2) NOT NULL,
    discount_amount  DECIMAL(15,2) DEFAULT 0,
    shipping_fee     DECIMAL(15,2) DEFAULT 0,
    total_amount     DECIMAL(15,2) NOT NULL,
    discount_id      BIGINT NULL,
    payment_method   ENUM('cod','bank_transfer','vnpay','momo','zalopay') DEFAULT 'cod',
    payment_status   ENUM('unpaid','paid','refunded') DEFAULT 'unpaid',
    order_status     ENUM('pending','confirmed','processing','shipping','completed','cancelled') DEFAULT 'pending',
    note             VARCHAR(500),
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (address_id) REFERENCES addresses(address_id),
    INDEX idx_orders_user (user_id),
    INDEX idx_orders_status (order_status),
    INDEX idx_orders_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE order_items (
    order_item_id    BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id         BIGINT NOT NULL,
    variant_id       BIGINT NOT NULL,
    product_name_snapshot VARCHAR(250) NOT NULL,  -- lưu tên tại thời điểm mua
    price_snapshot   DECIMAL(15,2) NOT NULL,        -- lưu giá tại thời điểm mua
    quantity         INT NOT NULL,
    subtotal         DECIMAL(15,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (variant_id) REFERENCES product_variants(variant_id)
) ENGINE=InnoDB;

-- Lịch sử thay đổi trạng thái đơn (phục vụ tracking / timeline cho khách)
CREATE TABLE order_status_history (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id     BIGINT NOT NULL,
    status       VARCHAR(50) NOT NULL,
    note         VARCHAR(255),
    changed_by   BIGINT,
    changed_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(user_id)
) ENGINE=InnoDB;

-- Bảo hành / khiếu nại
CREATE TABLE warranty_claims (
    claim_id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_item_id     BIGINT NOT NULL,
    user_id           BIGINT NOT NULL,
    issue_description TEXT NOT NULL,
    status             ENUM('pending','processing','resolved','rejected') DEFAULT 'pending',
    resolution_note    VARCHAR(500),
    created_at         DATETIME DEFAULT CURRENT_TIMESTAMP,
    resolved_at        DATETIME,
    FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

-- =====================================================================
-- 5. MÃ GIẢM GIÁ
-- =====================================================================

CREATE TABLE discount_codes (
    discount_id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    code                   VARCHAR(50) UNIQUE NOT NULL,
    description            VARCHAR(255),
    discount_type          ENUM('percent','fixed') NOT NULL,
    discount_value         DECIMAL(15,2) NOT NULL,
    max_discount_amount    DECIMAL(15,2),          -- giới hạn tối đa khi giảm theo %
    min_order_value        DECIMAL(15,2) DEFAULT 0,
    usage_limit            INT,                    -- NULL = không giới hạn
    usage_limit_per_user   INT DEFAULT 1,
    used_count             INT DEFAULT 0,
    applicable_category_id INT NULL,
    start_date             DATETIME NOT NULL,
    end_date               DATETIME NOT NULL,
    status                 ENUM('active','inactive','expired') DEFAULT 'active',
    created_at             DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (applicable_category_id) REFERENCES categories(category_id)
) ENGINE=InnoDB;

CREATE TABLE discount_usage (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    discount_id   BIGINT NOT NULL,
    user_id       BIGINT NOT NULL,
    order_id      BIGINT NOT NULL,
    used_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (discount_id) REFERENCES discount_codes(discount_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
) ENGINE=InnoDB;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_discount FOREIGN KEY (discount_id) REFERENCES discount_codes(discount_id);

-- =====================================================================
-- 6. TIN TỨC / BLOG
-- =====================================================================

CREATE TABLE news_categories (
    news_cat_id  INT PRIMARY KEY AUTO_INCREMENT,
    name         VARCHAR(150) NOT NULL,
    slug         VARCHAR(180) UNIQUE NOT NULL
) ENGINE=InnoDB;

CREATE TABLE news (
    news_id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    news_cat_id   INT,
    title         VARCHAR(250) NOT NULL,
    slug          VARCHAR(280) UNIQUE NOT NULL,
    thumbnail_url VARCHAR(500),
    summary       VARCHAR(500),
    content       LONGTEXT,
    author_id     BIGINT,
    view_count    INT DEFAULT 0,
    status        ENUM('draft','published','hidden') DEFAULT 'draft',
    published_at  DATETIME,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (news_cat_id) REFERENCES news_categories(news_cat_id),
    FOREIGN KEY (author_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

-- =====================================================================
-- 7. BANNER QUẢNG CÁO
-- =====================================================================

CREATE TABLE banners (
    banner_id    BIGINT PRIMARY KEY AUTO_INCREMENT,
    title        VARCHAR(200),
    image_url    VARCHAR(500) NOT NULL,
    link_url     VARCHAR(500),
    position     ENUM('homepage_slider','sidebar','popup','category_top') DEFAULT 'homepage_slider',
    sort_order   INT DEFAULT 0,
    start_date   DATETIME,
    end_date     DATETIME,
    status       ENUM('active','inactive') DEFAULT 'active',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- =====================================================================
-- 8. THỐNG KÊ (bảng cache — cập nhật qua cron job / scheduled task Spring)
-- =====================================================================

CREATE TABLE daily_statistics (
    stat_date            DATE PRIMARY KEY,
    total_orders         INT DEFAULT 0,
    total_revenue        DECIMAL(18,2) DEFAULT 0,
    total_profit         DECIMAL(18,2) DEFAULT 0,
    new_customers        INT DEFAULT 0,
    total_products_sold  INT DEFAULT 0,
    cancelled_orders     INT DEFAULT 0,
    updated_at           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 9. DỮ LIỆU MẪU CHO ROLES & PERMISSIONS (khởi tạo hệ thống)
-- =====================================================================

INSERT INTO roles (role_name, description) VALUES
('ROLE_ADMIN', 'Quản trị viên - toàn quyền hệ thống'),
('ROLE_STAFF', 'Nhân viên - quản lý sản phẩm, đơn hàng, kho'),
('ROLE_CUSTOMER', 'Khách hàng - mua sắm trên website');

INSERT INTO permissions (permission_code, description) VALUES
('PRODUCT_VIEW', 'Xem sản phẩm'),
('PRODUCT_CREATE', 'Tạo sản phẩm'),
('PRODUCT_UPDATE', 'Cập nhật sản phẩm'),
('PRODUCT_DELETE', 'Xóa sản phẩm'),
('ORDER_VIEW', 'Xem đơn hàng'),
('ORDER_UPDATE_STATUS', 'Cập nhật trạng thái đơn hàng'),
('USER_MANAGE', 'Quản lý người dùng'),
('INVENTORY_MANAGE', 'Quản lý kho hàng'),
('DISCOUNT_MANAGE', 'Quản lý mã giảm giá'),
('NEWS_MANAGE', 'Quản lý tin tức'),
('BANNER_MANAGE', 'Quản lý banner'),
('STATISTIC_VIEW', 'Xem thống kê báo cáo');

-- Admin có tất cả quyền
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, permission_id FROM permissions;

-- Staff có quyền vận hành (không có USER_MANAGE)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 2, permission_id FROM permissions WHERE permission_code != 'USER_MANAGE';

-- Customer chỉ xem sản phẩm
INSERT INTO role_permissions (role_id, permission_id)
SELECT 3, permission_id FROM permissions WHERE permission_code = 'PRODUCT_VIEW';

-- =====================================================================
-- 10. VIEW HỖ TRỢ THỐNG KÊ NHANH (dùng cho dashboard admin)
-- =====================================================================

CREATE OR REPLACE VIEW view_revenue_by_day AS
SELECT DATE(created_at) AS report_date,
       COUNT(DISTINCT order_id) AS order_count,
       SUM(total_amount) AS revenue
FROM orders
WHERE order_status = 'completed'
GROUP BY DATE(created_at);

CREATE OR REPLACE VIEW view_best_selling_products AS
SELECT p.product_id, p.name, SUM(oi.quantity) AS total_sold,
       SUM(oi.subtotal) AS total_revenue
FROM order_items oi
JOIN product_variants pv ON oi.variant_id = pv.variant_id
JOIN products p ON pv.product_id = p.product_id
JOIN orders o ON oi.order_id = o.order_id
WHERE o.order_status = 'completed'
GROUP BY p.product_id, p.name
ORDER BY total_sold DESC;

CREATE OR REPLACE VIEW view_low_stock AS
SELECT pv.variant_id, p.name AS product_name, pv.variant_name,
       i.quantity, i.reserved_qty, (i.quantity - i.reserved_qty) AS available_qty
FROM inventory i
JOIN product_variants pv ON i.variant_id = pv.variant_id
JOIN products p ON pv.product_id = p.product_id
WHERE (i.quantity - i.reserved_qty) < 10;

-- =====================================================================
-- HẾT FILE
-- =====================================================================