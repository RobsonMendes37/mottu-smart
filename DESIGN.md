---
version: "2.0"
name: Mottu Clean Fleet (Uber Base Web Inspired)
description: Sistema visual diurno de alto contraste inspirado no Uber Base Web (Uber Fleet / Uber for Business), desenhado para leitura confortável sem fadiga visual sob o sol na rua, no celular do mecânico pelo pátio e no painel corporativo do gestor.
colors:
  primary: "#05944F"
  primary-hover: "#047840"
  on-primary: "#FFFFFF"
  primary-tint: "#E6F4EE"
  primary-ink: "#046C3A"
  accent-black: "#141414"
  on-accent-black: "#FFFFFF"
  background: "#F6F6F6"
  surface: "#FFFFFF"
  surface-subtle: "#F3F3F3"
  border: "#E2E2E2"
  border-light: "#EEEEEE"
  on-surface: "#141414"
  text-secondary: "#545454"
  text-muted: "#757575"
  warning: "#D97706"
  warning-tint: "#FEF3C7"
  warning-ink: "#92400E"
  error: "#DC2626"
  error-tint: "#FEE2E2"
  error-ink: "#B91C1C"
  info-tint: "#EEF2F6"
  info-ink: "#334155"
typography:
  headline-display:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: 700
    lineHeight: 1.25
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: 700
    lineHeight: 1.3
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 15px
    fontWeight: 500
    lineHeight: 1.5
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 13px
    fontWeight: 400
    lineHeight: 1.5
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: 600
    lineHeight: 1.2
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: 600
    lineHeight: 1.2
  mono-telemetry:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: 600
    lineHeight: 1.3
rounded:
  none: 0px
  sm: 6px
  md: 8px
  lg: 12px
  full: 9999px
spacing:
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  touch: 52px
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.md}"
    height: "{spacing.touch}"
    padding: 16px
  button-dark:
    backgroundColor: "{colors.accent-black}"
    textColor: "{colors.on-accent-black}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.md}"
    height: "{spacing.touch}"
    padding: 16px
  button-secondary:
    backgroundColor: "{colors.surface-subtle}"
    textColor: "{colors.on-surface}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.md}"
    height: "{spacing.touch}"
    padding: 16px
  card-surface:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.lg}"
    padding: 20px
---

# Mottu Clean Fleet (Uber Base Web) — Design System

## Overview
Identidade visual inspirada diretamente no **Uber Base Web (`Uber Fleet` / `Uber for Business`)** combinada com o verde operacional da Mottu (`#05944F`). Elimina fundos pretos com verde neon saturado e alertas gritantes que causam fadiga ocular, adotando **polaridade positiva diurna** (`#F6F6F6` + `#FFFFFF` + tinta `#141414`), escrita humana natural em português (*sentence case*, sem jargões robóticos em CAIXA ALTA) e hierarquia calma de produto BigTech real.

> **Nota sobre visor de câmera (`T03`):** Apenas o visor fotográfico ativo (`viewfinder`) usa fundo grafite neutro (`#1C1C1C`) para enquadramento real da lente; todo o cabeçalho, instruções e botão inferior permanecem claros e confortáveis.

## Colors
- **Neutral Foundation (`#F6F6F6` / `#FFFFFF` / `#F3F3F3`):** Fundo cinza alabastro suave (`#F6F6F6`) para destacar cartões e tabelas brancas puras (`#FFFFFF`), com campos de formulário e pílulas inativas em cinza sutil (`#F3F3F3`) e bordas discretas (`#E2E2E2`).
- **Typography Ink (`#141414` / `#545454` / `#757575`):** Preto carvão suave (`#141414`) para títulos, valores e filtros selecionados; cinza médio (`#545454`) para textos de apoio; cinza neutro (`#757575`) para legendas e metadados.
- **Mottu Operational Green (`#05944F` / `#E6F4EE` / `#046C3A`):** Verde esmeralda equilibrado (padrão Uber Eats / Uber Green) reservado para **uma única ação principal por tela** (ex.: *Iniciar nova vistoria*, *Concluir laudo*, *Novo usuário*), barras positivas de indicadores e selos de isenção de custo (`#E6F4EE` com texto `#046C3A`).
- **Soft Semantic Tints (Sem cores vivas gritantes):**
  - **Atenção / Pendente:** Fundo âmbar pastel (`#FEF3C7`) com texto terroso (`#92400E`) ou ponto discreto de `8px` (`#D97706`).
  - **Urgente / Moto parada:** Fundo vermelho pastel (`#FEE2E2`) com texto carmim escuro (`#B91C1C`) ou ponto discreto de `8px` (`#DC2626`).
  - **Informativo:** Fundo ardósia claro (`#EEF2F6`) com texto (`#334155`).

## Typography
- **`Plus Jakarta Sans`:** Fonte principal limpa e geométrica. Todos os rótulos, cabeçalhos de tabela, botões e abas usam **capitalização natural de frase** (*Sentence case*, ex.: *"Catálogo de peças"*, *"Situação da conta"* — nunca `"CATÁLOGO DE PEÇAS AI"`).
- **`JetBrains Mono` (`mono-telemetry`):** Reservada exclusivamente para dados tabulares e operacionais que exigem leitura caractere a caractere: **placas Mercosul (`ABC1D23`)**, **códigos SKU (`MOT-110-MFD`)**, **contratos (`#CT-88412`)**, **quilometragem** e **valores monetários**.

