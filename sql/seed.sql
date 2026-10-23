-- Sample rows for a local mall. Every password_hash is the token __SEED_PASSWORD_HASH__.
-- sql/load.sh replaces that token. This file has no password and no BCrypt string.

SET @ts = '2026-10-01 00:00:00.000000';

INSERT INTO roles (id, name, description, created_at, updated_at, version) VALUES
  (1, 'ADMIN', 'Mall administrator', @ts, @ts, 0),
  (2, 'SHOP_OWNER', 'Approved shop owner', @ts, @ts, 0),
  (3, 'CUSTOMER', 'Shopper', @ts, @ts, 0);

INSERT INTO users (
  id, email, mobile, password_hash, full_name, status, failed_login_count,
  locked_until, last_login_at, created_at, updated_at, version
) VALUES
  (1, 'admin@mall.local', '9000000001', '__SEED_PASSWORD_HASH__', 'Mall Admin', 'ACTIVE', 0, NULL, NULL, @ts, @ts, 0),
  (2, 'owner.fashion@mall.local', '9000000002', '__SEED_PASSWORD_HASH__', 'Meera Iyer', 'ACTIVE', 0, NULL, NULL, @ts, @ts, 0),
  (3, 'owner.food@mall.local', '9000000003', '__SEED_PASSWORD_HASH__', 'Rahul Menon', 'ACTIVE', 0, NULL, NULL, @ts, @ts, 0),
  (4, 'owner.circuit@mall.local', '9000000004', '__SEED_PASSWORD_HASH__', 'Neha Shah', 'ACTIVE', 0, NULL, NULL, @ts, @ts, 0),
  (5, 'owner.pending@mall.local', '9000000005', '__SEED_PASSWORD_HASH__', 'Vikram Das', 'PENDING', 0, NULL, NULL, @ts, @ts, 0),
  (6, 'customer.riya@mall.local', '9000000006', '__SEED_PASSWORD_HASH__', 'Riya Nair', 'ACTIVE', 0, NULL, NULL, @ts, @ts, 0),
  (7, 'customer.arjun@mall.local', '9000000007', '__SEED_PASSWORD_HASH__', 'Arjun Reddy', 'ACTIVE', 0, NULL, NULL, @ts, @ts, 0),
  (8, 'inactive.customer@mall.local', '9000000008', '__SEED_PASSWORD_HASH__', 'Inactive Sample', 'INACTIVE', 0, NULL, NULL, @ts, @ts, 0);

INSERT INTO user_roles (user_id, role_id, assigned_at, assigned_by, created_at, updated_at, version) VALUES
  (1, 1, @ts, NULL, @ts, @ts, 0),
  (2, 2, @ts, 1, @ts, @ts, 0),
  (3, 2, @ts, 1, @ts, @ts, 0),
  (4, 2, @ts, 1, @ts, @ts, 0),
  (5, 2, @ts, 1, @ts, @ts, 0),
  (6, 3, @ts, 1, @ts, @ts, 0),
  (7, 3, @ts, 1, @ts, @ts, 0),
  (8, 3, @ts, 1, @ts, @ts, 0);

INSERT INTO customers (
  id, user_id, date_of_birth, gender, address_line1, address_line2, city, state, postal_code, country,
  created_at, updated_at, version
) VALUES
  (1, 6, NULL, 'FEMALE', '12 Residency Road', NULL, 'Bengaluru', 'Karnataka', '560001', 'India', @ts, @ts, 0),
  (2, 7, NULL, 'MALE', '48 MG Road', NULL, 'Bengaluru', 'Karnataka', '560001', 'India', @ts, @ts, 0),
  (3, 8, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'India', @ts, @ts, 0);

INSERT INTO shop_owners (
  id, user_id, business_name, gst_number, approval_status, approved_by, approved_at, rejection_reason,
  created_at, updated_at, version
) VALUES
  (1, 2, 'Urban Thread Textiles', NULL, 'APPROVED', 1, @ts, NULL, @ts, @ts, 0),
  (2, 3, 'Spice Court Foods', NULL, 'APPROVED', 1, @ts, NULL, @ts, @ts, 0),
  (3, 4, 'Circuit Hub Electronics', NULL, 'APPROVED', 1, @ts, NULL, @ts, @ts, 0),
  (4, 5, 'Pending Tailors', NULL, 'PENDING', NULL, NULL, NULL, @ts, @ts, 0);

