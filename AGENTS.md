# AGENTS.md — Manual Obrigatório para Todos os Agentes de IA

> **ATENÇÃO IA EXECUTORA:** Leia este arquivo inteiro antes de criar ou editar qualquer arquivo. Ele tem **prioridade máxima (`P0`)** sobre instruções genéricas de `.agents/agent/*.md` e foi feito para manter agentes autônomos noturnos (`/goal`), modelos menores e multi-agentes (`/teamwork`) 100% alinhados sem parar no meio.

---

## 1. Mapa de Leitura Rápida (Onde achar cada resposta)

1. **Qual tarefa devo fazer agora?** → Abra `mottu-smart-ops.md` (procure a primeira tarefa `[ ]` com dependências concluídas) e confira o último bloco de `docs/HISTORICO_IA.md`.
2. **Qual é a regra de negócio ou o que tem em cada tela?** → Abra `docs/01-PMD.md`.
3. **Qual é a porta, `.env`, pasta, tabela SQL, Enum ou JSON exato da API?** → Abra `docs/02-ARQUITETURA-E-APIS.md`.
4. **Qual teste devo escrever ANTES do código?** → Abra `docs/03-CASOS-DE-USO-TDD.md`.
5. **Quais cores, fontes e layouts devo usar?** → Abra `DESIGN.md` e `design/mottu-smart-ops.pen`.
6. **Faltou alguma chave externa do usuário?** → **NUNCA PARE A EXECUÇÃO!** Veja `docs/PENDENCIAS_DO_HUMANO.md` e a Lei 8 abaixo.

---

## 2. As 8 Leis Inquebráveis do Projeto

1. **TDD Real (Teste ANTES do Código):**
   * **Proibido** escrever regra de negócio antes de criar o arquivo de teste correspondente listado em `docs/03-CASOS-DE-USO-TDD.md`.
   * Ordem obrigatória: `1. Criar teste e ver falhar (RED)` → `2. Escrever código mínimo limpo (GREEN)` → `3. Conferir 100% de cobertura em domain`.
   * **Proibido** deixar `// TODO`, mocks fixos sujos no código de produção ou pular casos de fronteira.
2. **Pureza da Arquitetura Hexagonal:**
   * Nas pastas `apps/core-api/src/main/java/com/mottu/ops/domain/` (Java) e `apps/intelligence-service/app/domain/` (Python), **É PROIBIDO** importar frameworks externos (`org.springframework.*`, `jakarta.persistence.*`, `lombok.*` se gerar branches fantasmas, `fastapi`, `sqlalchemy`). Apenas código puro da linguagem!
3. **Contrato Travado (Zero Invenção):**
   * Nunca mude o nome de um campo JSON, rota REST, nome de fila Redis, coluna SQL, credencial seed (`Mottu@2026!`) ou valor de `Enum` definido em `docs/02-ARQUITETURA-E-APIS.md`.
4. **Respeito Estrito de Pasta (`Ownership`):**
   * Quando estiver executando uma tarefa do `mottu-smart-ops.md`, altere **apenas** os arquivos dentro do campo `Pasta Permitida` daquela tarefa. Isso evita conflito quando vários agentes rodam em paralelo.
5. **Fidelidade Visual (`DESIGN.md` + `.pen`) e Legibilidade Solar:**
   * Siga fielmente `design/mottu-smart-ops.pen` e os tokens de `DESIGN.md` (Fundo `#0A0E0D`, Verde Mottu `#00E676` com texto escuro `#06120B`).
   * **PROIBIDO usar cores roxas/violetas/índigo** em qualquer lugar (`Purple Ban`).
   * **Sobreposição sobre `frontend-specialist.md`:** Este é um sistema operacional de rua e oficina — **NUNCA** use textos sobrepostos ilegíveis ou layouts caóticos; priorize contraste extremo e botões de `52px+`.
6. **Travas Anti-Alucinação de Stack (Sobreposição sobre `.agents/agent/*.md`):**
   * **`apps/web-app` é Angular 19 (`Standalone` + `Signals`), NÃO é React/Next.js!** Proibido criar `.tsx`, `useState` ou `@NgModule` legado. Use apenas `standalone: true`, `@if`/`@for`/`@switch`, `signal()`/`computed()` e Tailwind CSS. Para streaming SSE autenticado, use `fetch()` com `ReadableStream` (nunca `new EventSource()` puro).
   * **`apps/core-api` é Java 21 + Spring Boot 3.4 + Maven (`mvn` / `./mvnw`), NÃO é Node.js!**
   * **`apps/intelligence-service` é Python 3.12+ + FastAPI + `pytest` (em `.venv` local)!**
7. **Registro Obrigatório de Entrega:**
   * Uma tarefa **SÓ** termina quando: *(a)* o comando `VERIFY` da tarefa rodou no terminal e passou sem erros, *(b)* você marcou `[x]` em `mottu-smart-ops.md`, e *(c)* você adicionou um bloco no final de `docs/HISTORICO_IA.md`.
8. **Modo Autônomo Noturno (Zero Parada por Chaves ou Runtimes):**
   * **Nunca pare esperando `GEMINI_API_KEY` ou credenciais Google Cloud:** Implemente os adaptadores **reais** (`google-genai` SDK e `google-cloud-bigquery` Batch Load) **E** mantenha o modo padrão `.env` com `GEMINI_MOCK_MODE=true` e `BIGQUERY_ENABLED=false` funcionando 100% offline para que todos os testes unitários, de integração e E2E passem durante a madrugada sem intervenção humana!
   * **Nota de Runtime desta Máquina Linux:** `Java 21`, `Maven 3.9`, `Python 3` e `Docker 29` já estão instalados nativamente. Caso `node`/`npm` não esteja no `PATH` da sessão bash (teste `source ~/.nvm/nvm.sh` primeiro; se não existir), execute todos os comandos do Angular (`npx`, `npm`, `ng`) via container oficial Docker: `docker run --rm -u $(id -u):$(id -g) -v "$PWD/apps/web-app:/app" -w /app node:22-bookworm-slim <comando>`, sem nunca parar pedindo instalação manual ao usuário!
