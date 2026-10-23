ALTER TABLE product ADD COLUMN size_chart_code VARCHAR(32);
ALTER TABLE product ADD CONSTRAINT chk_product_size_chart CHECK (size_chart_code IS NULL OR size_chart_code IN ('SIZE_AO_NU','SIZE_QUAN_NU','SIZE_CHAN_VAY','SIZE_DAM'));
UPDATE product SET size_chart_code=CASE
    WHEN category_id=1 THEN 'SIZE_AO_NU'
    WHEN category_id=2 THEN 'SIZE_QUAN_NU'
    WHEN category_id=3 AND LOWER(product_name) LIKE '%chân váy%' THEN 'SIZE_CHAN_VAY'
    WHEN category_id=3 THEN 'SIZE_DAM'
    ELSE NULL END
WHERE gender='Female';
ALTER TABLE product_interaction MODIFY product_id BIGINT NULL;
ALTER TABLE product_interaction ADD COLUMN query_text VARCHAR(100);
CREATE INDEX idx_interaction_user_event ON product_interaction(user_id,event_type,created_at);
