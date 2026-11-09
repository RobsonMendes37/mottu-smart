# PLANO_OPERACAO_REAL_E_IA_BIGQUERY.md — Especificação Técnica e Plano de Execução Autônoma (Fase 7)

> **INSTRUÇÃO PARA A IA EXECUTORA:** Este documento contém a especificação completa (SQL, Domínio, Contratos JSON, Segurança `SHA-256` + `BCrypt`, IA Preditiva BigQuery e Testes TDD) para executar as tarefas **`TASK-19` a `TASK-24`** listadas em `mottu-smart-ops.md`.
> Siga rigorosamente as **8 Leis Inquebráveis de `AGENTS.md`** (TDD `RED → GREEN → REFACTOR`, 100% de cobertura em `domain/` Java e Python, Zero frameworks em `domain/`, `Purple Ban` estrito e deploy contínuo).

---

## 1. Objetivo Executivo & Dores da Mottu Resolvidas

Eliminar **100% dos dados estáticos (mocks)** restantes no frontend (`apps/web-app`), transformando todos os módulos operacionais em fluxos transacionais persistidos no PostgreSQL e integrados ao Google BigQuery:

1. **Segurança e Perfil Corporativo (`SHA-256` + `BCrypt`):**
   * Implementar pré-hash `SHA-256` (`64` caracteres hexadecimais) combinado com `BCrypt`, impedindo reutilização da mesma senha (`password_sha256`), mantendo retrocompatibilidade com as credenciais seed (`Mottu@2026!`) e persistindo alterações de perfil (`full_name`, `phone`, `email`, `pix_key`, `preferred_unit`, `role_title`, `shift_label`) na tabela `users`.
2. **Ciclo de Oficina: Da Vistoria/Agendamento à Ordem de Serviço na Baia e Almoxarifado WMS (`/mechanic/operations`):**
   * Quando o entregador agenda visita ao galpão (`POST /api/v1/rider/inspections/{id}/appointment`), o backend gera automaticamente uma **Ordem de Serviço (`service_orders`)** `WAITING_BAY`.
   * Quando o mecânico inicia o reparo (`WAITING_BAY → IN_REPAIR`), a moto muda automaticamente para `IN_MAINTENANCE` na tabela `motorcycles`.
   * Quando o mecânico libera a moto (`IN_REPAIR → READY_PICKUP`), a moto volta para `RENTED` (se tiver contrato ativo) ou `AVAILABLE` e o sistema **desconta automaticamente `-1` unidade no estoque (`catalog_parts.stock_quantity`)** das peças vinculadas à vistoria.
3. **Ciclo Financeiro: Contratos, Fatura Semanal Dinâmica, Meta de Entregas e Conciliação Pix (`T12` `/manager/billing` e `/rider/rental`):**
   * A fatura semanal de cada um dos 16 contratos ativos (`rental_contracts`) soma dinamicamente os laudos de `MAU_USO` (`inspections.total_estimated_brl`), aplica o desconto de pontualidade (`R$ 15,00`) e o bônus automático de produtividade (`R$ 40,00` ao atingir `>= 120` entregas na tabela `rider_daily_deliveries`).
   * O pagamento Pix confirmado pelo Entregador (`/rider/rental`) ou conciliado pelo Gestor (`T12`) atualiza `payment_status = 'PAID'` e `paid_at = NOW()` no PostgreSQL.
4. **Painel Analítico 100% SQL Real + Consultor IA Preditivo sobre o BigQuery (`T10` `/manager/analytics`):**
   * Todos os gráficos da `T10` (snapshots por unidade, barras de 7 dias, rosca de pareceres, desempenho por modelo e top 6 peças) passam a vir de queries SQL reais no banco.
   * Novo motor **`Mottu AI Ops Advisor`** (`POST /api/v1/manager/analytics/ai-advisor` → Python FastAPI `/internal/v1/analytics/advisor`): cruza o histórico de avarias do **BigQuery (`mottu_ops_bi.fact_inspections`)** com o **saldo físico das gavetas (`catalog_parts.stock_quantity`)**, calcula a **Taxa de Crescimento de Demanda (7 dias)**, prevê **em quantos dias cada peça vai acabar no galpão (*Days to Stockout*)** e oferece botão de **Reposição de Lote em 1 Clique** direto na tela `T10`.

---

## 2. Especificação de Banco de Dados (`V8__operational_workflows_billing_and_security.sql`)

Criar o arquivo `apps/core-api/src/main/resources/db/migration/V8__operational_workflows_billing_and_security.sql` com a estrutura abaixo:

