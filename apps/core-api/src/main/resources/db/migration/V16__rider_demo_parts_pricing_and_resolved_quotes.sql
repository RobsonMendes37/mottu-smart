-- ==============================================================================
-- V16: Rider Demo & Real Rider Parts Pricing, Quantities & Resolved Quotes
-- 1. Unlocks Demo Rider quota (0 open inspections) by resolving Carlos's 3 inspections
--    and moving 2 workshop triage samples to fleet contracts.
-- 2. Populates 2-3 itemized parts per inspection with official Loja Mottu prices
--    so the rider sees exact part counts, unit prices, and amount to prepare to pay.
-- ==============================================================================

-- 1. Ensure additional catalog parts exist for accurate SKU linkage
INSERT INTO catalog_parts (
    id, vtex_sku, motorcycle_model, part_name, category, unit_price_brl,
    is_natural_wear, price_overridden, image_url, stock_quantity, min_stock_quantity, aisle_bin, updated_at
) VALUES
(
    '77777777-7777-7777-7777-777777777775',
    'SKU-PROT-ESCAP-SPORT',
    'MOTTU_SPORT_110I',
    'Protetor Lateral do Escapamento',
    'Carenagem & Chassi',
    38.00,
    FALSE,
    FALSE,
    '/demo-photos/avaria-escapamento.webp',
    12,
    8,
    'Corredor D • Gaveta 04',
    NOW()
),
(
    '77777777-7777-7777-7777-777777777776',
    'SKU-FAROL-SPORT',
    'MOTTU_SPORT_110I',
    'Bloco Óptico do Farol Dianteiro',
    'Iluminação & Painel',
    130.00,
    FALSE,
    FALSE,
    '/demo-photos/avaria-farol.webp',
    9,
    8,
    'Corredor B • Gaveta 05',
    NOW()
)
ON CONFLICT (vtex_sku, motorcycle_model) DO NOTHING;

-- 2. Move 2 HUMAN_TRIAGE inspections to secondary fleet contracts so Mechanic Queue (T05)
--    stays populated while Carlos Entregador (Demo) has 0 open inspections (quota unlocked)
UPDATE inspections
SET contract_id = 'a1000000-0000-0000-0000-000000000001',
    rider_id = 'e1000000-0000-0000-0000-000000000001',
    motorcycle_id = 'b1000000-0000-0000-0000-000000000001'
WHERE id = 'd2000000-0000-0000-0000-000000000002'
  AND EXISTS (SELECT 1 FROM rental_contracts WHERE id = 'a1000000-0000-0000-0000-000000000001');

UPDATE inspections
SET contract_id = 'a1000000-0000-0000-0000-000000000002',
    rider_id = 'e1000000-0000-0000-0000-000000000002',
    motorcycle_id = 'b1000000-0000-0000-0000-000000000002'
WHERE id = 'd2000000-0000-0000-0000-000000000005'
  AND EXISTS (SELECT 1 FROM rental_contracts WHERE id = 'a1000000-0000-0000-0000-000000000002');

-- 3. Resolve Carlos Entregador's 3 main inspections with transparent multi-part quotes
-- Vistoria #1: AUTO_RESOLVED (MAU_USO) -> 3 peças (R$ 42,50 + R$ 25,00 + R$ 38,00 = R$ 105,50) + R$ 35,00 oficina = R$ 140,50
UPDATE inspections
SET status = 'AUTO_RESOLVED',
    final_verdict = 'MAU_USO',
    ai_verdict = 'MAU_USO',
    ai_confidence = 0.940,
    labor_fee_brl = 35.00,
    total_estimated_brl = 140.50,
    contested_once = FALSE,
    customer_feedback_text = 'Identificamos 3 peças com avaria por queda lateral (Retrovisor Direito, Manete de Freio e Protetor de Escapamento). Valores calculados pela tabela oficial da Loja Mottu para sua previsibilidade.',
    created_at = NOW() - INTERVAL '25 minutes',
    resolved_at = NOW() - INTERVAL '24 minutes'
WHERE id = 'd2000000-0000-0000-0000-000000000001';

-- Vistoria #2: AUTO_RESOLVED (DESGASTE_NATURAL) -> 2 peças (R$ 180,00 + R$ 33,00 = R$ 213,00 cobertos) -> Total a pagar: R$ 0,00
UPDATE inspections
SET status = 'AUTO_RESOLVED',
    final_verdict = 'DESGASTE_NATURAL',
    ai_verdict = 'DESGASTE_NATURAL',
    ai_confidence = 0.965,
    labor_fee_brl = 0.00,
    total_estimated_brl = 0.00,
    contested_once = FALSE,
    customer_feedback_text = 'Desgaste natural confirmado em 2 itens (Pneu Traseiro e Lona de Freio). Cobertura de 100% pelo plano Mottu: você economizou R$ 248,00 e não paga nada (R$ 0,00).',
    created_at = NOW() - INTERVAL '3 hours',
    resolved_at = NOW() - INTERVAL '3 hours' + INTERVAL '5 seconds'
