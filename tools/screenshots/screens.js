/*
 * HATI² screen mockup renderer.
 *
 * The remote container has no Android SDK and no emulator, so we
 * cannot capture real device frames from the running app. Instead,
 * this file recreates each Compose screen in HTML/CSS using the
 * exact tokens from app/src/main/java/com/hativ2/ui/theme/Color.kt
 * and the copy from app/src/main/res/values/strings.xml.
 *
 * Output: 1080x2400 PNGs matching the real hub_final.png aspect ratio.
 */

const PALETTE = {
  NotionRed: "#FECACA",
  NotionBlue: "#BFDBFE",
  NotionYellow: "#FEF08A",
  NotionGreen: "#BBF7D0",
  NotionPurple: "#E9D5FF",
  NotionPink: "#FBCFE8",
  NotionOrange: "#FED7AA",
  NotionGray: "#E5E7EB",
  NotionWhite: "#FFFFFF",
  NotionMuted: "#6B7280",
  NotionDivider: "#D1D5DB",
  NotionDisabled: "#9CA3AF",
  MangaBlack: "#000000",
  MangaSuccess: "#22C55E",
  SurfaceHero: "#FFF5D6",
  AccentCategoryFood: "#FFC9A8",
  NotionRedDeep: "#EF4444",
};

