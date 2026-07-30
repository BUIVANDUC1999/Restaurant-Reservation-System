ALTER TABLE service_sessions ADD COLUMN assigned_staff_id BIGINT;
ALTER TABLE service_sessions ADD COLUMN assigned_staff_name VARCHAR(120);
ALTER TABLE service_sessions ADD COLUMN assigned_staff_email VARCHAR(180);
ALTER TABLE service_sessions ADD COLUMN assigned_by VARCHAR(180);
ALTER TABLE service_sessions ADD COLUMN assigned_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_service_session_assigned_staff
    ON service_sessions(assigned_staff_id, status);