```sql
-- 1. Evolução de Usuários: Perfil Completo + Hash SHA-256 de Auditoria de Senha
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_sha256 CHAR(64),
    ADD COLUMN IF NOT EXISTS pix_key VARCHAR(150),
    ADD COLUMN IF NOT EXISTS preferred_unit VARCHAR(100) NOT NULL DEFAULT 'Unidade Butantã (SP-04)',
    ADD COLUMN IF NOT EXISTS cnh_status VARCHAR(80) NOT NULL DEFAULT 'Categoria A • Válida até 08/2029',
    ADD COLUMN IF NOT EXISTS employee_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS role_title VARCHAR(80),
    ADD COLUMN IF NOT EXISTS shift_label VARCHAR(80),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- Preencher metadados iniciais de perfil e SHA-256 da senha seed ('Mottu@2026!')
UPDATE users
SET password_sha256 = encode(digest('Mottu@2026!', 'sha256'), 'hex'),
    pix_key = email,
    employee_code = CASE
        WHEN role = 'MANAGER' THEN 'MOT-SP04-0104'
        WHEN role = 'MECHANIC' THEN 'MOT-SP04-0892'
        ELSE 'CTR-2026-' || SUBSTRING(REPLACE(id::text, '-', ''), 1, 4)
    END,
    role_title = CASE
        WHEN role = 'MANAGER' THEN 'Gestor da unidade'
        WHEN role = 'MECHANIC' THEN 'Mecânico líder — Baia 02'
        ELSE 'Entregador parceiro'
    END,
    shift_label = CASE
        WHEN role = 'MANAGER' THEN 'Comercial • 08h às 18h'
        WHEN role = 'MECHANIC' THEN 'Manhã e tarde • 07h às 16h'
        ELSE 'Plano Minha Mottu Semanal'
    END
WHERE password_sha256 IS NULL;

-- 2. Evolução do Catálogo de Peças: Controle de Almoxarifado Físico (WMS)
ALTER TABLE catalog_parts
    ADD COLUMN IF NOT EXISTS stock_quantity INTEGER NOT NULL DEFAULT 18 CHECK (stock_quantity >= 0),
    ADD COLUMN IF NOT EXISTS min_stock_quantity INTEGER NOT NULL DEFAULT 8 CHECK (min_stock_quantity >= 0),
    ADD COLUMN IF NOT EXISTS aisle_bin VARCHAR(60) NOT NULL DEFAULT 'Corredor A • Gaveta 01';

-- Distribuir corredores/gavetas e criar itens com estoque crítico (< min_stock_quantity) para alertas reais de reposição
UPDATE catalog_parts
SET aisle_bin = CASE
        WHEN motorcycle_model = 'MOTTU_SPORT_110I' THEN 'Corredor A • Gaveta ' || LPAD(((ABS(HASHTEXT(vtex_sku)) % 12) + 1)::text, 2, '0')
        WHEN motorcycle_model = 'HONDA_POP_110I' THEN 'Corredor B • Gaveta ' || LPAD(((ABS(HASHTEXT(vtex_sku)) % 12) + 1)::text, 2, '0')
        WHEN motorcycle_model = 'TVS_RAIDER_125' THEN 'Corredor C • Gaveta ' || LPAD(((ABS(HASHTEXT(vtex_sku)) % 12) + 1)::text, 2, '0')
        ELSE 'Corredor D • Gaveta ' || LPAD(((ABS(HASHTEXT(vtex_sku)) % 12) + 1)::text, 2, '0')
    END,
    stock_quantity = CASE
        WHEN vtex_sku IN ('SKU-SPORT-001', 'SKU-POP-002', 'SKU-RAI-002') THEN 4
        WHEN vtex_sku IN ('SKU-SPORT-002', 'SKU-POP-003') THEN 6
        ELSE 14 + (ABS(HASHTEXT(vtex_sku)) % 15)
    END,
    min_stock_quantity = 8;

-- 3. Tabela de Ordens de Serviço de Baia (service_orders)
CREATE TABLE IF NOT EXISTS service_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(25) NOT NULL UNIQUE,
    motorcycle_id UUID NOT NULL REFERENCES motorcycles(id),
    inspection_id UUID REFERENCES inspections(id),
    yard_id UUID NOT NULL REFERENCES yards(id),
    bay_label VARCHAR(50) NOT NULL,
    service_title VARCHAR(220) NOT NULL,
    origin_label VARCHAR(140) NOT NULL,
    sla_minutes INTEGER NOT NULL DEFAULT 30,
    status VARCHAR(25) NOT NULL, -- 'WAITING_BAY', 'IN_REPAIR', 'READY_PICKUP'
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_service_orders_status ON service_orders(status, created_at DESC);

-- 4. Evolução de Contratos de Locação (Faturamento Semanal Pix)
ALTER TABLE rental_contracts
    ADD COLUMN IF NOT EXISTS contract_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS weekly_plan_brl NUMERIC(10, 2) NOT NULL DEFAULT 196.00,
    ADD COLUMN IF NOT EXISTS discount_brl NUMERIC(10, 2) NOT NULL DEFAULT 15.00,
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PIX', -- 'PAID', 'PENDING_PIX', 'OVERDUE'
    ADD COLUMN IF NOT EXISTS due_date_label VARCHAR(80) NOT NULL DEFAULT 'Vence segunda-feira • 18:00',
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ;

-- 5. Tabela de Entregas Diárias do Entregador (rider_daily_deliveries)
CREATE TABLE IF NOT EXISTS rider_daily_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rider_id UUID NOT NULL REFERENCES users(id),
    contract_id UUID NOT NULL REFERENCES rental_contracts(id),
    day_label VARCHAR(40) NOT NULL,
    completed_count INTEGER NOT NULL CHECK (completed_count >= 0),
    earnings_brl NUMERIC(10, 2) NOT NULL CHECK (earnings_brl >= 0),
    on_time_rate VARCHAR(10) NOT NULL DEFAULT '98%',
    delivery_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_rider_deliveries_rider ON rider_daily_deliveries(rider_id, delivery_date DESC);
```
*(A migration `V8` também deve popular os metadados de `contract_code`, `weekly_plan_brl` conforme o modelo da moto, `payment_status` variados `PAID`/`PENDING_PIX`/`OVERDUE`, 5 dias de `rider_daily_deliveries` somando 118 entregas para o entregador principal `11111111-1111-1111-1111-111111111101` — faltando 2 entregas para ganhar o bônus de R$ 40,00 — e 6 ordens de serviço iniciais reais em `service_orders`).*