const BASE_CSS = `
  :root {
    --black: ${PALETTE.MangaBlack};
    --muted: ${PALETTE.NotionMuted};
    --divider: ${PALETTE.NotionDivider};
    --hero: ${PALETTE.SurfaceHero};
    --yellow: ${PALETTE.NotionYellow};
    --green: ${PALETTE.NotionGreen};
    --red: ${PALETTE.NotionRed};
    --blue: ${PALETTE.NotionBlue};
    --purple: ${PALETTE.NotionPurple};
    --pink: ${PALETTE.NotionPink};
    --orange: ${PALETTE.NotionOrange};
    --gray: ${PALETTE.NotionGray};
    --food: ${PALETTE.AccentCategoryFood};
    --red-deep: ${PALETTE.NotionRedDeep};
  }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  html, body {
    width: 1080px;
    height: 2400px;
    background: #FFFFFF;
    font-family: 'Inter', 'Segoe UI', system-ui, -apple-system, sans-serif;
    color: var(--black);
    -webkit-font-smoothing: antialiased;
  }
  .phone {
    width: 1080px;
    height: 2400px;
    background: #FFFFFF;
    padding: 0;
    position: relative;
    overflow: hidden;
  }
  /* Android-style status bar */
  .status-bar {
    height: 88px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 56px;
    font-size: 32px;
    font-weight: 600;
    color: var(--black);
  }
  .status-bar .icons { display: flex; gap: 16px; align-items: center; font-size: 30px; }
  .status-bar .icons svg { width: 36px; height: 36px; }
  .top-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 32px 24px 32px;
    border-bottom: 4px solid var(--black);
  }
  .top-bar.no-border { border-bottom: none; }
  .top-bar .left { display: flex; align-items: center; gap: 24px; }
  .logo-tile {
    width: 88px;
    height: 88px;
    background: var(--black);
    color: white;
    border-radius: 12px;
    border: 4px solid var(--black);
    display: grid;
    place-items: center;
    font-size: 48px;
    font-weight: 900;
  }
  .top-title {
    font-size: 56px;
    font-weight: 900;
    letter-spacing: 2px;
  }
  .top-title small {
    display: block;
    font-size: 26px;
    color: var(--muted);
    font-weight: 600;
    letter-spacing: 1px;
  }
  .icon-btn {
    width: 88px;
    height: 88px;
    border-radius: 12px;
    border: 4px solid var(--black);
    background: white;
    display: grid;
    place-items: center;
    box-shadow: 8px 8px 0 var(--black);
    font-size: 38px;
  }
  .pill-btn {
    border-radius: 12px;
    border: 4px solid var(--black);
    background: var(--yellow);
    padding: 18px 28px;
    display: inline-flex;
    align-items: center;
    gap: 12px;
    font-weight: 900;
    box-shadow: 8px 8px 0 var(--black);
    font-size: 28px;
    letter-spacing: 1px;
  }
  .headline {
    font-size: 64px;
    font-weight: 900;
    text-align: center;
    margin: 36px 32px 12px 32px;
    letter-spacing: 1.5px;
  }
  .subhead {
    font-size: 28px;
    color: var(--muted);
    text-align: center;
    line-height: 1.4;
    padding: 0 64px;
    margin-bottom: 36px;
  }

  /* Volume card */
  .vol-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 36px;
    padding: 0 32px;
  }
  .vol-card-wrap { position: relative; padding: 0 8px 8px 0; }
  .vol-card-wrap::before {
    content: "";
    position: absolute;
    inset: 8px -8px -8px 8px;
    background: var(--black);
    border-radius: 16px;
    z-index: 0;
  }
  .vol-card {
    position: relative;
    z-index: 1;
    background: white;
    border-radius: 16px;
    border: 4px solid var(--black);
    overflow: hidden;
  }
  .vol-cover {
    background: #F4F4F5;
    height: 320px;
    position: relative;
    padding: 18px;
    border-bottom: 4px solid var(--black);
  }
  .vol-cover.travel { background: #DBEAFE; }
  .vol-cover.house { background: #FEF3C7; }
  .vol-cover.event { background: #FCE7F3; }
  .vol-cover .controls {
    display: flex;
    justify-content: space-between;
  }
  .vol-cover .controls .btn {
    width: 64px;
    height: 64px;
    border: 4px solid var(--black);
    border-radius: 12px;
    background: white;
    display: grid;
    place-items: center;
    font-size: 26px;
    box-shadow: 6px 6px 0 var(--black);
  }
  .vol-cover .controls .btn.dot { font-weight: 900; }
  .vol-cover .hero-emoji {
    position: absolute;
    inset: 0;
    display: grid;
    place-items: center;
    font-size: 130px;
  }
  .vol-cover .balance {
    position: absolute;
    right: 14px;
    bottom: 14px;
    padding: 6px 14px;
    border: 4px solid var(--black);
    border-radius: 10px;
    background: rgba(187,247,208,0.55);
    font-weight: 900;
    font-size: 30px;
  }
  .vol-cover .balance.neg { background: rgba(254,202,202,0.6); color: var(--black); }
  .vol-info { padding: 22px 24px 24px 24px; }
  .vol-info .title {
    font-size: 38px;
    font-weight: 900;
    letter-spacing: 1px;
  }
  .vol-info .meta {
    font-size: 22px;
    color: var(--muted);
    margin-top: 6px;
  }
  .vol-info hr {
    margin: 18px 0;
    border: none;
    border-top: 2px solid var(--black);
  }
  .vol-info .total {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
  }
  .vol-info .total span { color: var(--muted); font-size: 24px; }
  .vol-info .total strong { font-size: 30px; font-weight: 800; }

  /* Dashed empty card */
  .dashed {
    border: 5px dashed var(--black);
    border-radius: 16px;
    background: #FAFAFA;
    padding: 56px 24px;
    display: grid;
    place-items: center;
    text-align: center;
  }
  .dashed .plus {
    width: 110px;
    height: 110px;
    border: 4px solid var(--black);
    border-radius: 12px;
    background: #F4F4F5;
    display: grid;
    place-items: center;
    font-size: 60px;
    margin-bottom: 18px;
  }
  .dashed .label {
    font-weight: 900;
    font-size: 30px;
    letter-spacing: 1px;
  }
  .dashed .helper { color: var(--muted); font-size: 22px; margin-top: 4px; }

  /* Fab */
  .fab {
    position: absolute;
    right: 40px;
    bottom: 64px;
    background: var(--yellow);
    border: 4px solid var(--black);
    border-radius: 18px;
    padding: 26px 36px;
    box-shadow: 12px 12px 0 var(--black);
    display: inline-flex;
    align-items: center;
    gap: 16px;
    font-weight: 900;
    font-size: 34px;
  }

  /* Shared card with hard shadow */
  .card {
    position: relative;
    background: white;
    border: 4px solid var(--black);
    border-radius: 16px;
    padding: 24px;
    box-shadow: 10px 10px 0 var(--black);
  }
  .card .label-row {
    display: flex;
    align-items: center;
    gap: 12px;
    font-weight: 900;
    font-size: 24px;
    letter-spacing: 1px;
  }
  .row { display: flex; }
  .row.between { justify-content: space-between; align-items: center; }
  .row.center { align-items: center; }
  .row.gap-12 { gap: 24px; }
  .stack > * + * { margin-top: 24px; }
  .h1 { font-size: 52px; font-weight: 900; letter-spacing: 1px; }
  .h2 { font-size: 38px; font-weight: 900; }
  .h3 { font-size: 28px; font-weight: 800; }
  .muted { color: var(--muted); }
  .small { font-size: 22px; }
  .tiny { font-size: 20px; }

  /* Balance hero */
  .balance-hero {
    display: grid;
    place-items: center;
    padding: 16px;
  }
  .balance-hero .label { color: var(--muted); font-size: 26px; }
  .balance-hero .pill {
    background: var(--hero);
    border: 4px solid var(--black);
    border-radius: 16px;
    padding: 26px 60px;
    margin: 20px 0;
    font-size: 88px;
    font-weight: 900;
  }
  .balance-hero .sub { color: var(--muted); font-size: 24px; }

  /* Bottom bar */
  .bottom-bar {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    border-top: 4px solid var(--black);
    background: white;
    padding: 22px 28px 36px 28px;
    display: flex;
    justify-content: space-around;
    align-items: center;
  }
  .bottom-bar .item {
    text-align: center;
    font-size: 22px;
    font-weight: 700;
  }
  .bottom-bar .icon {
    width: 80px;
    height: 80px;
    border: 4px solid var(--black);
    border-radius: 12px;
    background: white;
    display: grid;
    place-items: center;
    font-size: 36px;
    margin: 0 auto 8px;
    box-shadow: 6px 6px 0 var(--black);
  }
  .bottom-bar .item.add .icon { background: var(--yellow); }

  /* Transaction card */
  .tx {
    display: flex;
    align-items: center;
    gap: 22px;
    background: white;
    border: 4px solid var(--black);
    border-radius: 16px;
    padding: 22px 24px;
    box-shadow: 8px 8px 0 var(--black);
  }
  .tx .avatar {
    width: 84px;
    height: 84px;
    border: 4px solid var(--black);
    border-radius: 14px;
    background: var(--yellow);
    display: grid;
    place-items: center;
    font-size: 34px;
    font-weight: 900;
  }
  .tx .body { flex: 1; }
  .tx .body .t { font-weight: 800; font-size: 30px; }
  .tx .body .s { color: var(--muted); font-size: 22px; margin-top: 4px; }
  .tx .right { text-align: right; }
  .tx .right .a { font-size: 30px; font-weight: 900; color: var(--black); }
  .tx .right .a.pos { color: #16A34A; }
  .tx .right .d { color: var(--muted); font-size: 22px; margin-top: 4px; }

  /* Chip */
  .chip {
    border: 4px solid var(--black);
    border-radius: 14px;
    padding: 16px 22px;
    background: white;
    font-weight: 800;
    font-size: 24px;
    display: inline-flex;
    align-items: center;
    gap: 10px;
  }
  .chip.sel { background: var(--food); }

  /* Field */
  .field {
    border: 4px solid var(--black);
    border-radius: 14px;
    background: white;
    padding: 22px 24px;
    box-shadow: 6px 6px 0 var(--black);
  }
  .field .lab { color: var(--muted); font-size: 22px; font-weight: 700; }
  .field .val { font-size: 32px; font-weight: 700; margin-top: 6px; }
  .field .val.placeholder { color: var(--muted); font-weight: 500; }

  /* Pie/bar */
  .pie {
    width: 360px;
    height: 360px;
    border-radius: 50%;
    border: 4px solid var(--black);
    background: conic-gradient(
      var(--food) 0 36%,
      var(--blue) 36% 60%,
      var(--purple) 60% 78%,
      var(--green) 78% 90%,
      var(--pink) 90% 100%
    );
    position: relative;
  }
  .pie::after {
    content: "";
    position: absolute;
    inset: 60px;
    background: white;
    border-radius: 50%;
    border: 4px solid var(--black);
  }
  .legend-row { display: flex; align-items: center; gap: 14px; font-size: 24px; margin: 8px 0; }
  .legend-row .sw { width: 32px; height: 32px; border: 3px solid var(--black); border-radius: 6px; }

  /* Trend bars */
  .bars { display: flex; align-items: flex-end; gap: 26px; height: 360px; padding-top: 16px; }
  .bars .bar {
    flex: 1;
    background: var(--food);
    border: 4px solid var(--black);
    border-radius: 10px 10px 0 0;
    position: relative;
  }
  .bars .lab {
    position: absolute;
    bottom: -42px;
    left: 0;
    right: 0;
    text-align: center;
    font-size: 22px;
    font-weight: 700;
    color: var(--muted);
  }
  .bars .top {
    position: absolute;
    top: -42px;
    left: 0;
    right: 0;
    text-align: center;
    font-size: 20px;
    font-weight: 800;
  }

  /* Search bar */
  .search {
    border: 4px solid var(--black);
    border-radius: 14px;
    background: white;
    padding: 22px 26px;
    display: flex;
    align-items: center;
    gap: 16px;
    box-shadow: 6px 6px 0 var(--black);
    font-size: 26px;
  }

  /* Two-up small summary */
  .two-up { display: grid; grid-template-columns: 1fr 1fr; gap: 28px; }
  .sum-card {
    position: relative;
    border: 4px solid var(--black);
    border-radius: 16px;
    overflow: hidden;
    background: white;
    box-shadow: 10px 10px 0 var(--black);
  }
  .sum-card .head {
    padding: 16px 22px;
    border-bottom: 4px solid var(--black);
    font-weight: 900;
    font-size: 24px;
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .sum-card.pos .head { background: rgba(187,247,208,0.55); }
  .sum-card.neg .head { background: rgba(251,207,232,0.7); }
  .sum-card .body { padding: 20px 22px; }
  .sum-card .amt { font-size: 40px; font-weight: 900; }
  .sum-card .amt.pos { color: #16A34A; }
  .sum-card .amt.neg { color: #DC2626; }
  .sum-card .detail { display: flex; justify-content: space-between; font-size: 22px; margin-top: 6px; }
  .sum-card .settle {
    margin-top: 14px;
    text-align: center;
    background: var(--yellow);
    border: 2px solid var(--black);
    padding: 10px;
    border-radius: 10px;
    font-weight: 900;
    font-size: 22px;
  }
`;

