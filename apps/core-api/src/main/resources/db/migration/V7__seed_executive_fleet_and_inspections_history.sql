-- ==============================================================================
-- V7: Executive Fleet & Historical Inspections Seed for Analytics & Fleet Views
-- Populates active rented fleet across all 4 motorcycle models and 72 resolved
-- historical inspections (~79% autonomous AI deflection) without touching V2 IDs.
-- ==============================================================================

-- 1. Additional Operational Yards
INSERT INTO yards (id, name, city, address, active) VALUES
('11111111-1111-1111-1111-111111111113', 'Galpão Mottu Interlagos', 'São Paulo', 'Av. Interlagos, 3400', TRUE),
('11111111-1111-1111-1111-111111111114', 'Galpão Mottu Lapa', 'São Paulo', 'Rua Guaicurus, 1250', TRUE)
ON CONFLICT (id) DO NOTHING;

-- 2. Additional Active Riders for Fleet Contracts
INSERT INTO users (id, full_name, cpf, phone, email, password_hash, role, status, approved_by, created_at) VALUES
('e1000000-0000-0000-0000-000000000001', 'Rafael Souza Silva', '41122233301', '11988010001', 'rafael.silva@mottu-ops.local', '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'RIDER', 'ACTIVE', 'e0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '45 days'),
('e1000000-0000-0000-0000-000000000002', 'Lucas Ferreira Santos', '41122233302', '11988010002', 'lucas.santos@mottu-ops.local', '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'RIDER', 'ACTIVE', 'e0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '40 days'),
('e1000000-0000-0000-0000-000000000003', 'Diego Oliveira Costa', '41122233303', '11988010003', 'diego.costa@mottu-ops.local', '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'RIDER', 'ACTIVE', 'e0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '38 days'),
('e1000000-0000-0000-0000-000000000004', 'Bruno Almeida Lima', '41122233304', '11988010004', 'bruno.lima@mottu-ops.local', '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'RIDER', 'ACTIVE', 'e0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '35 days'),
('e1000000-0000-0000-0000-000000000005', 'Thiago Pereira Rocha', '41122233305', '11988010005', 'thiago.rocha@mottu-ops.local', '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'RIDER', 'ACTIVE', 'e0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '32 days'),
('e1000000-0000-0000-0000-000000000006', 'Felipe Ribeiro Gomes', '41122233306', '11988010006', 'felipe.gomes@mottu-ops.local', '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'RIDER', 'ACTIVE', 'e0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '30 days'),
('e1000000-0000-0000-0000-000000000007', 'Gabriel Martins Dias', '41122233307', '11988010007', 'gabriel.dias@mottu-ops.local', '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'RIDER', 'ACTIVE', 'e0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '28 days'),
('e1000000-0000-0000-0000-000000000008', 'Rodrigo Barbosa Alves', '41122233308', '11988010008', 'rodrigo.alves@mottu-ops.local', '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'RIDER', 'ACTIVE', 'e0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '25 days'),
('e1000000-0000-0000-0000-000000000009', 'Marcelo Cardoso Nunes', '41122233309', '11988010009', 'marcelo.nunes@mottu-ops.local', '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'RIDER', 'ACTIVE', 'e0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '22 days'),
('e1000000-0000-0000-0000-000000000010', 'André Teixeira Mendes', '41122233310', '11988010010', 'andre.mendes@mottu-ops.local', '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'RIDER', 'ACTIVE', 'e0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '20 days'),
('e1000000-0000-0000-0000-000000000011', 'Leandro Araújo Castro', '41122233311', '11988010011', 'leandro.castro@mottu-ops.local', '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'RIDER', 'ACTIVE', 'e0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '18 days'),
('e1000000-0000-0000-0000-000000000012', 'Vinícius Moreira Pinto', '41122233312', '11988010012', 'vinicius.pinto@mottu-ops.local', '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'RIDER', 'ACTIVE', 'e0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '15 days'),
('e1000000-0000-0000-0000-000000000013', 'Gustavo Cavalcanti Reis', '41122233313', '11988010013', 'gustavo.reis@mottu-ops.local', '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'RIDER', 'ACTIVE', 'e0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '14 days'),
('e1000000-0000-0000-0000-000000000014', 'Fernando Correia Lopes', '41122233314', '11988010014', 'fernando.lopes@mottu-ops.local', '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'RIDER', 'ACTIVE', 'e0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '12 days'),
('e1000000-0000-0000-0000-000000000015', 'Henrique Monteiro Farias', '41122233315', '11988010015', 'henrique.farias@mottu-ops.local', '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'RIDER', 'ACTIVE', 'e0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '10 days'),
('e1000000-0000-0000-0000-000000000016', 'Eduardo Vieira Ramos', '41122233316', '11988010016', 'eduardo.ramos@mottu-ops.local', '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'RIDER', 'ACTIVE', 'e0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '8 days')
ON CONFLICT (id) DO NOTHING;

