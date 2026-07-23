ALTER TABLE restaurant_tables ADD COLUMN status_changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

CREATE TABLE operational_timeouts (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id BIGINT NOT NULL,
    reservation_id BIGINT REFERENCES reservations(id),
    table_id BIGINT REFERENCES restaurant_tables(id),
    title VARCHAR(180) NOT NULL,
    details VARCHAR(1200) NOT NULL,
    deadline_at TIMESTAMPTZ NOT NULL,
    opened_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    resolution_note VARCHAR(400),
    dedupe_key VARCHAR(220) NOT NULL UNIQUE
);
CREATE INDEX idx_operational_timeouts_work_queue
    ON operational_timeouts(status, severity, opened_at DESC);