function statusBar() {
  return `
  <div class="status-bar">
    <div>9:41</div>
    <div class="icons">
      <span>📶</span><span>📡</span><span>🔋</span>
    </div>
  </div>`;
}

function hubScreen() {
  return `
  <div class="phone">
    ${statusBar()}
    <div class="top-bar">
      <div class="left">
        <div class="logo-tile">H</div>
        <div class="top-title">HATI</div>
      </div>
      <div style="display:flex; align-items:center; gap:20px;">
        <div class="icon-btn">ⓘ</div>
        <div class="pill-btn">+ NEW VOLUME</div>
      </div>
    </div>
    <div class="headline">YOUR STORY VOLUMES</div>
    <div class="subhead">Each volume contains a separate expense arc. Perfect for trips, projects, or events.</div>

    <div class="vol-grid">
      <div class="vol-card-wrap">
        <div class="vol-card">
          <div class="vol-cover travel">
            <div class="controls">
              <div class="btn">✎</div>
              <div class="btn">🗑</div>
            </div>
            <div class="hero-emoji">✈️</div>
            <div class="balance">+₱1,240</div>
          </div>
          <div class="vol-info">
            <div class="title">BAGUIO</div>
            <div class="meta">✈️ Travel · 7 ch.</div>
            <hr/>
            <div class="total"><span>Total</span><strong>₱8,520.00</strong></div>
          </div>
        </div>
      </div>
      <div class="vol-card-wrap">
        <div class="vol-card">
          <div class="vol-cover house">
            <div class="controls">
              <div class="btn">✎</div>
              <div class="btn">🗑</div>
            </div>
            <div class="hero-emoji">🏠</div>
            <div class="balance neg">-₱360</div>
          </div>
          <div class="vol-info">
            <div class="title">CONDO</div>
            <div class="meta">🏠 Household · 12 ch.</div>
            <hr/>
            <div class="total"><span>Total</span><strong>₱14,200.00</strong></div>
          </div>
        </div>
      </div>
      <div class="vol-card-wrap">
        <div class="vol-card">
          <div class="vol-cover event">
            <div class="controls">
              <div class="btn">✎</div>
              <div class="btn">🗑</div>
            </div>
            <div class="hero-emoji">🎉</div>
            <div class="balance">+₱500</div>
          </div>
          <div class="vol-info">
            <div class="title">BIRTHDAY</div>
            <div class="meta">🎉 Event · 4 ch.</div>
            <hr/>
            <div class="total"><span>Total</span><strong>₱3,150.00</strong></div>
          </div>
        </div>
      </div>
      <div class="dashed">
        <div class="plus">+</div>
        <div class="label">NEW VOLUME</div>
        <div class="helper">Start a new arc</div>
      </div>
    </div>

    <div class="fab">+ NEW VOLUME</div>
  </div>`;
}