-- 3. Additional Operational Fleet (16 Rented + 4 Available across all 4 models)
INSERT INTO motorcycles (id, plate, model, yard_id, status, current_odometer_km, created_at) VALUES
('b1000000-0000-0000-0000-000000000001', 'MOT5E11', 'MOTTU_SPORT_110I', '11111111-1111-1111-1111-111111111111', 'RENTED', 4280, NOW() - INTERVAL '40 days'),
('b1000000-0000-0000-0000-000000000002', 'MOT5E12', 'MOTTU_SPORT_110I', '11111111-1111-1111-1111-111111111111', 'RENTED', 6120, NOW() - INTERVAL '38 days'),
('b1000000-0000-0000-0000-000000000003', 'MOT5E13', 'MOTTU_SPORT_110I', '22222222-2222-2222-2222-222222222222', 'RENTED', 3190, NOW() - INTERVAL '35 days'),
('b1000000-0000-0000-0000-000000000004', 'MOT5E14', 'MOTTU_SPORT_110I', '11111111-1111-1111-1111-111111111113', 'RENTED', 8450, NOW() - INTERVAL '30 days'),
('b1000000-0000-0000-0000-000000000005', 'MOT5E15', 'MOTTU_SPORT_110I', '11111111-1111-1111-1111-111111111114', 'RENTED', 2140, NOW() - INTERVAL '28 days'),
('b1000000-0000-0000-0000-000000000006', 'POP6F21', 'HONDA_POP_110I', '11111111-1111-1111-1111-111111111111', 'RENTED', 5340, NOW() - INTERVAL '36 days'),
('b1000000-0000-0000-0000-000000000007', 'POP6F22', 'HONDA_POP_110I', '22222222-2222-2222-2222-222222222222', 'RENTED', 7890, NOW() - INTERVAL '34 days'),
('b1000000-0000-0000-0000-000000000008', 'POP6F23', 'HONDA_POP_110I', '11111111-1111-1111-1111-111111111113', 'RENTED', 4110, NOW() - INTERVAL '29 days'),
('b1000000-0000-0000-0000-000000000009', 'POP6F24', 'HONDA_POP_110I', '11111111-1111-1111-1111-111111111114', 'RENTED', 1980, NOW() - INTERVAL '24 days'),
('b1000000-0000-0000-0000-000000000010', 'RAI7G31', 'TVS_RAIDER_125', '11111111-1111-1111-1111-111111111111', 'RENTED', 3670, NOW() - INTERVAL '32 days'),
('b1000000-0000-0000-0000-000000000011', 'RAI7G32', 'TVS_RAIDER_125', '22222222-2222-2222-2222-222222222222', 'RENTED', 4920, NOW() - INTERVAL '27 days'),
('b1000000-0000-0000-0000-000000000012', 'RAI7G33', 'TVS_RAIDER_125', '11111111-1111-1111-1111-111111111113', 'RENTED', 2850, NOW() - INTERVAL '21 days'),
('b1000000-0000-0000-0000-000000000013', 'APA8H41', 'TVS_APACHE_160', '11111111-1111-1111-1111-111111111111', 'RENTED', 6430, NOW() - INTERVAL '33 days'),
('b1000000-0000-0000-0000-000000000014', 'APA8H42', 'TVS_APACHE_160', '22222222-2222-2222-2222-222222222222', 'RENTED', 5120, NOW() - INTERVAL '26 days'),
('b1000000-0000-0000-0000-000000000015', 'APA8H43', 'TVS_APACHE_160', '11111111-1111-1111-1111-111111111113', 'RENTED', 3380, NOW() - INTERVAL '19 days'),
('b1000000-0000-0000-0000-000000000016', 'MOT5E16', 'MOTTU_SPORT_110I', '11111111-1111-1111-1111-111111111111', 'RENTED', 1540, NOW() - INTERVAL '15 days'),
('b1000000-0000-0000-0000-000000000017', 'RAI7G34', 'TVS_RAIDER_125', '11111111-1111-1111-1111-111111111111', 'AVAILABLE', 890, NOW() - INTERVAL '10 days'),
('b1000000-0000-0000-0000-000000000018', 'POP6F25', 'HONDA_POP_110I', '11111111-1111-1111-1111-111111111113', 'AVAILABLE', 410, NOW() - INTERVAL '8 days')
ON CONFLICT (plate) DO NOTHING;

