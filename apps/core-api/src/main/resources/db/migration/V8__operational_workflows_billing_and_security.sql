-- ==============================================================================
-- V8: Operational Workflows, WMS Inventory, Service Orders, Weekly Billing & SHA-256
-- ==============================================================================

-- 1. Evolução de Usuários: Perfil Completo + Hash SHA-256 de Auditoria de Senha
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_sha256 CHAR(64),
    ADD COLUMN IF NOT EXISTS pix_key VARCHAR(150),
    ADD COLUMN IF NOT EXISTS preferred_unit VARCHAR(100) NOT NULL DEFAULT 'Unidade Butantã (SP-04)',
    ADD COLUMN IF NOT EXISTS cnh_status VARCHAR(80) NOT NULL DEFAULT 'Categoria A • Válida até 08/2029',
    ADD COLUMN IF NOT EXISTS employee_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS role_title VARCHAR(80),
    ADD COLUMN IF NOT EXISTS shift_label VARCHAR(80),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- Preencher metadados iniciais de perfil e SHA-256 da senha seed ('Mottu@2026!')
-- Digest de 'Mottu@2026!' em SHA-256: e4948ce65b16f387db2c451ca5d8009623e1f021e1a5fdfdfa15998df90ff5b0
UPDATE users
SET password_sha256 = encode(digest('Mottu@2026!', 'sha256'), 'hex'),
    pix_key = email,
    employee_code = CASE
        WHEN role = 'MANAGER' THEN 'MOT-SP04-0104'
        WHEN role = 'MECHANIC' THEN 'MOT-SP04-0892'
        ELSE 'CTR-2026-' || SUBSTRING(REPLACE(id::text, '-', ''), 1, 4)
    END,
    role_title = CASE
        WHEN role = 'MANAGER' THEN 'Gestor da unidade'
        WHEN role = 'MECHANIC' THEN 'Mecânico líder — Baia 02'
        ELSE 'Entregador parceiro'
    END,
    shift_label = CASE
        WHEN role = 'MANAGER' THEN 'Comercial • 08h às 18h'
        WHEN role = 'MECHANIC' THEN 'Manhã e tarde • 07h às 16h'
        ELSE 'Plano Minha Mottu Semanal'
    END
WHERE password_sha256 IS NULL;

-- 2. Evolução do Catálogo de Peças: Controle de Almoxarifado Físico (WMS)
ALTER TABLE catalog_parts
    ADD COLUMN IF NOT EXISTS stock_quantity INTEGER NOT NULL DEFAULT 18 CHECK (stock_quantity >= 0),
    ADD COLUMN IF NOT EXISTS min_stock_quantity INTEGER NOT NULL DEFAULT 8 CHECK (min_stock_quantity >= 0),
    ADD COLUMN IF NOT EXISTS aisle_bin VARCHAR(60) NOT NULL DEFAULT 'Corredor A • Gaveta 01';

-- Distribuir corredores/gavetas e definir estoque crítico para peças estratégicas
UPDATE catalog_parts
SET aisle_bin = CASE
        WHEN motorcycle_model = 'MOTTU_SPORT_110I' THEN 'Corredor A • Gaveta ' || LPAD(((ABS(HASHTEXT(vtex_sku)) % 12) + 1)::text, 2, '0')
        WHEN motorcycle_model = 'HONDA_POP_110I' THEN 'Corredor B • Gaveta ' || LPAD(((ABS(HASHTEXT(vtex_sku)) % 12) + 1)::text, 2, '0')
        WHEN motorcycle_model = 'TVS_RAIDER_125' THEN 'Corredor C • Gaveta ' || LPAD(((ABS(HASHTEXT(vtex_sku)) % 12) + 1)::text, 2, '0')
        ELSE 'Corredor D • Gaveta ' || LPAD(((ABS(HASHTEXT(vtex_sku)) % 12) + 1)::text, 2, '0')
    END,
    stock_quantity = CASE
        WHEN vtex_sku IN ('SKU-MANETE-DIR-SPORT', 'SKU-SPORT-001', 'SKU-POP-002', 'SKU-RAI-002') THEN 4
        WHEN vtex_sku IN ('SKU-RETROVISOR-DIR-SPORT', 'SKU-SPORT-002', 'SKU-POP-003') THEN 6
        ELSE 14 + (ABS(HASHTEXT(vtex_sku)) % 15)
    END,
    min_stock_quantity = 8;

-- 3. Tabela de Ordens de Serviço de Baia (service_orders)
CREATE TABLE IF NOT EXISTS service_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(25) NOT NULL UNIQUE,
    motorcycle_id UUID NOT NULL REFERENCES motorcycles(id),
    inspection_id UUID REFERENCES inspections(id),
    yard_id UUID NOT NULL REFERENCES yards(id),
    bay_label VARCHAR(50) NOT NULL,
    service_title VARCHAR(220) NOT NULL,
    origin_label VARCHAR(140) NOT NULL,
    sla_minutes INTEGER NOT NULL DEFAULT 30,
    status VARCHAR(25) NOT NULL, -- 'WAITING_BAY', 'IN_REPAIR', 'READY_PICKUP'
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_service_orders_status ON service_orders(status, created_at DESC);

