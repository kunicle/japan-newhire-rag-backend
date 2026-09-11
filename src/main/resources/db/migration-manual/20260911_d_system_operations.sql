-- Manual production migration for D system operations. Do not execute full schema scripts.
-- Prerequisites: app_user, ai_model, rag_question and document_processing_job already exist.
-- Run 20260911_d_system_operations_precheck.sql and resolve duplicate notification events manually first.

CREATE TABLE external_api_call_log (
    external_api_call_log_id BIGINT NOT NULL AUTO_INCREMENT,
    ai_model_id BIGINT NOT NULL,
    rag_question_id BIGINT NULL,
    document_processing_job_id BIGINT NULL,
    api_type VARCHAR(20) NOT NULL,
    call_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_number INT NOT NULL DEFAULT 1,
    http_status_code INT NULL,
    error_type VARCHAR(100) NULL,
    error_message VARCHAR(1000) NULL,
    duration_ms INT NULL,
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_external_api_call_log PRIMARY KEY (external_api_call_log_id),
    CONSTRAINT fk_external_api_call_log_model FOREIGN KEY (ai_model_id) REFERENCES ai_model (ai_model_id) ON DELETE RESTRICT,
    CONSTRAINT fk_external_api_call_log_question FOREIGN KEY (rag_question_id) REFERENCES rag_question (rag_question_id) ON DELETE SET NULL,
    CONSTRAINT fk_external_api_call_log_job FOREIGN KEY (document_processing_job_id) REFERENCES document_processing_job (document_processing_job_id) ON DELETE SET NULL,
    CONSTRAINT ck_external_api_call_context CHECK (rag_question_id IS NOT NULL OR document_processing_job_id IS NOT NULL),
    CONSTRAINT ck_external_api_call_attempt CHECK (attempt_number BETWEEN 1 AND 3),
    CONSTRAINT ck_external_api_call_duration CHECK (duration_ms IS NULL OR duration_ms >= 0),
    INDEX idx_external_api_call_status_time (call_status, requested_at),
    INDEX idx_external_api_call_error_time (api_type, error_type, requested_at),
    INDEX idx_external_api_call_question_time (rag_question_id, requested_at),
    INDEX idx_external_api_call_job_time (document_processing_job_id, requested_at)
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
    CONSTRAINT fk_system_error_log_user FOREIGN KEY (app_user_id) REFERENCES app_user (app_user_id) ON DELETE SET NULL,
    CONSTRAINT fk_system_error_log_external_call FOREIGN KEY (external_api_call_log_id) REFERENCES external_api_call_log (external_api_call_log_id) ON DELETE SET NULL,
    CONSTRAINT ck_system_error_log_retry CHECK (retry_count BETWEEN 0 AND 2),
    INDEX idx_system_error_status_time (error_status, occurred_at),
    INDEX idx_system_error_source_type_time (error_source, error_type, occurred_at),
    INDEX idx_system_error_external_call (external_api_call_log_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

ALTER TABLE notification
    ADD CONSTRAINT uk_notification_event
    UNIQUE (app_user_id, notification_type, reference_type, reference_id);
