CREATE TABLE reservation_status_events (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL REFERENCES reservations(id),
    from_status VARCHAR(30) NOT NULL,
    to_status VARCHAR(30) NOT NULL,
    actor VARCHAR(180) NOT NULL,
    reason VARCHAR(400) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_reservation_status_event
    ON reservation_status_events(reservation_id, created_at DESC);