-- Inserir ordens de serviço iniciais reais na oficina
INSERT INTO service_orders (id, code, motorcycle_id, inspection_id, yard_id, bay_label, service_title, origin_label, sla_minutes, status, created_at, updated_at)
VALUES
(
    'c1000000-0000-0000-0000-000000000001',
    'OS-2026-0412',
    (SELECT id FROM motorcycles WHERE plate = 'MOT1A23' LIMIT 1),
    NULL,
    (SELECT yard_id FROM motorcycles WHERE plate = 'MOT1A23' LIMIT 1),
    'Baia 01 • Urgente',
    'Troca de manete direito + alinhamento do guidão',
    'Vistoria #8841 • Moto parada',
    25,
    'IN_REPAIR',
    NOW() - INTERVAL '40 minutes',
    NOW() - INTERVAL '10 minutes'
),
(
    'c1000000-0000-0000-0000-000000000002',
    'OS-2026-0413',
    (SELECT id FROM motorcycles WHERE plate = 'MOT2B34' LIMIT 1),
    NULL,
    (SELECT yard_id FROM motorcycles WHERE plate = 'MOT2B34' LIMIT 1),
    'Baia 02 • Preventiva',
    'Substituição preventiva de lona de freio',
    'Agendamento preventivo',
    30,
    'WAITING_BAY',
    NOW() - INTERVAL '25 minutes',
    NOW() - INTERVAL '25 minutes'
),
(
    'c1000000-0000-0000-0000-000000000003',
    'OS-2026-0414',
    (SELECT id FROM motorcycles WHERE plate = 'POP6F21' LIMIT 1),
    NULL,
    (SELECT yard_id FROM motorcycles WHERE plate = 'POP6F21' LIMIT 1),
    'Baia 03 • Rápida',
    'Regulagem da embreagem e lubrificação da corrente',
    'Vistoria de rotina',
    20,
    'WAITING_BAY',
    NOW() - INTERVAL '15 minutes',
    NOW() - INTERVAL '15 minutes'
),
(
    'c1000000-0000-0000-0000-000000000004',
    'OS-2026-0415',
    (SELECT id FROM motorcycles WHERE plate = 'RAI7G31' LIMIT 1),
    NULL,
    (SELECT yard_id FROM motorcycles WHERE plate = 'RAI7G31' LIMIT 1),
    'Baia 04 • Elétrica',
    'Diagnóstico de farol baixo e fusível principal',
    'Sinistro #1042',
    45,
    'IN_REPAIR',
    NOW() - INTERVAL '50 minutes',
    NOW() - INTERVAL '12 minutes'
),
(
    'c1000000-0000-0000-0000-000000000005',
    'OS-2026-0416',
    (SELECT id FROM motorcycles WHERE plate = 'APA8H41' LIMIT 1),
    NULL,
    (SELECT yard_id FROM motorcycles WHERE plate = 'APA8H41' LIMIT 1),
    'Baia 02 • Revisão',
    'Troca de óleo de motor e filtro de ar',
    'Revisão periódica 6.000 km',
    35,
    'READY_PICKUP',
    NOW() - INTERVAL '90 minutes',
    NOW() - INTERVAL '5 minutes'
),
(
    'c1000000-0000-0000-0000-000000000006',
    'OS-2026-0417',
    (SELECT id FROM motorcycles WHERE plate = 'MOT5E12' LIMIT 1),
    NULL,
    (SELECT yard_id FROM motorcycles WHERE plate = 'MOT5E12' LIMIT 1),
    'Baia 01 • Carenagem',
    'Ajuste do pedal de freio e espelho retrovisor',
    'Vistoria presencial',
    15,
    'READY_PICKUP',
    NOW() - INTERVAL '110 minutes',
    NOW() - INTERVAL '20 minutes'
)
ON CONFLICT (id) DO NOTHING;

-- 4. Evolução de Contratos de Locação (Faturamento Semanal Pix)
ALTER TABLE rental_contracts
    ADD COLUMN IF NOT EXISTS contract_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS weekly_plan_brl NUMERIC(10, 2) NOT NULL DEFAULT 196.00,
    ADD COLUMN IF NOT EXISTS discount_brl NUMERIC(10, 2) NOT NULL DEFAULT 15.00,
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PIX', -- 'PAID', 'PENDING_PIX', 'OVERDUE'
    ADD COLUMN IF NOT EXISTS due_date_label VARCHAR(80) NOT NULL DEFAULT 'Vence segunda-feira • 18:00',
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ;

