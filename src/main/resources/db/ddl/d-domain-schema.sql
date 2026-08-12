-- D domain schema for MySQL 8.x
-- Review and execute manually. This file is not a Flyway migration.

USE japan_newhire_rag;

CREATE TABLE evaluation_cycle (
    evaluation_cycle_id BIGINT NOT NULL AUTO_INCREMENT,
    cycle_name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    planned_publish_date DATE NOT NULL,
    cycle_status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    CONSTRAINT pk_evaluation_cycle PRIMARY KEY (evaluation_cycle_id),
    CONSTRAINT fk_evaluation_cycle_created_by
        FOREIGN KEY (created_by) REFERENCES app_user (app_user_id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_evaluation_cycle_dates
        CHECK (end_date >= start_date AND planned_publish_date >= start_date),
    INDEX idx_evaluation_cycle_status_dates (cycle_status, start_date, end_date),
    INDEX idx_evaluation_cycle_publish (planned_publish_date, cycle_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE evaluation_template (
    evaluation_template_id BIGINT NOT NULL AUTO_INCREMENT,
    evaluation_cycle_id BIGINT NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    evaluation_type VARCHAR(20) NOT NULL,
    template_description VARCHAR(1000) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_evaluation_template PRIMARY KEY (evaluation_template_id),
    CONSTRAINT uk_evaluation_template
        UNIQUE (evaluation_cycle_id, evaluation_type),
    CONSTRAINT fk_evaluation_template_cycle
        FOREIGN KEY (evaluation_cycle_id) REFERENCES evaluation_cycle (evaluation_cycle_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_evaluation_template_created_by
        FOREIGN KEY (created_by) REFERENCES app_user (app_user_id)
        ON DELETE RESTRICT,
    INDEX idx_evaluation_template_cycle (evaluation_cycle_id, is_active)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE evaluation_item (
    evaluation_item_id BIGINT NOT NULL AUTO_INCREMENT,
    evaluation_template_id BIGINT NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    item_description VARCHAR(1000) NULL,
    item_order INT NOT NULL,
    weight DECIMAL(7,2) NOT NULL,
    is_required TINYINT(1) NOT NULL DEFAULT 1,
    minimum_score INT NOT NULL DEFAULT 1,
    maximum_score INT NOT NULL DEFAULT 5,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_evaluation_item PRIMARY KEY (evaluation_item_id),
    CONSTRAINT uk_evaluation_item_order
        UNIQUE (evaluation_template_id, item_order),
    CONSTRAINT fk_evaluation_item_template
        FOREIGN KEY (evaluation_template_id) REFERENCES evaluation_template (evaluation_template_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_evaluation_item_scores
        CHECK (minimum_score = 1 AND maximum_score = 5),
    CONSTRAINT ck_evaluation_item_weight
        CHECK (weight > 0),
    INDEX idx_evaluation_item_template (evaluation_template_id, item_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE evaluation (
    evaluation_id BIGINT NOT NULL AUTO_INCREMENT,
    evaluation_cycle_id BIGINT NOT NULL,
    evaluation_template_id BIGINT NOT NULL,
    target_employee_id BIGINT NOT NULL,
    evaluator_employee_id BIGINT NOT NULL,
    evaluation_type VARCHAR(20) NOT NULL,
    evaluation_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_score DECIMAL(7,2) NULL,
    submitted_at DATETIME NULL,
    published_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_evaluation PRIMARY KEY (evaluation_id),
    CONSTRAINT uk_evaluation_assignment
        UNIQUE (
            evaluation_cycle_id,
            target_employee_id,
            evaluator_employee_id,
            evaluation_type
        ),
    CONSTRAINT fk_evaluation_cycle
        FOREIGN KEY (evaluation_cycle_id) REFERENCES evaluation_cycle (evaluation_cycle_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_evaluation_template
        FOREIGN KEY (evaluation_template_id) REFERENCES evaluation_template (evaluation_template_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_evaluation_target_employee
        FOREIGN KEY (target_employee_id) REFERENCES employee (employee_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_evaluation_evaluator_employee
        FOREIGN KEY (evaluator_employee_id) REFERENCES employee (employee_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_evaluation_type_people
        CHECK (
            (evaluation_type = 'SELF' AND target_employee_id = evaluator_employee_id)
            OR
            (evaluation_type = 'MANAGER' AND target_employee_id <> evaluator_employee_id)
        ),
    CONSTRAINT ck_evaluation_total_score
        CHECK (total_score IS NULL OR total_score >= 0),
    INDEX idx_evaluation_target
        (target_employee_id, evaluation_cycle_id, evaluation_status),
    INDEX idx_evaluation_evaluator
        (evaluator_employee_id, evaluation_cycle_id, evaluation_status),
    INDEX idx_evaluation_cycle_status
        (evaluation_cycle_id, evaluation_status),
    INDEX idx_evaluation_published
        (target_employee_id, published_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE evaluation_score (
    evaluation_score_id BIGINT NOT NULL AUTO_INCREMENT,
    evaluation_id BIGINT NOT NULL,
    evaluation_item_id BIGINT NOT NULL,
    score DECIMAL(3,1) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_evaluation_score PRIMARY KEY (evaluation_score_id),
    CONSTRAINT uk_evaluation_score_item
        UNIQUE (evaluation_id, evaluation_item_id),
    CONSTRAINT fk_evaluation_score_evaluation
        FOREIGN KEY (evaluation_id) REFERENCES evaluation (evaluation_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_evaluation_score_item
        FOREIGN KEY (evaluation_item_id) REFERENCES evaluation_item (evaluation_item_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_evaluation_score_range
        CHECK (score BETWEEN 1 AND 5),
    INDEX idx_evaluation_score_evaluation (evaluation_id),
    INDEX idx_evaluation_score_item (evaluation_item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE evaluation_feedback (
    evaluation_feedback_id BIGINT NOT NULL AUTO_INCREMENT,
    evaluation_id BIGINT NOT NULL,
    evaluation_item_id BIGINT NULL,
    feedback_type VARCHAR(20) NOT NULL,
    feedback_content VARCHAR(2000) NOT NULL,
    is_visible_to_employee TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_evaluation_feedback PRIMARY KEY (evaluation_feedback_id),
    CONSTRAINT uk_evaluation_feedback_item
        UNIQUE (evaluation_id, evaluation_item_id, feedback_type),
    CONSTRAINT fk_evaluation_feedback_evaluation
        FOREIGN KEY (evaluation_id) REFERENCES evaluation (evaluation_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_evaluation_feedback_item
        FOREIGN KEY (evaluation_item_id) REFERENCES evaluation_item (evaluation_item_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_evaluation_feedback_length
        CHECK (
            (feedback_type = 'ITEM' AND CHAR_LENGTH(feedback_content) <= 1000)
            OR
            (feedback_type = 'OVERALL' AND CHAR_LENGTH(feedback_content) <= 2000)
        ),
    INDEX idx_evaluation_feedback_visible
        (evaluation_id, is_visible_to_employee)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE evaluation_publish_history (
    evaluation_publish_history_id BIGINT NOT NULL AUTO_INCREMENT,
    evaluation_id BIGINT NOT NULL,
    published_by BIGINT NOT NULL,
    previous_status VARCHAR(20) NOT NULL,
    published_status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    publish_reason VARCHAR(500) NULL,
    published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_evaluation_publish_history
        PRIMARY KEY (evaluation_publish_history_id),
    CONSTRAINT fk_evaluation_publish_history_evaluation
        FOREIGN KEY (evaluation_id) REFERENCES evaluation (evaluation_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_evaluation_publish_history_published_by
        FOREIGN KEY (published_by) REFERENCES app_user (app_user_id)
        ON DELETE RESTRICT,
    INDEX idx_evaluation_publish_eval (evaluation_id, published_at),
    INDEX idx_evaluation_publish_user (published_by, published_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
