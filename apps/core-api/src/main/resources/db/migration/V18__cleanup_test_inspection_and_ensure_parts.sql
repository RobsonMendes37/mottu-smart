-- ==============================================================================
-- V18: Ensure all Demo & Real Rider inspections have 2-3 priced parts
--      and resolve any residual HUMAN_TRIAGE / QUEUED_AI test records on Demo contract
-- ==============================================================================

-- 1. Resolve any open inspections on Demo (Carlos) & Real (Rafael) contracts
UPDATE inspections
SET status = 'AUTO_RESOLVED',
    final_verdict = COALESCE(final_verdict, 'MAU_USO'),
    ai_verdict = COALESCE(ai_verdict, 'MAU_USO'),
    ai_confidence = COALESCE(ai_confidence, 0.940),
    fraud_suspected = FALSE,
    fraud_reason = NULL,
    labor_fee_brl = CASE WHEN COALESCE(final_verdict, 'MAU_USO') = 'MAU_USO' THEN 35.00 ELSE 0.00 END,
    total_estimated_brl = CASE WHEN COALESCE(final_verdict, 'MAU_USO') = 'MAU_USO' THEN 140.50 ELSE 0.00 END,
    customer_feedback_text = COALESCE(customer_feedback_text, 'Identificamos 3 peças com valores calculados pela tabela oficial da Loja Mottu para sua previsibilidade.'),
    resolved_at = COALESCE(resolved_at, NOW())
WHERE contract_id IN (
    'a0000000-0000-0000-0000-000000000001',
    'a2000000-0000-0000-0000-000000000001'
)
  AND status IN ('QUEUED_AI', 'PROCESSING_AI', 'HUMAN_TRIAGE', 'AWAITING_AI_RECOVERY');

-- 2. For any inspection on Carlos or Rafael's contract that has 0 items in inspection_items,
--    insert 2-3 canonical parts so the rider never sees an empty parts list
INSERT INTO inspection_items (id, inspection_id, catalog_part_id, part_name_snapshot, unit_price_snapshot_brl, added_by)
SELECT
    gen_random_uuid(),
    i.id,
    '77777777-7777-7777-7777-777777777773'::uuid,
    'Retrovisor Direito Sport',
    42.50,
    'AI'
FROM inspections i
WHERE i.contract_id IN ('a0000000-0000-0000-0000-000000000001', 'a2000000-0000-0000-0000-000000000001')
  AND NOT EXISTS (SELECT 1 FROM inspection_items ii WHERE ii.inspection_id = i.id);

INSERT INTO inspection_items (id, inspection_id, catalog_part_id, part_name_snapshot, unit_price_snapshot_brl, added_by)
SELECT
    gen_random_uuid(),
    i.id,
    '77777777-7777-7777-7777-777777777771'::uuid,
    'Manete de Freio Direito',
    25.00,
    'AI'
FROM inspections i
WHERE i.contract_id IN ('a0000000-0000-0000-0000-000000000001', 'a2000000-0000-0000-0000-000000000001')
  AND (SELECT COUNT(*) FROM inspection_items ii WHERE ii.inspection_id = i.id) = 1;

-- 3. Keep Carlos's 3 primary showcase inspections at the very top of recentInspections
UPDATE inspections
SET created_at = NOW() - INTERVAL '5 minutes',
    resolved_at = NOW() - INTERVAL '4 minutes'
WHERE id = 'd2000000-0000-0000-0000-000000000001';

UPDATE inspections
SET created_at = NOW() - INTERVAL '20 minutes',
    resolved_at = NOW() - INTERVAL '19 minutes'
WHERE id = 'd2000000-0000-0000-0000-000000000003';

UPDATE inspections
SET created_at = NOW() - INTERVAL '45 minutes',
    resolved_at = NOW() - INTERVAL '40 minutes'
WHERE id = 'd2000000-0000-0000-0000-000000000004';

