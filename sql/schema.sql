CREATE DATABASE IF NOT EXISTS shopping_mall
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE shopping_mall;

CREATE TABLE roles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(32) NOT NULL,
  description VARCHAR(255) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_roles_name (name),
  CONSTRAINT chk_roles_name CHECK (name IN ('ADMIN', 'SHOP_OWNER', 'CUSTOMER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  mobile CHAR(10) NOT NULL,
  password_hash CHAR(60) NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  status VARCHAR(20) NOT NULL,
  failed_login_count INT NOT NULL DEFAULT 0,
  locked_until DATETIME(6) NULL,
  last_login_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_mobile (mobile),
  KEY idx_users_status (status),
  CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'REJECTED')),
  CONSTRAINT chk_users_mobile CHECK (mobile REGEXP '^[6-9][0-9]{9}$'),
  CONSTRAINT chk_users_email CHECK (email REGEXP '^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$'),
  CONSTRAINT chk_users_password_hash CHECK (password_hash REGEXP '^\\$2a\\$10\\$[./A-Za-z0-9]{53}$'),
  CONSTRAINT chk_users_failed CHECK (failed_login_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_roles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  assigned_at DATETIME(6) NOT NULL,
  assigned_by BIGINT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_roles_user (user_id),
  UNIQUE KEY uk_user_roles_pair (user_id, role_id),
  KEY idx_user_roles_role (role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_user_roles_assigned_by FOREIGN KEY (assigned_by) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE customers (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  date_of_birth DATE NULL,
  gender VARCHAR(20) NULL,
  address_line1 VARCHAR(200) NULL,
  address_line2 VARCHAR(200) NULL,
  city VARCHAR(80) NULL,
  state VARCHAR(80) NULL,
  postal_code VARCHAR(20) NULL,
  country VARCHAR(80) NOT NULL DEFAULT 'India',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_customers_user (user_id),
  CONSTRAINT fk_customers_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_customers_gender CHECK (gender IS NULL OR gender IN ('FEMALE', 'MALE', 'OTHER', 'PREFER_NOT_TO_SAY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE shop_owners (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  business_name VARCHAR(160) NOT NULL,
  gst_number VARCHAR(15) NULL,
  approval_status VARCHAR(20) NOT NULL,
  approved_by BIGINT NULL,
  approved_at DATETIME(6) NULL,
  rejection_reason VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_shop_owners_user (user_id),
  UNIQUE KEY uk_shop_owners_gst (gst_number),
  KEY idx_shop_owners_approval (approval_status),
  CONSTRAINT fk_shop_owners_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_shop_owners_approved_by FOREIGN KEY (approved_by) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_shop_owners_approval CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED')),
  CONSTRAINT chk_shop_owners_gst CHECK (gst_number IS NULL OR gst_number REGEXP '^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE shops (
  id BIGINT NOT NULL AUTO_INCREMENT,
  owner_id BIGINT NOT NULL,
  name VARCHAR(160) NOT NULL,
  slug VARCHAR(180) NOT NULL,
  description VARCHAR(2000) NULL,
  floor_label VARCHAR(40) NOT NULL,
  unit_number VARCHAR(20) NOT NULL,
  contact_phone CHAR(10) NOT NULL,
  contact_email VARCHAR(255) NOT NULL,
  opening_time TIME NOT NULL,
  closing_time TIME NOT NULL,
  logo_path VARCHAR(255) NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_shops_slug (slug),
  UNIQUE KEY uk_shops_unit (floor_label, unit_number),
  KEY idx_shops_owner (owner_id),
  KEY idx_shops_status_name (status, name),
  CONSTRAINT fk_shops_owner FOREIGN KEY (owner_id) REFERENCES shop_owners (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_shops_status CHECK (status IN ('DRAFT', 'ACTIVE', 'SUSPENDED', 'CLOSED')),
  CONSTRAINT chk_shops_phone CHECK (contact_phone REGEXP '^[6-9][0-9]{9}$'),
  CONSTRAINT chk_shops_hours CHECK (opening_time < closing_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE categories (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  slug VARCHAR(100) NOT NULL,
  description VARCHAR(500) NULL,
  parent_id BIGINT NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  display_order INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_categories_name (name),
  UNIQUE KEY uk_categories_slug (slug),
  KEY idx_categories_parent (parent_id),
  KEY idx_categories_active_order (active, display_order),
  CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_categories_order CHECK (display_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE products (
  id BIGINT NOT NULL AUTO_INCREMENT,
  shop_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  name VARCHAR(180) NOT NULL,
  slug VARCHAR(200) NOT NULL,
  sku VARCHAR(64) NOT NULL,
  description VARCHAR(4000) NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  compare_at_price DECIMAL(12,2) NULL,
  tax_rate_percent DECIMAL(5,2) NOT NULL,
  image_path VARCHAR(255) NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_products_shop_sku (shop_id, sku),
  UNIQUE KEY uk_products_shop_slug (shop_id, slug),
  KEY idx_products_category (category_id),
  KEY idx_products_status_name (status, name),
  CONSTRAINT fk_products_shop FOREIGN KEY (shop_id) REFERENCES shops (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_products_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
  CONSTRAINT chk_products_price CHECK (unit_price >= 0),
  CONSTRAINT chk_products_compare CHECK (compare_at_price IS NULL OR compare_at_price >= 0),
  CONSTRAINT chk_products_tax CHECK (tax_rate_percent >= 0 AND tax_rate_percent <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE inventory (
  id BIGINT NOT NULL AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  quantity_on_hand INT NOT NULL,
  quantity_reserved INT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_inventory_product (product_id),
  CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_inventory_on_hand CHECK (quantity_on_hand >= 0),
  CONSTRAINT chk_inventory_reserved CHECK (quantity_reserved >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE stock_movements (
  id BIGINT NOT NULL AUTO_INCREMENT,
  inventory_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  shop_id BIGINT NOT NULL,
  movement_type VARCHAR(20) NOT NULL,
  quantity_delta INT NOT NULL,
  reserved_delta INT NOT NULL,
  quantity_on_hand_after INT NOT NULL,
  quantity_reserved_after INT NOT NULL,
  reference_type VARCHAR(30) NULL,
  reference_id BIGINT NULL,
  note VARCHAR(500) NULL,
  created_by BIGINT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_stock_movements_product_time (product_id, created_at),
  KEY idx_stock_movements_shop_time (shop_id, created_at),
  KEY idx_stock_movements_reference (reference_type, reference_id),
  KEY idx_stock_movements_inventory (inventory_id),
  CONSTRAINT fk_stock_movements_inventory FOREIGN KEY (inventory_id) REFERENCES inventory (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_stock_movements_shop FOREIGN KEY (shop_id) REFERENCES shops (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_stock_movements_user FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_stock_movements_type CHECK (movement_type IN ('OPENING', 'PURCHASE', 'SALE', 'RESERVE', 'RELEASE', 'ADJUSTMENT', 'RETURN', 'DAMAGE')),
  CONSTRAINT chk_stock_movements_after CHECK (quantity_on_hand_after >= 0 AND quantity_reserved_after >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE carts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  open_customer_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'OPEN' THEN customer_id ELSE NULL END) STORED,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_carts_one_open (open_customer_id),
  KEY idx_carts_customer (customer_id, status),
  CONSTRAINT fk_carts_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_carts_status CHECK (status IN ('OPEN', 'CHECKED_OUT', 'ABANDONED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cart_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  cart_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  unit_price_snapshot DECIMAL(12,2) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cart_items_product (cart_id, product_id),
  KEY idx_cart_items_product (product_id),
  CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_cart_items_qty CHECK (quantity >= 1 AND quantity <= 20),
  CONSTRAINT chk_cart_items_price CHECK (unit_price_snapshot >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE wishlists (
  id BIGINT NOT NULL AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wishlists_customer (customer_id),
  CONSTRAINT fk_wishlists_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE wishlist_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  wishlist_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wishlist_items_product (wishlist_id, product_id),
  KEY idx_wishlist_items_product (product_id),
  CONSTRAINT fk_wishlist_items_wishlist FOREIGN KEY (wishlist_id) REFERENCES wishlists (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_wishlist_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE coupons (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(40) NOT NULL,
  description VARCHAR(255) NOT NULL,
  discount_type VARCHAR(20) NOT NULL,
  discount_value DECIMAL(12,2) NOT NULL,
  min_order_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  max_discount_amount DECIMAL(12,2) NULL,
  usage_limit INT NULL,
  usage_count INT NOT NULL DEFAULT 0,
  per_customer_limit INT NOT NULL DEFAULT 1,
  starts_at DATETIME(6) NOT NULL,
  ends_at DATETIME(6) NOT NULL,
  active TINYINT(1) NOT NULL,
  scope VARCHAR(20) NOT NULL,
  shop_id BIGINT NULL,
  category_id BIGINT NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_coupons_code (code),
  KEY idx_coupons_active_window (active, starts_at, ends_at),
  KEY idx_coupons_shop (shop_id),
  KEY idx_coupons_category (category_id),
  CONSTRAINT fk_coupons_shop FOREIGN KEY (shop_id) REFERENCES shops (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_coupons_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_coupons_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_coupons_type CHECK (discount_type IN ('PERCENT', 'FIXED')),
  CONSTRAINT chk_coupons_scope CHECK (scope IN ('MALL', 'SHOP', 'CATEGORY')),
  CONSTRAINT chk_coupons_value CHECK (discount_value > 0),
  CONSTRAINT chk_coupons_percent CHECK (discount_type <> 'PERCENT' OR discount_value <= 100),
  CONSTRAINT chk_coupons_min CHECK (min_order_amount >= 0),
  CONSTRAINT chk_coupons_max CHECK (max_discount_amount IS NULL OR max_discount_amount >= 0),
  CONSTRAINT chk_coupons_usage CHECK (usage_count >= 0 AND (usage_limit IS NULL OR usage_limit > 0)),
  CONSTRAINT chk_coupons_per_customer CHECK (per_customer_limit > 0),
  CONSTRAINT chk_coupons_window CHECK (ends_at > starts_at),
  CONSTRAINT chk_coupons_scope_fk CHECK (
    (scope = 'MALL' AND shop_id IS NULL AND category_id IS NULL)
    OR (scope = 'SHOP' AND shop_id IS NOT NULL AND category_id IS NULL)
    OR (scope = 'CATEGORY' AND category_id IS NOT NULL AND shop_id IS NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE checkouts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  checkout_number VARCHAR(32) NOT NULL,
  customer_id BIGINT NOT NULL,
  idempotency_key CHAR(36) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_checkouts_number (checkout_number),
  UNIQUE KEY uk_checkouts_key (idempotency_key),
  KEY idx_checkouts_customer (customer_id, created_at),
  CONSTRAINT fk_checkouts_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE orders (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_number VARCHAR(32) NOT NULL,
  checkout_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  shop_id BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL,
  idempotency_key VARCHAR(64) NOT NULL,
  currency_code CHAR(3) NOT NULL DEFAULT 'INR',
  subtotal DECIMAL(12,2) NOT NULL,
  discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  total_amount DECIMAL(12,2) NOT NULL,
  coupon_id BIGINT NULL,
  coupon_code_snapshot VARCHAR(40) NULL,
  customer_note VARCHAR(500) NULL,
  cancel_reason VARCHAR(500) NULL,
  cancelled_at DATETIME(6) NULL,
  placed_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_orders_number (order_number),
  UNIQUE KEY uk_orders_idempotency (idempotency_key),
  KEY idx_orders_customer_placed (customer_id, placed_at),
  KEY idx_orders_shop_status_placed (shop_id, status, placed_at),
  KEY idx_orders_checkout (checkout_id),
  KEY idx_orders_coupon (coupon_id),
  CONSTRAINT fk_orders_checkout FOREIGN KEY (checkout_id) REFERENCES checkouts (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_orders_shop FOREIGN KEY (shop_id) REFERENCES shops (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_orders_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_orders_status CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'PACKED', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'PAYMENT_FAILED')),
  CONSTRAINT chk_orders_currency CHECK (currency_code = 'INR'),
  CONSTRAINT chk_orders_money CHECK (subtotal >= 0 AND discount_amount >= 0 AND tax_amount >= 0 AND total_amount >= 0),
  CONSTRAINT chk_orders_discount_cap CHECK (discount_amount <= subtotal)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name_snapshot VARCHAR(180) NOT NULL,
  sku_snapshot VARCHAR(64) NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  tax_rate_percent DECIMAL(5,2) NOT NULL,
  discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  tax_amount DECIMAL(12,2) NOT NULL,
  line_total DECIMAL(12,2) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_items_product (order_id, product_id),
  KEY idx_order_items_product (product_id),
  CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_order_items_qty CHECK (quantity >= 1 AND quantity <= 20),
  CONSTRAINT chk_order_items_money CHECK (
    unit_price >= 0 AND discount_amount >= 0 AND tax_amount >= 0 AND line_total >= 0
    AND discount_amount <= (unit_price * quantity)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_status_history (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  from_status VARCHAR(30) NULL,
  to_status VARCHAR(30) NOT NULL,
  changed_by BIGINT NULL,
  note VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_order_status_history_order (order_id, created_at),
  CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_order_status_history_user FOREIGN KEY (changed_by) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_order_status_history_from CHECK (from_status IS NULL OR from_status IN ('PENDING_PAYMENT', 'CONFIRMED', 'PACKED', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'PAYMENT_FAILED')),
  CONSTRAINT chk_order_status_history_to CHECK (to_status IN ('PENDING_PAYMENT', 'CONFIRMED', 'PACKED', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'PAYMENT_FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE payments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  payment_number VARCHAR(32) NOT NULL,
  status VARCHAR(20) NOT NULL,
  method VARCHAR(30) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  currency_code CHAR(3) NOT NULL,
  simulated_outcome VARCHAR(20) NULL,
  gateway_reference VARCHAR(64) NULL,
  failure_reason VARCHAR(255) NULL,
  paid_at DATETIME(6) NULL,
  refunded_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_payments_order (order_id),
  UNIQUE KEY uk_payments_number (payment_number),
  UNIQUE KEY uk_payments_gateway (gateway_reference),
  KEY idx_payments_status_paid (status, paid_at),
  CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')),
  CONSTRAINT chk_payments_method CHECK (method = 'SIMULATED_CARD'),
  CONSTRAINT chk_payments_outcome CHECK (simulated_outcome IS NULL OR simulated_outcome IN ('SUCCESS', 'DECLINED')),
  CONSTRAINT chk_payments_amount CHECK (amount >= 0),
  CONSTRAINT chk_payments_currency CHECK (currency_code = 'INR')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE invoices (
  id BIGINT NOT NULL AUTO_INCREMENT,
  invoice_number VARCHAR(32) NOT NULL,
  order_id BIGINT NOT NULL,
  payment_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  shop_id BIGINT NOT NULL,
  billed_name VARCHAR(120) NOT NULL,
  billed_email VARCHAR(255) NOT NULL,
  billed_mobile CHAR(10) NOT NULL,
  billed_address VARCHAR(500) NOT NULL,
  subtotal DECIMAL(12,2) NOT NULL,
  discount_amount DECIMAL(12,2) NOT NULL,
  tax_amount DECIMAL(12,2) NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL,
  currency_code CHAR(3) NOT NULL,
  issued_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_invoices_number (invoice_number),
  UNIQUE KEY uk_invoices_order (order_id),
  UNIQUE KEY uk_invoices_payment (payment_id),
  KEY idx_invoices_customer_issued (customer_id, issued_at),
  KEY idx_invoices_shop_issued (shop_id, issued_at),
  CONSTRAINT fk_invoices_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_invoices_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_invoices_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_invoices_shop FOREIGN KEY (shop_id) REFERENCES shops (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_invoices_money CHECK (subtotal >= 0 AND discount_amount >= 0 AND tax_amount >= 0 AND total_amount >= 0),
  CONSTRAINT chk_invoices_currency CHECK (currency_code = 'INR')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE coupon_redemptions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  coupon_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  checkout_id BIGINT NOT NULL,
  discount_amount DECIMAL(12,2) NOT NULL,
  redeemed_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_coupon_redemptions_order (order_id),
  KEY idx_coupon_redemptions_customer (coupon_id, customer_id),
  KEY idx_coupon_redemptions_checkout (checkout_id),
  CONSTRAINT fk_coupon_redemptions_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_coupon_redemptions_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_coupon_redemptions_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_coupon_redemptions_checkout FOREIGN KEY (checkout_id) REFERENCES checkouts (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_coupon_redemptions_amount CHECK (discount_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reviews (
  id BIGINT NOT NULL AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  order_item_id BIGINT NOT NULL,
  rating TINYINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  body VARCHAR(2000) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reviews_order_item (order_item_id),
  KEY idx_reviews_product_status (product_id, status),
  KEY idx_reviews_customer (customer_id),
  CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_reviews_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_reviews_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),
  CONSTRAINT chk_reviews_status CHECK (status IN ('PUBLISHED', 'HIDDEN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE feedback (
  id BIGINT NOT NULL AUTO_INCREMENT,
  customer_id BIGINT NULL,
  source VARCHAR(20) NOT NULL,
  guest_name VARCHAR(120) NULL,
  guest_email VARCHAR(255) NULL,
  subject VARCHAR(160) NOT NULL,
  message VARCHAR(2000) NOT NULL,
  rating TINYINT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_feedback_status_created (status, created_at),
  KEY idx_feedback_customer (customer_id),
  CONSTRAINT fk_feedback_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_feedback_source CHECK (source IN ('PROFILE', 'CONTACT')),
  CONSTRAINT chk_feedback_status CHECK (status IN ('OPEN', 'REVIEWED', 'CLOSED')),
  CONSTRAINT chk_feedback_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),
  CONSTRAINT chk_feedback_shape CHECK (
    (source = 'PROFILE' AND customer_id IS NOT NULL AND guest_name IS NULL AND guest_email IS NULL AND rating IS NOT NULL)
    OR (source = 'CONTACT' AND customer_id IS NULL AND guest_name IS NOT NULL AND guest_email IS NOT NULL AND rating IS NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE notifications (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type VARCHAR(40) NOT NULL,
  title VARCHAR(160) NOT NULL,
  body VARCHAR(1000) NOT NULL,
  link_path VARCHAR(255) NULL,
  read_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_notifications_user_unread (user_id, read_at, created_at),
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_notifications_type CHECK (type IN ('ORDER_PLACED', 'ORDER_STATUS', 'PAYMENT', 'LOW_STOCK', 'COMPLAINT', 'APPROVAL', 'PARKING', 'GENERAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE complaints (
  id BIGINT NOT NULL AUTO_INCREMENT,
  complaint_number VARCHAR(32) NOT NULL,
  customer_id BIGINT NOT NULL,
  shop_id BIGINT NULL,
  order_id BIGINT NULL,
  category VARCHAR(30) NOT NULL,
  subject VARCHAR(160) NOT NULL,
  description VARCHAR(2000) NOT NULL,
  status VARCHAR(20) NOT NULL,
  assigned_to BIGINT NULL,
  resolution_note VARCHAR(1000) NULL,
  resolved_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_complaints_number (complaint_number),
  KEY idx_complaints_status_created (status, created_at),
  KEY idx_complaints_customer (customer_id),
  KEY idx_complaints_shop (shop_id),
  KEY idx_complaints_order (order_id),
  CONSTRAINT fk_complaints_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_complaints_shop FOREIGN KEY (shop_id) REFERENCES shops (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_complaints_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_complaints_assigned FOREIGN KEY (assigned_to) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_complaints_category CHECK (category IN ('ORDER', 'PRODUCT', 'BILLING', 'PARKING', 'STAFF', 'OTHER')),
  CONSTRAINT chk_complaints_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE parking_rates (
  id BIGINT NOT NULL AUTO_INCREMENT,
  vehicle_type VARCHAR(20) NOT NULL,
  grace_minutes INT NOT NULL,
  hourly_rate DECIMAL(10,2) NOT NULL,
  daily_cap DECIMAL(10,2) NOT NULL,
  currency_code CHAR(3) NOT NULL DEFAULT 'INR',
  active TINYINT(1) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_parking_rates_vehicle (vehicle_type),
  CONSTRAINT chk_parking_rates_vehicle CHECK (vehicle_type IN ('CAR', 'BIKE')),
  CONSTRAINT chk_parking_rates_grace CHECK (grace_minutes >= 0),
  CONSTRAINT chk_parking_rates_money CHECK (hourly_rate >= 0 AND daily_cap >= hourly_rate),
  CONSTRAINT chk_parking_rates_currency CHECK (currency_code = 'INR')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE parking_slots (
  id BIGINT NOT NULL AUTO_INCREMENT,
  slot_code VARCHAR(20) NOT NULL,
  zone VARCHAR(40) NOT NULL,
  vehicle_type VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_parking_slots_code (slot_code),
  KEY idx_parking_slots_lookup (zone, vehicle_type, status),
  CONSTRAINT chk_parking_slots_code CHECK (slot_code REGEXP '^[A-Z]-[0-9]{3}$'),
  CONSTRAINT chk_parking_slots_vehicle CHECK (vehicle_type IN ('CAR', 'BIKE')),
  CONSTRAINT chk_parking_slots_status CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'OUT_OF_SERVICE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE parking_sessions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  ticket_number VARCHAR(32) NOT NULL,
  slot_id BIGINT NOT NULL,
  vehicle_type VARCHAR(20) NOT NULL,
  vehicle_number VARCHAR(20) NOT NULL,
  entry_at DATETIME(6) NOT NULL,
  exit_at DATETIME(6) NULL,
  rate_id BIGINT NOT NULL,
  grace_minutes_snapshot INT NOT NULL,
  hourly_rate_snapshot DECIMAL(10,2) NOT NULL,
  daily_cap_snapshot DECIMAL(10,2) NOT NULL,
  charged_amount DECIMAL(10,2) NULL,
  status VARCHAR(20) NOT NULL,
  recorded_by BIGINT NOT NULL,
  open_slot_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'OPEN' THEN slot_id ELSE NULL END) STORED,
  open_vehicle_number VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN status = 'OPEN' THEN vehicle_number ELSE NULL END) STORED,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_parking_sessions_ticket (ticket_number),
  UNIQUE KEY uk_parking_sessions_open_slot (open_slot_id),
  UNIQUE KEY uk_parking_sessions_open_vehicle (open_vehicle_number),
  KEY idx_parking_sessions_slot_status (slot_id, status),
  KEY idx_parking_sessions_vehicle_entry (vehicle_number, entry_at),
  KEY idx_parking_sessions_status_entry (status, entry_at),
  CONSTRAINT fk_parking_sessions_slot FOREIGN KEY (slot_id) REFERENCES parking_slots (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_parking_sessions_rate FOREIGN KEY (rate_id) REFERENCES parking_rates (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_parking_sessions_user FOREIGN KEY (recorded_by) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_parking_sessions_vehicle CHECK (vehicle_type IN ('CAR', 'BIKE')),
  CONSTRAINT chk_parking_sessions_plate CHECK (vehicle_number REGEXP '^[A-Z]{2}[0-9]{2}[A-Z]{1,2}[0-9]{4}$'),
  CONSTRAINT chk_parking_sessions_status CHECK (status IN ('OPEN', 'CLOSED', 'VOID')),
  CONSTRAINT chk_parking_sessions_money CHECK (
    grace_minutes_snapshot >= 0
    AND hourly_rate_snapshot >= 0
    AND daily_cap_snapshot >= hourly_rate_snapshot
    AND (charged_amount IS NULL OR charged_amount >= 0)
  ),
  CONSTRAINT chk_parking_sessions_times CHECK (exit_at IS NULL OR exit_at >= entry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE employees (
  id BIGINT NOT NULL AUTO_INCREMENT,
  employee_code VARCHAR(20) NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(255) NULL,
  mobile CHAR(10) NULL,
  job_title VARCHAR(80) NOT NULL,
  department VARCHAR(40) NOT NULL,
  hired_on DATE NOT NULL,
  status VARCHAR(20) NOT NULL,
  monthly_salary DECIMAL(12,2) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_employees_code (employee_code),
  UNIQUE KEY uk_employees_email (email),
  UNIQUE KEY uk_employees_mobile (mobile),
  KEY idx_employees_status (status),
  CONSTRAINT chk_employees_department CHECK (department IN ('SECURITY', 'HOUSEKEEPING', 'PARKING', 'ADMIN_OFFICE', 'MAINTENANCE')),
  CONSTRAINT chk_employees_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
  CONSTRAINT chk_employees_salary CHECK (monthly_salary >= 0),
  CONSTRAINT chk_employees_mobile CHECK (mobile IS NULL OR mobile REGEXP '^[6-9][0-9]{9}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE attendance (
  id BIGINT NOT NULL AUTO_INCREMENT,
  employee_id BIGINT NOT NULL,
  work_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL,
  check_in TIME NULL,
  check_out TIME NULL,
  note VARCHAR(255) NULL,
  recorded_by BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_attendance_employee_date (employee_id, work_date),
  KEY idx_attendance_date (work_date),
  CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_attendance_user FOREIGN KEY (recorded_by) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_attendance_status CHECK (status IN ('PRESENT', 'ABSENT', 'LEAVE', 'HALF_DAY')),
  CONSTRAINT chk_attendance_times CHECK (
    ((status IN ('ABSENT', 'LEAVE') AND check_in IS NULL AND check_out IS NULL)
    OR (status IN ('PRESENT', 'HALF_DAY') AND check_in IS NOT NULL AND (check_out IS NULL OR check_out > check_in)))
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE salary_records (
  id BIGINT NOT NULL AUTO_INCREMENT,
  employee_id BIGINT NOT NULL,
  `year_month` CHAR(7) NOT NULL,
  base_amount DECIMAL(12,2) NOT NULL,
  deduction_amount DECIMAL(12,2) NOT NULL,
  net_amount DECIMAL(12,2) NOT NULL,
  present_days INT NOT NULL,
  absent_days INT NOT NULL,
  half_days INT NOT NULL,
  leave_days INT NOT NULL,
  status VARCHAR(20) NOT NULL,
  paid_at DATETIME(6) NULL,
  generated_by BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_salary_employee_month (employee_id, `year_month`),
  KEY idx_salary_status (status),
  CONSTRAINT fk_salary_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_salary_user FOREIGN KEY (generated_by) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_salary_month CHECK (`year_month` REGEXP '^[0-9]{4}-[0-9]{2}$'),
  CONSTRAINT chk_salary_status CHECK (status IN ('DRAFT', 'PAID')),
  CONSTRAINT chk_salary_money CHECK (
    base_amount >= 0 AND deduction_amount >= 0 AND net_amount >= 0
    AND net_amount = base_amount - deduction_amount
  ),
  CONSTRAINT chk_salary_days CHECK (present_days >= 0 AND absent_days >= 0 AND half_days >= 0 AND leave_days >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE activity_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  actor_user_id BIGINT NULL,
  action VARCHAR(60) NOT NULL,
  entity_type VARCHAR(40) NOT NULL,
  entity_id BIGINT NULL,
  summary VARCHAR(500) NOT NULL,
  ip_address VARCHAR(45) NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_activity_logs_actor (actor_user_id, created_at),
  KEY idx_activity_logs_entity (entity_type, entity_id),
  KEY idx_activity_logs_action (action, created_at),
  CONSTRAINT fk_activity_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_activity_logs_action CHECK (action IN (
    'USER_STATUS_CHANGED', 'SHOP_APPROVED', 'SHOP_REJECTED', 'SHOP_STATUS_CHANGED',
    'PRODUCT_ARCHIVED', 'COUPON_DEACTIVATED', 'SETTINGS_UPDATED', 'PARKING_RATE_UPDATED',
    'PARKING_VOIDED', 'SALARY_PAID', 'COMPLAINT_RESOLVED', 'COMPLAINT_REJECTED',
    'NOTIFICATION_SENT', 'REVIEW_HIDDEN', 'ORDER_CANCELLED', 'PAYMENT_REFUNDED'
  ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE password_reset_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_hash CHAR(64) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  used_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_password_reset_hash (token_hash),
  KEY idx_password_reset_user (user_id, created_at),
  CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_password_reset_hash CHECK (token_hash REGEXP '^[0-9a-f]{64}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE app_settings (
  id BIGINT NOT NULL,
  mall_name VARCHAR(160) NOT NULL,
  currency_code CHAR(3) NOT NULL,
  default_tax_percent DECIMAL(5,2) NOT NULL,
  low_stock_threshold INT NOT NULL,
  max_upload_bytes INT NOT NULL,
  support_email VARCHAR(255) NOT NULL,
  support_phone CHAR(10) NOT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_app_settings_user FOREIGN KEY (updated_by) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_app_settings_id CHECK (id = 1),
  CONSTRAINT chk_app_settings_currency CHECK (currency_code = 'INR'),
  CONSTRAINT chk_app_settings_tax CHECK (default_tax_percent >= 0 AND default_tax_percent <= 100),
  CONSTRAINT chk_app_settings_threshold CHECK (low_stock_threshold >= 0),
  CONSTRAINT chk_app_settings_upload CHECK (max_upload_bytes > 0 AND max_upload_bytes <= 2097152),
  CONSTRAINT chk_app_settings_phone CHECK (support_phone REGEXP '^[0-9]{10}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE number_sequences (
  id BIGINT NOT NULL AUTO_INCREMENT,
  sequence_name VARCHAR(40) NOT NULL,
  prefix VARCHAR(8) NOT NULL,
  next_value BIGINT NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_number_sequences_name (sequence_name),
  CONSTRAINT chk_number_sequences_name CHECK (sequence_name IN ('ORDER', 'INVOICE', 'PAYMENT', 'COMPLAINT', 'PARKING', 'EMPLOYEE', 'CHECKOUT')),
  CONSTRAINT chk_number_sequences_next CHECK (next_value > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO app_settings (
  id, mall_name, currency_code, default_tax_percent, low_stock_threshold,
  max_upload_bytes, support_email, support_phone, updated_by,
  created_at, updated_at, version
) VALUES (
  1, 'Shopping Mall', 'INR', 5.00, 5, 2097152, 'admin@mall.local', '8045678901', NULL,
  '2026-10-01 00:00:00.000000', '2026-10-01 00:00:00.000000', 0
);

INSERT INTO number_sequences (sequence_name, prefix, next_value, updated_at, version) VALUES
  ('ORDER', 'ORD', 1, '2026-10-01 00:00:00.000000', 0),
  ('INVOICE', 'INV', 1, '2026-10-01 00:00:00.000000', 0),
  ('PAYMENT', 'PAY', 1, '2026-10-01 00:00:00.000000', 0),
  ('COMPLAINT', 'CMP', 1, '2026-10-01 00:00:00.000000', 0),
  ('PARKING', 'PKT', 1, '2026-10-01 00:00:00.000000', 0),
  ('EMPLOYEE', 'EMP', 4, '2026-10-01 00:00:00.000000', 0),
  ('CHECKOUT', 'CHK', 1, '2026-10-01 00:00:00.000000', 0);
