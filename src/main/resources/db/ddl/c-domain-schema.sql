-- 1번째 : course
CREATE TABLE IF NOT EXISTS course (
    course_id BIGINT NOT NULL AUTO_INCREMENT,
    course_name VARCHAR(100) NOT NULL,
    course_description VARCHAR(2000) NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    training_start_date DATE NOT NULL,
    training_end_date DATE NOT NULL,
    publication_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,

    CONSTRAINT pk_course
        PRIMARY KEY (course_id),

    CONSTRAINT fk_course_created_by
        FOREIGN KEY (created_by)
        REFERENCES app_user (app_user_id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_course_dates
        CHECK (training_end_date >= training_start_date),

    CONSTRAINT ck_course_name
        CHECK (CHAR_LENGTH(course_name) BETWEEN 1 AND 100),

    INDEX idx_course_status_dates (
        publication_status,
        training_start_date,
        training_end_date
    ),

    INDEX idx_course_created_at (created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

-- 2번째 : course_module
CREATE TABLE IF NOT EXISTS course_module (
    course_module_id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    module_title VARCHAR(200) NOT NULL,
    module_content TEXT NULL,
    reference_url VARCHAR(500) NULL,
    module_order INT NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_course_module
        PRIMARY KEY (course_module_id),

    CONSTRAINT fk_course_module_course
        FOREIGN KEY (course_id)
        REFERENCES course (course_id)
        ON DELETE CASCADE,

    CONSTRAINT uk_course_module_order
        UNIQUE (course_id, module_order),

    CONSTRAINT ck_course_module_order
        CHECK (module_order > 0),

    CONSTRAINT ck_course_module_content
        CHECK (
            module_content IS NOT NULL
            OR reference_url IS NOT NULL
        ),

    INDEX idx_course_module_course (
        course_id,
        is_active,
        module_order
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

-- 3번째 : course_assignment
CREATE TABLE IF NOT EXISTS course_assignment (
    course_assignment_id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    employee_id BIGINT NULL,
    department_id BIGINT NULL,
    job_grade_id BIGINT NULL,
    is_new_employee_target BOOLEAN NOT NULL DEFAULT FALSE,
    enrollment_round VARCHAR(30) NOT NULL DEFAULT '1',
    enrollment_start_date DATE NOT NULL,
    enrollment_due_date DATE NOT NULL,
    assigned_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_course_assignment
        PRIMARY KEY (course_assignment_id),

    CONSTRAINT fk_course_assignment_course
        FOREIGN KEY (course_id)
        REFERENCES course (course_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_course_assignment_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee (employee_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_course_assignment_department
        FOREIGN KEY (department_id)
        REFERENCES department (department_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_course_assignment_job_grade
        FOREIGN KEY (job_grade_id)
        REFERENCES job_grade (job_grade_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_course_assignment_assigned_by
        FOREIGN KEY (assigned_by)
        REFERENCES app_user (app_user_id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_course_assignment_dates
        CHECK (
            enrollment_due_date >= enrollment_start_date
        ),

    CONSTRAINT ck_course_assignment_target
        CHECK (
            (
                target_type = 'EMPLOYEE'
                AND employee_id IS NOT NULL
                AND department_id IS NULL
                AND job_grade_id IS NULL
                AND is_new_employee_target = FALSE
            )
            OR
            (
                target_type = 'DEPARTMENT'
                AND employee_id IS NULL
                AND department_id IS NOT NULL
                AND job_grade_id IS NULL
                AND is_new_employee_target = FALSE
            )
            OR
            (
                target_type = 'JOB_GRADE'
                AND employee_id IS NULL
                AND department_id IS NULL
                AND job_grade_id IS NOT NULL
                AND is_new_employee_target = FALSE
            )
            OR
            (
                target_type = 'NEW_HIRE'
                AND employee_id IS NULL
                AND department_id IS NULL
                AND job_grade_id IS NULL
                AND is_new_employee_target = TRUE
            )
        ),

    INDEX idx_course_assignment_course (
        course_id,
        enrollment_round
    ),

    INDEX idx_course_assignment_target (
        target_type,
        department_id,
        job_grade_id,
        employee_id
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
  
  -- 4번째 : course_enrollment
  CREATE TABLE IF NOT EXISTS course_enrollment (
    course_enrollment_id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    course_assignment_id BIGINT NOT NULL,
    enrollment_round VARCHAR(30) NOT NULL DEFAULT '1',
    enrollment_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    progress_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    enrollment_start_date DATE NOT NULL,
    enrollment_due_date DATE NOT NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_course_enrollment
        PRIMARY KEY (course_enrollment_id),

    CONSTRAINT fk_course_enrollment_course
        FOREIGN KEY (course_id)
        REFERENCES course (course_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_course_enrollment_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee (employee_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_course_enrollment_assignment
        FOREIGN KEY (course_assignment_id)
        REFERENCES course_assignment (course_assignment_id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_course_enrollment
        UNIQUE (course_id, employee_id, enrollment_round),

    CONSTRAINT ck_course_enrollment_dates
        CHECK (enrollment_due_date >= enrollment_start_date),

    CONSTRAINT ck_course_progress
        CHECK (progress_rate BETWEEN 0 AND 100),

    INDEX idx_course_enrollment_employee (
        employee_id,
        enrollment_status,
        enrollment_due_date
    ),

    INDEX idx_course_enrollment_course (
        course_id,
        enrollment_status
    ),

    INDEX idx_course_enrollment_due (
        enrollment_status,
        enrollment_due_date
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
  
  -- 5번째 : learning_progress
  CREATE TABLE IF NOT EXISTS learning_progress (
    learning_progress_id BIGINT NOT NULL AUTO_INCREMENT,
    course_enrollment_id BIGINT NOT NULL,
    course_module_id BIGINT NOT NULL,
    completion_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_learning_progress
        PRIMARY KEY (learning_progress_id),

    CONSTRAINT fk_learning_progress_enrollment
        FOREIGN KEY (course_enrollment_id)
        REFERENCES course_enrollment (course_enrollment_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_learning_progress_module
        FOREIGN KEY (course_module_id)
        REFERENCES course_module (course_module_id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_learning_progress
        UNIQUE (course_enrollment_id, course_module_id),

    INDEX idx_learning_progress_enrollment (
        course_enrollment_id,
        completion_status
    ),

    INDEX idx_learning_progress_module (
        course_module_id,
        completion_status
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
  
  -- 6번째 : quiz
  CREATE TABLE IF NOT EXISTS quiz (
    quiz_id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    course_module_id BIGINT NULL,
    quiz_title VARCHAR(200) NOT NULL,
    passing_score DECIMAL(5, 2) NOT NULL,
    max_attempt_count INT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_quiz
        PRIMARY KEY (quiz_id),

    CONSTRAINT fk_quiz_course
        FOREIGN KEY (course_id)
        REFERENCES course (course_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_quiz_course_module
        FOREIGN KEY (course_module_id)
        REFERENCES course_module (course_module_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_quiz_created_by
        FOREIGN KEY (created_by)
        REFERENCES app_user (app_user_id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_quiz_passing_score
        CHECK (passing_score BETWEEN 0 AND 100),

    CONSTRAINT ck_quiz_attempt_count
        CHECK (
            max_attempt_count IS NULL
            OR max_attempt_count > 0
        ),

    INDEX idx_quiz_course_active (
        course_id,
        is_active
    ),

    INDEX idx_quiz_module (
        course_module_id
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
  
-- 7번째 : quiz_question
CREATE TABLE IF NOT EXISTS quiz_question (
    quiz_question_id BIGINT NOT NULL AUTO_INCREMENT,
    quiz_id BIGINT NOT NULL,
    question_content TEXT NOT NULL,
    question_order INT NOT NULL,
    score DECIMAL(5, 2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_quiz_question
        PRIMARY KEY (quiz_question_id),

    CONSTRAINT fk_quiz_question_quiz
        FOREIGN KEY (quiz_id)
        REFERENCES quiz (quiz_id)
        ON DELETE CASCADE,

    CONSTRAINT uk_quiz_question_order
        UNIQUE (quiz_id, question_order),

    CONSTRAINT ck_quiz_question_score
        CHECK (score > 0),

    INDEX idx_quiz_question_quiz (
        quiz_id,
        is_active,
        question_order
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
  
-- 8번째 : quiz_option
  CREATE TABLE IF NOT EXISTS quiz_option (
    quiz_option_id BIGINT NOT NULL AUTO_INCREMENT,
    quiz_question_id BIGINT NOT NULL,
    option_content VARCHAR(1000) NOT NULL,
    option_order INT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_quiz_option
        PRIMARY KEY (quiz_option_id),

    CONSTRAINT fk_quiz_option_question
        FOREIGN KEY (quiz_question_id)
        REFERENCES quiz_question (quiz_question_id)
        ON DELETE CASCADE,

    CONSTRAINT uk_quiz_option_order
        UNIQUE (quiz_question_id, option_order),

    INDEX idx_quiz_option_question (
        quiz_question_id,
        option_order
    ),

    INDEX idx_quiz_option_correct (
        quiz_question_id,
        is_correct
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
  
-- 9번째 : quiz_attempt
CREATE TABLE IF NOT EXISTS quiz_attempt (
    quiz_attempt_id BIGINT NOT NULL AUTO_INCREMENT,
    quiz_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    course_enrollment_id BIGINT NOT NULL,
    attempt_number INT NOT NULL,
    total_score DECIMAL(7, 2) NULL,
    is_passed BOOLEAN NULL,
    attempt_status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_quiz_attempt
        PRIMARY KEY (quiz_attempt_id),

    CONSTRAINT fk_quiz_attempt_quiz
        FOREIGN KEY (quiz_id)
        REFERENCES quiz (quiz_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_quiz_attempt_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee (employee_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_quiz_attempt_enrollment
        FOREIGN KEY (course_enrollment_id)
        REFERENCES course_enrollment (course_enrollment_id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_quiz_attempt_number
        UNIQUE (
            quiz_id,
            employee_id,
            course_enrollment_id,
            attempt_number
        ),

    CONSTRAINT ck_quiz_attempt_number
        CHECK (attempt_number > 0),

    CONSTRAINT ck_quiz_total_score
        CHECK (
            total_score IS NULL
            OR total_score >= 0
        ),

    INDEX idx_quiz_attempt_employee (
        employee_id,
        quiz_id,
        attempt_number
    ),

    INDEX idx_quiz_attempt_status (
        attempt_status,
        submitted_at
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
  
-- 10번째 : quiz_response
CREATE TABLE IF NOT EXISTS quiz_response (
    quiz_response_id BIGINT NOT NULL AUTO_INCREMENT,
    quiz_attempt_id BIGINT NOT NULL,
    quiz_question_id BIGINT NOT NULL,
    is_correct BOOLEAN NULL,
    earned_score DECIMAL(5, 2) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_quiz_response
        PRIMARY KEY (quiz_response_id),

    CONSTRAINT fk_quiz_response_attempt
        FOREIGN KEY (quiz_attempt_id)
        REFERENCES quiz_attempt (quiz_attempt_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_quiz_response_question
        FOREIGN KEY (quiz_question_id)
        REFERENCES quiz_question (quiz_question_id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_quiz_response_question
        UNIQUE (
            quiz_attempt_id,
            quiz_question_id
        ),

    CONSTRAINT ck_quiz_earned_score
        CHECK (
            earned_score IS NULL
            OR earned_score >= 0
        ),

    INDEX idx_quiz_response_attempt (
        quiz_attempt_id
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
  
-- 11번째 : quiz_response_option
CREATE TABLE IF NOT EXISTS quiz_response_option (
    quiz_response_option_id BIGINT NOT NULL AUTO_INCREMENT,
    quiz_response_id BIGINT NOT NULL,
    quiz_option_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_quiz_response_option
        PRIMARY KEY (quiz_response_option_id),

    CONSTRAINT fk_quiz_response_option_response
        FOREIGN KEY (quiz_response_id)
        REFERENCES quiz_response (quiz_response_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_quiz_response_option_option
        FOREIGN KEY (quiz_option_id)
        REFERENCES quiz_option (quiz_option_id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_quiz_response_option
        UNIQUE (
            quiz_response_id,
            quiz_option_id
        ),

    INDEX idx_quiz_response_option_response (
        quiz_response_id
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
  
-- 12번째 : onboarding_task
CREATE TABLE IF NOT EXISTS onboarding_task (
    onboarding_task_id BIGINT NOT NULL AUTO_INCREMENT,
    department_id BIGINT NOT NULL,
    task_title VARCHAR(200) NOT NULL,
    task_description VARCHAR(2000) NOT NULL,
    default_due_days INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_onboarding_task
        PRIMARY KEY (onboarding_task_id),

    CONSTRAINT fk_onboarding_task_department
        FOREIGN KEY (department_id)
        REFERENCES department (department_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_onboarding_task_created_by
        FOREIGN KEY (created_by)
        REFERENCES app_user (app_user_id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_onboarding_due_days
        CHECK (default_due_days > 0),

    INDEX idx_onboarding_task_department (
        department_id,
        is_active
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
  
  -- 13번째 : onboarding_assignment
  CREATE TABLE IF NOT EXISTS onboarding_assignment (
    onboarding_assignment_id BIGINT NOT NULL AUTO_INCREMENT,
    onboarding_task_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    assigned_by BIGINT NOT NULL,
    assigned_date DATE NOT NULL,
    due_date DATE NOT NULL,
    assignment_status VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_onboarding_assignment
        PRIMARY KEY (onboarding_assignment_id),

    CONSTRAINT fk_onboarding_assignment_task
        FOREIGN KEY (onboarding_task_id)
        REFERENCES onboarding_task (onboarding_task_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_onboarding_assignment_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee (employee_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_onboarding_assignment_assigned_by
        FOREIGN KEY (assigned_by)
        REFERENCES app_user (app_user_id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_onboarding_assignment
        UNIQUE (
            onboarding_task_id,
            employee_id
        ),

    CONSTRAINT ck_onboarding_dates
        CHECK (due_date >= assigned_date),

    INDEX idx_onboarding_assignment_employee (
        employee_id,
        assignment_status,
        due_date
    ),

    INDEX idx_onboarding_assignment_task (
        onboarding_task_id,
        assignment_status
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
  
-- 14번째(마지막) : onboarding_progress
CREATE TABLE IF NOT EXISTS onboarding_progress (
    onboarding_progress_id BIGINT NOT NULL AUTO_INCREMENT,
    onboarding_assignment_id BIGINT NOT NULL,
    completion_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    completion_note VARCHAR(1000) NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_onboarding_progress
        PRIMARY KEY (onboarding_progress_id),

    CONSTRAINT fk_onboarding_progress_assignment
        FOREIGN KEY (onboarding_assignment_id)
        REFERENCES onboarding_assignment (onboarding_assignment_id)
        ON DELETE CASCADE,

    CONSTRAINT uk_onboarding_progress_assignment
        UNIQUE (onboarding_assignment_id),

    INDEX idx_onboarding_progress_status (
        completion_status,
        completed_at
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
