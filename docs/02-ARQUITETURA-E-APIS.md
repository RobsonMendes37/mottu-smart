# 02 — Arquitetura, Banco de Dados e Contratos de API

**Versão:** 2.0 (Blindado contra Ambiguidades para Execução Autônoma e Multi-Agente)  
**Objetivo deste Documento:** Definir com **zero espaço para invenção** todas as portas, variáveis `.env`, pastas, tabelas SQL, máquina de estados e formatos exatos de JSON de **todas** as rotas. Qualquer modelo de IA deve copiar fielmente os nomes deste documento.

---

## 1. Visão Geral das Tecnologias, Portas e Limite de Memória (VM 12 GB)

Todo o sistema mora em **1 único repositório (Monorepo)** e sobe via `Docker Compose` consumindo cerca de **`1,15 GB` de RAM** no total:

| Serviço / Container | Tecnologia Principal | Porta Host (Externa) → Interna Docker | Papel Simples | Limite RAM (Prod) |
| :--- | :--- | :--- | :--- | :--- |
| **`caddy`** | Caddy 2 + Build Angular 19 | `18088:80` *(Prod VM: proxy principal)* | Entrega o PWA, redireciona `/api/*` p/ Java e `/storage/*` p/ MinIO | `40 MB` |
| **`web-app`** *(Dev)* | Angular 19 (`Standalone` + `Signals`) + Vite + Tailwind | `14200:4200` (apenas modo dev) | Interface Mobile do Entregador, Mesa da Oficina e Painel do Gestor | *(Estático no Caddy)* |
| **`core-api`** | Java 21 (`Virtual Threads`) + Spring Boot 3.4 | `18080:8080` | Coração das regras de negócio (Hexagonal), Login JWT/RBAC, Frota e Outbox | `450 MB` |
| **`intelligence-service`** | Python 3.12 + FastAPI | `18000:8000` | Analisa fotos no **Gemini**, busca peças (**CRAG**) e minera `loja.mottu.com.br` | `280 MB` |
| **`postgres`** | PostgreSQL 16 (`pgvector/pgvector:pg16`) | `15432:5432` | Guarda usuários, motos, chamados, auditoria e vetores semânticos (`768d`) | `220 MB` |
| **`minio`** | MinIO (Compatível 100% com AWS S3) | `19000:9000` (API) / `19001:9001` (Painel) | Cofre de fotos das avarias via URL pré-assinada (`Bucket: mottu-inspections`) | `120 MB` |
| **`redis`** | Redis 7 Alpine | `16379:6379` | Filas IA, espera de recuperação, trava `Idempotency-Key` e Rate Limiter | `40 MB` |

> **Nota Anti-Conflito de Portas:** Usamos a faixa externa `1xxxx` (`15432`, `16379`, `18080`, `18000`, `19000`, `18088`) no host para **nunca colidir com outros containers já rodando** no computador local ou na VM compartilhada da Oracle (nunca pare ou altere containers de outros projetos como `nexo-*`). Internamente na rede Docker `mottu-net`, os containers conversam normalmente pelas portas padrão (`5432`, `6379`, `8080`, `8000`, `9000`).

---

## 2. Variáveis de Ambiente Travadas (`.env.example`) e Modo Offline de Testes

> [!TIP]
> **Por que existem chaves `MOCK / ENABLED`?** Para que os testes automatizados (`./mvnw verify`, `pytest`, `Playwright`) rodem **100% offline, rápidos e grátis** mesmo sem internet ou sem chave do Gemini/BigQuery configurada na máquina! Em produção na VM, basta ligar as chaves reais no `.env`.

```ini
# Modo de Operação (false = Seleção Mottu 50/h | true = Portfólio LinkedIn 5/h + Login Demo)
PUBLIC_DEMO_MODE=false

# Banco de Dados e Cache
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=mottu_ops
POSTGRES_USER=mottu
POSTGRES_PASSWORD=mottu_dev_pass
REDIS_HOST=redis
REDIS_PORT=6379

# Segurança JWT e Comunicação Interna Java <-> Python
JWT_SECRET=trocar-em-producao-minimo-32-bytes-mottu-smart-ops-2026
JWT_EXPIRATION_SECONDS=28800
INTERNAL_API_TOKEN=internal-mottu-service-token-2026
INTELLIGENCE_SERVICE_URL=http://intelligence-service:8000

# Armazenamento S3 (MinIO) — Separação obrigatória Rede Interna vs. Navegador!
S3_BUCKET_NAME=mottu-inspections
S3_ACCESS_KEY=minioadmin
S3_SECRET_KEY=minioadmin123
S3_INTERNAL_ENDPOINT=http://minio:9000
S3_PUBLIC_ENDPOINT=http://localhost:19000

# Inteligência Artificial (Gemini) e Loja Mottu (VTEX)
GEMINI_API_KEY=
GEMINI_VISION_MODEL=gemini-2.5-flash
GEMINI_EMBEDDING_MODEL=text-embedding-004
GEMINI_MOCK_MODE=true
VTEX_STORE_BASE_URL=https://www.loja.mottu.com.br

# Google BigQuery e Looker Studio (BI)
BIGQUERY_ENABLED=false
GCP_PROJECT_ID=
BIGQUERY_DATASET=mottu_ops_bi
BIGQUERY_TABLE=fact_inspections
LOOKER_STUDIO_EMBED_URL=https://lookerstudio.google.com/embed/reporting/placeholder
```

### Como resolver a armadilha do `MinIO` no Docker + Anti-Fraude (`IMPORTANTE!`)
1. **Assinatura de Upload (`S3Presigner`):** O navegador/celular do usuário não enxerga o hostname interno `minio:9000`. Por isso, o `core-api` usa **`S3_PUBLIC_ENDPOINT`** (em dev `http://localhost:19000`, em prod `https://seu-dominio/storage` via Caddy) exclusivamente no `S3Presigner` para gerar o `uploadUrl` (`PUT`), garantindo que a assinatura criptográfica AWS S3 V4 nunca dê erro `403 SignatureDoesNotMatch`.
2. **Leitura Interna de Bytes para Anti-Fraude (`S3Client`):** Quando o celular termina o `PUT` da foto e chama `POST /api/v1/rider/inspections`, o `core-api` lê os bytes do arquivo usando **`S3_INTERNAL_ENDPOINT` (`http://minio:9000`)** para:
   * Confirmar que o arquivo existe e tem `<= 10 MB` (`JPG`, `PNG` ou `WEBP`);
   * Calcular no servidor o **`photo_sha256`** real e um hash visual simples (`dHash` 64 bits em hexadecimal);
   * Checar se o hash já existe em outra vistoria no banco **OU** se a imagem tem sinal claro de reprocessamento suspeito sem metadados compatíveis (marcando `fraud_suspected = true` caso positivo).

