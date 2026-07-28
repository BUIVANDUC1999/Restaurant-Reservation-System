ALTER TABLE reservations ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'ONLINE';

CREATE TABLE walk_in_visits (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT,
    code VARCHAR(24) NOT NULL UNIQUE,
    customer_name VARCHAR(120) NOT NULL,
    phone VARCHAR(20),
    party_size INTEGER NOT NULL,
    area_preference VARCHAR(100),
    priority VARCHAR(30) NOT NULL,
    priority_reason VARCHAR(300),
    status VARCHAR(30) NOT NULL,
    arrived_at TIMESTAMPTZ NOT NULL,
    quoted_wait_minutes INTEGER NOT NULL,
    expected_seat_at TIMESTAMPTZ NOT NULL,
    offered_at TIMESTAMPTZ,
    offer_expires_at TIMESTAMPTZ,
    seated_at TIMESTAMPTZ,
    payment_requested_at TIMESTAMPTZ,
    cleaning_started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    left_at TIMESTAMPTZ,
    table_id BIGINT REFERENCES restaurant_tables(id),
    reservation_id BIGINT REFERENCES reservations(id),
    call_count INTEGER NOT NULL DEFAULT 0,
    note VARCHAR(500)
);
CREATE INDEX idx_walk_in_work_queue ON walk_in_visits(status, arrived_at);

CREATE TABLE walk_in_events (
    id BIGSERIAL PRIMARY KEY,
    walk_in_visit_id BIGINT NOT NULL REFERENCES walk_in_visits(id),
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    action VARCHAR(60) NOT NULL,
    note VARCHAR(500),
    actor VARCHAR(180) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_walk_in_events_timeline ON walk_in_events(walk_in_visit_id, created_at DESC);
