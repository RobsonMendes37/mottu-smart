# PLANO 01 — Previsibilidade de Peças, Quantidades e Valor a Pagar para o Entregador (`T02` & `T04`) com Auditoria Visual por Prints e TDD Estrito

> **Objetivo Executivo:** Garantir que o entregador (tanto no perfil **Demonstração** `entregador@mottu-ops.local` quanto no perfil **Operação Real** `entregador.real@mottu-ops.local`, além de novas vistorias enviadas pelo Wizard) visualize imediatamente a **quantidade de peças identificadas**, o **preço unitário oficial da Loja Mottu por peça**, a **faixa de desembolso (mínimo/máximo)** e **quanto ele precisa se preparar para pagar** (ou quanto economizou com a isenção de Desgaste Natural / Escudo 48h).
>
> **Trava Anti-Preguiça para a IA Executora (`P0`):** Nenhuma tarefa deste plano pode ser dada como concluída apenas "compilando código". É **obrigatório** validar os assets de imagem na largada, rodar testes unitários (`100% coverage`), testes de integração (RBAC + contratos JSON), rodar o Playwright para **capturar prints reais (`.png`) das telas `T02` e `T04`** e **inspecionar cada print usando `read_file`** para comprovar visualmente que os dados, fotos e preços renderizaram sem cortes, sem 404 e sem quebras.

---

## 1. Diagnóstico da Causa Raiz Atual & Verificação de Assets Existentes

### 1.1. Por que o retorno não aparecia para o usuário Demo (`entregador@mottu-ops.local`)
1. **Vistorias do Entregador Demo presas em `HUMAN_TRIAGE` (`V12`):**
   - Na migration `V12__seed_realistic_damaged_inspections.sql`, todas as 5 vistorias vinculadas ao contrato `a0000000-0000-0000-0000-000000000001` (`Carlos Entregador` — `entregador@mottu-ops.local`) foram inseridas com `status = 'HUMAN_TRIAGE'`.
   - **Impacto na Garagem (`T02`):** O contador `openInspectionsCount` ficava em `5 de 3`, acionando a trava de limite de 3 chamados abertos e bloqueando o botão **"Fazer vistoria rápida"**.
   - **Impacto no Status (`T04`):** Em `inspection-status.component.html`, o card com a lista de peças, quantidades e valores só era renderizado para `AUTO_RESOLVED`, `HUMAN_RESOLVED` ou `SCHEDULED`. Em `HUMAN_TRIAGE`, o sistema exibia apenas *"Em análise"* sem mostrar nenhum valor em reais.
2. **Colisão de Hash SHA-256 / dHash ao Reutilizar Fotos de Demonstração:**
   - Em `RiderInspectionService.createInspection()`, ao testar o envio de uma nova vistoria usando as fotos de demonstração (`/demo-photos/avaria-*.webp`), a checagem `existsByPhotoSha256` detectava que o hash já existia no banco, marcava `fraudSuspected = true` e jogava a vistoria direto para `HUMAN_TRIAGE` sem acionar o cálculo de peças da IA.
3. **Falta de Resumo de Quantidade de Peças na Garagem (`T02`):**
   - A listagem de `Vistorias recentes` na Garagem (`T02`) mostrava apenas o protocolo e o valor total, sem destacar a **quantidade de peças avaliadas**, os **nomes das peças** e o **demonstrativo claro de preparo financeiro** logo na tela inicial do entregador.

### 1.2. Inventário de Fotos de Demonstração Já Existentes no Projeto (Verificado)
Os seguintes arquivos de imagem **já existem** e estão sincronizados em `apps/web-app/public/demo-photos/` e `infra/mock-photos/` (zero necessidade de URLs externas que quebrem):
- `avaria-retrovisor.webp` / `.jpg` (`244 KB` — Retrovisor quebrado)
- `avaria-manete.webp` / `.jpg` (`146 KB` — Manete de freio avariado)
- `avaria-pneu.webp` / `.jpg` (`254 KB` — Pneu traseiro desgastado)
- `avaria-farol.webp` / `.jpg` (`212 KB` — Bloco óptico do farol trincado)
- `avaria-escapamento.webp` / `.jpg` (`205 KB` — Protetor de escapamento ralado)
- `avaria-painel.webp` / `.jpg` (`244 KB` — Painel / hodômetro)

---

## 2. Arquitetura da Solução (Zero Quebra, 100% Modular e TDD)

