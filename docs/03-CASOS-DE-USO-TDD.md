# 03 — Casos de Uso e Guia Mestre de Testes (TDD)

**Versão:** 2.0 (Com Caminhos Exatos de Testes e Travas Anti-Armadilha do JaCoCo/Pytest)  
**Objetivo deste Documento:** Servir de **roteiro obrigatório de TDD (Teste Antes do Código)** para garantir que mesmo uma IA menor implemente cada regra com **100% de precisão** nas regras de negócio e cobertura completa das rotas de API.

---

## 1. Como a IA Deve Trabalhar com TDD neste Projeto (Regra Obrigatória)

Para **toda** tarefa de código, o agente executor deve seguir rigorosamente estes 4 passos (proibido pular!):

1. **🔴 Passo 1 — Escrever o Teste Primeiro (`RED`):** Criar exatamente o arquivo de teste listado abaixo cobrando os valores deste documento e rodar o comando de teste para confirmar que ele falha.
2. **🟢 Passo 2 — Escrever o Código Simples (`GREEN`):** Criar apenas o código limpo necessário para o teste passar (sem `// TODO` e sem chutar valores).
3. **🔵 Passo 3 — Limpar e Validar Cobertura (`REFACTOR`):** Garantir código legível, sem duplicação e conferir se a cobertura da pasta `domain/` continua em **100%**.
4. **📝 Passo 4 — Registrar no Histórico:** Anotar no `docs/HISTORICO_IA.md` qual teste passou antes de ir para a próxima tarefa.

### Trava Anti-Armadilha do `JaCoCo` e Pureza Hexagonal no Java (`IMPORTANTE!`)
* **Teste Automático de Pureza (`DomainPurityTest.java`):** Deve varrer todos os arquivos `.java` dentro de `src/main/java/com/mottu/ops/domain/` e falhar imediatamente caso encontre qualquer `import org.springframework.` ou `import jakarta.persistence.`.
* **Configuração Segura do `jacoco-maven-plugin`:** O compilador Java cria branches ocultos em `switch` de `Enum` que travam IAs menores em `98%` de *branch coverage*. Por isso, configure a regra `jacoco:check` cobrando **`COUNTER = LINE`, `MINIMUM = 1.00` (`100%`)** nos pacotes `com/mottu/ops/domain/service/**` e `com/mottu/ops/domain/model/**` (excluindo apenas `domain/enums/**` e construtores triviais de `domain/exception/**`).

---

## 2. Catálogo Completo de Casos de Uso e Arquivos de Teste Obrigatórios

### `UC-01` — Auto-Cadastro e Trava de Aprovação do Gestor
* **Arquivo de Teste:** `apps/core-api/src/test/java/com/mottu/ops/infrastructure/adapter/in/web/AuthAndApprovalIntegrationTest.java`
* **Regra:** Entregadores e Mecânicos criam conta sozinhos, mas não conseguem logar até o Gestor aprovar. Ninguém pode se auto-cadastrar como `MANAGER` pela rota pública.
* **Testes Obrigatórios:**
  1. **Dado** um CPF e e-mail inéditos com perfil `RIDER`, **Quando** enviar `POST /api/v1/auth/register`, **Então** salva com status `PENDING_APPROVAL` e senha criptografada (`BCrypt`).
  2. **Dado** alguém tentando enviar `"role": "MANAGER"` no cadastro público, **Quando** chamar a API, **Então** rejeita na hora com erro `400 Bad Request`.
  3. **Dado** um usuário com status `PENDING_APPROVAL` ou `REJECTED`, **Quando** tentar fazer login com a senha certa, **Então** bloqueia com `403 Forbidden` informando o status em formato RFC 7807.
  4. **Dado** um Gestor logado aprovando um `RIDER` e escolhendo uma moto `AVAILABLE`, **Quando** confirmar `POST /api/v1/manager/users/{id}/approve`, **Então** o usuário vira `ACTIVE`, a moto vira `RENTED` e nasce um contrato ativo com a data/hora atual.

---

### `UC-02` — Segurança de Acesso entre Perfis (`RBAC`)
* **Arquivo de Teste:** `apps/core-api/src/test/java/com/mottu/ops/infrastructure/adapter/in/web/RbacSecurityIntegrationTest.java`
* **Regra:** Um perfil nunca acessa dados nem ações de outro perfil.
* **Testes Obrigatórios:**
  1. **Dado** um token de `RIDER`, **Quando** chamar qualquer rota `/api/v1/mechanic/**` ou `/api/v1/manager/**`, **Então** retorna `403 Forbidden`.
  2. **Dado** um `RIDER` tentando abrir por ID o chamado de vistoria de **outro** entregador, **Quando** chamar `GET /api/v1/rider/inspections/{id}`, **Então** retorna `404 Not Found` (proteção contra vazamento de dados / IDOR).

