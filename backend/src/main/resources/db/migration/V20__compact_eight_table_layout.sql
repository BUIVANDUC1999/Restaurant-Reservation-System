-- Một tầng, tổng 8 bàn: B07 và B08 ở trung tâm, 6 bàn còn lại bao quanh.
UPDATE restaurant_tables
SET active = FALSE,
    status = 'INACTIVE'
WHERE code NOT IN ('B01', 'B02', 'B03', 'B04', 'B05', 'B06', 'B07', 'B08');

UPDATE restaurant_tables
SET active = TRUE,
    status = CASE WHEN status = 'INACTIVE' THEN 'AVAILABLE' ELSE status END,
    floor = 'Tầng trệt',
    name = CASE code
        WHEN 'B07' THEN 'Bàn trung tâm 1'
        WHEN 'B08' THEN 'Bàn trung tâm 2'
        ELSE 'Bàn ' || CAST(CAST(SUBSTRING(code FROM 2) AS INTEGER) AS VARCHAR)
    END,
    area = CASE WHEN code IN ('B07', 'B08') THEN 'Trung tâm'
                WHEN code IN ('B01', 'B02') THEN 'Cửa sổ'
                ELSE 'Sảnh ngoài' END,
    seats = CASE code WHEN 'B02' THEN 6 WHEN 'B05' THEN 6 WHEN 'B07' THEN 6 WHEN 'B08' THEN 6 ELSE 4 END,
    layout_x = CASE code WHEN 'B01' THEN 18 WHEN 'B02' THEN 50 WHEN 'B03' THEN 82 WHEN 'B04' THEN 18
                         WHEN 'B05' THEN 50 WHEN 'B06' THEN 82 WHEN 'B07' THEN 40 WHEN 'B08' THEN 60 END,
    layout_y = CASE code WHEN 'B01' THEN 15 WHEN 'B02' THEN 10 WHEN 'B03' THEN 15 WHEN 'B04' THEN 75
                         WHEN 'B05' THEN 82 WHEN 'B06' THEN 75 WHEN 'B07' THEN 46 WHEN 'B08' THEN 46 END,
    shape = CASE WHEN code IN ('B02', 'B05', 'B07', 'B08') THEN 'RECTANGLE' ELSE 'ROUND' END
WHERE code IN ('B01', 'B02', 'B03', 'B04', 'B05', 'B06', 'B07', 'B08');
