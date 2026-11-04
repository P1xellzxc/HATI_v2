// Pixel inspector: measures alignment, spacing rhythm, sizing and balance on every main view.
// Usage: URL=http://127.0.0.1:PORT/ node web/tests/pixel.js [--mode dark] [--width 360]
// Prints findings with the element, the measured numbers and the view. Exit 0 always (it informs, the auditor judges).
const { chromium, devices } = require('playwright');
const URL = process.env.URL || 'http://127.0.0.1:8765/';
const arg = (k, d) => { const i = process.argv.indexOf(k); return i > 0 ? process.argv[i + 1] : d; };
const MODE = arg('--mode', 'light'), WIDTH = +arg('--width', 390);
(async () => {
  const b = await chromium.launch();
  const p = await (await b.newContext({ ...devices['iPhone 13'], viewport: { width: WIDTH, height: 844 } })).newPage();
  await p.goto(URL);
  const views = ['hub', 'volume', 'history', 'expense', 'settings'];
  for (const view of views) {
    const out = await p.evaluate(([view, mode]) => {
      S.settings.mode = mode; applyTheme();
      if (view === 'expense') { ui.volId = S.volumes[0].id; ui.view = 'volume'; openExpense(null); }
      else { if (view === 'volume' || view === 'history') ui.volId = S.volumes[0].id; go(view); }
      const F = [];
      const box = el => el.getBoundingClientRect();
      const name = el => el.tagName.toLowerCase() + (el.id ? '#' + el.id : '') + (el.className && typeof el.className === 'string' ? '.' + el.className.trim().split(/\s+/).slice(0, 2).join('.') : '') + ' "' + (el.textContent || '').trim().slice(0, 18) + '"';
      // 1. Left edges of sibling blocks inside the same container should match (or differ by a deliberate step).
      document.querySelectorAll('#app .stack, #app section, #app .ledger, #app .vol-info, #app form').forEach(c => {
        const kids = [...c.children].filter(k => box(k).width > 0 && getComputedStyle(k).position !== 'absolute');
        const lefts = kids.map(k => Math.round(box(k).left));
        const uniq = [...new Set(lefts)];
        if (uniq.length > 1 && Math.max(...uniq) - Math.min(...uniq) <= 3) F.push(`misaligned left edges (${uniq.join('/')}px) in ${name(c)}`);
      });
      // 2. Sibling gaps in a vertical stack should be equal.
      document.querySelectorAll('#app .stack, #app .ledger').forEach(c => {
        const kids = [...c.children].filter(k => box(k).height > 0);
        const gaps = kids.slice(1).map((k, i) => Math.round(box(k).top - box(kids[i]).bottom));
        const uniq = [...new Set(gaps)];
        if (uniq.length > 1 && Math.max(...uniq) - Math.min(...uniq) >= 2) F.push(`uneven gaps (${gaps.join(',')}px) in ${name(c)}`);
      });
      // 3. Text baselines in a row: items in a flex row should share a vertical center.
      document.querySelectorAll('#app .row, #app .tx, #app .debt, #app .chip').forEach(r => {
        const kids = [...r.children].filter(k => box(k).height > 0 && box(k).height < 80);
        if (kids.length < 2) return;
        const mids = kids.map(k => Math.round(box(k).top + box(k).height / 2));
        if (Math.max(...mids) - Math.min(...mids) > 3 && getComputedStyle(r).alignItems === 'center') F.push(`off-center items (${mids.join('/')}px) in ${name(r)}`);
      });
      // 4. Off-scale spacing: padding/margin/gap values not on the 2/4 px grid.
      const seen = new Set();
      document.querySelectorAll('#app *').forEach(el => {
        const cs = getComputedStyle(el);
        for (const k of ['paddingTop', 'paddingLeft', 'marginTop', 'rowGap']) {
          const v = parseFloat(cs[k]); if (v && v % 2 !== 0 && !seen.has(el.className + k)) { seen.add(el.className + k); F.push(`off-grid ${k}=${v}px on ${name(el)}`); }
        }
      });
      // 5. Tap targets under 44px and text truncated by accident.
      document.querySelectorAll('#app button, #app a, #app input, #app select, #nav button').forEach(el => {
        const r = box(el); if (r.width && (r.height < 44 && !el.matches('input[type=checkbox]'))) F.push(`small tap target ${Math.round(r.width)}×${Math.round(r.height)} ${name(el)}`);
      });
      document.querySelectorAll('#app *').forEach(el => {
        if (el.closest('.sr, .hero, .sw')) return; // screen-reader text and the screentone panel clip on purpose
        if (el.scrollWidth > el.clientWidth + 1 && getComputedStyle(el).textOverflow !== 'ellipsis' && getComputedStyle(el).overflow === 'hidden') F.push(`clipped text without ellipsis ${name(el)}`);
      });
      // 6. Icons centered in their boxes.
      document.querySelectorAll('#app .ico svg, #app .tx-ic svg, #app .logo svg').forEach(svg => {
        const a = box(svg), c = box(svg.parentElement);
        const dx = Math.abs((a.left + a.width / 2) - (c.left + c.width / 2)), dy = Math.abs((a.top + a.height / 2) - (c.top + c.height / 2));
        if (dx > 1 || dy > 1) F.push(`icon off-center by ${dx.toFixed(1)},${dy.toFixed(1)}px in ${name(svg.parentElement)}`);
      });
      // 7. Balance: horizontal center of mass of visible text vs page center.
      let wsum = 0, xsum = 0;
      document.querySelectorAll('#app *').forEach(el => { if (![...el.childNodes].some(n => n.nodeType === 3 && n.textContent.trim())) return; const r = box(el); const w = r.width * r.height; wsum += w; xsum += w * (r.left + r.width / 2); });
      const skew = Math.round(xsum / wsum - innerWidth / 2);
      if (Math.abs(skew) > innerWidth * 0.12) F.push(`visual weight skewed ${skew > 0 ? 'right' : 'left'} by ${Math.abs(skew)}px`);
      return F;
    }, [view, MODE]);
    console.log(`\n== ${view} (${MODE}, ${WIDTH}px): ${out.length} findings`);
    out.slice(0, 40).forEach(f => console.log(' - ' + f));
  }
  await b.close();
})();
