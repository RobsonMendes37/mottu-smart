# Mottu Smart Ops — Sistema de Vistoria Inteligente, Telemetria & Operação de Frota

[![Release](https://img.shields.io/badge/Release-v2.2.0-00E676?style=for-the-badge&logo=git&logoColor=06120B)](https://github.com/RobsonMendes37/motus-smart/releases)
[![Oracle Cloud](https://img.shields.io/badge/Oracle%20Cloud-Live%20Demo%20(Porta%2018088)-00E676?style=for-the-badge&logo=oracle&logoColor=06120B)](http://161.153.2.148:18088)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions%20Automated-00E676?style=for-the-badge&logo=githubactions&logoColor=06120B)](https://github.com/RobsonMendes37/motus-smart/actions)
[![Tests](https://img.shields.io/badge/Tests-707%20Passed%20(100%25%20Domain)-00E676?style=for-the-badge&logo=vitest&logoColor=06120B)](#-qualidade-de-código--bateria-de-testes)
[![Java 21](https://img.shields.io/badge/Java-21%20LTS-00E676?style=for-the-badge&logo=openjdk&logoColor=06120B)](https://openjdk.org/)
[![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4.2-00E676?style=for-the-badge&logo=springboot&logoColor=06120B)](https://spring.io/projects/spring-boot)
[![Python 3.12+](https://img.shields.io/badge/Python-3.12%2B-00E676?style=for-the-badge&logo=python&logoColor=06120B)](https://www.python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.115-00E676?style=for-the-badge&logo=fastapi&logoColor=06120B)](https://fastapi.tiangolo.com/)
[![Angular 19](https://img.shields.io/badge/Angular-19%20Standalone%20%2B%20Signals-00E676?style=for-the-badge&logo=angular&logoColor=06120B)](https://angular.dev/)
[![Tailwind CSS v4](https://img.shields.io/badge/Tailwind-CSS%20v4-00E676?style=for-the-badge&logo=tailwindcss&logoColor=06120B)](https://tailwindcss.com/)
[![PostgreSQL 16 + pgvector](https://img.shields.io/badge/PostgreSQL-16%20%2B%20pgvector-00E676?style=for-the-badge&logo=postgresql&logoColor=06120B)](https://github.com/pgvector/pgvector)
[![Redis](https://img.shields.io/badge/Redis-7%20Queues-00E676?style=for-the-badge&logo=redis&logoColor=06120B)](https://redis.io/)
[![MinIO S3](https://img.shields.io/badge/MinIO-S3%20Compatible-00E676?style=for-the-badge&logo=minio&logoColor=06120B)](https://min.io/)
[![Caddy](https://img.shields.io/badge/Caddy-Reverse%20Proxy%20v2-00E676?style=for-the-badge&logo=caddy&logoColor=06120B)](https://caddyserver.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose%20Ready-00E676?style=for-the-badge&logo=docker&logoColor=06120B)](https://www.docker.com/)

> **Case Operacional e Técnico de Alto Impacto para a Maior Plataforma de Locação de Motos da América Latina.**  
> Solução integrada ponta a ponta para eliminar filas de galpão, automatizar triagem de avarias e sinistros com IA Multimodal (Gemini Vision + CRAG), prevenir fraudes de imagens e conectar telemetria de oficina ao Google BigQuery & Looker Studio.

---

## 🌐 Demonstração ao Vivo na Nuvem (Oracle Cloud VM)

O sistema está implantado e operando continuamente em ambiente de nuvem de alta disponibilidade:

> 🚀 **URL Pública de Acesso:** **[http://161.153.2.148:18088](http://161.153.2.148:18088)**  
> ⚡ **Modo Demonstração (1 Clique):** Ao acessar a tela inicial, utilize os botões de demonstração rápida para entrar instantaneamente como **Entregador**, **Mecânico** ou **Gestor** sem necessidade de digitação.

---

## 📌 Visão Executiva & Impacto de Negócio

Em operações de alta escala com mais de **50.000 motocicletas ativas rodando 14h/dia**, **moto parada na oficina é prejuízo duplo**: receita cessante para a empresa e perda de faturamento diário para o entregador parceiro.

O **Mottu Smart Ops** transforma o processo físico tradicional de vistoria em uma experiência digital ultrarrápida com governança matemática estrita:

* ⏱️ **Triagem Autônoma em 5 Segundos:** A IA classifica a avaria direto pelo celular e consulta o catálogo oficial da Mottu Store (`loja.mottu.com.br`).
* 🎯 **78,5% de Deflexão Operacional:** Casos rotineiros e óbvios são resolvidos na hora sem necessidade de intervenção do mecânico humano.
* ⚖️ **Orçamento Transparente e Sem Atritos:**
  * **Pré-existente (`PRE_EXISTENTE`):** Contrato recente ($\le 48\text{h}$ ou $\le 100\text{km}$) $\rightarrow$ **R$ 0,00 (Gratuito)**.
  * **Desgaste Natural (`DESGASTE_NATURAL`):** Peças de consumo rotineiro (pastilha, lona, kit relação) $\rightarrow$ **R$ 0,00 (Gratuito)**.
  * **Mau Uso ou Queda (`MAU_USO`):** Peças canônicas tabeladas + taxa fixa de mão de obra (R$ 35,00) $\rightarrow$ **Orçamento oficial imediato**.
* 🚨 **Módulo de Sinistro com Vídeo de 30s (`UC-18`):** Registro de acidentes na rua com vídeo de 360°, extração inteligente de frames, taxa de guincho automática (R$ 80,00) e bloqueio operacional preventivo da moto.
* 🛡️ **Defesa Anti-Fraude com Hash Perceptual (`pHash`) e Metadados:** Bloqueia reutilização de fotos antigas ou imagens baixadas da internet.
* 🛠️ **Mesa de Decisão do Mecânico com Lupa Reticular (T06 & T13):** Fila inteligente priorizada por severidade (P1 Segurança a P4 Suspeita de Fraude), liberando mais de **36 horas de oficina por semana**.
* 📊 **Governança com BigQuery & Looker Studio:** Exportação assíncrona via Transactional Outbox em formato NDJSON para painéis executivos em tempo real.
* 🔄 **Entrega Contínua CI/CD Automatizada:** GitHub Actions integrado com deploy remoto automático via OpenSSH na VM Oracle a cada push ou tag na branch `main`.

---

## 🏗️ Arquitetura do Sistema

```mermaid
flowchart TD
    subgraph Frontend["Frontend PWA (Angular 19 Standalone + Signals)"]
        UI_Rider["T02-T04: Área do Entregador (Mobile PWA)"]
        UI_Accident["T11-T13: Registro de Sinistro com Vídeo"]
        UI_Mech["T05-T07: Console da Oficina (Tablet/Web)"]
        UI_Mgr["T08-T10: Painel do Gestor (Looker Studio BI)"]
    end

    subgraph Gateway["Reverse Proxy & Gateway"]
        CADDY["Caddy v2 (Porta 18088)<br/>Flush Interval -1 (SSE Stream)"]
    end

    subgraph CoreBackend["Core API (Java 21 + Spring Boot 3.4)"]
        direction TB
        subgraph PortsIn["Adaptadores de Entrada"]
            REST["REST Controllers"]
            SSE["SSE Stream Controller"]
        end
        subgraph Domain["Domínio Hexagonal Puro (100% Cobertura)"]
            FSM["Inspection & Accident State Machines"]
            RULES["PreExisting, TowingFee & Budget Calculators"]
            FRAUD["Photo & Video Fraud Detector (pHash)"]
        end
        subgraph PortsOut["Adaptadores de Saída"]
            REPO["Spring Data JPA"]
            REDIS_ADAPTER["Redis Queue Adapter"]
            MINIO_ADAPTER["MinIO S3 Client"]
            OUTBOX_JOB["BigQuery NDJSON Batch Exporter"]
        end
    end

    subgraph AIService["Intelligence Service (Python 3.12 + FastAPI)"]
        CRAG["CRAG Evaluator (Corrective RAG)"]
        VISION["Gemini Vision Multi-Model Cascade (3.1/3.5/3.8)"]
        SCRAPER["Mottu Store VTEX Scraper & Normalizer"]
        VIDEO_WORKER["Accident Video Frame Extractor & Worker"]
    end

    subgraph DataStorage["Infraestrutura de Dados & Armazenamento"]
        POSTGRES[("PostgreSQL 16<br/>pgvector + pgcrypto<br/>Porta 15432")]
        REDIS[("Redis 7<br/>Filas de Triagem & Sinistro<br/>Porta 16379")]
        MINIO[("MinIO S3 Storage<br/>Bucket mottu-inspections<br/>Portas 19000/19001")]
        BIGQUERY[("Google BigQuery Sandbox<br/>mottu_ops_bi.fact_inspections")]
    end

    Frontend -->|HTTP / SSE / Presigned Upload| CADDY
    CADDY -->|/api/*| REST
    CADDY -->|/storage/*| MINIO
    CADDY -->|/* SPA| Frontend

    REST --> Domain
    Domain --> REPO --> POSTGRES
    Domain --> REDIS_ADAPTER -->|queue:ai_triage_jobs| REDIS
    Domain --> REDIS_ADAPTER -->|queue:accident_video_jobs| REDIS
    REDIS -->|brpop| AIService
    AIService -->|Busca Cosseno <=>| POSTGRES
    AIService -->|queue:ai_triage_results| REDIS
    REDIS_ADAPTER -->|Consome Resultado IA| Domain
    Domain --> MINIO_ADAPTER --> MINIO
    OUTBOX_JOB -->|analytics_outbox| POSTGRES
    OUTBOX_JOB -.->|Batch Load NDJSON| BIGQUERY
    BIGQUERY -.->|Iframe Incorporado| UI_Mgr
```

---

## 🏷️ Controle de Versão Semântica (SemVer) & Releases

O projeto adota rigorosamente o padrão **Semantic Versioning 2.0.0** (`MAJOR.MINOR.PATCH`) e automação via Git Tags:

| Versão | Data | Principais Entregas |
| :--- | :--- | :--- |
| **`v1.0.0`** | Nov/2026 | Arquitetura Hexagonal, Vistoria Fotográfica, Mesa do Mecânico (T01-T07) e TDD Inicial. |
| **`v2.0.0`** | Nov/2026 | Catálogo Oficial Mottu Store (VTEX), Google BigQuery Batch Load e Dashboard Looker Studio BI. |
| **`v2.1.0`** | Nov/2026 | Migração para Oracle Cloud VM com isolamento total de portas e proteção de serviços existentes. |
| **`v2.2.0`** | Nov/2026 | **Versão Atual:** Módulo de Sinistro por Vídeo (`UC-18`), 707 Testes Automatizados, Suíte Playwright E2E e CI/CD Contínuo no GitHub Actions. |

### Como criar e publicar uma nova Release:

```bash
# 1. Crie uma tag anotada no Git
git tag -a v2.2.0 -m "Release v2.2.0: Mottu Smart Ops Completo com CI/CD e Sinistro de Vídeo"

# 2. Envie a tag para o repositório remoto
git push origin v2.2.0

# 3. O pipeline do GitHub Actions (.github/workflows/cd.yml) detectará a tag
#    e atualizará a máquina virtual na nuvem automaticamente!
```

---

## 🚀 Início Rápido (1 Comando Docker)

O projeto foi construído para subir de forma **100% autônoma, local e gratuita** (com o modo simulado offline `GEMINI_MOCK_MODE=true` e `BIGQUERY_ENABLED=false` ativado por padrão):

```bash
# 1. Clone o repositório
git clone https://github.com/RobsonMendes37/motus-smart.git
cd motus-smart

# 2. Copie o arquivo de ambiente padrão
cp .env.example .env

# 3. Suba toda a stack de desenvolvimento com 1 comando
docker compose -f infra/docker-compose.dev.yml up -d
```

### 🌐 Mapeamento de Portas e Serviços

| Serviço | URL / Endpoint | Descrição |
| :--- | :--- | :--- |
| **Aplicação Web (Angular 19 PWA)** | [`http://localhost:18088`](http://localhost:18088) | Porta pública via proxy reverso Caddy (ou na VM `161.153.2.148:18088`) |
| **Backend Core API (Java Spring)** | [`http://localhost:18080`](http://localhost:18080) | API REST e endpoints de domínio hexagonal |
| **Swagger / OpenAPI (Core API)** | [`http://localhost:18080/swagger-ui.html`](http://localhost:18080/swagger-ui.html) | Documentação interativa dos contratos REST |
| **Intelligence Service (FastAPI)** | [`http://localhost:18000`](http://localhost:18000) | Serviço de IA, Gemini Vision, CRAG e scrapers |
| **Swagger / OpenAPI (Intelligence)**| [`http://localhost:18000/docs`](http://localhost:18000/docs) | Documentação interativa da API de IA |
| **MinIO Console (S3 Storage)** | [`http://localhost:19001`](http://localhost:19001) | Painel web de fotos e vídeos (`minioadmin` / `minioadmin123`) |
| **PostgreSQL 16 + pgvector** | `localhost:15432` | Banco transacional (`mottu_ops` / senha `.env`) |
| **Redis 7 (Filas & Mensageria)** | `localhost:16379` | Broker de mensageria leve para triagem assíncrona |

---

## 🔑 Credenciais Seed Prontas para Teste

O banco de dados já inicializa com dados pré-populados (`V2__seed_data.sql`). Todas as contas utilizam a senha canônica do projeto:

> 🔐 **Senha Padrão para Todos os Perfis:** `Mottu@2026!`

| Perfil | E-mail de Acesso | Moto / Contrato Vinculado | Papel no Sistema |
| :--- | :--- | :--- | :--- |
| **Gestor (Manager)** | `gestor@mottu-ops.local` | Acesso Global a Todas as Unidades | Aprovação de usuários, catálogo de peças Mottu Store e Looker Studio BI |
| **Mecânico (Mechanic)** | `mecanico@mottu-ops.local` | Galpão Butantã (São Paulo) | Fila priorizada (P1-P4), mesa de decisão com zoom e triagem de sinistros |
| **Entregador (Rider)** | `entregador@mottu-ops.local` | Moto `MOT1A23` (*Mottu Sport 110i*, 12h de uso, 1.000 km) | Telemetria da moto, wizard de vistoria, sinistro com vídeo e agendamento |

---

## 📱 Catálogo de Telas do Sistema (`T01` a `T13`)

O sistema segue com fidelidade cirúrgica o arquivo de design `design/mottu-smart-ops.pen` e o Design System **Mottu Asphalt Telemetry** (`DESIGN.md`): fundo asfalto escuro `#0A0E0D`, verde Mottu `#00E676`, contraste solar 11.45:1, botões de 52px+ para operação com luvas e **proibição estrita de cores roxas/violetas** (*Purple Ban*).

### 🚪 Autenticação & Acesso Público
* **`T01` — Login & Solicitação de Cadastro:** Autenticação unificada com JWT. Aba de solicitação de cadastro com validação de CPF e telefone para `RIDER` ou `MECHANIC` (aguardando liberação do Gestor). Suporte a *Modo Demonstração Público (1 Clique)* e indicador de versão `v2.2.0`.

### 🛵 Portal do Entregador (Mobile PWA para Celular)
* **`T02` — Minha Moto (Garagem Digital):** Placa em destaque em tipografia mono 32px, odômetro atual, dias de contrato, status de circulação e histórico de vistorias com travas para no máximo 3 chamados simultâneos.
* **`T03` — Nova Vistoria (Wizard Ergonômico de 3 Passos):** Staging com 4 slots de fotos (Frente, Traseira, Lateral Direita, Lateral Esquerda), pré-validador de luminosidade via canvas (alerta fotos escuras), perguntas objetivas de contexto (`JA_RECEBI_ASSIM`, `USO_NORMAL`, `QUEDA_BATIDA`), toggle de risco de segurança e envio com chave de idempotência.
* **`T04` — Resultado da Vistoria & Agendamento:** Barra de progresso ao vivo via Server-Sent Events (SSE), orçamento transparente discriminando peças e mão de obra, modal de agendamento de galpão (data futura e turno) e botão único de contestação *"Discordo do resultado"* (mínimo 10 caracteres).
* **`T11` — Wizard de Sinistro por Vídeo (`UC-18`):** Gravação de vídeo de até 30s/30MB contornando a moto acidentada, upload direto via presigned URL S3, taxa de guincho obrigatória automática (R$ 80,00) e bloqueio operacional imediato da motocicleta.
* **`T12` — Laudo Preliminar de Sinistro:** Estimativa dinâmica de peças danificadas gerada por IA Multimodal, somatório transparente com mão de obra escalonada e instruções de resgate pelo guincho da filial.

### 🔧 Console da Oficina (Tablet / Desktop do Mecânico)
* **`T05` — Fila Inteligente da Oficina:** Lista ordenada estritamente por criticidade operacional:
  1. 🔴 **P1: Risco de Segurança / Sinistro / Moto Parada** (`#FF4D4F`)
  2. 🟠 **P2: Contestação do Entregador ("Discordo")** (`#FFB300`)
  3. 🟡 **P3: Dúvida da IA (< 80% de Certeza)** (`#FFB300`)
  4. 🔵 **P4: Suspeita de Fraude / Foto Repetida** (`#29B6F6` — *sem roxo*)
  * Exibe taxa de deflexão da IA (78,5% resolvidos sem intervenção humana) e filtros instantâneos.
* **`T06` — Mesa de Decisão do Mecânico:** Layout assimétrico (55% / 45%). Lado esquerdo com foto em alta definição e **lupa reticular central de zoom interativo (2x a 4x)**, caixas delimitadoras da IA (`bounding boxes`), contexto da retirada e alerta de fraude. Lado direito com veredito editável em 1 clique, checklist de peças com recálculo automático de orçamento via Signals e campo de parecer técnico que a IA converte em mensagem empática.
* **`T07` — Inventário e Gestão da Frota:** Painel de motocicletas do galpão por status (`AVAILABLE`, `RENTED`, `IN_MAINTENANCE`), ações rápidas de transição de estado e modal de cadastro de novas motos com validação de placa Mercosul.
* **`T13` — Painel de Triagem Física de Sinistro:** Comparativo lado a lado entre o vídeo gravado na rua e o exame físico no galpão, ajuste de orçamento e aprovação final com despacho analítico para o Google BigQuery.

### 👔 Painel Executivo do Gestor (Desktop)
* **`T08` — Gestão de Acessos & Aprovações:** Fila de cadastros pendentes com distinção visual. Ao aprovar um entregador, exige a seleção obrigatória de uma moto disponível no galpão para emissão automática do contrato de locação. Modal de recusa com justificativa.
* **`T09` — Catálogo Oficial Mottu Store (`loja.mottu.com.br`):** Botão CTA de sincronização em tempo real com a API VTEX da Mottu Store. Configuração da taxa padrão de mão de obra (R$ 35,00), busca textual, filtros por modelo e edição inline de preços com proteção de sobrescrita (`priceOverridden = true`).
* **`T10` — Inteligência Operacional & Looker Studio BI:** 4 cards de telemetria em tempo real (Motos Ativas, % Resolvido pela IA, Fila Atual da Oficina, Horas Economizadas) e **Iframe responsivo de 640px do Google Looker Studio BI** conectado à tabela analítica `fact_inspections` do BigQuery, com fallback offline rico integrado.

---

## 🧪 Qualidade de Código & Bateria de Testes

O desenvolvimento seguiu rigorosamente o **TDD Real (Teste ANTES do Código)** em todas as fases, assegurando estabilidade estrutural em cada módulo:

| Módulo / Camada | Framework de Testes | Quantidade | Cobertura de Domínio | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Core API (Java 21)** | JUnit 5 + Mockito + ArchUnit + TestRestTemplate | **257 testes** | **100% de Linhas (JaCoCo)** | ✅ 100% Passando |
| **Intelligence Service (Python 3.12)** | Pytest + Pytest-Asyncio + Pytest-Cov | **132 testes** | **100% de Linhas (Fail-under)** | ✅ 100% Passando |
| **Web App (Angular 19)** | Vitest + Testing Library + Angular Testbed | **170 testes** | **100% dos Casos Críticos** | ✅ 100% Passando |
| **E2E Playwright Suite** | Playwright Chromium Headless | **148 testes** | **Jornadas Críticas de Ponta a Ponta** | ✅ 100% Passando |
| **Total do Sistema** | **Suíte Automatizada Integrada** | **707 testes** | **Domínio Blindado** | 🚀 **Zero Falhas** |

### Comandos de Verificação Rápida

```bash
# 1. Testar Core API Java (incluindo teste de pureza arquitetural ArchUnit)
cd apps/core-api && ./mvnw clean test

# 2. Testar Intelligence Service Python com cobertura estrita
cd apps/intelligence-service && pytest tests/ -v

# 3. Testar Web App Angular (Vitest)
cd apps/web-app && npm run test:ci

# 4. Executar Testes E2E Integrados Playwright
npx playwright test
```

---

## 📚 Documentação Técnica Aprofundada

Para detalhes de engenharia, contratos fechados, regras de negócio e integrações de dados, consulte a documentação completa no diretório [`docs/`](docs/):

* 📘 [`docs/GUIA_ESTUDO_E_LOOKER.md`](docs/GUIA_ESTUDO_E_LOOKER.md) — **Guia Definitivo de Arquitetura, Estudo Didático para Entrevistas (Staff/Tech Lead) e Receita de Bolo do BigQuery & Looker Studio (Passo a Passo em 5 Minutos)**.
* 📋 [`docs/01-PMD.md`](docs/01-PMD.md) — Product Management Document com regras matemáticas fechadas, fluxos de exceção e matriz de telas.
* 📐 [`docs/02-ARQUITETURA-E-APIS.md`](docs/02-ARQUITETURA-E-APIS.md) — Especificação técnica com contratos JSON de todas as rotas, esquema Flyway SQL, filas Redis e variáveis `.env`.
* 🧪 [`docs/03-CASOS-DE-USO-TDD.md`](docs/03-CASOS-DE-USO-TDD.md) — Especificação dos casos de uso UC-01 a UC-18 com roteiro TDD `RED -> GREEN`.
* 🔑 [`docs/PENDENCIAS_DO_HUMANO.md`](docs/PENDENCIAS_DO_HUMANO.md) — Instruções rápidas (5 minutos) para inclusão opcional da chave real do Gemini e Looker Studio na nuvem.
* 🎨 [`DESIGN.md`](DESIGN.md) & [`design/mottu-smart-ops.pen`](design/mottu-smart-ops.pen) — Tokens de design system e protótipo interativo compatível com Pencil (`pen.dev`).
* 📜 [`docs/HISTORICO_IA.md`](docs/HISTORICO_IA.md) — Diário de bordo *append-only* com todas as entregas cronológicas do projeto.

---

## 📄 Licença

Desenvolvido para o case técnico de seleção operacional da **Mottu**.  
Distribuído sob a licença MIT. Consulte `LICENSE` para mais detalhes.