---

## 3. Regras de Domínio Puro (100% Cobertura TDD Obrigatória)

> **ATENÇÃO LEI 2 (`AGENTS.md`):** Nas pastas `apps/core-api/src/main/java/com/mottu/ops/domain/` e `apps/intelligence-service/app/domain/`, **É PROIBIDO** importar frameworks externos (`org.springframework.*`, `jakarta.persistence.*`, `lombok.*`, `fastapi`, `sqlalchemy`).

### 3.1 Domínio Java (`apps/core-api/src/main/java/com/mottu/ops/domain/`)
1. **`PasswordSecurityPolicy.java` (`domain/service/`):**
   * `String computeSha256Hex(String rawPassword)`: Calcula o hash `SHA-256` em formato hexadecimal minúsculo (`64` caracteres) usando apenas `java.security.MessageDigest` e `java.nio.charset.StandardCharsets`. Lança `IllegalArgumentException` se nulo/vazio.
   * `void validatePasswordChange(String currentPassword, String newPassword, String existingPasswordSha256)`:
     * Verifica se `currentPassword` não está em branco.
     * Verifica se `newPassword` tem pelo menos `8` caracteres.
     * Calcula `computeSha256Hex(newPassword)` e, se for igual a `existingPasswordSha256` (ou igual a `computeSha256Hex(currentPassword)`), lança `DomainRuleException("PASSWORD_REUSE_FORBIDDEN", "A nova senha deve ser diferente da senha atual.")`.
2. **`ServiceOrderPolicy.java` (`domain/service/`) e `ServiceOrderStatus.java` (`domain/enums/`):**
   * Enum `ServiceOrderStatus`: `WAITING_BAY`, `IN_REPAIR`, `READY_PICKUP`.
   * `ServiceOrderStatus nextStatus(ServiceOrderStatus current)`:
     * `WAITING_BAY -> IN_REPAIR`
     * `IN_REPAIR -> READY_PICKUP`
     * `READY_PICKUP -> READY_PICKUP` (idempotente).
   * `MotorcycleStatus resolveMotorcycleStatusOnOrderTransition(ServiceOrderStatus newOrderStatus, boolean hasActiveRentalContract)`:
     * Se `newOrderStatus == IN_REPAIR` → retorna `MotorcycleStatus.IN_MAINTENANCE`.
     * Se `newOrderStatus == READY_PICKUP` → retorna `hasActiveRentalContract ? MotorcycleStatus.RENTED : MotorcycleStatus.AVAILABLE`.
     * Se `newOrderStatus == WAITING_BAY` → retorna `hasActiveRentalContract ? MotorcycleStatus.RENTED : MotorcycleStatus.AVAILABLE`.
   * `int calculateUpdatedStock(int currentStock, int delta)`:
     * Retorna `Math.max(0, currentStock + delta)`.
3. **`WeeklyBillingCalculator.java` (`domain/service/`):**
   * `BigDecimal resolveBaseWeeklyPlanBrl(MotorcycleModel model)`:
     * `MOTTU_SPORT_110I` → `196.00`
     * `HONDA_POP_110I` → `210.00`
     * `TVS_RAIDER_125` → `224.00`
     * `TVS_APACHE_160` → `238.00`
   * `BigDecimal calculateDeliveryGoalBonusBrl(int weeklyDeliveriesCount)`:
     * Se `weeklyDeliveriesCount >= 120` → retorna `new BigDecimal("40.00")`, senão `new BigDecimal("0.00")`.
   * `BigDecimal calculateNetWeeklyTotal(BigDecimal basePlan, BigDecimal inspectionCharges, BigDecimal discount, int weeklyDeliveriesCount)`:
     * Retorna `max(0.00, basePlan + inspectionCharges - discount - bonus)` com escala de 2 casas decimais (`RoundingMode.HALF_UP`).

