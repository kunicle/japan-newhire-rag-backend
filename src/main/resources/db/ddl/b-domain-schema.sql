-- B domain schema for MySQL 8.x
-- Review and execute manually. This file is not a Flyway migration.
-- Tables are added only after their schema is confirmed.

USE japan_newhire_rag;

CREATE TABLE document_processing_job (
    document_processing_job_id BIGINT NOT NULL AUTO_INCREMENT,
    document_version_id BIGINT NOT NULL,
    job_type VARCHAR(30) NOT NULL DEFAULT 'FULL_PROCESSING',
    processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_chunk_count INT NOT NULL DEFAULT 0,
    completed_chunk_count INT NOT NULL DEFAULT 0,
    failed_chunk_count INT NOT NULL DEFAULT 0,
    retry_count INT NOT NULL DEFAULT 0,
    failure_reason VARCHAR(1000) NULL,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_document_processing_job
        PRIMARY KEY (document_processing_job_id),
    CONSTRAINT fk_document_processing_job_version
        FOREIGN KEY (document_version_id) REFERENCES document_version (document_version_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_document_processing_job_created_by
        FOREIGN KEY (created_by) REFERENCES app_user (app_user_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_document_job_counts
        CHECK (
            total_chunk_count >= 0
            AND completed_chunk_count >= 0
            AND failed_chunk_count >= 0
        ),
    CONSTRAINT ck_document_job_retry
        CHECK (retry_count BETWEEN 0 AND 2),
    INDEX idx_document_job_version_time (document_version_id, created_at),
    INDEX idx_document_job_status (processing_status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
