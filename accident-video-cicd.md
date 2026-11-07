# accident-video-cicd — Plano de Expansão v3.0 (Auto-Relato de Acidente por Vídeo + CI/CD GitHub Actions)

## 1. Overview & Project Type
* **O quê & Por quê:** Novo módulo operacional de **Auto-Relato de Acidente / Sinistro com Vídeo Guiado e Orçamento Automático de Peças Novas**, integrado a uma esteira completa de **CI/CD no GitHub Actions**. Enquanto o módulo atual (`Vistoria`) julga dúvidas de responsabilidade por foto (pré-existente vs. desgaste vs. mau uso), este módulo atende o entregador que sofreu um acidente, precisa registrar o protocolo rapidamente por vídeo, saber na hora a lista e o preço individual de todas as peças novas a substituir + soma total, e já enviar o dossiê mastigado para conferência física na Oficina.
* **Project Type:** `WEB` (Angular 19 Standalone + Signals Mobile-First PWA + Console Oficina/Gestor) + `BACKEND` (Java 21 Spring Boot 3.4 Hexagonal + Python 3.12 FastAPI Multimodal Video/CRAG) + `DEVOPS` (GitHub Actions CI/CD).
* **Branch Dedicada:** `feature/accident-video-cicd` (criada logo após o commit baseline da v2.0 na `main`).

---

## 2. Regras de Negócio Fechadas (Aprovadas pelo Usuário para TDD 100%)
1. **Entrada Multimodal Guiada (Máxima Precisão da IA):**
   * **1 Vídeo Guiado obrigatório** (`MP4` ou `WebM`, até `30 segundos` / `30 MB`) com roteiro na tela (*Frente → Direita → Traseira → Esquerda → Foco na batida*) + **até 3 fotos closes opcionais** (`JPG/PNG/WEBP` até `10 MB` cada), enviados direto ao `MinIO` via URL pré-assinada.
   * **Checklist rápido de emergência:** *(a)* Houve feridos? (se sim, exibe alerta imediato de socorro `192/193`), *(b)* Envolveu terceiros / B.O.?, *(c)* A moto consegue rodar (`DRIVABLE`) ou precisa de recolhimento/guincho (`NEEDS_TOWING`)?, *(d)* Odômetro e relato curto.
2. **Pipeline de IA para Vídeo (`Gemini 2.5 Flash` + `CRAG pgvector` + Deduplicação):**
   * O Gemini analisa o vídeo temporalmente + fotos extras + modelo da moto e retorna JSON estruturado com cada peça avariada, o **timestamp (`segundos`)** em que aparece no vídeo e indicador de **risco estrutural oculto** (`structuralRiskAlert`, ex.: possível empenamento de bengala/chassi).
   * **Deduplicação obrigatória de domínio:** Se a mesma peça do catálogo aparecer em momentos diferentes do vídeo (ex.: `00:04` e `00:25`), o domínio consolida em **1 único item de reposição** (evitando cobrança duplicada).
   * **Reposição por Peças Novas:** No módulo de acidente, **toda** peça danificada identificada entra como troca por peça nova com o preço unitário oficial de `catalog_parts` (`loja.mottu.com.br`).
3. **Fórmula Matemática Exata do Orçamento de Acidente:**
   * `Subtotal Peças Novas` = Soma de (`unit_price_brl * quantity`) de cada peça única identificada.
   * `Mão de Obra Escalonada` (editável em `system_settings`):
     * `0 peças` → `R$ 0,00`
     * `1 a 2 peças` → **`R$ 35,00`** (`ACCIDENT_LIGHT_LABOR_FEE_BRL`)
     * `3 ou mais peças` → **`R$ 90,00`** (`ACCIDENT_HEAVY_LABOR_FEE_BRL`)
   * `Taxa de Guincho / Recolhimento`:
     * `DRIVABLE` (moto consegue ir rodando ao galpão) → **`R$ 0,00`**
     * `NEEDS_TOWING` (moto parada / sem condição segura) → **`R$ 80,00`** (`ACCIDENT_TOWING_FEE_BRL`) + status da moto vai para `IN_MAINTENANCE`.
   * `Total Estimado` = `Subtotal Peças Novas + Mão de Obra Escalonada + Taxa de Guincho`.
4. **Fluxo Duplo Imediato (Entregador + Oficina):**
   * Assim que a IA conclui (`ESTIMATED_PENDING_WORKSHOP`), o entregador recebe **na hora via SSE** o orçamento completo item a item (`T12`) **E** o dossiê completo (vídeo com timestamps + fotos + checklist + peças pré-selecionadas) entra automaticamente no painel da Oficina (`T13`) para conferência presencial quando a moto chegar ao galpão (`WORKSHOP_CONFIRMED`).

