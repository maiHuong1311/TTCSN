-- Two additional samples to fill two catalog pages of ten products. Preserve existing IDs/data.

INSERT INTO product(category_id,product_name,description,brand,material,gender,price,sale_price,status,created_at,updated_at) VALUES (1,'Áo cotton cổ thuyền xếp ly eo','Áo dài tay cổ thuyền với chi tiết xếp ly eo tạo điểm nhấn nhẹ nhàng. Dễ phối cùng quần suông hoặc chân váy khi đi làm và dạo phố.','FORME','Vải cotton','Female',379000,339000,'Active','2026-10-18 00:00:00','2026-10-18 00:00:00');
SET @new_product_id = LAST_INSERT_ID();
INSERT INTO product_image(product_id,image_url,is_primary) VALUES (@new_product_id,'/assets/photos/collection-19-color-1.jpg',TRUE);
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'S','Nâu kẻ',15,'FRM-A26-19-C1-S','/assets/photos/collection-19-color-1.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'M','Nâu kẻ',15,'FRM-A26-19-C1-M','/assets/photos/collection-19-color-1.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'L','Nâu kẻ',15,'FRM-A26-19-C1-L','/assets/photos/collection-19-color-1.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'XL','Nâu kẻ',15,'FRM-A26-19-C1-XL','/assets/photos/collection-19-color-1.jpg');
INSERT INTO product_image(product_id,image_url,is_primary) VALUES (@new_product_id,'/assets/photos/collection-19-color-2.jpg',FALSE);
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'S','Xanh kẻ',15,'FRM-A26-19-C2-S','/assets/photos/collection-19-color-2.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'M','Xanh kẻ',15,'FRM-A26-19-C2-M','/assets/photos/collection-19-color-2.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'L','Xanh kẻ',15,'FRM-A26-19-C2-L','/assets/photos/collection-19-color-2.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'XL','Xanh kẻ',15,'FRM-A26-19-C2-XL','/assets/photos/collection-19-color-2.jpg');

INSERT INTO product(category_id,product_name,description,brand,material,gender,price,sale_price,status,created_at,updated_at) VALUES (3,'Chân váy kaki dáng suông xẻ sau','Chân váy dáng suông với thiết kế cạp kiểu và đường xẻ sau. Phối cùng áo sơ mi hoặc áo polo cho trang phục công sở thanh lịch.','FORME','Vải kaki','Female',499000,NULL,'Active','2026-10-18 00:00:00','2026-10-18 00:00:00');
SET @new_product_id = LAST_INSERT_ID();
INSERT INTO product_image(product_id,image_url,is_primary) VALUES (@new_product_id,'/assets/photos/collection-20-color-1.jpg',TRUE);
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'S','Be',15,'FRM-A26-20-C1-S','/assets/photos/collection-20-color-1.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'M','Be',15,'FRM-A26-20-C1-M','/assets/photos/collection-20-color-1.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'L','Be',15,'FRM-A26-20-C1-L','/assets/photos/collection-20-color-1.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'XL','Be',15,'FRM-A26-20-C1-XL','/assets/photos/collection-20-color-1.jpg');
INSERT INTO product_image(product_id,image_url,is_primary) VALUES (@new_product_id,'/assets/photos/collection-20-color-2.jpg',FALSE);
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'S','Ghi đậm',15,'FRM-A26-20-C2-S','/assets/photos/collection-20-color-2.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'M','Ghi đậm',15,'FRM-A26-20-C2-M','/assets/photos/collection-20-color-2.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'L','Ghi đậm',15,'FRM-A26-20-C2-L','/assets/photos/collection-20-color-2.jpg');
INSERT INTO product_variant(product_id,size,color,stock_quantity,sku,image_url) VALUES (@new_product_id,'XL','Ghi đậm',15,'FRM-A26-20-C2-XL','/assets/photos/collection-20-color-2.jpg');
