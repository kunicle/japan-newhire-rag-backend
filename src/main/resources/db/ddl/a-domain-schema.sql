-- A domain schema for MySQL 8.x
-- Review and execute manually. This file is not a Flyway migration.

USE japan_newhire_rag;

CREATE TABLE app_user (
    app_user_id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    account_status VARCHAR(20) NOT NULL,
    failed_login_count INT NOT NULL DEFAULT 0,
    locked_until DATETIME NULL,
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    CONSTRAINT pk_app_user PRIMARY KEY (app_user_id),
    CONSTRAINT uk_app_user_email UNIQUE (email),
    CONSTRAINT ck_app_user_account_status
        CHECK (account_status IN ('ACTIVE', 'INACTIVE', 'LOCKED')),
    CONSTRAINT ck_app_user_failed_login_count
        CHECK (failed_login_count >= 0),
    INDEX idx_app_user_account_status (account_status),
    INDEX idx_app_user_deleted_at (deleted_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE department (
    department_id BIGINT NOT NULL AUTO_INCREMENT,
    parent_department_id BIGINT NULL,
    department_code VARCHAR(30) NOT NULL,
    department_name VARCHAR(100) NOT NULL,
    department_status VARCHAR(20) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    CONSTRAINT pk_department PRIMARY KEY (department_id),
    CONSTRAINT uk_department_code UNIQUE (department_code),
    CONSTRAINT fk_department_parent
        FOREIGN KEY (parent_department_id) REFERENCES department (department_id)
        ON DELETE RESTRICT,
    -- department 자기참조 방지는 MySQL AUTO_INCREMENT CHECK 제한으로
    -- DB CHECK 대신 애플리케이션 Service에서 검증한다.
    CONSTRAINT ck_department_status
        CHECK (department_status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_department_display_order
        CHECK (display_order >= 0),
    INDEX idx_department_parent_department_id (parent_department_id),
    INDEX idx_department_status_display_order (department_status, display_order),
    INDEX idx_department_deleted_at (deleted_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE job_grade (
    job_grade_id BIGINT NOT NULL AUTO_INCREMENT,
    grade_code VARCHAR(30) NOT NULL,
    grade_name VARCHAR(50) NOT NULL,
    grade_level INT NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT pk_job_grade PRIMARY KEY (job_grade_id),
    CONSTRAINT uk_job_grade_code UNIQUE (grade_code),
    CONSTRAINT uk_job_grade_level UNIQUE (grade_level),
    CONSTRAINT ck_job_grade_level CHECK (grade_level > 0),
    INDEX idx_job_grade_is_active (is_active)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE role (
    role_id BIGINT NOT NULL AUTO_INCREMENT,
    role_code VARCHAR(30) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    role_description VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT pk_role PRIMARY KEY (role_id),
    CONSTRAINT uk_role_code UNIQUE (role_code),
    INDEX idx_role_is_active (is_active)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE employee (
    employee_id BIGINT NOT NULL AUTO_INCREMENT,
    app_user_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    job_grade_id BIGINT NOT NULL,
    employee_number VARCHAR(30) NOT NULL,
    employee_name VARCHAR(50) NOT NULL,
    employee_type VARCHAR(20) NOT NULL,
    hire_date DATE NOT NULL,
    employment_status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    CONSTRAINT pk_employee PRIMARY KEY (employee_id),
    CONSTRAINT uk_employee_app_user_id UNIQUE (app_user_id),
    CONSTRAINT uk_employee_number UNIQUE (employee_number),
    CONSTRAINT fk_employee_app_user
        FOREIGN KEY (app_user_id) REFERENCES app_user (app_user_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_employee_department
        FOREIGN KEY (department_id) REFERENCES department (department_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_employee_job_grade
        FOREIGN KEY (job_grade_id) REFERENCES job_grade (job_grade_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_employee_type
        CHECK (employee_type IN ('NEW_HIRE', 'GENERAL')),
    CONSTRAINT ck_employee_employment_status
        CHECK (employment_status IN ('EMPLOYED', 'LEAVE', 'RETIRED')),
    INDEX idx_employee_department_status (department_id, employment_status),
    INDEX idx_employee_job_grade_status (job_grade_id, employment_status),
    INDEX idx_employee_type_status (employee_type, employment_status),
    INDEX idx_employee_deleted_at (deleted_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE user_role (
    user_role_id BIGINT NOT NULL AUTO_INCREMENT,
    app_user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    granted_by BIGINT NOT NULL,
    granted_at DATETIME NOT NULL,
    revoked_by BIGINT NULL,
    revoked_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT pk_user_role PRIMARY KEY (user_role_id),
    CONSTRAINT fk_user_role_app_user
        FOREIGN KEY (app_user_id) REFERENCES app_user (app_user_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_user_role_role
        FOREIGN KEY (role_id) REFERENCES role (role_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_user_role_granted_by
        FOREIGN KEY (granted_by) REFERENCES app_user (app_user_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_user_role_revoked_by
        FOREIGN KEY (revoked_by) REFERENCES app_user (app_user_id)
        ON DELETE SET NULL,
    CONSTRAINT ck_user_role_revoked_at
        CHECK (revoked_at IS NULL OR revoked_at >= granted_at),
    INDEX idx_user_role_current (app_user_id, revoked_at),
    INDEX idx_user_role_role_id (role_id),
    INDEX idx_user_role_granted_by (granted_by),
    INDEX idx_user_role_revoked_by (revoked_by)
    -- MySQL UNIQUE permits multiple NULL values. Therefore UNIQUE
    -- (app_user_id, role_id, revoked_at) would not prevent duplicate active rows.
    -- Enforce one active assignment per user/role in the application transaction.
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE manager_relation (
    manager_relation_id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    manager_employee_id BIGINT NOT NULL,
    relation_type VARCHAR(20) NOT NULL,
    started_at DATETIME NOT NULL,
    ended_at DATETIME NULL,
    relation_status VARCHAR(20) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT pk_manager_relation PRIMARY KEY (manager_relation_id),
    CONSTRAINT fk_manager_relation_employee
        FOREIGN KEY (employee_id) REFERENCES employee (employee_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_manager_relation_manager_employee
        FOREIGN KEY (manager_employee_id) REFERENCES employee (employee_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_manager_relation_created_by
        FOREIGN KEY (created_by) REFERENCES app_user (app_user_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_manager_relation_not_self
        CHECK (employee_id <> manager_employee_id),
    CONSTRAINT ck_manager_relation_type
        CHECK (relation_type IN ('DIRECT', 'TEAM')),
    CONSTRAINT ck_manager_relation_status
        CHECK (relation_status IN ('ACTIVE', 'ENDED')),
    CONSTRAINT ck_manager_relation_ended_at
        CHECK (ended_at IS NULL OR ended_at >= started_at),
    INDEX idx_manager_relation_current_manager
        (manager_employee_id, relation_status, ended_at),
    INDEX idx_manager_relation_current_employee
        (employee_id, relation_status, ended_at),
    INDEX idx_manager_relation_created_by (created_by)
    -- MySQL cannot express a partial UNIQUE constraint for ACTIVE rows only,
    -- and a nullable ended_at in a composite UNIQUE does not prevent duplicates.
    -- Enforce active manager/employee/relation-type uniqueness in the application.
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE login_attempt (
    login_attempt_id BIGINT NOT NULL AUTO_INCREMENT,
    app_user_id BIGINT NULL,
    input_email VARCHAR(100) NOT NULL,
    login_result VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(30) NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    attempted_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT pk_login_attempt PRIMARY KEY (login_attempt_id),
    CONSTRAINT fk_login_attempt_app_user
        FOREIGN KEY (app_user_id) REFERENCES app_user (app_user_id)
        ON DELETE SET NULL,
    CONSTRAINT ck_login_attempt_result
        CHECK (login_result IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT ck_login_attempt_failure_reason
        CHECK (failure_reason IS NULL OR failure_reason IN
            ('INVALID_CREDENTIALS', 'LOCKED', 'INACTIVE')),
    INDEX idx_login_attempt_app_user_attempted_at (app_user_id, attempted_at),
    INDEX idx_login_attempt_input_email_attempted_at (input_email, attempted_at),
    INDEX idx_login_attempt_result_attempted_at (login_result, attempted_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE refresh_token (
    refresh_token_id BIGINT NOT NULL AUTO_INCREMENT,
    app_user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,
    device_info VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT pk_refresh_token PRIMARY KEY (refresh_token_id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_app_user
        FOREIGN KEY (app_user_id) REFERENCES app_user (app_user_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_refresh_token_expires_at
        CHECK (expires_at > created_at),
    CONSTRAINT ck_refresh_token_revoked_at
        CHECK (revoked_at IS NULL OR revoked_at >= created_at),
    INDEX idx_refresh_token_app_user (app_user_id),
    INDEX idx_refresh_token_expires_at (expires_at),
    INDEX idx_refresh_token_revoked_at (revoked_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
