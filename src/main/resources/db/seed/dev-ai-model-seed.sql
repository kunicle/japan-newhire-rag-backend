-- Development-only AI model seed for local E2E testing.
-- Run manually; this is not an application startup script or a Flyway migration.
-- Exactly one ACTIVE default EMBEDDING model is required.

USE japan_newhire_rag;

INSERT INTO ai_model (
    provider_name,
    model_name,
    model_type,
    embedding_dimension,
    model_status,
    is_default
)
SELECT
    'openai',
    'text-embedding-3-small',
    'EMBEDDING',
    1536,
    'ACTIVE',
    TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM ai_model
    WHERE provider_name = 'openai'
      AND model_name = 'text-embedding-3-small'
      AND model_type = 'EMBEDDING'
);

-- Verification queries.
SELECT
    ai_model_id,
    provider_name,
    model_name,
    model_type,
    embedding_dimension,
    model_status,
    is_default
FROM ai_model
WHERE provider_name = 'openai'
  AND model_name = 'text-embedding-3-small'
  AND model_type = 'EMBEDDING';

SELECT COUNT(*) AS active_default_embedding_model_count
FROM ai_model
WHERE model_type = 'EMBEDDING'
  AND model_status = 'ACTIVE'
  AND is_default = TRUE;