WHERE id = 'd2000000-0000-0000-0000-000000000003';

-- Vistoria #3: HUMAN_RESOLVED (MAU_USO) -> 2 peças (R$ 130,00 + R$ 25,00 = R$ 155,00) + R$ 35,00 oficina = R$ 190,00
UPDATE inspections
SET status = 'HUMAN_RESOLVED',
    final_verdict = 'MAU_USO',
    ai_verdict = 'MAU_USO',
    ai_confidence = 0.950,
    labor_fee_brl = 35.00,
    total_estimated_brl = 190.00,
    contested_once = FALSE,
    reviewed_by_mechanic_id = 'e0000000-0000-0000-0000-000000000002',
    mechanic_notes = 'Confirmada necessidade de troca do bloco óptico e manete direito na bancada.',
    customer_feedback_text = 'Avaliação concluída pela oficina: 2 peças listadas (Bloco Óptico do Farol e Manete de Freio Direito). Orçamento transparente pronto para agendamento.',
    created_at = NOW() - INTERVAL '1 day',
    resolved_at = NOW() - INTERVAL '23 hours'
WHERE id = 'd2000000-0000-0000-0000-000000000004';

-- 4. Rebuild inspection_items for Carlos's 3 inspections so each has 2 to 3 itemized parts
DELETE FROM inspection_items
WHERE inspection_id IN (
    'd2000000-0000-0000-0000-000000000001',
    'd2000000-0000-0000-0000-000000000003',
    'd2000000-0000-0000-0000-000000000004'
);

INSERT INTO inspection_items (id, inspection_id, catalog_part_id, part_name_snapshot, unit_price_snapshot_brl, added_by)
VALUES
-- Vistoria #1 (3 peças = R$ 105,50 + R$ 35,00 mão de obra = R$ 140,50)
('f1600000-0000-0000-0000-000000000001', 'd2000000-0000-0000-0000-000000000001', '77777777-7777-7777-7777-777777777773', 'Retrovisor Direito Sport', 42.50, 'AI'),
('f1600000-0000-0000-0000-000000000002', 'd2000000-0000-0000-0000-000000000001', '77777777-7777-7777-7777-777777777771', 'Manete de Freio Direito', 25.00, 'AI'),
('f1600000-0000-0000-0000-000000000003', 'd2000000-0000-0000-0000-000000000001', '77777777-7777-7777-7777-777777777775', 'Protetor Lateral do Escapamento', 38.00, 'AI'),

-- Vistoria #2 (2 peças de desgaste natural = R$ 213,00 cobertos -> R$ 0,00 cobrado)
('f1600000-0000-0000-0000-000000000004', 'd2000000-0000-0000-0000-000000000003', '77777777-7777-7777-7777-777777777774', 'Pneu Traseiro 80/100-14', 180.00, 'AI'),
('f1600000-0000-0000-0000-000000000005', 'd2000000-0000-0000-0000-000000000003', '77777777-7777-7777-7777-777777777772', 'Lona de Freio Traseira', 33.00, 'AI'),

-- Vistoria #3 (2 peças avaliadas pela oficina = R$ 155,00 + R$ 35,00 mão de obra = R$ 190,00)
('f1600000-0000-0000-0000-000000000006', 'd2000000-0000-0000-0000-000000000004', '77777777-7777-7777-7777-777777777776', 'Bloco Óptico do Farol Dianteiro', 130.00, 'MECHANIC'),
('f1600000-0000-0000-0000-000000000007', 'd2000000-0000-0000-0000-000000000004', '77777777-7777-7777-7777-777777777771', 'Manete de Freio Direito', 25.00, 'AI')
ON CONFLICT (id) DO NOTHING;

-- 5. Seed resolved multi-part inspections for Rafael Entregador (Vida Real) as well
INSERT INTO inspections (
    id, idempotency_key, contract_id, rider_id, motorcycle_id,
    photo_object_key, photo_sha256, fraud_suspected,
    reported_context, safety_risk, reported_odometer_km, rider_notes,
    status, priority_weight, priority_reason,
    ai_confidence, ai_verdict, ai_technical_summary, final_verdict,
    labor_fee_brl, total_estimated_brl, customer_feedback_text,
    contested_once, created_at, resolved_at
)
SELECT
    'd3000000-0000-0000-0000-000000000001'::uuid,
    'seed-v16-real-rider-mau-uso',
    'a2000000-0000-0000-0000-000000000001'::uuid,
    'e2000000-0000-0000-0000-000000000003'::uuid,
    'b2000000-0000-0000-0000-000000000001'::uuid,
    'inspections/sample-retrovisor.webp',
    '1111222233334444555566667777888899990000aaaabbbbccccddddeeee0001',
    FALSE,
    'ACCIDENT_OR_DROP',
    'SAFE_TO_RIDE',
    3150,
    'Moto escorregou ao estacionar na chuva; retrovisor e manete direitos avariados.',
    'AUTO_RESOLVED',
    4,
    'LOW_AI_CONFIDENCE',
    0.945,
    'MAU_USO',
    'Identificados 2 componentes avariados por tombamento lateral: Retrovisor Direito e Manete de Freio.',
    'MAU_USO',
    35.00,
    102.50,
    'Identificamos 2 peças para reposição (Retrovisor Direito Sport R$ 42,50 e Manete de Freio Direito R$ 25,00) + mão de obra tabelada de R$ 35,00.',
    FALSE,
    NOW() - INTERVAL '40 minutes',
    NOW() - INTERVAL '39 minutes'
