CREATE TABLE reservation_items (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL REFERENCES reservations(id),
    menu_item_id BIGINT NOT NULL REFERENCES menu_items(id),
    item_name_snapshot VARCHAR(160) NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED'
);

CREATE INDEX idx_reservation_items_reservation ON reservation_items(reservation_id);

INSERT INTO menu_items(name, category, price, description, image_url, featured, available) VALUES
('Gà đen nướng mật ong', 'Đồ nướng', 320000, 'Gà đen bản nướng mật ong rừng, da giòn và thịt ngọt.', 'https://images.unsplash.com/photo-1532550907401-a500c9a57435?auto=format&fit=crop&w=1000&q=80', TRUE, TRUE),
('Lợn bản quay mắc mật', 'Đặc sản', 350000, 'Lợn bản quay giòn bì cùng lá mắc mật thơm đặc trưng.', 'https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=1000&q=80', TRUE, TRUE),
('Trâu gác bếp', 'Đặc sản', 280000, 'Thịt trâu hun khói bếp củi, xé sợi chấm chẩm chéo.', 'https://images.unsplash.com/photo-1529692236671-f1f6cf9683ba?auto=format&fit=crop&w=1000&q=80', TRUE, TRUE),
('Cá hồi nướng giấy bạc', 'Cá hồi', 420000, 'Cá hồi Sa Pa nướng cùng nấm, rau thơm và sốt bơ tỏi.', 'https://images.unsplash.com/photo-1467003909585-2f8a72700288?auto=format&fit=crop&w=1000&q=80', TRUE, TRUE),
('Gỏi cá hồi Sa Pa', 'Cá hồi', 260000, 'Cá hồi tươi thái lát dùng cùng rau bản và mù tạt.', 'https://images.unsplash.com/photo-1579871494447-9811cf80d66c?auto=format&fit=crop&w=1000&q=80', FALSE, TRUE),
('Lẩu thắng cố', 'Lẩu', 720000, 'Nồi lẩu thắng cố đậm vị dành cho nhóm 6 đến 8 khách.', 'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=1000&q=80', TRUE, TRUE),
('Lẩu gà đen thuốc bắc', 'Lẩu', 620000, 'Gà đen hầm thảo mộc, táo đỏ và rau nấm vùng cao.', 'https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=1000&q=80', FALSE, TRUE),
('Lẩu cá tầm Sa Pa', 'Lẩu', 780000, 'Cá tầm tươi, nước dùng chua thanh và rau theo mùa.', 'https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=1000&q=80', TRUE, TRUE),
('Xiên nướng Tây Bắc', 'Đồ nướng', 150000, 'Thịt xiên ướp mắc khén, nướng than hoa thơm lừng.', 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=1000&q=80', FALSE, TRUE),
('Sườn nướng mắc khén', 'Đồ nướng', 260000, 'Sườn non tẩm mắc khén và hạt dổi, nướng chậm.', 'https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=1000&q=80', FALSE, TRUE),
('Ba chỉ nướng lá mắc mật', 'Đồ nướng', 220000, 'Ba chỉ lợn bản cuốn lá mắc mật, dùng cùng chẩm chéo.', 'https://images.unsplash.com/photo-1529193591184-b1d58069ecdd?auto=format&fit=crop&w=1000&q=80', FALSE, TRUE),
('Cơm lam ống tre', 'Món hàng ngày', 65000, 'Gạo nếp nương nướng trong ống tre, dẻo thơm tự nhiên.', 'https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=1000&q=80', TRUE, TRUE),
('Xôi ngũ sắc', 'Món hàng ngày', 85000, 'Xôi nếp nương nhuộm màu tự nhiên từ lá rừng.', 'https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=1000&q=80', FALSE, TRUE),
('Rau cải mèo xào tỏi', 'Món hàng ngày', 80000, 'Cải mèo giòn ngọt xào nhanh với tỏi thơm.', 'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=1000&q=80', FALSE, TRUE),
('Nấm hương Sa Pa xào', 'Món hàng ngày', 110000, 'Nấm hương bản địa xào rau củ, vị thanh nhẹ.', 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=1000&q=80', FALSE, TRUE),
('Canh rau rừng thịt băm', 'Món hàng ngày', 95000, 'Rau rừng theo mùa nấu cùng thịt băm ngọt nước.', 'https://images.unsplash.com/photo-1547592166-23ac45744acd?auto=format&fit=crop&w=1000&q=80', FALSE, TRUE),
('Mầm đá xào bò', 'Đặc sản', 180000, 'Mầm đá Sa Pa xào thịt bò mềm và tỏi bản.', 'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=1000&q=80', FALSE, TRUE),
('Bò cuốn cải mèo', 'Đặc sản', 240000, 'Bò nướng cuốn cải mèo, rau thơm và nước chấm Tây Bắc.', 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1000&q=80', FALSE, TRUE),
('Nộm rau dớn', 'Khai vị', 95000, 'Rau dớn trộn lạc rang, chanh và gia vị vùng cao.', 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=1000&q=80', FALSE, TRUE),
('Nem cá hồi', 'Khai vị', 160000, 'Nem chiên nhân cá hồi Sa Pa, dùng cùng rau sống.', 'https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=1000&q=80', FALSE, TRUE),
('Khoai lang mật nướng', 'Khai vị', 70000, 'Khoai mật nướng than, thơm mềm và ngọt tự nhiên.', 'https://images.unsplash.com/photo-1596097635121-14b63b7a0c19?auto=format&fit=crop&w=1000&q=80', FALSE, TRUE),
('Thực đơn đoàn 6 khách', 'Thực đơn đoàn', 900000, 'Mâm 6 khách gồm 7 món đặc sản và món tráng miệng.', 'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=1000&q=80', FALSE, TRUE),
('Thực đơn đoàn 10 khách', 'Thực đơn đoàn', 1800000, 'Mâm tiệc 10 khách với cá hồi, lợn bản và rau Sa Pa.', 'https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?auto=format&fit=crop&w=1000&q=80', TRUE, TRUE),
('Chè ngô nếp Sa Pa', 'Tráng miệng', 55000, 'Chè ngô nếp dẻo thơm, vị ngọt nhẹ dùng sau bữa ăn.', 'https://images.unsplash.com/photo-1551024506-0bccd828d307?auto=format&fit=crop&w=1000&q=80', FALSE, TRUE);

