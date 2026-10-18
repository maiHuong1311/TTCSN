ALTER TABLE payment DROP CHECK chk_payment_method;
ALTER TABLE payment ADD CONSTRAINT chk_payment_method CHECK (payment_method IN ('COD','VietQR','EWallet','DemoQR'));
ALTER TABLE payment ADD COLUMN demo_token_hash VARCHAR(64) UNIQUE, ADD COLUMN demo_expires_at TIMESTAMP(6);