function dashboardScreen() {
  return `
  <div class="phone">
    ${statusBar()}
    <div class="top-bar">
      <div class="left">
        <div class="icon-btn">←</div>
        <div class="top-title">BAGUIO<small>Travel Arc</small></div>
      </div>
      <div class="icon-btn">↗</div>
    </div>

    <div style="padding: 32px;">
      <div class="row gap-12" style="gap:24px;">
        <div class="card" style="flex:1; box-shadow: 8px 8px 0 var(--black); padding: 28px 0; text-align:center;">
          <div style="font-size:48px;">≡</div>
          <div style="font-weight:800; font-size:24px; margin-top:8px;">HISTORY</div>
        </div>
        <div class="card" style="flex:1; box-shadow: 8px 8px 0 var(--black); padding: 28px 0; text-align:center;">
          <div style="font-size:48px;">📅</div>
          <div style="font-weight:800; font-size:24px; margin-top:8px;">CHARTS</div>
        </div>
        <div class="card" style="flex:1; box-shadow: 8px 8px 0 var(--black); padding: 28px 0; text-align:center;">
          <div style="font-size:48px;">👤</div>
          <div style="font-weight:800; font-size:24px; margin-top:8px;">MEMBER</div>
        </div>
      </div>

      <div style="height: 32px;"></div>

      <div class="card balance-hero">
        <div class="label">Your Balance in "Baguio"</div>
        <div class="pill">+₱1,240.00</div>
        <div class="sub">Total spent: ₱8,520.00</div>
      </div>

      <div style="height: 32px;"></div>

      <div class="card">
        <div class="label-row">🛒 SPENDING BY MEMBER</div>
        <div style="height: 2px; background: rgba(0,0,0,0.15); margin: 14px 0;"></div>
        <div class="row" style="font-size:20px; font-weight:800; color:var(--muted);">
          <div style="flex:1.5;">Member</div>
          <div style="flex:1; text-align:right;">Paid</div>
          <div style="flex:1; text-align:right;">Share</div>
          <div style="flex:1; text-align:right;">Offset</div>
        </div>
        <div style="height: 2px; background: var(--black); margin: 10px 0;"></div>
        ${[
          ["You",       "₱3,520", "₱2,280", "+₱1,240"],
          ["Mika",      "₱2,500", "₱2,280", "+₱220"],
          ["Jun",       "₱1,500", "₱2,280", "-₱780"],
          ["Tala",      "₱1,000", "₱1,680", "-₱680"],
        ].map(([n,p,s,o], i) => `
          <div class="row" style="align-items:center; padding: 14px 0;">
            <div style="flex:1.5; font-weight:800; font-size:24px;">${n}</div>
            <div style="flex:1; text-align:right; font-size:22px;">${p}</div>
            <div style="flex:1; text-align:right; font-size:22px;">${s}</div>
            <div style="flex:1; text-align:right; font-weight:900; font-size:22px; color:${o.startsWith('+')?'#16A34A':'#DC2626'};">${o}</div>
          </div>
          ${i<3? '<div style="height:2px; background: var(--divider);"></div>' : ''}
        `).join("")}
      </div>

      <div style="height: 32px;"></div>

      <div class="two-up">
        <div class="sum-card pos">
          <div class="head">↙ OWED TO YOU</div>
          <div class="body">
            <div class="amt pos">₱1,460.00</div>
            <div class="detail"><span class="muted">Jun</span><strong>₱780</strong></div>
            <div class="detail"><span class="muted">Tala</span><strong>₱680</strong></div>
            <div class="settle">SETTLE UP</div>
          </div>
        </div>
        <div class="sum-card neg">
          <div class="head">↗ YOU OWE</div>
          <div class="body">
            <div class="amt neg">₱0.00</div>
            <div class="muted" style="font-size:22px;">You don't owe anyone</div>
          </div>
        </div>
      </div>

      <div style="height: 28px;"></div>

      <div class="row between" style="padding: 8px 4px;">
        <div class="label-row">≡ RECENT CHAPTERS</div>
        <div class="chip" style="padding: 8px 14px; font-size: 20px; box-shadow:none;">View All ›</div>
      </div>

      <div class="stack" style="margin-top:16px;">
        <div class="tx">
          <div class="avatar" style="background: var(--green);">Y</div>
          <div class="body"><div class="t">Jeepney + cab</div><div class="s">paid by You</div></div>
          <div class="right"><div class="a pos">₱420.00</div><div class="d">May 12</div></div>
        </div>
        <div class="tx">
          <div class="avatar" style="background: white;">M</div>
          <div class="body"><div class="t">Strawberry farm tour</div><div class="s">paid by Mika</div></div>
          <div class="right"><div class="a">₱1,200.00</div><div class="d">May 11</div></div>
        </div>
        <div class="tx">
          <div class="avatar" style="background: var(--blue);">✓</div>
          <div class="body"><div class="t">Settlement</div><div class="s">Jun → You</div></div>
          <div class="right"><div class="a">₱780.00</div><div class="d">May 10</div></div>
        </div>
      </div>
    </div>
  </div>`;
}