INSERT INTO shops (
  id, owner_id, name, slug, description, floor_label, unit_number, contact_phone, contact_email,
  opening_time, closing_time, logo_path, status, created_at, updated_at, version
) VALUES
  (1, 1, 'Urban Thread', 'urban-thread', 'Clothing and accessories', 'L1', '101', '9000000002', 'owner.fashion@mall.local', '10:00:00', '21:30:00', NULL, 'ACTIVE', @ts, @ts, 0),
  (2, 2, 'Spice Court', 'spice-court', 'South Indian meals and coffee', 'L2', '210', '9000000003', 'owner.food@mall.local', '11:00:00', '22:30:00', NULL, 'ACTIVE', @ts, @ts, 0),
  (3, 3, 'Circuit Hub', 'circuit-hub', 'Computer accessories', 'L1', '118', '9000000004', 'owner.circuit@mall.local', '10:30:00', '21:30:00', NULL, 'ACTIVE', @ts, @ts, 0),
  (4, 4, 'Pending Tailors', 'pending-tailors', 'Awaiting approval', 'L3', '301', '9000000005', 'owner.pending@mall.local', '10:00:00', '20:00:00', NULL, 'DRAFT', @ts, @ts, 0);

INSERT INTO categories (id, name, slug, description, parent_id, active, display_order, created_at, updated_at, version) VALUES
  (1, 'Fashion', 'fashion', NULL, NULL, 1, 1, @ts, @ts, 0),
  (2, 'Food', 'food', NULL, NULL, 1, 2, @ts, @ts, 0),
  (3, 'Electronics', 'electronics', NULL, NULL, 1, 3, @ts, @ts, 0),
  (4, 'Books', 'books', NULL, NULL, 1, 4, @ts, @ts, 0),
  (5, 'Beauty', 'beauty', NULL, NULL, 1, 5, @ts, @ts, 0);

INSERT INTO products (
  id, shop_id, category_id, name, slug, sku, description, unit_price, compare_at_price, tax_rate_percent,
  image_path, status, created_at, updated_at, version
) VALUES
  (1, 1, 1, 'Cotton Kurta', 'cotton-kurta', 'UT-KURTA-001', NULL, 1299.00, NULL, 5.00, NULL, 'ACTIVE', @ts, @ts, 0),
  (2, 1, 1, 'Denim Jacket', 'denim-jacket', 'UT-JACKET-001', NULL, 2499.00, NULL, 5.00, NULL, 'ACTIVE', @ts, @ts, 0),
  (3, 1, 1, 'Leather Belt', 'leather-belt', 'UT-BELT-001', NULL, 799.00, NULL, 5.00, NULL, 'ACTIVE', @ts, @ts, 0),
  (4, 1, 1, 'Silk Scarf', 'silk-scarf', 'UT-SCARF-001', NULL, 599.00, NULL, 5.00, NULL, 'ACTIVE', @ts, @ts, 0),
  (5, 2, 2, 'Masala Dosa Meal', 'masala-dosa-meal', 'SC-DOSA-001', NULL, 180.00, NULL, 5.00, NULL, 'ACTIVE', @ts, @ts, 0),
  (6, 2, 2, 'Filter Coffee', 'filter-coffee', 'SC-COFFEE-001', NULL, 60.00, NULL, 5.00, NULL, 'ACTIVE', @ts, @ts, 0),
  (7, 2, 2, 'Veg Thali', 'veg-thali', 'SC-THALI-001', NULL, 250.00, NULL, 5.00, NULL, 'ACTIVE', @ts, @ts, 0),
  (8, 2, 2, 'Gulab Jamun', 'gulab-jamun', 'SC-JAMUN-001', NULL, 90.00, NULL, 5.00, NULL, 'ACTIVE', @ts, @ts, 0),
  (9, 3, 3, 'Wireless Mouse', 'wireless-mouse', 'CH-MOUSE-001', NULL, 899.00, NULL, 5.00, NULL, 'ACTIVE', @ts, @ts, 0),
  (10, 3, 3, 'USB-C Cable', 'usb-c-cable', 'CH-CABLE-001', NULL, 299.00, NULL, 5.00, NULL, 'ACTIVE', @ts, @ts, 0),
  (11, 3, 3, 'Wired Earphones', 'wired-earphones', 'CH-EAR-001', NULL, 1499.00, NULL, 5.00, NULL, 'ACTIVE', @ts, @ts, 0);