### 3.2 Domínio Python (`apps/intelligence-service/app/domain/services/analytics_forecaster.py`)
1. **`calculate_demand_growth(current_period_count: int, previous_period_count: int) -> dict`:**
   * Calcula `growth_rate_pct` (`((current - previous) / max(previous, 1)) * 100.0`, arredondado em 1 casa) e `projected_next_7d_count` (`max(1, round((current_period_count / 4.0) * (1.0 + max(growth_rate_pct, -30.0) / 200.0)))`).
2. **`evaluate_stockout_risks(parts_usage: list[dict]) -> list[dict]`:**
   * Para cada item `{sku, part_name, model_name, stock_quantity, min_stock_quantity, occurrences_30d}`:
     * `daily_burn_rate = round(max(occurrences_30d / 30.0, 0.2), 2)`
     * `days_to_stockout = int(round(stock_quantity / daily_burn_rate))`
     * `risk_level`: `'CRITICAL'` se `stock_quantity <= min_stock_quantity` ou `days_to_stockout <= 4`; `'WARNING'` se `days_to_stockout <= 8`; senão `'HEALTHY'`.
     * `suggested_restock_qty = max(10, (min_stock_quantity * 3) - stock_quantity)` se `risk_level != 'HEALTHY'` senão `0`.
   * Ordena colocando `CRITICAL` primeiro, depois `WARNING`, depois menor `days_to_stockout`.
3. **`build_deterministic_ops_advice(yard_label: str, growth_info: dict, stock_risks: list[dict], top_model: str) -> dict`:**
   * Gera a estrutura canônica `{executive_headline, growth_analysis_text, stockout_alerts, recommended_actions}` usada tanto como base para enriquecimento pelo Gemini (`gemini-3.5-flash-lite`) quanto como fallback offline 100% confiável.

---

## 4. Contratos Exatos das Rotas REST (`apps/core-api` e `apps/intelligence-service`)

### 4.1 Perfil e Segurança (`/api/v1/profile/me`)
* **`GET /api/v1/profile/me`** (Autenticado: qualquer perfil)
  ```json
  {
    "userId": "11111111-1111-1111-1111-111111111101",
    "role": "RIDER",
    "fullName": "Carlos Silva",
    "cpf": "418.***.***-09",
    "phone": "(11) 98765-4321",
    "email": "entregador@mottu-ops.local",
    "preferredUnit": "Unidade Butantã (SP-04)",
    "pixKey": "entregador@mottu-ops.local",
    "cnhStatus": "Categoria A • Válida até 08/2029",
    "employeeCode": "CTR-2026-8841",
    "roleTitle": "Entregador parceiro",
    "shiftLabel": "Plano Minha Mottu Semanal",
    "activePlate": "MOT1A23",
    "contractCode": "CTR-2026-8841",
    "passwordSha256Fingerprint": "a1b2c3d4...",
    "updatedAt": "2026-11-09T03:00:00Z"
  }
  ```
* **`PUT /api/v1/profile/me`**
  * Request: `{"fullName": "...", "phone": "...", "email": "...", "pixKey": "...", "preferredUnit": "...", "roleTitle": "..."}`
  * Response `200 OK`: Mesmo formato do `GET /api/v1/profile/me`.
* **`PUT /api/v1/profile/me/password`**
  * Request: `{"currentPassword": "...", "newPassword": "...", "newPasswordSha256": "..."}`
  * Response `200 OK`: `{"success": true, "message": "Senha atualizada com criptografia SHA-256 + BCrypt em todos os dispositivos.", "passwordSha256Fingerprint": "..."}`
  * Erros: `400 Bad Request` (senha curta/vazia), `401 Unauthorized` (senha atual incorreta), `422 Unprocessable Entity` (reutilização da mesma senha detectada via `SHA-256`).

### 4.2 Operações de Oficina e Almoxarifado (`/api/v1/mechanic/operations`)
* **`GET /api/v1/mechanic/operations`** (`MECHANIC`, `MANAGER`)
  ```json
  {
    "serviceOrders": [
      {
        "id": "uuid",
        "code": "OS-2026-0412",
        "plate": "MOT8K11",
        "model": "Mottu Sport 110i",
        "bay": "Baia 01 • Urgente",
        "serviceTitle": "Troca de manete direito + alinhamento do guidão",
        "originLabel": "Vistoria #8841 • Moto parada",
        "slaMinutes": 25,
        "status": "IN_REPAIR"
      }
    ],
    "stockItems": [
      {
        "partId": "uuid",
        "sku": "SKU-SPORT-001",
        "partName": "Manete de Freio Direito",
        "model": "Mottu Sport 110i",
        "aisleBin": "Corredor A • Gaveta 04",
        "quantity": 4,
        "minQuantity": 8,
        "unitPriceBrl": 25.00
      }
    ]
  }
  ```
* **`POST /api/v1/mechanic/operations/orders`** (`MECHANIC`, `MANAGER`)
  * Request: `{"plate": "MOT1A23", "bay": "Baia 02", "serviceTitle": "Revisão preventiva de freio"}`
  * Response `201 Created`: Objeto `serviceOrder` criado.