---

## 3. Success Criteria (Critérios Medíveis de Sucesso)
- [ ] Novas telas `T11`, `T12` e `T13` (+ atalho em `T02`) prototipadas em `design/mottu-smart-ops.pen` mantendo 100% os tokens Asphalt Telemetry (`#0A0E0D`, `#121816`, `#00E676`, `#FFB300`, `#FF4D4F` e **zero roxo**).
- [ ] **100% de cobertura de testes unitários** mantida em `domain/` no Java (`JaCoCo LINE 100%` + `DomainPurityTest`) e no Python (`pytest-cov 100%` + pureza AST).
- [ ] Pipeline de vídeo funcionando nos **2 modos**: `GEMINI_MOCK_MODE=true` (offline instantâneo para CI/testes) e `GEMINI_MOCK_MODE=false` (SDK oficial `google-genai` com File API / inline bytes pronto para a chave real do usuário).
- [ ] Workflows `.github/workflows/ci.yml` e `.github/workflows/cd.yml` criados, validados sintaticamente e cobrindo Java, Python, Angular, Segurança, Purple Ban e Playwright E2E.
- [ ] Orçamento de RAM em produção mantido dentro de `~1,15 GB` (upload/leitura de vídeo via streaming/MinIO sem explodir heap).

---

## 4. Tech Stack & Decisões Técnicas
* **Design System:** `DESIGN.md` + `design/mottu-smart-ops.pen` (`pen.dev`).
* **Frontend (`apps/web-app`):** Angular 19 (`Standalone` + `Signals`), captura/validação de vídeo (`HTMLVideoElement` p/ checar duração `<= 30s` e tamanho `<= 30 MB`), upload direto via `fetch PUT` em URL assinada do MinIO, SSE autenticado e player com salto por timestamp (`00:07`).
* **Core API (`apps/core-api`):** Java 21 + Spring Boot 3.4 Hexagonal + Flyway `V3__accident_reports_schema.sql` + Filas Redis dedicadas (`queue:ai_accident_jobs` / `queue:ai_accident_results`) para não bloquear fotos rápidas.
* **Intelligence Service (`apps/intelligence-service`):** Python 3.12 + FastAPI + `google-genai` Multimodal Video (`types.Part.from_bytes` até 20 MB / `client.files.upload` > 20 MB com deleção automática pós-análise) + CRAG `pgvector` + Deduplicador puro.
* **CI/CD (`.github/workflows/`):** GitHub Actions com matriz paralela (Java 21, Python 3.12, Node 22, Playwright E2E, Security/UX Gates) + CD via Docker/SSH protegido por `GitHub Secrets`.

---

## 5. File Structure (Novos Arquivos e Atualizações)
```text
mottu-smart-ops/
├── accident-video-cicd.md                                   # Este plano de execução v3.0
├── .github/
│   └── workflows/
│       ├── ci.yml                                           # Pipeline automático de testes, cobertura 100% e segurança
│       └── cd.yml                                           # Deploy automatizado p/ VM Oracle via GitHub Secrets
├── docs/
│   ├── 01-PMD.md                                            # Atualizado p/ v3.0 (Regras de Acidente + Telas T11–T13)
│   ├── 02-ARQUITETURA-E-APIS.md                             # Atualizado p/ v3.0 (SQL V3, Filas e Rotas /accidents)
│   ├── 03-CASOS-DE-USO-TDD.md                               # Atualizado p/ v3.0 (UC-13 a UC-18)
│   └── PENDENCIAS_DO_HUMANO.md                              # Guia de chaves .env + GitHub Secrets p/ teste real
├── design/
│   └── mottu-smart-ops.pen                                  # Adicionados frames T11, T12 e T13
├── apps/
│   ├── core-api/src/main/
│   │   ├── resources/db/migration/V3__accident_reports_schema.sql
│   │   └── java/com/mottu/ops/ (domain, application, infrastructure p/ AccidentReport)
│   ├── intelligence-service/app/
│   │   ├── domain/accident_deduplicator.py                  # Deduplicação pura de peças vistas no vídeo
│   │   └── modules/accident_ai/                             # Gemini Video Adapter + Mock determinístico
│   └── web-app/src/app/features/
│       ├── rider/pages/accident-*                           # T11 (Relato + Vídeo) e T12 (Orçamento Peças Novas)
│       └── mechanic/pages/t13-accident-dossier/             # T13 (Dossiê de Sinistro c/ Player e Confirmação)
└── e2e/tests/                                               # Novos testes E2E do fluxo de Acidente por Vídeo
```

