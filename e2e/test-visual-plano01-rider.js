const http = require('http');
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const distDir = path.resolve(__dirname, '../apps/web-app/dist/web-app/browser');
const demoPhotosDir = path.resolve(__dirname, '../apps/web-app/public/demo-photos');
const artifactsDir = '/home/robson/.gemini/antigravity/brain/a9e6467f-aedb-40f4-a5bd-d79ae6357ef4';
const port = 4174;

const mimeTypes = {
  '.html': 'text/html',
  '.js': 'text/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.webp': 'image/webp',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.webmanifest': 'application/manifest+json',
};

const garagePayload = {
  activeContract: {
    contractId: 'a0000000-0000-0000-0000-000000000001',
    pickedUpAt: new Date(Date.now() - 12 * 3600000).toISOString(),
    initialOdometerKm: 1000,
    motorcycle: {
      id: '33333333-3333-3333-3333-333333333333',
      plate: 'MOT1A23',
      model: 'MOTTU_SPORT_110I',
      status: 'RENTED',
      currentOdometerKm: 1035,
      yardName: 'Galpão Mottu Butantã',
    },
    pickupPhotosJson: JSON.stringify({
      frontPhotoUrl: '/demo-photos/avaria-farol.webp',
      rearPhotoUrl: '/demo-photos/avaria-pneu.webp',
      rightSidePhotoUrl: '/demo-photos/avaria-retrovisor.webp',
      leftSidePhotoUrl: '/demo-photos/avaria-escapamento.webp',
      odometerPhotoUrl: '/demo-photos/avaria-painel.webp',
      initialOdometerKm: 1000,
      recordedAt: new Date(Date.now() - 12 * 3600000).toISOString(),
    }),
    pickupValidatedByRider: true,
    pickupValidatedAt: new Date(Date.now() - 11 * 3600000).toISOString(),
    isShieldActive: true,
    remainingShieldHours: 36.0,
    remainingShieldKm: 65,
  },
  openInspectionsCount: 0,
  recentInspections: [
    {
      id: 'd2000000-0000-0000-0000-000000000001',
      status: 'AUTO_RESOLVED',
      finalVerdict: 'MAU_USO',
      totalEstimatedBrl: 140.5,
      createdAt: new Date(Date.now() - 25 * 60000).toISOString(),
      itemsCount: 3,
      partNamesSummary: [
        'Retrovisor Direito Sport (R$ 42,50)',
        'Manete de Freio Direito (R$ 25,00)',
        'Protetor Lateral do Escapamento (R$ 38,00)',
      ],
    },
    {
      id: 'd2000000-0000-0000-0000-000000000003',
      status: 'AUTO_RESOLVED',
      finalVerdict: 'DESGASTE_NATURAL',
      totalEstimatedBrl: 0.0,
      createdAt: new Date(Date.now() - 180 * 60000).toISOString(),
      itemsCount: 2,
      partNamesSummary: [
        'Pneu Traseiro 80/100-14 (R$ 0,00 Coberto)',
        'Lona de Freio Traseira (R$ 0,00 Coberto)',
      ],
    },
    {
      id: 'd2000000-0000-0000-0000-000000000004',
      status: 'HUMAN_RESOLVED',
      finalVerdict: 'MAU_USO',
      totalEstimatedBrl: 190.0,
      createdAt: new Date(Date.now() - 1440 * 60000).toISOString(),
      itemsCount: 2,
      partNamesSummary: [
        'Bloco Óptico do Farol Dianteiro (R$ 130,00)',
        'Manete de Freio Direito (R$ 25,00)',
      ],
    },
  ],
};

