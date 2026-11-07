-- ==============================================================================
-- V2: Initial Seed Data for Mottu Smart Ops (E2E & Offline Verification)
-- Seed Password for all accounts: Mottu@2026!
-- Verified BCrypt Hash: $2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO
-- ==============================================================================

-- 1. Galpões Operacionais Ativos
INSERT INTO yards (id, name, city, address, active) VALUES
('11111111-1111-1111-1111-111111111111', 'Galpão Mottu Butantã', 'São Paulo', 'Av. Corifeu de Azevedo Marques, 1000', TRUE),
('22222222-2222-2222-2222-222222222222', 'Galpão Mottu Guarulhos', 'Guarulhos', 'Rodovia Presidente Dutra, km 220', TRUE)
ON CONFLICT (id) DO NOTHING;

-- 2. Usuários Oficiais
-- Gestor Oficial (Manager)
INSERT INTO users (id, full_name, cpf, phone, email, password_hash, role, status, approved_by, created_at) VALUES
('e0000000-0000-0000-0000-000000000001', 'Gestor Operações', '11122233344', '11987654321', 'gestor@mottu-ops.local',
 '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'MANAGER', 'ACTIVE', NULL, NOW())
ON CONFLICT (id) DO NOTHING;

-- Mecânico Chefe (Mechanic)
INSERT INTO users (id, full_name, cpf, phone, email, password_hash, role, status, approved_by, created_at) VALUES
('e0000000-0000-0000-0000-000000000002', 'Mecânico Chefe', '22233344455', '11987654322', 'mecanico@mottu-ops.local',
 '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'MECHANIC', 'ACTIVE', 'e0000000-0000-0000-0000-000000000001', NOW())
ON CONFLICT (id) DO NOTHING;

-- Entregador Demo (Rider)
INSERT INTO users (id, full_name, cpf, phone, email, password_hash, role, status, approved_by, created_at) VALUES
('e0000000-0000-0000-0000-000000000003', 'Carlos Entregador', '33344455566', '11987654323', 'entregador@mottu-ops.local',
 '$2a$10$rmM5nIBTU8u3zxOWxaqe7OIiNYFIsjmet/gyXgYIAfaeLML65QyPO', 'RIDER', 'ACTIVE', 'e0000000-0000-0000-0000-000000000001', NOW())
ON CONFLICT (id) DO NOTHING;

-- 3. Frota de Motocicletas
INSERT INTO motorcycles (id, plate, model, yard_id, status, current_odometer_km, created_at) VALUES
('33333333-3333-3333-3333-333333333333', 'MOT1A23', 'MOTTU_SPORT_110I', '11111111-1111-1111-1111-111111111111', 'RENTED', 1000, NOW() - INTERVAL '12 hours'),
('44444444-4444-4444-4444-444444444444', 'MOT2B34', 'MOTTU_SPORT_110I', '11111111-1111-1111-1111-111111111111', 'AVAILABLE', 250, NOW()),
('55555555-5555-5555-5555-555555555555', 'POP3C45', 'HONDA_POP_110I', '22222222-2222-2222-2222-222222222222', 'AVAILABLE', 1200, NOW()),
('66666666-6666-6666-6666-666666666666', 'TVS4D56', 'TVS_APACHE_160', '11111111-1111-1111-1111-111111111111', 'AVAILABLE', 50, NOW())
ON CONFLICT (id) DO NOTHING;

-- 4. Contrato de Locação Ativo do Entregador (retirada há 12 horas, 1000 km)
INSERT INTO rental_contracts (id, rider_id, motorcycle_id, picked_up_at, initial_odometer_km, ended_at, active) VALUES
('a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', '33333333-3333-3333-3333-333333333333',
 NOW() - INTERVAL '12 hours', 1000, NULL, TRUE)
ON CONFLICT (id) DO NOTHING;

-- 5. Configurações Globais (System Settings)
INSERT INTO system_settings (setting_key, setting_value, updated_at) VALUES
('STANDARD_LABOR_FEE_BRL', '35.00', NOW()),
('AI_CONFIDENCE_THRESHOLD', '0.80', NOW()),
('PRE_EXISTING_MAX_HOURS', '48', NOW()),
('PRE_EXISTING_MAX_KM', '100', NOW()),
('LAST_CATALOG_SYNC_AT', 'NEVER', NOW())
ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value, updated_at = NOW();

-- 6. Catálogo Inicial de Peças de Referência
INSERT INTO catalog_parts (id, vtex_sku, motorcycle_model, part_name, category, unit_price_brl, is_natural_wear, price_overridden, image_url, updated_at) VALUES
('77777777-7777-7777-7777-777777777771', 'SKU-MANETE-DIR-SPORT', 'MOTTU_SPORT_110I', 'Manete de Freio Direito', 'Comandos & Freios', 25.00, FALSE, FALSE, 'http://minio:9000/mottu-inspections/parts/manete.jpg', NOW()),
('77777777-7777-7777-7777-777777777772', 'SKU-LONA-FREIO-TRAS-SPORT', 'MOTTU_SPORT_110I', 'Lona de Freio Traseira', 'Freios', 33.00, TRUE, FALSE, 'http://minio:9000/mottu-inspections/parts/lona.jpg', NOW()),
('77777777-7777-7777-7777-777777777773', 'SKU-RETROVISOR-DIR-SPORT', 'MOTTU_SPORT_110I', 'Retrovisor Direito', 'Carenagem & Espelhos', 42.50, FALSE, FALSE, 'http://minio:9000/mottu-inspections/parts/retrovisor.jpg', NOW()),
('77777777-7777-7777-7777-777777777774', 'SKU-PNEU-TRAS-SPORT', 'MOTTU_SPORT_110I', 'Pneu Traseiro 80/100-14', 'Rodas & Pneus', 180.00, TRUE, FALSE, 'http://minio:9000/mottu-inspections/parts/pneu.jpg', NOW())
ON CONFLICT (vtex_sku, motorcycle_model) DO NOTHING;
