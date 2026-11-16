const http = require('http');
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const distDir = path.resolve(__dirname, '../apps/web-app/dist/web-app/browser');
const artifactsDir = '/home/robson/.gemini/antigravity/brain/a9e6467f-aedb-40f4-a5bd-d79ae6357ef4';
const port = 4175;

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

const reclameAquiSnapshot = JSON.parse(
  fs.readFileSync(
    path.resolve(
      __dirname,
      '../apps/intelligence-service/app/modules/reclame_aqui_miner/seed_reclame_aqui_snapshot.json'
    ),
    'utf-8'
  )
);

function buildReclameAquiDto(syncStatus, syncMessage) {
  const total = reclameAquiSnapshot.totalComplaintsMinered;
  const categories = reclameAquiSnapshot.categories
    .map((c) => {
      const catTotal = c.resolvedCount + c.unresolvedCount + c.inReplicaCount;
      const growth = Number((((c.currQuarterCount - c.prevQuarterCount) / c.prevQuarterCount) * 100).toFixed(1));
      return {
        ...c,
        totalCount: catTotal,
        sharePct: Number(((catTotal * 100) / total).toFixed(1)),
        resolutionRatePct: Number(((c.resolvedCount * 100) / catTotal).toFixed(1)),
        growthRatePct: growth,
      };
    })
    .sort((a, b) => b.growthRatePct - a.growthRatePct);

  return {
    companyName: reclameAquiSnapshot.companyName,
    sourceUrl: reclameAquiSnapshot.sourceUrl,
    lastSyncedAt: new Date().toISOString(),
    syncStatus,
    syncMessage,
    periodLabel: reclameAquiSnapshot.periodLabel,
    reputationScore: reclameAquiSnapshot.reputationScore,
    reputationLabel: reclameAquiSnapshot.reputationLabel,
    totalComplaintsMinered: total,
    resolvedCount: reclameAquiSnapshot.resolvedCount,
    unresolvedCount: reclameAquiSnapshot.unresolvedCount,
    inReplicaCount: reclameAquiSnapshot.inReplicaCount,
    overallResolutionRatePct: 76.6,
    responseRatePct: reclameAquiSnapshot.responseRatePct,
    wouldDoBusinessAgainPct: reclameAquiSnapshot.wouldDoBusinessAgainPct,
    avgResolutionDays: reclameAquiSnapshot.avgResolutionDays,
    topGrowingCategoryLabel: categories[0].label,
    topGrowingCategoryRatePct: categories[0].growthRatePct,
    smartOpsAddressableCount: 436,
    smartOpsAddressablePct: 68.1,
    executiveThesisHeadline:
      'De 640 reclamações mineradas no Reclame Aqui, 68,1% (436 casos) concentram-se em 3 dores que mais cresceram no trimestre: falta de previsibilidade do valor das peças (+30,4%), espera prolongada em fila de pátio (+25,0%) e indisponibilidade de peça na bancada (+17,7%).',
    productDecisionRationale:
      'Escolhemos otimizar o aplicativo com Orçamento Instantâneo de Peças (vinculado à tabela oficial da Loja Mottu) e Triagem Visual Rápida porque são intervenções de software de alta alavancagem: atacam simultaneamente os 2 problemas de maior crescimento (+30,4% e +25,0%), dando previsibilidade financeira imediata ao entregador antes mesmo de pisar na oficina.',
    categories,
    recentComplaints: reclameAquiSnapshot.recentComplaints,
  };
}

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
        accessToken: ['mock', 'jwt', 'manager'].join('.'),
        expiresIn: 86400,
        userId: 'e0000000-0000-0000-0000-000000000001',
        fullName: 'Marina Gestora',
        email: 'gestor@mottu-ops.local',
        role: 'MANAGER',
        status: 'ACTIVE',
      })
    );
  }

  if (reqUrl === '/api/v1/manager/users/pending') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify([]));
  }

  if (reqUrl === '/api/v1/manager/analytics/summary') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(
      JSON.stringify({
        activeMotorcyclesCount: 124,
        autoResolvedPercentage: 78.5,
        currentHumanQueueSize: 4,
        estimatedMechanicHoursSaved: 36.2,
        lookerStudioEmbedUrl: null,
      })
    );
  }

  if (reqUrl === '/api/v1/manager/analytics/detailed') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ yardSnapshots: [], dailyVolumeBars: [], verdictBreakdown: [] }));
  }

  if (reqUrl === '/api/v1/manager/analytics/ai-advisor') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(
      JSON.stringify({
        yardKey: 'ALL',
        yardLabel: 'Todas as Unidades (SP)',
        generatedAt: new Date().toISOString(),
        modelUsed: 'gemini-2.5-flash',
        demandGrowthPct: 18.4,
        projectedNext7DaysInspections: 135,
        executiveHeadline: 'Crescimento de 18,4% na demanda de vistorias com alerta preventivo.',
        growthAnalysisText: 'Reposição preventiva recomendada para manter liberação rápida.',
        stockoutAlerts: [],
        recommendedActions: ['Priorizar reposição preventiva nas unidades SP-04 e SP-01.'],
      })
    );
  }

  if (reqUrl === '/api/v1/manager/analytics/reclame-aqui' && req.method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(
      JSON.stringify(
        buildReclameAquiDto(
          'SNAPSHOT_FALLBACK',
          'Snapshot minerado auditado carregado (640 reclamações processadas).'
        )
      )
    );
  }

  if (reqUrl === '/api/v1/manager/analytics/reclame-aqui/sync' && req.method === 'POST') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(
      JSON.stringify(
        buildReclameAquiDto(
          'SYNCED_LIVE',
          'Sincronização Reclame Aqui concluída com sucesso: 640 reclamações auditadas (490 resolvidas • 100 não resolvidas • 50 em réplica).'
        )
      )
    );
  }

  if (reqUrl === '/api/v1/manager/analytics/ask' && req.method === 'POST') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(
      JSON.stringify({
        query: 'Reclame Aqui (VoC)',
        yardKey: 'ALL',
        yardLabel: 'Todas as Unidades (SP)',
        generatedAt: new Date().toISOString(),
        modelUsed: 'sql-deterministic-engine',
        directAnswer:
          'Estudo Reclame Aqui (640 reclamações mineradas): 76,6% resolvidas (490), 15,6% não resolvidas (100) e 7,8% em réplica (50). Os problemas que mais cresceram no trimestre foram Previsibilidade de Valor e Quantidade de Peças (+30,4%) e Demora em Fila de Pátio (+25,0%). O Mottu Smart Ops ataca diretamente 68,1% (436 casos) dessas dores.',
        spokenAnswer:
          'No Reclame Aqui, 76,6% das 640 reclamações foram resolvidas. Valor de peças cresceu 30,4% e fila de pátio 25%. O Smart Ops resolve 68,1% dessas dores.',
        shouldShowTable: true,
        keyMetricLabel: 'Dores Atacadas pelo Smart Ops',
        keyMetricValue: '68,1% (436 de 640 casos)',
        keyMetricTrend: '+30,4% crescimento em Previsibilidade de Peças (Top #1)',
        columns: ['Categoria no Reclame Aqui', 'Volume (Part.)', 'Resolvidas vs Pendentes', 'Crescimento Trim.', 'Solução Smart Ops'],
        rows: [
          ['Previsibilidade de Valor e Qtd. de Peças', '188 (29,4%)', '138 Resolv. / 34 Não Res.', '+30,4% ▲', 'Orçamento Instantâneo Loja Mottu'],
          ['Demora em Fila de Pátio p/ Vistoria', '150 (23,4%)', '118 Resolv. / 20 Não Res.', '+25,0% ▲', 'Triagem Multi-IA (< 5s sem fila)'],
          ['Moto Retida por Falta de Peça Básica', '98 (15,3%)', '74 Resolv. / 16 Não Res.', '+17,7% ▲', 'Previsão Preditiva WMS'],
          ['Divergência de Avaria na Retirada', '112 (17,5%)', '86 Resolv. / 17 Não Res.', '+9,5% ▲', 'Escudo de Retirada 360°'],
          ['Dúvidas de Fatura Semanal e Pix', '92 (14,4%)', '74 Resolv. / 13 Não Res.', '+5,9% ▲', 'Extrato Semanal Auditável'],
        ],
        suggestedActionLabel: 'Ver Catálogo Oficial de Peças',
        suggestedActionRoute: '/manager/catalog',
        activeSkill: 'SKILL_RECLAME_AQUI',
        isAiOffline: false,
      })
    );
  }

  if (reqUrl === '/api/v1/manager/analytics/tts' && req.method === 'POST') {
    res.writeHead(204);
    return res.end();
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
  console.log(`🚀 Visual Audit Server (Plano 02 - Reclame Aqui VoC) running at http://localhost:${port}`);
  const browser = await chromium.launch({ headless: true });

  try {
    const context = await browser.newContext({
      viewport: { width: 1440, height: 1850 },
    });
    const page = await context.newPage();

    const networkFailures = [];
    const requestCounts = new Map();

    page.on('request', (req) => {
      const url = req.url();
      const count = (requestCounts.get(url) || 0) + 1;
      requestCounts.set(url, count);
      if (count > 6 && !url.includes('/tts')) {
        throw new Error(`Loop de rede detectado em ${url} (${count} chamadas)`);
      }
    });

    page.on('response', (res) => {
      if (res.status() >= 400 && !res.url().includes('favicon.ico')) {
        networkFailures.push({ status: res.status(), url: res.url() });
      }
    });

    // 1. Login as Manager -> Navigate to T10 Analytics
    await page.goto(`http://localhost:${port}/login`, { waitUntil: 'networkidle' });
    await page.locator('button:has-text("Gestor")').first().click();
    await page.waitForURL('**/manager/**', { timeout: 10000 });
    await page.goto(`http://localhost:${port}/manager/analytics`, { waitUntil: 'networkidle' });

    // Switch to Reclame Aqui (VoC) tab
    const raTab = page.locator('[data-testid="tab-reclame-aqui"]');
    await raTab.waitFor({ state: 'visible', timeout: 8000 });
    await raTab.click();

    await page.waitForSelector('[data-testid="reclame-aqui-overview-section"]', { timeout: 8000 });
    await page.waitForSelector('text=640 casos', { timeout: 8000 });
    await page.waitForSelector('text=76.6% Resolvidas', { timeout: 8000 });
    await page.waitForSelector('text=+30.4% ▲', { timeout: 8000 });
    await page.waitForSelector('text=68.1% (436)', { timeout: 8000 });
    await assertAllImagesRendered(page, 'T10 Reclame Aqui Overview');

    // Scroll to top so the sticky header stays cleanly at the top of the viewport without overlapping mid-card
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(250);

    const print4 = path.join(artifactsDir, 'print-04-manager-t10-reclame-aqui-overview.png');
    await page.locator('[data-testid="reclame-aqui-overview-section"]').screenshot({ path: print4 });
    console.log(`✅ Print 04 salvo: ${print4}`);

    // 2. Click "Sincronizar Reclame Aqui" and filter table by "Valor e Qtd. de Peças (+30,4%)"
    await page.locator('[data-testid="btn-sync-reclame-aqui"]').click();
    await page.waitForSelector('[data-testid="reclame-aqui-sync-banner"]', { timeout: 8000 });
    await page.locator('[data-testid="filter-ra-pricing"]').click();
    await page.waitForSelector('text=RA-2026-908412', { timeout: 8000 });
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(250);

    const print5 = path.join(artifactsDir, 'print-05-manager-t10-reclame-aqui-sync-and-table.png');
    await page.locator('[data-testid="reclame-aqui-overview-section"]').screenshot({ path: print5 });
    console.log(`✅ Print 05 salvo: ${print5}`);

    // 3. Resize viewport to standard 1440x920 and click "Analisar com a Melissa" -> Opens Melissa Copilot Drawer with SKILL_RECLAME_AQUI response
    await page.setViewportSize({ width: 1440, height: 920 });
    await page.locator('[data-testid="reclame-aqui-overview-section"]').scrollIntoViewIfNeeded();
    await page.locator('[data-testid="btn-ask-melissa-ra"]').click();
    await page.waitForSelector('text=Dores Atacadas pelo Smart Ops', { timeout: 8000 });
    await page.waitForSelector('text=68,1% (436 de 640 casos)', { timeout: 8000 });
    await page.waitForTimeout(650); // wait for 0.5s typewriter animation + auto-scroll
    await page.evaluate(() => {
      const el = document.getElementById('melissa-chat-scroll-container');
      if (el) el.scrollTop = el.scrollHeight;
    });
    await page.waitForTimeout(150);

    const print6 = path.join(artifactsDir, 'print-06-manager-t10-melissa-reclame-aqui-skill.png');
    await page.screenshot({ path: print6, fullPage: false });
    console.log(`✅ Print 06 salvo: ${print6}`);

    if (networkFailures.length > 0) {
      throw new Error(`Erros HTTP detectados: ${JSON.stringify(networkFailures)}`);
    }

    console.log('\n🎉 PLANO 02 VISUAL AUDIT & NETWORK TRAP PASSED 100%!');
    await context.close();
  } catch (err) {
    console.error('❌ Erro no teste visual do Plano 02:', err);
    process.exitCode = 1;
  } finally {
    await browser.close();
    server.close();
  }
});
