ALTER TABLE app_users ADD COLUMN phone VARCHAR(20);

UPDATE menu_items
SET image_url='https://images.unsplash.com/photo-1518977676601-b53f82aba655?auto=format&fit=crop&w=1000&q=80'
WHERE name='Khoai lang mật nướng';

UPDATE menu_items SET available=FALSE WHERE category='Thực đơn đoàn';

INSERT INTO menu_items(name,category,price,description,image_url,featured,available) VALUES
('Cá tầm rang muối','Đặc sản',360000,'Cá tầm Sa Pa rang muối giòn thơm, dùng cùng rau răm.','https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?auto=format&fit=crop&w=1000&q=80',TRUE,TRUE),
('Gà đen hấp lá chanh','Đặc sản',300000,'Gà đen bản hấp mềm cùng lá chanh và gia vị vùng cao.','https://images.unsplash.com/photo-1532550907401-a500c9a57435?auto=format&fit=crop&w=1000&q=80',FALSE,TRUE),
('Bánh ngô Sa Pa','Khai vị',60000,'Bánh ngô vàng thơm, áp chảo nhẹ và dùng nóng.','https://images.unsplash.com/photo-1555507036-ab1f4038808a?auto=format&fit=crop&w=1000&q=80',FALSE,TRUE);
