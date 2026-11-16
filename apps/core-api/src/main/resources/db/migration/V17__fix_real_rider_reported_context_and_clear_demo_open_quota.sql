-- ==============================================================================
-- V17: Normalize reported_context enum values on Real Rider inspections
--      and clear any residual open inspections on Demo & Real Rider contracts
-- ==============================================================================

-- 1. Normalize reported_context values to valid ReportedContext enum constants
UPDATE inspections
SET reported_context = 'ACCIDENT_OR_DROP'
WHERE reported_context NOT IN ('NORMAL_USE_WEAR', 'RECENT_PICKUP_ISSUE', 'ACCIDENT_OR_DROP')
  AND reported_context ILIKE '%QUEDA%';

UPDATE inspections
SET reported_context = 'NORMAL_USE_WEAR'
WHERE reported_context NOT IN ('NORMAL_USE_WEAR', 'RECENT_PICKUP_ISSUE', 'ACCIDENT_OR_DROP');

-- 2. Resolve any lingering open inspections on Carlos (Demo) and Rafael (Real)
--    so both riders have 0 open inspections (0 de 3) and can run new Wizard flows
UPDATE inspections
SET status = 'AUTO_RESOLVED',
    final_verdict = COALESCE(final_verdict, 'DESGASTE_NATURAL'),
    ai_verdict = COALESCE(ai_verdict, 'DESGASTE_NATURAL'),
    ai_confidence = COALESCE(ai_confidence, 0.950),
    labor_fee_brl = COALESCE(labor_fee_brl, 0.00),
    total_estimated_brl = COALESCE(total_estimated_brl, 0.00),
    customer_feedback_text = COALESCE(customer_feedback_text, 'Vistoria concluída automaticamente sem custo adicional para o entregador.'),
    resolved_at = COALESCE(resolved_at, NOW())
WHERE contract_id IN (
    'a0000000-0000-0000-0000-000000000001',
    'a2000000-0000-0000-0000-000000000001'
)
  AND status IN ('PROCESSING_AI', 'HUMAN_TRIAGE', 'AWAITING_AI_RECOVERY');

-- 3. Ensure Carlos's top 3 showcase inspections have the most recent timestamps
UPDATE inspections
SET created_at = NOW() - INTERVAL '15 minutes',
    resolved_at = NOW() - INTERVAL '14 minutes'
WHERE id = 'd2000000-0000-0000-0000-000000000001';

UPDATE inspections
SET created_at = NOW() - INTERVAL '2 hours',
    resolved_at = NOW() - INTERVAL '2 hours' + INTERVAL '5 seconds'
WHERE id = 'd2000000-0000-0000-0000-000000000003';

UPDATE inspections
SET created_at = NOW() - INTERVAL '6 hours',
    resolved_at = NOW() - INTERVAL '5 hours'
WHERE id = 'd2000000-0000-0000-0000-000000000004';
