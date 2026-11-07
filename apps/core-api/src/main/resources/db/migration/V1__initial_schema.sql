CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "vector";

-- 1. Galpões / Filiais Operacionais
CREATE TABLE yards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    city VARCHAR(80) NOT NULL,
    address VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 2. Usuários (Entregador, Mecânico, Gestor)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(120) NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,               -- UserRole
    status VARCHAR(25) NOT NULL,             -- AccountStatus
    rejection_reason VARCHAR(255),
    approved_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. Motocicletas
CREATE TABLE motorcycles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plate VARCHAR(10) NOT NULL UNIQUE,
    model VARCHAR(30) NOT NULL,              -- MotorcycleModel
    yard_id UUID NOT NULL REFERENCES yards(id),
    status VARCHAR(25) NOT NULL,             -- MotorcycleStatus
    current_odometer_km INTEGER NOT NULL CHECK (current_odometer_km >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. Contratos de Locação
CREATE TABLE rental_contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rider_id UUID NOT NULL REFERENCES users(id),
    motorcycle_id UUID NOT NULL REFERENCES motorcycles(id),
    picked_up_at TIMESTAMPTZ NOT NULL,
    initial_odometer_km INTEGER NOT NULL CHECK (initial_odometer_km >= 0),
    ended_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 5. Configurações Globais Editáveis pelo Gestor
CREATE TABLE system_settings (
    setting_key VARCHAR(60) PRIMARY KEY,
    setting_value VARCHAR(100) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. Catálogo Oficial de Peças (Sincronizado pelo serviço Python + Editável pelo Gestor)
CREATE TABLE catalog_parts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vtex_sku VARCHAR(60) NOT NULL,
    motorcycle_model VARCHAR(30) NOT NULL,   -- MotorcycleModel
    part_name VARCHAR(180) NOT NULL,
    category VARCHAR(80) NOT NULL,
    unit_price_brl NUMERIC(10, 2) NOT NULL CHECK (unit_price_brl >= 0),
    is_natural_wear BOOLEAN NOT NULL DEFAULT FALSE,
    price_overridden BOOLEAN NOT NULL DEFAULT FALSE, -- Se TRUE, sync da loja não sobrescreve o preço manual!
    image_url TEXT,
    embedding vector(768),                   -- Vetor 768d p/ busca semântica CRAG
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (vtex_sku, motorcycle_model)
);
CREATE INDEX idx_catalog_parts_model ON catalog_parts(motorcycle_model);
CREATE INDEX idx_catalog_parts_embedding ON catalog_parts USING hnsw (embedding vector_cosine_ops);

-- 7. Chamados de Vistoria Inteligente
CREATE TABLE inspections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(80) NOT NULL UNIQUE,
    contract_id UUID NOT NULL REFERENCES rental_contracts(id),
    rider_id UUID NOT NULL REFERENCES users(id),
    motorcycle_id UUID NOT NULL REFERENCES motorcycles(id),
    photo_object_key VARCHAR(255) NOT NULL,
    photo_sha256 CHAR(64) NOT NULL,
    photo_perceptual_hash VARCHAR(64),
    fraud_suspected BOOLEAN NOT NULL DEFAULT FALSE,
    fraud_reason VARCHAR(200),
    reported_context VARCHAR(30) NOT NULL,   -- ReportedOriginContext
    safety_risk VARCHAR(25) NOT NULL,        -- SafetyRiskLevel
    reported_odometer_km INTEGER NOT NULL,
    rider_notes VARCHAR(300),
    status VARCHAR(30) NOT NULL,             -- InspectionStatus
    priority_weight SMALLINT,                -- 1 a 4
    priority_reason VARCHAR(35),             -- QueuePriorityReason
    ai_confidence NUMERIC(4, 3),
    ai_verdict VARCHAR(25),                  -- DamageVerdict sugerido pela IA
    ai_technical_summary TEXT,
    final_verdict VARCHAR(25),               -- DamageVerdict vigente
    labor_fee_brl NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    total_estimated_brl NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    customer_feedback_text TEXT,
    contested_once BOOLEAN NOT NULL DEFAULT FALSE,
    contestation_reason VARCHAR(500),
    reviewed_by_mechanic_id UUID REFERENCES users(id),
    mechanic_notes VARCHAR(500),
    retry_count SMALLINT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);
CREATE INDEX idx_inspections_triage_queue ON inspections(status, priority_weight, created_at);
CREATE INDEX idx_inspections_photo_sha256 ON inspections(photo_sha256);

-- 8. Peças Identificadas em cada Vistoria
CREATE TABLE inspection_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inspection_id UUID NOT NULL REFERENCES inspections(id) ON DELETE CASCADE,
    catalog_part_id UUID REFERENCES catalog_parts(id),
    part_name_snapshot VARCHAR(180) NOT NULL,
    unit_price_snapshot_brl NUMERIC(10, 2) NOT NULL,
    added_by VARCHAR(15) NOT NULL            -- 'AI' ou 'MECHANIC'
);
CREATE INDEX idx_inspection_items_inspection ON inspection_items(inspection_id);

-- 9. Agendamento de Ida ao Galpão
CREATE TABLE yard_appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inspection_id UUID NOT NULL UNIQUE REFERENCES inspections(id),
    yard_id UUID NOT NULL REFERENCES yards(id),
    scheduled_date DATE NOT NULL,
    shift VARCHAR(15) NOT NULL,              -- AppointmentShift
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 10. Trilha Imutável de Auditoria (Somente INSERT)
CREATE TABLE inspection_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inspection_id UUID NOT NULL REFERENCES inspections(id),
    actor_id UUID,
    actor_role VARCHAR(20) NOT NULL,         -- 'SYSTEM_AI', 'RIDER', 'MECHANIC', 'MANAGER'
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    details_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_logs_inspection ON inspection_audit_logs(inspection_id);

-- 11. Caixa de Saída para o Google BigQuery (Envio em Lote NDJSON)
CREATE TABLE analytics_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inspection_id UUID NOT NULL REFERENCES inspections(id),
    payload_json JSONB NOT NULL,
    exported BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    exported_at TIMESTAMPTZ
);
CREATE INDEX idx_analytics_outbox_unexported ON analytics_outbox(exported, created_at);
