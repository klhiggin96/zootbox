-- Migration 002: Seed 10 coil positions (A1-J1)
-- Each coil represents one physical motor/row in the vending machine

-- Insert 10 coils (one per row)
INSERT OR IGNORE INTO coils (id, inventory, status, version) VALUES ('A1', 10, 'available', 1);
INSERT OR IGNORE INTO coils (id, inventory, status, version) VALUES ('B1', 10, 'available', 1);
INSERT OR IGNORE INTO coils (id, inventory, status, version) VALUES ('C1', 10, 'available', 1);
INSERT OR IGNORE INTO coils (id, inventory, status, version) VALUES ('D1', 10, 'available', 1);
INSERT OR IGNORE INTO coils (id, inventory, status, version) VALUES ('E1', 10, 'available', 1);
INSERT OR IGNORE INTO coils (id, inventory, status, version) VALUES ('F1', 10, 'available', 1);
INSERT OR IGNORE INTO coils (id, inventory, status, version) VALUES ('G1', 10, 'available', 1);
INSERT OR IGNORE INTO coils (id, inventory, status, version) VALUES ('H1', 10, 'available', 1);
INSERT OR IGNORE INTO coils (id, inventory, status, version) VALUES ('I1', 10, 'available', 1);
INSERT OR IGNORE INTO coils (id, inventory, status, version) VALUES ('J1', 10, 'available', 1);

INSERT OR IGNORE INTO migrations (version) VALUES ('002_seed_coils');
