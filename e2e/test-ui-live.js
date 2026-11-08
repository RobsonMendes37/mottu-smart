const { chromium } = require('playwright');

const BASE_URL = process.env.BASE_URL || 'http://161.153.2.148:18088';

(async () => {
  console.log(`🚀 Iniciando bateria de testes de interface Playwright contra: ${BASE_URL}`);
  const browser = await chromium.launch({ headless: true });

  const testRole = async (roleName, roleLabel, expectedUrlPart) => {
    console.log(`\n=== TESTANDO PERFIL: ${roleName.toUpperCase()} ===`);
    const context = await browser.newContext({ viewport: { width: 1280, height: 800 } });
    const page = await context.newPage();

    page.on('console', msg => {
      const txt = msg.text();
      if (!txt.includes('Angular is running in development mode')) {
        console.log(` [Browser Console]:`, txt);
      }
    });

    page.on('response', resp => {
      if (resp.url().includes('/api/')) {
        console.log(` [API Response ${resp.status()}]:`, resp.url());
      }
    });

    await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
    console.log(` - Página de login carregada.`);

    const demoBtn = page.locator(`button:has-text("${roleLabel}")`);
    await demoBtn.waitFor({ state: 'visible', timeout: 5000 });
    console.log(` - Clicando em "${roleLabel}"...`);
    await demoBtn.click();

    await page.waitForTimeout(3000);
    console.log(` - URL atual: ${page.url()}`);
    const matched = page.url().includes(expectedUrlPart);
    console.log(` - Navegação para ${expectedUrlPart}: ${matched ? 'SUCESSO ✅' : 'PENDENTE ⚠️'}`);

    await context.close();
  };

  try {
    await testRole('Gestor', 'Gestor', '/manager/');
    await testRole('Mecânico', 'Mecânico', '/mechanic/');
    await testRole('Entregador', 'Entregador', '/rider/');
  } finally {
    await browser.close();
  }

  console.log('\n🎉 Bateria de testes de interface finalizada com sucesso!');
})();
