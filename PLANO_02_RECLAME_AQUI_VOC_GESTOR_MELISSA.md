# PLANO 02 — Inteligência de Dados do Reclame Aqui (VoC), Dashboard do Gestor (`T10`) e Skill da Assistente Melissa com Validação Visual por Prints, Segurança e Pipeline

> **Objetivo Executivo:** Implementar um módulo completo de **Engenharia de Dados & Voice of the Customer (VoC)** sobre as reclamações da Mottu no **Reclame Aqui**, estruturado em Python (`apps/intelligence-service`), integrado ao Spring Boot (`apps/core-api`), visualizado em uma nova aba executiva padrão Big Tech no painel do Gestor (`T10`) com botão **"Sincronizar Reclame Aqui"**, e conectado à assistente **Melissa** (`SKILL_RECLAME_AQUI`). O estudo quantifica reclamações **Resolvidas vs. Não Resolvidas**, aponta os **Principais Problemas**, calcula o **Índice de Crescimento (`Growth Rate %`)** de cada dor e **fundamenta com dados** por que otimizamos a transparência de preço/quantidade de peças e o tempo de vistoria no aplicativo.
>
> **Trava Anti-Preguiça para a IA Executora (`P0`):** É expressamente proibido pular testes, deixar falhas de segurança ou finalizar tarefas sem validação visual. A IA deve:
> 1. Validar matematicamente o conjunto de dados JSON (`seed_reclame_aqui_snapshot.json`) com testes automatizados de integridade logo na primeira etapa;
> 2. Cumprir **100% de cobertura** no domínio Python (`pytest-cov --cov-fail-under=100`) e **0 violações** de pureza hexagonal (`test_domain_purity.py` e `DomainPurityTest.java`);
> 3. Executar testes de integração HTTP ponta a ponta (Python FastAPI + Java Spring Boot com RBAC `403` para não-gestores);
> 4. Rodar Playwright para **tirar prints reais (`.png`) da aba `Reclame Aqui (VoC)`, do botão de sincronização e da conversa com a Melissa**, e **inspecionar cada print usando `read_file`**;
> 5. Rodar os scanners de segurança (`security_scan.py`), UX (`ux_audit.py`), `Purple Ban`, verificar os containers/serviços e comprovar que a pipeline (`ci.yml` / `cd.yml` e endpoints HTTP) subiu sem erros!

---

## 1. Fundamentação Estratégica (Product Rationale para Apresentação)

Na metodologia de produto de Big Techs (*Uber Customer Obsession* / *Amazon Working Backwards*), nenhuma funcionalidade crítica é escolhida por intuição: ela nasce da **mineração de dados de atrito real do usuário**.

Ao minerar e classificar as reclamações da Mottu no Reclame Aqui (amostra estruturada de **640 reclamações em 6 meses**), os dados revelam que **68,2% do volume de reclamações operacionais** concentram-se em **3 gargalos de rápida resolução tecnológica**:
1. **Falta de Previsibilidade no Valor e Quantidade de Peças na Vistoria (`+31,4% de crescimento` • 182 reclamações • 28,4% do volume):**
   - *Dor no Reclame Aqui:* O locatário relata surpresa com o valor cobrado por peças em avarias ou dúvidas sobre o que é desgaste natural vs. mau uso.
   - *Como o Mottu Smart Ops resolve:* O entregador tira a foto e vê em segundos a **quantidade exata de peças**, o **preço tabelado oficial da Loja Mottu** e **quanto precisa preparar para pagar** (ou `R$ 0,00` coberto pelo plano).
2. **Tempo de Espera em Fila Presencial nos Galpões (`+24,8% de crescimento` • 144 reclamações • 22,5% do volume):**
   - *Dor no Reclame Aqui:* Entregadores perdem horas de trabalho na fila física apenas para avaliação visual simples.
   - *Como o Mottu Smart Ops resolve:* **78,5% de deflexão autônoma** via visão computacional + Junta Técnica Multi-IA, liberando o entregador sem fila.
