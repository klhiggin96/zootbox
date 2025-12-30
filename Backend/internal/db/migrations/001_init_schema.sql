-- Migration 001: Initialize database schema
-- Creates all tables for ZootBox Coil-Counter Backend

-- Table 1: coils (10 vending machine positions A1-J1)
CREATE TABLE IF NOT EXISTS coils (
    id TEXT PRIMARY KEY,
    inventory INTEGER NOT NULL DEFAULT 10 CHECK (inventory >= 0 AND inventory <= 10),
    status TEXT NOT NULL DEFAULT 'available' CHECK (status IN ('available', 'jammed')),
    version INTEGER NOT NULL DEFAULT 1,
    link_group_id TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (link_group_id) REFERENCES product_links(link_group_id)
);

CREATE INDEX IF NOT EXISTS idx_coils_link_group ON coils(link_group_id) WHERE link_group_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_coils_low_stock ON coils(inventory) WHERE inventory <= 2;

CREATE TRIGGER IF NOT EXISTS update_coils_timestamp
AFTER UPDATE ON coils
FOR EACH ROW
BEGIN
    UPDATE coils SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- Table 2: transactions (immutable vend log)
CREATE TABLE IF NOT EXISTS transactions (
    id TEXT PRIMARY KEY,
    coil_id TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status TEXT NOT NULL CHECK (status IN ('success', 'jam', 'failed')),
    transaction_id TEXT NOT NULL,
    inventory_before INTEGER NOT NULL,
    inventory_after INTEGER,
    FOREIGN KEY (coil_id) REFERENCES coils(id)
);

CREATE INDEX IF NOT EXISTS idx_transactions_coil ON transactions(coil_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status) WHERE status = 'jam';

-- Table 3: jam_events (hardware failure log)
CREATE TABLE IF NOT EXISTS jam_events (
    id TEXT PRIMARY KEY,
    coil_id TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'resolved')),
    resolved_at TIMESTAMP,
    FOREIGN KEY (coil_id) REFERENCES coils(id)
);

CREATE INDEX IF NOT EXISTS idx_jam_events_open ON jam_events(status, timestamp DESC) WHERE status = 'open';
CREATE INDEX IF NOT EXISTS idx_jam_events_coil ON jam_events(coil_id, timestamp DESC);

-- Table 4: low_stock_alerts (monitoring queue)
CREATE TABLE IF NOT EXISTS low_stock_alerts (
    id TEXT PRIMARY KEY,
    coil_id TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    inventory_at_alert INTEGER NOT NULL CHECK (inventory_at_alert <= 2),
    delivery_status TEXT NOT NULL DEFAULT 'pending' CHECK (delivery_status IN ('pending', 'sent', 'failed')),
    retry_count INTEGER NOT NULL DEFAULT 0,
    delivered_at TIMESTAMP,
    FOREIGN KEY (coil_id) REFERENCES coils(id)
);

CREATE INDEX IF NOT EXISTS idx_alerts_pending ON low_stock_alerts(delivery_status, retry_count) WHERE delivery_status = 'pending';
CREATE UNIQUE INDEX IF NOT EXISTS idx_alerts_unique_pending ON low_stock_alerts(coil_id) WHERE delivery_status = 'pending';

-- Table 5: product_links (multi-coil product configuration)
CREATE TABLE IF NOT EXISTS product_links (
    link_group_id TEXT PRIMARY KEY,
    product_sku TEXT NOT NULL UNIQUE,
    linked_coil_ids TEXT NOT NULL,
    selection_strategy TEXT NOT NULL DEFAULT 'first_available',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_product_links_sku ON product_links(product_sku);

-- Table 6: migrations (track applied migrations)
CREATE TABLE IF NOT EXISTS migrations (
    version TEXT PRIMARY KEY,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT OR IGNORE INTO migrations (version) VALUES ('001_init_schema');
