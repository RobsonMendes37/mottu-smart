-- ==============================================================================
-- V11: Seed Dedicated Live Operational Accounts & Pending Approvals
-- Enables distinct Vitrine ("Demonstrativo Rico") vs Vida Real ("Operação ao Vivo")
-- Seed Password for all accounts: Mottu@2026!
-- Verified BCrypt Hash: $2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO
-- ==============================================================================

-- 1. Usuários de Operação Vida Real (com senha seed Mottu@2026! e hash SHA-256)
INSERT INTO users (
    id, full_name, cpf, phone, email, password_hash, password_sha256, role, status, approved_by,
    preferred_unit, cnh_status, employee_code, role_title, shift_label, created_at, updated_at
) VALUES
-- Gestor Vida Real
('e2000000-0000-0000-0000-000000000001', 'Roberto Gestor (Vida Real)', '12345678901', '11971230001', 'gestor.real@mottu-ops.local',
 '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO',
 encode(digest('Mottu@2026!', 'sha256'), 'hex'),
 'MANAGER', 'ACTIVE', NULL,
 'Unidade Butantã (SP-04)', 'Categoria A/B • Válida até 10/2030', 'MOT-SP04-0199', 'Supervisor de Pátio', 'Operação Diurna • 08h às 18h',
 NOW(), NOW()),

-- Mecânico Vida Real
('e2000000-0000-0000-0000-000000000002', 'Diego Mecânico (Vida Real)', '23456789012', '11971230002', 'mecanico.real@mottu-ops.local',
 '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO',
 encode(digest('Mottu@2026!', 'sha256'), 'hex'),
 'MECHANIC', 'ACTIVE', 'e2000000-0000-0000-0000-000000000001',
 'Unidade Butantã (SP-04)', 'Categoria A • Válida até 05/2028', 'MOT-SP04-0899', 'Mecânico Especialista — Baia 01', 'Turno Integral • 07h às 16h',
 NOW(), NOW()),

-- Entregador Vida Real
('e2000000-0000-0000-0000-000000000003', 'Rafael Entregador (Vida Real)', '34567890123', '11971230003', 'entregador.real@mottu-ops.local',
 '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO',
 encode(digest('Mottu@2026!', 'sha256'), 'hex'),
 'RIDER', 'ACTIVE', 'e2000000-0000-0000-0000-000000000001',
 'Unidade Butantã (SP-04)', 'Categoria A • Válida até 09/2029', 'CTR-2026-9901', 'Entregador Parceiro Real', 'Plano Minha Mottu Semanal',
 NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 2. Cadastros Pendentes para Teste de Aprovação/Rejeição na tela T08 do Gestor
INSERT INTO users (
    id, full_name, cpf, phone, email, password_hash, password_sha256, role, status, approved_by,
    preferred_unit, cnh_status, employee_code, role_title, shift_label, created_at, updated_at
) VALUES
('e3000000-0000-0000-0000-000000000001', 'Lucas Candidato Motoboy', '98765432101', '11988880001', 'lucas.candidato@mottu-ops.local',
 '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO',
 encode(digest('Mottu@2026!', 'sha256'), 'hex'),
 'RIDER', 'PENDING_APPROVAL', NULL,
 'Unidade Butantã (SP-04)', 'Categoria A • Em análise', 'CTR-PEND-001', 'Candidato a Locatário', 'Aguardando Aprovação de Documentos',
 NOW() - INTERVAL '2 hours', NOW()),

('e3000000-0000-0000-0000-000000000002', 'Marcos Candidato Oficina', '87654321092', '11988880002', 'marcos.candidato@mottu-ops.local',
 '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO',
 encode(digest('Mottu@2026!', 'sha256'), 'hex'),
 'MECHANIC', 'PENDING_APPROVAL', NULL,
 'Unidade Butantã (SP-04)', 'Categoria A/B • Em análise', 'MOT-PEND-002', 'Candidato a Mecânico Júnior', 'Aguardando Validação de Bancada',
 NOW() - INTERVAL '1 hour', NOW())
ON CONFLICT (id) DO NOTHING;

-- 3. Vincular Moto e Contrato Ativo Real para o Entregador Vida Real
-- Adiciona moto MOT9Z99 se não existir
INSERT INTO motorcycles (id, plate, model, yard_id, status, current_odometer_km, created_at)
VALUES (
    'b2000000-0000-0000-0000-000000000001', 'MOT9Z99', 'MOTTU_SPORT_110I',
    '11111111-1111-1111-1111-111111111111', 'RENTED', 3150, NOW() - INTERVAL '15 days'
)
ON CONFLICT (plate) DO UPDATE SET status = 'RENTED';

-- Contrato ativo para Rafael Entregador Real
INSERT INTO rental_contracts (
    id, rider_id, motorcycle_id, picked_up_at, initial_odometer_km, ended_at, active,
    contract_code, weekly_plan_brl, discount_brl, payment_status, due_date_label, paid_at
) VALUES (
    'a2000000-0000-0000-0000-000000000001',
    'e2000000-0000-0000-0000-000000000003',
    'b2000000-0000-0000-0000-000000000001',
    NOW() - INTERVAL '10 days', 2800, NULL, TRUE,
    'CTR-2026-9901', 196.00, 15.00, 'PENDING_PIX', 'Vence hoje • 18:00', NULL
)
ON CONFLICT (id) DO NOTHING;

-- Entregas diárias do entregador real (com 115 entregas, faltando apenas 5 para bater a meta de 120 e ganhar R$ 40)
INSERT INTO rider_daily_deliveries (
    id, rider_id, contract_id, day_label, completed_count, earnings_brl, on_time_rate, delivery_date, created_at
) VALUES
('d2000000-0000-0000-0000-000000000001', 'e2000000-0000-0000-0000-000000000003', 'a2000000-0000-0000-0000-000000000001', 'Segunda', 24, 216.00, '99%', CURRENT_DATE - 4, NOW()),
('d2000000-0000-0000-0000-000000000002', 'e2000000-0000-0000-0000-000000000003', 'a2000000-0000-0000-0000-000000000001', 'Terça', 22, 198.00, '97%', CURRENT_DATE - 3, NOW()),
('d2000000-0000-0000-0000-000000000003', 'e2000000-0000-0000-0000-000000000003', 'a2000000-0000-0000-0000-000000000001', 'Quarta', 26, 234.00, '98%', CURRENT_DATE - 2, NOW()),
('d2000000-0000-0000-0000-000000000004', 'e2000000-0000-0000-0000-000000000003', 'a2000000-0000-0000-0000-000000000001', 'Quinta', 23, 207.00, '100%', CURRENT_DATE - 1, NOW()),
('d2000000-0000-0000-0000-000000000005', 'e2000000-0000-0000-0000-000000000003', 'a2000000-0000-0000-0000-000000000001', 'Sexta (Hoje)', 20, 180.00, '98%', CURRENT_DATE, NOW())
ON CONFLICT (id) DO NOTHING;