---

## 3. Estrutura Exata de Pastas (Arquitetura Hexagonal)

> [!IMPORTANT]
> **Trava da Arquitetura Hexagonal:** Dentro de `domain/` (no Java e no Python), **É PROIBIDO** importar frameworks externos (`org.springframework.*`, `jakarta.persistence.*`, `FastAPI`, `SQLAlchemy`). Apenas código puro da linguagem!

```text
mottu-smart-ops/
├── AGENTS.md                                # 7 leis obrigatórias de alinhamento para IAs
├── DESIGN.md                                # Tokens visuais (Mottu Asphalt Telemetry)
├── mottu-smart-ops.md                       # Plano mestre de tarefas
├── .env.example                             # Variáveis padrão para dev/teste
├── design/
│   └── mottu-smart-ops.pen                  # Protótipo visual das 10 telas no Pencil (pen.dev)
├── docs/                                    # Documentação, Casos TDD e Histórico da IA
├── infra/
│   ├── docker-compose.dev.yml               # Ambiente de desenvolvimento local
│   ├── docker-compose.prod.yml              # Produção com limites rígidos (~1,15 GB RAM)
│   └── Caddyfile                            # Proxy HTTPS (/ -> Angular, /api -> Java, /storage -> MinIO)
│
├── apps/
│   ├── web-app/                             # FRONT-END: Angular 19 Standalone + Signals + Tailwind (PWA)
│   │   └── src/app/
│   │       ├── core/                        # Auth JWT, Role Guards, Correlation-ID Interceptor, SSE Client
│   │       ├── shared/                      # Botões 52px, Badges, Máscara Câmera + Luz, Compressor WebP
│   │       └── features/
│   │           ├── auth/                    # T01: Login, Auto-Cadastro e Demo Quick-Login (se ativo)
│   │           ├── rider/                   # T02, T03, T04: Garagem, Nova Vistoria, Resultado/SSE/Agendar
│   │           ├── mechanic/                # T05, T06, T07: Fila Oficina, Mesa Lado-a-Lado, Frota
│   │           └── manager/                 # T08, T09, T10: Aprovar Contas, Catálogo Mottu, Looker BI
│   │
│   ├── core-api/                            # BACK-END CORE: Java 21 + Spring Boot 3.4 (Hexagonal)
│   │   └── src/main/java/com/mottu/ops/
│   │       ├── domain/                      # PURO JAVA 21 (Zero Spring / Zero JPA!)
│   │       │   ├── model/                   # User, Motorcycle, Contract, Inspection, Part, Yard
│   │       │   ├── enums/                   # Todos os Enums oficiais (Seção 4)
│   │       │   ├── service/                 # Calculadoras puras (48h/100km, Orçamento, Fila, Contestação)
│   │       │   └── exception/               # DomainValidationException, BusinessRuleException
│   │       ├── application/
│   │       │   ├── port/in/                 # Interfaces de casos de uso
│   │       │   ├── port/out/                # Interfaces de saída (Repositórios, S3, Redis, IA, BigQuery)
│   │       │   └── usecase/                 # Implementação dos casos de uso
│   │       └── infrastructure/
│   │           ├── adapter/in/web/          # Controllers REST + SSE + GlobalExceptionHandler (RFC 7807)
│   │           ├── adapter/in/queue/        # Consumidor Redis de resultados IA + Job SLA de Recuperação
│   │           ├── adapter/out/persistence/ # Entidades JPA + Repositórios + Flyway SQL (V1 e V2)
│   │           ├── adapter/out/storage/     # S3StorageAdapter (MinIO Presigner + Leitor SHA256/dHash)
│   │           ├── adapter/out/redis/       # Filas Redis, Idempotency-Key e Rate Limiter
│   │           ├── adapter/out/ai/          # Cliente HTTP interno p/ FastAPI (Sync Catálogo + Polimento)
│   │           ├── adapter/out/bigquery/    # Exportador em lote (Outbox Batch NDJSON) p/ BigQuery
│   │           └── config/                  # Security JWT/RBAC, Virtual Threads, Wiring Hexagonal
│   │
│   └── intelligence-service/                # IA & MINERADOR: Python 3.12 + FastAPI
│       └── app/
│           ├── domain/                      # PURO PYTHON (Avaliador CRAG e Normalizador VTEX — 100% testado)
│           ├── modules/
│           │   ├── triage_ai/               # Gemini Vision + Busca pgvector + Polidor de Mensagem Amigável
│           │   └── catalog_miner/           # Scraper VTEX (loja.mottu.com.br) + seed_catalog_snapshot.json
│           └── infrastructure/              # Rotas /internal/v1/*, Worker Redis, Clientes S3 e Postgres
```

---

## 4. Valores Oficiais Padronizados (`Enums`) e Máquina de Estados

Todos os 3 sistemas (`Angular`, `Java` e `Python`) e o banco de dados usam **exatamente estes códigos**:

* **`UserRole`:** `RIDER` | `MECHANIC` | `MANAGER`
* **`AccountStatus`:** `PENDING_APPROVAL` | `ACTIVE` | `REJECTED`
* **`MotorcycleModel`:** `MOTTU_SPORT_110I` | `HONDA_POP_110I` | `TVS_APACHE_160` | `TVS_RAIDER_125`
* **`MotorcycleStatus`:** `AVAILABLE` | `RENTED` | `IN_MAINTENANCE` | `UNAVAILABLE`
* **`ReportedOriginContext`:** `RECEIVED_LIKE_THIS` | `NORMAL_USE_WEAR` | `ACCIDENT_OR_DROP`
* **`SafetyRiskLevel`:** `SAFE_TO_RIDE` | `CRITICAL_STOPPED`
* **`DamageVerdict`:** `PRE_EXISTENTE` | `DESGASTE_NATURAL` | `MAU_USO`
* **`QueuePriorityReason`:**
  1. `CRITICAL_SAFETY` *(peso `1` — mais urgente)*
  2. `RIDER_CONTESTATION` *(peso `2`)*
  3. `LOW_AI_CONFIDENCE` *(peso `3`)*
  4. `FRAUD_SUSPICION` *(peso `4`)*
