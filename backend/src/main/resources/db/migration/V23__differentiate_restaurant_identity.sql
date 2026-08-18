UPDATE menu_items
SET name = REPLACE(name, 'Sa Pa', 'Tây Bắc'),
    description = REPLACE(description, 'Sa Pa', 'vùng cao')
WHERE name LIKE '%Sa Pa%' OR description LIKE '%Sa Pa%';

UPDATE menu_items
SET name = 'Thắng cố vùng cao'
WHERE name = 'Thắng cố A Quỳnh';