INSERT INTO inventory (id, product_id, quantity_on_hand, quantity_reserved, created_at, updated_at, version) VALUES
  (1, 1, 25, 0, @ts, @ts, 0),
  (2, 2, 12, 0, @ts, @ts, 0),
  (3, 3, 40, 0, @ts, @ts, 0),
  (4, 4, 3, 0, @ts, @ts, 0),
  (5, 5, 50, 0, @ts, @ts, 0),
  (6, 6, 80, 0, @ts, @ts, 0),
  (7, 7, 30, 0, @ts, @ts, 0),
  (8, 8, 40, 0, @ts, @ts, 0),
  (9, 9, 20, 0, @ts, @ts, 0),
  (10, 10, 60, 0, @ts, @ts, 0),
  (11, 11, 15, 0, @ts, @ts, 0);

INSERT INTO stock_movements (
  inventory_id, product_id, shop_id, movement_type, quantity_delta, reserved_delta,
  quantity_on_hand_after, quantity_reserved_after, reference_type, reference_id, note, created_by, created_at
) VALUES
  (1, 1, 1, 'OPENING', 25, 0, 25, 0, NULL, NULL, NULL, NULL, @ts),
  (2, 2, 1, 'OPENING', 12, 0, 12, 0, NULL, NULL, NULL, NULL, @ts),
  (3, 3, 1, 'OPENING', 40, 0, 40, 0, NULL, NULL, NULL, NULL, @ts),
  (4, 4, 1, 'OPENING', 3, 0, 3, 0, NULL, NULL, NULL, NULL, @ts),
  (5, 5, 2, 'OPENING', 50, 0, 50, 0, NULL, NULL, NULL, NULL, @ts),
  (6, 6, 2, 'OPENING', 80, 0, 80, 0, NULL, NULL, NULL, NULL, @ts),
  (7, 7, 2, 'OPENING', 30, 0, 30, 0, NULL, NULL, NULL, NULL, @ts),
  (8, 8, 2, 'OPENING', 40, 0, 40, 0, NULL, NULL, NULL, NULL, @ts),
  (9, 9, 3, 'OPENING', 20, 0, 20, 0, NULL, NULL, NULL, NULL, @ts),
  (10, 10, 3, 'OPENING', 60, 0, 60, 0, NULL, NULL, NULL, NULL, @ts),
  (11, 11, 3, 'OPENING', 15, 0, 15, 0, NULL, NULL, NULL, NULL, @ts);

INSERT INTO carts (id, customer_id, status, created_at, updated_at, version) VALUES
  (1, 1, 'OPEN', @ts, @ts, 0),
  (2, 2, 'OPEN', @ts, @ts, 0),
  (3, 3, 'OPEN', @ts, @ts, 0);

INSERT INTO cart_items (cart_id, product_id, quantity, unit_price_snapshot, created_at, updated_at, version) VALUES
  (1, 1, 1, 1299.00, @ts, @ts, 0);

INSERT INTO wishlists (id, customer_id, created_at, updated_at, version) VALUES
  (1, 1, @ts, @ts, 0),
  (2, 2, @ts, @ts, 0),
  (3, 3, @ts, @ts, 0);

INSERT INTO wishlist_items (wishlist_id, product_id, created_at) VALUES
  (2, 10, @ts);