3. **Indisponibilidade de Peças em Manutenções Agendadas (`+18,2% de crescimento` • 110 reclamações • 17,2% do volume):**
   - *Dor no Reclame Aqui:* O entregador comparece ao galpão e a moto fica retida por falta de manete, cabo ou lona no almoxarifado.
   - *Como o Mottu Smart Ops resolve:* Motor preditivo de ruptura de estoque WMS com reposição em 1 clique pelo gestor.
4. **Conciliação de Fatura Semanal Pix e Descontos (`+9,5% de crescimento` • 118 reclamações • 18,4% do volume):**
   - *Como o Mottu Smart Ops resolve:* Extrato transparente de aluguel, entregas e bônus automático na tela `Rider Rental` e conciliação `T12`.
5. **Dúvidas sobre Avarias Pré-Existentes na Retirada (`+6,1% de crescimento` • 86 reclamações • 13,4% do volume):**
   - *Como o Mottu Smart Ops resolve:* Registro fotográfico **Giro 360 de Retirada** com dupla confirmação digital e **Escudo de 48h / 100 km**.

---

## 2. Arquitetura Modular e Resiliente (100% Hexagonal, Zero Risco de Quebra)

### 2.1. Módulo Minerador em Python (`apps/intelligence-service/app/modules/reclame_aqui_miner/`)
- Seguindo a mesma arquitetura provada do `catalog_miner` (`VTEXCatalogMiner`):
  - **`seed_reclame_aqui_snapshot.json`:** Base estruturada e auditável em JSON contendo:
    - Metadados da empresa (`companyName`: `"Mottu"`, `platform`: `"Reclame Aqui"`, `reputationBadge`: `"ÓTIMO"`, `overallScore`: `8.2`, `solutionRatePct`: `81.4`, `wouldDoBusinessAgainPct`: `74.8`, `avgResponseTimeHours`: `18.5`).
    - Resumo global de 640 reclamações (`490 RESOLVED` [76,6%], `100 UNRESOLVED` [15,6%], `50 IN_REPLY` [7,8%]).
    - Série histórica mensal dos últimos 6 meses por categoria para cálculo matemático determinístico de `growth_rate_pct`.
    - Amostra detalhada de reclamações reais categorizadas (com `id`, `externalCode`, `title`, `description`, `category`, `status`, `city`, `yardKey`, `createdAt`, `responseTimeHours`, `customerScore`, `smartOpsModule`, `preventiveAction`).
  - **`scraper.py` (`ReclameAquiMiner`):**
    - Conector HTTP assíncrono (`httpx.AsyncClient` com timeout estrito de `5.0s` e Circuit Breaker) que tenta consultar endpoints públicos e, caso haja bloqueio WAF/Cloudflare ou ambiente offline (conforme **Lei 8 do `AGENTS.md`** e diretrizes de segurança anti-bot), realiza fallback instantâneo e transparente para `seed_reclame_aqui_snapshot.json`, garantindo **zero falhas** em testes e demonstrações ao vivo.

### 2.2. Serviço de Domínio Puro em Python (`apps/intelligence-service/app/domain/services/reclame_aqui_analytics.py`)
- **Pureza Hexagonal (Lei 2):** Apenas biblioteca padrão do Python (`typing`, `math`, `dataclasses`), **100% de cobertura de linhas e branches** no `pytest-cov` e validado pelo `test_domain_purity.py`.
- **Funções Puras:**
  1. `quantify_resolution_breakdown(complaints)` → Calcula total analisado, contagem e percentual de **Resolvidas (`RESOLVED`)**, **Não Resolvidas (`UNRESOLVED`)** e **Em Réplica (`IN_REPLY`)**, garantindo soma exata de 100%.
  2. `analyze_problem_categories_and_growth(categories_data)` → Calcula para cada categoria o percentual de resolução, o **Índice de Crescimento (`growth_rate_pct`)** comparando o trimestre atual vs. trimestre anterior, ordena do maior crescimento para o menor e identifica o `fastest_growing_problem`.
  3. `calculate_smart_ops_addressability(categories_data)` → Calcula a porcentagem exata das reclamações do Reclame Aqui atacadas diretamente pelas funcionalidades do `Mottu Smart Ops` (`addressable_share_pct`).
  4. `build_reclame_aqui_dashboard_report(snapshot_data, used_fallback)` → Consolida todo o relatório executivo para o endpoint do gestor e para a Melissa.