---

## 6. Task Breakdown (Ordem Estrita TDD & Multi-Agente)

- [ ] **`TASK-19` — Baseline Git (`main` → `feature/accident-video-cicd`) + Especificações v3.0 (`PMD`, `APIs`, `TDD`, `PENDENCIAS`)**
  - **Agent:** `project-planner` + `devops-engineer` | **Skills:** `plan-writing`, `architecture`
  - **Pasta Permitida:** `.git/`, `docs/`, `mottu-smart-ops.md`, `.env.example`
  - **Dependencies:** Aprovação deste plano pelo usuário
  - **INPUT → OUTPUT → VERIFY:** Decisões aprovadas → Commit limpo da v2.0 na `main`, criação da branch `feature/accident-video-cicd` e atualização de `docs/01-PMD.md`, `02-ARQUITETURA-E-APIS.md`, `03-CASOS-DE-USO-TDD.md` (`UC-13` a `UC-18`) e `PENDENCIAS_DO_HUMANO.md` → `git branch --show-current` retorna `feature/accident-video-cicd`.

- [ ] **`TASK-20` — Prototipar Telas `T11`, `T12` e `T13` (+ Atalho `T02`) em `design/mottu-smart-ops.pen`**
  - **Agent:** `frontend-specialist` | **Skills:** `design-spec`, `frontend-design`
  - **Pasta Permitida:** `design/`, `scripts/`
  - **Dependencies:** `TASK-19`
  - **INPUT → OUTPUT → VERIFY:** `DESIGN.md` + Seção de telas v3.0 → Gerar frames de alta fidelidade `T11` (Vídeo Guiado + Emergência), `T12` (Tabela de Peças Novas + Preços + Total) e `T13` (Dossiê Vídeo na Oficina) sem nenhum tom roxo → Validação JSON dos 13 frames + `ux_audit.py` PASS.

- [ ] **`TASK-21` — TDD Domínio Java (`RED → GREEN`): `AccidentReport`, `AccidentBudgetCalculator` e Máquina de Estados (`UC-13`, `UC-14`)**
  - **Agent:** `test-engineer` → `backend-specialist` | **Skills:** `tdd-workflow`, `clean-code`
  - **Pasta Permitida:** `apps/core-api/src/test/java/com/mottu/ops/domain/`, `apps/core-api/src/main/java/com/mottu/ops/domain/`
  - **Dependencies:** `TASK-19`
  - **INPUT → OUTPUT → VERIFY:** Regras de deduplicação, soma item a item, mão de obra escalonada (`R$ 35` vs `R$ 90`), guincho (`R$ 80`) e bloqueio `IN_MAINTENANCE` → Escrever testes unitários primeiro (`RED`) e código puro sem Spring/JPA (`GREEN`) → `./mvnw test jacoco:check` passa com **100% LINE coverage** e `DomainPurityTest` verde.

- [ ] **`TASK-22` — TDD Domínio Python (`RED → GREEN`): Deduplicador Temporal de Vídeo e Avaliador de Peças Novas (`UC-15`)**
  - **Agent:** `test-engineer` → `backend-specialist` | **Skills:** `tdd-workflow`, `python-patterns`
  - **Pasta Permitida:** `apps/intelligence-service/tests/`, `apps/intelligence-service/app/domain/`
  - **Dependencies:** `TASK-19`
  - **INPUT → OUTPUT → VERIFY:** Casos de mesma peça em múltiplos timestamps, baixa confiança e dano estrutural → Criar `test_accident_deduplicator.py` (`RED`) e `accident_deduplicator.py` puro (`GREEN`) → `pytest --cov=app/domain --cov-fail-under=100` e `test_domain_purity.py` passam 100%.

- [ ] **`TASK-23` — Back-end Java Core API: Flyway `V3`, Upload Vídeo MinIO, Filas Redis, SSE e Rotas Entregador/Oficina (`UC-16`, `UC-17`)**
  - **Agent:** `database-architect` + `backend-specialist` | **Skills:** `database-design`, `api-patterns`, `tdd-workflow`
  - **Pasta Permitida:** `apps/core-api/`
  - **Dependencies:** `TASK-21`
  - **INPUT → OUTPUT → VERIFY:** Contratos REST `/api/v1/rider/accidents/**` e `/api/v1/mechanic/accidents/**` + `V3__accident_reports_schema.sql` → `AccidentReportIntegrationTest` (`RED → GREEN`) cobrindo presigned URL de vídeo (`<= 30MB`), idempotência, cálculo automático e confirmação pelo mecânico → `./mvnw clean verify` passa 100%.

