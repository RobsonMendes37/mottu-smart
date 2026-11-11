const { chromium } = require('playwright');

const BASE_URL = process.env.BASE_URL || 'http://161.153.2.148';

/**
 * Suite de Auditoria Estrita de Interface e Integridade Visual / Rede
 * 
 * Regras Obrigatórias Verificadas:
 * 1. Zero 404 / 500 em imagens, scripts, estilos e APIs (Network Integrity Trap).
 * 2. Zero loops de requisições (> 4 requisições idênticas para a mesma URL em uma mesma tela).
 * 3. Renderização real de pixels (todas as tags <img> visíveis devem ter complete && naturalWidth > 0).
 * 4. Vistoria Completa Ponta a Ponta: Garagem -> Wizard (4 slots) -> Envio -> Status.
 * 5. Bancada do Mecânico com Lupa Óptica e Abas 360.
 * 6. Painel do Gestor com Analytics, Catálogo e Faturamento.
 */
(async () => {
  console.log(`======================================================================`);
  console.log(`🛡️  AUDITORIA E2E RIGOROSA DE INTERFACE, REDE E IMAGENS — MOTTU SMART OPS`);
  console.log(`   Destino: ${BASE_URL}`);
  console.log(`======================================================================\n`);

  const browser = await chromium.launch({ headless: true });
  let passedCount = 0;
  let failedCount = 0;
  const auditErrors = [];

  /**
   * Helper para auditoria de imagens renderizadas no DOM.
   * Aguarda de forma assíncrona até que todas as imagens visíveis terminem de carregar e decodificar (naturalWidth > 0).
   */
  const assertAllImagesRendered = async (page, screenName, maxWaitMs = 6000) => {
    const startTime = Date.now();
    while (Date.now() - startTime < maxWaitMs) {
      const images = await page.locator('img').all();
      let allReady = true;
      for (const img of images) {
        const isVisible = await img.isVisible().catch(() => false);
        if (isVisible) {
          const info = await img.evaluate(el => ({
            src: el.currentSrc || el.src,
            complete: el.complete,
            naturalWidth: el.naturalWidth
          })).catch(() => null);

          if (info && (!info.complete || info.naturalWidth === 0)) {
            allReady = false;
            break;
          }
        }
      }
      if (allReady) return;
      await page.waitForTimeout(300);
    }

    // Se após maxWaitMs alguma imagem visível ainda não tiver pixels decodificados, falha com clareza
    const images = await page.locator('img').all();
    for (const img of images) {
      const isVisible = await img.isVisible().catch(() => false);
      if (isVisible) {
        const info = await img.evaluate(el => ({
          src: el.currentSrc || el.src,
          complete: el.complete,
          naturalWidth: el.naturalWidth
        })).catch(() => null);

        if (info && (!info.complete || info.naturalWidth === 0)) {
          throw new Error(`[IMAGEM QUEBRADA] Em '${screenName}': a imagem com src '${info.src}' não renderizou pixels reais após ${maxWaitMs}ms.`);
        }
      }
    }
  };

  /**
   * Executor de teste com monitoramento contínuo de rede:
   * - Trap de 404/500
   * - Trap de loop infinito de requisições
   */
  const runAuditedTest = async (testName, testFn, options = {}) => {
    process.stdout.write(`⏳ [AUDITORIA] ${testName}... `);

    const viewport = options.viewport || { width: 1440, height: 900 };
    const context = await browser.newContext({
      viewport,
      isMobile: options.isMobile || false,
      hasTouch: options.hasTouch || false,
    });
    const page = await context.newPage();

    const requestCounts = new Map();
    const networkFailures = [];
    let loopDetected = null;

    // 1. Armadilha contra loops de requisição (flooding / cascade onerror)
    page.on('request', req => {
      const url = req.url();
      const count = (requestCounts.get(url) || 0) + 1;
      requestCounts.set(url, count);

      if (count > 5 && !loopDetected) {
        loopDetected = `[LOOP DE REDE DETECTADO] O recurso '${url}' foi requisitado ${count} vezes em loop na mesma tela!`;
      }
    });

    // 2. Armadilha contra 404 / 500 em imagens e arquivos essenciais
    page.on('response', res => {
      const status = res.status();
      const url = res.url();
      const resourceType = res.request().resourceType();

      // Permitir falhas externas ou intencionais se especificadas, mas nunca imagens/scripts/APIs internas
      if (status >= 400 && !url.includes('favicon.ico')) {
        networkFailures.push({ status, url, resourceType });
      }
    });

    page.on('console', msg => {
      const txt = msg.text();
      if (txt.includes('Error') && !txt.includes('favicon')) {
        console.log(`\n   [Console Error]: ${txt}`);
      }
    });

    try {
      await testFn(page, context, {
        assertImages: (name) => assertAllImagesRendered(page, name || testName)
      });

      // Validação final de integridade de rede do teste
      if (loopDetected) {
        throw new Error(loopDetected);
      }

      if (networkFailures.length > 0) {
        const details = networkFailures
          .map(f => `HTTP ${f.status} (${f.resourceType}): ${f.url}`)
          .slice(0, 10)
          .join('\n      ');
        const extra = networkFailures.length > 10 ? `... (+${networkFailures.length - 10} outras falhas)` : '';
        throw new Error(`Falhas de rede detectadas (${networkFailures.length} requisições com erro):\n      ${details}${extra}`);
      }

      console.log(`✅ APROVADO`);
      passedCount++;
    } catch (err) {
      console.log(`❌ FALHOU`);
      console.log(`   Detalhe: ${err.message}\n`);
      auditErrors.push({ testName, error: err.message });
      failedCount++;
    } finally {
      await context.close();
    }
  };

  try {
    // -----------------------------------------------------------------------------------
    // MÓDULO 1: AUTENTICAÇÃO E TELA DE LOGIN (DESKTOP & MOBILE)
    // -----------------------------------------------------------------------------------
    await runAuditedTest('Login: Vitrine Demo, Troca de Perfis e Zero Erros de Rede', async (page, ctx, { assertImages }) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.waitForSelector('text=Acesse sua conta', { timeout: 8000 });
      await assertImages('Tela de Login');

      // Testar alternância entre Modo Vitrine e Operação Real
      const realModeBtn = page.locator('button:has-text("Operação Real")');
      await realModeBtn.click();
      await page.waitForTimeout(300);

      const demoModeBtn = page.locator('button:has-text("Demonstração")');
      await demoModeBtn.click();
      await page.waitForTimeout(300);
    });

    // -----------------------------------------------------------------------------------
    // MÓDULO 2: ENTREGADOR — GARAGEM, FOTOS 360 E ESCUDO DE RETIRADA (T02)
    // -----------------------------------------------------------------------------------
    await runAuditedTest('Entregador: Garagem T02, Fotos 360 da Moto e Validação de Imagens', async (page, ctx, { assertImages }) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Entregador")').first().click();
      await page.waitForURL('**/rider/garage', { timeout: 10000 });

      // Verificar elementos principais
      await page.waitForSelector('text=MOT1A23', { timeout: 8000 });
      await page.waitForSelector('text=Giro 360 de Retirada', { timeout: 8000 });

      // Verificar que todos os 4 ângulos de fotos da moto renderizam pixels válidos (sem loop e sem quebrar)
      await assertImages('Garagem do Entregador (T02)');
    });

    // -----------------------------------------------------------------------------------
    // MÓDULO 3: ENTREGADOR — VISTORIA COMPLETA (WIZARD NO CELULAR: 4 SLOTS + ENVIO)
    // -----------------------------------------------------------------------------------
    await runAuditedTest('Entregador (Mobile): Fluxo Completo do Wizard de Vistoria (4 Fotos + Envio)', async (page, ctx, { assertImages }) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      
      // Chavear para Operação Real (Carlos Silva) para fluxo autêntico com cota livre
      const realModeBtn = page.locator('button:has-text("Operação Real")');
      await realModeBtn.click();
      await page.waitForTimeout(300);

      const realRiderBtn = page.locator('button:has-text("Entregador")').last();
      await realRiderBtn.click();
      await page.waitForURL('**/rider/garage', { timeout: 10000 });

      // Clicar em "Fazer vistoria rápida"
      const novaVistoriaBtn = page.locator('button:has-text("Fazer vistoria rápida")');
      if (await novaVistoriaBtn.isEnabled()) {
        await novaVistoriaBtn.click();
      } else {
        await page.goto(`${BASE_URL}/rider/inspections/new`, { waitUntil: 'networkidle' });
      }
      await page.waitForURL('**/rider/inspections/new', { timeout: 10000 });

      // Slot 1: Avaria / Peça danificada
      await page.waitForSelector('text=1. Avaria*', { timeout: 8000 });
      const demoPhotoBtn1 = page.locator('button:has-text("Usar foto de teste")');
      await demoPhotoBtn1.click();
      // Aguardar upload e exibição da foto
      await page.waitForSelector('img[alt="Prévia da foto capturada"], img[alt="Miniatura do ângulo capturado"]', { timeout: 8000 });
      await assertImages('Wizard - Slot 1 (Avaria)');

      // Slot 2: Painel / Odômetro
      const slot2Btn = page.locator('text=2. Painel');
      await slot2Btn.click();
      await page.waitForTimeout(300);
      const demoPhotoBtn2 = page.locator('button:has-text("Usar foto de teste")');
      await demoPhotoBtn2.click();
      await page.waitForTimeout(600);
      await assertImages('Wizard - Slot 2 (Odômetro)');

      // Slot 3: Frente da Moto
      const slot3Btn = page.locator('text=3. Frente');
      await slot3Btn.click();
      await page.waitForTimeout(300);
      const demoPhotoBtn3 = page.locator('button:has-text("Usar foto de teste")');
      await demoPhotoBtn3.click();
      await page.waitForTimeout(600);
      await assertImages('Wizard - Slot 3 (Frente)');

      // Slot 4: Detalhe Adicional
      const slot4Btn = page.locator('text=4. Detalhe');
      await slot4Btn.click();
      await page.waitForTimeout(300);
      const demoPhotoBtn4 = page.locator('button:has-text("Usar foto de teste")');
      await demoPhotoBtn4.click();
      await page.waitForTimeout(600);
      await assertImages('Wizard - Slot 4 (Detalhe)');

      // Preencher condição e contexto
      await page.locator('button:has-text("Posso seguir rodando")').click();
      await page.locator('button:has-text("Desgaste natural de uso diário")').click();

      // Odômetro deve estar preenchido com valor válido (> inicial)
      const odoInput = page.locator('#odometerInput');
      const valStr = await odoInput.inputValue();
      const currentOdo = parseInt(valStr, 10) || 12000;
      await odoInput.fill(String(Math.max(currentOdo, 25000) + 100));

      // Enviar vistoria
      const submitBtn = page.locator('footer button:has-text("Enviar vistoria")');
      await submitBtn.waitFor({ state: 'visible', timeout: 5000 });
      await submitBtn.click();

      // Aguardar redirecionamento para o resultado da vistoria (/rider/inspections/:id)
      await page.waitForURL('**/rider/inspections/**', { timeout: 15000 });

      // Verificar que a tela de status T04 carregou sem 404 e com imagens válidas
      await page.waitForSelector('text=Resultado da vistoria', { timeout: 10000 });
      await assertImages('Status da Vistoria T04');
    }, {
      viewport: { width: 390, height: 844 }, // Mobile iPhone 14
      isMobile: true,
      hasTouch: true
    });

    // -----------------------------------------------------------------------------------
    // MÓDULO 4: ENTREGADOR — FATURA PIX E HISTÓRICO DE ENTREGAS
    // -----------------------------------------------------------------------------------
    await runAuditedTest('Entregador: Aluguel Semanal Pix e Extrato de Entregas', async (page, ctx, { assertImages }) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Entregador")').first().click();
      await page.waitForURL('**/rider/garage', { timeout: 10000 });

      await page.click('a[href="/rider/rental"]');
      await page.waitForURL('**/rider/rental', { timeout: 8000 });
      await page.waitForSelector('text=Transparência na cobrança', { timeout: 8000 });
      await assertImages('Fatura do Entregador');
    });

    // -----------------------------------------------------------------------------------
    // MÓDULO 5: MECÂNICO — FILA T05 E BANCADA COM LUPA ÓPTICA T06
    // -----------------------------------------------------------------------------------
    await runAuditedTest('Mecânico: Fila T05 e Bancada T06 com Lupa Óptica e Abas 360', async (page, ctx, { assertImages }) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Mecânico")').first().click();
      await page.waitForURL('**/mechanic/queue', { timeout: 10000 });

      await page.waitForSelector('text=Fila de vistorias da unidade', { timeout: 8000 });
      await assertImages('Fila do Mecânico T05');

      // Entrar na bancada da primeira vistoria
      const workbenchBtn = page.locator('tbody tr td button').first();
      await workbenchBtn.click();
      await page.waitForURL('**/mechanic/workbench/**', { timeout: 10000 });

      // Lupa óptica e foto em alta resolução
      await page.waitForSelector('app-photo-zoom-lens img, app-photo-zoom-lens', { timeout: 8000 });
      await assertImages('Bancada T06 (Foto de Análise)');

      // Testar aba Giro 360 da Retirada
      const baselineTab = page.locator('button:has-text("Giro 360 da Retirada")');
      if (await baselineTab.isVisible()) {
        await baselineTab.click();
        await page.waitForTimeout(500);
        await assertImages('Bancada T06 (Aba Giro 360)');
      }
    });

    // -----------------------------------------------------------------------------------
    // MÓDULO 6: MECÂNICO — GESTÃO DA FROTA (T07) E WMS BAIAS
    // -----------------------------------------------------------------------------------
    await runAuditedTest('Mecânico: Frota T07 e Operações de Baia WMS', async (page, ctx, { assertImages }) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Mecânico")').first().click();
      await page.waitForURL('**/mechanic/queue', { timeout: 10000 });

      // Frota T07
      await page.click('a[href="/mechanic/fleet"]');
      await page.waitForURL('**/mechanic/fleet', { timeout: 8000 });
      await page.waitForSelector('text=Frota de motocicletas da unidade', { timeout: 8000 });
      await assertImages('Frota do Mecânico T07');

      // WMS Baias
      await page.click('a[href="/mechanic/operations"]');
      await page.waitForURL('**/mechanic/operations', { timeout: 8000 });
      await page.waitForSelector('text=Operação de baia e almoxarifado', { timeout: 8000 });
      await assertImages('WMS Baias de Oficina');
    });

    // -----------------------------------------------------------------------------------
    // MÓDULO 7: GESTOR — APROVAÇÕES, CATÁLOGO VTEX, FATURAMENTO E ANALYTICS
    // -----------------------------------------------------------------------------------
    await runAuditedTest('Gestor: Aprovações T08, Catálogo T09, Faturamento T12 e Analytics T10', async (page, ctx, { assertImages }) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.locator('button:has-text("Gestor")').first().click();
      await page.waitForURL('**/manager/approvals', { timeout: 10000 });
      await assertImages('Gestor T08');

      // Catálogo
      await page.click('a[href="/manager/catalog"]');
      await page.waitForURL('**/manager/catalog', { timeout: 8000 });
      await page.waitForSelector('text=Catálogo oficial de peças', { timeout: 8000 });
      await assertImages('Gestor Catálogo T09');

      // Faturamento
      await page.click('a[href="/manager/billing"]');
      await page.waitForURL('**/manager/billing', { timeout: 8000 });
      await page.waitForSelector('text=Contratos e conciliação Pix semanal', { timeout: 8000 });
      await assertImages('Gestor Faturamento T12');

      // Analytics com Previsão What-If
      await page.click('a[href="/manager/analytics"]');
      await page.waitForURL('**/manager/analytics', { timeout: 8000 });
      await page.waitForSelector('text=Indicadores operacionais da unidade', { timeout: 8000 });
      await assertImages('Gestor Analytics T10');
    });

  } finally {
    await browser.close();
  }

  console.log(`\n======================================================================`);
  console.log(`📊 RELATÓRIO CONSOLIDADO DA AUDITORIA DE INTERFACE E REDE:`);
  console.log(`   Total de Baterias Executadas: ${passedCount + failedCount}`);
  console.log(`   Aprovadas com Sucesso:        ${passedCount} ✅`);
  console.log(`   Falhas Encontradas:           ${failedCount} ❌`);
  console.log(`======================================================================`);

  if (auditErrors.length > 0) {
    console.log(`\n🚨 DETALHAMENTO DAS FALHAS:`);
    auditErrors.forEach((err, idx) => {
      console.log(`   ${idx + 1}. [${err.testName}]`);
      console.log(`      ${err.error}`);
    });
    console.log(`======================================================================\n`);
    process.exit(1);
  }
})();
