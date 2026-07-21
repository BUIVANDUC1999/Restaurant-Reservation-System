CREATE TABLE reservation_table_assignments (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL REFERENCES reservations(id),
    table_id BIGINT NOT NULL REFERENCES restaurant_tables(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(reservation_id, table_id)
);

CREATE INDEX idx_assignment_reservation ON reservation_table_assignments(reservation_id);
CREATE INDEX idx_assignment_table ON reservation_table_assignments(table_id);

CREATE TABLE service_sessions (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL UNIQUE REFERENCES reservations(id),
    status VARCHAR(30) NOT NULL,
    opened_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ
);