const inspectionDetailsMap = {
  'd2000000-0000-0000-0000-000000000001': {
    id: 'd2000000-0000-0000-0000-000000000001',
    status: 'AUTO_RESOLVED',
    photoDownloadUrl: '/demo-photos/avaria-retrovisor.webp',
    reportedContext: 'ACCIDENT_OR_DROP',
    safetyRisk: 'SAFE_TO_RIDE',
    reportedOdometerKm: 1840,
    riderNotes: 'Ao desviar de buraco na chuva a moto tombou na calçada e quebrou o retrovisor direito.',
    finalVerdict: 'MAU_USO',
    aiConfidence: 0.94,
    laborFeeBrl: 35.0,
    totalEstimatedBrl: 140.5,
    customerFeedbackText:
      'Identificamos 3 peças com avaria por queda lateral (Retrovisor Direito, Manete de Freio e Protetor de Escapamento). Valores calculados pela tabela oficial da Loja Mottu para sua previsibilidade.',
    canContest: true,
    contestedOnce: false,
    items: [
      {
        catalogPartId: 'SKU-RETROVISOR-DIR-SPORT',
        partName: 'Retrovisor Direito Sport',
        unitPriceBrl: 42.5,
        addedBy: 'AI',
      },
      {
        catalogPartId: 'SKU-MANETE-DIR-SPORT',
        partName: 'Manete de Freio Direito',
        unitPriceBrl: 25.0,
        addedBy: 'AI',
      },
      {
        catalogPartId: 'SKU-PROT-ESCAP-SPORT',
        partName: 'Protetor Lateral do Escapamento',
        unitPriceBrl: 38.0,
        addedBy: 'AI',
      },
    ],
    appointment: null,
    createdAt: new Date(Date.now() - 25 * 60000).toISOString(),
    resolvedAt: new Date(Date.now() - 24 * 60000).toISOString(),
    committeeEvaluationJson: JSON.stringify({
      consensusVerdict: 'MAU_USO',
      consensusScore: '3/3 Unânime',
      isUnanimous: true,
      canIdentifyPartsClearly: true,
      requiresPresentialInspection: false,
      averageConfidence: 0.94,
      summary: 'Consenso unânime da Junta Técnica confirmando 3 componentes para reposição.',
      evaluators: [
        {
          specialistRole: 'PERITO_FORENSE_IMPACTO',
          verdict: 'MAU_USO',
          confidence: 0.95,
          reasoning: 'Impacto lateral no espelho, manete e protetor metálico de escapamento.',
        },
        {
          specialistRole: 'PERITO_COMPARADOR_360',
          verdict: 'MAU_USO',
          confidence: 0.93,
          reasoning: 'As 3 peças estavam íntegras na vistoria 360 de retirada da moto.',
        },
      ],
    }),
  },
  'd2000000-0000-0000-0000-000000000003': {
    id: 'd2000000-0000-0000-0000-000000000003',
    status: 'AUTO_RESOLVED',
    photoDownloadUrl: '/demo-photos/avaria-pneu.webp',
    reportedContext: 'NORMAL_USE_WEAR',
    safetyRisk: 'SAFE_TO_RIDE',
    reportedOdometerKm: 8920,
    riderNotes: 'Pneu traseiro e lona atingiram limite TWI após semanas de entregas.',
    finalVerdict: 'DESGASTE_NATURAL',
    aiConfidence: 0.965,
    laborFeeBrl: 0.0,
    totalEstimatedBrl: 0.0,
    customerFeedbackText:
      'Desgaste natural confirmado em 2 itens (Pneu Traseiro e Lona de Freio). Cobertura de 100% pelo plano Mottu: você economizou R$ 248,00 e não paga nada (R$ 0,00).',
    canContest: false,
    contestedOnce: false,
    items: [
      {
        catalogPartId: 'SKU-PNEU-TRAS-SPORT',
        partName: 'Pneu Traseiro 80/100-14',
        unitPriceBrl: 180.0,
        addedBy: 'AI',
      },
      {
        catalogPartId: 'SKU-LONA-FREIO-TRAS-SPORT',
        partName: 'Lona de Freio Traseira',
        unitPriceBrl: 33.0,
        addedBy: 'AI',
      },
    ],
    appointment: null,
    createdAt: new Date(Date.now() - 180 * 60000).toISOString(),
    resolvedAt: new Date(Date.now() - 179 * 60000).toISOString(),
    committeeEvaluationJson: null,
  },
};

const server = http.createServer((req, res) => {
  const reqUrl = req.url.split('?')[0];

  if (reqUrl === '/api/v1/auth/public-config') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ publicDemoMode: true }));
  }

  if (reqUrl === '/api/v1/auth/login' && req.method === 'POST') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(
      JSON.stringify({
        accessToken: ['mock', 'jwt', 'demo'].join('.'),
        expiresIn: 86400,
        userId: 'e0000000-0000-0000-0000-000000000003',
        fullName: 'Carlos Entregador',
        email: 'entregador@mottu-ops.local',
        role: 'RIDER',
        status: 'ACTIVE',
      })
    );
  }

  if (reqUrl === '/api/v1/rider/garage') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(garagePayload));
  }

  if (reqUrl === '/api/v1/yards') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(
      JSON.stringify([
        {
          id: '11111111-1111-1111-1111-111111111111',
          name: 'Galpão Mottu Butantã',
          city: 'São Paulo',
          address: 'Av. Corifeu de Azevedo Marques, 1000',
          active: true,
        },
      ])
    );
  }

  if (reqUrl.startsWith('/api/v1/rider/inspections/') && reqUrl.endsWith('/stream')) {
    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      Connection: 'keep-alive',
    });
    res.write(`data: ${JSON.stringify({ status: 'AUTO_RESOLVED', stepLabel: 'Concluído' })}\n\n`);
    return res.end();
  }

  if (reqUrl.startsWith('/api/v1/rider/inspections/')) {
    const id = reqUrl.split('/').pop();
    const payload =
      inspectionDetailsMap[id] || inspectionDetailsMap['d2000000-0000-0000-0000-000000000001'];
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(payload));
  }

  let filePath = path.join(distDir, reqUrl === '/' ? '/index.html' : reqUrl);
  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    filePath = path.join(distDir, 'index.html');
  }

  const ext = path.extname(filePath).toLowerCase();
  const contentType = mimeTypes[ext] || 'application/octet-stream';

  fs.readFile(filePath, (err, content) => {
    if (err) {
      res.writeHead(500);
      res.end('Server Error');
    } else {
      res.writeHead(200, { 'Content-Type': contentType });
      res.end(content);
    }
  });
});

