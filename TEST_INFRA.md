# TEST_INFRA.md — Mottu Smart Ops E2E Test Infrastructure Guide

## 1. Overview & Test Philosophy
The Mottu Smart Ops E2E test suite provides an **opaque-box, requirement-driven, contract-certified testing architecture**. Every test case is derived directly from user requirements (`docs/01-PMD.md`), architectural interfaces (`docs/02-ARQUITETURA-E-APIS.md`), and TDD use cases (`docs/03-CASOS-DE-USO-TDD.md`), maintaining 100% independence from internal code implementations.

### Core Principles
1. **Opaque-Box Testing**: Evaluates observable inputs, outputs, HTTP response contracts, RFC 7807 problem details, and database/queue side-effects.
2. **Dual-Mode Execution**:
   - **Contract/Mock Mode (`E2E_CONTRACT_MODE=mock`)**: Self-contained in-memory mock server executing 100% offline without dependencies on external networks, Google Cloud keys, or live containers.
   - **Live Stack Mode (`E2E_CONTRACT_MODE=live`)**: Directly targets the Caddy Reverse Proxy (`http://localhost:18088`), Core API (`http://localhost:18080`), and Web App.
3. **Deterministic Isolation**: Tests do not depend on test execution order. State is isolated or reset per test fixture.
4. **Adversarial Hardening**: Enforces exact boundary values (e.g. 48.0h vs 48.1h, 100km vs 101km, confidence 0.80 vs 0.79, 10 MB vs 10 MB + 1 byte, 10 chars vs 9 chars).

---

## 2. Directory Layout & Architecture

```text
e2e/
├── package.json                   # Playwright, TypeScript and Node dependencies
├── tsconfig.json                  # ES2022 TypeScript configuration
├── playwright.config.ts           # Playwright runner configuration (timeout, workers, reporters)
├── src/
│   ├── contracts/
│   │   ├── enums.ts               # Official Enums (UserRole, InspectionStatus, DamageVerdict, etc.)
│   │   ├── types.ts               # Strongly-typed API schemas, DTOs, and PageResponse<T>
│   │   └── validator.ts           # RFC 7807 Problem Detail validator & format checkers
│   ├── fixtures/
│   │   ├── seed-data.ts           # Standard test credentials, yards, and motorcycles
│   │   ├── test-images.ts         # Base64 WebP/JPEG fixtures and SHA-256 test hashes
│   │   └── test-context.ts        # Custom Playwright fixtures (clients for Rider, Mechanic, Manager)
│   ├── client/
│   │   └── api-client.ts          # Strong-typed HTTP client with JWT & Idempotency-Key support
│   ├── mock-server/
│   │   └── contract-server.ts     # In-memory reference mock server matching 100% of API specs
│   └── pages/
│       └── index.ts               # Page Object Models for UI browser journeys (T01-T10)
└── tests/
    ├── tier1-features/            # Tier 1: Feature Coverage (61 tests across 12 features)
    │   ├── 01-auth-rbac.spec.ts
    │   ├── 02-yards-fleet.spec.ts
    │   ├── 03-rider-garage.spec.ts
    │   ├── 04-photo-upload-antifraud.spec.ts
    │   ├── 05-inspection-idempotency.spec.ts
    │   ├── 06-ai-triage-preexisting.spec.ts
    │   ├── 07-ai-recovery-sla.spec.ts
    │   ├── 08-rider-contestation.spec.ts
    │   ├── 09-mechanic-workbench.spec.ts
    │   ├── 10-yard-appointment.spec.ts
    │   ├── 11-catalog-vtex-sync.spec.ts
    │   └── 12-analytics-outbox.spec.ts
    ├── tier2-boundaries/          # Tier 2: Boundary & Corner Cases (60 tests across all features)
    │   ├── 01-auth-boundaries.spec.ts
    │   ├── 02-fleet-boundaries.spec.ts
    │   ├── 03-garage-boundaries.spec.ts
    │   ├── 04-upload-fraud-boundaries.spec.ts
    │   ├── 05-idempotency-limits-boundaries.spec.ts
    │   ├── 06-preexisting-math-boundaries.spec.ts
    │   ├── 07-recovery-sla-boundaries.spec.ts
    │   ├── 08-contestation-boundaries.spec.ts
    │   ├── 09-mechanic-queue-boundaries.spec.ts
    │   ├── 10-appointment-boundaries.spec.ts
    │   ├── 11-catalog-price-boundaries.spec.ts
    │   └── 12-analytics-boundaries.spec.ts
    ├── tier3-cross-feature/       # Tier 3: Cross-Feature Combinations (8 pairwise interaction tests)
    │   └── cross-feature-flows.spec.ts
    └── tier4-scenarios/           # Tier 4: Real-World Scenarios (6 complete user journeys)
        ├── journey-preexisting-free.spec.ts
        ├── journey-wear-and-tear.spec.ts
        ├── journey-accident-and-contestation.spec.ts
        ├── journey-photo-fraud-flagged.spec.ts
        ├── journey-critical-safety-sla.spec.ts
        └── journey-manager-bi-catalog.spec.ts
```

