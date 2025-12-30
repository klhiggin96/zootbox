-- Migration: 002_payment_and_products.sql
-- Description: Add product catalog, pricing, cart functionality, and payment tracking
-- Date: 2025-12-30

-- ============================================================================
-- PRODUCTS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS products (
    id TEXT PRIMARY KEY,                    -- Product SKU (e.g., "PRD001")
    name TEXT NOT NULL,                     -- "ZYN CITRUS"
    category TEXT NOT NULL,                 -- "ZyNS", "Extras", "Donations"
    price REAL NOT NULL,                    -- Base price in USD (e.g., 3.50)
    age_restriction INTEGER DEFAULT 0,      -- Minimum age (0 = no restriction, 21 = tobacco)
    image_url TEXT,                         -- Product image path/URL
    video_filename TEXT,                    -- Product video filename
    is_digital BOOLEAN DEFAULT FALSE,       -- Virtual product (e.g., donation)
    active BOOLEAN DEFAULT TRUE,            -- Can be sold (inactive products hidden)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_active ON products(active);

-- ============================================================================
-- PRODUCT-COIL ASSIGNMENTS
-- ============================================================================
CREATE TABLE IF NOT EXISTS product_coil_assignments (
    id TEXT PRIMARY KEY,
    product_id TEXT NOT NULL,
    coil_id TEXT NOT NULL,
    priority INTEGER DEFAULT 0,             -- Vend priority (0 = highest, for multi-coil products)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (coil_id) REFERENCES coils(id) ON DELETE CASCADE,
    UNIQUE (product_id, coil_id)
);

CREATE INDEX idx_product_coil_product ON product_coil_assignments(product_id);
CREATE INDEX idx_product_coil_coil ON product_coil_assignments(coil_id);

-- ============================================================================
-- ENHANCE TRANSACTIONS TABLE WITH PAYMENT DATA
-- ============================================================================
ALTER TABLE transactions ADD COLUMN amount REAL;
ALTER TABLE transactions ADD COLUMN payment_method TEXT;      -- 'card', 'nfc', 'cash', 'free'
ALTER TABLE transactions ADD COLUMN payment_status TEXT;      -- 'pending', 'approved', 'declined', 'refunded'
ALTER TABLE transactions ADD COLUMN currency TEXT DEFAULT 'USD';
ALTER TABLE transactions ADD COLUMN nayax_transaction_id TEXT;  -- From VPOS Touch (different from transaction_id)
ALTER TABLE transactions ADD COLUMN product_id TEXT;

CREATE INDEX idx_transactions_payment_status ON transactions(payment_status);
CREATE INDEX idx_transactions_nayax_id ON transactions(nayax_transaction_id);
CREATE INDEX idx_transactions_product ON transactions(product_id);

-- ============================================================================
-- SHOPPING CART SESSIONS
-- ============================================================================
CREATE TABLE IF NOT EXISTS cart_sessions (
    id TEXT PRIMARY KEY,
    device_id TEXT NOT NULL,                -- Android device identifier (persistent)
    total_amount REAL NOT NULL DEFAULT 0.00,
    item_count INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'active',  -- 'active', 'paid', 'dispensing', 'completed', 'cancelled'
    payment_transaction_id TEXT,            -- Links to Nayax transaction
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_cart_sessions_device ON cart_sessions(device_id);
CREATE INDEX idx_cart_sessions_status ON cart_sessions(status);

-- ============================================================================
-- CART ITEMS
-- ============================================================================
CREATE TABLE IF NOT EXISTS cart_items (
    id TEXT PRIMARY KEY,
    cart_session_id TEXT NOT NULL,
    product_id TEXT NOT NULL,
    coil_id TEXT,                           -- Reserved coil for this item
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price REAL NOT NULL,               -- Price at time of cart addition (frozen)
    FOREIGN KEY (cart_session_id) REFERENCES cart_sessions(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    FOREIGN KEY (coil_id) REFERENCES coils(id) ON DELETE SET NULL
);

CREATE INDEX idx_cart_items_session ON cart_items(cart_session_id);
CREATE INDEX idx_cart_items_product ON cart_items(product_id);

-- ============================================================================
-- SEED INITIAL PRODUCTS (11 products from ProductGridActivity)
-- ============================================================================

-- ZyNS Products (age restricted, 21+)
INSERT INTO products (id, name, category, price, age_restriction, is_digital, active) VALUES
('PRD001', 'ZYN CITRUS', 'ZyNS', 3.50, 21, FALSE, TRUE),
('PRD002', 'ZYN COOL MINT', 'ZyNS', 3.50, 21, FALSE, TRUE),
('PRD003', 'ZYN WINTERGREEN', 'ZyNS', 3.50, 21, FALSE, TRUE),
('PRD004', 'ZYN SMOOTH', 'ZyNS', 3.50, 21, FALSE, TRUE),
('PRD005', 'ZYN PEPPERMINT', 'ZyNS', 3.50, 21, FALSE, TRUE),
('PRD006', 'ZYN CINNAMON', 'ZyNS', 3.50, 21, FALSE, TRUE),
('PRD007', 'ZYN COFFEE', 'ZyNS', 3.50, 21, FALSE, TRUE),
('PRD008', 'ZYN SPEARMINT', 'ZyNS', 3.50, 21, FALSE, TRUE);

-- Extras (no age restriction)
INSERT INTO products (id, name, category, price, age_restriction, is_digital, active) VALUES
('PRD009', 'BOTTLE WATER', 'Extras', 1.50, 0, FALSE, TRUE),
('PRD010', 'CANDY', 'Extras', 1.00, 0, FALSE, TRUE);

-- Donations (digital/virtual products)
INSERT INTO products (id, name, category, price, age_restriction, is_digital, active) VALUES
('PRD011', 'TIP DONATION', 'Donations', 0.50, 0, TRUE, TRUE);

-- ============================================================================
-- ASSIGN PRODUCTS TO COILS (A1-J1)
-- ============================================================================
-- Assuming 10 coils (A1-J1) for 11 products
-- Coil A1: ZYN CITRUS
INSERT INTO product_coil_assignments (id, product_id, coil_id, priority) VALUES
('ASSIGN001', 'PRD001', 'A1', 0);

-- Coil B1: ZYN COOL MINT
INSERT INTO product_coil_assignments (id, product_id, coil_id, priority) VALUES
('ASSIGN002', 'PRD002', 'B1', 0);

-- Coil C1: ZYN WINTERGREEN
INSERT INTO product_coil_assignments (id, product_id, coil_id, priority) VALUES
('ASSIGN003', 'PRD003', 'C1', 0);

-- Coil D1: ZYN SMOOTH
INSERT INTO product_coil_assignments (id, product_id, coil_id, priority) VALUES
('ASSIGN004', 'PRD004', 'D1', 0);

-- Coil E1: ZYN PEPPERMINT
INSERT INTO product_coil_assignments (id, product_id, coil_id, priority) VALUES
('ASSIGN005', 'PRD005', 'E1', 0);

-- Coil F1: ZYN CINNAMON
INSERT INTO product_coil_assignments (id, product_id, coil_id, priority) VALUES
('ASSIGN006', 'PRD006', 'F1', 0);

-- Coil G1: ZYN COFFEE
INSERT INTO product_coil_assignments (id, product_id, coil_id, priority) VALUES
('ASSIGN007', 'PRD007', 'G1', 0);

-- Coil H1: ZYN SPEARMINT
INSERT INTO product_coil_assignments (id, product_id, coil_id, priority) VALUES
('ASSIGN008', 'PRD008', 'H1', 0);

-- Coil I1: BOTTLE WATER
INSERT INTO product_coil_assignments (id, product_id, coil_id, priority) VALUES
('ASSIGN009', 'PRD009', 'I1', 0);

-- Coil J1: CANDY
INSERT INTO product_coil_assignments (id, product_id, coil_id, priority) VALUES
('ASSIGN010', 'PRD010', 'J1', 0);

-- Note: TIP DONATION (PRD011) is digital/virtual, no coil assignment needed

-- ============================================================================
-- MIGRATION COMPLETE
-- ============================================================================
-- Summary:
-- - Added 4 new tables: products, product_coil_assignments, cart_sessions, cart_items
-- - Enhanced transactions table with 6 new payment-related columns
-- - Seeded 11 products with prices
-- - Assigned 10 products to coils A1-J1
-- - Ready for payment integration!
