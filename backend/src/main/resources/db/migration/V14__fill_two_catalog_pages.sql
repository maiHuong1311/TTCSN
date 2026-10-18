-- Four new samples fill two pages of twelve products in the four-column desktop grid.
-- Preserve custom IDs, existing catalog and stock. All prices/stock are local sample values.

INSERT INTO product(category_id,product_name,description,brand,material,gender,price,sale_price,status,created_at,updated_at) VALUES (1,'Áo sơ mi nữ phom rộng tay dơi','Áo sơ mi cổ đức với dáng rộng và tay dơi tạo vẻ nhẹ nhàng. Dễ kết hợp với quần suông hoặc chân váy khi đi làm và dạo phố.','FORME','Vải sơ mi','Female',559000,499000,'Active','2026-10-18 00:00:00','2026-10-18 00:00:00');
SET @new_product_id = LAST_INSERT_ID();
INSERT INTO product_image(product_id,image_url,is_primary) VALUES (@new_product_id,'/assets/photos/collection-21-color-1.jpg',TRUE);
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'S','Tím',15,'FRM-A26-21-C1-S','/assets/photos/collection-21-color-1.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'M','Tím',15,'FRM-A26-21-C1-M','/assets/photos/collection-21-color-1.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'L','Tím',15,'FRM-A26-21-C1-L','/assets/photos/collection-21-color-1.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'XL','Tím',15,'FRM-A26-21-C1-XL','/assets/photos/collection-21-color-1.jpg');
INSERT INTO product_image(product_id,image_url,is_primary) VALUES (@new_product_id,'/assets/photos/collection-21-color-2.jpg',FALSE);
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'S','Vàng',15,'FRM-A26-21-C2-S','/assets/photos/collection-21-color-2.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'M','Vàng',15,'FRM-A26-21-C2-M','/assets/photos/collection-21-color-2.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'L','Vàng',15,'FRM-A26-21-C2-L','/assets/photos/collection-21-color-2.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'XL','Vàng',15,'FRM-A26-21-C2-XL','/assets/photos/collection-21-color-2.jpg');

INSERT INTO product(category_id,product_name,description,brand,material,gender,price,sale_price,status,created_at,updated_at) VALUES (1,'Áo khoác đũi mỏng dáng ngắn','Áo khoác đũi mỏng với phom gọn gàng, phù hợp khoác ngoài áo thun hoặc áo hai dây. Hai gam màu dễ phối cho trang phục thường ngày.','FORME','Vải đũi','Female',599000,NULL,'Active','2026-10-18 00:00:00','2026-10-18 00:00:00');
SET @new_product_id = LAST_INSERT_ID();
INSERT INTO product_image(product_id,image_url,is_primary) VALUES (@new_product_id,'/assets/photos/collection-22-color-1.jpg',TRUE);
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'S','Nâu',15,'FRM-A26-22-C1-S','/assets/photos/collection-22-color-1.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'M','Nâu',15,'FRM-A26-22-C1-M','/assets/photos/collection-22-color-1.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'L','Nâu',15,'FRM-A26-22-C1-L','/assets/photos/collection-22-color-1.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'XL','Nâu',15,'FRM-A26-22-C1-XL','/assets/photos/collection-22-color-1.jpg');
INSERT INTO product_image(product_id,image_url,is_primary) VALUES (@new_product_id,'/assets/photos/collection-22-color-2.jpg',FALSE);
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'S','Trắng gạo',15,'FRM-A26-22-C2-S','/assets/photos/collection-22-color-2.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'M','Trắng gạo',15,'FRM-A26-22-C2-M','/assets/photos/collection-22-color-2.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'L','Trắng gạo',15,'FRM-A26-22-C2-L','/assets/photos/collection-22-color-2.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'XL','Trắng gạo',15,'FRM-A26-22-C2-XL','/assets/photos/collection-22-color-2.jpg');

INSERT INTO product(category_id,product_name,description,brand,material,gender,price,sale_price,status,created_at,updated_at) VALUES (2,'Quần short tweed cạp cao túi chéo','Quần short dáng chữ A với cạp cao và túi chéo tiện dụng. Bề mặt vải tweed họa tiết tạo điểm nhấn khi phối cùng áo len hoặc sơ mi.','FORME','Vải tweed','Female',479000,439000,'Active','2026-10-18 00:00:00','2026-10-18 00:00:00');
SET @new_product_id = LAST_INSERT_ID();
INSERT INTO product_image(product_id,image_url,is_primary) VALUES (@new_product_id,'/assets/photos/collection-23-color-1.jpg',TRUE);
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'S','Họa tiết nâu',15,'FRM-A26-23-C1-S','/assets/photos/collection-23-color-1.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'M','Họa tiết nâu',15,'FRM-A26-23-C1-M','/assets/photos/collection-23-color-1.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'L','Họa tiết nâu',15,'FRM-A26-23-C1-L','/assets/photos/collection-23-color-1.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'XL','Họa tiết nâu',15,'FRM-A26-23-C1-XL','/assets/photos/collection-23-color-1.jpg');
INSERT INTO product_image(product_id,image_url,is_primary) VALUES (@new_product_id,'/assets/photos/collection-23-color-2.jpg',FALSE);
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'S','Họa tiết rêu',15,'FRM-A26-23-C2-S','/assets/photos/collection-23-color-2.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'M','Họa tiết rêu',15,'FRM-A26-23-C2-M','/assets/photos/collection-23-color-2.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'L','Họa tiết rêu',15,'FRM-A26-23-C2-L','/assets/photos/collection-23-color-2.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'XL','Họa tiết rêu',15,'FRM-A26-23-C2-XL','/assets/photos/collection-23-color-2.jpg');

INSERT INTO product(category_id,product_name,description,brand,material,gender,price,sale_price,status,created_at,updated_at) VALUES (3,'Chân váy linen chấm bi phối ren','Chân váy dáng xòe chia tầng với họa tiết chấm bi và chi tiết ren. Có thể kết hợp cùng áo kiểu đơn giản cho những buổi dạo phố hoặc gặp gỡ cuối tuần.','FORME','Vải linen phối ren','Female',629000,579000,'Active','2026-10-18 00:00:00','2026-10-18 00:00:00');
SET @new_product_id = LAST_INSERT_ID();
INSERT INTO product_image(product_id,image_url,is_primary) VALUES (@new_product_id,'/assets/photos/collection-24-color-1.jpg',TRUE);
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'S','Navy',15,'FRM-A26-24-C1-S','/assets/photos/collection-24-color-1.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'M','Navy',15,'FRM-A26-24-C1-M','/assets/photos/collection-24-color-1.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'L','Navy',15,'FRM-A26-24-C1-L','/assets/photos/collection-24-color-1.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'XL','Navy',15,'FRM-A26-24-C1-XL','/assets/photos/collection-24-color-1.jpg');
INSERT INTO product_image(product_id,image_url,is_primary) VALUES (@new_product_id,'/assets/photos/collection-24-color-2.jpg',FALSE);
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'S','Be kaki',15,'FRM-A26-24-C2-S','/assets/photos/collection-24-color-2.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'M','Be kaki',15,'FRM-A26-24-C2-M','/assets/photos/collection-24-color-2.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'L','Be kaki',15,'FRM-A26-24-C2-L','/assets/photos/collection-24-color-2.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'XL','Be kaki',15,'FRM-A26-24-C2-XL','/assets/photos/collection-24-color-2.jpg');