function addExpenseScreen() {
  return `
  <div class="phone">
    ${statusBar()}
    <div class="top-bar">
      <div class="left">
        <div class="icon-btn">←</div>
        <div class="top-title">NEW CHAPTER<small>in Baguio</small></div>
      </div>
      <div></div>
    </div>

    <div style="padding: 32px;">
      <div class="card">
        <div class="label-row">📝 EXPENSE DETAILS</div>
        <div style="height: 2px; background: rgba(0,0,0,0.12); margin: 14px 0 22px;"></div>

        <div class="field">
          <div class="lab">Description</div>
          <div class="val">Dinner at Good Taste</div>
        </div>
        <div style="height:18px"></div>
        <div class="field">
          <div class="lab">Amount</div>
          <div class="val">₱ 1,650.00</div>
        </div>
        <div style="height:18px"></div>
        <div class="field">
          <div class="lab">Paid by</div>
          <div class="val">You ▾</div>
        </div>
      </div>

      <div style="height: 32px"></div>

      <div class="card">
        <div class="label-row">🏷️ CATEGORY</div>
        <div style="display:flex; flex-wrap:wrap; gap:16px; margin-top:18px;">
          <div class="chip sel">🍽️ Food &amp; Drinks</div>
          <div class="chip">🚗 Transport</div>
          <div class="chip">🎬 Entertainment</div>
          <div class="chip">💡 Utilities</div>
          <div class="chip">🛍️ Shopping</div>
          <div class="chip">📦 Other</div>
        </div>
      </div>

      <div style="height: 32px"></div>

      <div class="card">
        <div class="row between">
          <div class="label-row">👥 SPLIT BETWEEN</div>
          <div class="chip" style="box-shadow:none; padding:8px 14px; font-size:20px;">Deselect all</div>
        </div>
        <div style="display:flex; flex-direction: column; gap: 18px; margin-top:22px;">
          ${[
            ["You",  true],
            ["Mika", true],
            ["Jun",  true],
            ["Tala", false],
          ].map(([n,sel]) => `
            <div class="row between" style="border:4px solid var(--black); border-radius:14px; padding: 18px 22px; background:${sel?'#FEF9C3':'white'}; box-shadow:6px 6px 0 var(--black);">
              <div class="row center" style="gap:18px;">
                <div style="width:56px;height:56px;border:4px solid var(--black);border-radius:12px;background:white;display:grid;place-items:center;font-weight:900;font-size:24px;">${n[0]}</div>
                <div style="font-weight:800; font-size:28px;">${n}</div>
              </div>
              <div style="width:48px;height:48px;border:4px solid var(--black);border-radius:10px;background:${sel?'var(--green)':'white'};display:grid;place-items:center;font-weight:900;font-size:26px;">${sel?'✓':''}</div>
            </div>`).join("")}
        </div>

        <div style="height: 18px"></div>
        <div class="row between" style="border-top:2px solid var(--divider); padding-top:16px;">
          <div class="muted" style="font-size:22px;">Each person pays</div>
          <div style="font-weight:900; font-size:30px;">₱ 550.00</div>
        </div>
      </div>

      <div style="height: 36px"></div>

      <div style="background: var(--yellow); border:4px solid var(--black); border-radius:16px; padding:30px; text-align:center; font-weight:900; font-size:34px; letter-spacing:1.5px; box-shadow: 10px 10px 0 var(--black);">
        ADD EXPENSE
      </div>
    </div>
  </div>`;
}

