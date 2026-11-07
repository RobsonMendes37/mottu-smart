# Checklist do Humano — O Que Só Você Precisa Preencher Amanhã (5 Minutos)

> **Como funciona o Modo Autônomo Noturno:**  
> A IA constrói **100% do sistema** sozinha (Banco de Dados, API Java Hexagonal, Serviço Python com código oficial do Gemini/CRAG/VTEX, Telas Angular PWA, Docker e Testes Automatizados) usando o **Modo Simulado Offline (`GEMINI_MOCK_MODE=true` e `BIGQUERY_ENABLED=false`)** para testar tudo sem gastar nada e sem travar de madrugada esperando senha sua.
>
> Quando você voltar amanhã e o código estiver pronto, basta preencher apenas os **3 itens abaixo** no arquivo `.env` para ligar os serviços externos reais!

---

## 1. Chave da Inteligência Artificial do Google (`GEMINI_API_KEY`)
* **Por que a IA não criou sozinha?** A chave é vinculada à sua conta pessoal do Google.
* **Como pegar de graça (1 minuto):**
  1. Acesse https://aistudio.google.com/app/apikey com sua conta Google;
  2. Clique em **Create API Key** e copie o código gerado (`AIzaSy...`).
* **Onde colar amanhã no `.env`:**
  ```ini
  GEMINI_API_KEY=cole_sua_chave_aqui
  GEMINI_MOCK_MODE=false
  ```

---

## 2. Google BigQuery + Painel Looker Studio (Opcional no Dia 1, Grátis no Modo Sandbox)
* **Por que a IA não criou sozinha?** Precisa estar logado no seu Google Cloud / Looker Studio no navegador.
* **O que já estará 100% pronto no código:** A tabela `analytics_outbox`, o exportador em lote (`Batch Load NDJSON` — compatível com a conta gratuita sem cartão!) e o espaço do gráfico na tela `T10` + o passo a passo completo em `docs/GUIA_ESTUDO_E_LOOKER.md`.
* **Onde colar amanhã no `.env` (depois de seguir o guia de 5 min):**
  ```ini
  BIGQUERY_ENABLED=true
  GCP_PROJECT_ID=id-do-seu-projeto-google
  GOOGLE_APPLICATION_CREDENTIALS=/secrets/gcp-bigquery-key.json
  LOOKER_STUDIO_EMBED_URL=https://lookerstudio.google.com/embed/reporting/seu-link
  ```

---

## 3. Revisão Visual no `pen.dev` e Subida na VM Oracle (12 GB)
* **Ajustar telas (se quiser):** Abra o arquivo [`design/mottu-smart-ops.pen`](file:///home/robson/Projetos_Desenvolvimento/mattu-projeto-selecao/design/mottu-smart-ops.pen) com a extensão **Pencil (`pen.dev`)**. Se você mudar qualquer cor, texto ou posição lá dentro amanhã, basta mandar no chat: *"Atualize as telas do Angular conforme minhas edições no `.pen`"*.
* **Domínio HTTPS na sua VM Oracle:** No arquivo `infra/Caddyfile`, troque `localhost` pelo seu domínio/subdomínio quando for subir na nuvem e troque `JWT_SECRET` / senhas do `.env` por senhas fortes de produção.

---

## 4. GitHub Secrets para CI/CD Automático (GitHub Actions v3.0)
* **Onde configurar:** No seu repositório no GitHub: `Settings` → `Secrets and variables` → `Actions` → `New repository secret`.
* **Secrets Opcionais para Deploy Automático na VM Oracle (`cd.yml`):**
  * `SSH_HOST`: IP público ou domínio da sua VM Oracle.
  * `SSH_USER`: Usuário SSH da máquina (ex: `ubuntu` ou `opc`).
  * `SSH_PRIVATE_KEY`: Sua chave privada SSH para acesso à VM.
  * `PROD_ENV_FILE`: Conteúdo completo do seu `.env` de produção com senhas reais e `GEMINI_API_KEY`.
* *(Nota: O workflow de CI `ci.yml` não depende de nenhuma secret e roda 100% dos testes offline com sucesso imediato em qualquer PR).*


