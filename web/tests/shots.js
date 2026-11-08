// Standard screenshot set for every role: views, sheets and states at 360/390 px, light/dark, four palettes (ALL_DARK=1: all 13 in dark).
// Usage: URL=http://127.0.0.1:PORT/ node web/tests/shots.js <out-dir>   → <out-dir>/*.png + index.html contact sheet
const { chromium, devices } = require('playwright');
const fs = require('fs'), path = require('path');
const URL = process.env.URL || 'http://127.0.0.1:8765/';
const OUT = process.argv[2] || 'shots';
fs.mkdirSync(OUT, { recursive: true });

const VIEWS = {
  hub: p => p.evaluate(() => go('hub')),
  volume: p => p.evaluate(() => { ui.volId = S.volumes[0].id; go('volume'); }),
  history: p => p.evaluate(() => { ui.volId = S.volumes[0].id; go('history'); }),
  expense: p => p.evaluate(() => { ui.volId = S.volumes[0].id; ui.view = 'volume'; openExpense(null); }),
  'expense-exact': p => p.evaluate(() => { ui.volId = S.volumes[0].id; ui.view = 'volume'; openExpense(null); }).then(() => p.fill('#f-amount', '2400')).then(() => p.click('[data-split="exact"]')),
  settings: p => p.evaluate(() => go('settings')),
  'sheet-settle': p => p.evaluate(() => { ui.volId = S.volumes[0].id; go('volume'); settleSheet(null); }),
  'sheet-new-volume': p => p.evaluate(() => { go('hub'); volumeSheet(null); }),
  'sheet-share': p => p.evaluate(async () => { ui.volId = S.volumes[0].id; go('volume'); await shareSheet(); }),
  'sheet-share-phone': p => p.evaluate(async () => { navigator.share = async () => {}; ui.volId = S.volumes[0].id; go('volume'); await shareSheet(); }),
  'sheet-share-code': p => p.evaluate(async () => { ui.volId = S.volumes[0].id; go('volume'); await shareSheet(); const d = $('#s-codebox'); if (d) { d.open = true; d.scrollIntoView({ block: 'center' }); } }),
  'sheet-join': p => p.evaluate(async () => { const l = await inviteLink(S.volumes[0]); S.volumes = []; save(); go('hub'); location.hash = l.slice(l.indexOf('#')); }).then(() => p.waitForTimeout(500)),
  'sheet-import': p => p.evaluate(() => { go('hub'); importSheet(); }),
  'sheet-member-used': p => p.evaluate(() => { ui.volId = S.volumes[0].id; go('volume'); editMemberSheet(S.volumes[0].members[1]); }),
  'sheet-import-merge': p => p.evaluate(() => {
    const v = JSON.parse(JSON.stringify(S.volumes[0])); v.expenses.push({ ...v.expenses[0], id: 'fromb1', desc: 'From B', upd: Date.now() }); v.expenses.splice(1, 1);
    go('hub'); importSheet(); $('#s-paste').value = JSON.stringify({ app: 'hati2-mock', v: 1, volume: v });
  }).then(() => p.click('#s-go')).then(() => p.waitForTimeout(600)), // Import is disabled for 500 ms after Check code
  'settings-data': p => p.evaluate(() => { go('settings'); window.scrollTo(0, document.body.scrollHeight); }),
  'state-empty': p => p.evaluate(() => { S.volumes = []; save(); go('hub'); }),
  'state-one-member': p => p.evaluate(() => { const id = 'solo' + 1; S.volumes.unshift({ id: 'solo1', title: 'Solo test', type: 'other', currency: '$', meId: 'me1', created: 1, members: [{ id: 'me1', name: 'Me' }], expenses: [], settlements: [] }); save(); ui.volId = 'solo1'; go('volume'); }),
  'state-long-text': p => p.evaluate(() => {
    const v = exampleVolume(); v.title = 'Bartholomew and friends: summer road trip 2026'.slice(0, 40);
    v.members[1].name = 'Maximilian Featherstonehaugh-Wolfe'; v.expenses[0].desc = 'Very long description of a dinner at a place with an extremely long name that keeps going'.slice(0, 100);
    v.expenses[0].amount = 99999999999; v.expenses[0].splits = splitEqual(99999999999, v.members.map(m => m.id));
    S.volumes.unshift(v); save(); ui.volId = v.id; go('volume');
  }),
};
const FULL = ['classic'], PARTIAL_VIEWS = ['hub', 'volume', 'expense', 'sheet-settle', 'sheet-share'];
// ALL_DARK=1: every palette in dark at 360 only (dark contact sheet for before/after); default: three palettes plus spring (brightest accent).
const ALL_DARK = !!process.env.ALL_DARK;
const PARTIAL = ALL_DARK ? ['persimmon', 'lantern', 'vermilion', 'moss', 'fern', 'spring', 'lagoon', 'patina', 'harbor', 'wisteria', 'peony', 'coral'] : ['persimmon', 'lagoon', 'spring'];
const MODES = ALL_DARK ? ['dark'] : ['light', 'dark'], WIDTHS = ALL_DARK ? [360] : [360, 390];

(async () => {
  const b = await chromium.launch();
  const shots = [];
  for (const pal of [...FULL, ...PARTIAL]) for (const mode of MODES) for (const w of WIDTHS) {
    const ctx = await b.newContext({ ...devices['iPhone 13'], viewport: { width: w, height: 780 }, deviceScaleFactor: 1 });
    const p = await ctx.newPage();
    const errors = []; p.on('pageerror', e => errors.push(e.message));
    await p.goto(URL);
    for (const [name, open] of Object.entries(VIEWS)) {
      if ((ALL_DARK || !FULL.includes(pal)) && !PARTIAL_VIEWS.includes(name)) continue;
      await p.evaluate(([pal, mode]) => { localStorage.clear(); location.hash = ''; }, [pal, mode]);
      await p.reload();
      await p.evaluate(([pal, mode]) => { S.settings.palette = pal; S.settings.mode = mode; save(); applyTheme(); }, [pal, mode]);
      await open(p); await p.waitForTimeout(120);
      const file = `${pal}-${mode}-${w}-${name}.png`;
      await p.screenshot({ path: path.join(OUT, file) });
      shots.push({ file, pal, mode, w, name });
    }
    if (errors.length) console.log(`PAGE ERRORS ${pal}/${mode}/${w}: ${errors.join(' | ')}`);
    await ctx.close();
  }
  await b.close();
  const rows = shots.map(s => `<figure><img src="${s.file}" loading="lazy"><figcaption>${s.name} · ${s.pal} · ${s.mode} · ${s.w}</figcaption></figure>`).join('\n');
  fs.writeFileSync(path.join(OUT, 'index.html'), `<!doctype html><meta charset="utf-8"><title>HATI² shots</title><style>body{font:12px system-ui;margin:12px;display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:12px}img{width:100%;border:1px solid #999}</style>${rows}`);
  console.log(`${shots.length} screenshots in ${OUT} (open index.html for the contact sheet)`);
})();