### 2.1. Etapa 0 (Pré-Voo): Validação de Integridade dos Assets e Dados de Semente
- Antes de alterar a UI, verificar via comando e teste automatizado que todas as chaves `photo_object_key` referenciadas na nova migration `V16` possuem correspondência exata no bucket MinIO (`infra/scripts/init-minio.sh`) e fallback direto para `/demo-photos/avaria-*.webp` caso o navegador rode em modo estático/offline, garantindo **0 erros 404** e `naturalWidth > 0`.

### 2.2. Banco de Dados & Semente Realista (`V16__rider_demo_parts_pricing_and_resolved_quotes.sql`)
- **Desbloqueio da Cota do Entregador Demo (`Carlos Entregador`):**
  - Atualizar as vistorias do contrato `a0000000-0000-0000-0000-000000000001` para estados concluídos (`AUTO_RESOLVED` e `HUMAN_RESOLVED`), deixando `openInspectionsCount = 0` para que o usuário Demo possa abrir novas vistorias livremente.
  - Preservar a fila do mecânico (`T05`) transferindo os chamados `HUMAN_TRIAGE` de demonstração da oficina para contratos secundários da frota (`a1000000-0000-0000-0000-000000000001` a `0005`).
- **Múltiplas Peças Precificadas por Vistoria (Cenários Completos para Apresentação):**
  - **Vistoria #1 (`d2000000-0000-0000-0000-000000000001` — Orçamento de Queda / Mau Uso — Preparo de Pagamento):**
    - Status: `AUTO_RESOLVED` | Veredito: `MAU_USO` | Foto: `inspections/sample-retrovisor.webp`.
    - **3 Peças identificadas em `inspection_items`:**
      1. `Retrovisor Direito Sport` (`SKU-RETROVISOR-DIR-SPORT`) — `1 un.` × `R$ 42,50`
      2. `Manete de Freio Direito` (`SKU-MANETE-DIR-SPORT`) — `1 un.` × `R$ 25,00`
      3. `Protetor Lateral do Escapamento` (`SKU-PROT-ESCAP-SPORT`) — `1 un.` × `R$ 38,00`
    - **Resumo Financeiro:** Subtotal de Peças = `R$ 105,50` + Mão de Obra Tabelada = `R$ 35,00` → **Total a preparar para pagar = `R$ 140,50`** (Estimativa mínima só peças: `R$ 105,50` | Estimativa máxima com oficina: `R$ 140,50`).
  - **Vistoria #2 (`d2000000-0000-0000-0000-000000000003` — Desgaste Natural — 100% Coberto pela Mottu, `R$ 0,00`):**
    - Status: `AUTO_RESOLVED` | Veredito: `DESGASTE_NATURAL` | Foto: `inspections/sample-pneu.webp`.
    - **2 Peças identificadas em `inspection_items`:**
      1. `Pneu Traseiro 80/100-14` (`SKU-PNEU-TRAS-SPORT`) — `1 un.` × `R$ 180,00` (Coberto Mottu → `R$ 0,00`)
      2. `Lona de Freio Traseira` (`SKU-LONA-FREIO-TRAS-SPORT`) — `1 un.` × `R$ 33,00` (Coberto Mottu → `R$ 0,00`)
    - **Resumo Financeiro:** Valor de tabela das 2 peças (`R$ 213,00`) + Oficina (`R$ 35,00`) = **Economia de `R$ 248,00` para o entregador (Total a pagar: `R$ 0,00`)**.
  - **Vistoria #3 (`d2000000-0000-0000-0000-000000000004` — Avaliada pela Oficina — Orçamento Confirmado pelo Mecânico):**
    - Status: `HUMAN_RESOLVED` | Veredito: `MAU_USO` | Foto: `inspections/sample-farol.webp`.
    - **2 Peças identificadas em `inspection_items`:**
      1. `Bloco Óptico do Farol Dianteiro` (`SKU-FAROL-SPORT`) — `1 un.` × `R$ 130,00`
      2. `Manete de Freio Direito` (`SKU-MANETE-DIR-SPORT`) — `1 un.` × `R$ 25,00`
    - **Resumo Financeiro:** Subtotal de Peças = `R$ 155,00` + Mão de Obra = `R$ 35,00` → **Total a preparar para pagar = `R$ 190,00`**.
  - Vincular também 2 vistorias concluídas ricas (1 `MAU_USO` com 2 peças e 1 `DESGASTE_NATURAL` com 2 peças) para o perfil `Rafael Entregador (Vida Real)` (`entregador.real@mottu-ops.local`, contrato `a2000000-0000-0000-0000-000000000001`).

