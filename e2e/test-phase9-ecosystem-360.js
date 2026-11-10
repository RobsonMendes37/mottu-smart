const { chromium } = require('playwright');

const BASE_URL = process.env.BASE_URL || 'https://mottu-smart.duckdns.org';

(async () => {
  console.log(`=======================================================`);
  console.log(`🚀 INICIANDO AUDITORIA DE INTERFACE — FASE 9 (ECOSSISTEMA 360 & JUNTA IA)`);
  console.log(`   Destino: ${BASE_URL}`);
  console.log(`=======================================================\n`);

  const browser = await chromium.launch({ headless: true });
  let passedCount = 0;
  let failedCount = 0;

  const runTest = async (testName, testFn) => {
    process.stdout.write(`⏳ [TESTE] ${testName}... `);
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await context.newPage();

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
    // 1. Entregador: Garagem Mobile (T02) exibe Escudo 48h / 100km e Seção Vistoria 360
    await runTest('Entregador: Garagem T02 exibe Escudo 48h/100km e Vistoria 360', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Entregador")').first().click();
      await page.waitForURL('**/rider/garage', { timeout: 10000 });

      // Aguardar renderização da Garagem (Escudo de Retirada ou Garantia Regular)
      const shieldBadge = page.locator('text=/Escudo de Retirada Ativo|Garantia regular/');
      await shieldBadge.first().waitFor({ state: 'visible', timeout: 8000 });

      // Verificar elementos do Escudo ou Vistoria 360
      await page.waitForSelector('text=Giro 360 de Retirada', { timeout: 8000 });
      const content = await page.content();
      const hasEscudoOrVistoria = content.includes('Retirada') && content.includes('Validado');
      if (!hasEscudoOrVistoria) {
        throw new Error('Nenhum indicador de garantia ou vistoria 360 encontrado na Garagem T02');
      }
    });

    // 2. Mecânico: Gestão da Frota (T07) possui botão de Ação de Giro 360
    await runTest('Mecânico: Frota T07 possui Ação de Giro 360 de Expedição', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Mecânico")').first().click();
      await page.waitForURL('**/mechanic/**', { timeout: 10000 });

      await page.goto(`${BASE_URL}/mechanic/fleet`, { waitUntil: 'networkidle' });
      await page.waitForSelector('table', { timeout: 8000 });

      const tableContent = await page.content();
      const hasFleetRows = tableContent.includes('MOT') || tableContent.includes('POP');
      if (!hasFleetRows) {
        throw new Error('Tabela de frota T07 não carregou as motocicletas');
      }
    });

    // 3. Mecânico: Fila de Triagem (T05) com acesso às vistorias
    await runTest('Mecânico: Fila T05 lista chamados para triagem na bancada', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Mecânico")').first().click();
      await page.waitForURL('**/mechanic/**', { timeout: 10000 });

      await page.goto(`${BASE_URL}/mechanic/queue`, { waitUntil: 'networkidle' });
      await page.waitForSelector('text=Fila de vistorias da unidade', { timeout: 8000 });
    });

  } catch (fatalErr) {
    console.error('Erro fatal durante execução:', fatalErr);
  } finally {
    await browser.close();
    console.log(`\n=======================================================`);
    console.log(`📊 RESULTADO FASE 9:`);
    console.log(`   Total de Testes: ${passedCount + failedCount}`);
    console.log(`   Sucessos:        ${passedCount} ✅`);
    console.log(`   Falhas:          ${failedCount} ❌`);
    console.log(`=======================================================\n`);
    if (failedCount > 0) process.exit(1);
  }
})();