* **`PATCH /api/v1/mechanic/operations/orders/{id}/advance`** (`MECHANIC`, `MANAGER`)
  * Avança status da OS no banco, atualiza status da moto (`IN_MAINTENANCE` ou `RENTED`/`AVAILABLE`) e, ao chegar em `READY_PICKUP`, desconta `-1` unidade no estoque das peças da vistoria.
* **`PATCH /api/v1/mechanic/operations/stock/{sku}`** (`MECHANIC`, `MANAGER`)
  * Request: `{"delta": 1}` (ou `-1`, `+20`). Atualiza `stock_quantity` no PostgreSQL e retorna o item atualizado.

### 4.3 Contratos, Faturamento Semanal e Entregas (`/api/v1/manager/billing` e `/api/v1/rider/rental`)
* **`GET /api/v1/manager/billing/contracts`** (`MANAGER`)
  ```json
  [
    {
      "contractId": "uuid",
      "contractCode": "CTR-2026-8841",
      "riderName": "Carlos Silva",
      "plate": "MOT1A23",
      "model": "Mottu Sport 110i",
      "weeklyPlanBrl": 196.00,
      "inspectionChargesBrl": 60.00,
      "discountBrl": 15.00,
      "deliveryBonusBrl": 0.00,
      "netTotalBrl": 241.00,
      "paymentStatus": "PENDING_PIX",
      "dueDateLabel": "Vence segunda-feira • 18:00"
    }
  ]
  ```
* **`POST /api/v1/manager/billing/contracts/{contractCode}/confirm-pix`** (`MANAGER`)
  * Marca `payment_status = 'PAID'`, `paid_at = NOW()`, `due_date_label = 'Pago via Pix • Confirmado'` e retorna o contrato atualizado.
* **`GET /api/v1/rider/rental/summary`** (`RIDER`)
  * Retorna a fatura semanal real do entregador logado (`contractCode`, `activePlate`, `baseWeeklyPlanBrl`, `punctualityDiscountBrl`, `deliveryBonusBrl`, `inspectionChargesBrl`, `weeklyTotalBrl`, `paymentStatus`, `dueDateLabel`, `chargedInspections[]`, `weeklyDeliveriesTotal`, `weeklyDeliveryEarningsBrl`, `deliveryGoalTarget: 120`, `deliveryDays[]`).
* **`POST /api/v1/rider/rental/pay-pix`** (`RIDER`)
  * Registra o pagamento Pix do contrato ativo do entregador (`payment_status = 'PAID'`) e retorna o `summary` atualizado.
* **`POST /api/v1/rider/rental/deliveries`** (`RIDER`)
  * Request: `{"completedCount": 5, "earningsBrl": 52.50}`
  * Incrementa as entregas do dia atual em `rider_daily_deliveries`; se o total semanal atingir `>= 120`, o `deliveryBonusBrl` passa automaticamente para `40.00` abatendo o valor da fatura!

### 4.4 Indicadores Analíticos Reais & Consultor IA BigQuery (`/api/v1/manager/analytics`)
* **`GET /api/v1/manager/analytics/detailed`** (`MANAGER`)
  * Retorna `yardSnapshots[]` (calculados por SQL real para `ALL` e cada unidade), `dailyVolumeBars[]`, `verdictBreakdown[]`, `fleetModelBreakdown[]` e `topAuditedParts[]`.
* **`POST /api/v1/manager/analytics/ai-advisor?yardKey=ALL`** (`MANAGER`)
  * Chama `/internal/v1/analytics/advisor` no `intelligence-service` passando métricas do BigQuery + saldo de estoque atual e retorna:
  ```json
  {
    "yardKey": "ALL",
    "yardLabel": "Todas as Unidades (SP)",
    "generatedAt": "2026-11-09T03:30:00Z",
    "modelUsed": "gemini-3.5-flash-lite",
    "demandGrowthPct": 18.4,
    "projectedNext7DaysInspections": 135,
    "executiveHeadline": "Crescimento de 18,4% na demanda de vistorias com alerta de reposição imediata para 3 peças de freio e guidão.",
    "growthAnalysisText": "O cruzamento do BigQuery (fact_inspections) indica aceleração de chamados na Mottu Sport 110i e Honda Pop 110i...",
    "stockoutAlerts": [
      {
        "sku": "SKU-SPORT-001",
        "partName": "Manete de Freio Direito",
        "modelName": "Mottu Sport 110i",
        "currentStock": 4,
        "minStock": 8,
        "dailyBurnRate": 1.5,
        "daysToStockout": 3,
        "riskLevel": "CRITICAL",
        "suggestedRestockQty": 20
      }
    ],
    "recommendedActions": [
      "Autorizar reposição imediata de +20 un. de Manete de Freio Direito (SKU-SPORT-001) para evitar parada de baia em 3 dias.",
      "Priorizar check-up preventivo de lonas de freio traseiro nas motos Mottu Sport 110i acima de 10.000 km.",
      "Reforçar bancada do turno da manhã na unidade Butantã para absorver a projeção de 135 vistorias nos próximos 7 dias."
    ]
  }
  ```

---

