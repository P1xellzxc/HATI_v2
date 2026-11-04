"""Build the HATI² web app.

Reads web/src/app.src.html, injects the palettes exported from HatiPalette.kt, and writes:
  docs/app/            installable web app (GitHub Pages, /docs)
  --artifact PATH      optional single-file page for the claude.ai artifact preview
"""
import json, re, sys, os, shutil
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from export_palettes import palettes

WEB = Path(__file__).resolve().parents[1]
REPO = WEB.parent
R = str(REPO)
S = str(WEB)
args = sys.argv[1:]
artifact = args[args.index("--artifact") + 1] if "--artifact" in args else None
src = (WEB / "src/app.src.html").read_text()
pal = json.dumps(palettes(), separators=(",", ":"))
body = src.replace('/*PALETTES*/', pal)
if artifact:
    Path(artifact).write_text(body.replace('/*PWA*/', ''))
# Installable web app
title=re.search(r'<title>.*?</title>',body).group(0)
rest=body.replace(title,'').replace('<meta name="claude:viewport-fit-sides" content="cover">\n','')
rest=re.sub(r'<link rel="preconnect"[^>]*>\n','',rest)
rest=re.sub(r'<link rel="stylesheet" href="https://fonts.googleapis.com[^>]*>\n','',rest)
assert 'fonts.googleapis' not in rest
os.makedirs(f'{R}/docs/app/fonts', exist_ok=True)
for f in ['black-han-sans-latin.woff2','dm-sans-latin.woff2']: shutil.copy(f'{S}/fonts/{f}', f'{R}/docs/app/fonts/{f}')
BHS_RANGE='U+20-21, U+24-29, U+2b-3a, U+3f, U+41-5a, U+61-7a, U+a3, U+a5, U+b2, U+b7, U+d7, U+e3f, U+2026, U+20a9, U+20ac, U+20b1, U+20b9, U+2212'
fontcss=("@font-face{font-family:'Black Han Sans';font-style:normal;font-weight:400;font-display:swap;src:url(fonts/black-han-sans-latin.woff2) format('woff2');unicode-range:"+BHS_RANGE+"}"
 "@font-face{font-family:'DM Sans';font-style:normal;font-weight:400 700;font-display:swap;src:url(fonts/dm-sans-latin.woff2) format('woff2')}")
pwa='''if ("serviceWorker" in navigator && window.isSecureContext) {
  navigator.serviceWorker.register("sw.js").catch(() => {});
}'''
html=f'''<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>HATI²</title>
<meta name="description" content="Split trip and group expenses offline.">
<meta name="color-scheme" content="light dark">
<meta name="theme-color" content="#FFFFFF">
<link rel="preload" href="fonts/black-han-sans-latin.woff2" as="font" type="font/woff2" crossorigin>
<link rel="manifest" href="manifest.webmanifest">
<link rel="icon" href="icon-192.png" type="image/png">
<link rel="apple-touch-icon" href="apple-touch-icon.png">
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-title" content="HATI²">
<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
<style>{fontcss}:root{{padding-top:env(safe-area-inset-top,0px);padding-bottom:0}}[hidden]{{display:none!important}}img{{max-width:100%}}html{{background:#FFFFFF}}@media (prefers-color-scheme:dark){{html{{background:#0B0A18}}}}</style>
</head>
<body>
{rest.replace('/*PWA*/',pwa)}
</body>
</html>
'''
open(f'{R}/docs/app/index.html','w').write(html)
open(f'{R}/docs/app/manifest.webmanifest','w').write(json.dumps({
 "id":"./","name":"HATI²","short_name":"HATI²","description":"Split group expenses offline.",
 "start_url":"./","scope":"./","display":"standalone","orientation":"portrait",
 "background_color":"#FFFFFF","theme_color":"#FEF08A",
 "icons":[{"src":"icon-192.png","sizes":"192x192","type":"image/png"},
          {"src":"icon-512.png","sizes":"512x512","type":"image/png"},
          {"src":"icon-maskable-512.png","sizes":"512x512","type":"image/png","purpose":"maskable"}]},indent=2))
import hashlib
version=hashlib.sha256((html+fontcss).encode()).hexdigest()[:10]
open(f'{R}/docs/app/sw.js','w').write('''const CACHE = "hati2-'''+version+'''";
const ASSETS = ["./", "index.html", "manifest.webmanifest", "icon-192.png", "icon-512.png", "icon-maskable-512.png", "apple-touch-icon.png",
  "fonts/black-han-sans-latin.woff2", "fonts/dm-sans-latin.woff2"];
self.addEventListener("install", e => e.waitUntil(caches.open(CACHE).then(c => c.addAll(ASSETS)).then(() => self.skipWaiting())));
self.addEventListener("activate", e => e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim())));
const put = (req, res) => { if (res && res.ok) { const copy = res.clone(); caches.open(CACHE).then(c => c.put(req, copy)); } return res; };
self.addEventListener("fetch", e => {
  const req = e.request;
  if (req.method !== "GET" || new URL(req.url).origin !== location.origin) return;
  if (req.mode === "navigate") {
    // Fresh page when the network answers within 3s; otherwise the cached shell.
    const net = fetch(req).then(r => put(req, r));
    const timeout = new Promise(res => setTimeout(res, 3000));
    e.respondWith(Promise.race([net, timeout]).then(r => r || caches.match("index.html")).catch(() => caches.match("index.html")));
    e.waitUntil(net.catch(() => {}));
    return;
  }
  e.respondWith(caches.match(req).then(hit => {
    const net = fetch(req).then(r => put(req, r));
    if (hit) { e.waitUntil(net.catch(() => {})); return hit; }
    return net;
  }));
});
''')
open(f'{R}/docs/index.html','w').write('''<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>HATI²</title><meta http-equiv="refresh" content="0; url=app/"><link rel="canonical" href="app/"></head>
<body style="font-family:system-ui,sans-serif;padding:24px"><p><a href="app/">Open HATI²</a></p></body></html>
''')
open(f'{R}/docs/.nojekyll','w').write('')
print('built')
