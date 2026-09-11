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

CREATE TABLE notification (
    notification_id BIGINT NOT NULL AUTO_INCREMENT,
    app_user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    notification_title VARCHAR(200) NOT NULL,
    notification_content TEXT NOT NULL,
    reference_type VARCHAR(50) NULL,
    reference_id BIGINT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_notification PRIMARY KEY (notification_id),
    CONSTRAINT fk_notification_recipient
        FOREIGN KEY (app_user_id) REFERENCES app_user (app_user_id)
        ON DELETE RESTRICT,
    CONSTRAINT uk_notification_event
        UNIQUE (app_user_id, notification_type, reference_type, reference_id),
    CONSTRAINT ck_notification_read_state
        CHECK ((is_read = FALSE AND read_at IS NULL) OR (is_read = TRUE AND read_at IS NOT NULL)),
    INDEX idx_notification_app_user_created
        (app_user_id, created_at, notification_id),
    INDEX idx_notification_app_user_read_created
        (app_user_id, is_read, created_at, notification_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE system_error_log (
    system_error_log_id BIGINT NOT NULL AUTO_INCREMENT,
    app_user_id BIGINT NULL,
    external_api_call_log_id BIGINT NULL,
    error_source VARCHAR(50) NOT NULL,
    error_type VARCHAR(100) NOT NULL,
    error_code VARCHAR(100) NULL,
    error_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    retry_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(2000) NOT NULL,
    occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_system_error_log PRIMARY KEY (system_error_log_id),
    CONSTRAINT fk_system_error_log_user FOREIGN KEY (app_user_id)
        REFERENCES app_user (app_user_id) ON DELETE SET NULL,
    CONSTRAINT fk_system_error_log_external_call FOREIGN KEY (external_api_call_log_id)
        REFERENCES external_api_call_log (external_api_call_log_id) ON DELETE SET NULL,
    CONSTRAINT ck_system_error_log_retry CHECK (retry_count BETWEEN 0 AND 2),
    INDEX idx_system_error_status_time (error_status, occurred_at),
    INDEX idx_system_error_source_type_time (error_source, error_type, occurred_at),
    INDEX idx_system_error_external_call (external_api_call_log_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
