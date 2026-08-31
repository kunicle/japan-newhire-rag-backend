-- Development-only document category seed for local E2E testing.
-- Run manually; this is not an application startup script or a Flyway migration.

USE japan_newhire_rag;

INSERT INTO document_category (
    category_code,
    category_name,
    category_description,
    is_active
)
SELECT 'EMPLOYEE_POLICY', '사내 규정', NULL, TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM document_category
    WHERE category_code = 'EMPLOYEE_POLICY'
);

INSERT INTO document_category (
    category_code,
    category_name,
    category_description,
    is_active
)
SELECT 'ONBOARDING', '온보딩', NULL, TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM document_category
    WHERE category_code = 'ONBOARDING'
);

INSERT INTO document_category (
    category_code,
    category_name,
    category_description,
    is_active
)
SELECT 'BENEFITS', '복리후생', NULL, TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM document_category
    WHERE category_code = 'BENEFITS'
);

-- Verification query.
SELECT
    document_category_id,
    category_code,
    category_name,
    is_active
FROM document_category
WHERE category_code IN (
    'EMPLOYEE_POLICY',
    'ONBOARDING',
    'BENEFITS'
)
ORDER BY category_code;
