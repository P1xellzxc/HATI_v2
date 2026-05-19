#!/usr/bin/env node
/*
 * Render HATI² screen mockups to 1080x2400 PNGs using Playwright.
 *
 * Usage:
 *   node tools/screenshots/render.js
 *
 * Outputs into docs/screenshots/.
 */
const path = require('path');
const fs = require('fs');
const { chromium } = require('playwright');
const { SCREENS, BASE_CSS } = require('./screens');

const OUT_DIR = path.join(__dirname, '..', '..', 'docs', 'screenshots');

(async () => {
  fs.mkdirSync(OUT_DIR, { recursive: true });

  const browser = await chromium.launch();
  const context = await browser.newContext({
    viewport: { width: 1080, height: 2400 },
    deviceScaleFactor: 1,
  });
  const page = await context.newPage();

  for (const [name, builder] of Object.entries(SCREENS)) {
    const html = `<!doctype html><html><head>
      <meta charset="utf-8"/>
      <style>${BASE_CSS}</style>
    </head><body>${builder()}</body></html>`;

    await page.setContent(html, { waitUntil: 'load' });
    const outPath = path.join(OUT_DIR, `${name}.png`);
    await page.screenshot({
      path: outPath,
      clip: { x: 0, y: 0, width: 1080, height: 2400 },
    });
    console.log(`wrote ${outPath}`);
  }

  await browser.close();
})().catch(err => { console.error(err); process.exit(1); });