---

### `UC-03` — Proteção contra Clique Duplo no 3G (`Idempotency-Key`) e Limite de Chamados
* **Arquivo de Teste:** `apps/core-api/src/test/java/com/mottu/ops/infrastructure/adapter/in/web/InspectionCreationIntegrationTest.java`
* **Regra:** Se o entregador enviar a mesma requisição várias vezes seguidas pela internet ruim, o sistema cria apenas 1 chamado. Máximo de 3 chamados em andamento (`QUEUED_AI`, `AWAITING_AI_RECOVERY`, `HUMAN_TRIAGE`) por contrato.
* **Testes Obrigatórios:**
  1. **Dado** o mesmo `Idempotency-Key` enviado 3 vezes para `POST /api/v1/rider/inspections`, **Quando** processar as chamadas, **Então** apenas **1 registro** existe no banco e todas retornam o mesmo `inspectionId`.
  2. **Dado** um entregador que já possui **3 chamados em andamento** (`QUEUED_AI` / `HUMAN_TRIAGE`), **Quando** tentar abrir o 4º chamado, **Então** retorna `422 Unprocessable Entity` pedindo para aguardar a conclusão dos atuais.

---

### `UC-04` — Barreira Anti-Fraude de Fotos (Hash SHA-256 / dHash no Servidor)
* **Arquivo de Teste:** `apps/core-api/src/test/java/com/mottu/ops/domain/service/PhotoFraudDetectorTest.java` (+ integração em `InspectionCreationIntegrationTest.java`)
* **Regra:** O servidor lê os bytes da foto no `MinIO` e calcula `photo_sha256` e `dHash`. Foto repetida de outro chamado ou reprocessada sem metadados confiáveis nunca recebe aprovação gratuita automática — nasce direto em `HUMAN_TRIAGE` (`FRAUD_SUSPICION`, peso `4`).
* **Testes Obrigatórios:**
  1. **Dado** uma foto cujo `photo_sha256` (ou hash visual perceptual) já existe em qualquer vistoria anterior no banco, **Quando** registrar a nova vistoria, **Então** marca `fraud_suspected = true`, define status `HUMAN_TRIAGE` com motivo `FRAUD_SUSPICION` (peso `4`) e **não envia para aprovação automática**.
  2. **Dado** uma imagem marcada com sinalizador de ausência/adulteração de metadados de captura, **Quando** validar a imagem, **Então** marca `fraud_suspected = true` e encaminha ao Mecânico.

---

### `UC-05` — Regra Matemática de Defeito Pré-Existente (`48 horas` ou `100 km`)
* **Arquivo de Teste:** `apps/core-api/src/test/java/com/mottu/ops/domain/service/PreExistingRuleCalculatorTest.java`
* **Regra (100% Testada):** Só aprova `PRE_EXISTENTE` automático (`R$ 0,00`) se `reportedContext == RECEIVED_LIKE_THIS`, sem suspeita de fraude, confiança da IA `>= 0.80` **E** (`horas_desde_retirada <= 48.0` **OU** `km_rodados_desde_retirada <= 100`).
* **Testes Unitários Obrigatórios (Casos de Fronteira Exatos):**
  1. **Dado** retirada há **`48,0` horas** e `150 km` rodados (`<= 48h` verdadeiro), **Quando** avaliar elegibilidade, **Então** **permite** `PRE_EXISTENTE` gratuito (`R$ 0,00`).
  2. **Dado** retirada há `72,0` horas, mas apenas **`100 km`** rodados (`<= 100 km` verdadeiro), **Quando** avaliar elegibilidade, **Então** **permite** `PRE_EXISTENTE` gratuito (`R$ 0,00`).
  3. **Dado** retirada há **`48,1` horas** **E** **`101 km`** rodados (estourou os dois limites!), **Quando** o entregador alegar `RECEIVED_LIKE_THIS`, **Então** **bloqueia aprovação automática** e envia para `HUMAN_TRIAGE` (Mecânico).

---

### `UC-06` — Regra de Desgaste Natural vs. Mau Uso e Cálculo do Orçamento
* **Arquivo de Teste:** `apps/core-api/src/test/java/com/mottu/ops/domain/service/BudgetAndVerdictCalculatorTest.java`
* **Regra (100% Testada):**
  * `PRE_EXISTENTE` ou `DESGASTE_NATURAL` → `total_estimated_brl = 0.00` e `labor_fee_brl = 0.00`.
  * `MAU_USO` → `total_estimated_brl = Soma(preço unitário das peças) + taxa fixa de mão de obra (padrão R$ 35,00)`.