- [ ] **`TASK-24` — Serviço Python FastAPI: Worker `queue:ai_accident_jobs`, Gemini Multimodal Video SDK + Mock Offline (`UC-15`)**
  - **Agent:** `backend-specialist` | **Skills:** `python-patterns`, `tdd-workflow`
  - **Pasta Permitida:** `apps/intelligence-service/`
  - **Dependencies:** `TASK-22`
  - **INPUT → OUTPUT → VERIFY:** Contrato da fila de vídeo + SDK `google-genai` (inline + File API c/ cleanup) + CRAG `pgvector` filtrando pelo modelo da moto → Testes de integração `test_accident_worker.py` em modo mock e teste de fallback `AI_UNAVAILABLE` → `pytest` 100% verde.

- [ ] **`TASK-25` — Front-end Angular 19: Telas do Entregador (`T02` Atalho + `T11` Vídeo Guiado + `T12` Orçamento de Peças Novas) & Oficina (`T13`)**
  - **Agent:** `frontend-specialist` | **Skills:** `frontend-architecture`, `tailwind-patterns`, `tdd-workflow`
  - **Pasta Permitida:** `apps/web-app/`
  - **Dependencies:** `TASK-20`, `TASK-23`
  - **INPUT → OUTPUT → VERIFY:** Frames `T11–T13` do `.pen` → Testes Vitest (`RED → GREEN`) + gravação/seleção de vídeo com validação de 30s, upload MinIO, SSE ao vivo, tabela de peças novas com clique no segundo do vídeo e tela `T13` do mecânico → `npm run lint && npm run test:ci && npm run build` passa.

- [ ] **`TASK-26` — Esteira Completa CI/CD no GitHub Actions (`.github/workflows/ci.yml` e `cd.yml`) + Script de Smoke Test Real**
  - **Agent:** `devops-engineer` + `security-auditor` | **Skills:** `deployment-procedures`, `bash-linux`, `vulnerability-scanner`
  - **Pasta Permitida:** `.github/`, `scripts/`, `README.md`, `docs/`
  - **Dependencies:** `TASK-23`, `TASK-24`, `TASK-25`
  - **INPUT → OUTPUT → VERIFY:** Criar `.github/workflows/ci.yml` (Java + Python + Angular + Playwright E2E + Security + Purple Ban) e `cd.yml` (Build Docker + SSH Deploy condicional na VM Oracle) + script `scripts/verify_env_keys.sh` para validar as chaves reais do usuário → Validação YAML + simulação local de todos os steps da CI.

- [ ] **`TASK-27` — Phase X: Suíte E2E Playwright do Módulo de Acidente, Auditoria Final e Preparação p/ Chaves Reais (`UC-18`)**
  - **Agent:** `qa-automation-engineer` + `security-auditor` | **Skills:** `webapp-testing`, `verify-changes`
  - **Pasta Permitida:** `e2e/`, `docs/HISTORICO_IA.md`, `accident-video-cicd.md`, `mottu-smart-ops.md`
  - **Dependencies:** `TASK-25`, `TASK-26`
  - **INPUT → OUTPUT → VERIFY:** Rodar stack completa + Playwright E2E cobrindo relato de acidente com vídeo, orçamento item a item, guincho e confirmação na oficina + todos os portões de qualidade → 100% dos testes verdes!

---

## 7. Phase X: Verification Checklist (Obrigatório Antes de Encerrar)
- [ ] Zero códigos roxos/violetas (`Purple Ban`) em `.pen`, Angular e CSS.
- [ ] `python3 .agents/skills/vulnerability-scanner/scripts/security_scan.py . --fail-on critical` sem falhas.
- [ ] `python3 .agents/skills/frontend-design/scripts/ux_audit.py .` aprovado (`PASS`).
- [ ] Java `./mvnw clean verify` (`DomainPurityTest` + `JaCoCo 100%` em `domain/`).
- [ ] Python `pytest --cov=app/domain --cov-fail-under=100` (`100%` cobertura + pureza AST).
- [ ] Angular `npm run lint && npm run test:ci && npm run build` sem erros.
- [ ] Playwright E2E (`npx playwright test`) 100% verde (incluindo jornada completa de Acidente por Vídeo).
- [ ] Workflows `.github/workflows/ci.yml` e `cd.yml` validados e prontos para `git push`.