INSERT INTO coupons (
  code, description, discount_type, discount_value, min_order_amount, max_discount_amount,
  usage_limit, usage_count, per_customer_limit, starts_at, ends_at, active, scope, shop_id, category_id,
  created_by, created_at, updated_at, version
) VALUES
  ('WELCOME10', 'Welcome discount', 'PERCENT', 10.00, 500.00, 300.00, 100, 0, 1, '2026-01-01 00:00:00.000000', '2026-12-31 23:59:59.000000', 1, 'MALL', NULL, NULL, 1, @ts, @ts, 0),
  ('SPICE50', 'Spice Court fixed discount', 'FIXED', 50.00, 299.00, NULL, 200, 0, 2, '2026-01-01 00:00:00.000000', '2026-12-31 23:59:59.000000', 1, 'SHOP', 2, NULL, 1, @ts, @ts, 0),
  ('STYLE5', 'Fashion category discount', 'PERCENT', 5.00, 0.00, 200.00, 100, 0, 1, '2026-01-01 00:00:00.000000', '2026-12-31 23:59:59.000000', 1, 'CATEGORY', NULL, 1, 1, @ts, @ts, 0),
  ('OLD20', 'Expired welcome offer', 'PERCENT', 20.00, 0.00, 500.00, 20, 0, 1, '2025-01-01 00:00:00.000000', '2026-01-31 23:59:59.000000', 1, 'MALL', NULL, NULL, 1, @ts, @ts, 0);

INSERT INTO feedback (
  customer_id, source, guest_name, guest_email, subject, message, rating, status, created_at, updated_at, version
) VALUES (
  2, 'PROFILE', NULL, NULL, 'Mall visit',
  'The food court was easy to find and the shops were open.',
  4, 'OPEN', @ts, @ts, 0
);

INSERT INTO notifications (user_id, type, title, body, link_path, read_at, created_at, updated_at, version) VALUES
  (6, 'GENERAL', 'Welcome to the mall', 'Browse the shops and add a product to your cart.', '/products', NULL, @ts, @ts, 0),
  (7, 'GENERAL', 'Welcome to the mall', 'Browse the shops and add a product to your cart.', '/products', NULL, @ts, @ts, 0);

INSERT INTO complaints (
  complaint_number, customer_id, shop_id, order_id, category, subject, description, status,
  assigned_to, resolution_note, resolved_at, created_at, updated_at, version
) VALUES (
  'CMP-20261001-000001', 1, NULL, NULL, 'PARKING', 'Basement A lighting',
  'The light near the car ramp was out during my visit.',
  'OPEN', NULL, NULL, NULL, @ts, @ts, 0
);

UPDATE number_sequences SET next_value = 2, version = 0 WHERE sequence_name = 'COMPLAINT';

INSERT INTO parking_rates (
  id, vehicle_type, grace_minutes, hourly_rate, daily_cap, currency_code, active, created_at, updated_at, version
) VALUES
  (1, 'CAR', 15, 40.00, 200.00, 'INR', 1, @ts, @ts, 0),
  (2, 'BIKE', 10, 15.00, 80.00, 'INR', 1, @ts, @ts, 0);

INSERT INTO parking_slots (id, slot_code, zone, vehicle_type, status, created_at, updated_at, version) VALUES
  (1, 'A-001', 'BASEMENT_A', 'CAR', 'AVAILABLE', @ts, @ts, 0),
  (2, 'A-002', 'BASEMENT_A', 'CAR', 'AVAILABLE', @ts, @ts, 0),
  (3, 'A-003', 'BASEMENT_A', 'CAR', 'OCCUPIED', @ts, @ts, 0),
  (4, 'A-004', 'BASEMENT_A', 'CAR', 'AVAILABLE', @ts, @ts, 0),
  (5, 'A-005', 'BASEMENT_A', 'CAR', 'AVAILABLE', @ts, @ts, 0),
  (6, 'A-006', 'BASEMENT_A', 'CAR', 'AVAILABLE', @ts, @ts, 0),
  (7, 'B-001', 'BASEMENT_B', 'BIKE', 'AVAILABLE', @ts, @ts, 0),
  (8, 'B-002', 'BASEMENT_B', 'BIKE', 'AVAILABLE', @ts, @ts, 0),
  (9, 'B-003', 'BASEMENT_B', 'BIKE', 'AVAILABLE', @ts, @ts, 0),
  (10, 'B-004', 'BASEMENT_B', 'BIKE', 'AVAILABLE', @ts, @ts, 0);