* **Testes Unitários Obrigatórios:**
  1. **Dado** peça *"Lona de Freio (`R$ 33,00`)"* com `is_natural_wear = true` e laudo da IA sem impacto (`DESGASTE_NATURAL`), **Quando** calcular orçamento, **Então** total é **`R$ 0,00`**.
  2. **Dado** 1 peça *"Manete de Freio (`R$ 25,00`)"* classificada como `MAU_USO` e mão de obra padrão `R$ 35,00`, **Quando** calcular orçamento, **Então** total é **`R$ 60,00`**.
  3. **Dado** 2 peças na mesma foto (*"Manete `R$ 25,00`"* + *"Retrovisor `R$ 42,50`"*) em `MAU_USO` com mão de obra `R$ 35,00`, **Quando** calcular orçamento, **Então** cobra apenas 1 taxa de mão de obra e o total é **`R$ 102,50`**.

---

### `UC-07` — Filtro de Confiança da IA (`>= 0.80`) e Foto Inválida
* **Arquivo de Teste:** `apps/core-api/src/test/java/com/mottu/ops/domain/service/InspectionStateMachineTest.java`
* **Regra:** Só resolve sozinho com certeza `>= 0.80`. Se a foto não for de moto, rejeita na hora pedindo outra foto.
* **Testes Obrigatórios:**
  1. **Dado** retorno da IA com `isValidMotorcyclePhoto = false`, **Quando** processar resultado, **Então** status vira `INVALID_IMAGE` e não entra na fila do mecânico.
  2. **Dado** retorno da IA com `confidence = 0.80` e contexto válido, **Quando** processar resultado sem alertas, **Então** status vira `AUTO_RESOLVED`.
  3. **Dado** retorno da IA com `confidence = 0.79`, **Quando** processar resultado, **Então** status vira `HUMAN_TRIAGE` com motivo `LOW_AI_CONFIDENCE` (peso `3`).

---

### `UC-08` — Fila Inteligente de Espera quando o Gemini Cai (Sem Lotar os Mecânicos!)
* **Arquivo de Teste:** `apps/core-api/src/test/java/com/mottu/ops/domain/service/AiRecoveryPolicyTest.java`
* **Regra:** Se a API do Gemini falhar/oscilar (`outcome == AI_UNAVAILABLE`), o chamado fica em `AWAITING_AI_RECOVERY` tentando novamente (`30s → 60s → 300s`). **Só** vai para o Mecânico se `safetyRisk == CRITICAL_STOPPED` e já tiver passado de **5 minutos** desde `created_at`!
* **Testes Obrigatórios:**
  1. **Dado** um chamado `SAFE_TO_RIDE` onde o Gemini retornou indisponível, **Quando** processar falha temporária, **Então** status muda para `AWAITING_AI_RECOVERY`, incrementa `retry_count`, calcula `next_retry_at` e **NÃO entra em `HUMAN_TRIAGE`**.
  2. **Dado** um chamado `CRITICAL_STOPPED` em `AWAITING_AI_RECOVERY` criado há **`6 minutos`** sem sucesso da IA, **Quando** o verificador de SLA avaliar, **Então** move o chamado para `HUMAN_TRIAGE` com prioridade máxima **`CRITICAL_SAFETY` (peso `1`)**.

---

### `UC-09` — Contestação Única do Entregador ("Discordo do Resultado")
* **Arquivo de Teste:** `apps/core-api/src/test/java/com/mottu/ops/domain/service/InspectionStateMachineTest.java` (+ integração REST)
* **Regra:** O entregador pode contestar 1 única vez um chamado `AUTO_RESOLVED`, explicando o motivo com pelo menos 10 caracteres.
* **Testes Obrigatórios:**
  1. **Dado** um chamado `AUTO_RESOLVED` com `contested_once = false` e justificativa de 25 letras, **Quando** contestar, **Então** muda `status` para `HUMAN_TRIAGE`, grava `contested_once = true` e define `priorityReason = RIDER_CONTESTATION` (peso `2`).
  2. **Dado** justificativa curta demais (`"não fui"` — menos de 10 letras), **Quando** contestar, **Então** rejeita com `400 Bad Request`.
  3. **Dado** um chamado que **já teve contestação** (`contested_once = true`) ou já foi julgado por um mecânico (`HUMAN_RESOLVED`), **Quando** tentar contestar de novo, **Então** rejeita com `422 Unprocessable Entity`.

---

