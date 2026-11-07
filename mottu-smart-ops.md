# mottu-smart-ops — Plano Mestre Central de Tarefas (TDD & Multi-Agente v2.0)

## 1. Overview & Project Type
* **O quê & Por quê:** Sistema completo de vistoria autônoma por foto (Gemini Vision + CRAG), fila inteligente de oficina e inteligência de preços (`loja.mottu.com.br` VTEX + BigQuery/Looker Studio) para operação de frota de motos.
* **Project Type:** `WEB` (Angular 19 Standalone + Signals Mobile-First PWA + Console Oficina/Gestor) + `BACKEND` (Java 21 Spring Boot Hexagonal + Python 3.12 FastAPI IA/ETL).
* **Regra de Execução Autônoma:** As telas base (`T01` a `T10`) já estão geradas em `design/mottu-smart-ops.pen`. Todas as tarefas de código (`TASK-03` a `TASK-18`) podem rodar **100% sem parar durante a madrugada** seguindo **TDD (`RED → GREEN → REFACTOR`)**, as 8 Leis de `AGENTS.md` e `GEMINI_MOCK_MODE=true` / `BIGQUERY_ENABLED=false` para validação offline completa.

---

## 2. Success Criteria (Critérios de Sucesso Medíveis)
- [x] As 10 telas (`T01` a `T10`) desenhadas em `design/mottu-smart-ops.pen` (editáveis pelo usuário a qualquer momento na extensão `pen.dev`).
- [x] **100% de cobertura de testes unitários** nas regras de `domain/` (Java `JaCoCo LINE 100%` + `DomainPurityTest` e Python `pytest-cov 100%`).
- [x] **100% das rotas REST** cobertas por testes de integração automatizados rodando 100% offline (`GEMINI_MOCK_MODE=true`).
- [x] Stack completa rodando via Docker Compose dentro do orçamento de **`~1,15 GB` de RAM** (pronta para a VM Oracle de 12 GB).
- [x] Fluxo ponta a ponta validado: Cadastro → Aprovação Gestor → Sync Catálogo Mottu Store → Foto Vistoria (`MinIO`) → Diagnóstico IA/CRAG ou Fila Mecânico → Contestação → Exportação em Lote BigQuery.

---

## 3. Tech Stack Oficial
* **Design UI:** `DESIGN.md` + Extensão **Pencil (`pen.dev`)** no editor (`design/mottu-smart-ops.pen`).
* **Front-end (`apps/web-app`):** Angular 19 (`Standalone` + `Signals` + Vite builder) + TypeScript + Tailwind CSS + PWA + Vitest + Playwright.
* **Core API (`apps/core-api`):** Java 21 (`Virtual Threads`) + Spring Boot 3.4 (Hexagonal Puro) + Spring Security (JWT/RBAC) + JPA/Hibernate + Flyway + AWS SDK S3 (`MinIO`) + Redis + Resilience4j + Google Cloud BigQuery Batch + JUnit 5 + JaCoCo.
* **AI & Scraper (`apps/intelligence-service`):** Python 3.12 + FastAPI + Google GenAI SDK (Gemini Multimodal + `text-embedding-004`) + `pgvector` (CRAG) + `httpx` (Minerador VTEX `loja.mottu.com.br`) + Pytest.
* **Infraestrutura (`infra/`):** Docker Compose + PostgreSQL 16 (`pgvector`) + Redis 7 + MinIO (S3) + Caddy 2 (HTTPS + Proxy `/api` e `/storage`).

---

## 4. File Structure Resumida
```text
mottu-smart-ops/
├── AGENTS.md
├── DESIGN.md
├── mottu-smart-ops.md
├── .env.example
├── design/mottu-smart-ops.pen
├── docs/ (01-PMD.md, 02-ARQUITETURA-E-APIS.md, 03-CASOS-DE-USO-TDD.md, HISTORICO_IA.md, GUIA_ESTUDO_E_LOOKER.md)
├── infra/ (docker-compose.dev.yml, docker-compose.prod.yml, Caddyfile)
└── apps/
    ├── core-api/
    ├── intelligence-service/
    └── web-app/
```