-- Atualizar códigos e valores conforme o modelo de moto
UPDATE rental_contracts rc
SET contract_code = CASE
        WHEN rc.id = 'a0000000-0000-0000-0000-000000000001' THEN 'CTR-2026-8841'
        ELSE 'CTR-2026-' || SUBSTRING(REPLACE(rc.id::text, '-', ''), 1, 4)
    END,
    weekly_plan_brl = CASE
        WHEN m.model = 'MOTTU_SPORT_110I' THEN 196.00
        WHEN m.model = 'HONDA_POP_110I' THEN 210.00
        WHEN m.model = 'TVS_RAIDER_125' THEN 224.00
        WHEN m.model = 'TVS_APACHE_160' THEN 238.00
        ELSE 196.00
    END,
    discount_brl = 15.00,
    payment_status = CASE
        WHEN rc.id IN ('a1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000005', 'a1000000-0000-0000-0000-000000000008', 'a1000000-0000-0000-0000-000000000011') THEN 'PAID'
        WHEN rc.id IN ('a1000000-0000-0000-0000-000000000004', 'a1000000-0000-0000-0000-000000000010') THEN 'OVERDUE'
        ELSE 'PENDING_PIX'
    END,
    due_date_label = CASE
        WHEN rc.id IN ('a1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000005', 'a1000000-0000-0000-0000-000000000008', 'a1000000-0000-0000-0000-000000000011') THEN 'Pago via Pix • Confirmado'
        WHEN rc.id IN ('a1000000-0000-0000-0000-000000000004', 'a1000000-0000-0000-0000-000000000010') THEN 'Vencido em 02/11 • Regularize'
        ELSE 'Vence segunda-feira • 18:00'
    END,
    paid_at = CASE
        WHEN rc.id IN ('a1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000005', 'a1000000-0000-0000-0000-000000000008', 'a1000000-0000-0000-0000-000000000011') THEN NOW() - INTERVAL '2 days'
        ELSE NULL
    END
FROM motorcycles m
WHERE rc.motorcycle_id = m.id;

-- 5. Tabela de Entregas Diárias do Entregador (rider_daily_deliveries)
CREATE TABLE IF NOT EXISTS rider_daily_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rider_id UUID NOT NULL REFERENCES users(id),
    contract_id UUID NOT NULL REFERENCES rental_contracts(id),
    day_label VARCHAR(40) NOT NULL,
    completed_count INTEGER NOT NULL CHECK (completed_count >= 0),
    earnings_brl NUMERIC(10, 2) NOT NULL CHECK (earnings_brl >= 0),
    on_time_rate VARCHAR(10) NOT NULL DEFAULT '98%',
    delivery_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_rider_deliveries_rider ON rider_daily_deliveries(rider_id, delivery_date DESC);

-- Popular 5 dias de entregas diárias do entregador Carlos somando 118 entregas (faltam 2 para o bônus de 120!)
INSERT INTO rider_daily_deliveries (id, rider_id, contract_id, day_label, completed_count, earnings_brl, on_time_rate, delivery_date, created_at)
VALUES
(
    'f1000000-0000-0000-0000-000000000001',
    (SELECT id FROM users WHERE email = 'entregador@mottu-ops.local' LIMIT 1),
    (SELECT id FROM rental_contracts WHERE rider_id = (SELECT id FROM users WHERE email = 'entregador@mottu-ops.local' LIMIT 1) AND active = TRUE LIMIT 1),
    'Segunda-feira',
    24,
    252.00,
    '99%',
    CURRENT_DATE - 4,
    NOW() - INTERVAL '4 days'
),
(
    'f1000000-0000-0000-0000-000000000002',
    (SELECT id FROM users WHERE email = 'entregador@mottu-ops.local' LIMIT 1),
    (SELECT id FROM rental_contracts WHERE rider_id = (SELECT id FROM users WHERE email = 'entregador@mottu-ops.local' LIMIT 1) AND active = TRUE LIMIT 1),
    'Terça-feira',
    26,
    273.00,
    '98%',
    CURRENT_DATE - 3,
    NOW() - INTERVAL '3 days'
),
(
    'f1000000-0000-0000-0000-000000000003',
    (SELECT id FROM users WHERE email = 'entregador@mottu-ops.local' LIMIT 1),
    (SELECT id FROM rental_contracts WHERE rider_id = (SELECT id FROM users WHERE email = 'entregador@mottu-ops.local' LIMIT 1) AND active = TRUE LIMIT 1),
    'Quarta-feira',
    22,
    231.00,
    '97%',
    CURRENT_DATE - 2,
    NOW() - INTERVAL '2 days'
),
(
    'f1000000-0000-0000-0000-000000000004',
    (SELECT id FROM users WHERE email = 'entregador@mottu-ops.local' LIMIT 1),
    (SELECT id FROM rental_contracts WHERE rider_id = (SELECT id FROM users WHERE email = 'entregador@mottu-ops.local' LIMIT 1) AND active = TRUE LIMIT 1),
    'Quinta-feira',
    25,
    262.50,
    '100%',
    CURRENT_DATE - 1,
    NOW() - INTERVAL '1 day'
),
(
    'f1000000-0000-0000-0000-000000000005',
    (SELECT id FROM users WHERE email = 'entregador@mottu-ops.local' LIMIT 1),
    (SELECT id FROM rental_contracts WHERE rider_id = (SELECT id FROM users WHERE email = 'entregador@mottu-ops.local' LIMIT 1) AND active = TRUE LIMIT 1),
    'Hoje (Sexta-feira)',
    21,
    220.50,
    '98%',
    CURRENT_DATE,
    NOW()
)
ON CONFLICT (id) DO NOTHING;