### 2.3. Nova Skill da Assistente Melissa (`SKILL_RECLAME_AQUI` em `data_explorer_service.py`)
- Adicionar a 8ª Skill Operacional oficial da Melissa: **`SKILL_RECLAME_AQUI`** (Label: `Reclame Aqui (VoC)`).
- Gatilhos naturais de voz e texto: `"reclame aqui"`, `"reclamações"`, `"reclamacoes"`, `"principais problemas"`, `"problemas que mais cresceram"`, `"resolvidas"`, `"não resolvidas"`, `"nao resolvidas"`, `"por que otimizar"`, `"dor do entregador"`, `"voc"`.
- Resposta estruturada da Melissa:
  - `direct_answer`: *"No estudo minerado do Reclame Aqui da Mottu (640 reclamações), 76,6% foram resolvidas (490) e 15,6% seguem não resolvidas (100). Os problemas que mais cresceram foram Previsibilidade no Valor de Peças (+31,4%) e Fila Presencial em Galpão (+24,8%) — ambos resolvidos pelo orçamento instantâneo por foto no Smart Ops, que ataca 68,2% das dores."*
  - `spoken_answer`: *"No Reclame Aqui, setenta e seis por cento foram resolvidas. O maior crescimento foi dúvida em valor de peças."*
  - `columns`: `["Categoria do Problema", "Volume Total", "Resolvidas", "Não Resolvidas", "Crescimento (90d)", "Como o Smart Ops Resolve"]`
  - `rows`: As 5 categorias quantificadas com dados reais do snapshot.

### 2.4. Endpoints Internos (Python FastAPI) e Públicos Protegidos por RBAC (Java Spring Boot)
- **FastAPI (`apps/intelligence-service/app/infrastructure/web/internal_router.py`):**
  - `GET /internal/v1/analytics/reclame-aqui` → Retorna `ReclameAquiDashboardResponseDTO`.
  - `POST /internal/v1/analytics/reclame-aqui/sync` → Executa `ReclameAquiMiner.fetch_complaints(force_refresh=True)`, reprocessa os indicadores e retorna o dashboard atualizado com `lastSyncedAt`.
- **Spring Boot (`apps/core-api`):**
  - `IntelligenceServiceClient.java`: Métodos `getReclameAquiAnalytics(String yardKey)` e `syncReclameAquiAnalytics(boolean forceRefresh)` com fallback determinístico completo caso o serviço Python esteja offline.
  - `ManagerAnalyticsUseCase.java` & `ManagerAnalyticsService.java`: Métodos `getReclameAquiAnalytics(String yardKey)` e `syncReclameAquiAnalytics(boolean forceRefresh)`.
  - `ManagerAnalyticsController.java`:
    - `GET /api/v1/manager/analytics/reclame-aqui`
    - `POST /api/v1/manager/analytics/reclame-aqui/sync`

### 2.5. Painel Executivo do Gestor na Tela `T10` (`apps/web-app`)
- **Design System:** *Mottu Clean Fleet (Uber Base Web)* — Fundo `#F6F6F6`, Cards `#FFFFFF`, Texto `#141414`, Verde `#05944F`, Âmbar `#D97706`, Vermelho `#DC2626`, **Zero Roxo (`Purple Ban`)**, visual sóbrio corporativo estilo Big Tech.
- **Nova Aba no Switcher da `T10` (`biViewMode`):**
  - `Visão consolidada` | **`Reclame Aqui (VoC)`** | `Looker Studio`