## Layout
- **App Shell Completo Obrigatório em Todas as Telas:**
  - **Mobile (`390×844` — Entregador e Mecânico no Pátio):** Barra de status (`36px`) + **Header fixo (`56px`, `#FFFFFF` com borda inferior `#EEEEEE`)** + Corpo rolável em `#F6F6F6` + **Bottom Tab Bar (`74px`, `#FFFFFF`)** com até 5 abas na zona do polegar (ou rodapé institucional no Login e botão primário fixo na Câmera).
  - **Web Desktop (`1440×900` — Oficina e Gestão):** **Sidebar esquerda (`240px`, `#FFFFFF`)** com seletor de unidade, grupos de navegação claros (*Vistorias e oficina*, *Cadastros*, *Operação da unidade*) e cartão do usuário logado no rodapé + **Top Header (`60px`, `#FFFFFF`)** com *breadcrumbs*, busca global, indicador discreto de sincronização da loja Mottu, notificações e botão *Minha conta* + Área de trabalho fluida (`#F6F6F6`) + **Global Footer (`40px`, `#FFFFFF`)** com unidade, conformidade LGPD e disponibilidade.
- **Mecânico Híbrido (Celular no Pátio + Bancada Web):**
  - **Celular (`T05-Mobile`, `T06-Mobile`, `T07-Mobile`):** Pensado para o mecânico caminhando entre as baias com leitor QR de placa, foto com zoom `4x` por pinça, *Bottom Sheet* rápido de parecer e consulta de ordens de serviço/prateleiras de estoque.
  - **Web (`T05-Web`, `T06-Web`):** Fila em tabela limpa e bancada dividida (`55%` visor fotográfico com lupa `4x` + `45%` decisão, peças e mensagem ao entregador).
- **CRUDs Padronizados Fluidos + Gaveta Lateral (`T08`, `T09`, `T07-Web`):**
  - *Usuários e permissões* (`T08`), *Catálogo de peças* (`T09`) e *Frota do galpão* (`T07-Web`) seguem exatamente o **mesmo padrão de Data Table sem quebra de layout**: cartões de resumo no topo, barra de filtros em pílula (ativa em `#141414`), filtro rápido inline, colunas proporcionais com pontos discretos de `8px` para situação e paginação inferior, combinados com **gaveta lateral direita (`400px`, *Slide-over Drawer*)** para criar ou editar registros sem perder o contexto da lista.
- **Auto-Edição de Conta & Módulos Simulados do Ecossistema:**
  - Qualquer usuário edita seus próprios dados pessoais, unidade, chave Pix e senha em *Meus dados* (`T04B` Mobile) e *Minha conta* (`T11` Web).
  - Módulos complementares navegáveis simulam o ecossistema completo da Mottu: *Aluguel e ganhos* (`T04C`), *Pátio, ordens de serviço e estoque* (`T07-Mobile`) e *Contratos e faturamento semanal* (`T12`).

## Elevation & Depth
Superfícies planas e limpas delimitadas por bordas sutis de `1px` (`#E2E2E2` / `#EEEEEE`) sobre o canvas `#F6F6F6`. Sombras externas suaves (`#00000014`, `blur: 24px`) são usadas **apenas** em camadas sobrepostas reais, como a gaveta lateral de edição (`400px`) e o *Bottom Sheet* mobile.

## Shapes
Geometria contida estilo Uber Base Web: cartões e tabelas usam `12px` (`lg`), botões e campos de entrada usam `8px` (`md` — com foco em borda `1.5px solid #141414`), e filtros rápidos / selos usam pílula (`9999px`).

## Components
- **Botões:** No celular, ações principais têm `52px` de altura (`#05944F` ou `#141414` com texto `#FFFFFF`). Na Web, botões de barra superior usam `38px` e botões dentro de linhas de tabela usam `28px` com borda `#E2E2E2`.
- **Campos de Formulário e Gavetas (`Drawer`):** Fundo `#F3F3F3` em repouso e fundo `#FFFFFF` com borda `#141414` (`1.5px`) quando em foco, sempre com rótulo visível acima do campo.
- **Indicadores em Tabelas:** Priorizar **ponto colorido de `8px` + texto escuro (`#141414`)** em vez de dezenas de etiquetas coloridas piscando na mesma grade.

## Do's and Don'ts
- **DO:** Use capitalização natural de frase (*Sentence case*) e termos simples do dia a dia da operação (*"Moto parada"*, *"Desgaste natural"*, *"Aguardando vínculo"*).
- **DO:** Reserve o verde `#05944F` para 1 ação principal por tela e use `#141414` para abas/filtros selecionados.
- **DO:** Mantenha placas, códigos de peça e valores alinhados com `JetBrains Mono`.
- **DON'T:** **Proibido usar qualquer tom de roxo, violeta, índigo ou magenta** (`Purple Ban` absoluto).
- **DON'T:** Proibido usar verde neon `#00FF00`/`#00E676` sobre preto puro ou amarelo/vermelho fluorescentes em áreas grandes.
- **DON'T:** Nunca use textos técnicos de IA na interface (como `"CRAG ENGINE"`, `"GEMINI CONFIDENCE 94%"`, `"VTEX SYNC OK"`).
