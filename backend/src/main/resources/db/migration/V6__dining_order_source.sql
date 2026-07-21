ALTER TABLE dining_orders ADD COLUMN source VARCHAR(30) NOT NULL DEFAULT 'TABLE_ORDER';

CREATE INDEX idx_dining_orders_session_source ON dining_orders(service_session_id, source);