-- 4. Active Rental Contracts for the 16 Rented Fleet Motorcycles
INSERT INTO rental_contracts (id, rider_id, motorcycle_id, picked_up_at, initial_odometer_km, ended_at, active) VALUES
('a1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', NOW() - INTERVAL '30 days', 1200, NULL, TRUE),
('a1000000-0000-0000-0000-000000000002', 'e1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000002', NOW() - INTERVAL '28 days', 2400, NULL, TRUE),
('a1000000-0000-0000-0000-000000000003', 'e1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000003', NOW() - INTERVAL '25 days', 1100, NULL, TRUE),
('a1000000-0000-0000-0000-000000000004', 'e1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000004', NOW() - INTERVAL '22 days', 4500, NULL, TRUE),
('a1000000-0000-0000-0000-000000000005', 'e1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000005', NOW() - INTERVAL '20 days', 800, NULL, TRUE),
('a1000000-0000-0000-0000-000000000006', 'e1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000006', NOW() - INTERVAL '18 days', 3100, NULL, TRUE),
('a1000000-0000-0000-0000-000000000007', 'e1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000007', NOW() - INTERVAL '16 days', 5200, NULL, TRUE),
('a1000000-0000-0000-0000-000000000008', 'e1000000-0000-0000-0000-000000000008', 'b1000000-0000-0000-0000-000000000008', NOW() - INTERVAL '14 days', 2200, NULL, TRUE),
('a1000000-0000-0000-0000-000000000009', 'e1000000-0000-0000-0000-000000000009', 'b1000000-0000-0000-0000-000000000009', NOW() - INTERVAL '12 days', 900, NULL, TRUE),
('a1000000-0000-0000-0000-000000000010', 'e1000000-0000-0000-0000-000000000010', 'b1000000-0000-0000-0000-000000000010', NOW() - INTERVAL '11 days', 1800, NULL, TRUE),
('a1000000-0000-0000-0000-000000000011', 'e1000000-0000-0000-0000-000000000011', 'b1000000-0000-0000-0000-000000000011', NOW() - INTERVAL '10 days', 2900, NULL, TRUE),
('a1000000-0000-0000-0000-000000000012', 'e1000000-0000-0000-0000-000000000012', 'b1000000-0000-0000-0000-000000000012', NOW() - INTERVAL '9 days', 1400, NULL, TRUE),
('a1000000-0000-0000-0000-000000000013', 'e1000000-0000-0000-0000-000000000013', 'b1000000-0000-0000-0000-000000000013', NOW() - INTERVAL '8 days', 4100, NULL, TRUE),
('a1000000-0000-0000-0000-000000000014', 'e1000000-0000-0000-0000-000000000014', 'b1000000-0000-0000-0000-000000000014', NOW() - INTERVAL '7 days', 3300, NULL, TRUE),
('a1000000-0000-0000-0000-000000000015', 'e1000000-0000-0000-0000-000000000015', 'b1000000-0000-0000-0000-000000000015', NOW() - INTERVAL '6 days', 1900, NULL, TRUE),
('a1000000-0000-0000-0000-000000000016', 'e1000000-0000-0000-0000-000000000016', 'b1000000-0000-0000-0000-000000000016', NOW() - INTERVAL '2 days', 1500, NULL, TRUE)
ON CONFLICT (id) DO NOTHING;

