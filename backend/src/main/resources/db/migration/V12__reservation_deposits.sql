CREATE TABLE reservation_deposits (
 id BIGSERIAL PRIMARY KEY,
 reservation_id BIGINT NOT NULL UNIQUE REFERENCES reservations(id),
 amount NUMERIC(12,2) NOT NULL CHECK(amount >= 0),
 status VARCHAR(20) NOT NULL,
 method VARCHAR(20),
 paid_at TIMESTAMPTZ,
 provider_order_id VARCHAR(120),
 provider_capture_id VARCHAR(120),
 version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_deposit_provider_order ON reservation_deposits(provider_order_id) WHERE provider_order_id IS NOT NULL;