WHERE EXISTS (SELECT 1 FROM rental_contracts WHERE id = 'a2000000-0000-0000-0000-000000000001')
ON CONFLICT (id) DO NOTHING;

INSERT INTO inspections (
    id, idempotency_key, contract_id, rider_id, motorcycle_id,
    photo_object_key, photo_sha256, fraud_suspected,
    reported_context, safety_risk, reported_odometer_km, rider_notes,
    status, priority_weight, priority_reason,
    ai_confidence, ai_verdict, ai_technical_summary, final_verdict,
    labor_fee_brl, total_estimated_brl, customer_feedback_text,
    contested_once, created_at, resolved_at
)
SELECT
    'd3000000-0000-0000-0000-000000000002'::uuid,
    'seed-v16-real-rider-isento',
    'a2000000-0000-0000-0000-000000000001'::uuid,
    'e2000000-0000-0000-0000-000000000003'::uuid,
    'b2000000-0000-0000-0000-000000000001'::uuid,
    'inspections/sample-pneu.webp',
    '1111222233334444555566667777888899990000aaaabbbbccccddddeeee0002',
    FALSE,
    'NORMAL_USE_WEAR',
    'SAFE_TO_RIDE',
    3120,
    'Revisão preventiva de pneu traseiro e lona de freio.',
    'AUTO_RESOLVED',
    4,
    'LOW_AI_CONFIDENCE',
    0.960,
    'DESGASTE_NATURAL',
    'Desgaste natural compatível com quilometragem do contrato.',
    'DESGASTE_NATURAL',
    0.00,
    0.00,
    '2 peças avaliadas como desgaste natural (Pneu Traseiro e Lona de Freio). Custo R$ 0,00 coberto pelo plano Mottu.',
    FALSE,
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days' + INTERVAL '4 seconds'
WHERE EXISTS (SELECT 1 FROM rental_contracts WHERE id = 'a2000000-0000-0000-0000-000000000001')
ON CONFLICT (id) DO NOTHING;

INSERT INTO inspection_items (id, inspection_id, catalog_part_id, part_name_snapshot, unit_price_snapshot_brl, added_by)
SELECT 'f1600000-0000-0000-0000-000000000008'::uuid, 'd3000000-0000-0000-0000-000000000001'::uuid, '77777777-7777-7777-7777-777777777773'::uuid, 'Retrovisor Direito Sport', 42.50, 'AI'
WHERE EXISTS (SELECT 1 FROM inspections WHERE id = 'd3000000-0000-0000-0000-000000000001')
ON CONFLICT (id) DO NOTHING;

INSERT INTO inspection_items (id, inspection_id, catalog_part_id, part_name_snapshot, unit_price_snapshot_brl, added_by)
SELECT 'f1600000-0000-0000-0000-000000000009'::uuid, 'd3000000-0000-0000-0000-000000000001'::uuid, '77777777-7777-7777-7777-777777777771'::uuid, 'Manete de Freio Direito', 25.00, 'AI'
WHERE EXISTS (SELECT 1 FROM inspections WHERE id = 'd3000000-0000-0000-0000-000000000001')
ON CONFLICT (id) DO NOTHING;

INSERT INTO inspection_items (id, inspection_id, catalog_part_id, part_name_snapshot, unit_price_snapshot_brl, added_by)
SELECT 'f1600000-0000-0000-0000-000000000010'::uuid, 'd3000000-0000-0000-0000-000000000002'::uuid, '77777777-7777-7777-7777-777777777774'::uuid, 'Pneu Traseiro 80/100-14', 180.00, 'AI'
WHERE EXISTS (SELECT 1 FROM inspections WHERE id = 'd3000000-0000-0000-0000-000000000002')
ON CONFLICT (id) DO NOTHING;

INSERT INTO inspection_items (id, inspection_id, catalog_part_id, part_name_snapshot, unit_price_snapshot_brl, added_by)
SELECT 'f1600000-0000-0000-0000-000000000011'::uuid, 'd3000000-0000-0000-0000-000000000002'::uuid, '77777777-7777-7777-7777-777777777772'::uuid, 'Lona de Freio Traseira', 33.00, 'AI'
WHERE EXISTS (SELECT 1 FROM inspections WHERE id = 'd3000000-0000-0000-0000-000000000002')
ON CONFLICT (id) DO NOTHING;