---

## 5. Task Breakdown (Tarefas Cirúrgicas e Atômicas para Qualquer Agente)

### FASE 1 — Prototipação Visual no Editor (`pen.dev`) [CONCLUÍDA — EDITÁVEL PELO USUÁRIO]
- [x] **`TASK-01` — Criar Telas Mobile do Entregador e Login no `.pen` (`T01` a `T04`)**
  - **Agent:** `frontend-specialist` | **Skills:** `design-spec`, `frontend-design`
  - **Pasta Permitida:** `design/`
  - **Dependencies:** Nenhuma (extensão `pen.dev` instalada)
  - **INPUT → OUTPUT → VERIFY:** Ler `DESIGN.md` e Seção 6 de `docs/01-PMD.md` → Gerar frames `T01`, `T02`, `T03`, `T04` em `design/mottu-smart-ops.pen` (zero roxo, alto contraste solar) → Validado via JSON schema.

- [x] **`TASK-02` — Criar Telas Web do Mecânico e Gestor no `.pen` (`T05` a `T10`)**
  - **Agent:** `frontend-specialist` | **Skills:** `design-spec`, `frontend-design`
  - **Pasta Permitida:** `design/`
  - **Dependencies:** `TASK-01`
  - **INPUT → OUTPUT → VERIFY:** Ler `DESIGN.md` e Seção 6 de `docs/01-PMD.md` → Gerar frames `T05` a `T10` em `design/mottu-smart-ops.pen` → Validado via JSON schema (10 frames prontos para edição visual).

---

### FASE 2 — Fundação de Infraestrutura & Scaffolds Limpos
- [x] **`TASK-03` — Configurar Docker Dev/Prod (`Postgres pgvector`, `Redis`, `MinIO`, `Caddy`), `.env.example` e Git**
  - **Agent:** `devops-engineer` | **Skills:** `deployment-procedures`, `bash-linux`
  - **Pasta Permitida:** `infra/`, `.gitignore`, `.env.example`
  - **Dependencies:** Nenhuma
  - **INPUT → OUTPUT → VERIFY:** Seções 1 e 2 de `docs/02-ARQUITETURA-E-APIS.md` → Criar `.env.example`, `docker-compose.dev.yml`, `docker-compose.prod.yml` (limites ~1,15 GB) e `Caddyfile` (com `/api` e `/storage`) → `docker compose -f infra/docker-compose.dev.yml config` retorna código `0`.

- [x] **`TASK-04` — Inicializar Java 21 Spring Boot 3.4 Hexagonal + Flyway (`V1` e `V2`) + `DomainPurityTest` + JaCoCo 100%**
  - **Agent:** `backend-specialist` + `database-architect` | **Skills:** `database-design`, `clean-code`
  - **Pasta Permitida:** `apps/core-api/`
  - **Dependencies:** `TASK-03`
  - **INPUT → OUTPUT → VERIFY:** Seção 5 de `docs/02-ARQUITETURA-E-APIS.md` e Seção 1 de `docs/03-CASOS-DE-USO-TDD.md` → Criar `pom.xml`, `V1__initial_schema.sql`, `V2__seed_data.sql` (credenciais oficiais `Mottu@2026!`) e `DomainPurityTest` → `./mvnw test` passa sem erros.

- [x] **`TASK-05` — Inicializar Python 3.12 FastAPI + Configuração Pytest 100% Domain + Snapshot Inicial VTEX**
  - **Agent:** `backend-specialist` | **Skills:** `python-patterns`, `clean-code`
  - **Pasta Permitida:** `apps/intelligence-service/`
  - **Dependencies:** `TASK-03`
  - **INPUT → OUTPUT → VERIFY:** Seção 3 de `docs/02-ARQUITETURA-E-APIS.md` → Criar `pyproject.toml`, estrutura `app/domain` e `app/modules`, `seed_catalog_snapshot.json`, rota `/health` e teste inicial → `pytest` passa com sucesso.

---