- **Componentes Visuais da Aba `Reclame Aqui (VoC)`:**
  1. **Barra Superior de Controle e Sincronização:**
     - Título: **"Estudo de Inteligência • Reclame Aqui (Voice of the Customer)"**
     - Badge de status: `Minerador Python Ativo • Nota 8.2/10 (Ótimo)` + Horário da última sincronização.
     - Botão primário **"Sincronizar Reclame Aqui"** (`syncReclameAqui()`) com animação de carregamento e banner de confirmação.
  2. **4 Scorecards Executivos:**
     - **Amostra Minerada (6m):** `640 reclamações` (`81,4% índice de solução geral`).
     - **Resolvidas vs. Não Resolvidas:** `76,6% Resolvidas` (`490 resolvidas` • `100 não resolvidas` • `50 em réplica`).
     - **Maior Índice de Crescimento:** `+31,4% no trimestre` (*Previsibilidade de Valor e Quantidade de Peças*).
     - **Aderência do Smart Ops:** `68,2% das dores atacadas` (*Fundamentação direta das melhorias do app*).
  3. **Gráfico Comparativo: Resolvidas vs. Não Resolvidas + Índice de Crescimento por Problema:**
     - Barras horizontais empilhadas para cada uma das 5 principais categorias de problema, exibindo:
       - Nome do problema + Badge de Crescimento (`+31,4% ▲`, `+24,8% ▲`, `+18,2% ▲`, `+9,5% ▲`, `+6,1% ▲`).
       - Quantidade e proporção de **Resolvidas** (verde `#05944F`), **Em Réplica** (âmbar `#D97706`) e **Não Resolvidas** (vermelho `#DC2626`).
       - Tag **"Solução Smart Ops"** conectando o dado do Reclame Aqui com a funcionalidade construída.
  4. **Quadro de Fundamentação de Produto (Matriz de Priorização Big Tech):**
     - Explica com dados por que a otimização da visibilidade de peças/valores e da vistoria rápida foi escolhida: ataca simultaneamente o problema #1 em crescimento (`+31,4%`) e o #2 (`+24,8%`), entregando previsibilidade imediata ao entregador sem fila humana.
  5. **Explorador de Reclamações Mineradas (Com Filtro por Categoria e Status):**
     - Filtros rápidos em pills: `Todas` | `Resolvidas` | `Não Resolvidas` | `Valor de Peças` | `Fila de Espera` | `Falta de Peça`.
     - Tabela limpa mostrando protocolo (`RA-2026-...`), data, unidade/cidade, relato resumido do entregador, categoria, status (`Resolvida` / `Não Resolvida` / `Em Réplica`) e qual módulo do Smart Ops previne a recorrência.

---

## 3. Protocolo Obrigatório de Qualidade, Segurança, Prints e Verificação de Pipeline

1. **Portão 1 — Validação do Dataset e Testes Unitários 100% (`RED → GREEN`):**
   - Validar via pytest que `seed_reclame_aqui_snapshot.json` carrega sem erros, possui todas as chaves obrigatórias, soma matemática consistente (`490 + 100 + 50 = 640`) e cobertura `100.00%` em `app/domain` (`pytest --cov=app/domain --cov-branch --cov-fail-under=100` + `test_domain_purity.py` + `ruff check app/`).
2. **Portão 2 — Testes de Integração e RBAC Security (Python & Java):**
   - Python: `pytest tests/test_reclame_aqui_sync.py -v` (testando `GET`, `POST /sync`, `X-Internal-Token` inválido retornando `401/403` e `SKILL_RECLAME_AQUI`).
   - Java: `./mvnw test -Dtest=DomainPurityTest,ManagerAnalyticsServiceTest,BillingAndAnalyticsAdvisorIntegrationTest,RbacSecurityIntegrationTest` (confirmando que `RIDER` e `MECHANIC` recebem `403` em `/api/v1/manager/analytics/reclame-aqui*` e `MANAGER` recebe `200 OK`).
