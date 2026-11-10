-- ==============================================================================
-- V14: Baseline 360 Dispatch Inspection, Rider Double-Validation & Multi-AI Committee
-- Phase 9: Closes loop between Depot Handover, 48h/100km Shield, Committee Consensus,
-- and In-Person Workshop Triage.
-- ==============================================================================

-- 1. Rental Contracts: 360 Baseline Photos & Rider Digital Confirmation
ALTER TABLE rental_contracts
    ADD COLUMN IF NOT EXISTS pickup_photos_json JSONB DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS pickup_validated_by_rider BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS pickup_validated_at TIMESTAMPTZ DEFAULT NULL;

-- 2. Inspections: Multi-AI Committee Scorecard & Presential Triage Convocatoria
ALTER TABLE inspections
    ADD COLUMN IF NOT EXISTS committee_evaluation_json JSONB DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS presential_inspection_requested BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS presential_request_reason VARCHAR(255) DEFAULT NULL;

-- 3. Seed Realistic Baseline 360 Photos for Active Rental Contracts
UPDATE rental_contracts
SET pickup_photos_json = jsonb_build_object(
        'frontPhotoUrl', '/mottu-inspections/baseline_front_mottu_sport.jpg',
        'rearPhotoUrl', '/mottu-inspections/baseline_rear_mottu_sport.jpg',
        'rightSidePhotoUrl', '/mottu-inspections/baseline_right_mottu_sport.jpg',
        'leftSidePhotoUrl', '/mottu-inspections/baseline_left_mottu_sport.jpg',
        'odometerPhotoUrl', '/mottu-inspections/baseline_odometer_10000.jpg',
        'initialOdometerKm', initial_odometer_km,
        'recordedAt', picked_up_at
    ),
    pickup_validated_by_rider = TRUE,
    pickup_validated_at = picked_up_at + INTERVAL '15 minutes'
WHERE active = TRUE;

-- 4. Seed Multi-Mechanic AI Committee Evaluations for Realistic Damaged Inspections
UPDATE inspections
SET committee_evaluation_json = jsonb_build_object(
        'consensusVerdict', 'DESGASTE_NATURAL',
        'consensusScore', '3/3 Unânime',
        'isUnanimous', true,
        'canIdentifyPartsClearly', true,
        'requiresPresentialInspection', false,
        'averageConfidence', 0.94,
        'summary', 'Consenso unânime da Junta Técnica (DESGASTE_NATURAL) com 94% de certeza. Pneu traseiro apresenta desgaste uniforme decorrente da quilometragem rodada.',
        'evaluators', jsonb_build_array(
            jsonb_build_object(
                'specialistRole', 'PERITO_FADIGA_DESGASTE',
                'verdict', 'DESGASTE_NATURAL',
                'confidence', 0.96,
                'reasoning', 'Pneu atingiu indicador TWI com rodagem de 8.200km; desgaste simétrico compatível com uso urbano contínuo.'
            ),
            jsonb_build_object(
                'specialistRole', 'PERITO_FORENSE_IMPACTO',
                'verdict', 'DESGASTE_NATURAL',
                'confidence', 0.92,
                'reasoning', 'Ausência total de marcas de impacto, cortes por objeto perfurante ou deformação de roda.'
            ),
            jsonb_build_object(
                'specialistRole', 'PERITO_COMPARADOR_360',
                'verdict', 'DESGASTE_NATURAL',
                'confidence', 0.94,
                'reasoning', 'Banda de rodagem compatível com evolução natural desde o registro 360 da retirada da moto.'
            )
        )
    )
WHERE id = 'd2000000-0000-0000-0000-000000000001';

UPDATE inspections
SET committee_evaluation_json = jsonb_build_object(
        'consensusVerdict', 'MAU_USO',
        'consensusScore', '2/3 Divergência Técnica (1 divergente)',
        'isUnanimous', false,
        'canIdentifyPartsClearly', true,
        'requiresPresentialInspection', false,
        'averageConfidence', 0.88,
        'dissentingOpinion', 'PERITO_FADIGA_DESGASTE votou DESGASTE_NATURAL: Borracha da manopla com sinal de atrito suave.',
        'summary', 'Decisão majoritária (2/3 Divergência Técnica) com divergência registrada para arbitramento do mecânico de bancada.',
        'evaluators', jsonb_build_array(
            jsonb_build_object(
                'specialistRole', 'PERITO_FADIGA_DESGASTE',
                'verdict', 'DESGASTE_NATURAL',
                'confidence', 0.82,
                'reasoning', 'Possível ressecamento da borracha da manopla e vibração de pilotagem.'
            ),
            jsonb_build_object(
                'specialistRole', 'PERITO_FORENSE_IMPACTO',
                'verdict', 'MAU_USO',
                'confidence', 0.94,
                'reasoning', 'Manete empenado a 45 graus com ralado pronunciado de asfalto característico de queda lateral.'
            ),
            jsonb_build_object(
                'specialistRole', 'PERITO_COMPARADOR_360',
                'verdict', 'MAU_USO',
                'confidence', 0.89,
                'reasoning', 'Manete estava 100% íntegro e alinhado no registro fotográfico 360 da entrega da moto.'
            )
        )
    )
WHERE id = 'd2000000-0000-0000-0000-000000000002';

UPDATE inspections
SET committee_evaluation_json = jsonb_build_object(
        'consensusVerdict', 'MAU_USO',
        'consensusScore', '3/3 Unânime',
        'isUnanimous', true,
        'canIdentifyPartsClearly', true,
        'requiresPresentialInspection', false,
        'averageConfidence', 0.95,
        'summary', 'Consenso unânime da Junta Técnica (MAU_USO) com 95% de certeza. Bloco óptico do farol dianteiro estilhaçado por impacto contundente.',
        'evaluators', jsonb_build_array(
            jsonb_build_object(
                'specialistRole', 'PERITO_FADIGA_DESGASTE',
                'verdict', 'MAU_USO',
                'confidence', 0.95,
                'reasoning', 'Lente de policarbonato estilhaçada não se rompe por vibração mecânica natural.'
            ),
            jsonb_build_object(
                'specialistRole', 'PERITO_FORENSE_IMPACTO',
                'verdict', 'MAU_USO',
                'confidence', 0.97,
                'reasoning', 'Ponto focal de colisão frontal com rachaduras radiais típicas de batida direta.'
            ),
            jsonb_build_object(
                'specialistRole', 'PERITO_COMPARADOR_360',
                'verdict', 'MAU_USO',
                'confidence', 0.93,
                'reasoning', 'Farol frontal estava perfeitamente íntegro na foto de expedição 360 da moto.'
            )
        )
    )
WHERE id = 'd2000000-0000-0000-0000-000000000003';