### 2.3. Backend Java (`apps/core-api`)
- **Enriquecimento de `RecentInspectionDto` e `InspectionRepositoryAdapter`:**
  - Adicionar `int itemsCount` e `List<String> partNamesSummary` em `RecentInspectionDto` (mantendo um construtor sobrecarregado de 5 argumentos para não quebrar nenhum teste legado).
  - Na consulta `findRecentByContractId` / `findRecentByRiderId`, buscar os itens em `inspection_items` de cada vistoria recente para preencher `itemsCount` (ex.: `3`) e `partNamesSummary` (ex.: `["Retrovisor Direito Sport", "Manete de Freio Direito", "Protetor Lateral do Escapamento"]`).
- **Prevenção de Falso Positivo de Fraude em Fotos de Demonstração (`RiderInspectionService`):**
  - Quando `photoObjectKey` for um asset de teste (`sample-*`, `avaria-*` ou `demo-*`) ou o usuário for a conta de vitrine (`entregador@mottu-ops.local`), ignorar colisão de SHA-256/dHash repetido para que o usuário possa demonstrar o envio de fotos no Wizard quantas vezes quiser sem cair em bloqueio de fraude.
  - Além disso, quando o entregador criar uma nova vistoria em ambiente sem worker externo ativo ou usando foto de teste, gerar automaticamente os `inspection_items` precificados de acordo com o contexto (`USO_NORMAL` → `DESGASTE_NATURAL` `R$ 0,00` com 2 peças; `QUEDA_BATIDA` → `MAU_USO` com 2 peças e orçamento completo) para retorno imediato na tela `T04`.

### 2.4. Frontend Angular 19 (`apps/web-app`)
- **Tela `T02` — Garagem do Entregador (`garage.component.ts` & `.html`):**
  - Adicionar o card em destaque **"Último Orçamento de Peças & Previsibilidade de Pagamento"**:
    - Exibe a quantidade de peças avaliadas na última vistoria (`3 peças identificadas`), os nomes/preços resumidos e o valor exato a preparar para pagar (`R$ 140,50`) ou cobertura gratuita (`R$ 0,00`), com botão direto **"Ver preço de cada peça →"**.
  - Na lista **"Vistorias recentes"**, exibir para cada item:
    - Badge de quantidade de peças (`3 peças avaliadas` / `2 peças cobertas`), nomes das peças (`Retrovisor Direito Sport, Manete de Freio Direito...`) e status claro do valor (`Preparar R$ 140,50` ou `Isento R$ 0,00`).
- **Tela `T04` — Resultado da Vistoria (`inspection-status.component.ts` & `.html`):**
  - Destacar no topo um bloco claro **"Quanto você precisa preparar para pagar"** com:
    - **Quantidade total de peças:** `3 unidades identificadas`
    - **Tabela detalhada peça a peça:** Nome da peça, código de catálogo Loja Mottu, quantidade (`1 un.`), preço unitário (`R$ 42,50`, `R$ 25,00`, `R$ 38,00`).
    - **Demonstrativo de Desembolso:** Subtotal somente peças (`R$ 105,50`), Taxa única de mão de obra (`R$ 35,00`) e Total líquido a preparar para pagamento (`R$ 140,50`).
  - No estado `HUMAN_TRIAGE` (em análise pelo mecânico), exibir o **valor unitário tabelado de cada peça (`R$`) e a prévia do orçamento** em vez de ocultar os preços.

---

## 3. Protocolo Obrigatório de Qualidade, Segurança e Validação Visual por Prints

Para cada tarefa executada neste plano, a IA **DEVE** cumprir os 4 portões abaixo em ordem:

1. **Portão 1 — Testes Unitários e Pureza Hexagonal (`RED → GREEN`):**
   - Java: `cd apps/core-api && ./mvnw test -Dtest=DomainPurityTest,RiderGarageServiceTest,YardAndFleetIntegrationTest`
   - Angular: `source ~/.nvm/nvm.sh && cd apps/web-app && npm test -- --run`
2. **Portão 2 — Testes de Integração e Segurança (RBAC + Contratos):**
   - Verificar via MockMvc (`YardAndFleetIntegrationTest` e `InspectionCreationIntegrationTest`) que `GET /api/v1/rider/garage` retorna `openInspectionsCount: 0`, `recentInspections` com `itemsCount >= 2` e `partNamesSummary` populado, e que `GET /api/v1/rider/inspections/{id}` retorna os `items` com preços exatos e `totalEstimatedBrl` correto.
