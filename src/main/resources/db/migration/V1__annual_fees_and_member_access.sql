-- Existing fee rows remain untouched as the pre-migration archive.
CREATE TABLE annual_fees (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    fiscal_year INT NOT NULL,
    amount INT NOT NULL DEFAULT 0,
    paid_amount INT NOT NULL DEFAULT 0,
    comment VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uq_annual_fee UNIQUE (player_id, fiscal_year),
    CONSTRAINT chk_fee_amount CHECK (amount >= 0 AND paid_amount >= 0 AND paid_amount <= amount)
);
INSERT INTO annual_fees (player_id, fiscal_year, amount, paid_amount, comment, created_at, updated_at)
SELECT player_id, 2026, amount, CASE WHEN paid_flg = 1 THEN amount ELSE 0 END,
       comment, COALESCE(created_at, CURRENT_TIMESTAMP), COALESCE(updated_at, CURRENT_TIMESTAMP)
FROM fee;

ALTER TABLE news ADD COLUMN audience VARCHAR(16) NOT NULL DEFAULT 'PUBLIC';

CREATE TABLE login_credentials (
    login_id VARCHAR(64) NOT NULL PRIMARY KEY,
    code_hash VARCHAR(100) NOT NULL,
    role VARCHAR(16) NOT NULL,
    member_id BIGINT,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE security_state (
    id INT NOT NULL PRIMARY KEY,
    generation BIGINT NOT NULL DEFAULT 0
);
INSERT INTO security_state (id, generation) VALUES (1, 0);
