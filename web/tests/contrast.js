// Measured WCAG contrast of rendered text against its real background, for every palette in light and dark.
// Usage: URL=http://127.0.0.1:PORT/ node web/tests/contrast.js   → table; exit code 1 if any text is under 4.5:1
const { chromium, devices } = require('playwright');
const URL = process.env.URL || 'http://127.0.0.1:8765/';
(async () => {
  const b = await chromium.launch(); const p = await (await b.newContext({ ...devices['iPhone 13'] })).newPage();
  await p.goto(URL);
  const ids = await p.evaluate(() => PALETTES.map(x => x.id));
  let worst = [], fail = false;
  for (const id of ids) for (const mode of ['light', 'dark']) for (const view of ['hub', 'volume', 'expense', 'settings']) {
    const res = await p.evaluate(([id, mode, view]) => {
      S.settings.palette = id; S.settings.mode = mode; applyTheme();
      if (view === 'expense') { ui.volId = S.volumes[0].id; ui.view = 'volume'; openExpense(null); }
      else { if (view !== 'hub' && view !== 'settings') ui.volId = S.volumes[0].id; go(view); }
      const parse = c => { const m = c.match(/[\d.]+/g); if (!m) return [0, 0, 0, 0]; const n = m.map(Number); if (/^color\(srgb/.test(c)) { n[0] *= 255; n[1] *= 255; n[2] *= 255; } return n; }; // color-mix() computes to color(srgb r g b [/ a]) with 0..1 channels
      const lin = v => { v /= 255; return v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4; };
      const L = ([r, g, b]) => 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b);
      const bgOf = el => { for (let e = el; e; e = e.parentElement) { const c = parse(getComputedStyle(e).backgroundColor); if (c.length < 4 || c[3] > 0.95) return c; } return parse(getComputedStyle(document.body).backgroundColor); };
      const out = [];
      document.querySelectorAll('#app *, #nav *').forEach(el => {
        if (!el.childNodes.length || ![...el.childNodes].some(n => n.nodeType === 3 && n.textContent.trim())) return;
        const r = el.getBoundingClientRect(); if (!r.width || !r.height) return;
        const fg = parse(getComputedStyle(el).color), bg = bgOf(el);
        const a = L(fg), c = L(bg), ratio = (Math.max(a, c) + 0.05) / (Math.min(a, c) + 0.05);
        out.push({ text: el.textContent.trim().slice(0, 24), cls: el.className && String(el.className).slice(0, 30), ratio: Math.round(ratio * 100) / 100 });
      });
      return out.sort((x, y) => x.ratio - y.ratio).slice(0, 3);
    }, [id, mode, view]);
    for (const r of res) { if (r.ratio < 4.5) fail = true; worst.push({ palette: id, mode, view, ...r }); }
  }
  worst.sort((x, y) => x.ratio - y.ratio);
  console.table(worst.slice(0, 25));
  console.log(fail ? 'FAIL: some text is under 4.5:1' : 'PASS: all measured text is at least 4.5:1');
  await b.close(); process.exit(fail ? 1 : 0);
})();