* **`InspectionStatus`:** `QUEUED_AI` | `INVALID_IMAGE` | `AWAITING_AI_RECOVERY` | `AUTO_RESOLVED` | `HUMAN_TRIAGE` | `HUMAN_RESOLVED` | `SCHEDULED` | `CLOSED`
* **`AppointmentShift`:** `MORNING` | `AFTERNOON`
* **`AccidentReportStatus` (v3.0):** `QUEUED_AI` | `AWAITING_AI_RECOVERY` | `ESTIMATED_PENDING_WORKSHOP` | `WORKSHOP_CONFIRMED`
* **`VehicleDrivability` (v3.0):** `DRIVABLE` | `NEEDS_TOWING`

### Máquina de Estados Exata da Vistoria (Zero Dúvida!)
* **Quais status contam como "Chamado em Aberto" (limite máximo de `3` por contrato)?**
  * Apenas os status não resolvidos: **`QUEUED_AI`**, **`AWAITING_AI_RECOVERY`** e **`HUMAN_TRIAGE`**.
* **Quais chamados aparecem na Fila do Mecânico (`T05`)?**
  * Exclusivamente chamados com **`status = 'HUMAN_TRIAGE'`**, ordenados por `priority_weight ASC, created_at ASC`.
* **Transições Válidas:**
  1. **Ao criar vistoria (`POST /rider/inspections`):**
     * Se foto repetida / alerta anti-fraude → nasce direto em **`HUMAN_TRIAGE`** (`priorityReason = FRAUD_SUSPICION`, peso `4`);
     * Caso contrário → nasce em **`QUEUED_AI`** e vai para `queue:ai_triage_jobs`.
  2. **Ao receber resposta do serviço Python (`queue:ai_triage_results`):**
     * Se `outcome == AI_UNAVAILABLE` → vai para **`AWAITING_AI_RECOVERY`** (`retry_count + 1`, agenda nova tentativa em `30s`, `60s`, `300s`). Se `safetyRisk == CRITICAL_STOPPED` e `created_at <= agora - 5 min` → escala para **`HUMAN_TRIAGE`** (`CRITICAL_SAFETY`, peso `1`).
     * Se `isValidMotorcyclePhoto == false` → vai para **`INVALID_IMAGE`** (fim, não cobra nada).
     * Se `confidence < 0.80` **OU** (`reportedContext == RECEIVED_LIKE_THIS` fora da janela `48h / 100km`) → vai para **`HUMAN_TRIAGE`** (`LOW_AI_CONFIDENCE`, peso `3`, ou `CRITICAL_SAFETY` peso `1` se `safetyRisk == CRITICAL_STOPPED`).
     * Se `confidence >= 0.80` e elegível → vai para **`AUTO_RESOLVED`** (grava evento na `analytics_outbox`).
  3. **Se o Entregador clicar em "Discordo" (`POST /contest`):**
     * Só permitido quando `status == AUTO_RESOLVED` e `contested_once == false` → muda `status` para **`HUMAN_TRIAGE`**, marca `contested_once = true` e define `priorityReason = RIDER_CONTESTATION` (peso `2`, ou peso `1` se `CRITICAL_STOPPED`).
  4. **Quando o Mecânico finaliza o laudo (`POST /mechanic/inspections/{id}/resolve`):**
     * Muda de `HUMAN_TRIAGE` para **`HUMAN_RESOLVED`** (decisão final, `contested_once` bloqueado para sempre, grava evento na `analytics_outbox`).
  5. **Quando o Entregador agenda ida ao galpão (`POST /appointment`):**
     * Permitido em `AUTO_RESOLVED` ou `HUMAN_RESOLVED` → muda para **`SCHEDULED`**.

### Máquina de Estados do Auto-Relato de Acidente (v3.0)
* **Transições Válidas de Acidente:**
  1. **Ao criar relato de acidente (`POST /rider/accidents`):**
     * Nasce em **`QUEUED_AI`** e vai para `queue:ai_accident_jobs`.
     * Se `drivability == 'NEEDS_TOWING'`, o status da moto é automaticamente atualizado para `IN_MAINTENANCE`.
  2. **Ao receber processamento do vídeo (`queue:ai_accident_results`):**
     * Se `outcome == 'AI_UNAVAILABLE'` → muda para **`AWAITING_AI_RECOVERY`** (backoff exponencial).
     * Se `outcome == 'SUCCESS'` → muda para **`ESTIMATED_PENDING_WORKSHOP`**. O entregador recebe o orçamento detalhado na hora via SSE e o dossiê fica disponível para o Mecânico.
  3. **Conferência na Oficina (`POST /mechanic/accidents/{id}/confirm`):**
     * Mecânico revisa o vídeo com timestamps e peças no galpão, podendo adicionar/remover itens e emite a confirmação → status transiciona para **`WORKSHOP_CONFIRMED`** (grava evento na `analytics_outbox`).

---

## 5. Esquema Exato do Banco de Dados (`Flyway` + `Seed` + `BigQuery`)

### 5.1. Tabelas PostgreSQL (`V1__initial_schema.sql`)