### `UC-10` — Mesa de Decisão do Mecânico e Auditoria de Acurácia da IA
* **Arquivo de Teste:** `apps/core-api/src/test/java/com/mottu/ops/infrastructure/adapter/in/web/MechanicWorkbenchIntegrationTest.java`
* **Regra:** A fila do mecânico busca apenas `status = HUMAN_TRIAGE` respeitando rigorosamente `priority_weight ASC, created_at ASC`. A decisão do mecânico recalcula o valor, gera o texto educado para o entregador, preserva `ai_verdict` para estatística e grava `inspection_audit_logs`.
* **Testes Obrigatórios:**
  1. **Dado** 4 chamados em `HUMAN_TRIAGE` (um de cada motivo), **Quando** buscar `GET /api/v1/mechanic/queue`, **Então** retorna exatamente na ordem: `CRITICAL_SAFETY` (1), `RIDER_CONTESTATION` (2), `LOW_AI_CONFIDENCE` (3), `FRAUD_SUSPICION` (4).
  2. **Dado** que a IA sugeriu `MAU_USO` (`R$ 60,00`), mas o Mecânico revisou e mudou para `DESGASTE_NATURAL`, **Quando** finalizar o laudo, **Então** `final_verdict` vira `DESGASTE_NATURAL` (`R$ 0,00`), `ai_verdict` continua `MAU_USO` (gerando na `analytics_outbox` `ai_matched_mechanic = false`) e cria registro em `inspection_audit_logs`.

---

### `UC-11` — Minerador VTEX (`loja.mottu.com.br`) + Preservação de Preço do Gestor + CRAG
* **Arquivos de Teste (`Pytest`):**
  * `apps/intelligence-service/tests/domain/test_vtex_normalizer.py`
  * `apps/intelligence-service/tests/domain/test_crag_evaluator.py`
  * `apps/intelligence-service/tests/modules/test_catalog_sync.py`
* **Regra:** O módulo Python busca peças por modelo de moto na VTEX, preserva preços editados manualmente pelo Gestor (`price_overridden = true`), usa snapshot local seguro se a loja cair e reduz confiança para `< 0.80` quando a similaridade vetorial CRAG for baixa.
* **Testes Obrigatórios:**
  1. **Dado** payload JSON simulado da API VTEX da Mottu Store, **Quando** rodar o normalizador, **Então** extrai `sku`, `part_name`, `unit_price_brl`, `is_natural_wear` (por palavras-chave de itens consumíveis) e associa ao `MotorcycleModel`.
  2. **Dado** que o Gestor editou manualmente uma peça no painel (`price_overridden = true`), **Quando** rodar uma nova sincronização da loja, **Então** o `unit_price_brl` manual daquela peça **é preservado** sem ser sobrescrito.
  3. **Dado** erro `503` / timeout ao acessar `loja.mottu.com.br`, **Quando** o Gestor acionar a sincronização, **Então** carrega o `seed_catalog_snapshot.json` se a tabela estiver vazia ou mantém os preços atuais, retornando `usedFallback = true` sem quebrar.
  4. **Dado** que o Gemini identificou uma peça com similaridade vetorial abaixo do mínimo confiável (`< 0.72`) para aquele modelo de moto, **Quando** o avaliador do **CRAG** pontuar o resultado, **Então** rebaixa `confidence` para `< 0.80` encaminhando ao mecânico humano.

---

### `UC-12` — Agendamento de Galpão e Exportação em Lote para o Google BigQuery
* **Arquivo de Teste:** `apps/core-api/src/test/java/com/mottu/ops/infrastructure/adapter/in/web/AppointmentAndOutboxIntegrationTest.java`
* **Regra:** O entregador agenda data hoje/futura e turno (`MORNING` ou `AFTERNOON`) em um galpão ativo. Chamados resolvidos entram na `analytics_outbox` e são enviados em lote (`Batch Load`) ao BigQuery.
* **Testes Obrigatórios:**
  1. **Dado** uma tentativa de agendar ida ao galpão para uma **data no passado**, **Quando** enviar `POST /api/v1/rider/inspections/{id}/appointment`, **Então** rejeita com `400 Bad Request`.
  2. **Dado** 5 chamados finalizados pendentes na tabela `analytics_outbox`, **Quando** o job de exportação rodar, **Então** agrupa os 5 em um único lote NDJSON, envia pelo `AnalyticsPort` e marca `exported = true`.

---

## 3. Comandos Automáticos de Verificação (Portão de Qualidade)

Nenhum agente poderá marcar uma funcionalidade como concluída sem que estes comandos terminem sem erros:

* **Java (`apps/core-api`):** `./mvnw clean verify` *(Roda `DomainPurityTest` + Testes Unitários + Testes de Integração + `JaCoCo 100% LINE` em `domain/service` e `domain/model`)*.
* **Python (`apps/intelligence-service`):** `pytest --cov=app/domain --cov-fail-under=100` *(Roda testes unitários e de contrato do CRAG + Minerador VTEX offline)*.
* **Angular (`apps/web-app`):** `npm run lint && npm run test:ci && npm run build` *(Valida TypeScript estrito, Standalone Components, Vitest e build PWA)*.
