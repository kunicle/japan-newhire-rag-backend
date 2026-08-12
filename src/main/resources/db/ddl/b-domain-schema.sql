-- B domain schema for MySQL 8.x
-- Review and execute manually. This file is not a Flyway migration.
-- Tables are added only after their schema is confirmed.

USE japan_newhire_rag;

CREATE TABLE ai_model (
    ai_model_id BIGINT NOT NULL AUTO_INCREMENT,
    provider_name VARCHAR(100) NOT NULL,
    model_name VARCHAR(150) NOT NULL,
    model_type VARCHAR(20) NOT NULL,
    embedding_dimension INT NULL,
    model_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_ai_model
        PRIMARY KEY (ai_model_id),
    CONSTRAINT uk_ai_model_name
        UNIQUE (provider_name, model_name, model_type),
    CONSTRAINT ck_ai_model_dimension
        CHECK (embedding_dimension IS NULL OR embedding_dimension > 0),
    INDEX idx_ai_model_type_status (model_type, model_status, is_default)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE document_chunk (
    document_chunk_id BIGINT NOT NULL AUTO_INCREMENT,
    document_version_id BIGINT NOT NULL,
    chunk_sequence INT NOT NULL,
    article_number VARCHAR(50) NULL,
    article_title VARCHAR(200) NULL,
    chunk_content TEXT NOT NULL,
    token_count INT NULL,
    chunk_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_document_chunk
        PRIMARY KEY (document_chunk_id),
    CONSTRAINT fk_document_chunk_version
        FOREIGN KEY (document_version_id) REFERENCES document_version (document_version_id)
        ON DELETE CASCADE,
    CONSTRAINT uk_document_chunk_sequence
        UNIQUE (document_version_id, chunk_sequence),
    CONSTRAINT ck_document_chunk_sequence
        CHECK (chunk_sequence > 0),
    INDEX idx_document_chunk_version_status (document_version_id, chunk_status),
    INDEX idx_document_chunk_article (document_version_id, article_number)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE chunk_embedding (
    chunk_embedding_id BIGINT NOT NULL AUTO_INCREMENT,
    document_chunk_id BIGINT NOT NULL,
    ai_model_id BIGINT NOT NULL,
    vector_reference VARCHAR(500) NOT NULL,
    embedding_dimension INT NOT NULL,
    embedding_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    embedded_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_chunk_embedding
        PRIMARY KEY (chunk_embedding_id),
    CONSTRAINT fk_chunk_embedding_chunk
        FOREIGN KEY (document_chunk_id) REFERENCES document_chunk (document_chunk_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_chunk_embedding_model
        FOREIGN KEY (ai_model_id) REFERENCES ai_model (ai_model_id)
        ON DELETE RESTRICT,
    CONSTRAINT uk_chunk_embedding_model
        UNIQUE (document_chunk_id, ai_model_id),
    CONSTRAINT ck_chunk_embedding_dimension
        CHECK (embedding_dimension > 0),
    INDEX idx_chunk_embedding_status (embedding_status, created_at),
    INDEX idx_chunk_embedding_reference (vector_reference)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

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

CREATE TABLE document_processing_job_detail (
    processing_job_detail_id BIGINT NOT NULL AUTO_INCREMENT,
    document_processing_job_id BIGINT NOT NULL,
    document_chunk_id BIGINT NULL,
    processing_step VARCHAR(30) NOT NULL,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_number INT NOT NULL DEFAULT 1,
    failure_reason VARCHAR(1000) NULL,
    processed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_document_processing_job_detail
        PRIMARY KEY (processing_job_detail_id),
    CONSTRAINT fk_processing_job_detail_job
        FOREIGN KEY (document_processing_job_id)
        REFERENCES document_processing_job (document_processing_job_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_processing_job_detail_chunk
        FOREIGN KEY (document_chunk_id) REFERENCES document_chunk (document_chunk_id)
        ON DELETE SET NULL,
    CONSTRAINT ck_processing_attempt
        CHECK (attempt_number > 0),
    INDEX idx_job_detail_job_step (
        document_processing_job_id,
        processing_step,
        processing_status
    ),
    INDEX idx_job_detail_chunk (document_chunk_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
