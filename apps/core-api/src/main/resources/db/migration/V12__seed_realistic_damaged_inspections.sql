-- ==============================================================================
-- V12: Realistic Damaged Motorcycle Inspection Photos & Mechanic Triage Queue Seed
-- Provides authentic high-resolution damage photos (Mirror, Lever, Tire, Headlight, Exhaust)
-- ==============================================================================

-- 1. Atualizar vistorias ativas existentes para apontar para fotos reais de avaria
UPDATE inspections
SET photo_object_key = 'inspections/sample-retrovisor.webp'
WHERE photo_object_key LIKE '%633cc57e%'
   OR photo_object_key LIKE '%sample-%';

-- 2. Inserir vistorias realistas com fotos de alta definição na fila da oficina
INSERT INTO inspections (
    id, idempotency_key, contract_id, rider_id, motorcycle_id,
    photo_object_key, photo_sha256, fraud_suspected,
    reported_context, safety_risk, reported_odometer_km, rider_notes,
    status, priority_weight, priority_reason,
    ai_confidence, ai_verdict, ai_technical_summary, final_verdict,
    labor_fee_brl, total_estimated_brl, customer_feedback_text,
    contested_once, created_at
)
VALUES
(
    'd2000000-0000-0000-0000-000000000001',
    'seed-v12-damaged-retrovisor',
    'a0000000-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000003',
    '33333333-3333-3333-3333-333333333333',
    'inspections/sample-retrovisor.webp',
    'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    FALSE,
    'ACCIDENT_OR_DROP',
    'SAFE_TO_RIDE',
    1840,
    'Ao desviar de buraco na chuva a moto tombou na calçada e quebrou o retrovisor direito.',
    'HUMAN_TRIAGE',
    2,
    'RIDER_CONTESTATION',
    0.940,
    'MAU_USO',
    'Detecção visual de impacto no guidão e lateral. Retrovisor direito quebrado na articulação com espelho estilhaçado e carenagem lateral raspada por tombamento. Compatível com queda lateral em baixa velocidade.',
    'MAU_USO',
    35.00,
    77.50,
    'Identificamos avaria por queda lateral no retrovisor direito. Orçamento transparente calculado com peças de reposição e mão de obra padronizada.',
    TRUE,
    NOW() - INTERVAL '35 minutes'
),
(
    'd2000000-0000-0000-0000-000000000002',
    'seed-v12-damaged-manete',
    'a0000000-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000003',
    '44444444-4444-4444-4444-444444444444',
    'inspections/sample-manete.webp',
    'a1b2c3d4e5f60718293a4b5c6d7e8f90123456789abcdef0123456789abcdef0',
    FALSE,
    'NORMAL_USE_WEAR',
    'SAFE_TO_RIDE',
    2150,
    'A ponta do manete de freio quebrou após encostar no portão durante manobra.',
    'HUMAN_TRIAGE',
    3,
    'LOW_AI_CONFIDENCE',
    0.910,
    'MAU_USO',
    'Fratura completa na haste do manete de freio dianteiro direito com ponto de metal exposto e ranhuras no contra-peso do guidão.',
    'MAU_USO',
    35.00,
    60.00,
    'Manete de freio dianteiro danificado. Substituição necessária para garantir a segurança na frenagem diária.',
    FALSE,
    NOW() - INTERVAL '20 minutes'
),
(
    'd2000000-0000-0000-0000-000000000003',
    'seed-v12-damaged-pneu',
    'a0000000-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000003',
    '55555555-5555-5555-5555-555555555555',
    'inspections/sample-pneu.webp',
    'b2c3d4e5f6a10718293a4b5c6d7e8f90123456789abcdef0123456789abcdef1',
    FALSE,
    'NORMAL_USE_WEAR',
    'SAFE_TO_RIDE',
    8920,
    'Pneu traseiro já atingiu o limite TWI da borracha após semanas intensas de entregas.',
    'HUMAN_TRIAGE',
    3,
    'LOW_AI_CONFIDENCE',
    0.965,
    'DESGASTE_NATURAL',
    'Banda de rodagem do pneu traseiro totalmente desgastada e careca por quilometragem percorrida. Desgaste simétrico compatível com uso normal de tráfego urbano. Coberto pelo plano.',
    'DESGASTE_NATURAL',
    0.00,
    0.00,
    'Desgaste natural de pneu aprovado. Custo R$ 0,00 coberto pelo plano de manutenção preventiva Mottu.',
    FALSE,
    NOW() - INTERVAL '15 minutes'
),
(
    'd2000000-0000-0000-0000-000000000004',
    'seed-v12-damaged-farol',
    'a0000000-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000003',
    '66666666-6666-6666-6666-666666666666',
    'inspections/sample-farol.webp',
    'c3d4e5f6a1b20718293a4b5c6d7e8f90123456789abcdef0123456789abcdef2',
    FALSE,
    'ACCIDENT_OR_DROP',
    'CRITICAL_STOPPED',
    3420,
    'Pedra solta de caminhão na rodovia atingiu o farol dianteiro e trincou toda a lente.',
    'HUMAN_TRIAGE',
    1,
    'CRITICAL_SAFETY',
    0.950,
    'MAU_USO',
    'Bloco óptico frontal estilhaçado com trincas em teia de aranha e desencaixe da carcaça frontal. Risco imediato à segurança noturna (P1). Bloqueio preventivo.',
    'MAU_USO',
    35.00,
    165.00,
    'Farol danificado com risco de segurança operacional. Troca prioritária na oficina autorizada.',
    FALSE,
    NOW() - INTERVAL '10 minutes'
),
(
    'd2000000-0000-0000-0000-000000000005',
    'seed-v12-damaged-escapamento',
    'a0000000-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000003',
    '33333333-3333-3333-3333-333333333333',
    'inspections/sample-escapamento.webp',
    'd4e5f6a1b2c30718293a4b5c6d7e8f90123456789abcdef0123456789abcdef3',
    FALSE,
    'ACCIDENT_OR_DROP',
    'SAFE_TO_RIDE',
    4100,
    'Queda lateral ao manobrar em rampa. Escapamento ralado no chão.',
    'HUMAN_TRIAGE',
    1,
    'CRITICAL_SAFETY',
    0.890,
    'MAU_USO',
    'Abrasão superficial e marcas profundas de atrito no protetor metálico de escapamento com pedaleira lateral levemente entortada.',
    'MAU_USO',
    35.00,
    73.00,
    'Avaria no escapamento e pedaleira. Agendamento disponível para troca do protetor.',
    FALSE,
    NOW() - INTERVAL '5 minutes'
)
ON CONFLICT (id) DO NOTHING;