```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "vector";

-- 1. Galpões / Filiais Operacionais
CREATE TABLE yards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    city VARCHAR(80) NOT NULL,
    address VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 2. Usuários (Entregador, Mecânico, Gestor)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(120) NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,               -- UserRole
    status VARCHAR(25) NOT NULL,             -- AccountStatus
    rejection_reason VARCHAR(255),
    approved_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. Motocicletas
CREATE TABLE motorcycles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plate VARCHAR(10) NOT NULL UNIQUE,
    model VARCHAR(30) NOT NULL,              -- MotorcycleModel
    yard_id UUID NOT NULL REFERENCES yards(id),
    status VARCHAR(25) NOT NULL,             -- MotorcycleStatus
    current_odometer_km INTEGER NOT NULL CHECK (current_odometer_km >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. Contratos de Locação
CREATE TABLE rental_contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rider_id UUID NOT NULL REFERENCES users(id),
    motorcycle_id UUID NOT NULL REFERENCES motorcycles(id),
    picked_up_at TIMESTAMPTZ NOT NULL,
    initial_odometer_km INTEGER NOT NULL CHECK (initial_odometer_km >= 0),
    ended_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 5. Configurações Globais Editáveis pelo Gestor
CREATE TABLE system_settings (
    setting_key VARCHAR(60) PRIMARY KEY,
    setting_value VARCHAR(100) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. Catálogo Oficial de Peças (Sincronizado pelo serviço Python + Editável pelo Gestor)
CREATE TABLE catalog_parts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vtex_sku VARCHAR(60) NOT NULL,
    motorcycle_model VARCHAR(30) NOT NULL,   -- MotorcycleModel
    part_name VARCHAR(180) NOT NULL,
    category VARCHAR(80) NOT NULL,
    unit_price_brl NUMERIC(10, 2) NOT NULL CHECK (unit_price_brl >= 0),
    is_natural_wear BOOLEAN NOT NULL DEFAULT FALSE,
    price_overridden BOOLEAN NOT NULL DEFAULT FALSE, -- Se TRUE, sync da loja não sobrescreve o preço manual!
    image_url TEXT,
    embedding vector(768),                   -- Vetor 768d p/ busca semântica CRAG
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (vtex_sku, motorcycle_model)
);
CREATE INDEX idx_catalog_parts_model ON catalog_parts(motorcycle_model);
CREATE INDEX idx_catalog_parts_embedding ON catalog_parts USING hnsw (embedding vector_cosine_ops);

-- 7. Chamados de Vistoria Inteligente
CREATE TABLE inspections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(80) NOT NULL UNIQUE,
    contract_id UUID NOT NULL REFERENCES rental_contracts(id),
    rider_id UUID NOT NULL REFERENCES users(id),
    motorcycle_id UUID NOT NULL REFERENCES motorcycles(id),
    photo_object_key VARCHAR(255) NOT NULL,
    photo_sha256 CHAR(64) NOT NULL,
    photo_perceptual_hash VARCHAR(64),
    fraud_suspected BOOLEAN NOT NULL DEFAULT FALSE,
    fraud_reason VARCHAR(200),
    reported_context VARCHAR(30) NOT NULL,   -- ReportedOriginContext
    safety_risk VARCHAR(25) NOT NULL,        -- SafetyRiskLevel
    reported_odometer_km INTEGER NOT NULL,
    rider_notes VARCHAR(300),
    status VARCHAR(30) NOT NULL,             -- InspectionStatus
    priority_weight SMALLINT,                -- 1 a 4
    priority_reason VARCHAR(35),             -- QueuePriorityReason
    ai_confidence NUMERIC(4, 3),
    ai_verdict VARCHAR(25),                  -- DamageVerdict sugerido pela IA
    ai_technical_summary TEXT,
    final_verdict VARCHAR(25),               -- DamageVerdict vigente
    labor_fee_brl NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    total_estimated_brl NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    customer_feedback_text TEXT,
    contested_once BOOLEAN NOT NULL DEFAULT FALSE,
    contestation_reason VARCHAR(500),
    reviewed_by_mechanic_id UUID REFERENCES users(id),
    mechanic_notes VARCHAR(500),
    retry_count SMALLINT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);
CREATE INDEX idx_inspections_triage_queue ON inspections(status, priority_weight, created_at);
CREATE INDEX idx_inspections_photo_sha256 ON inspections(photo_sha256);

-- 8. Peças Identificadas em cada Vistoria
CREATE TABLE inspection_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inspection_id UUID NOT NULL REFERENCES inspections(id) ON DELETE CASCADE,
    catalog_part_id UUID REFERENCES catalog_parts(id),
    part_name_snapshot VARCHAR(180) NOT NULL,
    unit_price_snapshot_brl NUMERIC(10, 2) NOT NULL,
    added_by VARCHAR(15) NOT NULL            -- 'AI' ou 'MECHANIC'
);

-- 9. Agendamento de Ida ao Galpão
CREATE TABLE yard_appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inspection_id UUID NOT NULL UNIQUE REFERENCES inspections(id),
    yard_id UUID NOT NULL REFERENCES yards(id),
    scheduled_date DATE NOT NULL,
    shift VARCHAR(15) NOT NULL,              -- AppointmentShift
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 10. Trilha Imutável de Auditoria (Somente INSERT)
CREATE TABLE inspection_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inspection_id UUID NOT NULL REFERENCES inspections(id),
    actor_id UUID,
    actor_role VARCHAR(20) NOT NULL,         -- 'SYSTEM_AI', 'RIDER', 'MECHANIC', 'MANAGER'
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    details_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 11. Caixa de Saída para o Google BigQuery (Envio em Lote NDJSON)
CREATE TABLE analytics_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inspection_id UUID REFERENCES inspections(id),
    accident_report_id UUID,
    payload_json JSONB NOT NULL,
    exported BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    exported_at TIMESTAMPTZ
);

-- 12. Relatórios de Acidente com Vídeo (V3__accident_reports_schema.sql)
CREATE TABLE accident_reports (
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
    drivability VARCHAR(20) NOT NULL,            -- VehicleDrivability (DRIVABLE / NEEDS_TOWING)
    reported_odometer_km INTEGER NOT NULL,
    rider_notes VARCHAR(500),
    rider_to_mechanic_notes VARCHAR(500),
    status VARCHAR(35) NOT NULL,                 -- AccidentReportStatus
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
CREATE INDEX idx_accident_reports_status ON accident_reports(status, created_at);

-- 13. Fotos Opcionais de Close do Acidente
CREATE TABLE accident_report_photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    accident_report_id UUID NOT NULL REFERENCES accident_reports(id) ON DELETE CASCADE,
    photo_object_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 14. Peças Identificadas e Deduplicadas no Acidente
CREATE TABLE accident_report_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    accident_report_id UUID NOT NULL REFERENCES accident_reports(id) ON DELETE CASCADE,
    catalog_part_id UUID REFERENCES catalog_parts(id),
    part_name_snapshot VARCHAR(180) NOT NULL,
    unit_price_snapshot_brl NUMERIC(10, 2) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    video_timestamp_seconds INTEGER,
    confirmed_by_mechanic BOOLEAN NOT NULL DEFAULT TRUE,
    added_by VARCHAR(15) NOT NULL                -- 'AI' ou 'MECHANIC'
);
```