function historyScreen() {
  return `
  <div class="phone">
    ${statusBar()}
    <div class="top-bar">
      <div class="left">
        <div class="icon-btn">←</div>
        <div class="top-title">HISTORY<small>Baguio · 7 chapters</small></div>
      </div>
      <div class="icon-btn">↗</div>
    </div>

    <div style="padding: 32px;">
      <div class="search">🔍 <span class="muted">Search chapters…</span></div>
      <div style="height: 22px"></div>
      <div style="display:flex; gap:14px; overflow:hidden;">
        <div class="chip sel" style="box-shadow:none;">All</div>
        <div class="chip" style="box-shadow:none;">🍽️ Food</div>
        <div class="chip" style="box-shadow:none;">🚗 Transport</div>
        <div class="chip" style="box-shadow:none;">🎬 Ent.</div>
        <div class="chip" style="box-shadow:none;">🛍️ Shop</div>
      </div>

      <div style="height: 28px"></div>

      <div style="font-weight:900; color:var(--muted); font-size:24px; letter-spacing:1px; padding: 8px 4px;">MAY 2026</div>
      <div class="stack">
        <div class="tx">
          <div class="avatar" style="background: var(--food);">Y</div>
          <div class="body"><div class="t">Dinner at Good Taste</div><div class="s">🍽️ Food · paid by You</div></div>
          <div class="right"><div class="a pos">₱1,650.00</div><div class="d">May 12</div></div>
        </div>
        <div class="tx">
          <div class="avatar" style="background: var(--blue);">M</div>
          <div class="body"><div class="t">Jeepney + cab</div><div class="s">🚗 Transport · paid by Mika</div></div>
          <div class="right"><div class="a">₱420.00</div><div class="d">May 12</div></div>
        </div>
        <div class="tx">
          <div class="avatar" style="background: var(--purple);">M</div>
          <div class="body"><div class="t">Strawberry farm tour</div><div class="s">🎬 Ent. · paid by Mika</div></div>
          <div class="right"><div class="a">₱1,200.00</div><div class="d">May 11</div></div>
        </div>
        <div class="tx">
          <div class="avatar" style="background: var(--blue);">✓</div>
          <div class="body"><div class="t">Settlement</div><div class="s">Jun → You</div></div>
          <div class="right"><div class="a">₱780.00</div><div class="d">May 10</div></div>
        </div>
      </div>

      <div style="height:20px"></div>
      <div style="font-weight:900; color:var(--muted); font-size:24px; letter-spacing:1px; padding: 8px 4px;">EARLIER</div>
      <div class="stack">
        <div class="tx">
          <div class="avatar" style="background: var(--pink);">Y</div>
          <div class="body"><div class="t">Souvenirs</div><div class="s">🛍️ Shopping · paid by You</div></div>
          <div class="right"><div class="a pos">₱950.00</div><div class="d">May 09</div></div>
        </div>
        <div class="tx">
          <div class="avatar" style="background: var(--food);">J</div>
          <div class="body"><div class="t">Burnham Park brunch</div><div class="s">🍽️ Food · paid by Jun</div></div>
          <div class="right"><div class="a">₱1,500.00</div><div class="d">May 09</div></div>
        </div>
        <div class="tx">
          <div class="avatar" style="background: var(--green);">Y</div>
          <div class="body"><div class="t">Mines View entrance</div><div class="s">🎬 Ent. · paid by You</div></div>
          <div class="right"><div class="a pos">₱500.00</div><div class="d">May 08</div></div>
        </div>
      </div>
    </div>
  </div>`;
}

