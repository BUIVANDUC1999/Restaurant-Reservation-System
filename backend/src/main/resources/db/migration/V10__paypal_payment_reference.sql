ALTER TABLE payments ADD COLUMN provider_order_id VARCHAR(80);
ALTER TABLE payments ADD COLUMN provider_capture_id VARCHAR(80);
CREATE UNIQUE INDEX idx_payments_provider_order ON payments(provider_order_id) WHERE provider_order_id IS NOT NULL;
