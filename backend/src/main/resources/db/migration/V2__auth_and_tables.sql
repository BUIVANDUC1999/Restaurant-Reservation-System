CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE restaurant_tables (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    floor VARCHAR(30) NOT NULL,
    area VARCHAR(80) NOT NULL,
    seats INTEGER NOT NULL CHECK (seats > 0),
    status VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_tables_floor_status ON restaurant_tables(floor, status);

