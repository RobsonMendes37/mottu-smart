const { chromium } = require('playwright');

const BASE_URL = process.env.BASE_URL || 'http://161.153.2.148';

(async () => {
  console.log(`=======================================================`);
  console.log(`🚀 INICIANDO AUDITORIA COMPLETA DE INTERFACE E FLUXOS`);
  console.log(`   Destino: ${BASE_URL}`);
  console.log(`=======================================================\n`);

  const browser = await chromium.launch({ headless: true });
  let passedCount = 0;
  let failedCount = 0;

  const runTest = async (testName, testFn) => {
    process.stdout.write(`⏳ [TESTE] ${testName}... `);
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await context.newPage();

    page.on('console', msg => {
      const txt = msg.text();
      if (txt.includes('Error') && !txt.includes('favicon')) {
        console.log(`\n   [Console Error]: ${txt}`);
      }
    });

    try {
      await testFn(page, context);
      console.log(`✅ APROVADO`);
      passedCount++;
    } catch (err) {
      console.log(`❌ FALHOU: ${err.message}`);
      failedCount++;
    } finally {
      await context.close();
    }
  };

  try {
    // -------------------------------------------------------------
    // FLUXO 1: GESTOR (MANAGER) — VISTORIAS, BI, WMS, PREVISÃO WHAT-IF
    // -------------------------------------------------------------
    await runTest('Gestor: Login Vitrine e Navegação Inicial', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      const gestorBtn = page.locator('button:has-text("Gestor")').first();
      await gestorBtn.click();
      await page.waitForURL('**/manager/**', { timeout: 8000 });
      if (!page.url().includes('/manager/')) throw new Error(`URL esperada /manager/, recebida ${page.url()}`);
    });

    await runTest('Gestor: T08 Aprovações de Entregadores e Vínculo de Moto', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Gestor")').first().click();
      await page.waitForURL('**/manager/approvals', { timeout: 8000 });

      // Verificar título e tabela de pendentes
      await page.waitForSelector('text=Aprovações de Cadastros', { timeout: 5000 });
      const pendingCards = await page.locator('table, .border').count();
      if (pendingCards === 0) throw new Error('Nenhum elemento de aprovação encontrado na tela T08');
    });

    await runTest('Gestor: T09 Catálogo VTEX com Sincronização e Edição Inline', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Gestor")').first().click();
      await page.waitForURL('**/manager/approvals', { timeout: 8000 });

      // Navegar para o catálogo
      await page.click('a[href="/manager/catalog"]');
      await page.waitForURL('**/manager/catalog', { timeout: 5000 });
      await page.waitForSelector('text=Catálogo de Peças', { timeout: 5000 });

      // Verificar botão de sync com a Mottu Store
      const syncBtn = page.locator('button:has-text("Sincronizar com Mottu Store")');
      await syncBtn.waitFor({ state: 'visible', timeout: 5000 });
    });

    await runTest('Gestor: T12 Faturamento Semanal e Conciliação Pix em 1 Clique', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Gestor")').first().click();
      await page.waitForURL('**/manager/approvals', { timeout: 8000 });

      // Navegar para faturamento
      await page.click('a[href="/manager/billing"]');
      await page.waitForURL('**/manager/billing', { timeout: 5000 });
      await page.waitForSelector('text=Faturamento e Cobrança', { timeout: 5000 });

      // Verificar presença de contratos
      const contracts = await page.locator('tbody tr').count();
      if (contracts === 0) throw new Error('Nenhum contrato listado na tabela de faturamento');
    });

    await runTest('Gestor: T10 Analytics — Busca Operacional de Dados com Chips e Export', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Gestor")').first().click();
      await page.waitForURL('**/manager/**', { timeout: 8000 });

      // Navegar para Analytics
      await page.click('a[href="/manager/analytics"]');
      await page.waitForURL('**/manager/analytics', { timeout: 5000 });

      // Testar chip rápido "Peças críticas"
      const chip = page.locator('button:has-text("Peças críticas")').first();
      await chip.click();
      await page.waitForTimeout(1000);

      // Verificar resposta da busca
      const directAnswer = page.locator('text=Diagnóstico Operacional');
      await directAnswer.waitFor({ state: 'visible', timeout: 5000 });

      // Verificar botões de exportação
      const copyBtn = page.locator('button:has-text("Copiar Resumo")');
      await copyBtn.waitFor({ state: 'visible', timeout: 5000 });
      const exportBtn = page.locator('button:has-text("Exportar CSV")');
      await exportBtn.waitFor({ state: 'visible', timeout: 5000 });
    });

    await runTest('Gestor: T10 Analytics — Nova Barra Preditiva e Simulação What-If de Cenários', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Gestor")').first().click();
      await page.waitForURL('**/manager/approvals', { timeout: 8000 });

      await page.click('a[href="/manager/analytics"]');
      await page.waitForURL('**/manager/analytics', { timeout: 5000 });

      // Localizar seção de previsão de demanda
      const section = page.locator('#ai-advisor-section');
      await section.scrollIntoViewIfNeeded();

      // Testar mudança de horizonte: clicar em "15 dias"
      const h15Btn = section.locator('button:has-text("15 dias")');
      await h15Btn.click();
      await page.waitForTimeout(500);

      // Testar filtro de sistema: clicar em "Freio e Guidão"
      const brakeBtn = section.locator('button:has-text("Freio e Guidão")');
      await brakeBtn.click();
      await page.waitForTimeout(500);

      // Testar simulação de estresse: clicar no card "Pico de Chuva e Quedas"
      const rainCard = section.locator('text=Pico de Chuva e Quedas');
      await rainCard.click();
      await page.waitForTimeout(500);

      // Verificar badge de estresse ativo
      const badge = section.locator('text=+35% Estresse');
      await badge.waitFor({ state: 'visible', timeout: 3000 });

      // Verificar tabela de alertas com colunas de preço unitário e orçamento
      const table = section.locator('table');
      await table.waitFor({ state: 'visible', timeout: 3000 });
    });

    // -------------------------------------------------------------
    // FLUXO 2: MECÂNICO (MECHANIC) — FILA, BANCADA, WMS DE OFICINA
    // -------------------------------------------------------------
    await runTest('Mecânico: Login e Fila de Triagem Priorizada (T05)', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Mecânico")').first().click();
      await page.waitForURL('**/mechanic/queue', { timeout: 8000 });

      // Verificar cards da fila
      await page.waitForSelector('text=Fila de Triagem da Oficina', { timeout: 5000 });
      const queueCards = await page.locator('tbody tr, .cursor-pointer').count();
      if (queueCards === 0) throw new Error('Nenhum item encontrado na fila do mecânico');
    });

    await runTest('Mecânico: Bancada de Análise com Lupa Interativa e Foto Real (T06)', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Mecânico")').first().click();
      await page.waitForURL('**/mechanic/queue', { timeout: 8000 });

      // Clicar na primeira vistoria da fila
      const firstItem = page.locator('tbody tr, .cursor-pointer').first();
      await firstItem.click();
      await page.waitForURL('**/mechanic/workbench/**', { timeout: 6000 });

      // Verificar foto de alta resolução e lupa óptica
      await page.waitForSelector('img[alt="Foto da avaria da moto"]', { timeout: 6000 });
      const zoomButtons = await page.locator('button:has-text("2x"), button:has-text("3x"), button:has-text("4x")').count();
      if (zoomButtons === 0) throw new Error('Controles de zoom da lupa não encontrados na T06');
    });

    await runTest('Mecânico: WMS de Oficina — Ordens de Serviço nas Baias e Almoxarifado', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Mecânico")').first().click();
      await page.waitForURL('**/mechanic/queue', { timeout: 8000 });

      // Navegar para WMS de operações
      await page.click('a[href="/mechanic/operations"]');
      await page.waitForURL('**/mechanic/operations', { timeout: 5000 });
      await page.waitForSelector('text=Operações de Oficina & Almoxarifado WMS', { timeout: 5000 });

      // Verificar baias
      const bays = await page.locator('text=Aguardando Baia, text=Em Reparo na Baia, text=Pronta p/ Retirada').count();
      if (bays === 0) throw new Error('Quadro de baias de oficina não encontrado');

      // Verificar almoxarifado com corredores e gavetas
      const bins = await page.locator('text=Corredor').count();
      if (bins === 0) throw new Error('Nenhuma gaveta física de almoxarifado encontrada');
    });

    await runTest('Mecânico: Gestão da Frota de Motos do Pátio (T07)', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Mecânico")').first().click();
      await page.waitForURL('**/mechanic/queue', { timeout: 8000 });

      // Navegar para Frota
      await page.click('a[href="/mechanic/fleet"]');
      await page.waitForURL('**/mechanic/fleet', { timeout: 5000 });
      await page.waitForSelector('text=Frota do Pátio', { timeout: 5000 });

      const motoRows = await page.locator('tbody tr').count();
      if (motoRows === 0) throw new Error('Nenhuma moto listada na frota');
    });

    // -------------------------------------------------------------
    // FLUXO 3: ENTREGADOR (RIDER) — GARAGEM, LOCAÇÃO, FATURA E ENTREGAS
    // -------------------------------------------------------------
    await runTest('Entregador: Garagem Mobile e Dados da Moto Ativa (T02)', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Entregador")').first().click();
      await page.waitForURL('**/rider/garage', { timeout: 8000 });

      // Verificar placa ativa e ações rápidas
      await page.waitForSelector('text=MOT1A23', { timeout: 5000 });
      await page.waitForSelector('text=Iniciar Vistoria', { timeout: 5000 });
    });

    await runTest('Entregador: Fatura Semanal Pix e Simulador de Entregas com Bônus', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Entregador")').first().click();
      await page.waitForURL('**/rider/garage', { timeout: 8000 });

      // Navegar para Aluguel e Fatura
      await page.click('a[href="/rider/rental"]');
      await page.waitForURL('**/rider/rental', { timeout: 5000 });
      await page.waitForSelector('text=Minha Locação e Fatura', { timeout: 5000 });

      // Verificar tabela de entregas diárias
      const deliveryRows = await page.locator('tbody tr, .text-xs').count();
      if (deliveryRows === 0) throw new Error('Tabela de entregas diárias não exibida');

      // Testar botão de registrar entregas hoje
      const addDeliveryBtn = page.locator('button:has-text("Registrar 5 Entregas Hoje")');
      if (await addDeliveryBtn.isVisible()) {
        await addDeliveryBtn.click();
        await page.waitForTimeout(1000);
      }
    });

    await runTest('Entregador: Perfil Pessoal e Auditoria de Segurança', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Entregador")').first().click();
      await page.waitForURL('**/rider/garage', { timeout: 8000 });

      // Navegar para Perfil
      await page.click('a[href="/rider/account"]');
      await page.waitForURL('**/rider/account', { timeout: 5000 });
      await page.waitForSelector('text=Meu Perfil e Dados Cadastrais', { timeout: 5000 });
      await page.waitForSelector('input[name="fullName"], input[type="text"]', { timeout: 5000 });
    });

    // -------------------------------------------------------------
    // FLUXO 4: CHAVEAMENTO VIDA REAL (PERFIS OPERACIONAIS DO DIA A DIA)
    // -------------------------------------------------------------
    await runTest('Vida Real: Login com Entregador da Vida Real (Carlos Silva)', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      const realRiderBtn = page.locator('button:has-text("carlos.entregador@mottu.com.br")').first();
      await realRiderBtn.click();
      await page.waitForURL('**/rider/**', { timeout: 8000 });
      if (!page.url().includes('/rider/')) throw new Error('Falha no login do perfil vida real');
    });

    await runTest('Vida Real: Login com Gestor da Vida Real (Diego Gestor)', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      const realManagerBtn = page.locator('button:has-text("diego.gestor@mottu.com.br")').first();
      await realManagerBtn.click();
      await page.waitForURL('**/manager/**', { timeout: 8000 });
      if (!page.url().includes('/manager/')) throw new Error('Falha no login do gestor vida real');
    });

  } finally {
    await browser.close();
  }

  console.log(`\n=======================================================`);
  console.log(`📊 RESULTADO DA AUDITORIA DE INTERFACE E FLUXOS:`);
  console.log(`   Total de Testes: ${passedCount + failedCount}`);
  console.log(`   Sucessos:        ${passedCount} ✅`);
  console.log(`   Falhas:          ${failedCount} ❌`);
  console.log(`=======================================================`);

  if (failedCount > 0) {
    process.exit(1);
  }
})();