## 5. Roteiro Passo a Passo das Tarefas (`TASK-19` a `TASK-24`)

### `TASK-19` — TDD Domínio Java (`SHA-256`, Ordem de Serviço, Faturamento Semanal) + Migration `V8`
* **Pasta Permitida:** `apps/core-api/src/test/java/com/mottu/ops/domain/`, `apps/core-api/src/main/java/com/mottu/ops/domain/`, `apps/core-api/src/main/resources/db/migration/`
* **Passos:**
  1. Criar `PasswordSecurityPolicyTest.java`, `ServiceOrderPolicyTest.java` e `WeeklyBillingCalculatorTest.java` em `src/test/java/com/mottu/ops/domain/service/`.
  2. Implementar `PasswordSecurityPolicy.java`, `ServiceOrderPolicy.java`, `ServiceOrderStatus.java` e `WeeklyBillingCalculator.java` em `src/main/java/com/mottu/ops/domain/` (100% Java puro, zero imports externos).
  3. Criar `V8__operational_workflows_billing_and_security.sql` populando metadados de perfil, estoque físico, ordens de serviço iniciais, dados financeiros dos 16 contratos e entregas diárias.
* **VERIFY:** `cd apps/core-api && ./mvnw test jacoco:check -Dtest="com.mottu.ops.domain.**"` (deve passar com 100% de cobertura de linhas e branches em `domain/`).

### `TASK-20` — TDD Domínio Python (`AnalyticsForecaster`) + Rota FastAPI `/internal/v1/analytics/advisor`
* **Pasta Permitida:** `apps/intelligence-service/`
* **Passos:**
  1. Criar `tests/unit/test_analytics_forecaster.py` cobrindo `calculate_demand_growth`, `evaluate_stockout_risks` (`CRITICAL`, `WARNING`, `HEALTHY`) e `build_deterministic_ops_advice` (incluindo casos de fronteira como `previous_period_count = 0` e lista vazia).
  2. Implementar `app/domain/services/analytics_forecaster.py` com 100% de cobertura.
  3. Criar o endpoint `POST /internal/v1/analytics/advisor` em `app/modules/analytics/` que calcula as previsões matemáticas e, quando `GEMINI_MOCK_MODE=false`, enriquece `executive_headline`, `growth_analysis_text` e `recommended_actions` via `gemini-3.5-flash-lite` (com fallback automático seguro).
* **VERIFY:** `cd apps/intelligence-service && .venv/bin/pytest --cov=app/domain --cov-branch --cov-fail-under=100`

### `TASK-21` — Backend Spring Boot: Perfil `SHA-256` (`/api/v1/profile/me`) + Operações de Oficina e Almoxarifado (`/api/v1/mechanic/operations`)
* **Pasta Permitida:** `apps/core-api/`
* **Passos:**
  1. Atualizar `UserJpaEntity`, `CatalogPartJpaEntity` e criar `ServiceOrderJpaEntity` + `ServiceOrderJpaRepository`.
  2. Implementar `ProfileApplicationService` e `ProfileController` (`GET /api/v1/profile/me`, `PUT /api/v1/profile/me`, `PUT /api/v1/profile/me/password` com validação `PasswordSecurityPolicy` e `BCryptPasswordEncoder`).
  3. Implementar `MechanicOperationsService` e `MechanicOperationsController` (`GET /api/v1/mechanic/operations`, `POST .../orders`, `PATCH .../orders/{id}/advance`, `PATCH .../stock/{sku}`).
  4. Adicionar gatilho em `AppointmentService`: ao agendar visita ao galpão, criar automaticamente uma Ordem de Serviço `WAITING_BAY` para aquela moto/vistoria.
  5. Criar teste de integração `ProfileAndOperationsIntegrationTest.java`.
* **VERIFY:** `cd apps/core-api && ./mvnw test -Dtest=ProfileAndOperationsIntegrationTest`

### `TASK-22` — Backend Spring Boot: Faturamento Semanal Pix (`T12`), Aluguel/Entregas do Entregador (`/rider/rental`) e Analytics Detalhado + IA BigQuery (`T10`)
* **Pasta Permitida:** `apps/core-api/`
* **Passos:**
  1. Atualizar `RentalContractJpaEntity` e criar `RiderDailyDeliveryJpaEntity` + `RiderDailyDeliveryJpaRepository`.
  2. Implementar `BillingAndRentalService`, `ManagerBillingController` (`/api/v1/manager/billing/*`) e `RiderRentalController` (`/api/v1/rider/rental/*`), calculando avarias `MAU_USO` direto da tabela `inspections`.
  3. Expandir `ManagerAnalyticsService` e `ManagerAnalyticsController` com `GET /api/v1/manager/analytics/detailed` (queries SQL reais agrupadas por unidade, modelo, veredito e peças) e `POST /api/v1/manager/analytics/ai-advisor` (integrado ao `IntelligenceServiceClient`).
  4. Criar teste de integração `BillingAndAnalyticsAdvisorIntegrationTest.java`.
* **VERIFY:** `cd apps/core-api && ./mvnw clean verify`