---

## 3. The 4-Tier Test Architecture

| Tier | Focus | Requirement | Test Count | Description |
|---|---|---|---|---|
| **Tier 1** | **Feature Coverage** | $\ge 5$ tests per feature | **61 tests** | Covers all primary happy paths, responses, status transitions, and data integrity across all 12 system features. |
| **Tier 2** | **Boundary & Corner Cases** | $\ge 5$ tests per feature | **60 tests** | Exact boundary conditions: 48.0h vs 48.1h, 100km vs 101km, confidence 0.80 vs 0.79, 10 MB vs 10 MB + 1B, 10 chars vs 9 chars, max open count 3 vs 4, SLA 299s vs 301s. |
| **Tier 3** | **Cross-Feature Combinations** | Multi-domain flows | **8 tests** | Pairwise interactions: Auth $\to$ Inspection $\to$ AI Triage $\to$ Mechanic Workbench $\to$ Yard Appointment, Multi-part budget breakdown, Price override across VTEX sync. |
| **Tier 4** | **Real-World Scenarios** | Complete user journeys | **6 tests** | Full end-to-end application lifecycle: Pre-existing damage claim, Normal wear preventive exchange, Drop contestation & resolution, Photo fraud quarantine, Gemini outage SLA escalation, Executive BI management. |
| **TOTAL** | | | **135 tests** | **100% Pass Rate** |

---

## 4. How to Run the Tests

### Prerequisites
- Node.js 22+ (native or via `source ~/.nvm/nvm.sh`)
- npm dependencies installed in `e2e/` (`npm install`)

### Commands

1. **Run Full 135-Test Suite (Default / Offline Mode):**
   ```bash
   cd e2e
   npm test
   # OR
   npx playwright test
   ```

2. **Run Individual Tiers:**
   ```bash
   cd e2e
   npm run test:tier1    # Run all 61 Tier 1 Feature tests
   npm run test:tier2    # Run all 60 Tier 2 Boundary tests
   npm run test:tier3    # Run all 8 Tier 3 Cross-feature tests
   npm run test:tier4    # Run all 6 Tier 4 Real-world Scenario tests
   ```

3. **Run Against Live Stack (Docker Compose Environment):**
   ```bash
   cd e2e
   E2E_CONTRACT_MODE=live E2E_BASE_URL=http://localhost:18088 npm test
   ```

4. **Generate HTML Test Report:**
   ```bash
   cd e2e
   npx playwright show-report
   ```

---

## 5. Offline Autonomous Execution Certification
All tests are 100% decoupled from external cloud dependencies:
- `GEMINI_MOCK_MODE=true`: Multimodal vision triage works offline with predictable mock fixtures.
- `BIGQUERY_ENABLED=false`: Outbox batch load writes local NDJSON logs without Google Cloud credentials.
- Zero network round-trips to external third parties (`loja.mottu.com.br` fallback snapshot active).
