-- ==============================================================================
-- V13: Heal V12 Enum Constraints
-- Aligns safety_risk and reported_context with strict Java Domain Enums:
-- ReportedOriginContext: NORMAL_USE_WEAR, RECEIVED_LIKE_THIS, ACCIDENT_OR_DROP
-- SafetyRiskLevel: SAFE_TO_RIDE, CRITICAL_STOPPED
-- ==============================================================================

UPDATE inspections
SET reported_context = CASE
        WHEN reported_context IN ('USO_NORMAL', 'NORMAL_USE_WEAR') THEN 'NORMAL_USE_WEAR'
        WHEN reported_context IN ('JA_RECEBI_ASSIM', 'RECEIVED_LIKE_THIS') THEN 'RECEIVED_LIKE_THIS'
        WHEN reported_context IN ('QUEDA_BATIDA', 'ACCIDENT_OR_DROP') THEN 'ACCIDENT_OR_DROP'
        ELSE 'NORMAL_USE_WEAR'
    END,
    safety_risk = CASE
        WHEN safety_risk IN ('HIGH', 'CRITICAL_STOPPED', 'IMMEDIATE_STOP') THEN 'CRITICAL_STOPPED'
        ELSE 'SAFE_TO_RIDE'
    END,
    priority_reason = CASE
        WHEN priority_reason IN ('CRITICAL_SAFETY', 'RIDER_CONTESTATION', 'LOW_AI_CONFIDENCE', 'FRAUD_SUSPICION') THEN priority_reason
        ELSE 'LOW_AI_CONFIDENCE'
    END
WHERE id LIKE 'd2000000-%'
   OR reported_context NOT IN ('NORMAL_USE_WEAR', 'RECEIVED_LIKE_THIS', 'ACCIDENT_OR_DROP')
   OR safety_risk NOT IN ('SAFE_TO_RIDE', 'CRITICAL_STOPPED');