3. **Portão 3 — Teste de Interface Playwright com Captura e Leitura Obrigatória de Prints (`read_file`):**
   - Criar e executar o script `e2e/test-visual-plano01-rider.js` (em viewport Mobile `390x844` e Desktop `1440x900`) com **Network Integrity Trap** (zero HTTP >= 400 e zero loops) e **Image Pixel Trap** (`complete === true && naturalWidth > 0`).
   - O script deve salvar os seguintes prints em `/home/robson/.gemini/antigravity/brain/a9e6467f-aedb-40f4-a5bd-d79ae6357ef4/`:
     - `print-01-rider-demo-garage-t02.png`: Garagem do Entregador Demo mostrando cota livre (`0 de 3`), card de previsibilidade de peças e lista de vistorias com quantidade de peças e valores em `R$`.
     - `print-02-rider-demo-status-mau-uso-t04.png`: Tela `T04` da vistoria de Mau Uso mostrando as 3 peças, seus valores unitários (`R$ 42,50`, `R$ 25,00`, `R$ 38,00`), mão de obra (`R$ 35,00`) e total a preparar para pagar (`R$ 140,50`).
     - `print-03-rider-demo-status-isento-t04.png`: Tela `T04` da vistoria de Desgaste Natural mostrando as 2 peças (`R$ 180,00` e `R$ 33,00` riscados), economia de `R$ 248,00` e total a pagar `R$ 0,00`.
   - **OBRIGATÓRIO:** A IA deve chamar `read_file` nas 3 imagens `.png` geradas para inspecionar visualmente o layout antes de prosseguir!

---

## 4. Task Breakdown (INPUT → OUTPUT → VERIFY)

- [x] **`TASK-33` — Migration `V16` + Enriquecimento de Peças, Quantidades e Preços no Backend Java (`apps/core-api`)**
  - **Agent:** `backend-specialist` | **Skills:** `tdd-workflow`, `database-design`, `clean-code`
  - **Pasta Permitida:** `apps/core-api/`
  - **Dependencies:** `TASK-32`
  - **INPUT → OUTPUT → VERIFY:**
    - Validar que os arquivos `avaria-*.webp` existem em `infra/mock-photos/` e `apps/web-app/public/demo-photos/`.
    - Escrever testes em `YardAndFleetIntegrationTest` e `RiderGarageServiceTest` validando `itemsCount` e `partNamesSummary` (`RED`).
    - Criar `apps/core-api/src/main/resources/db/migration/V16__rider_demo_parts_pricing_and_resolved_quotes.sql`.
    - Atualizar `RecentInspectionDto`, `InspectionRepositoryAdapter` e `RiderInspectionService` (`GREEN`).
    - **VERIFY:** `cd apps/core-api && ./mvnw test -Dtest=DomainPurityTest,RiderGarageServiceTest,YardAndFleetIntegrationTest,InspectionCreationIntegrationTest` passa com 100% de aprovação.

- [x] **`TASK-34` — Frontend Angular 19 (`T02` e `T04`) + Auditoria Visual Playwright com Prints Inspecionados (`apps/web-app` & `e2e/`)**
  - **Agent:** `frontend-specialist` + `qa-automation-engineer` | **Skills:** `frontend-architecture`, `frontend-design`, `tdd-workflow`, `webapp-testing`
  - **Pasta Permitida:** `apps/web-app/`, `e2e/`
  - **Dependencies:** `TASK-33`
  - **INPUT → OUTPUT → VERIFY:**
    - Escrever testes unitários Vitest em `garage.component.spec.ts` e `inspection-status.component.spec.ts` exigindo renderização da quantidade de peças, preços unitários e valor a preparar para pagar (`RED`).
    - Atualizar `rider.models.ts`, `garage.component.ts`, `garage.component.html`, `inspection-status.component.ts` e `inspection-status.component.html` (`GREEN`).
    - Criar e rodar `e2e/test-visual-plano01-rider.js` gerando os 3 prints (`print-01-rider-demo-garage-t02.png`, `print-02-rider-demo-status-mau-uso-t04.png`, `print-03-rider-demo-status-isento-t04.png`) e **inspecioná-los via `read_file`**.
    - **VERIFY:** `source ~/.nvm/nvm.sh && cd apps/web-app && npm test -- --run && npm run build -- --configuration production` + `node e2e/test-visual-plano01-rider.js` passam com 0 erros de rede e 100% das imagens renderizadas.
