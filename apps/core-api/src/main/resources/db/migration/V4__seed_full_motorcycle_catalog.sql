-- =============================================================================
-- V4: Seed de Catálogo Completo para Todas as Motos (Honda Pop, Apache, Raider)
-- =============================================================================

INSERT INTO catalog_parts (id, vtex_sku, motorcycle_model, part_name, category, unit_price_brl, is_natural_wear, price_overridden, image_url, updated_at) VALUES
-- Honda Pop 110i
('88888888-8888-8888-8888-888888888881', 'SKU-POP-001', 'HONDA_POP_110I', 'Manete de Freio Dianteiro', 'Comandos & Freios', 22.00, FALSE, FALSE, 'http://minio:9000/mottu-inspections/parts/manete-pop.jpg', NOW()),
('88888888-8888-8888-8888-888888888882', 'SKU-POP-002', 'HONDA_POP_110I', 'Espelho Retrovisor Esquerdo', 'Carenagem & Espelhos', 39.00, FALSE, FALSE, 'http://minio:9000/mottu-inspections/parts/retrovisor-pop.jpg', NOW()),
('88888888-8888-8888-8888-888888888883', 'SKU-POP-003', 'HONDA_POP_110I', 'Lona de Freio Traseira Pop', 'Freios', 31.00, TRUE, FALSE, 'http://minio:9000/mottu-inspections/parts/lona-pop.jpg', NOW()),
('88888888-8888-8888-8888-888888888884', 'SKU-POP-004', 'HONDA_POP_110I', 'Pneu Traseiro 80/100-14', 'Rodas & Pneus', 165.00, TRUE, FALSE, 'http://minio:9000/mottu-inspections/parts/pneu-pop.jpg', NOW()),
('88888888-8888-8888-8888-888888888885', 'SKU-POP-005', 'HONDA_POP_110I', 'Filtro de Ar Pop', 'Motor & Ignição', 24.00, TRUE, FALSE, 'http://minio:9000/mottu-inspections/parts/filtro-pop.jpg', NOW()),
('88888888-8888-8888-8888-888888888886', 'SKU-POP-006', 'HONDA_POP_110I', 'Pisca Traseiro Direito', 'Elétrica & Iluminação', 29.00, FALSE, FALSE, 'http://minio:9000/mottu-inspections/parts/pisca-pop.jpg', NOW()),

-- TVS Apache 160
('88888888-8888-8888-8888-888888888887', 'SKU-APA-001', 'TVS_APACHE_160', 'Manete de Embreagem', 'Comandos & Freios', 32.00, FALSE, FALSE, 'http://minio:9000/mottu-inspections/parts/manete-apache.jpg', NOW()),
('88888888-8888-8888-8888-888888888888', 'SKU-APA-002', 'TVS_APACHE_160', 'Espelho Retrovisor Esportivo', 'Carenagem & Espelhos', 55.00, FALSE, FALSE, 'http://minio:9000/mottu-inspections/parts/retrovisor-apache.jpg', NOW()),
('88888888-8888-8888-8888-888888888889', 'SKU-APA-003', 'TVS_APACHE_160', 'Pastilha de Freio Dianteira', 'Freios', 48.00, TRUE, FALSE, 'http://minio:9000/mottu-inspections/parts/pastilha-apache.jpg', NOW()),
('88888888-8888-8888-8888-88888888888a', 'SKU-APA-004', 'TVS_APACHE_160', 'Pneu Traseiro 110/80-17', 'Rodas & Pneus', 240.00, TRUE, FALSE, 'http://minio:9000/mottu-inspections/parts/pneu-apache.jpg', NOW()),
('88888888-8888-8888-8888-88888888888b', 'SKU-APA-005', 'TVS_APACHE_160', 'Kit Relação Completa Apache', 'Transmissão', 145.00, TRUE, FALSE, 'http://minio:9000/mottu-inspections/parts/relacao-apache.jpg', NOW()),
('88888888-8888-8888-8888-88888888888c', 'SKU-APA-006', 'TVS_APACHE_160', 'Pedal de Freio Traseiro', 'Comandos & Freios', 45.00, FALSE, FALSE, 'http://minio:9000/mottu-inspections/parts/pedal-apache.jpg', NOW()),

-- TVS Raider 125
('88888888-8888-8888-8888-88888888888d', 'SKU-RAI-001', 'TVS_RAIDER_125', 'Manete de Freio Raider', 'Comandos & Freios', 28.00, FALSE, FALSE, 'http://minio:9000/mottu-inspections/parts/manete-raider.jpg', NOW()),
('88888888-8888-8888-8888-88888888888e', 'SKU-RAI-002', 'TVS_RAIDER_125', 'Espelho Retrovisor Raider', 'Carenagem & Espelhos', 46.00, FALSE, FALSE, 'http://minio:9000/mottu-inspections/parts/retrovisor-raider.jpg', NOW()),
('88888888-8888-8888-8888-88888888888f', 'SKU-RAI-003', 'TVS_RAIDER_125', 'Pastilha de Freio Raider', 'Freios', 42.00, TRUE, FALSE, 'http://minio:9000/mottu-inspections/parts/pastilha-raider.jpg', NOW()),
('88888888-8888-8888-8888-888888888890', 'SKU-RAI-004', 'TVS_RAIDER_125', 'Pneu Dianteiro 90/90-17', 'Rodas & Pneus', 195.00, TRUE, FALSE, 'http://minio:9000/mottu-inspections/parts/pneu-raider.jpg', NOW()),
('88888888-8888-8888-8888-888888888891', 'SKU-RAI-005', 'TVS_RAIDER_125', 'Óleo de Motor 10W30 4T', 'Lubrificantes', 35.00, TRUE, FALSE, 'http://minio:9000/mottu-inspections/parts/oleo-raider.jpg', NOW()),
('88888888-8888-8888-8888-888888888892', 'SKU-RAI-006', 'TVS_RAIDER_125', 'Farol Principal LED', 'Elétrica & Iluminação', 280.00, FALSE, FALSE, 'http://minio:9000/mottu-inspections/parts/farol-raider.jpg', NOW()),

-- Mottu Sport 110i (itens adicionais de motor e transmissão)
('88888888-8888-8888-8888-888888888893', 'SKU-SPT-005', 'MOTTU_SPORT_110I', 'Vela de Ignição CPR8EA', 'Motor & Ignição', 28.00, TRUE, FALSE, 'http://minio:9000/mottu-inspections/parts/vela-sport.jpg', NOW()),
('88888888-8888-8888-8888-888888888894', 'SKU-SPT-006', 'MOTTU_SPORT_110I', 'Pedal de Câmbio Sport', 'Comandos & Freios', 38.00, FALSE, FALSE, 'http://minio:9000/mottu-inspections/parts/pedal-sport.jpg', NOW())
ON CONFLICT (vtex_sku, motorcycle_model) DO NOTHING;
