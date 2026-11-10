const { chromium } = require('playwright');

const BASE_URL = process.env.BASE_URL || 'https://mottu-smart.duckdns.org';

(async () => {
  console.log(`======================================================================`);
  console.log(`🚀 AUDITORIA E2E COMPLETA DE INTERFACE — FASE 9 (ECOSSISTEMA 360 & JUNTA IA)`);
  console.log(`   Destino: ${BASE_URL}`);
  console.log(`======================================================================\n`);

  const browser = await chromium.launch({ headless: true });
  let passedCount = 0;
  let failedCount = 0;

  const runTest = async (testName, testFn) => {
    process.stdout.write(`⏳ [TESTE E2E] ${testName}... `);
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
    // -----------------------------------------------------------------------------------
    // TESTE 1: T07 FROTA — MODAL DE GIRO 360 DE EXPEDIÇÃO (ESTADO ZERO)
    // -----------------------------------------------------------------------------------
    await runTest('T07 Frota: Abrir Modal de Giro 360, Preencher Fotos e Salvar Expedição', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Mecânico")').first().click();
      await page.waitForURL('**/mechanic/**', { timeout: 10000 });

      await page.goto(`${BASE_URL}/mechanic/fleet`, { waitUntil: 'networkidle' });
      await page.waitForSelector('text=Frota de motocicletas da unidade', { timeout: 8000 });

      // Localizar o primeiro botão de "Giro 360"
      const giroBtn = page.locator('button:has-text("📸 Giro 360")').first();
      await giroBtn.waitFor({ state: 'visible', timeout: 8000 });
      await giroBtn.click();

      // Verificar abertura do modal com os 5 campos de fotos e odômetro
      await page.waitForSelector('text=Giro 360 de Expedição (Estado Zero)', { timeout: 6000 });
      await page.waitForSelector('#baseFront', { timeout: 4000 });
      await page.waitForSelector('#baseRear', { timeout: 4000 });
      await page.waitForSelector('#baseRight', { timeout: 4000 });
      await page.waitForSelector('#baseLeft', { timeout: 4000 });
      await page.waitForSelector('#baseOdo', { timeout: 4000 });
      await page.waitForSelector('#baseOdoKm', { timeout: 4000 });

      // Alterar odômetro inicial
      await page.fill('#baseOdoKm', '1250');

      // Clicar em "Salvar Giro 360 de Expedição"
      const saveBtn = page.locator('button:has-text("Salvar Giro 360 de Expedição")');
      await saveBtn.click();

      // Verificar fechamento do modal e feedback de sucesso
      await page.waitForSelector('text=Giro 360 de Expedição gravado com sucesso', { timeout: 8000 });
    });

    // -----------------------------------------------------------------------------------
    // TESTE 2: T02 GARAGEM — ESCUDO 48H/100KM & DUPLA VALIDAÇÃO DA RETIRADA PELO ENTREGADOR
    // -----------------------------------------------------------------------------------
    await runTest('T02 Garagem: Escudo 48h/100km e Dupla Validação Digital de Retirada', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Entregador")').first().click();
      await page.waitForURL('**/rider/garage', { timeout: 10000 });

      // Verificar status de cobertura (Escudo de Retirada 48h ou Garantia regular da oficina)
      const shieldBadge = page.locator('text=Escudo de Retirada Ativo, text=Garantia regular');
      await shieldBadge.first().waitFor({ state: 'visible', timeout: 8000 });

      // Verificar card "Vistoria 360 de Retirada & Dupla Validação"
      await page.waitForSelector('text=Vistoria 360 de Retirada & Dupla Validação', { timeout: 8000 });

      // Verificar galeria de ângulos
      await page.waitForSelector('text=1. Frente', { timeout: 4000 });
      await page.waitForSelector('text=2. Traseira', { timeout: 4000 });
      await page.waitForSelector('text=3. Lateral Dir.', { timeout: 4000 });
      await page.waitForSelector('text=4. Lateral Esq.', { timeout: 4000 });

      // Verificar botão de confirmação digital ou badge de já validado
      const validateBtn = page.locator('button:has-text("Confirmar e Validar Vistoria de Retirada")');
      const alreadyValidated = page.locator('text=Vistoria de Retirada Validada Digitalmente, text=Validado por você');

      if (await validateBtn.isVisible()) {
        await validateBtn.click();
        await page.waitForTimeout(1000);
        // Após clicar, deve transicionar para validado
        await alreadyValidated.first().waitFor({ state: 'visible', timeout: 8000 });
      } else {
        await alreadyValidated.first().waitFor({ state: 'visible', timeout: 5000 });
      }
    });

    // -----------------------------------------------------------------------------------
    // TESTE 3: T06 BANCADA — ABAS DE FOTO COMPARATIVA 360 & JUNTA TÉCNICA MULTI-IA
    // -----------------------------------------------------------------------------------
    await runTest('T06 Bancada: Abas de Giro 360 da Retirada e Placar da Junta Técnica', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Mecânico")').first().click();
      await page.waitForURL('**/mechanic/queue', { timeout: 10000 });

      // Entrar na bancada da primeira vistoria disponível
      const actionBtn = page.locator('tbody tr td button').first();
      await actionBtn.click();
      await page.waitForURL('**/mechanic/workbench/**', { timeout: 10000 });

      // Verificar as abas de fotos
      const currentTab = page.locator('button:has-text("Foto Atual da Vistoria")');
      const baselineTab = page.locator('button:has-text("Giro 360 da Retirada")');
      await currentTab.waitFor({ state: 'visible', timeout: 6000 });
      await baselineTab.waitFor({ state: 'visible', timeout: 6000 });

      // Clicar na aba Giro 360 da Retirada (Estado Zero)
      await baselineTab.click();
      await page.waitForTimeout(500);

      // Se houver fotos 360, testar botões de ângulos
      const angleFront = page.locator('button:has-text("Frente")');
      const angleRear = page.locator('button:has-text("Traseira")');
      const angleOdometer = page.locator('button:has-text("Odômetro")');

      if (await angleFront.isVisible()) {
        await angleRear.click();
        await page.waitForTimeout(300);
        await angleOdometer.click();
        await page.waitForTimeout(300);
        await angleFront.click();
      }

      // Voltar para Foto Atual
      await currentTab.click();
      await page.waitForTimeout(300);

      // Verificar se a seção da Junta Técnica ou Resumo Preliminar está presente
      const pageText = await page.content();
      const hasCommitteeOrSummary = pageText.includes('Junta Técnica') || pageText.includes('Observação técnica') || pageText.includes('Inspeção óptica');
      if (!hasCommitteeOrSummary) {
        throw new Error('Seções analíticas da bancada não foram renderizadas');
      }
    });

    // -----------------------------------------------------------------------------------
    // TESTE 4: T06 BANCADA — BOTÃO DE AÇÃO: CONVOCAÇÃO PRESENCIAL (R$ 0,00)
    // -----------------------------------------------------------------------------------
    await runTest('T06 Bancada: Acionar Convocação Presencial no Galpão com Isenção (R$ 0,00)', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Mecânico")').first().click();
      await page.waitForURL('**/mechanic/queue', { timeout: 10000 });

      const actionBtn = page.locator('tbody tr td button').first();
      await actionBtn.click();
      await page.waitForURL('**/mechanic/workbench/**', { timeout: 10000 });

      // Localizar o 4º botão de ação
      const convocarBtn = page.locator('button:has-text("Convocar para Diagnóstico Presencial no Galpão")');
      await convocarBtn.waitFor({ state: 'visible', timeout: 6000 });

      if (await convocarBtn.isEnabled()) {
        await convocarBtn.click();

        // Verificar abertura do modal de convocação presencial
        await page.waitForSelector('text=Convocação Presencial no Galpão', { timeout: 5000 });
        await page.waitForSelector('text=Garantia de Isenção para o Entregador Parceiro (R$ 0,00)', { timeout: 5000 });

        // Preencher motivo técnico
        await page.fill('#presentialNotes', 'Verificação estrutural na bancada mecânica Mottu sem ônus.');

        // Confirmar convocação
        const confirmBtn = page.locator('button:has-text("Confirmar Convocação (R$ 0,00)")');
        await confirmBtn.click();

        // Verificar banner de laudo emitido com isenção
        await page.waitForSelector('text=Laudo Técnico Emitido com Sucesso', { timeout: 10000 });
        await page.waitForSelector('text=Desgaste Natural (Isento • R$ 0,00)', { timeout: 8000 });
      } else {
        console.log('   (Vistoria já estava resolvida; botão desabilitado conforme regras)');
      }
    });

    // -----------------------------------------------------------------------------------
    // TESTE 5: T04 STATUS — VISÃO DO ENTREGADOR COM CONVOCAÇÃO PRESENCIAL E JUNTA IA
    // -----------------------------------------------------------------------------------
    await runTest('T04 Status: Banner de Convocação Presencial e Placar da Junta Técnica Multi-IA', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Entregador")').first().click();
      await page.waitForURL('**/rider/garage', { timeout: 10000 });

      // Clicar em "Ver resultado" na primeira vistoria recente
      const resultLink = page.locator('a:has-text("Ver resultado")').first();
      if (await resultLink.isVisible()) {
        await resultLink.click();
        await page.waitForURL('**/rider/inspections/**/status', { timeout: 8000 });

        // Verificar tela T04
        await page.waitForSelector('text=Resultado da vistoria', { timeout: 8000 });

        // Verificar se tem valor calculado
        await page.waitForSelector('text=R$', { timeout: 8000 });

        // Verificar presença de elementos de proteção, comitê ou parecer
        const html = await page.content();
        const hasTechnicalDetails = html.includes('Junta Técnica') || html.includes('Liberado sem custo') || html.includes('Orçamento') || html.includes('Convocação');
        if (!hasTechnicalDetails) {
          throw new Error('Nenhum detalhe de deliberação técnica encontrado na T04');
        }
      } else {
        console.log('   (Nenhuma vistoria recente vinculada ao entregador de teste)');
      }
    });

    // -----------------------------------------------------------------------------------
    // TESTE 6: WMS BAIAS — CONFIRMAÇÃO DE ORDEM DE SERVIÇO GERADA EM BAIAS DA OFICINA
    // -----------------------------------------------------------------------------------
    await runTest('WMS Oficina: Baias Operacionais com Ordens de Serviço Ativas', async (page) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Mecânico")').first().click();
      await page.waitForURL('**/mechanic/**', { timeout: 10000 });

      await page.goto(`${BASE_URL}/mechanic/operations`, { waitUntil: 'networkidle' });
      await page.waitForSelector('text=Operação de baia e almoxarifado', { timeout: 8000 });

      // Verificar baias WMS
      const bayCount = await page.locator('.border, table, tr').count();
      if (bayCount === 0) {
        throw new Error('Nenhuma baia ou ordem de serviço listada no WMS');
      }
    });

  } finally {
    await browser.close();
  }

  console.log(`\n======================================================================`);
  console.log(`📊 RESULTADO FINAL DA AUDITORIA FASE 9:`);
  console.log(`   Testes Realizados: ${passedCount + failedCount}`);
  console.log(`   Aprovados:         ${passedCount} ✅`);
  console.log(`   Falhas:            ${failedCount} ❌`);
  console.log(`======================================================================\n`);

  if (failedCount > 0) {
    process.exit(1);
  }
})();
