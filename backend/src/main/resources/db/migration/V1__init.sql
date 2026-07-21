CREATE TABLE menu_items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(80) NOT NULL,
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    description VARCHAR(600),
    image_url VARCHAR(600),
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    available BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(16) NOT NULL UNIQUE,
    customer_name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(160),
    reservation_date DATE NOT NULL,
    time_slot VARCHAR(20) NOT NULL CHECK (time_slot IN ('LUNCH', 'DINNER')),
    party_size INTEGER NOT NULL CHECK (party_size BETWEEN 1 AND 300),
    preferred_floor VARCHAR(30),
    note VARCHAR(1000),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reservation_schedule ON reservations(reservation_date, time_slot, status);
CREATE INDEX idx_reservation_phone ON reservations(phone);

INSERT INTO menu_items(name, category, price, description, image_url, featured, available) VALUES
('Thắng cố A Quỳnh', 'Đặc sản', 220000, 'Hương vị đặc trưng vùng cao, nấu cùng thảo quả và gia vị Tây Bắc.', 'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=1000&q=80', TRUE, TRUE),
('Lẩu cá hồi Sa Pa', 'Lẩu', 650000, 'Cá hồi tươi, nước lẩu chua cay và rau bản địa theo mùa.', 'https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=1000&q=80', TRUE, TRUE),
('Mẹt nướng Tây Bắc', 'Đồ nướng', 480000, 'Tổng hợp thịt nướng, rau củ và chẩm chéo.', 'https://images.unsplash.com/photo-1529692236671-f1f6cf9683ba?auto=format&fit=crop&w=1000&q=80', TRUE, TRUE),
('Ngọn su su xào tỏi', 'Món hàng ngày', 90000, 'Rau su su Sa Pa xanh giòn, xào nhanh cùng tỏi thơm.', 'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=1000&q=80', FALSE, TRUE),
('Cá suối nướng', 'Đặc sản', 180000, 'Cá suối nướng than hoa, ăn kèm rau thơm và chẩm chéo.', 'https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?auto=format&fit=crop&w=1000&q=80', TRUE, TRUE),
('Thực đơn đoàn 8 khách', 'Thực đơn đoàn', 1200000, 'Mâm cơm 8 khách gồm món khai vị, món chính, rau và canh.', 'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=1000&q=80', FALSE, TRUE);

