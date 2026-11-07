-- =============================================================================
-- MOTTU SMART OPS — POSTGRESQL EXTENSIONS INITIALIZATION
-- =============================================================================
-- This script runs automatically on initial container creation via
-- /docker-entrypoint-initdb.d/01-init-extensions.sql.
-- Both extensions are declared with IF NOT EXISTS to ensure full idempotency
-- with Flyway V1__initial_schema.sql.
-- =============================================================================

-- 1. Enable pgcrypto for UUID generation and cryptographic utilities
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. Enable pgvector for 768-dimensional semantic embeddings (CRAG)
CREATE EXTENSION IF NOT EXISTS "vector";

-- 3. Verify extension installation
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pgcrypto') AND
       EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        RAISE NOTICE 'Mottu Smart Ops: Extensions pgcrypto and vector initialized successfully in database %.', current_database();
    ELSE
        RAISE EXCEPTION 'Mottu Smart Ops: Failed to initialize required PostgreSQL extensions.';
    END IF;
END $$;
