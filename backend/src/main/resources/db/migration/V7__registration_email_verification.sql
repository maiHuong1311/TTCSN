CREATE TABLE registration_email_verification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    sent_at TIMESTAMP(6) NOT NULL,
    window_start TIMESTAMP(6) NOT NULL,
    send_count INT NOT NULL,
    attempts INT NOT NULL,
    consumed BOOLEAN NOT NULL
);
