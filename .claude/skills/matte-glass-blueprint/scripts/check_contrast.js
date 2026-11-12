#!/usr/bin/env node
// Text contrast check for a running matte glass app (Playwright).
// Every visible text node is measured against the first opaque background behind it (walking up the tree;
// glass bodies must end their `background` with an opaque base colour, which is what this reads).
// Usage: URL=http://localhost:8000/ node check_contrast.js [--themes light,dark] [--min 4.5] [--width 390]
// Exit 1 if any text is under the minimum. Optional: set window.setTheme(name) in your app to switch themes.
const { chromium } = require('playwright');
const arg = (k, d) => { const i = process.argv.indexOf(k); return i > 0 ? process.argv[i + 1] : d; };
const URL = process.env.URL || 'http://localhost:8000/';
const themes = arg('--themes', 'light,dark').split(','), min = +arg('--min', '4.5'), width = +arg('--width', '390');

(async () => {
  const b = await chromium.launch(); let bad = 0;
  for (const theme of themes) {
    const c = await b.newContext({ viewport: { width, height: 844 }, colorScheme: theme === 'dark' ? 'dark' : 'light' });
    const p = await c.newPage(); await p.goto(URL); await p.waitForTimeout(500);
    await p.evaluate(t => { if (window.setTheme) window.setTheme(t); else document.documentElement.setAttribute('data-theme', t); }, theme);
    await p.waitForTimeout(200);
    const rows = await p.evaluate(min => {
      const parse = c => { const n = (c.match(/[\d.]+/g) || [0, 0, 0, 1]).map(Number); if (/^color\(srgb/.test(c)) { n[0] *= 255; n[1] *= 255; n[2] *= 255; } return { rgb: n.slice(0, 3), a: n.length > 3 ? n[3] : 1 }; };
      const lin = v => { v /= 255; return v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4; };
      const L = ([r, g, b]) => 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b);
      const bgOf = el => { for (let e = el; e; e = e.parentElement) { const x = parse(getComputedStyle(e).backgroundColor); if (x.a > 0.95) return x.rgb; } return parse(getComputedStyle(document.documentElement).backgroundColor).rgb; };
      const out = [], seen = new Set();
      const walk = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
      for (let n; (n = walk.nextNode());) {
        const el = n.parentElement, t = n.textContent.trim(); if (!t || seen.has(el)) continue; seen.add(el);
        const r = el.getBoundingClientRect(), cs = getComputedStyle(el);
        if (!r.width || !r.height || cs.visibility === 'hidden' || +cs.opacity === 0) continue;
        const fg = L(parse(cs.color).rgb), bg = L(bgOf(el)), k = (Math.max(fg, bg) + 0.05) / (Math.min(fg, bg) + 0.05);
        if (k < min) out.push({ text: t.slice(0, 32), cls: el.className && String(el.className).slice(0, 30), ratio: +k.toFixed(2) });
      }
      return out.sort((a, b) => a.ratio - b.ratio);
    }, min);
    if (rows.length) { bad += rows.length; console.log(`FAIL ${theme}: ${rows.length} text(s) under ${min}:1`); console.table(rows.slice(0, 20)); }
    else console.log(`PASS ${theme}: all visible text ≥ ${min}:1`);
    await c.close();
  }
  await b.close(); process.exit(bad ? 1 : 0);
})();
