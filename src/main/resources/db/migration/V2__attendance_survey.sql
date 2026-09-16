ALTER TABLE survey_members ADD COLUMN player_id BIGINT NULL;
ALTER TABLE survey_members ADD CONSTRAINT uk_survey_member_player UNIQUE (player_id);

CREATE TABLE attendance_answers (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    target_date DATE NOT NULL,
    status VARCHAR(1) NOT NULL,
    memo VARCHAR(500) NOT NULL DEFAULT '',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_attendance_player_date UNIQUE (player_id, target_date),
    INDEX idx_attendance_date (target_date)
) CHARACTER SET utf8mb4;