### 5.2. Dados Iniciais Obrigatórios (`V2__seed_data.sql`)
Para que todos os agentes e testes E2E usem as **mesmas credenciais** (senha padrão de seed com hash BCrypt equivalente a **`Mottu@2026!`**):
* **Usuários `ACTIVE` pré-criados:**
  * **Gestor Oficial:** `gestor@mottu-ops.local` / senha `Mottu@2026!` (`role = MANAGER`)
  * **Mecânico Demo/E2E:** `mecanico@mottu-ops.local` / senha `Mottu@2026!` (`role = MECHANIC`)
  * **Entregador Demo/E2E:** `entregador@mottu-ops.local` / senha `Mottu@2026!` (`role = RIDER`, já com contrato ativo na moto placa `MOT1A23`, modelo `MOTTU_SPORT_110I`, retirada há 12 horas com `1000 km` iniciais)
* **Galpões Ativos:** `Galpão Mottu Butantã (São Paulo)` e `Galpão Mottu Guarulhos (Guarulhos)`.
* **Motos Extras `AVAILABLE`:** Placas `MOT2B34` (`MOTTU_SPORT_110I`), `POP3C45` (`HONDA_POP_110I`), `TVS4D56` (`TVS_APACHE_160`).
* **Configurações `system_settings`:**
  * `('STANDARD_LABOR_FEE_BRL', '35.00')`
  * `('AI_CONFIDENCE_THRESHOLD', '0.80')`
  * `('PRE_EXISTING_MAX_HOURS', '48')`
  * `('PRE_EXISTING_MAX_KM', '100')`
  * `('LAST_CATALOG_SYNC_AT', 'NEVER')`

### 5.3. Tabela Analítica no Google BigQuery (`mottu_ops_bi.fact_inspections`)
Enviada em lote (`Batch Load NDJSON`) a cada 60s pelo Spring Boot quando `BIGQUERY_ENABLED=true` (se `false`, apenas marca `exported = true` em modo local testável):
* Colunas: `inspection_id (STRING)`, `created_at (TIMESTAMP)`, `resolved_at (TIMESTAMP)`, `resolution_seconds (INT64)`, `yard_name (STRING)`, `motorcycle_model (STRING)`, `plate (STRING)`, `reported_context (STRING)`, `safety_risk (STRING)`, `handled_autonomously (BOOL)`, `ai_confidence (FLOAT64)`, `ai_verdict (STRING)`, `final_verdict (STRING)`, `ai_matched_mechanic (BOOL)`, `contested_by_rider (BOOL)`, `fraud_suspected (BOOL)`, `primary_part_name (STRING)`, `total_estimated_brl (FLOAT64)`.

---

## 6. Contratos Completos de API REST (Todos os JSONs Travados!)

* **Cabeçalhos Padrão:** `Authorization: Bearer <accessToken>` (exceto rotas públicas) e `X-Correlation-Id: <uuid>`.
* **Formato Padrão de Erro (`RFC 7807` para `400`, `401`, `403`, `404`, `422`, `429`, `500`):**
  ```json
  {
    "title": "Regra de Negócio Violada",
    "status": 422,
    "detail": "Este chamado já foi contestado anteriormente e possui parecer final.",
    "correlationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
  }
  ```
* **Formato Padrão de Listas Paginadas (`PageResponse<T>`):**
  ```json
  {
    "items": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0
  }
  ```

### 6.1. Rotas Públicas e Compartilhadas
* **`GET /api/v1/auth/public-config`** *(Público — informa à tela `T01` se deve exibir os botões de Login Rápido Demo)*
  * **Saída (`200 OK`):** `{ "publicDemoMode": false }`
* **`POST /api/v1/auth/register`** *(Público — apenas `RIDER` ou `MECHANIC`; bloqueia `MANAGER` com `400`)*
  * **Entrada:** `{ "fullName": "João Silva", "cpf": "12345678901", "phone": "11999990000", "email": "joao@email.com", "password": "Senha@123", "role": "RIDER" }`
  * **Saída (`201 Created`):** `{ "userId": "uuid", "status": "PENDING_APPROVAL", "message": "Cadastro recebido! Aguarde a liberação do Gestor." }`
* **`POST /api/v1/auth/login`** *(Público)*
  * **Entrada:** `{ "email": "entregador@mottu-ops.local", "password": "Mottu@2026!" }`
  * **Saída (`200 OK` se `ACTIVE`):** `{ "accessToken": "eyJ...", "userId": "uuid", "role": "RIDER", "fullName": "Carlos Entregador" }`
  * **Saída (`403 Forbidden` se `PENDING_APPROVAL` ou `REJECTED`):** Erro RFC 7807 informando o motivo.
* **`GET /api/v1/yards`** *(Autenticado — qualquer perfil `ACTIVE`)*
  * **Saída (`200 OK`):** `[ { "id": "uuid", "name": "Galpão Mottu Butantã", "city": "São Paulo", "address": "Av. Corifeu, 1000" } ]`

### 6.2. Rotas do Entregador (`ROLE_RIDER`)
* **`GET /api/v1/rider/garage`** *(Alimenta a tela `T02`)*
  * **Saída (`200 OK`):**
    ```json
    {
      "activeContract": {
        "contractId": "uuid",
        "pickedUpAt": "2026-11-06T08:00:00Z",
        "initialOdometerKm": 1000,
        "motorcycle": {
          "id": "uuid",
          "plate": "MOT1A23",
          "model": "MOTTU_SPORT_110I",
          "status": "RENTED",
          "currentOdometerKm": 1035,
          "yardName": "Galpão Mottu Butantã"
        }
      },
      "openInspectionsCount": 1,
      "recentInspections": [
        {
          "id": "uuid",
          "status": "AUTO_RESOLVED",
          "finalVerdict": "PRE_EXISTENTE",
          "totalEstimatedBrl": 0.00,
          "createdAt": "2026-11-06T18:30:00Z"
        }
      ]
    }
    ```