### `TASK-23` — Frontend Angular 19: Conexão 100% Real nas Telas `Account`, `Operations`, `T12 Billing`, `Rider Rental` e `T10 Analytics` (Com Painel IA BigQuery)
* **Pasta Permitida:** `apps/web-app/`
* **Passos:**
  1. Atualizar `ProfileService`, `RiderAccountComponent` e `StaffAccountComponent` para consumir `GET / PUT /api/v1/profile/me` e `PUT /api/v1/profile/me/password` (enviando digest `SHA-256` calculado no cliente + senha), mantendo fallback seguro para testes unitários offline.
  2. Atualizar `MechanicOperationsComponent` para consumir `/api/v1/mechanic/operations`, incluir formulário rápido de "+ Nova Ordem de Serviço" e persistir avanços de baia e ajustes de estoque no banco.
  3. Atualizar `T12BillingComponent` para consumir `/api/v1/manager/billing/contracts` e permitir confirmar recebimento Pix no drawer lateral (`POST .../confirm-pix`).
  4. Atualizar `RiderRentalComponent` para consumir `/api/v1/rider/rental/summary`, permitir confirmar pagamento Pix (`POST .../pay-pix`) e registrar novas entregas do dia (`POST .../deliveries`) para bater a meta de 120 entregas e ganhar o bônus de `R$ 40,00`.
  5. Atualizar `T10AnalyticsComponent` para carregar dados reais de `/api/v1/manager/analytics/detailed` e exibir a nova seção **"Diagnóstico e Previsão de Demanda IA (BigQuery + Almoxarifado)"** (`POST /api/v1/manager/analytics/ai-advisor`), com botão de 1 clique **"Repor lote no estoque"** nos alertas de ruptura de peças.
  6. Atualizar/adicionar testes Vitest (`*.spec.ts`).
* **VERIFY:** `source ~/.nvm/nvm.sh && cd apps/web-app && npm test -- --run && npm run build -- --configuration production`

### `TASK-24` — Verificação Ponta a Ponta, Registro em `HISTORICO_IA.md` e Deploy Contínuo na VM
* **Pasta Permitida:** Repositório completo
* **Passos:**
  1. Rodar todas as suítes de testes (`apps/core-api`, `apps/intelligence-service`, `apps/web-app`).
  2. Marcar `[x]` nas tarefas `TASK-19` a `TASK-24` em `mottu-smart-ops.md` e registrar o bloco detalhado em `docs/HISTORICO_IA.md`.
  3. Fazer commit e push para `origin main` e validar o deploy automático na VM (`http://161.153.2.148:18088`).



---

## 6. Fase 8 (Refinamento Executivo): Seletor Duplo de Perfis no Login (Vitrine vs. Vida Real) & Google-Grade AI Data Explorer (`TASK-25` a `TASK-27`)