### FASE 3 — Coração de Regras de Negócio com TDD Puro (`100% Coverage`)
- [x] **`TASK-06` — TDD Domínio Java: Enums, Entidades Puras, Anti-Fraude e Regra de 48h / 100 km (`UC-04`, `UC-05`)**
  - **Agent:** `test-engineer` → `backend-specialist` | **Skills:** `tdd-workflow`, `clean-code`
  - **Pasta Permitida:** `apps/core-api/src/test/java/com/mottu/ops/domain/`, `apps/core-api/src/main/java/com/mottu/ops/domain/`
  - **Dependencies:** `TASK-04`
  - **INPUT → OUTPUT → VERIFY:** Casos `UC-04` e `UC-05` → Escrever `PhotoFraudDetectorTest` e `PreExistingRuleCalculatorTest` primeiro (`RED`), depois implementar domínio puro sem Spring/JPA (`GREEN`) → `./mvnw test` passa com 100% em `domain`.

- [x] **`TASK-07` — TDD Domínio Java: Orçamento, Confiança IA, Máquina de Estados, Contestação e Fila Espera (`UC-06` a `UC-09`)**
  - **Agent:** `test-engineer` → `backend-specialist` | **Skills:** `tdd-workflow`, `clean-code`
  - **Pasta Permitida:** `apps/core-api/src/test/java/com/mottu/ops/domain/`, `apps/core-api/src/main/java/com/mottu/ops/domain/`
  - **Dependencies:** `TASK-06`
  - **INPUT → OUTPUT → VERIFY:** Casos `UC-06`, `UC-07`, `UC-08`, `UC-09` → Criar `BudgetAndVerdictCalculatorTest`, `InspectionStateMachineTest` e `AiRecoveryPolicyTest` (`RED` → `GREEN`) → `./mvnw test jacoco:check` passa com **100%** em `domain/service` e `domain/model`.

- [x] **`TASK-08` — TDD Domínio Python: Avaliador CRAG e Normalizador VTEX (`UC-11`)**
  - **Agent:** `test-engineer` → `backend-specialist` | **Skills:** `tdd-workflow`, `python-patterns`
  - **Pasta Permitida:** `apps/intelligence-service/tests/domain/`, `apps/intelligence-service/app/domain/`
  - **Dependencies:** `TASK-05`
  - **INPUT → OUTPUT → VERIFY:** Casos `UC-11` → Escrever `test_crag_evaluator.py` e `test_vtex_normalizer.py` primeiro (`RED`), depois implementar regras puras (`GREEN`) → `pytest --cov=app/domain --cov-fail-under=100` passa.

---

### FASE 4 — Casos de Uso & Adaptadores Back-end (Testes de Integração)
- [x] **`TASK-09` — Auth JWT, RBAC, Config Pública Demo, Auto-Cadastro e Aprovação pelo Gestor (`UC-01`, `UC-02`)**
  - **Agent:** `security-auditor` + `backend-specialist` | **Skills:** `api-patterns`, `tdd-workflow`
  - **Pasta Permitida:** `apps/core-api/`
  - **Dependencies:** `TASK-07`
  - **INPUT → OUTPUT → VERIFY:** Contratos 6.1 e 6.4 (`02-ARQUITETURA-E-APIS.md`) → `AuthAndApprovalIntegrationTest` e `RbacSecurityIntegrationTest` (`RED`) + Controllers/JPA/JWT (`GREEN`) → `./mvnw verify` passa.

- [x] **`TASK-10` — Galpões, Frota, Garagem do Entregador e Upload MinIO (`S3_PUBLIC_ENDPOINT` vs `S3_INTERNAL_ENDPOINT`)**
  - **Agent:** `backend-specialist` | **Skills:** `api-patterns`, `tdd-workflow`
  - **Pasta Permitida:** `apps/core-api/`
  - **Dependencies:** `TASK-09`
  - **INPUT → OUTPUT → VERIFY:** Contratos 6.1 (`/yards`), 6.2 (`/garage`, `/upload-url`) e 6.3 (`/fleet/motorcycles`) → Testes de integração + adaptadores S3 → `./mvnw verify` passa.

