UPDATE menu_items
SET preparation_minutes = CASE
    WHEN category IN ('Khai vị', 'Tráng miệng') THEN 5
    WHEN category IN ('Lẩu', 'Đồ nướng', 'Cá hồi') THEN 10
    ELSE 8
END;

ALTER TABLE menu_items ALTER COLUMN preparation_minutes SET DEFAULT 8;
ALTER TABLE dining_order_items ALTER COLUMN preparation_minutes SET DEFAULT 8;
