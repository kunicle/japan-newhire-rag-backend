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

CREATE TABLE document_category (
    document_category_id BIGINT NOT NULL AUTO_INCREMENT,
    category_code VARCHAR(30) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    category_description VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_document_category
        PRIMARY KEY (document_category_id),
    CONSTRAINT uk_document_category_code
        UNIQUE (category_code),
    CONSTRAINT uk_document_category_name
        UNIQUE (category_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE document (
    document_id BIGINT NOT NULL AUTO_INCREMENT,
    document_category_id BIGINT NOT NULL,
    document_name VARCHAR(200) NOT NULL,
    document_description VARCHAR(1000) NULL,
    document_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL,
    deleted_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_document
        PRIMARY KEY (document_id),
    CONSTRAINT fk_document_category
        FOREIGN KEY (document_category_id)
        REFERENCES document_category (document_category_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_document_created_by
        FOREIGN KEY (created_by) REFERENCES app_user (app_user_id)
        ON DELETE RESTRICT,
    INDEX idx_document_category (document_category_id),
    INDEX idx_document_created_by (created_by)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE document_version (
    document_version_id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    version_name VARCHAR(20) NOT NULL,
    effective_date DATE NOT NULL,
    expiration_date DATE NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL DEFAULT 'text/plain',
    publication_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    published_by BIGINT NULL,
    published_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_document_version
        PRIMARY KEY (document_version_id),
    CONSTRAINT fk_document_version_document
        FOREIGN KEY (document_id) REFERENCES document (document_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_document_version_published_by
        FOREIGN KEY (published_by) REFERENCES app_user (app_user_id)
        ON DELETE SET NULL,
    INDEX idx_document_version_document (document_id),
    INDEX idx_document_version_published_by (published_by)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE document_access_rule (
    document_access_rule_id BIGINT NOT NULL AUTO_INCREMENT,
    document_version_id BIGINT NOT NULL,
    minimum_job_grade_id BIGINT NULL,
    access_scope VARCHAR(20) NOT NULL DEFAULT 'ALL',
    condition_operator VARCHAR(10) NOT NULL DEFAULT 'OR',
    is_new_employee_only BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_document_access_rule
        PRIMARY KEY (document_access_rule_id),
    CONSTRAINT uk_document_access_rule_version
        UNIQUE (document_version_id),
    CONSTRAINT fk_document_access_rule_version
        FOREIGN KEY (document_version_id) REFERENCES document_version (document_version_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_document_access_rule_minimum_job_grade
        FOREIGN KEY (minimum_job_grade_id) REFERENCES job_grade (job_grade_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_document_access_rule_created_by
        FOREIGN KEY (created_by) REFERENCES app_user (app_user_id)
        ON DELETE RESTRICT,
    INDEX idx_document_access_rule_minimum_job_grade (minimum_job_grade_id),
    INDEX idx_document_access_rule_created_by (created_by)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE document_access_role (
    document_access_role_id BIGINT NOT NULL AUTO_INCREMENT,
    document_access_rule_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_document_access_role
        PRIMARY KEY (document_access_role_id),
    CONSTRAINT fk_document_access_role_rule
        FOREIGN KEY (document_access_rule_id)
        REFERENCES document_access_rule (document_access_rule_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_document_access_role_role
        FOREIGN KEY (role_id) REFERENCES role (role_id)
        ON DELETE RESTRICT,
    INDEX idx_document_access_role_rule (document_access_rule_id),
    INDEX idx_document_access_role_role (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE document_access_department (
    document_access_department_id BIGINT NOT NULL AUTO_INCREMENT,
    document_access_rule_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_document_access_department
        PRIMARY KEY (document_access_department_id),
    CONSTRAINT fk_document_access_department_rule
        FOREIGN KEY (document_access_rule_id)
        REFERENCES document_access_rule (document_access_rule_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_document_access_department_department
        FOREIGN KEY (department_id) REFERENCES department (department_id)
        ON DELETE RESTRICT,
    INDEX idx_document_access_department_rule (document_access_rule_id),
    INDEX idx_document_access_department_department (department_id)
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

CREATE TABLE rag_question (
    rag_question_id BIGINT NOT NULL AUTO_INCREMENT,
    question_text TEXT NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_rag_question PRIMARY KEY (rag_question_id),
    CONSTRAINT fk_rag_question_created_by
        FOREIGN KEY (created_by) REFERENCES app_user (app_user_id)
        ON DELETE RESTRICT,
    INDEX idx_rag_question_created_by (created_by, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE rag_search (
    rag_search_id BIGINT NOT NULL AUTO_INCREMENT,
    rag_question_id BIGINT NOT NULL,
    ai_model_id BIGINT NOT NULL,
    searched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_rag_search PRIMARY KEY (rag_search_id),
    CONSTRAINT fk_rag_search_question
        FOREIGN KEY (rag_question_id) REFERENCES rag_question (rag_question_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_rag_search_ai_model
        FOREIGN KEY (ai_model_id) REFERENCES ai_model (ai_model_id)
        ON DELETE RESTRICT,
    INDEX idx_rag_search_question (rag_question_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE rag_search_result (
    rag_search_result_id BIGINT NOT NULL AUTO_INCREMENT,
    rag_search_id BIGINT NOT NULL,
    document_chunk_id BIGINT NOT NULL,
    document_version_id BIGINT NOT NULL,
    similarity_score DOUBLE NOT NULL,
    result_rank INT NOT NULL,
    CONSTRAINT pk_rag_search_result PRIMARY KEY (rag_search_result_id),
    CONSTRAINT fk_rag_search_result_search
        FOREIGN KEY (rag_search_id) REFERENCES rag_search (rag_search_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_rag_search_result_chunk
        FOREIGN KEY (document_chunk_id) REFERENCES document_chunk (document_chunk_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_rag_search_result_version
        FOREIGN KEY (document_version_id) REFERENCES document_version (document_version_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_rag_search_result_score
        CHECK (similarity_score >= 0.0),
    CONSTRAINT ck_rag_search_result_rank
        CHECK (result_rank > 0),
    CONSTRAINT uk_rag_search_result_rank
        UNIQUE (rag_search_id, result_rank)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE rag_answer (
    rag_answer_id BIGINT NOT NULL AUTO_INCREMENT,
    rag_search_id BIGINT NOT NULL,
    answer_text TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_rag_answer PRIMARY KEY (rag_answer_id),
    CONSTRAINT fk_rag_answer_search
        FOREIGN KEY (rag_search_id) REFERENCES rag_search (rag_search_id)
        ON DELETE CASCADE,
    CONSTRAINT uk_rag_answer_search UNIQUE (rag_search_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE rag_citation (
    rag_citation_id BIGINT NOT NULL AUTO_INCREMENT,
    rag_answer_id BIGINT NOT NULL,
    document_chunk_id BIGINT NOT NULL,
    position INT NOT NULL,
    CONSTRAINT pk_rag_citation PRIMARY KEY (rag_citation_id),
    CONSTRAINT fk_rag_citation_answer
        FOREIGN KEY (rag_answer_id) REFERENCES rag_answer (rag_answer_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_rag_citation_chunk
        FOREIGN KEY (document_chunk_id) REFERENCES document_chunk (document_chunk_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_rag_citation_position CHECK (position > 0),
    CONSTRAINT uk_rag_citation_position UNIQUE (rag_answer_id, position)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
