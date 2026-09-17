ALTER TABLE schedules ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE survey_answers ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE INDEX idx_survey_answer_event ON survey_answers(survey_event_id);
CREATE TABLE audit_entries (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    occurred_at DATETIME(6) NOT NULL,
    login_id VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL,
    method VARCHAR(8) NOT NULL,
    target VARCHAR(255) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    http_status INT NOT NULL,
    INDEX idx_audit_time (occurred_at)
) CHARACTER SET utf8mb4;
