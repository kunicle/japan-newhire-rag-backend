-- System operations schema for MySQL 8.x
-- Execute after a-domain-schema.sql because audit_log references app_user.
-- Review and execute manually. This file is not a Flyway migration.

USE japan_newhire_rag;

CREATE TABLE audit_log (
    audit_log_id BIGINT NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT NOT NULL,
    previous_value TEXT NULL,
    changed_value TEXT NULL,
    request_ip VARCHAR(45) NULL,
    request_id VARCHAR(100) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_audit_log PRIMARY KEY (audit_log_id),
    CONSTRAINT fk_audit_log_actor_user
        FOREIGN KEY (actor_user_id) REFERENCES app_user (app_user_id)
        ON DELETE RESTRICT,
    INDEX idx_audit_log_action_time (action_type, created_at),
    INDEX idx_audit_log_actor_time (actor_user_id, created_at),
    INDEX idx_audit_log_target (target_type, target_id, created_at),
    INDEX idx_audit_log_request (request_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