- [x] **`TASK-11A` — Abertura de Vistoria do Entregador: Idempotência Redis, Leitura S3 Anti-Fraude e Stream SSE (`UC-03`, `UC-04`)**
  - **Agent:** `backend-specialist` | **Skills:** `api-patterns`, `tdd-workflow`
  - **Pasta Permitida:** `apps/core-api/`
  - **Dependencies:** `TASK-10`
  - **INPUT → OUTPUT → VERIFY:** Contratos 6.2 (`POST /inspections`, `GET /inspections/{id}`, `GET /stream`) → `InspectionCreationIntegrationTest` cobrindo clique triplo (`Idempotency-Key`), limite de 3 abertos, SHA-256 repetido e SSE → `./mvnw verify` passa.

- [x] **`TASK-11B` — Retorno da IA, Job SLA de Recuperação, Contestação e Mesa do Mecânico com Auditoria (`UC-08`, `UC-09`, `UC-10`)**
  - **Agent:** `backend-specialist` | **Skills:** `api-patterns`, `tdd-workflow`
  - **Pasta Permitida:** `apps/core-api/`
  - **Dependencies:** `TASK-11A`
  - **INPUT → OUTPUT → VERIFY:** Contratos 6.2 (`/contest`), 6.3 (`/mechanic/queue`, `/resolve`) e 6.5 (Filas Redis) → `MechanicWorkbenchIntegrationTest` cobrindo ordem de prioridade `1→4`, recálculo e `inspection_audit_logs` → `./mvnw verify` passa.

- [x] **`TASK-12` — Agendamento de Galpão, Catálogo/Settings do Gestor e Exportação Lote Google BigQuery (`UC-12`)**
  - **Agent:** `backend-specialist` | **Skills:** `database-design`, `tdd-workflow`
  - **Pasta Permitida:** `apps/core-api/`
  - **Dependencies:** `TASK-11B`
  - **INPUT → OUTPUT → VERIFY:** Contratos 6.2 (`/appointment`), 6.4 (`/catalog/*`, `/settings`, `/analytics/summary`) e Seção 5.3 → `AppointmentAndOutboxIntegrationTest` → `./mvnw clean verify` passa 100%.

- [x] **`TASK-13` — Módulos FastAPI: Worker Gemini Vision + CRAG (`pgvector` + Modo Mock Offline) e Minerador `loja.mottu.com.br`**
  - **Agent:** `backend-specialist` | **Skills:** `python-patterns`, `tdd-workflow`
  - **Pasta Permitida:** `apps/intelligence-service/`
  - **Dependencies:** `TASK-08`
  - **INPUT → OUTPUT → VERIFY:** Contratos 6.5 (Filas Redis + `/internal/v1/catalog/sync` respeitando `price_overridden` + `/internal/v1/feedback/polish`) → Testes `test_catalog_sync.py` e worker com `GEMINI_MOCK_MODE=true` → `pytest --cov=app/domain --cov-fail-under=100` passa.


---

### FASE 5 — Front-end Angular 19 Standalone + Signals Fiel ao `.pen`
- [x] **`TASK-14` — Inicializar Angular 19 (`Standalone` + `Signals`) + Tailwind + PWA + Tokens `DESIGN.md` + Tela `T01`**
  - **Agent:** `frontend-specialist` | **Skills:** `frontend-architecture`, `tailwind-patterns`
  - **Pasta Permitida:** `apps/web-app/`
  - **Dependencies:** `TASK-02` *(Usa `design/mottu-smart-ops.pen` como referência visual direta sem travar execução noturna)*
  - **INPUT → OUTPUT → VERIFY:** `DESIGN.md` + `design/mottu-smart-ops.pen` → Configurar tema sem roxo, guardiões de perfil, interceptor `X-Correlation-Id`, cliente SSE via `fetch` e tela `T01` (Login/Cadastro + Demo Mode) → `npm run lint && npm run test:ci && npm run build` passa.