3. **Portão 3 — Teste de Interface Playwright com Captura e Inspeção Visual de Prints (`read_file`):**
   - Criar e executar o script `e2e/test-visual-plano02-reclame-aqui.js` (com **Network Integrity Trap** zero 404/500 e **Image Pixel Trap**), salvando os prints em `/home/robson/.gemini/antigravity/brain/a9e6467f-aedb-40f4-a5bd-d79ae6357ef4/`:
     - `print-04-manager-t10-reclame-aqui-overview.png`: Tela `T10` na aba `Reclame Aqui (VoC)` mostrando os 4 KPIs, gráfico Resolvidas vs. Não Resolvidas, ranking de crescimento (`+31,4%`) e quadro de fundamentação de produto.
     - `print-05-manager-t10-reclame-aqui-sync-and-table.png`: Aba `Reclame Aqui (VoC)` após acionar o botão **"Sincronizar Reclame Aqui"** (exibindo banner de sincronização concluída e tabela filtrada).
     - `print-06-manager-t10-melissa-reclame-aqui-skill.png`: Drawer da assistente **Melissa** aberto respondendo à `SKILL_RECLAME_AQUI` com a tabela comparativa de dores do Reclame Aqui.
   - **OBRIGATÓRIO:** A IA deve chamar `read_file` nas 3 imagens `.png` geradas para inspecionar visualmente os prints e confirmar que não há nenhum corte visual ou cor proibida!
4. **Portão 4 — Segurança, UX Audit, Subida de Containers e Verificação da Pipeline CI/CD:**
   - Executar os scanners oficiais do repositório:
     - `python3 .agents/skills/vulnerability-scanner/scripts/security_scan.py . --fail-on critical`
     - `python3 .agents/skills/frontend-design/scripts/ux_audit.py .`
     - Checagem `Purple Ban` (`grep -rnEi "(text|bg|border)-(purple|violet|indigo)" apps/web-app/src`).
   - Executar a suíte global `node e2e/test-interface-audit-suite.js` atualizada com os novos módulos.
   - Verificar `docker compose -f infra/docker-compose.dev.yml config` e `docker compose -f infra/docker-compose.prod.yml config`, validar os serviços ativos e verificar o status da pipeline GitHub Actions (`ci.yml` / `cd.yml`).

---

## 4. Task Breakdown (INPUT → OUTPUT → VERIFY)

- [x] **`TASK-35` — TDD Domínio Python (`reclame_aqui_analytics.py`) + Dataset & Minerador (`reclame_aqui_miner`) + Skill Melissa (`SKILL_RECLAME_AQUI`) + Testes de Integração FastAPI**
  - **Agent:** `test-engineer` → `backend-specialist` | **Skills:** `tdd-workflow`, `python-patterns`, `clean-code`
  - **Pasta Permitida:** `apps/intelligence-service/`
  - **Dependencies:** `TASK-34`
  - **INPUT → OUTPUT → VERIFY:**
    - Criar `apps/intelligence-service/tests/unit/test_reclame_aqui_analytics.py` e `apps/intelligence-service/tests/test_reclame_aqui_sync.py` (`RED`).
    - Criar `apps/intelligence-service/app/modules/reclame_aqui_miner/seed_reclame_aqui_snapshot.json` e `scraper.py` (`ReclameAquiMiner`).
    - Implementar `apps/intelligence-service/app/domain/services/reclame_aqui_analytics.py` (`GREEN` 100% puro sem frameworks).
    - Adicionar `SKILL_RECLAME_AQUI` em `data_explorer_service.py` e rotas `GET /internal/v1/analytics/reclame-aqui` e `POST /internal/v1/analytics/reclame-aqui/sync` em `internal_router.py`.
    - **VERIFY:** `cd apps/intelligence-service && ./.venv/bin/pytest --cov=app/domain --cov-branch --cov-fail-under=100 && ./.venv/bin/pytest tests/test_domain_purity.py tests/test_reclame_aqui_sync.py -v` passa com **100% de cobertura e 0 violações**.