* **`POST /api/v1/rider/inspections/upload-url`** *(Valida tipo `image/jpeg|png|webp` e `fileSizeBytes <= 10485760`)*
  * **Entrada:** `{ "contentType": "image/webp", "fileSizeBytes": 385000 }`
  * **Saída (`200 OK`):** `{ "uploadUrl": "http://localhost:9000/mottu-inspections/...", "photoObjectKey": "inspections/2026/11/uuid.webp", "expiresInSeconds": 300 }`
* **`POST /api/v1/rider/inspections`** *(Obrigatório header `Idempotency-Key: <uuid>`)*
  * **Entrada:**
    ```json
    {
      "photoObjectKey": "inspections/2026/11/uuid.webp",
      "reportedContext": "RECEIVED_LIKE_THIS",
      "safetyRisk": "SAFE_TO_RIDE",
      "reportedOdometerKm": 1035,
      "riderNotes": "Peguei a moto hoje cedo e percebi o manete ralado."
    }
    ```
  * **Saída (`202 Accepted`):** `{ "inspectionId": "uuid", "status": "QUEUED_AI" }` *(ou `"HUMAN_TRIAGE"` se cair no anti-fraude imediato)*
* **`GET /api/v1/rider/inspections/{id}`** *(Alimenta a tela `T04`; valida que `rider_id == usuário logado`, senão `404`)*
  * **Saída (`200 OK`):**
    ```json
    {
      "id": "uuid",
      "status": "AUTO_RESOLVED",
      "photoDownloadUrl": "http://localhost:9000/mottu-inspections/...",
      "reportedContext": "ACCIDENT_OR_DROP",
      "safetyRisk": "SAFE_TO_RIDE",
      "reportedOdometerKm": 1035,
      "riderNotes": "Deixei tombar estacionando.",
      "finalVerdict": "MAU_USO",
      "aiConfidence": 0.92,
      "laborFeeBrl": 35.00,
      "totalEstimatedBrl": 60.00,
      "customerFeedbackText": "Identificamos avaria por queda no manete de freio...",
      "canContest": true,
      "contestedOnce": false,
      "items": [
        { "catalogPartId": "uuid", "partName": "Manete de Freio Direito", "unitPriceBrl": 25.00, "addedBy": "AI" }
      ],
      "appointment": null,
      "createdAt": "2026-11-06T19:00:00Z",
      "resolvedAt": "2026-11-06T19:00:04Z"
    }
    ```
* **`GET /api/v1/rider/inspections/{id}/stream`** (`Accept: text/event-stream` via `fetch` autenticado!)
  * **Formato do Evento SSE emitido a cada mudança:**
    ```text
    event: status-change
    data: {"inspectionId":"uuid","status":"AUTO_RESOLVED","stepLabel":"Resultado pronto!"}
    ```
* **`POST /api/v1/rider/inspections/{id}/contest`**
  * **Entrada:** `{ "reason": "Esse ralado já constava quando saí do portão da filial hoje cedo." }` *(mínimo 10 caracteres)*
  * **Saída (`200 OK`):** `{ "inspectionId": "uuid", "status": "HUMAN_TRIAGE", "priorityReason": "RIDER_CONTESTATION" }`
* **`POST /api/v1/rider/inspections/{id}/appointment`**
  * **Entrada:** `{ "yardId": "uuid", "scheduledDate": "2026-11-10", "shift": "MORNING" }`
  * **Saída (`201 Created`):** `{ "appointmentId": "uuid", "inspectionStatus": "SCHEDULED", "yardName": "Galpão Mottu Butantã", "scheduledDate": "2026-11-10", "shift": "MORNING" }`

#### 6.2.1. Rotas de Acidente do Entregador (v3.0)
* **`POST /api/v1/rider/accidents/upload-video-url`** *(Valida `video/mp4` ou `video/webm` e `fileSizeBytes <= 31457280` - 30MB)*
  * **Entrada:** `{ "contentType": "video/mp4", "fileSizeBytes": 15400000 }`
  * **Saída (`200 OK`):** `{ "uploadUrl": "http://localhost:9000/mottu-inspections/...", "videoObjectKey": "accidents/2026/11/uuid.mp4", "expiresInSeconds": 600 }`
* **`POST /api/v1/rider/accidents/upload-photo-url`** *(Até 3 fotos de close opcionais)*
  * **Entrada:** `{ "contentType": "image/webp", "fileSizeBytes": 450000 }`
  * **Saída (`200 OK`):** `{ "uploadUrl": "http://localhost:9000/mottu-inspections/...", "photoObjectKey": "accidents/2026/11/uuid.webp", "expiresInSeconds": 300 }`
* **`POST /api/v1/rider/accidents`** *(Obrigatório header `Idempotency-Key: <uuid>`)*
  * **Entrada:**
    ```json
    {
      "videoObjectKey": "accidents/2026/11/uuid.mp4",
      "videoDurationSeconds": 24,
      "videoSizeBytes": 15400000,
      "photoObjectKeys": ["accidents/2026/11/photo1.webp"],
      "hasInjuries": false,
      "involvesThirdParty": false,
      "policeReportNumber": null,
      "drivability": "NEEDS_TOWING",
      "reportedOdometerKm": 1045,
      "riderNotes": "Queda em curva molhada, quebrou manete, retrovisor e estribo.",
      "riderToMechanicNotes": "A carenagem só ralou um pouco."
    }
    ```
  * **Saída (`202 Accepted`):** `{ "accidentReportId": "uuid", "status": "QUEUED_AI", "motorcycleStatus": "IN_MAINTENANCE" }`