- [x] **`TASK-15` — Implementar Telas Mobile do Entregador (`T02`, `T03`, `T04`) com Guia de Câmera, Compressão WebP e SSE**
  - **Agent:** `frontend-specialist` | **Skills:** `frontend-design`, `tdd-workflow`
  - **Pasta Permitida:** `apps/web-app/src/app/features/rider/`, `apps/web-app/src/app/shared/`
  - **Dependencies:** `TASK-14`, `TASK-11B`
  - **INPUT → OUTPUT → VERIFY:** Frames `T02–T04` do `.pen` → Testes Vitest + Garagem, Câmera com alerta de escuro, upload direto `PUT`, barra ao vivo SSE, Agendamento e botão *"Discordo"* → `npm run test:ci && npm run build` passa.

- [x] **`TASK-16A` — Implementar Console Web da Oficina (`T05`, `T06`, `T07`: Fila Priorizada, Mesa com Zoom e Frota)**
  - **Agent:** `frontend-specialist` | **Skills:** `frontend-design`, `tdd-workflow`
  - **Pasta Permitida:** `apps/web-app/src/app/features/mechanic/`
  - **Dependencies:** `TASK-14`, `TASK-11B`
  - **INPUT → OUTPUT → VERIFY:** Frames `T05–T07` do `.pen` → Testes Vitest + Fila `1→4`, Mesa assimétrica `55%/45%` com lupa na foto e tabela de motos → `npm run test:ci && npm run build` passa.

- [x] **`TASK-16B` — Implementar Painel Web do Gestor (`T08`, `T09`, `T10`: Aprovações, Catálogo Mottu Store e Iframe Looker Studio)**
  - **Agent:** `frontend-specialist` | **Skills:** `frontend-design`, `tdd-workflow`
  - **Pasta Permitida:** `apps/web-app/src/app/features/manager/`
  - **Dependencies:** `TASK-14`, `TASK-12`
  - **INPUT → OUTPUT → VERIFY:** Frames `T08–T10` do `.pen` → Testes Vitest + Aprovação com vínculo de moto, botão Sync `loja.mottu.com.br`, edição inline de preço/mão de obra e Iframe responsivo do Looker Studio → `npm run test:ci && npm run build` passa.

---

### FASE 6 — Testes Ponta a Ponta, Guia de Estudo & Verificação Final (Phase X)
- [x] **`TASK-17` — Criar `docs/GUIA_ESTUDO_E_LOOKER.md` (Explicação Didática p/ Entrevista + Receita de Bolo do Looker Studio) e `README.md`**
  - **Agent:** `documentation-writer` | **Skills:** `documentation-templates`
  - **Pasta Permitida:** `docs/`, `README.md`
  - **Dependencies:** `TASK-13`, `TASK-16B`
  - **INPUT → OUTPUT → VERIFY:** Código construído → Gerar `README.md` de alto impacto para LinkedIn + Guia Didático explicando cada tecnologia sem jargão + Passo a Passo do BigQuery/Looker → Revisão completa.

- [x] **`TASK-18` — Phase X: Bateria Final E2E (Playwright), Segurança, Acessibilidade e Subida Docker Completa**
  - **Agent:** `qa-automation-engineer` + `security-auditor` | **Skills:** `webapp-testing`, `vulnerability-scanner`, `verify-changes`
  - **Pasta Permitida:** Repositório completo (somente suíte E2E e ajustes finais)
  - **Dependencies:** `TASK-15`, `TASK-16A`, `TASK-16B`, `TASK-17`
  - **INPUT → OUTPUT → VERIFY:** Subir stack completa via Docker Compose → Rodar Playwright E2E + scripts de auditoria → Todos os checks verdes!

---

## 6. Phase X: Checklist Obrigatório de Conclusão
- [x] Zero códigos de cor roxo/violeta (`Purple Ban` respeitado em CSS, HTML e `.pen`).
- [x] `python .agents/skills/vulnerability-scanner/scripts/security_scan.py .` sem falhas críticas.
- [x] `python .agents/skills/frontend-design/scripts/ux_audit.py .` aprovado.
- [x] Cobertura `100%` verificada nas regras de negócio (`domain/` Java e Python).
- [x] Build completo de produção (`Docker Compose`) saudável e abaixo de `1,2 GB` de RAM.