-- 3. Vincular peças de reposição reais para cada uma das vistorias
INSERT INTO inspection_items (inspection_id, catalog_part_id, part_name_snapshot, unit_price_snapshot_brl, added_by)
VALUES
-- Itens da Vistoria 1 (Retrovisor)
('d2000000-0000-0000-0000-000000000001', '77777777-7777-7777-7777-777777777773', 'Retrovisor Direito Sport', 42.50, 'AI'),
-- Itens da Vistoria 2 (Manete)
('d2000000-0000-0000-0000-000000000002', '77777777-7777-7777-7777-777777777771', 'Manete de Freio Direito', 25.00, 'AI'),
-- Itens da Vistoria 3 (Pneu Traseiro)
('d2000000-0000-0000-0000-000000000003', '77777777-7777-7777-7777-777777777774', 'Pneu Traseiro 80/100-14', 180.00, 'AI'),
-- Itens da Vistoria 4 (Farol)
('d2000000-0000-0000-0000-000000000004', '77777777-7777-7777-7777-777777777771', 'Bloco Óptico do Farol Dianteiro', 130.00, 'AI'),
-- Itens da Vistoria 5 (Escapamento)
('d2000000-0000-0000-0000-000000000005', '77777777-7777-7777-7777-777777777771', 'Protetor Lateral do Escapamento', 38.00, 'AI')
ON CONFLICT (id) DO NOTHING;

-- 4. Atualizar fotos de sinistros de acidentes para usar fotos realistas de quedas
UPDATE accident_report_photos
SET photo_object_key = 'inspections/sample-escapamento.webp'
WHERE photo_object_key LIKE '%dummy%' OR photo_object_key LIKE '%sample%';
