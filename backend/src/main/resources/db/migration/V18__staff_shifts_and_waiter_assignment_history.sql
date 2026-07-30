CREATE TABLE staff_shifts (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT,
    staff_id BIGINT NOT NULL REFERENCES app_users(id),
    staff_name VARCHAR(120) NOT NULL,
    staff_email VARCHAR(180) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    actual_started_at TIMESTAMPTZ,
    actual_ended_at TIMESTAMPTZ,
    created_by VARCHAR(180) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_staff_shift_window ON staff_shifts(status, starts_at, ends_at);
CREATE INDEX idx_staff_shift_staff ON staff_shifts(staff_id, starts_at DESC);

CREATE TABLE waiter_assignment_events (
    id BIGSERIAL PRIMARY KEY,
    service_session_id BIGINT NOT NULL REFERENCES service_sessions(id),
    reservation_id BIGINT NOT NULL REFERENCES reservations(id),
    action VARCHAR(20) NOT NULL,
    from_staff_id BIGINT,
    from_staff_name VARCHAR(120),
    to_staff_id BIGINT,
    to_staff_name VARCHAR(120),
    actor VARCHAR(180) NOT NULL,
    reason VARCHAR(400) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_waiter_event_session ON waiter_assignment_events(service_session_id, created_at DESC);
CREATE INDEX idx_waiter_event_created ON waiter_assignment_events(created_at DESC);
