-- V3__accident_reports_schema.sql
-- Module for Accident / Crash Video Reporting and Auto-Quotation

ALTER TABLE analytics_outbox ALTER COLUMN inspection_id DROP NOT NULL;
ALTER TABLE analytics_outbox ADD COLUMN IF NOT EXISTS accident_report_id UUID;

CREATE TABLE IF NOT EXISTS accident_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(80) NOT NULL UNIQUE,
    contract_id UUID NOT NULL REFERENCES rental_contracts(id),
    rider_id UUID NOT NULL REFERENCES users(id),
    motorcycle_id UUID NOT NULL REFERENCES motorcycles(id),
    video_object_key VARCHAR(255) NOT NULL,
    video_duration_seconds INTEGER NOT NULL,
    video_size_bytes BIGINT NOT NULL,
    has_injuries BOOLEAN NOT NULL DEFAULT FALSE,
    involves_third_party BOOLEAN NOT NULL DEFAULT FALSE,
    police_report_number VARCHAR(60),
    drivability VARCHAR(20) NOT NULL,
    reported_odometer_km INTEGER NOT NULL,
    rider_notes VARCHAR(500),
    rider_to_mechanic_notes VARCHAR(500),
    status VARCHAR(35) NOT NULL,
    ai_confidence NUMERIC(4, 3),
    structural_risk_alert BOOLEAN NOT NULL DEFAULT FALSE,
    ai_technical_summary TEXT,
    parts_subtotal_brl NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    labor_fee_brl NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    towing_fee_brl NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    total_estimated_brl NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    reviewed_by_mechanic_id UUID REFERENCES users(id),
    mechanic_notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    confirmed_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_accident_reports_status ON accident_reports(status, created_at);
CREATE INDEX IF NOT EXISTS idx_accident_reports_rider ON accident_reports(rider_id);

CREATE TABLE IF NOT EXISTS accident_report_photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    accident_report_id UUID NOT NULL REFERENCES accident_reports(id) ON DELETE CASCADE,
    photo_object_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_accident_photos_report ON accident_report_photos(accident_report_id);

CREATE TABLE IF NOT EXISTS accident_report_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    accident_report_id UUID NOT NULL REFERENCES accident_reports(id) ON DELETE CASCADE,
    catalog_part_id UUID REFERENCES catalog_parts(id),
    part_name_snapshot VARCHAR(180) NOT NULL,
    unit_price_snapshot_brl NUMERIC(10, 2) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    video_timestamp_seconds INTEGER,
    confirmed_by_mechanic BOOLEAN NOT NULL DEFAULT TRUE,
    added_by VARCHAR(15) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_accident_items_report ON accident_report_items(accident_report_id);

