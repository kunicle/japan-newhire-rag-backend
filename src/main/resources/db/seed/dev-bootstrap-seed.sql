-- Development-only prerequisite seed for the first SYSTEM_ADMIN bootstrap.
-- Apply a-domain-schema.sql before running this file. Run manually; this is not
-- an application startup script or a Flyway migration.

USE japan_newhire_rag;

INSERT INTO role (role_code, role_name, role_description, is_active)
SELECT 'EMPLOYEE', 'Employee', 'Standard employee role', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM role WHERE role_code = 'EMPLOYEE'
);

INSERT INTO role (role_code, role_name, role_description, is_active)
SELECT 'MANAGER', 'Manager', 'People manager role', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM role WHERE role_code = 'MANAGER'
);

INSERT INTO role (role_code, role_name, role_description, is_active)
SELECT 'HR_MANAGER', 'HR Manager', 'Human resources manager role', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM role WHERE role_code = 'HR_MANAGER'
);

INSERT INTO role (role_code, role_name, role_description, is_active)
SELECT 'SYSTEM_ADMIN', 'System Administrator', 'System administration role', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM role WHERE role_code = 'SYSTEM_ADMIN'
);

INSERT INTO department (
    parent_department_id,
    department_code,
    department_name,
    department_status,
    display_order,
    deleted_at
)
SELECT NULL, 'DEV-DEFAULT', 'Development Default Department', 'ACTIVE', 0, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM department WHERE department_code = 'DEV-DEFAULT'
);

INSERT INTO job_grade (grade_code, grade_name, grade_level, is_active)
SELECT 'DEV-G1', 'Development Default Grade', 1, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM job_grade WHERE grade_code = 'DEV-G1'
)
AND NOT EXISTS (
    SELECT 1 FROM job_grade WHERE grade_level = 1
);

-- Verification queries. Use the returned department_id and job_grade_id for
-- AUTH_BOOTSTRAP_DEPARTMENT_ID and AUTH_BOOTSTRAP_JOB_GRADE_ID.
SELECT role_id, role_code, role_name, is_active
FROM role
WHERE role_code IN ('EMPLOYEE', 'MANAGER', 'HR_MANAGER', 'SYSTEM_ADMIN')
ORDER BY role_code;

SELECT department_id, department_code, department_name, department_status,
       deleted_at
FROM department
WHERE department_code = 'DEV-DEFAULT';

SELECT job_grade_id, grade_code, grade_name, grade_level, is_active
FROM job_grade
WHERE grade_code = 'DEV-G1';

-- Official local bootstrap procedure:
-- 1. Apply all DDL files, including a-domain-schema.sql.
-- 2. Run this seed file and use the queries above to verify SYSTEM_ADMIN and
--    obtain the bootstrap department and job-grade IDs.
-- 3. Set DB_USERNAME, DB_PASSWORD, and JWT_SECRET in the environment.
-- 4. Set the following PowerShell environment variables (supply your own
--    password and the IDs returned above):
--    $env:AUTH_BOOTSTRAP_ENABLED="true"
--    $env:AUTH_BOOTSTRAP_ADMIN_EMAIL="bootstrap-admin@test.local"
--    $env:AUTH_BOOTSTRAP_ADMIN_PASSWORD="<choose a local test password>"
--    $env:AUTH_BOOTSTRAP_EMPLOYEE_NUMBER="BOOTSTRAP001"
--    $env:AUTH_BOOTSTRAP_EMPLOYEE_NAME="Bootstrap Admin"
--    $env:AUTH_BOOTSTRAP_DEPARTMENT_ID="<department_id from query>"
--    $env:AUTH_BOOTSTRAP_JOB_GRADE_ID="<job_grade_id from query>"
-- 5. Start the server once and verify that the SYSTEM_ADMIN was created.
-- 6. Set AUTH_BOOTSTRAP_ENABLED=false, then restart the server.
-- 7. Log in as the SYSTEM_ADMIN and use the official administration APIs to
--    create HR_MANAGER, MANAGER, and EMPLOYEE accounts.