INSERT INTO parking_sessions (
  ticket_number, slot_id, vehicle_type, vehicle_number, entry_at, exit_at, rate_id,
  grace_minutes_snapshot, hourly_rate_snapshot, daily_cap_snapshot, charged_amount, status, recorded_by,
  created_at, updated_at, version
) VALUES (
  'PKT-20261001-000001', 3, 'CAR', 'KA01AB1234', '2026-10-23 03:45:00.000000', NULL, 1,
  15, 40.00, 200.00, NULL, 'OPEN', 1, @ts, @ts, 0
);

UPDATE number_sequences SET next_value = 2, version = 0 WHERE sequence_name = 'PARKING';

INSERT INTO employees (
  id, employee_code, full_name, email, mobile, job_title, department, hired_on, status, monthly_salary,
  created_at, updated_at, version
) VALUES
  (1, 'EMP-000001', 'Anita Rao', NULL, NULL, 'Security Officer', 'SECURITY', '2025-04-01', 'ACTIVE', 28000.00, @ts, @ts, 0),
  (2, 'EMP-000002', 'Joseph Mathew', NULL, NULL, 'Housekeeping Lead', 'HOUSEKEEPING', '2025-06-15', 'ACTIVE', 22000.00, @ts, @ts, 0),
  (3, 'EMP-000003', 'Fatima Khan', NULL, NULL, 'Parking Marshal', 'PARKING', '2024-11-01', 'ACTIVE', 24000.00, @ts, @ts, 0);

INSERT INTO attendance (
  employee_id, work_date, status, check_in, check_out, note, recorded_by, created_at, updated_at, version
) VALUES
  (1, '2026-10-01', 'PRESENT', '09:00:00', '18:00:00', NULL, 1, @ts, @ts, 0),
  (1, '2026-10-02', 'PRESENT', '09:00:00', '18:00:00', NULL, 1, @ts, @ts, 0),
  (1, '2026-10-03', 'PRESENT', '09:00:00', '18:00:00', NULL, 1, @ts, @ts, 0),
  (2, '2026-10-01', 'PRESENT', '09:00:00', '18:00:00', NULL, 1, @ts, @ts, 0),
  (2, '2026-10-02', 'PRESENT', '09:00:00', '18:00:00', NULL, 1, @ts, @ts, 0),
  (2, '2026-10-03', 'PRESENT', '09:00:00', '18:00:00', NULL, 1, @ts, @ts, 0),
  (2, '2026-10-04', 'ABSENT', NULL, NULL, NULL, 1, @ts, @ts, 0),
  (3, '2026-10-01', 'PRESENT', '09:00:00', '18:00:00', NULL, 1, @ts, @ts, 0),
  (3, '2026-10-02', 'PRESENT', '09:00:00', '18:00:00', NULL, 1, @ts, @ts, 0),
  (3, '2026-10-03', 'PRESENT', '09:00:00', '18:00:00', NULL, 1, @ts, @ts, 0);

INSERT INTO salary_records (
  employee_id, `year_month`, base_amount, deduction_amount, net_amount, present_days, absent_days, half_days,
  leave_days, status, paid_at, generated_by, created_at, updated_at, version
) VALUES (
  1, '2026-09', 28000.00, 0.00, 28000.00, 30, 0, 0, 0, 'DRAFT', NULL, 1, @ts, @ts, 0
);

INSERT INTO activity_logs (actor_user_id, action, entity_type, entity_id, summary, ip_address, created_at) VALUES
  (1, 'SETTINGS_UPDATED', 'app_settings', 1, 'Seed data loaded.', NULL, @ts);
