ALTER TABLE reservations ADD COLUMN reservation_time TIME;
ALTER TABLE reservations ADD COLUMN duration_minutes INTEGER NOT NULL DEFAULT 120;
ALTER TABLE reservations ADD COLUMN hold_expires_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '10 minutes');
ALTER TABLE reservations ADD COLUMN confirmed_at TIMESTAMPTZ;
ALTER TABLE reservations ADD COLUMN checked_in_at TIMESTAMPTZ;
ALTER TABLE reservations ADD COLUMN completed_at TIMESTAMPTZ;
ALTER TABLE reservations ADD COLUMN cancelled_at TIMESTAMPTZ;
ALTER TABLE reservations ADD COLUMN notify_email BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE reservations ADD COLUMN notify_sms BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE reservations SET reservation_time = CASE WHEN time_slot = 'LUNCH' THEN TIME '11:00' ELSE TIME '17:30' END;
ALTER TABLE reservations ALTER COLUMN reservation_time SET NOT NULL;
CREATE INDEX idx_reservation_exact_schedule ON reservations(reservation_date, reservation_time, status);

ALTER TABLE restaurant_tables ADD COLUMN layout_x INTEGER NOT NULL DEFAULT 0;
ALTER TABLE restaurant_tables ADD COLUMN layout_y INTEGER NOT NULL DEFAULT 0;
ALTER TABLE restaurant_tables ADD COLUMN shape VARCHAR(20) NOT NULL DEFAULT 'ROUND';
ALTER TABLE restaurant_tables ADD COLUMN public_token VARCHAR(64);
UPDATE restaurant_tables SET public_token = MD5(RANDOM()::TEXT || id::TEXT || NOW()::TEXT);
ALTER TABLE restaurant_tables ALTER COLUMN public_token SET NOT NULL;
ALTER TABLE restaurant_tables ADD CONSTRAINT uk_restaurant_table_public_token UNIQUE(public_token);
UPDATE restaurant_tables SET floor = 'Tầng trệt';
UPDATE restaurant_tables
SET layout_x = 8 + MOD(CAST(id - 1 AS INTEGER), 6) * 15,
    layout_y = 12 + CAST(FLOOR((id - 1) / 6.0) AS INTEGER) * 23,
    shape = CASE WHEN MOD(CAST(id AS INTEGER), 4) = 0 THEN 'RECTANGLE' ELSE 'ROUND' END;

ALTER TABLE menu_items ADD COLUMN preparation_minutes INTEGER NOT NULL DEFAULT 20;

CREATE TABLE operational_notifications (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT REFERENCES reservations(id),
    type VARCHAR(40) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    recipient VARCHAR(180),
    title VARCHAR(180) NOT NULL,
    message VARCHAR(1200) NOT NULL,
    dedupe_key VARCHAR(180) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    error_message VARCHAR(600)
);
CREATE INDEX idx_operational_notifications_feed ON operational_notifications(channel, created_at DESC);

CREATE TABLE table_service_requests (
    id BIGSERIAL PRIMARY KEY,
    table_id BIGINT NOT NULL REFERENCES restaurant_tables(id),
    service_session_id BIGINT NOT NULL REFERENCES service_sessions(id),
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    note VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL,
    acknowledged_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ
);
CREATE INDEX idx_table_service_requests_status ON table_service_requests(status, created_at DESC);

ALTER TABLE dining_order_items ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED';
ALTER TABLE dining_order_items ADD COLUMN preparation_minutes INTEGER NOT NULL DEFAULT 20;
ALTER TABLE dining_order_items ADD COLUMN estimated_ready_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '20 minutes');
ALTER TABLE dining_order_items ADD COLUMN started_at TIMESTAMPTZ;
ALTER TABLE dining_order_items ADD COLUMN delayed_until TIMESTAMPTZ;
ALTER TABLE dining_order_items ADD COLUMN delay_reason VARCHAR(300);
ALTER TABLE dining_order_items ADD COLUMN ready_at TIMESTAMPTZ;
ALTER TABLE dining_order_items ADD COLUMN served_at TIMESTAMPTZ;
