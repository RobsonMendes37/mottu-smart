-- =============================================================================
-- V15: CALIBRATE STOCK LEVELS ACROSS ALL MOTORCYCLE OPERATIONAL SYSTEMS
-- =============================================================================
-- Ensures every system category (Brakes/Handlebars, Tires/Wheels, Engine/Gearbox,
-- Fairing/Body) has realistic, operational critical and warning alerts in the
-- WMS inventory for accurate predictive modeling and simulation.
-- =============================================================================

UPDATE catalog_parts
SET min_stock_quantity = 8;

-- 1. Freio e Guidão (BRAKE_HANDLEBAR)
UPDATE catalog_parts
SET stock_quantity = 4, aisle_bin = 'Corredor A • Gaveta 01'
WHERE vtex_sku = 'SKU-MANETE-DIR-SPORT';

UPDATE catalog_parts
SET stock_quantity = 6, aisle_bin = 'Corredor A • Gaveta 02'
WHERE vtex_sku = 'SKU-LONA-FREIO-TRAS-SPORT';

UPDATE catalog_parts
SET stock_quantity = 7, aisle_bin = 'Corredor A • Gaveta 03'
WHERE vtex_sku = 'SKU-APA-003';

-- 2. Pneus e Rodas (TIRES_WHEELS)
UPDATE catalog_parts
SET stock_quantity = 5, aisle_bin = 'Corredor C • Baia 01'
WHERE vtex_sku IN ('SKU-PNEU-TRAS-SPORT', '467');

UPDATE catalog_parts
SET stock_quantity = 6, aisle_bin = 'Corredor C • Baia 02'
WHERE vtex_sku IN ('SKU-RAI-004', '466');

UPDATE catalog_parts
SET stock_quantity = 7, aisle_bin = 'Corredor C • Baia 03'
WHERE vtex_sku IN ('SKU-POP-004', '841');

-- 3. Motor e Câmbio (ENGINE_TRANSMISSION)
UPDATE catalog_parts
SET stock_quantity = 4, aisle_bin = 'Corredor B • Gaveta 01'
WHERE vtex_sku = 'SKU-POP-003' OR part_name ILIKE '%cabo de embreagem%';

UPDATE catalog_parts
SET stock_quantity = 5, aisle_bin = 'Corredor B • Gaveta 02'
WHERE vtex_sku = 'SKU-SPT-006';

UPDATE catalog_parts
SET stock_quantity = 7, aisle_bin = 'Corredor B • Gaveta 03'
WHERE vtex_sku = 'SKU-RAI-005';

-- 4. Carenagem e Chassi (FAIRING_BODY)
UPDATE catalog_parts
SET stock_quantity = 4, aisle_bin = 'Corredor D • Gaveta 01'
WHERE vtex_sku = 'SKU-POP-002';

UPDATE catalog_parts
SET stock_quantity = 4, aisle_bin = 'Corredor D • Gaveta 02'
WHERE vtex_sku = 'SKU-RAI-002';

UPDATE catalog_parts
SET stock_quantity = 6, aisle_bin = 'Corredor D • Gaveta 03'
WHERE vtex_sku = 'SKU-RETROVISOR-DIR-SPORT';