* **`GET /api/v1/rider/accidents/{id}`** *(Alimenta a tela `T12`)*
  * **Saída (`200 OK`):**
    ```json
    {
      "id": "uuid",
      "status": "ESTIMATED_PENDING_WORKSHOP",
      "videoDownloadUrl": "http://localhost:9000/mottu-inspections/...",
      "drivability": "NEEDS_TOWING",
      "hasInjuries": false,
      "structuralRiskAlert": false,
      "aiConfidence": 0.94,
      "partsSubtotalBrl": 147.50,
      "laborFeeBrl": 90.00,
      "towingFeeBrl": 80.00,
      "totalEstimatedBrl": 317.50,
      "items": [
        { "catalogPartId": "uuid-1", "partName": "Manete de Freio Direito", "unitPriceBrl": 25.00, "quantity": 1, "videoTimestampSeconds": 6 },
        { "catalogPartId": "uuid-2", "partName": "Retrovisor Direito", "unitPriceBrl": 42.50, "quantity": 1, "videoTimestampSeconds": 14 },
        { "catalogPartId": "uuid-3", "partName": "Pedal de Apoio Dianteiro Direito", "unitPriceBrl": 80.00, "quantity": 1, "videoTimestampSeconds": 21 }
      ],
      "riderToMechanicNotes": "A carenagem só ralou um pouco.",
      "createdAt": "2026-11-07T12:00:00Z"
    }
    ```
* **`GET /api/v1/rider/accidents/{id}/stream`** (`text/event-stream` SSE)
  * Evento: `data: {"accidentReportId":"uuid","status":"ESTIMATED_PENDING_WORKSHOP","stepLabel":"Orçamento de peças novas pronto!"}`

### 6.3. Rotas do Mecânico (`ROLE_MECHANIC` ou `ROLE_MANAGER`)
* **`GET /api/v1/mechanic/queue?reason=&plate=&page=0&size=20`** *(Retorna `PageResponse` de chamados em `HUMAN_TRIAGE`)*
  * **Item da Fila (`items[]`):**
    ```json
    {
      "inspectionId": "uuid",
      "plate": "MOT1A23",
      "motorcycleModel": "MOTTU_SPORT_110I",
      "riderName": "Carlos Entregador",
      "priorityWeight": 2,
      "priorityReason": "RIDER_CONTESTATION",
      "safetyRisk": "SAFE_TO_RIDE",
      "fraudSuspected": false,
      "aiConfidence": 0.88,
      "aiSuggestedVerdict": "MAU_USO",
      "createdAt": "2026-11-06T19:00:00Z"
    }
    ```
* **`GET /api/v1/mechanic/inspections/{id}`** *(Alimenta a Mesa de Decisão `T06` com dados completos de telemetria e IA)*
  * **Saída (`200 OK`):** Inclui todos os campos de detalhe da vistoria + `hoursSincePickup`, `kmDrivenSincePickup`, `fraudSuspected`, `fraudReason`, `aiTechnicalSummary`, `aiSuggestedVerdict`, `contestationReason`.
* **`POST /api/v1/mechanic/inspections/{id}/resolve`**
  * **Entrada:**
    ```json
    {
      "finalVerdict": "MAU_USO",
      "catalogPartIds": ["uuid-peca-1"],
      "mechanicNotes": "Quebra típica de queda lateral recente no manete direito."
    }
    ```
  * **Saída (`200 OK`):** `{ "inspectionId": "uuid", "status": "HUMAN_RESOLVED", "finalVerdict": "MAU_USO", "totalEstimatedBrl": 60.00, "customerFeedbackText": "Olá! Nosso mecânico revisou sua foto com atenção..." }`
* **`GET /api/v1/mechanic/accidents?page=0&size=20`** *(Alimenta a Fila de Dossiês de Sinistro na Oficina)*
  * **Saída (`200 OK`):** PageResponse contendo itens com `id`, `plate`, `model`, `riderName`, `drivability`, `hasInjuries`, `totalEstimatedBrl`, `itemCount`, `status`, `createdAt`.
* **`GET /api/v1/mechanic/accidents/{id}`** *(Alimenta a tela `T13` - Dossiê de Acidente)*
  * **Saída (`200 OK`):** Objeto completo com vídeo URL, fotos URLs, checklist de emergência, lista de peças identificadas pela IA com `videoTimestampSeconds`, observações do entregador.
* **`POST /api/v1/mechanic/accidents/{id}/confirm`**
  * **Entrada:**
    ```json
    {
      "confirmedPartIds": ["uuid-1", "uuid-2"],
      "additionalCatalogPartIds": ["uuid-4"],
      "mechanicNotes": "Confirmadas peças danificadas no impacto lateral e substituído também o suporte de placa."
    }
    ```
  * **Saída (`200 OK`):** `{ "accidentReportId": "uuid", "status": "WORKSHOP_CONFIRMED", "totalEstimatedBrl": 340.00, "confirmedAt": "2026-11-07T14:00:00Z" }`
* **`GET /api/v1/fleet/motorcycles?status=&plate=&page=0&size=20`** → Retorna `PageResponse` com `{ "id", "plate", "model", "yardId", "yardName", "status", "currentOdometerKm", "activeRiderName" }`.
* **`POST /api/v1/fleet/motorcycles`** → Entrada: `{ "plate": "MOT9Z99", "model": "MOTTU_SPORT_110I", "yardId": "uuid", "currentOdometerKm": 0 }` → Saída (`201`): objeto da moto criada (`AVAILABLE`).
* **`PATCH /api/v1/fleet/motorcycles/{id}/status`** → Entrada: `{ "status": "IN_MAINTENANCE" }` → Saída (`200`): moto atualizada.

