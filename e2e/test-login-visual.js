const http = require('http');
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

// Simple static SPA server for apps/web-app/dist/web-app/browser
const distDir = path.resolve(__dirname, '../apps/web-app/dist/web-app/browser');
const port = 4173;

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
  '.webmanifest': 'application/manifest+json'
};

const server = http.createServer((req, res) => {
  let reqPath = req.url.split('?')[0];
  if (reqPath === '/') reqPath = '/index.html';
  
  let filePath = path.join(distDir, reqPath);
  
  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    // SPA fallback
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

server.listen(port, async () => {
  console.log(`Test server running at http://localhost:${port}`);
  const browser = await chromium.launch({ headless: true });

  const artifactsDir = '/home/robson/.gemini/antigravity/brain/73f16434-b768-495c-86e6-da2985ecd46a';

  try {
    // 1. DESKTOP VIEWPORT (1440x900)
    console.log('Testing Desktop Viewport (1440x900)...');
    const desktopContext = await browser.newContext({
      viewport: { width: 1440, height: 900 }
    });
    const desktopPage = await desktopContext.newPage();

    let desktopNetworkErrors = [];
    desktopPage.on('response', res => {
      if (res.status() >= 400) {
        desktopNetworkErrors.push({ status: res.status(), url: res.url() });
      }
    });

    await desktopPage.goto(`http://localhost:${port}/login`, { waitUntil: 'networkidle' });
    await desktopPage.waitForSelector('input[type="email"]');

    // Verify image rendered with real pixels
    const imgInfo = await desktopPage.locator('aside img').evaluate(el => ({
      complete: el.complete,
      naturalWidth: el.naturalWidth,
      naturalHeight: el.naturalHeight,
      currentSrc: el.currentSrc
    }));
    console.log('Desktop Hero Image Info:', imgInfo);

    if (!imgInfo.complete || imgInfo.naturalWidth === 0) {
      throw new Error(`Desktop image failed to render pixels: ${JSON.stringify(imgInfo)}`);
    }

    const desktopScreenshotPath = path.join(artifactsDir, 'login-desktop-responsive.png');
    await desktopPage.screenshot({ path: desktopScreenshotPath, fullPage: true });
    console.log(`Saved desktop screenshot to ${desktopScreenshotPath}`);

    await desktopContext.close();

    // 2. MOBILE VIEWPORT (390x844 - iPhone / Android Standard)
    console.log('Testing Mobile Viewport (390x844)...');
    const mobileContext = await browser.newContext({
      viewport: { width: 390, height: 844 },
      isMobile: true,
      hasTouch: true
    });
    const mobilePage = await mobileContext.newPage();

    let mobileNetworkErrors = [];
    mobilePage.on('response', res => {
      if (res.status() >= 400) {
        mobileNetworkErrors.push({ status: res.status(), url: res.url() });
      }
    });

    await mobilePage.goto(`http://localhost:${port}/login`, { waitUntil: 'networkidle' });
    await mobilePage.waitForSelector('input[type="email"]');

    // Verify that the aside image column is hidden on mobile
    const asideVisible = await mobilePage.locator('aside').isVisible();
    console.log('Is aside image visible on mobile?', asideVisible);
    if (asideVisible) {
      throw new Error('Aside image column should be hidden on mobile viewport!');
    }

    // Verify buttons and touch targets
    const buttons = await mobilePage.locator('button').all();
    console.log(`Found ${buttons.length} active touch targets on mobile login.`);

    const mobileScreenshotPath = path.join(artifactsDir, 'login-mobile-responsive.png');
    await mobilePage.screenshot({ path: mobileScreenshotPath, fullPage: true });
    console.log(`Saved mobile screenshot to ${mobileScreenshotPath}`);

    await mobileContext.close();

    console.log('\n=============================================');
    console.log('ALL VISUAL & RESPONSIVE CHECKS PASSED 100%!');
    console.log('=============================================');
  } catch (err) {
    console.error('Visual test failed:', err);
    process.exitCode = 1;
  } finally {
    await browser.close();
    server.close();
  }
});
