-- Translate only the original sample values; preserve customer/admin edits and API enum codes.
UPDATE category SET category_name='Áo', description='Trang phục thiết yếu mỗi ngày' WHERE category_id=1 AND category_name='Tops';
UPDATE category SET category_name='Quần', description='Nền tảng cho phong cách của bạn' WHERE category_id=2 AND category_name='Bottoms';
UPDATE category SET category_name='Váy và đầm', description='Kiểu dáng thoải mái, thanh lịch' WHERE category_id=3 AND category_name='Dresses';
UPDATE category SET category_name='Phụ kiện', description='Điểm nhấn hoàn thiện trang phục' WHERE category_id=4 AND category_name='Accessories';
UPDATE product SET product_name='Áo sơ mi linen dáng rộng' WHERE product_id=1 AND product_name='Linen relaxed shirt';
UPDATE product SET product_name='Quần ống rộng thường ngày' WHERE product_id=2 AND product_name='Everyday wide-leg trousers';
UPDATE product SET product_name='Đầm midi dáng mềm mại' WHERE product_id=3 AND product_name='Soft form midi dress';
UPDATE product SET product_name='Áo thun cotton cơ bản' WHERE product_id=4 AND product_name='Essential cotton tee';
UPDATE product SET product_name='Quần jeans dáng đứng' WHERE product_id=5 AND product_name='Straight fit denim';
UPDATE product SET product_name='Túi tote vải canvas' WHERE product_id=6 AND product_name='Everyday canvas tote';
UPDATE product SET product_name='Áo cardigan dệt nổi' WHERE product_id=7 AND product_name='Textured knit cardigan';
UPDATE product SET product_name='Quần short thanh lịch' WHERE product_id=8 AND product_name='Weekend tailored shorts';
UPDATE product SET description='Thiết kế thoải mái cho mọi hoạt động thường ngày, dễ dàng kết hợp cùng nhiều phong cách. Vui lòng chọn kích cỡ và màu sắc còn hàng bên dưới.' WHERE product_id BETWEEN 1 AND 8 AND description='Made for everyday movement. A versatile wardrobe staple with a comfortable fit. See available sizes and colors below.';
UPDATE product SET material=CASE material WHEN 'Linen blend' THEN 'Linen pha' WHEN 'Cotton twill' THEN 'Cotton dệt chéo' WHEN 'Viscose blend' THEN 'Viscose pha' WHEN 'Organic cotton' THEN 'Cotton hữu cơ' WHEN 'Cotton denim' THEN 'Vải denim cotton' WHEN 'Cotton canvas' THEN 'Vải canvas cotton' WHEN 'Cotton knit' THEN 'Cotton dệt kim' WHEN 'Cotton linen' THEN 'Cotton pha linen' ELSE material END WHERE product_id BETWEEN 1 AND 8;
UPDATE product_variant SET color='Màu cát' WHERE product_id BETWEEN 1 AND 8 AND color='Sand';
UPDATE product_variant SET color='Xanh ô liu' WHERE product_id BETWEEN 1 AND 8 AND color='Olive';
UPDATE app_user SET full_name='Quản trị viên' WHERE full_name='Local Administrator' AND role='Admin';
