ALTER TABLE players ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE gear ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE annual_fees ADD COLUMN paid BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE annual_fees SET paid = (amount > 0 AND paid_amount >= amount);
CREATE TABLE site_settings (setting_key VARCHAR(80) PRIMARY KEY, setting_value VARCHAR(255) NOT NULL);
CREATE TABLE attendance_dates (target_date DATE PRIMARY KEY);