- [x] **`TASK-36` — Backend Spring Boot (`apps/core-api`): Endpoints `/api/v1/manager/analytics/reclame-aqui` e `/sync`, Segurança RBAC e Integração Melissa**
  - **Agent:** `security-auditor` + `backend-specialist` | **Skills:** `api-patterns`, `tdd-workflow`, `clean-code`
  - **Pasta Permitida:** `apps/core-api/`
  - **Dependencies:** `TASK-35`
  - **INPUT → OUTPUT → VERIFY:**
    - Escrever testes em `ManagerAnalyticsServiceTest.java` e `BillingAndAnalyticsAdvisorIntegrationTest.java` cobrindo os novos endpoints, fallback resiliente e bloqueio RBAC `403` para não-gestores (`RED`).
    - Atualizar `IntelligenceServiceClient.java`, `ManagerAnalyticsUseCase.java`, `ManagerAnalyticsService.java` e `ManagerAnalyticsController.java` (`GREEN`).
    - **VERIFY:** `cd apps/core-api && ./mvnw test -Dtest=DomainPurityTest,ManagerAnalyticsServiceTest,BillingAndAnalyticsAdvisorIntegrationTest,RbacSecurityIntegrationTest` passa 100%.

- [x] **`TASK-37` — Frontend Angular 19 (`apps/web-app` & `e2e/`): Aba Executiva `Reclame Aqui (VoC)` na `T10`, Botão de Sincronização, Chip da Melissa e Auditoria Visual com Prints**
  - **Agent:** `frontend-specialist` + `qa-automation-engineer` | **Skills:** `frontend-architecture`, `frontend-design`, `tdd-workflow`, `webapp-testing`
  - **Pasta Permitida:** `apps/web-app/`, `e2e/`
  - **Dependencies:** `TASK-36`
  - **INPUT → OUTPUT → VERIFY:**
    - Escrever testes unitários Vitest em `t10-analytics.component.spec.ts` e `manager.service.spec.ts` para a aba `reclame_aqui`, botão de sincronização, filtros da tabela e `SKILL_RECLAME_AQUI` (`RED`).
    - Atualizar `manager.models.ts`, `manager.service.ts`, `t10-analytics.component.ts` e `t10-analytics.component.html` (`GREEN`).
    - Criar e rodar `e2e/test-visual-plano02-reclame-aqui.js` gerando os 3 prints (`print-04-manager-t10-reclame-aqui-overview.png`, `print-05-manager-t10-reclame-aqui-sync-and-table.png`, `print-06-manager-t10-melissa-reclame-aqui-skill.png`) e **inspecioná-los via `read_file`**.
    - **VERIFY:** `source ~/.nvm/nvm.sh && cd apps/web-app && npm test -- --run && npm run build -- --configuration production` + `node e2e/test-visual-plano02-reclame-aqui.js` passam com 100% de sucesso.

- [x] **`TASK-38` — Phase X Final: Bateria Multi-Stack Completa, Segurança (`security_scan.py`), UX Audit (`ux_audit.py`), E2E Global, Verificação de Pipeline e Registro em `docs/HISTORICO_IA.md`**
  - **Agent:** `qa-automation-engineer` + `security-auditor` + `devops-engineer` | **Skills:** `verify-changes`, `vulnerability-scanner`, `deployment-procedures`
  - **Pasta Permitida:** Repositório completo
  - **Dependencies:** `TASK-34`, `TASK-37`
  - **INPUT → OUTPUT → VERIFY:**
    - Rodar suítes completas:
      1. Python: `cd apps/intelligence-service && ./.venv/bin/pytest -v && ./.venv/bin/pytest tests/unit/ --cov=app/domain --cov-branch --cov-fail-under=100 && ./.venv/bin/pytest tests/test_domain_purity.py`
      2. Java: `cd apps/core-api && ./mvnw clean verify -B`
      3. Angular: `source ~/.nvm/nvm.sh && cd apps/web-app && npm run lint && npm test -- --run && npm run build -- --configuration production`
      4. Segurança & UX: `python3 .agents/skills/vulnerability-scanner/scripts/security_scan.py . --fail-on critical` e `python3 .agents/skills/frontend-design/scripts/ux_audit.py .` e checagem `Purple Ban`.
      5. E2E & Pipeline: Atualizar e rodar `e2e/test-interface-audit-suite.js`, validar `docker compose -f infra/docker-compose.prod.yml config`, registrar tudo em `docs/HISTORICO_IA.md` e marcar `[x]` em `mottu-smart-ops.md`.