### 6.1 Objetivo do Refinamento
Atender às duas melhorias centrais de experiência de produto (UX Executiva):
1. **Login com Duas Barras de Acesso Rápido (Perfis Vitrine "Dia a Dia" vs. Perfis "Operação Vida Real"):**
   * Na tela de login ([login.component.html](file:///home/robson/Projetos_Desenvolvimento/mattu-projeto-selecao/apps/web-app/src/app/features/auth/login.component.html)), exibir **duas listas separadas e claramente identificadas**:
     * **Lista 1 — Perfis Vitrine ("Dia a Dia Perfeito" / Dados Demonstrativos Pré-Populados):**
       * *Entregador Vitrine (`Carlos Entregador` — `entregador@mottu-ops.local`):* Mostra a rotina completa já preenchida (moto `MOT1A23`, 118/120 entregas, histórico de laudos e fatura).
       * *Mecânico Vitrine (`Mecânico Chefe` — `mecanico@mottu-ops.local`):* Mostra as 6 baias ativas, fila de triagem e almoxarifado completo.
       * *Gestor Vitrine (`Gestor Operações` — `gestor@mottu-ops.local`):* Mostra o consolidado dos 5 pátios de SP, 72 vistorias e indicadores cheios.
     * **Lista 2 — Perfis de Operação Real ("Vida Real" — Salvar, Enviar, Aprovar e Pagar no Banco):**
       * *Entregador Vida Real (`Rafael Souza` — `rafael.silva@mottu-ops.local`):* Conta real dedicada para o usuário testar **enviar nova vistoria com foto**, **lançar entregas e destravar bônus na hora**, **pagar Pix** e **salvar alterações de perfil/senha**.
       * *Mecânico Vida Real (`Diego Oficina` — `mecanico.real@mottu-ops.local`):* Conta real dedicada para **assumir chamado da fila**, **salvar laudo técnico**, **criar nova Ordem de Serviço**, **avançar moto na baia** e **baixar estoque real**.
       * *Gestor Vida Real (`Roberto Diretor` — `gestor.real@mottu-ops.local`):* Conta real dedicada para **aprovar/rejeitar cadastros**, **conciliar Pix**, **sincronizar Loja Mottu** e **pesquisar nos dados com a IA**.
2. **Redesenho do Relatório de IA no Padrão Google (*BigQuery Studio / Looker Concierge*) com Pesquisa Livre (`Ask Your Data`):**
   * Posicionar a inteligência logo no topo da tela `T10` ([t10-analytics.component.html](file:///home/robson/Projetos_Desenvolvimento/mattu-projeto-selecao/apps/web-app/src/app/features/manager/pages/t10-analytics/t10-analytics.component.html)) com layout corporativo limpo, direto e sem textos longos.
   * Adicionar **Barra de Pesquisa Inteligente nos Dados (`POST /api/v1/manager/analytics/ask`)**:
     * Permite ao gestor digitar qualquer pergunta ou termo sobre a operação (ex.: *"Qual peça dá mais prejuízo?"*, *"Estoque crítico Butantã"*, *"Motos com mau uso"*, *"Resumo de faturamento Pix"*) ou clicar em **6 pílulas rápidas**.
     * Retorna resposta estruturada em **3 blocos executivos diretos**:
       1. **Resposta Direta (TL;DR + Métrica Principal)**
       2. **Tabela de Evidências Reais do Banco (`evidenceRows[]`)**
       3. **Botões de Ação Direta + Exportar CSV / Copiar Resumo Executivo**

---

### 6.2 Contratos Técnicos da Fase 8

#### 1. Migration `V11__seed_live_operational_profiles.sql`
* Garante a existência dos 3 usuários da lista **"Operação Vida Real"** (`rafael.silva@mottu-ops.local` como `RIDER`, `mecanico.real@mottu-ops.local` como `MECHANIC`, `gestor.real@mottu-ops.local` como `MANAGER`, todos com senha `Mottu@2026!`), além de 2 cadastros `PENDING_APPROVAL` prontos para o Gestor Vida Real aprovar com 1 clique na tela `T08`.

#### 2. Rota de Pesquisa Inteligente nos Dados (`POST /api/v1/manager/analytics/ask`)
* **Request (`ManagerDataQueryRequest`):**
  ```json
  {
    "query": "Quais peças estão com estoque crítico e precisam de compra hoje?",
    "yardKey": "ALL"
  }
  ```
* **Response `200 OK` (`ManagerDataQueryResponse`):**
  ```json
  {
    "query": "Quais peças estão com estoque crítico e precisam de compra hoje?",
    "yardKey": "ALL",
    "generatedAt": "2026-11-09T19:30:00Z",
    "modelUsed": "gemini-3.5-flash-lite",
    "directAnswer": "Existem 5 peças abaixo do estoque mínimo com risco de parada de baia em até 4 dias (custo estimado de reposição: R$ 1.840,00).",
    "keyMetricLabel": "Itens em Ruptura Iminente",
    "keyMetricValue": "5 SKUs críticos",
    "keyMetricTrend": "Ação imediata recomendada",
    "columns": ["SKU", "Peça / Modelo", "Saldo Atual", "Consumo Diário", "Previsão Ruptura", "Lote Sugerido"],
    "rows": [
      ["SKU-SPORT-001", "Manete de Freio Direito (Mottu Sport 110i)", "4 un. (mín 8)", "1.5 un./dia", "3 dias (CRÍTICO)", "+20 un."],
      ["SKU-POP-002", "Espelho Retrovisor Par (Honda Pop 110i)", "4 un. (mín 8)", "1.2 un./dia", "3 dias (CRÍTICO)", "+20 un."]
    ],
    "suggestedActionLabel": "Repor todos os itens críticos (+20 un.)",
    "suggestedActionRoute": "/mechanic/operations"
  }
  ```

---

### 6.3 Roteiro de Execução (`TASK-25` a `TASK-27`)

* **`TASK-25` — Backend Java + Python: Migration `V11` (Perfis Vida Real) + Motor de Pesquisa Direta nos Dados (`POST /api/v1/manager/analytics/ask`)**
  * Implementar função pura de domínio + rota `/internal/v1/analytics/ask` no Python e `/api/v1/manager/analytics/ask` no Spring Boot cruzando dados reais de `inspections`, `catalog_parts`, `motorcycles` e `rental_contracts`.
* **`TASK-26` — Frontend Angular 19: Login com Duas Barras de Perfis (Vitrine vs. Vida Real) + Seletor Rápido no Header + Redesign Google-Grade do Relatório IA (`T10`)**
  * Atualizar `login.component.ts|html` com as duas listas de perfis detalhadas (nome, papel, placa/galpão e o que testar em cada um).
  * Redesenhar o bloco de IA da `T10` no padrão Google Cloud / Looker com barra de pesquisa livre, pílulas rápidas, tabela de evidências filtrada, exportação CSV e cópia de resumo em 1 clique.
* **`TASK-27` — Testes Automatizados Ponta a Ponta e Deploy Ao Vivo na Porta 80 (`http://161.153.2.148`)**
  * Rodar `pytest`, `./mvnw test` e `npm test -- --run`, commitar na `main` e validar ao vivo na nuvem.