-- 5. Historical Resolved Inspections (72 inspections: 57 AUTO_RESOLVED + 15 HUMAN_RESOLVED = 79.2% AI Deflection)
INSERT INTO inspections (
    id, idempotency_key, contract_id, rider_id, motorcycle_id,
    photo_object_key, photo_sha256, fraud_suspected,
    reported_context, safety_risk, reported_odometer_km, rider_notes,
    status, priority_weight, priority_reason,
    ai_confidence, ai_verdict, ai_technical_summary, final_verdict,
    labor_fee_brl, total_estimated_brl, customer_feedback_text,
    contested_once, reviewed_by_mechanic_id, mechanic_notes,
    created_at, resolved_at
)
SELECT
    ('d1000000-0000-0000-0000-' || LPAD(seq::text, 12, '0'))::uuid AS id,
    'hist-exec-seed-v7-' || seq AS idempotency_key,
    ('a1000000-0000-0000-0000-' || LPAD((1 + (seq % 16))::text, 12, '0'))::uuid AS contract_id,
    ('e1000000-0000-0000-0000-' || LPAD((1 + (seq % 16))::text, 12, '0'))::uuid AS rider_id,
    ('b1000000-0000-0000-0000-' || LPAD((1 + (seq % 16))::text, 12, '0'))::uuid AS motorcycle_id,
    'inspections/history/sample-' || seq || '.webp' AS photo_object_key,
    LPAD(md5('mottu-hist-sha256-' || seq::text) || md5('salt-' || seq::text), 64, '0') AS photo_sha256,
    FALSE AS fraud_suspected,
    CASE
        WHEN seq <= 35 THEN 'USO_NORMAL'
        WHEN seq <= 57 THEN 'JA_RECEBI_ASSIM'
        ELSE 'QUEDA_BATIDA'
    END AS reported_context,
    CASE
        WHEN seq <= 57 THEN 'LOW'
        WHEN seq <= 68 THEN 'MEDIUM'
        ELSE 'HIGH'
    END AS safety_risk,
    2500 + (seq * 45) AS reported_odometer_km,
    CASE
        WHEN seq <= 35 THEN 'Desgaste natural identificado durante turno de entregas.'
        WHEN seq <= 57 THEN 'Avaria estética superficial registrada na janela de retirada.'
        ELSE 'Queda lateral em baixa velocidade com avaria em comando.'
    END AS rider_notes,
    CASE WHEN seq <= 57 THEN 'AUTO_RESOLVED' ELSE 'HUMAN_RESOLVED' END AS status,
    CASE WHEN seq <= 57 THEN 4 ELSE 2 END AS priority_weight,
    CASE WHEN seq <= 57 THEN 'NORMAL_QUEUE' ELSE 'LOW_AI_CONFIDENCE' END AS priority_reason,
    CASE WHEN seq <= 57 THEN 0.920 ELSE 0.740 END AS ai_confidence,
    CASE
        WHEN seq <= 35 THEN 'DESGASTE_NATURAL'
        WHEN seq <= 57 THEN 'PRE_EXISTENTE'
        ELSE 'MAU_USO'
    END AS ai_verdict,
    'Análise multimodal concluída via Gemini Vision + CRAG pgvector.' AS ai_technical_summary,
    CASE
        WHEN seq <= 35 THEN 'DESGASTE_NATURAL'
        WHEN seq <= 57 THEN 'PRE_EXISTENTE'
        ELSE 'MAU_USO'
    END AS final_verdict,
    CASE WHEN seq <= 57 THEN 0.00 ELSE 35.00 END AS labor_fee_brl,
    CASE WHEN seq <= 57 THEN 0.00 ELSE 98.00 END AS total_estimated_brl,
    'Laudo processado com transparência pela tabela oficial da Loja Mottu.' AS customer_feedback_text,
    FALSE AS contested_once,
    CASE WHEN seq <= 57 THEN NULL ELSE 'e0000000-0000-0000-0000-000000000002'::uuid END AS reviewed_by_mechanic_id,
    CASE WHEN seq <= 57 THEN NULL ELSE 'Confirmado na bancada física conforme tabela oficial.' END AS mechanic_notes,
    NOW() - ((seq * 3) || ' hours')::interval AS created_at,
    CASE
        WHEN seq <= 57 THEN NOW() - ((seq * 3) || ' hours')::interval + INTERVAL '4 seconds'
        ELSE NOW() - ((seq * 3) || ' hours')::interval + INTERVAL '25 minutes'
    END AS resolved_at
FROM generate_series(1, 72) AS seq
ON CONFLICT (id) DO NOTHING;
