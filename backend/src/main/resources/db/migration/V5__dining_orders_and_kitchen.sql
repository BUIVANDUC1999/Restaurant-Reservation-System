CREATE TABLE dining_orders (
    id BIGSERIAL PRIMARY KEY,
    service_session_id BIGINT NOT NULL REFERENCES service_sessions(id),
    status VARCHAR(30) NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE dining_order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES dining_orders(id),
    menu_item_id BIGINT NOT NULL REFERENCES menu_items(id),
    item_name_snapshot VARCHAR(200) NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_dining_orders_session ON dining_orders(service_session_id);
CREATE INDEX idx_dining_orders_status_created ON dining_orders(status, created_at);
CREATE INDEX idx_dining_order_items_order ON dining_order_items(order_id);
