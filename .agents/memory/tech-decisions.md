---
type: project
created: 2026-07-18
updated: 2026-11-06
---

# Technical & Product Decisions — Mottu Smart Triage & Fleet Ops

## Stack & Monorepo Structure
- Polyglot Monorepo (`1 Git repo`) containing 3 decoupled apps (`apps/web-app`, `apps/core-api`, `apps/intelligence-service`) + `infra/` Docker Compose (~1.1 GB RAM budget for shared 12 GB Oracle Cloud VM).
- **Frontend (`apps/web-app`):** Angular (Vite native builder) + TypeScript + Tailwind CSS + Spartan UI, designed first in `pen.dev`, configured as installable Mobile-First PWA.
- **Core Backend (`apps/core-api`):** Java 21 (Virtual Threads) + Spring Boot 3 using strict Hexagonal Architecture (pure `domain/` without JPA/Spring annotations), Spring Security (JWT + RBAC), JPA/Hibernate, Flyway, Resilience4j.
- **Storage, Cache & Queue:** MinIO in Docker simulating AWS S3 via official AWS S3 SDK + Presigned URLs; Redis for async AI queues, idempotency keys, catalog cache, and rate limiting (`50/h` closed mode, `5/h` public LinkedIn mode via `PUBLIC_DEMO_MODE` flag); PostgreSQL + `pgvector`.
- **AI & ETL (`apps/intelligence-service`):** Python + FastAPI with two bounded modules: (1) Gemini Multimodal Vision + CRAG (`pgvector`) + empathetic feedback generator, and (2) async VTEX catalog scraper for `loja.mottu.com.br`.
- **Analytics / BI:** Transactional Outbox micro-batching (`Batch Load`) to Google BigQuery, feeding Looker Studio embedded via responsive `iframe` in the Manager portal.

## Locked Business Rules (PMD v2.0)
- **Roles:** `ROLE_RIDER` (self-signup -> `PENDING_APPROVAL`), `ROLE_MECHANIC` (self-signup -> `PENDING_APPROVAL`), `ROLE_MANAGER` (pre-seeded, approves accounts & links motorcycles).
- **Pre-existing Defect (`PRE_EXISTENTE` -> `R$ 0.00`):** Auto-approved ONLY if rider selects "received like this" AND contract pickup is `<= 48 hours` OR `<= 100 km` AND no fraud alert; otherwise routed to Mechanic.
- **Natural Wear (`DESGASTE_NATURAL` -> `R$ 0.00`):** Part flagged as natural wear in catalog AND no impact/crash visual signs.
- **Misuse / Crash (`MAU_USO`):** `Estimated Total = Sum(Catalog Part Prices) + Standard Labor Fee (default R$ 35.00, editable by Manager)`.
- **AI Confidence Gate:** `>= 0.80` auto-resolves; `< 0.80` routes to Mechanic queue.
- **Anti-Fraud & Outage Resilience:** Duplicate image hash (`SHA-256`/perceptual) or suspicious EXIF forces Mechanic review. Temporary Gemini outages hold tickets in Redis `AWAITING_AI_RECOVERY` with backoff (do NOT flood mechanics!), escalating to Mechanic ONLY if `CRITICAL_SAFETY` exceeds `5 min`.
- **Contestation:** Rider can contest ("Discordo") max `1 time` per ticket (min 10 chars); Mechanic decision is final.
- **Testing Strategy (TDD):** Write failing tests BEFORE implementation (`RED -> GREEN -> REFACTOR`) with 100% mandatory coverage on `domain` / business rules + integration tests on all API endpoints so smaller AI models stay strictly on rails.