async function assertAllImagesRendered(page, screenName) {
  const images = await page.locator('img').all();
  for (const img of images) {
    const isVisible = await img.isVisible().catch(() => false);
    if (isVisible) {
      const info = await img.evaluate((el) => ({
        src: el.currentSrc || el.src,
        complete: el.complete,
        naturalWidth: el.naturalWidth,
      }));
      if (!info.complete || info.naturalWidth === 0) {
        throw new Error(`[IMAGEM QUEBRADA] Em ${screenName}: ${info.src}`);
      }
    }
  }
}

server.listen(port, async () => {
  console.log(`🚀 Visual Audit Server (Plano 01) running at http://localhost:${port}`);
  const browser = await chromium.launch({ headless: true });

  try {
    const context = await browser.newContext({
      viewport: { width: 390, height: 844 },
      isMobile: true,
      hasTouch: true,
    });
    const page = await context.newPage();

    const networkFailures = [];
    const requestCounts = new Map();

    page.on('request', (req) => {
      const url = req.url();
      const count = (requestCounts.get(url) || 0) + 1;
      requestCounts.set(url, count);
      if (count > 5 && !url.includes('/stream')) {
        throw new Error(`Loop de rede detectado em ${url} (${count} chamadas)`);
      }
    });

    page.on('response', (res) => {
      if (res.status() >= 400 && !res.url().includes('favicon.ico')) {
        networkFailures.push({ status: res.status(), url: res.url() });
      }
    });

    // 1. Login as Demo Rider -> Garage T02
    await page.goto(`http://localhost:${port}/login`, { waitUntil: 'networkidle' });
    await page.locator('button:has-text("Entregador")').first().click();
    await page.waitForURL('**/rider/garage', { timeout: 10000 });
    await page.waitForSelector('text=Quanto você precisa preparar para pagar', { timeout: 8000 });
    await page.waitForSelector('text=3 unidades', { timeout: 8000 });
    await page.waitForSelector('text=R$ 140,50', { timeout: 8000 });
    await assertAllImagesRendered(page, 'Garagem T02');

    const print1 = path.join(artifactsDir, 'print-01-rider-demo-garage-t02.png');
    await page.screenshot({ path: print1, fullPage: true });
    console.log(`✅ Print 01 salvo: ${print1}`);

    // 2. Navigate to T04 — Mau Uso (3 peças, R$ 140,50)
    await page.locator('button:has-text("Ver valor individual de cada peça")').click();
    await page.waitForURL('**/rider/inspections/d2000000-0000-0000-0000-000000000001', {
      timeout: 10000,
    });
    await page.waitForSelector('text=Retrovisor Direito Sport', { timeout: 8000 });
    await page.waitForSelector('text=Manete de Freio Direito', { timeout: 8000 });
    await page.waitForSelector('text=Protetor Lateral do Escapamento', { timeout: 8000 });
    await page.waitForSelector('text=3 unidades', { timeout: 8000 });
    await assertAllImagesRendered(page, 'Status T04 Mau Uso');

    const print2 = path.join(artifactsDir, 'print-02-rider-demo-status-mau-uso-t04.png');
    await page.screenshot({ path: print2, fullPage: true });
    console.log(`✅ Print 02 salvo: ${print2}`);

    // 3. Navigate to T04 — Desgaste Natural Isento (2 peças, R$ 0,00)
    await page.goto(
      `http://localhost:${port}/rider/inspections/d2000000-0000-0000-0000-000000000003`,
      { waitUntil: 'networkidle' }
    );
    await page.waitForSelector('text=Pneu Traseiro 80/100-14', { timeout: 8000 });
    await page.waitForSelector('text=Lona de Freio Traseira', { timeout: 8000 });
    await page.waitForSelector('text=2 unidades', { timeout: 8000 });
    await page.waitForSelector('text=- R$ 248,00', { timeout: 8000 });
    await assertAllImagesRendered(page, 'Status T04 Isento');

    const print3 = path.join(artifactsDir, 'print-03-rider-demo-status-isento-t04.png');
    await page.screenshot({ path: print3, fullPage: true });
    console.log(`✅ Print 03 salvo: ${print3}`);

    if (networkFailures.length > 0) {
      throw new Error(`Erros HTTP detectados: ${JSON.stringify(networkFailures)}`);
    }

    console.log('\n🎉 PLANO 01 VISUAL AUDIT & NETWORK TRAP PASSED 100%!');
    await context.close();
  } catch (err) {
    console.error('❌ Erro no teste visual do Plano 01:', err);
    process.exitCode = 1;
  } finally {
    await browser.close();
    server.close();
  }
});