function chartsScreen() {
  return `
  <div class="phone">
    ${statusBar()}
    <div class="top-bar">
      <div class="left">
        <div class="icon-btn">←</div>
        <div class="top-title">ARC STATISTICS<small>Baguio · Last 6 Months</small></div>
      </div>
      <div></div>
    </div>

    <div style="padding: 32px;">
      <div class="card balance-hero">
        <div class="label">Total Arc Spending</div>
        <div class="pill">₱8,520.00</div>
        <div class="sub">▲ 18% more than last month</div>
      </div>

      <div style="height: 32px"></div>

      <div class="card">
        <div class="label-row">📊 CATEGORY BREAKDOWN</div>
        <div style="height: 2px; background: rgba(0,0,0,0.12); margin: 16px 0 22px;"></div>
        <div class="row" style="gap:30px; align-items:center;">
          <div class="pie"></div>
          <div style="flex:1;">
            ${[
              ["Food & Drinks", "var(--food)", "36%"],
              ["Transport",      "var(--blue)", "24%"],
              ["Entertainment",  "var(--purple)", "18%"],
              ["Utilities",      "var(--green)", "12%"],
              ["Shopping",       "var(--pink)", "10%"],
            ].map(([n,c,p]) => `
              <div class="legend-row">
                <div class="sw" style="background:${c};"></div>
                <div style="flex:1; font-weight:700;">${n}</div>
                <div style="font-weight:900;">${p}</div>
              </div>`).join("")}
          </div>
        </div>
        <div style="height: 14px;"></div>
        <div class="muted small">Top: Food &amp; Drinks (36%)</div>
      </div>

      <div style="height: 32px"></div>

      <div class="card">
        <div class="label-row">📅 MONTHLY TREND</div>
        <div style="height: 2px; background: rgba(0,0,0,0.12); margin: 16px 0 32px;"></div>
        <div class="bars">
          ${[
            ["Dec","30%","₱2.6k"],
            ["Jan","48%","₱4.1k"],
            ["Feb","42%","₱3.6k"],
            ["Mar","62%","₱5.3k"],
            ["Apr","72%","₱6.1k"],
            ["May","100%","₱8.5k"],
          ].map(([m,h,v]) => `
            <div class="bar" style="height:${h};">
              <div class="top">${v}</div>
              <div class="lab">${m}</div>
            </div>`).join("")}
        </div>
        <div style="height: 60px"></div>
        <div class="muted small">Avg: ₱5,033 / month</div>
      </div>

      <div style="height: 32px"></div>

      <div class="card">
        <div class="label-row">↔ OFFSET COMPUTATION</div>
        <div style="height: 2px; background: rgba(0,0,0,0.12); margin: 16px 0 18px;"></div>
        <div class="row between" style="font-size:24px; padding: 12px 0;">
          <span>You → balance</span><strong style="color:#16A34A;">+₱1,240</strong>
        </div>
        <div class="row between" style="font-size:24px; padding: 12px 0; border-top: 2px solid var(--divider);">
          <span>Jun owes you</span><strong style="color:#16A34A;">+₱780</strong>
        </div>
        <div class="row between" style="font-size:24px; padding: 12px 0; border-top: 2px solid var(--divider);">
          <span>Tala owes you</span><strong style="color:#16A34A;">+₱680</strong>
        </div>
      </div>
    </div>
  </div>`;
}

const SCREENS = {
  "01_hub_list": hubScreen,
  "02_dashboard_detail": dashboardScreen,
  "03_add_expense": addExpenseScreen,
  "04_history": historyScreen,
  "05_charts": chartsScreen,
};

module.exports = { SCREENS, BASE_CSS };