### 6.4. Rotas do Gestor (`ROLE_MANAGER`)
* **`GET /api/v1/manager/users/pending`** → Saída (`200 OK`): `[ { "id": "uuid", "fullName": "João", "cpf": "12345678901", "phone": "11999990000", "email": "joao@email.com", "role": "RIDER", "createdAt": "..." } ]`
* **`POST /api/v1/manager/users/{id}/approve`** → Entrada: `{ "motorcycleId": "uuid" }` *(obrigatório se `role == RIDER`, opcional/`null` se `MECHANIC`)* → Saída (`200 OK`): `{ "userId": "uuid", "status": "ACTIVE", "contractId": "uuid-ou-null" }`
* **`POST /api/v1/manager/users/{id}/reject`** → Entrada: `{ "reason": "CPF divergente na validação cadastral." }` → Saída (`200 OK`): `{ "userId": "uuid", "status": "REJECTED" }`
* **`POST /api/v1/manager/catalog/sync-mottu-store`** → Chama o serviço Python e retorna (`200 OK`): `{ "syncedCount": 48, "usedFallback": false, "syncedAt": "2026-11-06T20:00:00Z", "message": "Catálogo sincronizado com loja.mottu.com.br!" }`
* **`GET /api/v1/manager/catalog/parts?model=&search=&page=0&size=20`** → Retorna `PageResponse` de `{ "id", "vtexSku", "motorcycleModel", "partName", "category", "unitPriceBrl", "isNaturalWear", "priceOverridden", "imageUrl", "updatedAt" }`.
* **`PATCH /api/v1/manager/catalog/parts/{id}`** → Entrada: `{ "unitPriceBrl": 29.90, "isNaturalWear": false }` *(marca automaticamente `price_overridden = true`)* → Saída (`200 OK`): peça atualizada.
* **`GET /api/v1/manager/settings`** & **`PATCH /api/v1/manager/settings`** → Entrada/Saída: `{ "standardLaborFeeBrl": 35.00 }`.
* **`GET /api/v1/manager/analytics/summary`** *(Alimenta o topo e o iframe de `T10`)*
  * **Saída (`200 OK`):**
    ```json
    {
      "activeMotorcyclesCount": 124,
      "autoResolvedPercentage": 78.5,
      "currentHumanQueueSize": 4,
      "estimatedMechanicHoursSaved": 36.2,
      "lookerStudioEmbedUrl": "https://lookerstudio.google.com/embed/reporting/..."
    }
    ```

### 6.5. Contratos Internos Java ↔ Python (Filas Redis + HTTP Interno com `X-Internal-Token`)

> [!NOTE]
> **Quem grava em `catalog_parts`?** O serviço Python (`intelligence-service`) faz o `UPSERT` direto na tabela PostgreSQL `catalog_parts` já com o vetor `embedding vector(768)` calculado — respeitando `price_overridden = true` para nunca sobrescrever um preço editado manualmente pelo Gestor!

1. **Fila Redis `queue:ai_triage_jobs` (`LPUSH` Java → `BRPOP` Python):**
   ```json
   {
     "inspectionId": "uuid",
     "correlationId": "uuid",
     "photoObjectKey": "inspections/2026/11/uuid.webp",
     "motorcycleModel": "MOTTU_SPORT_110I",
     "reportedContext": "RECEIVED_LIKE_THIS",
     "hoursSincePickup": 18.5,
     "kmDrivenSincePickup": 35,
     "riderNotes": "Peguei a moto hoje cedo..."
   }
   ```
2. **Fila Redis `queue:ai_triage_results` (`LPUSH` Python → `BRPOP` Java):**
   ```json
   {
     "inspectionId": "uuid",
     "correlationId": "uuid",
     "outcome": "SUCCESS",
     "isValidMotorcyclePhoto": true,
     "confidence": 0.91,
     "suggestedVerdict": "PRE_EXISTENTE",
     "matchedParts": [
       { "catalogPartId": "uuid", "partName": "Manete de Freio", "unitPriceBrl": 24.90, "isNaturalWear": false }
     ],
     "technicalSummary": "Ralado superficial na ponta esférica do manete direito.",
     "customerFriendlyMessage": "Olá! Identificamos o ralado no manete de freio. Como você retirou a moto há menos de 48h, o reparo é totalmente gratuito (R$ 0,00)..."
   }
   ```
   *(Em caso de queda temporária do Gemini, retorna `{ "inspectionId": "uuid", "correlationId": "uuid", "outcome": "AI_UNAVAILABLE" }` para acionar `AWAITING_AI_RECOVERY` no Java).*
3. **HTTP Interno `POST http://intelligence-service:8000/internal/v1/catalog/sync`** *(Header `X-Internal-Token`)*
   * **Entrada:** `{ "forceRefresh": true }`
   * **Saída (`200 OK`):** `{ "syncedCount": 48, "usedFallback": false, "syncedAt": "2026-11-06T20:00:00Z" }`
4. **HTTP Interno `POST http://intelligence-service:8000/internal/v1/feedback/polish`** *(Transforma a nota curta do mecânico em resposta empática; se o Gemini oscilar, usa template educado local sem nunca falhar o laudo do mecânico!)*
   * **Entrada:** `{ "finalVerdict": "MAU_USO", "partNames": ["Manete de Freio"], "totalEstimatedBrl": 60.00, "mechanicNotes": "Quebra por queda lateral recente." }`
   * **Saída (`200 OK`):** `{ "customerFriendlyMessage": "Olá! Nosso mecânico revisou sua foto com atenção e confirmou quebra por impacto lateral no Manete de Freio. O valor estimado para reposição no galpão é de R$ 60,00." }`

5. **Fila Redis `queue:ai_accident_jobs` (v3.0 - `LPUSH` Java → `BRPOP` Python):**
   ```json
   {
     "accidentReportId": "uuid",
     "correlationId": "uuid",
     "videoObjectKey": "accidents/2026/11/uuid.mp4",
     "photoObjectKeys": ["accidents/2026/11/photo1.webp"],
     "motorcycleModel": "MOTTU_SPORT_110I",
     "drivability": "NEEDS_TOWING",
     "riderNotes": "Queda em curva molhada..."
   }
   ```
6. **Fila Redis `queue:ai_accident_results` (v3.0 - `LPUSH` Python → `BRPOP` Java):**
   ```json
   {
     "accidentReportId": "uuid",
     "correlationId": "uuid",
     "outcome": "SUCCESS",
     "confidence": 0.94,
     "structuralRiskAlert": false,
     "aiTechnicalSummary": "Avarias por queda lateral direita identificadas no vídeo.",
     "deduplicatedParts": [
       {
         "catalogPartId": "uuid-1",
         "partName": "Manete de Freio Direito",
         "unitPriceBrl": 25.00,
         "quantity": 1,
         "videoTimestampSeconds": 6
       },
       {
         "catalogPartId": "uuid-2",
         "partName": "Retrovisor Direito",
         "unitPriceBrl": 42.50,
         "quantity": 1,
         "videoTimestampSeconds": 14
       }
     ]
   }
   ```
