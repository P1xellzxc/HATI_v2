const {chromium, devices}=require('playwright');
const URL=process.env.URL||'http://127.0.0.1:8765/';
(async()=>{
 const b=await chromium.launch();
 // Live sync server mock (protocol in sync/README.md). Every context gets it, so no test ever reaches the real Worker;
 // all contexts share one in-memory server, like phones sharing the real one. mock.down holds contexts cut off from it.
 const SYNC='https://hati-sync.gerymefernando.workers.dev';
 const mock={rooms:new Map(),puts:[],urls:[],socks:new Map(),down:new Set(),status:null,n409:0};
 const mroom=id=>mock.rooms.get(id)||{version:0,blob:null};
 const mjson={'access-control-allow-origin':'*','access-control-allow-methods':'GET, PUT, OPTIONS','access-control-allow-headers':'content-type','cache-control':'no-store','content-type':'application/json'};
 const mbroadcast=(id,v)=>{ for (const ws of mock.socks.get(id)||[]) try { ws.send(JSON.stringify({version:v})); } catch(e){} };
 const wire=async c=>{
   await c.route(u=>u.href.startsWith(SYNC+'/'),async rt=>{ const rq=rt.request(), u=new globalThis.URL(rq.url()); mock.urls.push(rq.url());
     if (mock.down.has(c)) return rt.abort('internetdisconnected');
     const say=(status,body)=>rt.fulfill({status,headers:mjson,body:JSON.stringify(body)});
     if (rq.method()==='OPTIONS') return rt.fulfill({status:204,headers:mjson});
     const m=/^\/v1\/rooms\/([A-Za-z0-9_-]{22,64})$/.exec(u.pathname); if (!m) return say(400,{error:'bad_room'});
     const r=mroom(m[1]);
     if (rq.method()==='GET') return say(200,r);
     if (rq.method()!=='PUT') return say(405,{error:'method'});
     const body=rq.postData()||''; mock.puts.push({room:m[1],body});
     if (mock.status) return say(mock.status,{error:mock.status===413?'too_large':'rate_limited'});
     let j; try { j=JSON.parse(body); } catch(e) { return say(400,{error:'bad_json'}); }
     if (j.baseVersion!==r.version) { mock.n409++; return say(409,r); }
     const n={version:r.version+1,blob:j.blob}; mock.rooms.set(m[1],n); mbroadcast(m[1],n.version);
     return say(200,{version:n.version}); });
   await c.routeWebSocket(u=>u.href.startsWith(SYNC.replace('https','wss')+'/'),ws=>{ mock.urls.push(ws.url());
     const id=(/\/v1\/rooms\/([\w-]+)\/ws$/.exec(new globalThis.URL(ws.url()).pathname)||[])[1];
     if (!id || mock.down.has(c)) { ws.close({code:1011}); return; }
     const set=mock.socks.get(id)||new Set(); mock.socks.set(id,set); set.add(ws); ws.__c=c;
     ws.onMessage(m=>{ if (m==='ping') ws.send('pong'); }); ws.onClose(()=>set.delete(ws));
     ws.send(JSON.stringify({version:mroom(id).version})); });
 };
 // Service workers are blocked: Playwright can't route a request that a service worker lets through to the network,
 // and that request would reach the real Worker. Only the offline check below, which never syncs, allows one.
 const newContext0=b.newContext.bind(b); b.newContext=async o=>{ const c=await newContext0({serviceWorkers:'block',...o}); await wire(c); return c; };
 // An init script for a phone that can't reach the sync server, quietly (no failed requests in the console): for the
 // checks of manual share codes, merges and snapshot links, which live sync would otherwise do for them.
 const noSync=()=>{ const f=window.fetch; window.fetch=function(u){ return String(u&&u.url||u).startsWith('https://hati-sync.')?Promise.reject(new TypeError('Failed to fetch')):f.apply(this,arguments); }; window.WebSocket=undefined; };
 // Cut a context off the network (and the mock server), or bring it back.
 const setNet=async(c,on)=>{ if (on) mock.down.delete(c); else { mock.down.add(c); for (const s of mock.socks.values()) for (const ws of s) if (ws.__c===c) ws.close({code:1011}); } await c.setOffline(!on); };
 const ok=(c,msg)=>{console.log((c?'PASS ':'FAIL ')+msg); if(!c) process.exitCode=1;};
 // Drive a designed picker (D0, <hati-pick>) the way a user does: tap the segment, or tap the button and then the option
 // in the ink sheet list. want: a value, {label} or {index}. Returns the taps it took.
 const pick=async(pg,id,want)=>{ const el=pg.locator('#'+id);
   const name=await el.evaluate((e,w)=>{ const os=e.opts, i=typeof w==='string'?os.findIndex(x=>x[0]===w):w.label!=null?os.findIndex(x=>x[1]===w.label):w.index; return os[i]?os[i][1]:null; },want);
   if (name==null) throw new Error('no option '+JSON.stringify(want)+' in #'+id);
   if (['seg','grid'].includes(await el.getAttribute('data-mode'))) { await el.getByRole('radio',{name,exact:true}).click(); return 1; }
   await el.locator('.pick-btn').click(); await pg.locator('#pickLayer').getByRole('option',{name,exact:true}).click(); return 2; };
 const pickVal=(pg,id)=>pg.locator('#'+id).evaluate(e=>e.value);
 // There is no bottom bar: the hub is reached with the top bar's Back (or Escape for a sheet), Settings with the hub's gear.
 const toHub=async pg=>{ for (let i=0;i<8;i++) { const s=await pg.evaluate(()=>({v:ui.view,sh:sheetOpen()}));
     if (s.sh) { await pg.keyboard.press('Escape'); continue; } if (s.v==='hub') return;
     await pg.locator('#app .topbar [data-act=back]').click(); await pg.waitForFunction(v=>ui.view!==v,s.v,{timeout:2000}).catch(()=>{}); } };
 const toSettings=async pg=>{ await toHub(pg); await pg.locator('#app .topbar [data-act=settings]').click(); };
 const ctx=await b.newContext({...devices['iPhone 13']});
 const p=await ctx.newPage(); const errs=[]; p.on('pageerror',e=>errs.push(e.message)); p.on('console',m=>{if(m.type()==='error')errs.push(m.text())});
 await p.goto(URL);
 const meBal=()=>p.evaluate(()=>{const v=S.volumes.find(x=>x.id===ui.volId)||S.volumes[0]; return balances(v)[v.meId];});
 const zeroSum=()=>p.evaluate(()=>S.volumes.every(v=>Object.values(balances(v)).reduce((a,b)=>a+b,0)===0));
 ok(await p.locator('.vol').count()===1,'hub shows the example volume');
 ok((await p.locator('h1').innerText())==='HATI²','hub title is HATI²');
 ok(await p.getByRole('button',{name:'Import'}).isVisible(),'Import is a labeled button');
 await p.locator('.vol').first().click();
 ok(await meBal()===-130000,'example: You owe 1,300.00 (got '+await meBal()+')');
 ok(/^v-[a-z0-9]+$/.test((await p.evaluate(()=>location.hash)).slice(1)),'volume has its own URL hash');
 await p.reload(); ok(await p.evaluate(()=>ui.view)==='volume','reload keeps you on the volume page');
 // back button stays in the app
 await toHub(p); await p.locator('.vol').first().click();
 await p.goBack(); ok(await p.evaluate(()=>ui.view)==='hub' && p.url().startsWith(URL),'browser Back returns to the hub inside the app');
 await p.goForward(); ok(await p.evaluate(()=>ui.view)==='volume','browser Forward returns to the volume');
 // exact split + comma decimal
 await p.getByRole('button',{name:/Add expense/}).click();
 await p.fill('#f-amount','300'); await p.fill('#f-desc','Taxi');
 await p.locator('[data-split="exact"]').click();
 await p.locator('[data-inc]').nth(2).uncheck();
 const ins=p.locator('[data-in]');
 await ins.nth(0).fill('100'); await ins.nth(1).fill('150');
 await p.locator('#expForm button[type=submit]').click();
 ok((await p.locator('#formMsg').innerText()).includes('left to assign'),'exact split that does not add up is blocked');
 await ins.nth(1).fill('200,00'.replace(',00',''));
 // draft survives reload
 await p.reload(); ok(await p.locator('#f-desc').inputValue()==='Taxi','half-filled form survives a reload');
 await p.locator('#expForm button[type=submit]').click();
 ok(await meBal()===-110000,'after Taxi (paid 300, my share 100): −1,100.00 (got '+await meBal()+')');
 ok(await p.evaluate(()=>ui.view)==='volume','saving returns to the volume page');
 await p.getByRole('button',{name:/Add expense/}).click();
 await p.fill('#f-amount','12,50'); await p.fill('#f-desc','Coffee'); await p.locator('#expForm button[type=submit]').click();
 ok(await p.evaluate(()=>S.volumes[0].expenses.find(e=>e.desc==='Coffee').amount)===1250,'"12,50" saves as 12.50');
 await p.getByRole('button',{name:/Add expense/}).click();
 await p.fill('#f-amount','10'); await p.fill('#f-desc','Bad shares'); await p.locator('[data-split="shares"]').click();
 await p.locator('[data-in]').nth(0).fill('Infinity'); await p.locator('#expForm button[type=submit]').click();
 ok((await p.locator('#formMsg').innerText()).includes('Check the shares'),'Infinity share is rejected');
 await p.locator('[data-act=back]').click();
 // empty description error is visible near the field
 await p.getByRole('button',{name:/Add expense/}).click(); await p.fill('#f-amount','5'); await p.locator('#expForm button[type=submit]').click();
 const dm=await p.locator('#descMsg').boundingBox(); ok(dm && dm.y < 664 && (await p.locator('#descMsg').innerText()).length>0,'missing description error shows next to the field, on screen');
 await p.locator('[data-act=back]').click();
 ok(await zeroSum(),'balances sum to zero');
 // settle: prefilled with first suggestion
 const before=await p.evaluate(()=>debts(S.volumes[0]).length);
 await p.getByRole('button',{name:/Settle up/}).click();
 ok(await p.locator('#sheetForm[role=dialog][aria-modal=true]').count()===1,'sheet is a modal dialog');
 ok(await p.evaluate(()=>document.getElementById('app').inert),'page behind the sheet is inert');
 ok((await p.locator('#s-amt').inputValue()).length>0,'settle up is prefilled with the first suggested payment');
 await p.goBack(); ok(!(await p.evaluate(()=>sheetOpen())) && await p.evaluate(()=>ui.view)==='volume','Back closes the sheet and stays on the page');
 await p.getByRole('button',{name:/Settle up/}).click();
 await p.getByRole('button',{name:'Record payment'}).click();
 const after=await p.evaluate(()=>debts(S.volumes[0]).length);
 ok(after===before-1,`recording the suggested payment clears it (${before} → ${after})`);
 ok(await zeroSum(),'still zero-sum after settlement');
 // share & import as copy; XSS code rejected
 await p.locator('[data-act=volMenu]').click(); await p.locator('.sheet [data-act=share]').click();
 const code=await p.locator('#s-code').inputValue(); await p.locator('[data-act=closeSheet]').first().click();
 await toHub(p); await p.getByRole('button',{name:'Import'}).click();
 const evil=JSON.parse(code); evil.volume.id='q"><img src=x onerror=window.__x=1>'; evil.volume.members[1].id='a"><b>';
 await p.fill('#s-paste',JSON.stringify(evil)); await p.locator('#s-go').click();
 ok((await p.locator('#sheetMsg').innerText()).length>0 && !(await p.evaluate(()=>window.__x)),'malicious share code is rejected');
 await p.fill('#s-paste',code); await p.locator('#s-go').click();
 await pick(p,'s-mode','copy'); await pick(p,'s-who',{label:'Mira'}); await p.waitForTimeout(600); await p.locator('#s-go').click();
 ok(await p.evaluate(()=>S.volumes.length)===2,'share code imported as a second copy');
 ok(await p.evaluate(()=>{const v=S.volumes.find(x=>x.id===ui.volId);return nameOf(v,v.meId)})==='Mira','importer is Mira in the copy');
 await p.reload(); ok(await p.locator('h1').count()===1,'data survives reload');
 // themes
 await toSettings(p); await p.locator('[data-pal=persimmon]').click(); await p.locator('[data-set=mode][data-val=dark]').click();
 ok(await p.evaluate(()=>getComputedStyle(document.documentElement).getPropertyValue('--acc-d').trim())==='#FFB31A','Persimmon dark accent applied');
 ok(await p.evaluate(()=>document.activeElement && document.activeElement.dataset.val)==='dark','focus stays on the toggle after switching');
 await p.keyboard.press('Shift+Tab'); 
 for (const w of [390,360,320]) { await p.setViewportSize({width:w,height:700});
   for (const view of ['hub','volume','expense','settings']) {
     await p.evaluate(v=>{ if(v==='expense'){ui.volId=S.volumes[0].id; ui.view='volume'; openExpense(null);} else { if(v==='volume') ui.volId=S.volumes[0].id; go(v);} }, view);
     const sw=await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth); if(!sw) ok(false,`horizontal scroll at ${w} on ${view}`);
   } }
 ok(true,'checked 390/360/320 for horizontal scroll');
 await p.setViewportSize({width:390,height:844});
 await p.evaluate(()=>{ui.volId=S.volumes[0].id; go('volume');}); await p.screenshot({path:process.argv[2]+'/after-dark.png'});
 await toSettings(p); await p.locator('[data-pal=lagoon]').click(); await p.locator('[data-set=mode][data-val=light]').click();
 await toHub(p); await p.screenshot({path:process.argv[2]+'/after-light-hub.png'});
 await p.locator('.vol').first().click(); await p.getByRole('button',{name:/Add expense/}).click(); await p.locator('[data-split="exact"]').click();
 await p.screenshot({path:process.argv[2]+'/after-light-form.png'});
 // inputs are 16px (no iOS zoom)
 ok(await p.evaluate(()=>[...document.querySelectorAll('.input')].every(i=>parseFloat(getComputedStyle(i).fontSize)>=16)),'all inputs are at least 16px');
 ok(errs.length===0,'no console errors '+JSON.stringify(errs));
 // corrupted data does not blank the app
 const c2=await b.newContext({...devices['iPhone 13'],serviceWorkers:'allow'}); const q=await c2.newPage();
 await q.goto(URL); await q.evaluate(()=>localStorage.setItem('hati2.mock.v1',JSON.stringify({volumes:[null,{id:'abc',title:'Old',members:[{id:'m1',name:'A'}],expenses:[]}]})));
 await q.reload(); ok(await q.locator('.vol').count()===1 && await q.locator('.banner:not(#iosHint)').count()===1,'old/damaged saved data loads with a notice instead of a blank screen');
 await q.evaluate(()=>localStorage.setItem('hati2.mock.v1','{not json')); await q.reload();
 ok(await q.evaluate(()=>Object.keys(localStorage).some(k=>k.startsWith('hati2.corrupt.')))&&await q.locator('h1').count()===1,'unreadable data is backed up and the app still opens');
 // two tabs
 await q.evaluate(()=>{localStorage.clear();}); await q.reload();
 const q2=await c2.newPage(); await q2.goto(URL);
 await q.locator('.vol').first().click(); await q.evaluate(()=>{const v=vol(); v.members.push({id:'tabA',name:'FromTabA'}); save();});
 await q2.waitForTimeout(200);
 await q2.locator('.vol').first().click(); await q2.evaluate(()=>{const v=vol(); v.members.push({id:'tabB',name:'FromTabB'}); save();});
 const names=await q.evaluate(()=>JSON.parse(localStorage.getItem('hati2.mock.v1')).volumes[0].members.map(m=>m.name));
 ok(names.includes('FromTabA')&&names.includes('FromTabB'),'two open tabs do not erase each other: '+names.join(','));
 // offline
 await q.waitForTimeout(500); await c2.setOffline(true); await q.reload();
 ok(await q.locator('h1').count()===1,'app opens offline from the service worker');
 ok(await q.evaluate(async()=>{await document.fonts.ready; return document.fonts.check("16px 'Black Han Sans'")}),'display font available offline');
 // R1-4/R1-5 (design): share sheet, import sheet, palette grid
 { const c3=await b.newContext({...devices['iPhone 13'],viewport:{width:360,height:780}}); const d=await c3.newPage(); const derr=[]; d.on('pageerror',e=>derr.push(e.message));
   await d.goto(URL);
   await d.locator('.vol').first().click(); await d.locator('[data-act=volMenu]').click(); await d.locator('.sheet [data-act=share]').click();
   ok(/3 members · \d+ expenses/.test(await d.locator('.share-sum').innerText()),'share sheet names what is shared: '+(await d.locator('.share-sum').innerText()).replace(/\n/g,' | '));
   ok(!(await d.locator('#s-code').isVisible()) && !(await d.locator('#sheetForm').innerText()).includes('"app"'),'no raw code is visible before "Show code"');
   await d.getByText('Show code').click(); ok(await d.locator('#s-code').isVisible(),'"Show code" reveals the code');
   await d.evaluate(()=>{ Object.defineProperty(navigator,'clipboard',{value:{writeText:()=>Promise.reject(new Error('denied'))},configurable:true}); });
   await d.getByRole('button',{name:'Copy code'}).click(); await d.waitForTimeout(100);
   ok(await d.locator('#s-code').isVisible() && await d.evaluate(()=>{const t=$('#s-code'); return document.activeElement===t && t.selectionEnd-t.selectionStart===t.value.length;}),'clipboard denied: Copy opens the code and selects all of it');
   await d.locator('[data-act=closeSheet]').click();
   await d.evaluate(()=>{ window.__shared=null; navigator.share=d=>{window.__shared=d; return Promise.resolve();}; window.__copied=0; Object.defineProperty(navigator,'clipboard',{value:{writeText:()=>{window.__copied++; return Promise.resolve();}},configurable:true}); });
   await d.locator('[data-act=volMenu]').click(); await d.locator('.sheet [data-act=share]').click();
   await d.getByRole('button',{name:'Send invite'}).click(); await d.waitForTimeout(100);
   ok(await d.evaluate(()=>!!window.__shared && window.__shared.url===$('#s-link').value && /#join=[\w.-]+$/.test(window.__shared.url) && window.__copied===0),'Send invite passes the invite link to the native share sheet');
   await d.evaluate(()=>{ navigator.share=()=>Promise.reject(new DOMException('cancel','AbortError')); });
   await d.getByRole('button',{name:'Send invite'}).click(); await d.waitForTimeout(100);
   ok(await d.evaluate(()=>window.__copied===0 && !$('#sheetMsg').textContent),'cancelling the share sheet is silent and does not copy');
   await d.locator('[data-act=closeSheet]').click(); await toHub(d); await d.getByRole('button',{name:'Import'}).click();
   const ph=await d.locator('#s-paste').getAttribute('placeholder'); ok(!/[{}"]/.test(ph),'import placeholder has no JSON: '+ph);
   ok(await d.getByText('Open file').isVisible(),'file import is a labeled button, not a bare file input');
   await d.locator('[data-act=closeSheet]').click();
   await toSettings(d);
   ok(await d.evaluate(()=>document.scrollingElement.scrollHeight)<=1300,'settings is at most 1300px tall at 360 (got '+await d.evaluate(()=>document.scrollingElement.scrollHeight)+')');
   const names=await d.evaluate(()=>PALETTES.map(x=>x.name)); let allSel=true;
   for (const n of names) { await d.getByRole('radio',{name:new RegExp(n,'i')}).click(); if(await d.getByRole('radio',{name:new RegExp(n,'i')}).getAttribute('aria-checked')!=='true') allSel=false; }
   ok(allSel && names.length===13,'all 13 palettes are selectable by name');
   ok(await d.locator('.pal[aria-checked=true] .dot').innerText()==='✓' && await d.locator('.pal .dot').count()===1,'current palette carries a check mark, not only colour');
   await d.getByRole('radio',{name:/Classic/i}).click(); const seen=new Set();
   for (let i=0;i<13;i++){ seen.add(await d.evaluate(()=>document.activeElement.dataset.pal)); await d.keyboard.press('ArrowRight'); }
   ok(seen.size===13 && await d.locator('.pal[tabindex="0"]').count()===1 && await d.evaluate(()=>document.activeElement.dataset.pal)==='classic','arrow keys walk all 13 palettes with one tab stop');
   await d.setViewportSize({width:320,height:700}); ok(await d.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'palette grid has no horizontal scroll at 320');
   ok(derr.length===0,'no page errors in design checks '+JSON.stringify(derr));
   await c3.close(); }

 // Round 1 revise (design): page background in every OS/app theme pair, money never wraps, no dead form bands, import/restore copy
 { for (const [os,app] of [['light','dark'],['dark','light']]) {
     const c=await b.newContext({viewport:{width:360,height:780},colorScheme:os}); const pg=await c.newPage(); await pg.goto(URL);
     await pg.evaluate(a=>{ S.settings.mode=a; applyTheme(); ui.volId=S.volumes[0].id; go('volume'); scrollTo(0,1e5); },app);
     const bg=await pg.evaluate(()=>[getComputedStyle(document.documentElement).backgroundColor,getComputedStyle(document.body).backgroundColor,document.body.offsetHeight>=document.scrollingElement.scrollHeight-1]);
     ok(bg[0]===bg[1] && bg[2],`OS ${os} + app ${app}: page background is one colour to the bottom (${bg[0]} / ${bg[1]}, body covers page: ${bg[2]})`);
     // R2 revise (iOS): native controls (checkbox, date picker, keyboard) follow the app theme, not the OS
     const cs=await pg.evaluate(()=>{ const o=[getComputedStyle(document.documentElement).colorScheme]; S.settings.mode='system'; applyTheme(); o.push(getComputedStyle(document.documentElement).colorScheme); return o; });
     ok(cs[0]===app && cs[1]===os,`OS ${os} + app ${app}: color-scheme is ${app}, and ${os} on System ${JSON.stringify(cs)}`);
     await c.close(); }
   const c=await b.newContext({...devices['iPhone 13'],viewport:{width:360,height:780}}); const pg=await c.newPage(); await pg.goto(URL);
   await pg.evaluate(()=>{ const v=S.volumes[0]; v.members[1].name='Maximilian Featherstonehaugh-Wolfe'; v.expenses.push({id:'big',desc:'Big',amount:99999999999,paidBy:v.meId,splits:{[v.meId]:33333333333,[v.members[1].id]:33333333333,[v.members[2].id]:33333333333},inputs:{},splitType:'equal',category:'other',date:today(),upd:Date.now()}); save(); ui.volId=v.id; go('volume'); });
   const wrap=await pg.evaluate(()=>[...document.querySelectorAll('.chip .num, .debt b.num, .hero .amt, .hero .mine b')].filter(e=>e.getClientRects().length>1||e.offsetHeight>parseFloat(getComputedStyle(e).lineHeight)*1.5).map(e=>e.textContent));
   ok(wrap.length===0,'long amounts stay on one line: '+JSON.stringify(wrap));
   ok(await pg.evaluate(()=>[...document.querySelectorAll('.debt .who, .hero .mine .who')].every(w=>/pays?/.test(w.innerText) && w.lastElementChild.getBoundingClientRect().right<=w.getBoundingClientRect().right+1)),'debt rows keep the verb visible next to a long name');
   ok(await pg.evaluate(()=>document.scrollingElement.scrollWidth<=innerWidth),'no sideways scroll with huge amounts at 360');
   await pg.evaluate(()=>{ const v=vol(); editMemberSheet(v.members[1]); });
   const gap=await pg.evaluate(()=>{ const f=$('#s-name').closest('.field').getBoundingClientRect().bottom, b=$('#sheetForm .btns').getBoundingClientRect().top; return Math.round(b-f); });
   ok(gap<=13,'empty message slot leaves no dead band in Edit member (gap '+gap+'px)');
   await pg.keyboard.press('Escape'); await pg.evaluate(()=>go('settings')); await pg.locator('[data-act=restore]').click();
   ok(await pg.locator('#sheetForm h2').innerText()==='Restore backup','Settings > Restore opens "Restore backup"');
   await pg.keyboard.press('Escape'); await pg.evaluate(()=>go('hub')); await pg.getByRole('button',{name:'Import'}).click();
   await pg.locator('#s-paste').fill(await pg.evaluate(()=>JSON.stringify({app:'hati2-mock',v:1,volume:S.volumes[0]}))); await pg.locator('#s-go').click();
   ok(/tap Import/.test(await pg.locator('#s-help').innerText()) && /\.$/.test(await pg.locator('#s-sum').innerText()),'after Check code the help line says Import and the summary ends with a full stop: '+await pg.locator('#s-sum').innerText());
   await c.close(); }

 // NB: no bottom bar (owner, 2026-11-09). Settings is the gear in the hub's top bar; Back returns to the hub.
 { const c=await b.newContext({...devices['iPhone 13']}); await c.addInitScript(noSync); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{if(m.type()==='error')e.push(m.text())});
   await pg.goto(URL);
   ok(await pg.evaluate(()=>!document.querySelector('#nav, nav, .nav') && ![...document.body.querySelectorAll('*')].some(n=>{ const s=getComputedStyle(n); if (!/fixed|sticky/.test(s.position) || n.closest('.toast')) return false; const r=n.getBoundingClientRect(); return r.height>0 && r.bottom>=innerHeight-1 && r.top>innerHeight/2; })),'NB: there is no #nav and no bar fixed to the bottom of the screen');
   const gear=await pg.evaluate(()=>{ const g=$('#app .topbar [data-act=settings]'); if (!g) return null; const r=g.getBoundingClientRect(), t=g.closest('.topbar').getBoundingClientRect(); return {cls:g.className,label:g.getAttribute('aria-label'),w:r.width,h:r.height,last:g===g.parentElement.lastElementChild,right:Math.round(t.right-r.right),mid:Math.abs((r.top+r.bottom)/2-(t.top+t.bottom)/2)}; });
   ok(gear && gear.cls==='ico' && gear.label==='Settings' && gear.w>=44 && gear.h>=44 && gear.last && gear.right<=17 && gear.mid<=1.5,'NB: the hub top bar ends with a clay .ico gear labelled "Settings", 44px+, top-right '+JSON.stringify(gear));
   const t0=await pg.evaluate(()=>history.length);
   await pg.locator('#app .topbar [data-act=settings]').click();
   await pg.waitForFunction(()=>$('#app').dataset.turn==='fwd',null,{timeout:2000}).catch(()=>{});
   const s1=await pg.evaluate(()=>({view:ui.view,h1:$('#app h1').textContent,hash:location.hash,turn:$('#app').dataset.turn,back:!!$('#app .topbar [data-act=back]'),focus:document.activeElement===$('#app h1')}));
   ok(s1.view==='settings' && s1.h1==='Settings' && s1.hash==='#prefs' && s1.turn==='fwd' && s1.back && s1.focus,'NB: one tap on the gear opens Settings (#prefs, wipe goes left, title focused, Back in its top bar) '+JSON.stringify(s1));
   await pg.locator('#app .topbar [data-act=back]').click(); await pg.waitForFunction(()=>ui.view==='hub',null,{timeout:2000}).catch(()=>{});
   await pg.waitForFunction(()=>$('#app').dataset.turn==='back',null,{timeout:2000}).catch(()=>{});
   const s2=await pg.evaluate(()=>({view:ui.view,hash:location.hash,turn:$('#app').dataset.turn}));
   ok(s2.view==='hub' && s2.hash==='' && s2.turn==='back' && await pg.evaluate(()=>history.length)===t0+1,'NB: Settings Back returns to the hub (wipe goes right, no extra history entry) '+JSON.stringify(s2));
   await pg.locator('#app .topbar [data-act=settings]').click(); await pg.goBack(); await pg.waitForFunction(()=>ui.view==='hub',null,{timeout:2000}).catch(()=>{});
   ok(await pg.evaluate(()=>ui.view)==='hub' && pg.url().startsWith(URL),'NB: Android/browser Back from Settings returns to the hub inside the app');
   await pg.locator('.vol').first().click();
   ok(await pg.evaluate(()=>ui.view==='volume' && !$('#app [data-act=settings]')),'NB: a volume has no Settings button (hub only; Back then the gear)');
   await pg.goto(URL+'#prefs'); await pg.reload();
   ok(await pg.evaluate(()=>ui.view==='settings' && $('#app h1').textContent==='Settings'),'NB: the #prefs deep link opens Settings');
   await pg.locator('#app .topbar [data-act=back]').click(); await pg.waitForFunction(()=>ui.view==='hub',null,{timeout:2000}).catch(()=>{});
   ok(await pg.evaluate(()=>ui.view==='hub' && location.hash==='') && pg.url().startsWith(URL),'NB: Back from a #prefs deep link lands on the hub, still in the app');
   // content reaches the bottom: no dead strip under it (only #app's 24px + safe-area padding)
   await pg.evaluate(()=>{ for (let i=0;i<6;i++) { const v=JSON.parse(JSON.stringify(S.volumes[0])); v.id='nb'+i; v.title='Trip '+i; v.created=(S.volumes[0].created||0)+i+1; S.volumes.push(v); } save(); });
   const padRule=await pg.evaluate(()=>[...document.styleSheets].flatMap(s=>[...s.cssRules]).filter(r=>r.selectorText==='#app').map(r=>(/padding-block:[^;]*/.exec(r.cssText)||[''])[0]).join(' '));
   ok(/safe-area-inset-bottom/.test(padRule) && /24px/.test(padRule),'NB: #app keeps only 24px plus the bottom safe area under the content: '+padRule);
   const strips=[];
   for (const w of [320,360,390]) for (const mode of ['light','dark']) { await pg.setViewportSize({width:w,height:640});
     for (const view of ['hub','settings']) {
       const m=await pg.evaluate(([mode,view])=>{ S.settings.mode=mode; applyTheme(); go(view); window.scrollTo(0,document.scrollingElement.scrollHeight);
         const bottom=Math.max(...[...$('#app').children].map(n=>n.getBoundingClientRect().bottom)), at=document.elementFromPoint(innerWidth/2,innerHeight-3);
         const gw=$('#app .topbar [data-act=settings]'), h1=$('#app h1');
         return {gap:Math.round(innerHeight-bottom),at:at?(at===document.documentElement||at===document.body||!!at.closest('#app')):true,sw:document.scrollingElement.scrollWidth<=innerWidth,
           gear:gw?gw.getBoundingClientRect().right<=innerWidth-15:true,h1:h1.scrollWidth<=h1.clientWidth+1}; },[mode,view]);
       if (m.gap<8 || m.gap>40 || !m.at || !m.sw || !m.gear || !m.h1) strips.push(`${w}/${mode}/${view} ${JSON.stringify(m)}`); } }
   ok(strips.length===0,'NB: at 320/360/390, light and dark, hub and Settings scroll down to content 8-40px from the bottom edge, nothing fixed there, gear and title fit '+JSON.stringify(strips.slice(0,3)));
   ok(e.length===0,'NB: no console errors '+e.slice(0,2).join(' | '));
   await c.close(); }

 // R1-1/R1-2/R1-3 (code): merge on import, member edit, native share, backup
 { const fresh=async()=>{ const c=await b.newContext({...devices['iPhone 13'],acceptDownloads:true}); await c.addInitScript(noSync); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{if(m.type()==='error')e.push(m.text())}); await pg.goto(URL); return {c,pg,e}; };
   const home=async pg=>{ if(await pg.evaluate(()=>sheetOpen())) await pg.keyboard.press('Escape'); await toHub(pg); };
   const openVol=async (pg,title)=>{ await home(pg); await pg.locator('.vol',{hasText:title}).first().click(); };
   const shareCode=async (pg,title)=>{ await openVol(pg,title); await pg.locator('[data-act=volMenu]').click(); await pg.locator('.sheet [data-act=share]').click(); const c=await pg.locator('#s-code').inputValue(); await pg.keyboard.press('Escape'); return c; };
   const check=async (pg,text)=>{ await home(pg); await pg.getByRole('button',{name:'Import'}).click(); await pg.fill('#s-paste',text); await pg.locator('#s-go').click(); };
   const doImport=async pg=>{ await pg.waitForTimeout(600); await pg.locator('#s-go').click(); };
   const toHist=async pg=>{ if(await pg.evaluate(()=>ui.view)!=='history') await pg.locator('[data-act=history]').first().click(); };
   const toVol=async pg=>{ if(await pg.evaluate(()=>ui.view)!=='volume') await pg.locator('[data-act=back]').click(); };
   const addExp=async (pg,desc,amt)=>{ await toVol(pg); await pg.getByRole('button',{name:/Add expense/}).click(); await pg.fill('#f-amount',amt); await pg.fill('#f-desc',desc); await pg.locator('#expForm button[type=submit]').click(); };
   const editDesc=async (pg,from,to)=>{ await toHist(pg); await pg.locator('.tx',{hasText:from}).click(); await pg.fill('#f-desc',to); await pg.locator('#expForm button[type=submit]').click(); };
   const delExp=async (pg,desc)=>{ await toHist(pg); await pg.locator('.tx',{hasText:desc}).click(); await pg.locator('[data-act=deleteExpense]').click(); await pg.locator('#sheetForm button.danger').click(); };
   const descs=pg=>pg.evaluate(()=>vol().expenses.map(e=>e.desc));
   const zs=pg=>pg.evaluate(()=>S.volumes.every(v=>Object.values(balances(v)).reduce((a,b)=>a+b,0)===0));
   const A=await fresh(), B=await fresh(), tick=()=>A.pg.waitForTimeout(15);
   // A adds Zed, then shares the volume with B (B imports as Mira)
   await A.pg.locator('.vol').first().click(); await A.pg.locator('.chip.add').click(); await A.pg.fill('#s-name','Zed'); await A.pg.locator('#sheetForm button.primary').click();
   const codeA0=await shareCode(A.pg,'Tokyo');
   await check(B.pg,codeA0); await pick(B.pg,'s-who',{label:'Mira'}); await doImport(B.pg);
   const volId=await A.pg.evaluate(()=>vol().id);
   ok(await B.pg.evaluate(id=>ui.volId===id && nameOf(vol(),vol().meId)==='Mira',volId),'B holds A\'s volume as Mira');
   // diverge: A edits Shinkansen, then B edits it later; A edits the hotel while B deletes it; B deletes Ramen and the payment; A removes Zed while B uses Zed
   await openVol(A.pg,'Tokyo'); await editDesc(A.pg,'Shinkansen','Shinkansen A'); await tick();
   await editDesc(A.pg,'Shibuya hotel','Hotel edited on A'); await tick();
   await editDesc(B.pg,'Shinkansen','Shinkansen B'); await tick();
   await delExp(B.pg,'Shibuya hotel'); await delExp(B.pg,'Ramen');
   await B.pg.locator('.tx',{hasText:'paid Rio'}).first().click(); await B.pg.locator('[data-act=deleteSettle]').click();
   ok((await B.pg.locator('#sheetTitle').innerText())==='Delete payment?','deleting a payment asks first');
   await B.pg.locator('#sheetForm button.danger').click();
   await addExp(B.pg,'FromB','90');
   await toVol(A.pg); await A.pg.getByRole('button',{name:/^Zed,/}).click(); await A.pg.locator('[data-act=removeMember]').click();
   ok(await A.pg.evaluate(()=>!vol().members.some(m=>m.name==='Zed')),'A removed unused member Zed');
   await addExp(A.pg,'FromA','30');
   // A imports B's code with the default options, all through the UI
   const codeB=await shareCode(B.pg,'Tokyo');
   const meA=await A.pg.evaluate(()=>vol().meId);
   await check(A.pg,codeB);
   ok(await pickVal(A.pg,'s-mode')==='merge' && !(await A.pg.locator('#s-who').isVisible()),'existing volume: Merge is the default and the member picker is hidden');
   const sum=await A.pg.locator('#s-sum').innerText(); ok(/ Adds \d+ (expense|payment|member)s?.*\. Updates .*\. Removes .*\.$/.test(sum) && !/·/.test(sum),'merge summary names what it adds, updates and removes: '+sum);
   await doImport(A.pg);
   let d=await descs(A.pg);
   ok(d.includes('FromA')&&d.includes('FromB')&&!d.includes('Ramen at Ichiran'),'merge keeps FromA and FromB and drops the expense B deleted: '+d.join('|'));
   ok(!d.some(x=>/hotel/i.test(x)),'delete on B beats edit on A');
   ok(d.includes('Shinkansen B')&&!d.includes('Shinkansen A'),'both edited: the newer edit wins');
   ok(await A.pg.evaluate(()=>vol().settlements.length===0),'payment deleted on B is gone on A');
   ok(await A.pg.evaluate(()=>vol().members.some(m=>m.name==='Zed') && validVolume(vol())),'Zed comes back because B used Zed; volume is valid');
   ok(await A.pg.evaluate(m=>vol().meId===m,meA),'merge keeps A\'s own member');
   ok(await zs(A.pg),'balances sum to zero after merge');
   await A.pg.waitForTimeout(100); const st=await A.pg.evaluate(()=>$('#status').textContent); ok(/^Merged: adds \d+ (expense|payment|member)s?.*\. Updates .*\. Removes .*\.$/.test(st),'after merge the message says what changed: '+st);
   await A.pg.reload(); d=await descs(A.pg);
   ok(d.includes('FromA')&&d.includes('FromB')&&(await A.pg.evaluate(()=>vol().deleted.length))>=3,'after reload both entries and the tombstones remain');
   await check(A.pg,codeA0); await doImport(A.pg); d=await descs(A.pg);
   ok(!d.includes('Ramen at Ichiran')&&d.includes('FromB'),'merging an older copy again does not bring the deleted expense back');
   // B merges A's copy: B keeps being Mira and gets FromA
   const codeA1=await shareCode(A.pg,'Tokyo'); await check(B.pg,codeA1); await doImport(B.pg);
   ok((await descs(B.pg)).includes('FromA') && await B.pg.evaluate(()=>nameOf(vol(),vol().meId))==='Mira' && await zs(B.pg),'B merges A\'s copy, stays Mira, zero-sum');
   // a merge that would be invalid leaves everything unchanged
   await openVol(A.pg,'Tokyo'); await A.pg.locator('.chip.add').click(); await A.pg.fill('#s-name','Solo'); await A.pg.locator('#sheetForm button.primary').click();
   const big=JSON.parse(codeA1); for(let i=big.volume.members.length;i<200;i++) big.volume.members.push({id:'fill'+i,name:'Filler '+i});
   const beforeS=await A.pg.evaluate(()=>JSON.stringify(S.volumes));
   await check(A.pg,JSON.stringify(big)); await doImport(A.pg);
   ok((await A.pg.locator('#sheetMsg').innerText())==="These copies couldn't be combined. Nothing was changed." && await A.pg.evaluate(b=>JSON.stringify(S.volumes)===b,beforeS),'a merge that would break the volume changes nothing and says so');
   await A.pg.keyboard.press('Escape');
   // crafted far-future upd does not win
   const fut=JSON.parse(codeA1); fut.volume.expenses.find(e=>e.desc==='FromA').desc='Hijacked'; fut.volume.expenses.find(e=>e.desc==='Hijacked').upd=Date.now()+1e12;
   await openVol(A.pg,'Tokyo'); await editDesc(A.pg,'FromA','FromA v2');
   await check(A.pg,JSON.stringify(fut)); await doImport(A.pg);
   ok((await descs(A.pg)).includes('FromA v2'),'a far-future upd in a code is treated as 0 and does not win');
   // old-format code (no upd, no deleted) and chat text around the code
   const C=await fresh(); const old=JSON.parse(codeA0); delete old.volume.deleted; delete old.volume.upd; old.volume.members.forEach(m=>delete m.upd); old.volume.expenses.forEach(e=>delete e.upd); old.volume.settlements.forEach(s=>delete s.upd);
   await check(C.pg,'here you go: '+JSON.stringify(old)+' thanks!'); ok(await C.pg.locator('#s-who').isVisible(),'code pasted with chat text around it is accepted');
   await pick(C.pg,'s-who',{label:'Rio'}); await doImport(C.pg);
   ok(await C.pg.evaluate(id=>ui.volId===id && validVolume(vol()),volId) && await zs(C.pg),'old-format code without upd/deleted imports');
   // R1-2: rename and remove members
   await openVol(A.pg,'Tokyo');
   await A.pg.getByRole('button',{name:/^Mira,/}).click(); await A.pg.fill('#s-name','rio'); await A.pg.locator('#sheetForm button.primary').click();
   ok(/already in this volume/.test(await A.pg.locator('#sheetMsg').innerText()),'rename to an existing name (any case) is blocked');
   ok(await A.pg.locator('[data-act=removeMember]').isDisabled() && /can't be removed/.test(await A.pg.locator('#s-why').innerText()),'member used in expenses: Remove is disabled and the reason is visible');
   await A.pg.fill('#s-name','  Mia '); await A.pg.locator('#sheetForm button.primary').click();
   const volText=await A.pg.locator('#app').innerText(); await A.pg.locator('[data-act=history]').first().click(); const histText=await A.pg.locator('#app').innerText();
   ok(volText.includes('Mia')&&!volText.includes('Mira')&&histText.includes('Mia')&&!histText.includes('Mira'),'renamed Mira to Mia everywhere on volume and history');
   await A.pg.locator('[data-act=back]').click();
   await A.pg.getByRole('button',{name:/^You,/}).click(); ok(await A.pg.locator('[data-act=removeMember]').isDisabled() && /yourself/.test(await A.pg.locator('#s-why').innerText()),'you cannot remove yourself'); await A.pg.keyboard.press('Escape');
   const n0=await A.pg.evaluate(()=>vol().members.length);
   await A.pg.getByRole('button',{name:/^Solo,/}).click(); await A.pg.locator('[data-act=removeMember]').click();
   ok(await A.pg.evaluate(()=>vol().members.length)===n0-1 && await zs(A.pg),'unused member removed: count drops by 1, zero-sum holds');
   // a 0-cent exact split still counts as use
   await A.pg.locator('.chip.add').click(); await A.pg.fill('#s-name','Nil'); await A.pg.locator('#sheetForm button.primary').click();
   await A.pg.getByRole('button',{name:/Add expense/}).click(); await A.pg.fill('#f-amount','10'); await A.pg.fill('#f-desc','Zero split');
   await A.pg.locator('[data-split="exact"]').click(); const nIn=await A.pg.locator('[data-in]').count();
   for(let i=0;i<nIn;i++) await A.pg.locator('[data-in]').nth(i).fill(i===0?'10':'0');
   await A.pg.locator('#expForm button[type=submit]').click();
   ok(await A.pg.evaluate(()=>{const v=vol(),n=v.members.find(m=>m.name==='Nil'); return v.expenses.some(e=>e.splits[n.id]===0);}),'Nil has a 0-cent exact split');
   await A.pg.getByRole('button',{name:/^Nil,/}).click(); ok(await A.pg.locator('[data-act=removeMember]').isDisabled(),'member with a 0-cent split cannot be removed');
   await A.pg.keyboard.press('Escape'); await A.pg.reload(); ok(await A.pg.evaluate(id=>S.volumes.some(v=>v.id===id),volId),'volume survives reload');
   // a stored draft that names a removed member is ignored
   ok(await A.pg.evaluate(()=>{ sessionStorage.setItem('hati2.draft',JSON.stringify({volId:ui.volId,form:{amount:'5',desc:'x',paidBy:'gone1',include:{gone1:true},inputs:{},splitType:'equal',category:'food',date:today(),note:''}})); return draftFor(null)===null; }),'draft paying from a removed member is dropped');
   // R1-3 / R2-1: Send invite hands the link to the share sheet; pasted into Import on another phone, it imports
   await openVol(B.pg,'Tokyo'); await B.pg.evaluate(()=>{ window.__shared=null; navigator.share=d=>{window.__shared=d; return Promise.resolve();}; });
   await B.pg.locator('[data-act=volMenu]').click(); await B.pg.locator('.sheet [data-act=share]').click(); await B.pg.getByRole('button',{name:'Send invite'}).click(); await B.pg.waitForTimeout(100);
   const sent=await B.pg.evaluate(()=>window.__shared && window.__shared.url); await B.pg.keyboard.press('Escape');
   const D=await fresh(); await check(D.pg,sent); await pick(D.pg,'s-who',{label:'Alex'}); await doImport(D.pg);
   ok(await D.pg.evaluate(id=>ui.volId===id,volId) && await zs(D.pg),'the link passed to navigator.share, pasted into Import on another phone, imports '+JSON.stringify([String(sent).slice(0,60),await D.pg.evaluate(()=>[ui.view,ui.volId,$('#sheetMsg')&&$('#sheetMsg').textContent])]));
   // backup: download, restore on a fresh phone and onto a phone that already has one volume
   await toSettings(A.pg);
   const [dl]=await Promise.all([A.pg.waitForEvent('download'),A.pg.locator('[data-act=backup]').click()]);
   ok(/^hati2-backup-\d{4}-\d{2}-\d{2}\.json$/.test(dl.suggestedFilename()),'Save backup downloads '+dl.suggestedFilename());
   const file=await dl.path(); const nA=await A.pg.evaluate(()=>S.volumes.length);
   const E=await fresh(); await E.pg.evaluate(()=>{S.volumes=[];save();}); await E.pg.reload();
   await toSettings(E.pg); await E.pg.locator('[data-act=restore]').click(); await E.pg.setInputFiles('#s-file',file); await E.pg.waitForTimeout(100); await E.pg.locator('#s-go').click();
   ok(new RegExp('Restores '+nA+' volume').test(await E.pg.locator('#s-sum').innerText()),'backup summary: '+await E.pg.locator('#s-sum').innerText());
   await doImport(E.pg);
   ok(await E.pg.evaluate(()=>S.volumes.length)===nA && await zs(E.pg) && await E.pg.evaluate(id=>S.volumes.some(v=>v.id===id&&nameOf(v,v.meId)==='Alex'),volId),'backup restores every volume on a fresh phone, each with its own member, zero-sum');
   await check(B.pg,require('fs').readFileSync(file,'utf8'));
   ok(/merges 1 volume/i.test(await B.pg.locator('#s-sum').innerText()),'backup onto a phone that has the volume: '+await B.pg.locator('#s-sum').innerText());
   await doImport(B.pg);
   ok(await B.pg.evaluate(id=>{const v=S.volumes.find(x=>x.id===id); return nameOf(v,v.meId)==='Mia' && v.expenses.some(e=>e.desc==='Zero split');},volId) && await zs(B.pg),'backup merges into the existing volume; B keeps its own member (renamed Mia on A) '+JSON.stringify(await B.pg.evaluate(id=>{const v=S.volumes.find(x=>x.id===id); return [nameOf(v,v.meId),v.expenses.map(e=>e.desc),$('#sheetMsg')&&$('#sheetMsg').textContent];},volId)));
   const allE=[...A.e,...B.e,...C.e,...D.e,...E.e]; ok(allE.length===0,'no console errors in merge/member/backup checks '+JSON.stringify(allE));
   for (const x of [A,B,C,D,E]) await x.c.close(); }
 // Round 1 revise (code): prototype ids, storage full, deleted in another tab, Replace summary and who you are, tombstone flood, unchanged name, iOS hint/status bar/backup, Who else is in
 { const fresh=async(dev=devices['iPhone 13'],init)=>{ const c=await b.newContext({...dev,acceptDownloads:true}); if(init) await c.addInitScript(init); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{if(m.type()==='error')e.push(m.text())}); await pg.goto(URL); return {c,pg,e}; };
   const zs=pg=>pg.evaluate(()=>S.volumes.every(v=>Object.values(balances(v)).reduce((a,b)=>a+b,0)===0));
   const importCode=async (pg,code)=>{ await pg.evaluate(()=>go('hub')); await pg.getByRole('button',{name:'Import'}).click(); await pg.fill('#s-paste',code); await pg.locator('#s-go').click(); };
   const doImport=async pg=>{ await pg.waitForTimeout(600); await pg.locator('#s-go').click(); };
   const toastText=pg=>pg.evaluate(()=>{const t=document.querySelector('.toast'); return t?t.textContent:'';});
   const all=[];
   // N1/F2: ids that are Object.prototype keys are refused; pairwise never touches the prototype
   { const A=await fresh(); all.push(A);
     const evil=JSON.stringify({app:'hati2-mock',v:1,volume:{id:'volX1',title:'Flat',type:'household',currency:'$',meId:'me1',members:[{id:'me1',name:'Ana'},{id:'constructor',name:'Ben'}],expenses:[{id:'e1',desc:'Rent',amount:10000,paidBy:'me1',category:'other',date:'2026-11-01',splitType:'equal',splits:{me1:5000,constructor:5000},inputs:{}}],settlements:[]}});
     await importCode(A.pg,evil);
     ok(/doesn't contain/.test(await A.pg.locator('#sheetMsg').innerText()) && await A.pg.evaluate(()=>!S.volumes.some(v=>v.id==='volX1')),'a code with member id "constructor" is refused');
     ok(await A.pg.evaluate(()=>['toString','valueOf','hasOwnProperty'].every(id=>!validVolume({id:'v1',title:'T',members:[{id:'a',name:'A'},{id,name:'B'}],expenses:[],settlements:[]})) && !validVolume({id:'constructor',title:'T',members:[{id:'a',name:'A'}],expenses:[],settlements:[]})),'validVolume refuses every Object.prototype key as a member or volume id');
     ok(await A.pg.evaluate(()=>{ const v={expenses:[{paidBy:'me1',splits:{me1:5000,constructor:5000}}],settlements:[]}; const r=[1,2,3].map(()=>JSON.stringify(pairwise(v))); return r[0]===r[2] && /5000/.test(r[0]) && Object.me1===undefined; }),'pairwise gives the same amount on every call and writes nothing onto Object');
     ok(await A.pg.evaluate(()=>!memberUsed({expenses:[{paidBy:'a',splits:{a:1},inputs:{}}],settlements:[]},'constructor') && formFrom.toString().includes('has(e.splits')),'memberUsed/formFrom only look at own keys'); }
   // F1: storage full. Merge import and Add expense say so, change nothing, and a reload agrees
   { const B=await fresh(); all.push(B);
     const code=await B.pg.evaluate(()=>{ const v=JSON.parse(JSON.stringify(S.volumes[0])); v.expenses.push({id:'fromX',desc:'FromX',amount:300,paidBy:v.meId,splits:{[v.meId]:300},inputs:{},splitType:'exact',category:'food',date:today(),upd:Date.now()}); return JSON.stringify({app:'hati2-mock',v:1,volume:v}); });
     const full=()=>B.pg.evaluate(()=>{ window.__set=Storage.prototype.setItem; Storage.prototype.setItem=function(k,v){ if(k==='hati2.mock.v1') throw new DOMException('full','QuotaExceededError'); return window.__set.call(this,k,v); }; });
     await importCode(B.pg,code); await full(); await doImport(B.pg);
     ok(/Couldn't save/.test(await B.pg.locator('#sheetMsg').innerText()) && !/Merged/.test(await toastText(B.pg)) && await B.pg.evaluate(()=>!S.volumes[0].expenses.some(e=>e.id==='fromX')),'storage full: merge import says it could not save and changes nothing ('+await B.pg.locator('#sheetMsg').innerText()+')');
     await B.pg.keyboard.press('Escape'); await B.pg.locator('.vol').first().click();
     const n0=await B.pg.evaluate(()=>vol().expenses.length);
     await B.pg.getByRole('button',{name:/Add expense/}).click(); await B.pg.fill('#f-amount','7'); await B.pg.fill('#f-desc','Lost?'); await B.pg.locator('#expForm button[type=submit]').click();
     const t=await toastText(B.pg);
     ok(/Couldn't save/.test(t) && await B.pg.evaluate(()=>ui.view)==='expense' && await B.pg.locator('#f-desc').inputValue()==='Lost?' && await B.pg.evaluate(n=>vol().expenses.length===n,n0),'storage full: Add expense says so, keeps the form filled and adds nothing (toast "'+t+'")');
     await B.pg.evaluate(()=>{ Storage.prototype.setItem=window.__set; }); await B.pg.locator('#expForm button[type=submit]').click();
     ok(/Expense saved/.test(await toastText(B.pg)) && await B.pg.evaluate(n=>vol().expenses.length===n+1,n0),'once storage has room, the same Save works');
     await B.pg.reload(); ok(await B.pg.evaluate(()=>!S.volumes[0].expenses.some(e=>e.id==='fromX') && S.volumes[0].expenses.some(e=>e.desc==='Lost?')) && await zs(B.pg),'after reload: no merged entry that was never saved, the retried expense is there'); }
   // N3: an expense deleted in another tab while it is open for editing
   { const C=await fresh(); all.push(C); const p2=await C.c.newPage(); await p2.goto(URL);
     await C.pg.locator('.vol').first().click(); await C.pg.locator('.tx',{hasText:'Ramen'}).click(); await C.pg.fill('#f-desc','Ramen (fixed)');
     await p2.evaluate(()=>{ const v=S.volumes[0], e=v.expenses.find(x=>/Ramen/.test(x.desc)); v.expenses=v.expenses.filter(x=>x!==e); tombstone(v,e.id); save(); });
     await C.pg.waitForTimeout(150);
     ok(await C.pg.evaluate(()=>ui.view)!=='expense' && /deleted in another tab/.test(await toastText(C.pg)),'editing an expense another tab deleted: the form closes and says why');
     ok(await C.pg.evaluate(()=>{ const v=S.volumes[0]; return !v.expenses.some(e=>/Ramen/.test(e.desc)); }) && await p2.evaluate(()=>!S.volumes[0].expenses.some(e=>/Ramen/.test(e.desc))),'no edit of the deleted expense is stored');
     await p2.close(); }
   // F4 + scenario 6: Replace says what it loses, and keeps who you are
   { const D=await fresh(); all.push(D);
     const code=await D.pg.evaluate(()=>JSON.stringify({app:'hati2-mock',v:1,volume:S.volumes[0]}));
     await D.pg.evaluate(()=>{ const v=S.volumes[0]; v.meId=v.members[1].id; v.expenses.push({id:'onlyHere',desc:'OnlyHere',amount:500,paidBy:v.meId,splits:{[v.meId]:500},inputs:{},splitType:'exact',category:'food',date:today(),upd:Date.now()}); save(); render(); });
     await importCode(D.pg,code);
     const sum=()=>D.pg.locator('#s-sum').innerText();
     await pick(D.pg,'s-mode','replace'); const rs=await sum();
     ok(/1 entry only on this phone will be lost\./.test(rs),'Replace selected: summary counts what will be lost ('+rs+')');
     await pick(D.pg,'s-mode','copy'); ok(/Adds a second copy\./.test(await sum()),'Keep both selected: summary says it adds a second copy');
     await pick(D.pg,'s-mode','merge'); ok(/Your copy already has everything|Adds|Updates|Removes/.test(await sum()) && !/lost/.test(await sum()),'Merge selected: summary is the merge result again');
     await pick(D.pg,'s-mode','replace'); await doImport(D.pg);
     ok(await D.pg.evaluate(()=>{ const v=S.volumes[0]; return v.meId===v.members[1].id && !v.expenses.some(e=>e.id==='onlyHere'); }),'Replace without touching the picker keeps you as the same member (second member stays Mira)'); }
   // N2: a flood of incoming tombstones never evicts local ones
   { const E=await fresh(); all.push(E);
     ok(await E.pg.evaluate(()=>{ const v=S.volumes[0], old=JSON.parse(JSON.stringify(v)), gone=v.expenses[0].id;
       v.expenses=v.expenses.slice(1); tombstone(v,gone);
       const junk=JSON.parse(JSON.stringify(v)); junk.deleted=Array.from({length:MAX_DELETED},(_,i)=>'junk'+i);
       const r1=mergeVolumes(v,junk); if(r1.error||!r1.vol.deleted.includes(gone)) return false;
       const r2=mergeVolumes(r1.vol,old); return !r2.error && !r2.vol.expenses.some(e=>e.id===gone); }),'20000 junk tombstones in a code keep the local tombstone; an older copy cannot bring the deleted expense back'); }
   // F3: unchanged name saves even with two members of that name
   { const F=await fresh(); all.push(F);
     await F.pg.evaluate(()=>{ const v=S.volumes[0]; v.members.push({id:'samA',name:'Sam'},{id:'samB',name:'Sam'}); save(); ui.volId=v.id; go('volume'); });
     await F.pg.getByRole('button',{name:/^Sam,/}).first().click(); await F.pg.locator('#sheetForm button.primary').click();
     ok(!(await F.pg.evaluate(()=>sheetOpen())) && /Name saved/.test(await toastText(F.pg)),'Save on an unchanged name closes the sheet even with two Sams');
     // Scenario 7: Who else is in? makes the group in one sheet
     await F.pg.evaluate(()=>go('hub')); await F.pg.locator('[data-act=newVolume]').click();
     await F.pg.fill('#s-title','Flat'); await F.pg.fill('#s-others','Mira, mira'); await F.pg.locator('#sheetForm button.primary').click();
     ok(/already in this volume/.test(await F.pg.locator('#sheetMsg').innerText()) && await F.pg.evaluate(()=>!S.volumes.some(v=>v.title==='Flat')),'Who else is in: a repeated name is caught before anything is made');
     await F.pg.fill('#s-others',' Mira ,  Rio Tanaka,,'); await F.pg.locator('#sheetForm button.primary').click();
     ok(await F.pg.evaluate(()=>{ const v=vol(); return v.title==='Flat' && v.members.map(m=>m.name).join('|')==='Me|Mira|Rio Tanaka'; }),'Who else is in: "Mira, Rio Tanaka" makes a 3-member volume in one sheet');
     await F.pg.getByRole('button',{name:/Add expense/}).click(); const ah=await F.pg.evaluate(()=>$('#f-amount').getBoundingClientRect().height);
     ok(ah>=44,'Amount field is a 44px tap target ('+ah+'px)'); }
   // iOS: install hint only in Safari (not standalone, not Android), stays dismissed; status bar strip dark in every theme; backup via share sheet
   { const G=await fresh(); all.push(G);
     ok(await G.pg.locator('#iosHint').isVisible() && /Add to Home Screen/.test(await G.pg.locator('#iosHint').innerText()) && /keeps its own data/.test(await G.pg.locator('#iosHint').innerText()),'iPhone Safari: hub shows the Add to Home Screen hint with the separate-data warning');
     await G.pg.locator('[data-act=dismissIosHint]').click(); await G.pg.reload();
     ok(await G.pg.locator('#iosHint').count()===0,'the hint stays gone after OK and a reload');
     const H=await fresh(devices['iPhone 13'],()=>{ Object.defineProperty(navigator,'standalone',{get:()=>true}); }); all.push(H);
     ok(await H.pg.locator('.vol').count()>0 && await H.pg.locator('#iosHint').count()===0,'Home Screen app (navigator.standalone): no install hint');
     const K=await fresh(devices['Pixel 5']); all.push(K);
     ok(await K.pg.locator('.vol').count()>0 && await K.pg.locator('#iosHint').count()===0,'Android: no iOS install hint');
     const strip=await G.pg.evaluate(()=>{ const lum=c=>{ const m=c.match(/[\d.]+/g).map(Number); return (0.2126*m[0]+0.7152*m[1]+0.0722*m[2])/255; }; const out=[];
       for (const mode of ['light','dark']) for (const p of PALETTES) { S.settings.mode=mode; S.settings.palette=p.id; applyTheme(); out.push([mode,p.id,lum(getComputedStyle(document.body,'::before').backgroundColor)]); }
       S.settings.mode='system'; S.settings.palette='classic'; applyTheme(); return out.filter(x=>x[2]>0.35); });
     ok(strip.length===0,'strip under the iOS status bar (white text) is dark in both themes and all palettes '+JSON.stringify(strip));
     await G.pg.evaluate(()=>{ window.__files=null; navigator.canShare=d=>!!(d&&d.files); navigator.share=d=>{ window.__files=d.files.map(f=>f.name+':'+f.type); return Promise.resolve(); }; go('settings'); });
     await G.pg.locator('[data-act=backup]').click(); await G.pg.waitForTimeout(100);
     const sent=await G.pg.evaluate(()=>window.__files);
     ok(sent && /^hati2-backup-\d{4}-\d{2}-\d{2}\.json:application\/json$/.test(sent[0]) && /Backup saved/.test(await toastText(G.pg)),'iOS: Save backup hands the file to the share sheet '+JSON.stringify(sent));
     await G.pg.evaluate(()=>{ document.querySelectorAll('.toast').forEach(n=>n.remove()); navigator.share=()=>Promise.reject(new DOMException('x','AbortError')); });
     await G.pg.locator('[data-act=backup]').click(); await G.pg.waitForTimeout(100);
     ok(!/Backup saved/.test(await toastText(G.pg)),'iOS: cancelling the share sheet does not claim the backup was saved');
     await K.pg.evaluate(()=>{ window.__saved=null; window.HatiAndroid={ saveFile:(n,m,t)=>{ window.__saved=[n,m,JSON.parse(t).kind,JSON.parse(t).volumes.length]; } }; document.querySelectorAll('.toast').forEach(n=>n.remove()); go('settings'); });
     await K.pg.locator('[data-act=backup]').click(); await K.pg.waitForTimeout(100);
     const kept=await K.pg.evaluate(()=>[window.__saved,S.volumes.length]);
     ok(kept[0] && /^hati2-backup-\d{4}-\d{2}-\d{2}\.json$/.test(kept[0][0]) && kept[0][1]==='application/json' && kept[0][2]==='backup' && kept[0][3]===kept[1] && !/Backup saved/.test(await toastText(K.pg)),'Android app: Save backup goes to the app\'s save-as dialog with every volume, and does not claim it saved '+JSON.stringify(kept)); }
   // Ponytail R1: Replace counts a newer local edit and a member only on this phone; volume-only edits get their own line
   { const R=await fresh(); all.push(R);
     const code=await R.pg.evaluate(()=>JSON.stringify({app:'hati2-mock',v:1,volume:S.volumes[0]}));
     await R.pg.evaluate(()=>{ const v=S.volumes[0]; v.expenses[0].desc='LOCAL EDIT'; v.expenses[0].upd=Date.now()+5; v.members.push({id:'onlyhere1',name:'OnlyHere',upd:Date.now()}); save(); render(); });
     await importCode(R.pg,code); await pick(R.pg,'s-mode','replace'); const rs=await R.pg.locator('#s-sum').innerText();
     ok(/1 entry and 1 member only on this phone will be lost\./.test(rs),'Replace: a newer local edit and a member only on this phone are counted as lost ('+rs+')');
     await R.pg.evaluate(()=>{ dropSheet(); const v=S.volumes[0]; v.members=v.members.filter(m=>m.id!=='onlyhere1'); save(); });
     const code2=await R.pg.evaluate(()=>{ const v=S.volumes[0]; const c=JSON.parse(JSON.stringify(v)); v.title='Renamed here'; v.upd=Date.now()+10; save(); render(); return JSON.stringify({app:'hati2-mock',v:1,volume:c}); });
     await importCode(R.pg,code2); await pick(R.pg,'s-mode','replace'); const rs2=await R.pg.locator('#s-sum').innerText();
     ok(/Edits made only on this phone will be lost\./.test(rs2),'Replace: a newer local rename is not reported as nothing lost ('+rs2+')');
     await R.pg.evaluate(()=>dropSheet()); }
   // Ponytail R2: a stored volume that fails validation is copied to hati2.corrupt.* before the next save can erase it
   { const Q=await fresh(); all.push(Q);
     await Q.pg.evaluate(()=>{ const d=JSON.parse(localStorage.getItem(KEY)); d.volumes[0].expenses[0].amount+=1; localStorage.setItem(KEY,JSON.stringify(d)); });
     await Q.pg.reload();
     const r=await Q.pg.evaluate(()=>{ const ks=Object.keys(localStorage).filter(k=>k.startsWith('hati2.corrupt.')); return { vols:S.volumes.length, ks:ks.length, held: ks.some(k=>{ try { return JSON.parse(localStorage.getItem(k)).volumes.length===1; } catch(_) { return false; } }), banner:/set aside/.test(document.body.innerText) }; });
     ok(r.vols===0 && r.banner && r.ks===1 && r.held,'a stored volume that fails validation is set aside in a hati2.corrupt.* copy that holds it '+JSON.stringify(r));
     await Q.pg.evaluate(()=>{ load(); load(); });
     ok(await Q.pg.evaluate(()=>Object.keys(localStorage).filter(k=>k.startsWith('hati2.corrupt.')).length)===1,'loading the same damaged data again does not write a second copy'); }
   const errs2=all.flatMap(x=>x.e); ok(errs2.length===0,'no console errors in revise (code) checks '+JSON.stringify(errs2));
   for (const x of all) await x.c.close(); }
 // R1-clay / R1-share / R1-ioshint (design): manga clay on pressable controls only, one-tap Share, short iOS hint
 { const layers=s=>s==='none'?[]:s.split(/,(?![^(]*\))/).map(x=>x.trim());
   const nums=l=>(l.replace(/(rgba?|color)\([^)]*\)/g,'').match(/-?[\d.]+px/g)||[]).map(parseFloat);
   const offLayer=s=>layers(s).find(l=>!/inset/.test(l));
   const isClay=cs=>{ const o=offLayer(cs.boxShadow); const n=o?nums(o):[]; const r=parseFloat(cs.borderTopLeftRadius); return cs.borderTopWidth==='2px' && r>=6 && r<=10 && /inset/.test(cs.boxShadow) && !!o && n[2]===0; };
   const st=(pg,sel)=>pg.evaluate(sel=>{ const e=document.querySelector(sel); if(!e) return null; const c=getComputedStyle(e); return {boxShadow:c.boxShadow,borderTopWidth:c.borderTopWidth,borderTopLeftRadius:c.borderTopLeftRadius,borderTopRightRadius:c.borderTopRightRadius,backgroundImage:c.backgroundImage,transform:c.transform,outlineWidth:c.outlineWidth}; },sel);
   const c=await b.newContext({...devices['iPhone 13'],viewport:{width:360,height:780}}); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{if(m.type()==='error')e.push(m.text())});
   await pg.goto(URL);
   const ids=await pg.evaluate(()=>PALETTES.map(x=>x.id)); const bad=[];
   const CLAY_SET='.btn:not(.quiet):not(.text), .ico, .cat, .seg button, .vol, button.chip:not(.add)';
   for (const id of ids) for (const mode of ['light','dark']) {
     const set=async v=>pg.evaluate(([id,mode,v])=>{ S.settings.palette=id; S.settings.mode=mode; applyTheme(); dropSheet&&dropSheet(); if(v==='expense'){ ui.volId=S.volumes[0].id; ui.view='volume'; openExpense(null); } else { if(v==='volume') ui.volId=S.volumes[0].id; go(v); } },[id,mode,v]);
     const want=async(sel,clay,tag)=>{ const s=await st(pg,sel); if(!s) { bad.push(`${id}/${mode} ${tag||sel}: missing`); return; } if(clay? !isClay(s) : (layers(s.boxShadow).some(l=>/inset/.test(l)&&nums(l)[2]>0)||s.borderTopLeftRadius!==(tag==='sheet'?'6px':'3px'))) bad.push(`${id}/${mode} ${tag||sel}: ${s.borderTopWidth} r${s.borderTopLeftRadius} ${s.boxShadow}`); };
     await set('hub'); await want('.vol',true); await want('.topbar [data-act=settings]',true,'hub gear');
     await set('volume'); await want('.btn.primary',true); await want('.ico',true); await want('button.chip:not(.add)',true); await want('.ledger',false); await want('.hero',false);
     await want('.chip.add',false,'chip.add (flat, 3px)');
     await set('expense'); await want('.cat[aria-pressed="false"]',true); await want('.cat[aria-pressed="true"]',true); await want('.input',false);
     const pc=await st(pg,'.cat[aria-pressed="true"]'); if(!/^0px 0px 0px/.test(nums(offLayer(pc.boxShadow)).slice(0,3).map(x=>x+'px').join(' '))) bad.push(`${id}/${mode} selected cat not pushed in: ${pc.boxShadow}`);
     await set('settings'); await want('.panel',false); await want('.pal',false,'pal (flat swatch)');
     const pal=await st(pg,'.pal'); if(/inset/.test(pal.boxShadow)) bad.push(`${id}/${mode} .pal has inset`);
     await pg.evaluate(()=>{ ui.volId=S.volumes[0].id; go('volume'); volMenuSheet(); }); await want('.sheet',false,'sheet');
     const grad=await pg.evaluate(sel=>[...document.querySelectorAll(sel)].filter(x=>getComputedStyle(x).backgroundImage!=='none').map(x=>x.className),CLAY_SET); if(grad.length) bad.push(`${id}/${mode} gradient on clay: ${grad}`);
     await pg.evaluate(()=>dropSheet()); }
   ok(bad.length===0,'clay: clay set has 2px ink, 6-10px corners, inset light/shade and a 0-blur offset; reading surfaces stay 3px with no clay shading (frosted light lines are 0-blur); no gradients on clay; .pal and .chip.add flat, in 13 palettes x light/dark '+JSON.stringify(bad.slice(0,4)));
   // Disabled clay is flat
   await pg.evaluate(()=>{ S.settings.palette='classic'; S.settings.mode='light'; applyTheme(); go('hub'); $('#app').insertAdjacentHTML('beforeend','<button class="btn primary" id="__dis" disabled>x</button>'); });
   ok((await st(pg,'#__dis')).boxShadow==='none','clay: a disabled .btn has no shadow at all');
   // Pressed: squash (normal motion) and squash shading without movement (reduced motion)
   const press=async(P,sel)=>{ await P.evaluate(sel=>document.querySelector(sel).scrollIntoView({block:'center'}),sel); const r=await P.locator(sel).first().boundingBox(); const rest=await st(P,sel); await P.mouse.move(r.x+r.width/2,r.y+r.height/2); await P.mouse.down(); await P.waitForTimeout(150); const held=await st(P,sel); await P.mouse.move(1,1); await P.mouse.up(); return {rest,held}; };
   const shade=s=>{ const ins=layers(s).filter(l=>/inset/.test(l)); return ins.reduce((a,l)=>a+Math.abs(nums(l)[2]||0),0); };
   for (const [sel,view] of [['.btn.primary','volume'],['.vol','hub']]) {
     await pg.evaluate(v=>{ ui.volId=S.volumes[0].id; go(v); },view);
     const {rest,held}=await press(pg,sel); const m=/matrix\(1, 0, 0, 1, ([\d.]+), ([\d.]+)\)/.exec(held.transform)||[];
     { const lift=nums(offLayer(rest.boxShadow))[0]; ok(+m[1]===lift && +m[2]===lift && lift===3 && nums(offLayer(held.boxShadow)).slice(0,3).every(x=>x===0) && shade(held.boxShadow)>shade(rest.boxShadow),`clay: held ${sel} moves exactly onto its ${lift}px slab (controls 3, volume cards are inner cards: 3), loses its offset and its inner shade grows (${held.transform}; ${held.boxShadow})`); } }
   { const rc=await b.newContext({...devices['iPhone 13'],viewport:{width:360,height:780},reducedMotion:'reduce'}); const R=await rc.newPage(); await R.goto(URL);
     for (const [sel,view] of [['.btn.primary','volume'],['.vol','hub']]) {
       await R.evaluate(v=>{ ui.volId=S.volumes[0].id; go(v); },view);
       const {rest,held}=await press(R,sel);
       ok(held.transform==='none' && held.boxShadow!==rest.boxShadow,`clay, reduced motion: held ${sel} does not move but still squashes (${held.transform})`); }
     await rc.close(); }
   // Focus ring survives clay (classic light and dark)
   for (const mode of ['light','dark']) {
     await pg.evaluate(m=>{ S.settings.palette='classic'; S.settings.mode=m; applyTheme(); },mode);
     for (const [sel,view] of [['.btn.primary','volume'],['.cat','expense'],['.vol','hub']]) {
       await pg.evaluate(v=>{ if(v==='expense'){ ui.volId=S.volumes[0].id; ui.view='volume'; openExpense(null); } else { ui.volId=S.volumes[0].id; go(v); } },view);
       await pg.evaluate(sel=>document.querySelector(sel).focus(),sel); await pg.keyboard.press('Tab'); await pg.keyboard.press('Shift+Tab'); await pg.waitForTimeout(200);
       const s=await st(pg,sel); const vis=await pg.evaluate(sel=>document.querySelector(sel).matches(':focus-visible'),sel);
       ok(vis && s.outlineWidth==='3px' && /0px 0px 0px 6px/.test(s.boxShadow) && /inset/.test(s.boxShadow),`clay focus (${mode}) on ${sel}: 3px outline plus the 6px accent ring on top of the clay `+JSON.stringify([vis,s.outlineWidth,s.boxShadow])); } }
   await pg.evaluate(()=>{ S.settings.mode='light'; applyTheme(); });
   // Rows of clay buttons share height and text baseline (±1px)
   for (const w of [360,390]) { await pg.setViewportSize({width:w,height:780});
     const row=async sels=>pg.evaluate(sels=>sels.map(s=>{ const el=document.querySelector(s); const r=el.getBoundingClientRect(); const t=[...el.childNodes].filter(n=>n.nodeType===3&&n.textContent.trim()).pop(); const rg=document.createRange(); rg.selectNodeContents(t); const tr=rg.getBoundingClientRect(); return {h:r.height,base:tr.bottom-r.top,top:r.top}; }),sels);
     await pg.evaluate(()=>{ ui.volId=S.volumes[0].id; go('volume'); });
     const a=await row(['[data-act=addExpense]','[data-act=settle]','.acts [data-act=share]']);
     ok(Math.max(...a.map(x=>x.h))-Math.min(...a.map(x=>x.h))<=1 && Math.max(...a.map(x=>x.base))-Math.min(...a.map(x=>x.base))<=1 && Math.abs(a[1].top-a[2].top)<=1,`clay rows at ${w}: Add expense, Settle up and Share share height and baseline ${JSON.stringify(a)}`);
     await pg.evaluate(()=>go('settings'));
     const d=await row(['[data-act=backup]','[data-act=restore]','[data-act=seed]','[data-act=reset]']);
     ok(Math.max(...d.map(x=>x.h))-Math.min(...d.map(x=>x.h))<=1 && Math.abs(d[0].base-d[1].base)<=1 && Math.abs(d[2].base-d[3].base)<=1 && Math.abs(d[0].top-d[1].top)<=1,`clay rows at ${w}: Settings Data pairs share height and baseline`); }
   // R1-share: one tap from the volume page, menu entry still there
   await pg.setViewportSize({width:360,height:780}); await pg.evaluate(()=>{ ui.volId=S.volumes[0].id; go('volume'); });
   const sb=pg.getByRole('button',{name:'Share volume',exact:true});
   ok(await sb.count()===1 && await sb.isVisible(),'Share volume is a visible button on the volume page, without opening a menu');
   await sb.click(); await pg.locator('.sheet .share-sum').waitFor({timeout:3000}).catch(()=>{}); ok(await pg.locator('.sheet .share-sum').isVisible(),'one tap on Share volume opens the share sheet');
   await pg.evaluate(()=>dropSheet()); await pg.locator('[data-act=volMenu]').click(); await pg.locator('.sheet [data-act=share]').click();
   await pg.locator('.sheet .share-sum').waitFor({timeout:3000}).catch(()=>{}); ok(await pg.locator('.sheet .share-sum').isVisible(),'the ⋮ menu Share entry still opens the share sheet'); await pg.evaluate(()=>dropSheet());
   for (const w of [320,360]) { await pg.setViewportSize({width:w,height:780});
     await pg.evaluate(()=>{ const v=exampleVolume(); v.title='Bartholomew and friends: summer road trip 2026'.slice(0,40); v.members[1].name='Maximilian Featherstonehaugh-Wolfe'; S.volumes.unshift(v); ui.volId=v.id; go('volume'); });
     const m=await pg.evaluate(()=>{ const h=$('.topbar h1'); return {hw:h.getBoundingClientRect().width,lines:Math.round(h.getBoundingClientRect().height/parseFloat(getComputedStyle(h).lineHeight)),sw:document.scrollingElement.scrollWidth,cw:document.scrollingElement.clientWidth}; });
     ok(m.hw>=110 && m.lines>=2 && m.sw<=m.cw,`long title at ${w}: h1 ≥110px wide, 2 lines, no sideways scroll ${JSON.stringify(m)}`);
     await pg.evaluate(()=>{ S.volumes.shift(); }); }
   ok(e.length===0,'no console errors in clay checks '+JSON.stringify(e));
   await c.close(); }
 // R2-4 (design): quiet buttons share the clay corner, no resize grip on the share code, one-line hub tagline, selected .seg shade readable in dark
 { const c=await b.newContext({...devices['iPhone 13'],viewport:{width:360,height:780},deviceScaleFactor:1}); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); await pg.goto(URL);
   const ids=await pg.evaluate(()=>PALETTES.map(x=>x.id)); const bad=[], segBad=[];
   const lum=([r,g,b])=>{const f=c=>{c/=255;return c<=.03928?c/12.92:((c+.055)/1.055)**2.4};return .2126*f(r)+.7152*f(g)+.0722*f(b)}; const cr=(a,b)=>{const x=lum(a),y=lum(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05)};
   for (const id of ids) for (const mode of ['light','dark']) {
     const r=await pg.evaluate(([id,mode])=>{ S.settings.palette=id; S.settings.mode=mode; applyTheme(); dropSheet(); go('hub'); const q=getComputedStyle($('.btn.quiet')), qr=q.borderTopLeftRadius, qs=q.boxShadow; ui.volId=S.volumes[0].id; go('volume'); return [qr,getComputedStyle($('.btn.primary')).borderTopLeftRadius,qs]; },[id,mode]);
     if (r[0]!==r[1] || r[2]!=='none') bad.push(`${id}/${mode} quiet r${r[0]} vs primary r${r[1]} ${r[2]}`);
     if (mode==='dark') {
       await pg.evaluate(()=>{ ui.view='volume'; openExpense(null); }); const el=pg.locator('.seg button[aria-pressed="true"]').first(); await el.scrollIntoViewIfNeeded(); await pg.waitForTimeout(120);
       const box=await el.boundingBox(); const buf=await pg.screenshot({clip:box}); const fgs=await el.evaluate(x=>getComputedStyle(x).color), fg=fgs.match(/[\d.]+/g).slice(0,3).map(Number).map(v=>fgs.startsWith('color(')?v*255:v);
       const dec=await pg.context().newPage(); const px=await dec.evaluate(async b64=>{ const im=new Image(); im.src='data:image/png;base64,'+b64; await im.decode(); const cv=document.createElement('canvas'); cv.width=im.width; cv.height=im.height; const x=cv.getContext('2d'); x.drawImage(im,0,0); const d=x.getImageData(0,0,cv.width,cv.height).data; const at=(i,j)=>{const k=(j*cv.width+i)*4; return [d[k],d[k+1],d[k+2]];}; let min=at(cv.width-6,cv.height-6); const fill=min; for(let j=0;j<cv.height;j++) for(let i=0;i<cv.width;i++){ if((i>=12&&j>=12)||((i<8||i>=cv.width-8)&&(j<8||j>=cv.height-8))) continue; /* the container's rounded corner is its frame, not shade */ const v=at(i,j); if(v[0]+v[1]+v[2]<min[0]+min[1]+min[2]) min=v; } return {fill,min}; },buf.toString('base64')); await dec.close();
       if (cr(fg,px.min)<4.5 || cr(px.fill,px.min)>2.5) segBad.push(`${id} text/shade ${cr(fg,px.min).toFixed(2)} fill/shade ${cr(px.fill,px.min).toFixed(2)}`);
       await pg.evaluate(()=>dropSheet()); } }
   ok(bad.length===0,'R2-4: .btn.quiet has the same corner as .btn.primary and stays flat, 13 palettes x light/dark '+JSON.stringify(bad.slice(0,4)));
   ok(segBad.length===0,'R2-4: selected .seg in dark keeps text ≥4.5:1 against its darkest shade pixel and the shade stays light (≤2.5:1 vs the fill), 13 palettes '+JSON.stringify(segBad.slice(0,4)));
   await pg.evaluate(()=>{ S.settings.palette='classic'; S.settings.mode='light'; applyTheme(); ui.volId=S.volumes[0].id; go('volume'); return shareSheet(); });
   ok(await pg.evaluate(()=>getComputedStyle($('#s-code')).resize)==='none','R2-4: the share code box has no resize grip');
   await pg.evaluate(()=>dropSheet());
   for (const w of [360,390]) { await pg.setViewportSize({width:w,height:780}); await pg.evaluate(()=>go('hub'));
     const t=await pg.evaluate(()=>{ const el=$('.tagline'); return {lines:Math.round(el.getBoundingClientRect().height/parseFloat(getComputedStyle(el).lineHeight)),text:el.textContent,gloss:/trip or group/.test($('[data-act=newVolume]').textContent)}; });
     ok(t.lines===1 && t.gloss,`R2-4: hub tagline is one line at ${w} and "New volume" still says what a volume is ${JSON.stringify(t)}`); }
   ok(e.length===0,'no page errors in R2-4 checks '+JSON.stringify(e));
   await c.close(); }
 // DEPTH (owner 2026-11-09, direction A "layered"): hard 0-blur ink slabs by rank (controls 3px, cards and page panels 5px,
 // the hero an 8px dotted slab), a dotted L shade on the card's far edges, a stepped dot ramp on the hero only, and a header
 // with a 3px ink edge and an 8px screentone band that stays the same while the page scrolls. 13 palettes x light/dark.
 { const c=await b.newContext({...devices['iPhone 13'],viewport:{width:360,height:780},deviceScaleFactor:1}); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{if(m.type()==='error')e.push(m.text())});
   await pg.goto(URL);
   const ids=await pg.evaluate(()=>PALETTES.map(x=>x.id)); const bad=[], band=[], flat=[];
   for (const id of ids) for (const mode of ['light','dark']) {
     const r=await pg.evaluate(([id,mode])=>{ S.settings.palette=id; S.settings.mode=mode; applyTheme(); dropSheet(); const out={};
       const lay=s=>s==='none'?[]:s.split(/,(?![^(]*\))/).map(x=>x.trim());
       const off=el=>{ const l=lay(getComputedStyle(el).boxShadow).find(x=>!/inset/.test(x)); if(!l) return null; const n=(l.replace(/(rgba?|color)\([^)]*\)/g,'').match(/-?[\d.]+px/g)||[]).map(parseFloat); return n.slice(0,3).join('/'); };
       const blurs=el=>lay(getComputedStyle(el).boxShadow).filter(x=>!/inset/.test(x)).map(l=>(l.replace(/(rgba?|color)\([^)]*\)/g,'').match(/-?[\d.]+px/g)||[]).map(parseFloat)[2]).filter(x=>x>0).length;
       go('hub'); out.vol=off($('.vol')); out.gear=off($('.topbar [data-act=settings]')); const vi=getComputedStyle($('.vol-info'));
       out.strip=[(vi.backgroundImage.match(/radial-gradient/g)||[]).length, /repeat-x/.test(vi.backgroundRepeat)&&/repeat-y/.test(vi.backgroundRepeat), /linear-gradient/.test(vi.backgroundImage)];
       const tb=getComputedStyle($('.topbar')), ta=getComputedStyle($('.topbar'),'::after');
       out.head=[tb.borderBottomWidth, getComputedStyle($('.vol')).borderTopWidth, ta.content, ta.height, ta.position, /radial-gradient/.test(ta.backgroundImage), /(\/ 0\.[0-9]+\)|rgba\([^)]*, 0\.[0-9]+\))/.test(tb.backgroundColor), tb.boxShadow, tb.filter, tb.backdropFilter];
       ui.volId=S.volumes[0].id; go('volume');
       out.sect=off($('#app .sect')); out.btn=off($('.btn.primary')); out.ico=off($('.topbar .ico')); out.chip=off($('button.chip:not(.add)')); out.ledger=off($('#app .ledger'));
       const h=$('.hero'), hs=getComputedStyle(h), ha=getComputedStyle(h,'::after');
       const z=l=>(l.replace(/(rgba?|color)\([^)]*\)/g,'').match(/-?[\d.]+px/g)||[]).map(parseFloat);
       out.hero=[lay(hs.boxShadow).every(l=>/inset/.test(l)&&z(l)[2]===0)?'none':hs.boxShadow, hs.marginBottom, ha.top, ha.left, ha.right, ha.bottom, ha.borderTopWidth, /radial-gradient/.test(ha.backgroundImage), /8px/.test(ha.clipPath), (hs.backgroundImage.match(/radial-gradient/g)||[]).length, (hs.backgroundImage.match(/linear-gradient/g)||[]).length>1||/conic/.test(hs.backgroundImage), hs.maskImage||hs.webkitMaskImage];
       // the heavy end of the ramp (the 5 columns with dots over 1.2px: the right 30px of the padding box) holds no text
       const hr=h.getBoundingClientRect(), heavyL=hr.right-2-30; out.heavy=[...h.querySelectorAll('*')].filter(x=>[...x.childNodes].some(n=>n.nodeType===3&&n.textContent.trim())).map(x=>{ const g=document.createRange(); g.selectNodeContents(x); return g.getBoundingClientRect().right; }).filter(x=>x>heavyL).length;
       out.rows=[...document.querySelectorAll('#app .ledger, #app .ledger > *, #app .tx, #app .debt')].reduce((a,x)=>a+blurs(x),0);
       out.filters=[...document.querySelectorAll('#app *')].filter(x=>{ const s=getComputedStyle(x); return s.filter!=='none'||s.backdropFilter!=='none'; }).length;
       go('settings'); out.panel=off($('#app .panel')); ui.volId=S.volumes[0].id; ui.view='volume'; openExpense(null); out.cat=off($('.cat[aria-pressed="false"]')); out.seg=off($('.seg')); dropSheet();
       return out; },[id,mode]);
     const why=[];
     for (const k of ['gear','btn','ico','chip','cat','seg']) if (r[k]!=='3/3/0') why.push(`${k} ${r[k]}`);
     for (const k of ['vol','ledger','panel']) if (r[k]!=='3/3/0') why.push(`${k} ${r[k]}`);
     if (r.sect!=='5/5/0') why.push(`sect ${r.sect}`);
     const H=r.hero; if (H[0]!=='none'||H[1]!=='8px'||H[2]!=='6px'||H[3]!=='6px'||H[4]!=='-10px'||H[5]!=='-10px'||H[6]!=='2px'||!H[7]||!H[8]||H[9]!==9||H[10]||(H[11]&&H[11]!=='none')) why.push('hero '+JSON.stringify(H));
     if (r.heavy) why.push(`${r.heavy} text runs reach the heavy end`);
     if (r.strip[0]!==4||!r.strip[1]||r.strip[2]) why.push('card strip '+JSON.stringify(r.strip));
     if (why.length) bad.push(`${id}/${mode}: ${why.join(', ')}`);
     const T=r.head; if (T[0]!=='2px'||T[1]!=='2px'||T[2]!=='""'||T[3]!=='10px'||T[4]!=='absolute'||!T[5]||!T[6]||!/0px 3px 0px/.test(T[7])||/\d+px \d+px [1-9]/.test(T[7].replace(/inset[^,]*/g,''))||T[8]!=='none'||T[9]!=='none') band.push(`${id}/${mode} ${JSON.stringify(T)}`);
     if (r.rows||r.filters) flat.push(`${id}/${mode} rows with blur ${r.rows}, filters ${r.filters}`); }
   ok(bad.length===0,'DEPTH: offsets by rank — controls 3px (gear, primary, icon, chip, category, segmented), section cards 5px, inner cards 3px (volume card, ledger, panel), hero 8px dotted slab (frosted: 0-blur light lines, one sheen) (pseudo-element, 8px margin under it, 9-column dot ramp, no gradient fade or mask), heavy ramp end free of text, card shade is an L (right and bottom), all 0-blur, 13 palettes x light/dark '+JSON.stringify(bad.slice(0,3)));
   ok(band.length===0,'DEPTH: header is manga acetate in both themes — translucent cel (alpha < 1), 2px ink edge, a 3px 0-blur slab, a second dotted cel (10px) behind it, no filter or backdrop-filter, 13 palettes x light/dark '+JSON.stringify(band.slice(0,3)));
   ok(flat.length===0,'DEPTH: paint cost — no blurred box-shadow on ledgers or list rows and no filter/backdrop-filter anywhere in the volume page, 13 palettes x light/dark '+JSON.stringify(flat.slice(0,3)));
   // The header is static: scrolling a long history changes neither its edge, its band nor where they are drawn
   await pg.evaluate(()=>{ S.settings.palette='classic'; S.settings.mode='light'; applyTheme(); const v=S.volumes[0], ids=v.members.map(m=>m.id); for (let i=0;i<40;i++) v.expenses.push({id:'dp'+i,desc:'Lunch '+i,amount:1000,paidBy:ids[0],category:'food',date:'2026-01-01',splitType:'equal',splits:splitEqual(1000,ids),inputs:{},note:'',upd:1}); ui.volId=v.id; go('history'); scrollTo(0,0); });
   const hs=async()=>pg.evaluate(()=>{ const t=$('.topbar'), s=getComputedStyle(t), a=getComputedStyle(t,'::after'); return JSON.stringify([Math.round(t.getBoundingClientRect().top),Math.round(t.getBoundingClientRect().bottom),s.borderBottom,s.boxShadow,s.background,a.background,a.height,a.top,a.opacity]); });
   const top0=await hs(); await pg.mouse.move(180,500); await pg.mouse.wheel(0,900); await pg.waitForTimeout(300); const sy=await pg.evaluate(()=>scrollY); const top1=await hs();
   ok(sy>300 && top0===top1,`DEPTH: the header and its band look and sit the same after scrolling ${sy}px (no scroll-linked shadow) ${top0===top1?'':top0+' vs '+top1}`);
   await pg.evaluate(()=>{ S.volumes[0].expenses=S.volumes[0].expenses.filter(x=>!/^dp/.test(x.id)); scrollTo(0,0); });
   // Slabs stay on the screen at phone widths: nothing scrolls sideways and the hero's slab ends inside the gutter
   for (const w of [320,360,390]) { await pg.setViewportSize({width:w,height:780});
     const m=await pg.evaluate(()=>{ ui.volId=S.volumes[0].id; go('volume'); const h=$('.hero').getBoundingClientRect(); return {sw:document.scrollingElement.scrollWidth,cw:document.scrollingElement.clientWidth,slabR:h.right+8,vw:innerWidth}; });
     ok(m.sw<=m.cw && m.slabR<=m.vw-8,`DEPTH: at ${w}px no sideways scroll and the hero slab ends ≥8px from the screen edge ${JSON.stringify(m)}`); }
   ok(e.length===0,'no page errors in DEPTH checks '+JSON.stringify(e));
   await c.close(); }
 // ALL-OUT (design, 2026-11-11): every card-like block on every view and sheet stands on a non-zero 0-blur ink offset;
 // no filter, backdrop-filter or blurred outer shadow anywhere; the header cel is translucent and its grey words hold 4.5:1.
 { const c=await b.newContext({...devices['iPhone 13'],viewport:{width:360,height:780},deviceScaleFactor:1}); await c.addInitScript(noSync); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message));
   await pg.goto(URL); const bad=[], blur=[], head=[];
   const scenes=[['hub',()=>go('hub')],['volume',()=>{ ui.volId=S.volumes[0].id; go('volume'); }],['history',()=>{ ui.volId=S.volumes[0].id; go('history'); }],
     ['expense',()=>{ ui.volId=S.volumes[0].id; ui.view='volume'; openExpense(null); }],['settings',()=>go('settings')],
     ['empty volume',()=>{ S.volumes=S.volumes.filter(v=>v.id!=='ao1'); S.volumes.push({id:'ao1',title:'Flat',type:'home',currency:'$',meId:'a',created:Date.now(),members:[{id:'a',name:'Ana'},{id:'b',name:'Rio'}],expenses:[],settlements:[]}); ui.volId='ao1'; go('volume'); }],
     ['settle sheet',()=>{ ui.volId=S.volumes[0].id; go('volume'); settleSheet(null); }],['edit volume sheet',()=>{ ui.volId=S.volumes[0].id; go('volume'); volumeSheet(S.volumes[0]); }],
     ['toast',()=>{ go('hub'); toast('Expense saved'); }]];
   for (const mode of ['light','dark']) for (const [name,open] of scenes) {
     await pg.evaluate(([mode,name])=>{ S.settings.palette='classic'; S.settings.mode=mode; applyTheme(); dropSheet(); document.querySelectorAll('.toast').forEach(n=>n.remove()); },[mode,name]);
     await pg.evaluate(`(${open.toString()})()`);
     const r=await pg.evaluate(()=>{ const lay=s=>s==='none'?[]:s.split(/,(?![^(]*\))/).map(x=>x.trim()), num=l=>(l.replace(/(rgba?|color)\([^)]*\)/g,'').match(/-?[\d.]+px/g)||[]).map(parseFloat);
       const cards=[...document.querySelectorAll('#app :is(.sect, .panel, .ledger, .vol, .amount-box), .sheet :is(.panel, .ledger, .share-sum), .toast')].filter(x=>x.getClientRects().length);
       const flat=cards.filter(x=>{ const o=lay(getComputedStyle(x).boxShadow).filter(l=>!/inset/.test(l)).map(num); return !o.some(n=>(n[0]>0||n[1]>0)&&!n[2]); }).map(x=>x.className);
       const blurred=[...document.querySelectorAll('*')].flatMap(x=>[null,'::before','::after'].map(pe=>{ const s=getComputedStyle(x,pe); const o=lay(s.boxShadow).filter(l=>!/inset/.test(l)).some(l=>num(l)[2]>0);
         const dof=!pe&&x.id==='app'&&x.inert&&/^blur\(([0-2](\.\d+)?|3)px\)$/.test(s.filter);
         return (s.filter!=='none'&&!dof)||s.backdropFilter!=='none'||o||(s.textShadow!=='none')?(x.id||x.className)+(pe||''):null; })).filter(Boolean);
       return {n:cards.length,flat,blurred}; });
     if (!r.n||r.flat.length) bad.push(`${mode}/${name}: ${r.n} cards, flat: ${r.flat.slice(0,3)}`);
     if (r.blurred.length) blur.push(`${mode}/${name}: ${r.blurred.slice(0,3)}`); }
   // header cel: translucent, its grey text 4.5:1 composited over the page and over ink scrolling under it, 13 plates x 2 themes
   const ids=await pg.evaluate(()=>PALETTES.map(x=>x.id));
   for (const id of ids) for (const mode of ['light','dark']) {
     const r=await pg.evaluate(([id,mode])=>{ S.settings.palette=id; S.settings.mode=mode; applyTheme(); dropSheet(); go('settings');
       const t=$('.topbar'); t.insertAdjacentHTML('beforeend','<span class="muted" id="__celsub">x</span>');
       const P=c=>{ const n=(c.match(/[\d.]+/g)||[0,0,0]).map(Number); if (/^color\(srgb/.test(c)) { n[0]*=255; n[1]*=255; n[2]*=255; } if (n.length<4) n[3]=1; return n; };
       const lin=v=>{ v/=255; return v<=0.03928?v/12.92:((v+0.055)/1.055)**2.4; }, L=([r,g,b])=>0.2126*lin(r)+0.7152*lin(g)+0.0722*lin(b);
       const over=(a,u)=>[0,1,2].map(i=>a[i]*a[3]+u[i]*(1-a[3]));
       const cel=P(getComputedStyle(t).backgroundColor), page=P(getComputedStyle(document.body).backgroundColor), ink=P(getComputedStyle(document.body).color);
       const cr=(f,g)=>{ const x=L(f), y=L(g); return (Math.max(x,y)+0.05)/(Math.min(x,y)+0.05); };
       const sub=P(getComputedStyle($('#__celsub')).color), h1=P(getComputedStyle($('.topbar h1')).color); $('#__celsub').remove();
       return {a:cel[3],sub:Math.min(cr(sub,over(cel,page)),cr(sub,over(cel,ink))),h1:Math.min(cr(h1,over(cel,page)),cr(h1,over(cel,ink))),bf:getComputedStyle(t).backdropFilter}; },[id,mode]);
     if (!(r.a>=0.85&&r.a<=0.92)||r.sub<4.5||r.h1<4.5||r.bf!=='none') head.push(`${id}/${mode} ${JSON.stringify(r)}`); }
   ok(bad.length===0,'ALL-OUT: every card-like block (cards, panels, ledgers, amount box, sheet ledgers and summaries, toast) on 9 views and sheets has a non-zero 0-blur ink offset, light and dark '+JSON.stringify(bad.slice(0,3)));
   ok(blur.length===0,'ALL-OUT: no filter, backdrop-filter, text-shadow or blurred outer shadow on any element or pseudo-element, 9 views and sheets, light and dark '+JSON.stringify(blur.slice(0,3)));
   ok(head.length===0,'ALL-OUT: header acetate cel is 85-92% opaque with no backdrop-filter; its title and grey words hold 4.5:1 over the page and over ink under it, 13 palettes x light/dark '+JSON.stringify(head.slice(0,3)));
   ok(e.length===0,'no page errors in ALL-OUT checks '+JSON.stringify(e));
   await c.close(); }
 // L3-dark (design): ink on night paper. Layer steps, chalk frames quieter than text, a chalk-ink offset slab (R2 revise), plate-tinted hero, selected
 // states mixed from chalk (never diluted accent), sheet lifts off a darker scrim, one dark source (no drift), status bar
 // colour = night paper, focus ring and toasts in dark. All measured from computed colours in 13 palettes.
 { const c=await b.newContext({...devices['iPhone 13'],viewport:{width:360,height:780},deviceScaleFactor:1}); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); await pg.goto(URL);
   const ids=await pg.evaluate(()=>PALETTES.map(x=>x.id)); const bad=[], drift=[];
   const TOK=['--bg','--fg','--sub','--panel','--sunk','--line','--rule','--off','--accent','--on-accent','--hero','--chip','--sel-bg','--sel-fg','--tone','--nav','--sbar','--scrim','--sheet','--field','--clay-hi','--clay-lo','--clay-sel','--pos','--neg','--err'];
   const snap=()=>pg.evaluate(TOK=>{ const pr=document.createElement('i'); document.body.appendChild(pr); const o={}; for(const k of TOK){ pr.style.background='var('+k+')'; o[k]=getComputedStyle(pr).backgroundColor; } pr.remove();
     const pal=document.createElement('div'); pal.innerHTML='<button class="pal" aria-checked="true"><span class="n">x</span></button>'; document.body.appendChild(pal); const pc=getComputedStyle(pal.firstChild); o.pal=[pc.backgroundColor,pc.color,pc.borderTopColor,pc.boxShadow,getComputedStyle(pal.querySelector('.n')).color].join('|'); pal.remove(); return o; },TOK);
   const rgba=s=>{ const m=s.match(/[\d.]+/g).map(Number), k=s.startsWith('color(')?255:1; return {c:m.slice(0,3).map(x=>x*k),a:m.length>3?m[3]:1}; };
   const hex=s=>'#'+C(s).map(x=>Math.round(x).toString(16).padStart(2,'0').toUpperCase()).join('');
   const over=(t,b)=>{ const x=rgba(t), y=rgba(b).c; return x.c.map((v,i)=>v*x.a+y[i]*(1-x.a)); };
   const lum=([r,g,b])=>{const f=c=>{c/=255;return c<=.04045?c/12.92:((c+.055)/1.055)**2.4};return .2126*f(r)+.7152*f(g)+.0722*f(b)}; const cr=(a,b)=>{const x=lum(a),y=lum(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05)};
   const C=s=>rgba(s).c;
   for (const id of ids) {
     await pg.emulateMedia({colorScheme:'dark'});
     await pg.evaluate(id=>{ S.settings.palette=id; S.settings.mode='system'; applyTheme(); },id); const sys=await snap();
     await pg.evaluate(()=>{ S.settings.mode='dark'; applyTheme(); }); const t=await snap();
     for (const k of Object.keys(t)) if (t[k]!==sys[k]) drift.push(`${id} ${k}: ${t[k]} vs system ${sys[k]}`);
     const [bg,fg,panel,sunk,line,rule,off,hero,sel,selfg,acc,chip,sheet,field]=['--bg','--fg','--panel','--sunk','--line','--rule','--off','--hero','--sel-bg','--sel-fg','--accent','--chip','--sheet','--field'].map(k=>C(t[k]));
     const r={ pb:cr(panel,bg), lp:cr(line,panel), rp:cr(rule,panel), rb:cr(rule,bg), fb:cr(fg,bg), ob:cr(off,bg), oh:cr(off,hero), hp:cr(hero,panel), fh:cr(fg,hero),
       tone:cr(over(t['--tone'],t['--hero']),hero), sa:cr(sel,acc), sp:cr(sel,panel), fs:cr(selfg,sel), cp:cr(chip,panel), fc:cr(C(t['--on-accent']),chip), fld:cr(field,sheet),
       sheet:cr(sheet,over(t['--scrim'],t['--bg'])), ring:cr(fg,panel), halo:cr(acc,bg) };
     const order=lum(bg)<lum(sunk) && lum(sunk)<lum(panel);
     await pg.evaluate(()=>{ ui.volId=S.volumes[0].id; go('volume'); }); const meta=C(await pg.evaluate(()=>getComputedStyle($('.hero .meta')).color)); r.mh=cr(meta,hero);
     const tc=await pg.evaluate(()=>[document.querySelector('meta[name=theme-color]').content,getComputedStyle(document.body).backgroundColor,getComputedStyle(document.body,'::before').backgroundColor]);
     const why=[];
     if(!order) why.push('bg<sunk<panel'); if(r.pb<1.25) why.push('panel/bg '+r.pb.toFixed(2)); if(r.lp<3) why.push('line/panel '+r.lp.toFixed(2)); if(r.rp<3) why.push('rule/panel '+r.rp.toFixed(2)); if(!(r.rb<r.fb)) why.push('rule as loud as text');
     if(r.ob<3 || r.oh<3 || r.ob>r.rb+0.01) why.push(`off/bg ${r.ob.toFixed(2)}, off/hero ${r.oh.toFixed(2)} not ≥3 (and ≤ rule/bg ${r.rb.toFixed(2)})`); if(r.hp<1.15) why.push('hero/panel '+r.hp.toFixed(2)); if(r.fh<4.5||r.mh<4.5) why.push(`hero text ${r.fh.toFixed(2)}/${r.mh.toFixed(2)}`);
     if(r.tone<1.3||r.tone>=1.6) why.push('screentone '+r.tone.toFixed(2)); if(r.sp<3||r.fs<4.5) why.push(`selected seg ${r.sp.toFixed(2)}/${r.fs.toFixed(2)}`); if(r.cp<3||r.fc<4.5) why.push(`selected cat ${r.cp.toFixed(2)}/${r.fc.toFixed(2)}`);
     if(r.fld<1.15) why.push('field/sheet '+r.fld.toFixed(2)); if(r.sheet<1.3) why.push('sheet/scrimmed page '+r.sheet.toFixed(2)); if(r.ring<3||r.halo<3) why.push(`focus ${r.ring.toFixed(2)}/${r.halo.toFixed(2)}`);
     if(tc[0]!==hex(tc[1]) || tc[1]!==tc[2]) why.push('theme-color '+tc.join(' / '));
     if(why.length) bad.push(id+': '+why.join(', '));
     // the rendered selected segment and category really use the selected fill
     await pg.evaluate(()=>{ ui.view='volume'; openExpense(null); });
     const rs=await pg.evaluate(()=>[getComputedStyle($('.seg button[aria-pressed="true"]')).backgroundColor,getComputedStyle($('.seg button[aria-pressed="true"]')).color,getComputedStyle($('.cat[aria-pressed="true"]')).backgroundColor,getComputedStyle($('.cat[aria-pressed="true"]')).color,getComputedStyle($('.cat[aria-pressed="true"]')).getPropertyValue('--clay-lo').trim(),getComputedStyle($('.input')).backgroundColor,getComputedStyle($('.btn.primary')||$('.sheet')).backgroundColor]);
     if(rs[0]!==t['--sel-bg']||rs[1]!==t['--sel-fg']||rs[2]!==t['--chip']||rs[3]!==t['--on-accent']||!/16%/.test(rs[4])) bad.push(`${id}: rendered selected fills ${rs.join(' | ')}`);
     await pg.evaluate(()=>dropSheet()); }
   await pg.emulateMedia({colorScheme:'light'});
   ok(bad.length===0,'L3-dark: night paper layers (panel/bg ≥1.25, line & rule/panel ≥3, frames quieter than text, offset slab ≥3 vs night and hero, ≤ rule), hero ≥1.15 with text ≥4.5, screentone 1.3–1.6, selected seg/cat ≥3 vs unselected (WCAG 1.4.11) with text ≥4.5, field/sheet ≥1.15, sheet/scrim ≥1.3, focus ring ≥3, theme-color = night paper, 13 palettes '+JSON.stringify(bad.slice(0,3)));
   ok(drift.length===0,'L3-dark: OS dark and the Dark setting give identical tokens and .pal tiles in 13 palettes (one dark source) '+JSON.stringify(drift.slice(0,3)));
   // light keeps the old scrim and sheet, offsets stay ink
   const lt=await pg.evaluate(()=>{ S.settings.palette='classic'; S.settings.mode='light'; applyTheme(); ui.volId=S.volumes[0].id; go('volume'); settleSheet(null); const r=[getComputedStyle($('.scrim')).backgroundColor,getComputedStyle($('.sheet')).backgroundColor,getComputedStyle(document.body).backgroundColor,getComputedStyle($('.hero')).boxShadow+' / '+getComputedStyle($('.hero'),'::after').right,document.querySelector('meta[name=theme-color]').content]; dropSheet(); return r; });
   ok(lt[0]==='rgba(10, 10, 14, 0.5)' && lt[1]===lt[2] && /^(none|((color\([^)]*\)|rgba?\([^)]*\)) 0px \d+px 0px \d+px inset(, )?)+) \/ -10px$/.test(lt[3]) && lt[4]==='#FFFFFF','L3-dark: light scrim, sheet, hero 8px slab (a pseudo-element; its only box-shadows are 0-blur inset frost lines) and theme-color '+JSON.stringify(lt));
   // toasts in dark wrap at 320/360/390 (merge summary and SAVE_GONE), offset stays the accent
   const tb=[];
   for (const w of [320,360,390]) { await pg.setViewportSize({width:w,height:780});
     for (const msg of ['Adds 3 expenses, 1 payment and 2 members. Updates 1 expense and the volume details. Removes 1 expense.','__SAVE_GONE__']) {
       const m=await pg.evaluate(msg=>{ if(msg==='__SAVE_GONE__') msg=SAVE_GONE; S.settings.mode='dark'; applyTheme(); go('hub'); toast(msg); const n=$('.toast'); const r=n.getBoundingClientRect(); return {sw:n.scrollWidth,cw:n.clientWidth,sh:n.scrollHeight,ch:n.clientHeight,l:r.left,r:r.right,vw:innerWidth,sh2:getComputedStyle(n).boxShadow,acc:(()=>{const i=document.createElement('i'); i.style.color='var(--accent)'; document.body.appendChild(i); const c=getComputedStyle(i).color; i.remove(); return c;})()}; },msg);
       if(m.sw>m.cw||m.sh>m.ch||m.l<0||m.r>m.vw||!m.sh2.startsWith(m.acc)) tb.push(`${w}: ${JSON.stringify(m)}`); } }
   ok(tb.length===0,'L3-dark: dark toasts (merge summary, SAVE_GONE) wrap inside the screen at 320/360/390 with the accent offset '+JSON.stringify(tb.slice(0,2)));
   // L3-4: hub card text never cut off (title, balance, meta), 320/360/390, both themes, example and a 40-char title with a huge amount
   const vb=[];
   for (const mode of ['light','dark']) for (const w of [320,360,390]) { await pg.setViewportSize({width:w,height:780});
     const r=await pg.evaluate(mode=>{ S.settings.mode=mode; applyTheme(); S.volumes=S.volumes.filter(v=>v.id!=='l34'); const v=exampleVolume(); v.id='l34'; v.title='Bartholomew and friends: summer road trip'.slice(0,40); v.currency='RM'; v.expenses[0].amount=99999999999; S.volumes.push(v); go('hub');
       return [...document.querySelectorAll('.vol-info > *')].filter(x=>x.scrollWidth>x.clientWidth||x.scrollHeight>x.clientHeight).map(x=>x.className+' '+x.scrollWidth+'/'+x.clientWidth); },mode);
     if(r.length) vb.push(`${mode}/${w}: ${r}`); }
   await pg.evaluate(()=>{ S.volumes=S.volumes.filter(v=>v.id!=='l34'); S.settings.mode='light'; applyTheme(); go('hub'); });
   ok(vb.length===0,'L3-4: hub card title, balance and meta are never cut off at 320/360/390 in both themes '+JSON.stringify(vb.slice(0,3)));
   // R2-2 (design): one near-neutral night in every plate; the plate shows in dark through accent and pastel only
   const sp=[];
   const chroma=([r,g,b])=>{ const f=c=>{c/=255;return c<=.04045?c/12.92:((c+.055)/1.055)**2.4}; const [R,G,B]=[f(r),f(g),f(b)];
     const l=Math.cbrt(.4122214708*R+.5363325363*G+.0514459929*B), m=Math.cbrt(.2119034982*R+.6806995451*G+.1073969566*B), q=Math.cbrt(.0883024619*R+.2817188376*G+.6299787005*B);
     const a=1.9779984951*l-2.4285922050*m+.4505937099*q, bb=.0259040371*l+.7827717662*m-.8086757660*q; return Math.hypot(a,bb); };
   for (const id of ids) { const [bg,lb]=await pg.evaluate(id=>{ S.settings.palette=id; S.settings.mode='dark'; applyTheme(); const d=getComputedStyle(document.body).backgroundColor; S.settings.mode='light'; applyTheme();
       const pr=document.createElement('i'); document.body.appendChild(pr); const g=k=>{ pr.style.background='var('+k+')'; return getComputedStyle(pr).backgroundColor; };
       const mix=document.createElement('i'); document.body.appendChild(mix); mix.style.background='color-mix(in srgb, var(--ink-l) 22%, var(--paper-l))';
       const o=[g('--bg')===g('--paper-l'), g('--panel')===g('--paper-l'), g('--hero')===g('--hero-l'), g('--chip')===g('--food-l'), g('--line')===getComputedStyle(mix).backgroundColor]; pr.remove(); mix.remove(); return [d,o]; },id);
     const c=C(bg).map(Math.round), s2=Math.max(...c)-Math.min(...c), ch=chroma(c); sp.push(`${id} ${s2}/${ch.toFixed(3)}`);
     if(s2>14||ch>0.03) bad.push(`${id}: night ${c} spread ${s2} chroma ${ch.toFixed(3)}`); if(lb.includes(false)) bad.push(`${id}: light tokens changed ${lb}`); }
   ok(bad.length===0,'R2-2: dark --bg RGB spread ≤14 and OKLCH chroma ≤0.03 in 13 palettes, light --bg/--panel/--line/--hero/--chip still the plate inputs ['+sp.join(', ')+'] '+JSON.stringify(bad.slice(-3)));
   // R2-5 (design): in light the selected category carries an inner ink edge the others lack (not hue alone); dark keeps the pastel inversion
   const ce=[];
   for (const id of ids) for (const mode of ['light','dark']) {
     const r=await pg.evaluate(([id,mode])=>{ S.settings.palette=id; S.settings.mode=mode; applyTheme(); ui.volId=S.volumes[0].id; go('volume'); openExpense(null);
       const fg=getComputedStyle(document.body).color, s=getComputedStyle($('.cat[aria-pressed="true"]')).boxShadow, u=getComputedStyle($('.cat[aria-pressed="false"]')).boxShadow; dropSheet(); return {fg,s,u}; },[id,mode]);
     const ring=r.s.startsWith(r.fg+' 0px 0px 0px 2px inset'), uHas=/0px 0px 0px 2px inset/.test(r.u);
     if(mode==='light' ? (!ring||uHas) : ring) ce.push(`${id}/${mode}: ${JSON.stringify(r)}`); }
   ok(ce.length===0,'R2-5: light selected .cat has a 2px inner --fg edge and unselected has none (13 palettes); dark selected has no chalk edge '+JSON.stringify(ce.slice(0,2)));
   // R2-5 revise (a11y): the inner ink edge survives keyboard focus (the :is() focus rule must not out-specify it)
   const cf=[];
   for (const id of ids) { await pg.evaluate(id=>{ S.settings.palette=id; S.settings.mode='light'; applyTheme(); ui.volId=S.volumes[0].id; go('volume'); openExpense(null); },id);
     await pg.evaluate(()=>{ const s=$('.cat[aria-pressed="true"]'); (s.previousElementSibling||s.nextElementSibling).focus(); });
     await pg.keyboard.press(await pg.evaluate(()=>$('.cat[aria-pressed="true"]').previousElementSibling?'Tab':'Shift+Tab')); await pg.waitForTimeout(400); // let the box-shadow transition settle
     const r=await pg.evaluate(()=>{ const s=$('.cat[aria-pressed="true"]'); const o={fv:s.matches(':focus-visible'),fg:getComputedStyle(document.body).color,s:getComputedStyle(s).boxShadow}; dropSheet(); return o; });
     if(!r.fv || !r.s.startsWith(r.fg+' 0px 0px 0px 2px inset') || !/0px 0px 0px 6px/.test(r.s)) cf.push(`${id}: ${JSON.stringify(r)}`); }
   ok(cf.length===0,'R2-5: light selected .cat keeps its 2px inner --fg edge plus the focus ring under keyboard :focus-visible (13 palettes) '+JSON.stringify(cf.slice(0,2)));
   // R2-3 (design): history rows wrap instead of cutting off; title ≤2 lines, icon/title/amount share line 1, no dangling "·"
   const hb=[];
   for (const mode of ['light','dark']) for (const w of [360,390]) for (const long of [false,true]) { await pg.setViewportSize({width:w,height:780});
     const r=await pg.evaluate(([mode,long])=>{ S.settings.palette='classic'; S.settings.mode=mode; applyTheme(); S.volumes=S.volumes.filter(v=>v.id!=='r23'); const v=exampleVolume(); v.id='r23';
       if(long){ v.members[1].name='Maximilian Featherstonehaugh-Wolfe'; v.expenses.forEach(e=>e.paidBy=v.members[1].id); v.expenses[0].desc='Very long description of a dinner at a place with an extremely long name that keeps going'; }
       S.volumes.push(v); ui.volId='r23'; go('history'); const out=[];
       for (const t of document.querySelectorAll('.tx')) { const ti=t.querySelector('.tx-t'), a=t.querySelector('.tx-a'), ic=t.querySelector('.tx-ic'), m=t.querySelector('.tx-m'); const lh=parseFloat(getComputedStyle(ti).lineHeight);
         for (const x of t.querySelectorAll('*')) if (x.scrollWidth>x.clientWidth+1 && getComputedStyle(x).overflow!=='visible') out.push('overflow '+x.className);
         if (ti.clientHeight>2*lh+1) out.push('title >2 lines '+ti.clientHeight);
         if (Math.abs(ti.getBoundingClientRect().top-a.getBoundingClientRect().top)>1 || Math.abs(ic.getBoundingClientRect().top-ti.getBoundingClientRect().top)>1) out.push('line 1 misaligned '+ti.textContent.slice(0,20));
         if (/^\s*·|·\s*$/.test(m.innerText)) out.push('dangling dot '+m.innerText);
         if (t.getBoundingClientRect().right>innerWidth) out.push('row wider than screen');
         const d=t.querySelector('.date'); if (d && Math.abs(d.getBoundingClientRect().bottom-m.getBoundingClientRect().bottom)>1) out.push(`date off the meta line ${(m.getBoundingClientRect().bottom-d.getBoundingClientRect().bottom).toFixed(1)}px `+ti.textContent.slice(0,20)); }
       S.volumes=S.volumes.filter(v=>v.id!=='r23'); return out; },[mode,long]);
     if(r.length) hb.push(`${mode}/${w}/${long?'long':'example'}: ${r.slice(0,3)}`); }
   await pg.evaluate(()=>{ S.settings.mode='light'; applyTheme(); go('hub'); });
   ok(hb.length===0,'R2-3: history rows wrap (title ≤2 lines, nothing clipped sideways, icon/title/amount on line 1, date on the meta line when title or meta wraps, no dangling ·) at 360/390, both themes, long names '+JSON.stringify(hb.slice(0,3)));
   ok(e.length===0,'no page errors in L3-dark checks '+JSON.stringify(e));
   await c.close(); }
 // R1-ioshint: short hint with OK inline (iPhone Safari at 360)
 { const c=await b.newContext({...devices['iPhone 13'],viewport:{width:360,height:780}}); const pg=await c.newPage(); await pg.goto(URL);
   const m=await pg.evaluate(()=>{ const h=$('#iosHint'), ok=h&&h.querySelector('[data-act=dismissIosHint]'); return h&&{h:h.getBoundingClientRect().height,okH:ok.getBoundingClientRect().height,okW:ok.getBoundingClientRect().width,vol:$('.vol').getBoundingClientRect().top+scrollY}; });
   ok(m && m.h<=120 && m.okH>=44 && m.okW>=44 && m.vol<=290,'iOS hint at 360: ≤120px tall, OK is a 44px target, first volume starts by 290px '+JSON.stringify(m));
   await c.close(); }
 // R1-merge-copy / R1-p1 (code): merge summary names its nouns; a failed save with nothing stored does not claim "Nothing was changed"
 { const fresh=async()=>{ const c=await b.newContext({viewport:{width:360,height:780}}); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{if(m.type()==='error')e.push(m.text())}); await pg.goto(URL); return {c,pg,e}; };
   const importCode=async (pg,code)=>{ await pg.evaluate(()=>go('hub')); await pg.getByRole('button',{name:'Import'}).click(); await pg.fill('#s-paste',code); await pg.locator('#s-go').click(); await pg.waitForTimeout(600); };
   const toastText=pg=>pg.evaluate(()=>{const t=document.querySelector('.toast'); return t?t.textContent:'';});
   const all=[];
   { const M=await fresh(); all.push(M);
     const code=await M.pg.evaluate(()=>{ const v=JSON.parse(JSON.stringify(S.volumes[0])); v.expenses.push({id:'mx1',desc:'MergeOne',amount:300,paidBy:v.meId,splits:{[v.meId]:300},inputs:{},splitType:'exact',category:'food',date:today(),upd:Date.now()}); return JSON.stringify({app:'hati2-mock',v:1,volume:v}); });
     await importCode(M.pg,code); const sum=await M.pg.locator('#s-sum').innerText();
     ok(/ Adds 1 expense\.$/.test(sum),'merge summary: one new expense reads "Adds 1 expense." ('+sum+')');
     await M.pg.locator('#s-go').click(); await M.pg.waitForTimeout(100);
     const st=await M.pg.evaluate(()=>$('#status').textContent); ok(/^Merged: adds 1 expense\.$/.test(st),'merge toast: "Merged: adds 1 expense." ('+st+')');
     const r=await M.pg.evaluate(()=>{ const v=S.volumes[0], inc=JSON.parse(JSON.stringify(v)), t=Date.now()+50;
       const member=JSON.parse(JSON.stringify(v)); member.members.push({id:'mm1',name:'Newbie',upd:t});
       const two=JSON.parse(JSON.stringify(v)); two.expenses.forEach((e,i)=>{ if(i<2){ e.desc+=' edited'; e.upd=t; } }); two.title='Renamed'; two.upd=t;
       const mix=JSON.parse(JSON.stringify(member)); mix.expenses.push({id:'mx2',desc:'Mixed',amount:200,paidBy:v.meId,splits:{[v.meId]:200},inputs:{},splitType:'exact',category:'food',date:today(),upd:t}); mix.settlements.push({id:'ms2',from:v.members[1].id,to:v.meId,amount:100,date:today(),upd:t});
       return [member,two,mix,inc].map(x=>mergeSummary(mergeVolumes(v,x))); });
     ok(r[0]==='Adds 1 member.','a member-only merge reads "Adds 1 member." ('+r[0]+')');
     ok(r[1]==='Updates 2 expenses and the volume details.','plural and details: "Updates 2 expenses and the volume details." ('+r[1]+')');
     ok(r[2]==='Adds 1 expense, 1 payment and 1 member.','three kinds: "Adds 1 expense, 1 payment and 1 member." ('+r[2]+')');
     ok(r[3]==='','an identical copy has an empty summary ("Already up to date")'); }
   { const P=await fresh(); all.push(P);
     // storage emptied elsewhere (another tab, eviction), then every write throws
     await P.pg.evaluate(()=>{ localStorage.removeItem(KEY); window.__set=Storage.prototype.setItem; Storage.prototype.setItem=function(){ throw new DOMException('full','QuotaExceededError'); }; ui.volId=S.volumes[0].id; go('volume'); });
     const n0=await P.pg.evaluate(()=>vol().expenses.length);
     await P.pg.getByRole('button',{name:/Add expense/}).click(); await P.pg.fill('#f-amount','7'); await P.pg.fill('#f-desc','Gone?'); await P.pg.locator('#expForm button[type=submit]').click();
     const t1=await toastText(P.pg);
     ok(!/Nothing was changed/.test(t1) && /Couldn't save/.test(t1) && /not made/.test(t1) && /backup/.test(t1),'nothing stored + write fails: the toast says the change was not made and asks for a backup, not "Nothing was changed" ('+t1+')');
     await P.pg.locator('#expForm button[type=submit]').click(); const t2=await toastText(P.pg);
     ok(!/Nothing was changed/.test(t2) && /not made/.test(t2) && await P.pg.evaluate(n=>vol().expenses.length===n && !vol().expenses.some(e=>e.desc==='Gone?'),n0),'a second try says the same and never adds the expense twice ('+t2+')');
     await P.pg.evaluate(()=>{ Storage.prototype.setItem=window.__set; }); await P.pg.locator('#expForm button[type=submit]').click();
     ok(/Expense saved/.test(await toastText(P.pg)) && await P.pg.evaluate(n=>vol().expenses.filter(e=>e.desc==='Gone?').length===1 && !!localStorage.getItem(KEY),n0),'once storage works, the same Save stores the expense once'); }
   const errs=all.flatMap(x=>x.e); ok(errs.length===0,'no console errors in merge-copy / save-gone checks '+JSON.stringify(errs));
   for (const x of all) await x.c.close(); }
 // R1-revise (design): long toasts wrap instead of cutting off; member chips 2 per row at 360; selected category stays in line
 for (const w of [320,360]) { const c=await b.newContext({viewport:{width:w,height:780}}); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message));
   await pg.goto(URL);
   for (const msg of ['SAVE_GONE','"Merged: adds 1 expense, 1 payment and 1 member. Updates the volume details."']) {
     const r=await pg.evaluate(m=>{ toast(eval(m)); const t=document.querySelector('.toast'), bx=t.getBoundingClientRect(); return {sw:t.scrollWidth,cw:t.clientWidth,sh:t.scrollHeight,ch:t.clientHeight,l:bx.left,r:bx.right,vw:innerWidth}; },msg);
     ok(r.sw<=r.cw && r.sh<=r.ch && r.l>=16-0.5 && r.r<=r.vw-16+0.5,`toast fits on screen at ${w}: ${msg.slice(0,30)} (scroll ${r.sw}/${r.cw}, box ${Math.round(r.l)}-${Math.round(r.r)})`); }
   if (w===360) {
     await pg.locator('.vol').first().click();
     const rows=await pg.evaluate(()=>new Set([...document.querySelectorAll('.chips > button.chip:not(.add)')].map(x=>Math.round(x.getBoundingClientRect().top))).size);
     const n=await pg.locator('.chips > button.chip:not(.add)').count();
     ok(rows===Math.ceil(n/2),`member chips sit two to a row at 360 (${n} chips, ${rows} rows)`);
     await pg.getByRole('button',{name:/Add expense/}).click();
     const cat=await pg.evaluate(()=>{ const sel=document.querySelector('.cat[aria-pressed="true"]'), all=[...document.querySelectorAll('.cat')]; const t=sel.getBoundingClientRect().top; const row=all.filter(x=>x!==sel && Math.abs(x.getBoundingClientRect().top-t)<6); return {tr:getComputedStyle(sel).transform,d:row.map(x=>Math.abs(x.getBoundingClientRect().top-t))}; });
     ok(cat.tr==='none' && cat.d.length>0 && cat.d.every(d=>d<0.5),`selected category is not shifted against its row (${cat.tr}; ${JSON.stringify(cat.d)})`); }
   ok(e.length===0,`no page errors in R1-revise checks at ${w} `+JSON.stringify(e)); await c.close(); }
 // R1-revise (code): iOS Safari shows :active (clay press) on a tap only with a touchstart listener; the shipped page must register one, passive
 { const html=require('fs').readFileSync(require('path').join(process.cwd(),'index.html'),'utf8');
   ok(/document\.addEventListener\("touchstart",[^;]*\{ passive: true \}\)/.test(html),'shipped page registers a passive touchstart listener so iOS shows the clay pressed state');
   const c=await b.newContext({viewport:{width:360,height:780},hasTouch:true}); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); await pg.goto(URL);
   await pg.locator('.vol').first().tap(); const v=await pg.evaluate(()=>ui.view);
   ok(v==='volume' && e.length===0,'a touch tap still opens a volume with the touch listener in place, no page errors ('+v+' '+JSON.stringify(e)+')'); await c.close(); }
 // R1 ship fix: pairwise counts every payment (a refund from someone with no expense debt too) and always agrees with balances()
 { const c=await b.newContext({viewport:{width:360,height:780}}); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); await pg.goto(URL);
   const r=await pg.evaluate(()=>{ S.settings.strategy='pairwise';
     const v={id:'volPW',title:'Refund',type:'household',currency:'$',meId:'a',members:[{id:'a',name:'Ana'},{id:'b',name:'Ben'}],
       expenses:[{id:'e1',desc:'Lunch',amount:3000,paidBy:'b',category:'food',date:'2026-11-01',splitType:'exact',splits:{a:3000},inputs:{}}],
       settlements:[{id:'s1',from:'a',to:'b',amount:5000,date:'2026-11-02'},{id:'s2',from:'b',to:'a',amount:2000,date:'2026-11-03'}]};
     S.volumes.push(v); ui.volId='volPW'; go('volume'); const txt=document.querySelector('main, body').innerText; S.volumes.pop(); return {pw:pairwise(v),bal:balances(v),settled:/all settled/i.test(txt),pays:/pays you/i.test(txt)}; });
   ok(r.pw.length===0 && r.settled && !r.pays,'pairwise: Ben paid $30 for Ana, Ana paid $50, Ben refunded $20 → settled, no leftover suggestion '+JSON.stringify(r));
   const bad=await pg.evaluate(()=>{ let seed=7; const rnd=n=>{ seed=(seed*1103515245+12345)%2147483648; return (seed>>>16)%n; }; let fails=0;
     for (let k=0;k<200;k++) { const ids=['a','b','c','d'].slice(0,2+rnd(3)); const v={members:ids.map(id=>({id,name:id})),expenses:[],settlements:[]};
       for (let i=rnd(5);i>0;i--) { const paidBy=ids[rnd(ids.length)], splits={}; for (const id of ids) if (rnd(2)) splits[id]=1+rnd(5000); v.expenses.push({paidBy,splits,amount:Object.values(splits).reduce((x,y)=>x+y,0)}); }
       for (let i=rnd(4);i>0;i--) { const f=ids[rnd(ids.length)], t=ids.filter(x=>x!==f)[rnd(ids.length-1)]; v.settlements.push({from:f,to:t,amount:1+rnd(6000)}); }
       const net=Object.fromEntries(ids.map(id=>[id,0])); for (const d of pairwise(v)) { net[d.from]+=d.amount; net[d.to]-=d.amount; }
       const bal=balances(v); if (ids.some(id=>Math.abs(net[id]+(bal[id]||0))>2*ids.length)) fails++; }
     return fails; });
   ok(bad===0,'pairwise net per member equals balances() in 200 random volumes ('+bad+' mismatches)');
   ok(e.length===0,'no page errors in pairwise checks '+JSON.stringify(e)); await c.close(); }
 // R2-1 (code): invite link <base>#join=<base64url(deflate-raw(share code))> opens a join sheet with a who-are-you picker
 { const fresh=async(dev=devices['Pixel 7'],{init,url=URL}={})=>{ const c=await b.newContext({...dev}); if(init) await c.addInitScript(init); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{if(m.type()==='error')e.push(m.text())}); await pg.goto(url); return {c,pg,e}; };
   const all=[], msgOf=pg=>pg.locator('#sheetMsg').innerText();
   const A=await fresh(); all.push(A);
   const exId=await A.pg.evaluate(()=>S.volumes[0].id), link=await A.pg.evaluate(()=>inviteLink(S.volumes[0])), pay=link.split('#join=')[1];
   ok(link===URL+'#join='+pay && /^[A-Za-z0-9_-]+$/.test(pay) && link.length<1000,`invite link is <page>#join=<base64url>, ${link.length} chars for the example volume`);
   // cold start on a phone that doesn't have the volume
   const B=await fresh(devices['Pixel 7'],{url:link}); all.push(B); await B.pg.waitForSelector('#s-who');
   const j=await B.pg.evaluate(()=>({h:$('#sheetTitle').textContent,sum:$('#sheetForm .share-sum').innerText,opts:$('#s-who').opts.map(o=>o[1]),sel:$('#s-who').value,isNew:!!$('#s-new'),hash:location.hash,n:S.volumes.length}));
   ok(j.h==='Join volume' && /Tokyo Trip \(example\)/.test(j.sum) && /3 members/.test(j.sum) && ['Alex','Mira','Rio'].every(n=>j.opts.includes(n)) && j.sel==='' && j.isNew,'cold link: "Join volume" shows Tokyo Trip (example), 3 members, "New volume" and a picker that starts on no one '+JSON.stringify(j));
   ok(!/join/.test(j.hash) && j.n===1,'the invite leaves the address bar as soon as it is read, nothing saved yet '+JSON.stringify([j.hash,j.n]));
   await B.pg.getByRole('button',{name:'Join'}).click(); ok(/Choose which member/.test(await msgOf(B.pg)),'Join without picking a name asks who you are');
   await pick(B.pg,'s-who',{label:'Mira'}); await B.pg.getByRole('button',{name:'Join'}).click();
   const r=await B.pg.evaluate(id=>{ const v=vol(); return {view:ui.view,ok:ui.volId===id,me:nameOf(v,v.meId),hash:location.hash,zero:Object.values(balances(v)).reduce((a,b)=>a+b,0),you:/You/.test($('#app').innerText)}; },exId);
   ok(r.view==='volume' && r.ok && r.me==='Mira' && !/join/.test(r.hash) && r.zero===0,'Join as Mira opens the volume from Mira\'s side; no #join in the URL; zero-sum '+JSON.stringify(r));
   await B.pg.goBack(); await B.pg.waitForTimeout(200);
   ok(!(await B.pg.evaluate(()=>sheetOpen())) && await B.pg.evaluate(()=>ui.view)==='hub','Back after Join goes to the volumes, the join sheet does not come back');
   // the same link on a phone that has the volume (running page: a fragment change): merge, no picker, keeps you
   await B.pg.goto(link); await B.pg.waitForSelector('#s-mode');
   ok(await pickVal(B.pg,'s-mode')==='merge' && !(await B.pg.locator('#s-who').isVisible()) && !(await B.pg.locator('#s-new').count()) && /You already have Tokyo/.test(await B.pg.locator('#s-sum').innerText()),'link for a volume already here: merge sheet, no picker, says you already have it');
   await B.pg.locator('#sheetForm button.primary').click();
   ok(await B.pg.evaluate(id=>S.volumes.filter(x=>x.id===id).length===1 && nameOf(vol(),vol().meId)==='Mira',exId),'merging the same link adds no copy and keeps Mira as you');
   // a running page whose hash is set to the link (APK onNewIntent, address bar)
   const C=await fresh(devices['Pixel 7']); all.push(C);
   await C.pg.evaluate(()=>{ ui.volId=S.volumes[0].id; go('volume'); });
   await C.pg.evaluate(p=>{ location.hash='join='+p; },pay); await C.pg.waitForSelector('#s-who');
   ok(await C.pg.evaluate(()=>/Tokyo Trip/.test($('#sheetForm').innerText) && !/join/.test(location.hash) && ui.view==='volume'),'setting location.hash to the link in an open page opens the join sheet over the current page');
   await C.pg.keyboard.press('Escape');
   // broken and newer links change nothing
   const S0=await C.pg.evaluate(()=>JSON.stringify(S)), mid=Math.floor(pay.length/2);
   for (const [bad,want,why] of [[pay.slice(0,mid)+pay.slice(mid+1),'This invite link is broken. Ask for a new one.','one middle character removed'],[pay.slice(0,40),'This invite link is broken. Ask for a new one.','cut short'],['abc$%','This invite link is broken. Ask for a new one.','not base64url'],
       ['A'.repeat(22)+'.'+'B'.repeat(43),"This volume isn't online yet. Ask whoever sent the link to open HATI² while online, then try again.",'a live link to a room nobody wrote to'],
       ['A'.repeat(22)+'.'+'B'.repeat(44),'This invite needs a newer version of HATI².','a room.key link with a key of the wrong length']]) {
     await C.pg.evaluate(p=>{ location.hash='join='+p; },bad); await C.pg.waitForFunction(()=>$('#sheetMsg') && $('#sheetMsg').textContent.length>0);
     ok(await msgOf(C.pg)===want && await C.pg.evaluate(s=>JSON.stringify(S)===s,S0),`${why}: "${want}" and nothing changes`);
     await C.pg.keyboard.press('Escape'); }
   const D=await fresh(devices['Pixel 7'],{url:URL+'#join='+pay.slice(0,mid)+pay.slice(mid+1)}); all.push(D); await D.pg.waitForFunction(()=>$('#sheetMsg') && $('#sheetMsg').textContent.length>0);
   ok(await msgOf(D.pg)==='This invite link is broken. Ask for a new one.' && await D.pg.evaluate(()=>S.volumes.length===1 && S.volumes[0].title==='Tokyo Trip (example)'),'cold start with a broken link: the message, and only the phone\'s own volume');
   // decompression is capped while streaming: a tiny link that inflates to 8 MB is stopped early
   const bomb=await C.pg.evaluate(async()=>{ const big='{"app":"hati2-mock","v":1,"volume":'+' '.repeat(8*1024*1024)+'}';
     const u=new Uint8Array(await new Response(new Blob([big]).stream().pipeThrough(new CompressionStream('deflate-raw'))).arrayBuffer());
     const W=WritableStreamDefaultWriter.prototype, R=ReadableStreamDefaultReader.prototype, w0=W.write, r0=R.read; let fed=0, got=0;
     W.write=function(c){ fed+=c.length; return w0.call(this,c); }; R.read=async function(){ const x=await r0.call(this); if(x.value) got+=x.value.length; return x; };
     try { const out=await inflateCapped(u,JOIN_OUT_MAX); return {inLen:u.length,fed,got,out,err:(await readInvite(b64url(u))).error}; } finally { W.write=w0; R.read=r0; } });
   ok(bomb.out===null && bomb.err==='This invite link is broken. Ask for a new one.' && bomb.fed<bomb.inLen && bomb.got<=512*1024+300*1024,'a link that inflates to 8 MB is cut off mid-stream ('+JSON.stringify({...bomb,out:undefined})+')');
   // a pasted link in Import gives the same summary as the pasted code (here: a phone that has the volume)
   const code=await A.pg.evaluate(()=>shareCodeOf(S.volumes[0])), sums=[];
   for (const t of [code,'Join us: '+link+'.']) { await A.pg.evaluate(()=>{ dropSheet(); go('hub'); }); await A.pg.getByRole('button',{name:'Import'}).click(); await A.pg.fill('#s-paste',t); await A.pg.locator('#s-go').click(); await A.pg.waitForSelector('#s-sum'); sums.push(await A.pg.locator('#s-pick').innerText()); }
   ok(sums[0]===sums[1] && /already has everything/.test(sums[1]),'Import: a pasted link (with chat text and a full stop) gives the same summary as the code '+JSON.stringify(sums[1]));
   await A.pg.evaluate(()=>{ dropSheet(); go('hub'); }); await A.pg.getByRole('button',{name:'Import'}).click(); await A.pg.fill('#s-paste',URL+'#join='+pay.slice(0,mid)+pay.slice(mid+1)); await A.pg.locator('#s-go').click();
   await A.pg.waitForFunction(()=>$('#sheetMsg').textContent.length>0); ok(await msgOf(A.pg)==='This invite link is broken. Ask for a new one.','Import: a broken pasted link says so');
   await A.pg.keyboard.press('Escape');
   const noted=await A.pg.evaluate(()=>{ const v=JSON.parse(JSON.stringify(S.volumes[0])); v.expenses[0].note='see #join=abc'; return shareCodeOf(v); });
   await A.pg.evaluate(()=>{ dropSheet(); go('hub'); }); await A.pg.getByRole('button',{name:'Import'}).click(); await A.pg.fill('#s-paste',noted); await A.pg.locator('#s-go').click();
   await A.pg.waitForSelector('#s-sum'); ok(/Updates 1 expense/.test(await A.pg.locator('#s-sum').innerText()),'a share code whose note mentions "#join=" is still read as a code');
   await A.pg.keyboard.press('Escape');
   // without CompressionStream (old WebView): Copy code, no error; a link says this browser can't open it
   const O=await fresh(devices['Pixel 7'],{init:()=>{ delete window.CompressionStream; delete window.DecompressionStream; }}); all.push(O); await setNet(O.c,false);
   await O.pg.evaluate(()=>{ ui.volId=S.volumes[0].id; go('volume'); return shareSheet(); });
   ok(await O.pg.getByRole('button',{name:'Copy code'}).isVisible() && !(await O.pg.locator('[data-act=copyLink],[data-act=sendLink],svg.qr').count()),'no CompressionStream: the share sheet falls back to Copy code');
   await O.pg.evaluate(p=>{ dropSheet(); location.hash='join='+p; },pay); await O.pg.waitForFunction(()=>$('#sheetMsg') && $('#sheetMsg').textContent.length>0);
   ok(/can't open invite links/.test(await msgOf(O.pg)),'no DecompressionStream: a link says this browser can\'t open it');
   await setNet(O.c,true);
   // iPhone Safari (not the Home Screen app): one line and Copy link, for the app's own storage
   const I1=await fresh(devices['iPhone 13'],{url:link}); all.push(I1); await I1.pg.waitForSelector('#s-who');
   await I1.pg.evaluate(()=>{ window.__c=null; Object.defineProperty(navigator,'clipboard',{value:{writeText:t=>{window.__c=t; return Promise.resolve();}},configurable:true}); });
   ok(/home screen\? Copy this link and paste it in Import/.test(await I1.pg.locator('#s-safari').innerText()),'iPhone Safari: the join sheet says to copy the link into the Home Screen app');
   await I1.pg.locator('#sheetForm [data-act=copyLink]').click(); await I1.pg.waitForTimeout(100);
   ok(await I1.pg.evaluate(l=>window.__c===l,link),'iPhone Safari: Copy link copies the whole invite link');
   const I2=await fresh(devices['iPhone 13'],{url:link,init:()=>{ Object.defineProperty(navigator,'standalone',{get:()=>true}); }}); all.push(I2); await I2.pg.waitForSelector('#s-who');
   ok(!(await I2.pg.locator('#s-safari').count()),'Home Screen app: no copy-to-app line');
   const errs=all.flatMap(x=>x.e); ok(errs.length===0,'no page errors in R2-1 invite checks '+JSON.stringify(errs));
   for (const x of all) await x.c.close(); }
 // R2-2 (code): QR of the same link, checked against segno; ink on white in every theme; too big → a line instead
 { const c=await b.newContext({viewport:{width:360,height:780}}); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{if(m.type()==='error')e.push(m.text())}); await pg.goto(URL);
   const fx=JSON.parse(require('fs').readFileSync(require('path').join(__dirname,'..','..','web','tests','qr-fixtures.json'),'utf8'));
   const res=await pg.evaluate(cs=>cs.map(c=>{ const m=qrMatrix(c.text,c.forced?{version:c.version,mask:c.mask}:{}); return !!m && m.length===c.matrix.length && m.every((row,y)=>row.map(x=>x?'1':'0').join('')===c.matrix[y]); }),fx.cases);
   const forced=fx.cases.filter(c=>c.forced).length, auto=fx.cases.length-forced;
   ok(res.every(Boolean) && forced>=3 && auto>=1,`QR matrix equals segno module for module: ${forced} fixed (version, mask) cases and ${auto} where the app picks version and mask itself ${JSON.stringify(res)}`);
   ok(await pg.evaluate(()=>qrMatrix('x'.repeat(858)).length===97 && qrMatrix('x'.repeat(859))===null),'QR stops at version 20: 858 bytes fit, 859 do not');
   for (const w of [320,360,390]) { await pg.setViewportSize({width:w,height:780}); await pg.evaluate(()=>{ dropSheet(); ui.volId=S.volumes[0].id; go('volume'); return shareSheet(); });
     // measured at rest: the sheet slides up for 200 ms (motion pass), and mid-slide the QR is still below the fold
     await pg.evaluate(()=>Promise.all($('#sheetForm').getAnimations({subtree:true}).map(a=>a.finished)));
     const q=await pg.evaluate(()=>{ const s=$('svg.qr'); if(!s) return null; s.scrollIntoView(); const r=s.getBoundingClientRect(), n=+s.getAttribute('viewBox').split(' ')[2]; return {w:r.width,n,px:r.width/n,vis:r.width>0&&r.top>=0&&r.bottom<=innerHeight,label:s.getAttribute('aria-label'),sw:document.scrollingElement.scrollWidth,sheetSw:$('#sheetForm').scrollWidth-$('#sheetForm').clientWidth,iw:innerWidth}; });
     ok(q && q.vis && q.px>=2 && q.w<=280 && q.sw<=q.iw && q.sheetSw<=0 && q.label==='QR code of the invite link',`QR visible at ${w}: ${q&&q.w.toFixed(0)}px, ${q&&q.n} modules with quiet zone, ${q&&q.px.toFixed(2)} CSS px per module, no sideways scroll`); }
   const ink=await pg.evaluate(()=>{ const bad=[]; for (const mode of ['light','dark']) for (const p of PALETTES) { S.settings.mode=mode; S.settings.palette=p.id; applyTheme();
       const rect=getComputedStyle($('svg.qr rect')).fill, path=getComputedStyle($('svg.qr path')).fill, bg=getComputedStyle($('svg.qr')).backgroundColor;
       if (rect!=='rgb(255, 255, 255)' || path!=='rgb(0, 0, 0)' || bg!=='rgb(255, 255, 255)') bad.push([mode,p.id,rect,path,bg]); }
     S.settings.mode='system'; S.settings.palette='classic'; applyTheme(); return bad; });
   ok(ink.length===0,'QR is #000 on #fff in all 13 palettes, light and dark '+JSON.stringify(ink.slice(0,3)));
   await pg.evaluate(()=>{ dropSheet(); const v=JSON.parse(JSON.stringify(S.volumes[0])); v.id='bigQR1'; v.expenses=[];
     for (let i=0;i<204;i++) v.expenses.push({id:'bq'+i,desc:'Shared thing '+i+' '+Math.random().toString(36).slice(2,8),amount:1000+i*37,paidBy:v.meId,category:'food',date:'2026-10-01',splitType:'exact',splits:{[v.meId]:1000+i*37},inputs:{},note:'',upd:0});
     S.volumes.push(v); ui.volId='bigQR1'; go('volume'); window.__f=window.fetch; window.fetch=()=>Promise.reject(new TypeError('Failed to fetch')); return shareSheet(); });
   ok(await pg.locator('#s-noqr').innerText()==='This volume is too big for a QR code. Send the link instead.' && !(await pg.locator('svg.qr').count()) && await pg.locator('[data-act=copyLink]').isVisible(),'204 expenses, sync server out of reach: the too-big line, no QR, the snapshot link still there');
   await pg.evaluate(()=>{ dropSheet(); window.fetch=window.__f; return shareSheet(); });
   ok(await pg.locator('svg.qr').count()===1 && /#join=[\w-]{22}\.[\w-]{43}$/.test(await pg.locator('#s-link').inputValue()),'204 expenses with the server reachable: the live link is short, so the QR is back');
   ok(e.length===0,'no page errors in R2-2 QR checks '+JSON.stringify(e)); await c.close(); }
 // R2-3 (code + scenarios): scenario: two phones join by invite link and merge back
 { const fresh=async dev=>{ const c=await b.newContext({...dev}); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{if(m.type()==='error')e.push(m.text())}); await pg.goto(URL);
     await pg.evaluate(()=>{ window.__shared=null; navigator.share=d=>{ window.__shared=d; return Promise.resolve(); }; }); return {c,pg,e}; };
   const sendInvite=async pg=>{ await pg.evaluate(()=>{ window.__shared=null; }); await pg.getByRole('button',{name:'Send invite'}).click(); await pg.waitForFunction(()=>window.__shared); return pg.evaluate(()=>window.__shared.url); };
   const addExp=async (pg,desc,amt)=>{ await pg.getByRole('button',{name:/Add expense/}).click(); await pg.fill('#f-amount',amt); await pg.fill('#f-desc',desc); await pg.locator('#expForm button[type=submit]').click(); };
   const A=await fresh(devices['Pixel 7']), B=await fresh(devices['iPhone 13']);
   // A creates "Flat" with Ben and leaves their own name blank
   await A.pg.locator('[data-act=newVolume]').click(); await A.pg.fill('#s-title','Flat'); await A.pg.fill('#s-others','Ben'); await A.pg.locator('#sheetForm button.primary').click();
   await addExp(A.pg,'Rent','100');
   await A.pg.locator('[data-act=share]').first().click(); await A.pg.waitForSelector('#s-name');
   ok(/your name/i.test(await A.pg.locator('#sheetForm').innerText()),'sharing with the default name "Me" asks for your name first');
   await A.pg.fill('#s-name','me'); await A.pg.locator('#sheetForm button.primary').click(); ok(/own name/.test(await A.pg.locator('#sheetMsg').innerText()),'"me" is not accepted as a name');
   await A.pg.fill('#s-name','Ana'); await A.pg.locator('#sheetForm button.primary').click();
   const linkA=await sendInvite(A.pg); await A.pg.keyboard.press('Escape');
   // B opens the link: pick (2 taps) + Join (1 tap)
   let taps=0; await B.pg.goto(linkA); await B.pg.waitForSelector('#s-who');
   const opts=await B.pg.evaluate(()=>$('#s-who').opts.map(o=>o[1]));
   ok(opts.includes('Ana') && !opts.includes('Me'),'B\'s picker names the sender (Ana), not "Me" '+JSON.stringify(opts));
   taps+=await pick(B.pg,'s-who',{label:'Ben'}); await B.pg.getByRole('button',{name:'Join'}).click(); taps+=1;
   ok(taps<=2 && await B.pg.evaluate(()=>ui.view==='volume' && vol().title==='Flat' && nameOf(vol(),vol().meId)==='Ben'),`B joins Flat as Ben in ${taps} taps (opening the link counts 0)`);
   await addExp(B.pg,'Groceries','40');
   await B.pg.locator('[data-act=share]').first().click(); const linkB=await sendInvite(B.pg); await B.pg.keyboard.press('Escape');
   // A opens B's link: merge
   await A.pg.goto(linkB); await A.pg.waitForSelector('#s-mode'); await A.pg.locator('#sheetForm button.primary').click();
   const view=pg=>pg.evaluate(()=>{ const v=vol(), bal=balances(v); return {exp:v.expenses.map(e=>e.desc+':'+e.amount).sort(), me:bal[v.meId], sum:Object.values(bal).reduce((a,b)=>a+b,0), you:nameOf(v,v.meId), id:v.id}; });
   const a=await view(A.pg), bb=await view(B.pg);
   ok(a.id===bb.id && JSON.stringify(a.exp)===JSON.stringify(bb.exp) && a.exp.length===2,'both phones show the same 2 expenses '+JSON.stringify([a.exp,bb.exp]));
   ok(a.me===3000 && bb.me===-3000 && a.me+bb.me===0 && a.sum===0 && bb.sum===0 && a.you==='Ana' && bb.you==='Ben',`balances are opposite to the cent from each phone's own "You" (A ${a.you} ${a.me}, B ${bb.you} ${bb.me}) and sum to 0`);
   // A opens its own (older) link again: nothing new
   await A.pg.goto(linkA); await A.pg.waitForSelector('#s-sum');
   ok(/already has everything/.test(await A.pg.locator('#s-sum').innerText()),'A opening its own link again: "already has everything"');
   await A.pg.locator('#sheetForm button.primary').click(); ok(JSON.stringify((await view(A.pg)).exp)===JSON.stringify(a.exp),'and merging it adds nothing');
   // stale link: B deletes the volume, then opens the old link → it is offered as a new volume, not merged silently
   await B.pg.locator('[data-act=volMenu]').click(); await B.pg.locator('[data-act=deleteVolume]').click(); await B.pg.locator('#sheetForm button.danger').click();
   await B.pg.goto(linkA); await B.pg.waitForSelector('#s-who');
   ok(await B.pg.locator('#s-new').isVisible() && !(await B.pg.locator('#s-mode').count()),'after deleting the volume, the old link is offered as a new volume with the picker');
   const errs=[...A.e,...B.e]; ok(errs.length===0,'no page errors in the two-phone invite scenario '+JSON.stringify(errs));
   await A.c.close(); await B.c.close(); }
 // R2-5 (code): copy fallbacks select the whole text; a toast from before a sheet moves out of its way
 { const c=await b.newContext({...devices['iPhone 13']}); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{if(m.type()==='error')e.push(m.text())}); await pg.goto(URL);
   await pg.evaluate(()=>{ Object.defineProperty(navigator,'clipboard',{value:{writeText:()=>Promise.reject(new Error('denied'))},configurable:true}); ui.volId=S.volumes[0].id; go('volume'); return shareSheet(); });
   await pg.getByRole('button',{name:'Copy link'}).click(); await pg.waitForTimeout(100);
   const sel=await pg.evaluate(()=>{ const t=$('#s-link'); return {vis:!t.hidden && t.getBoundingClientRect().height>0, focus:document.activeElement===t, len:t.selectionEnd-t.selectionStart, all:t.value.length, msg:$('#sheetMsg').textContent}; });
   ok(sel.vis && sel.focus && sel.len===sel.all && sel.all>0 && /Link selected/.test(sel.msg),'clipboard refused: Copy link shows the link with all of it selected '+JSON.stringify(sel));
   await pg.evaluate(()=>{ $('#s-codebox').open=true; }); await pg.getByRole('button',{name:'Copy code'}).click(); await pg.waitForTimeout(100);
   ok(await pg.evaluate(()=>{ const t=$('#s-code'); return document.activeElement===t && t.selectionEnd-t.selectionStart===t.value.length; }),'clipboard refused: Copy code selects all of the code');
   await pg.keyboard.press('Escape');
   await pg.getByRole('button',{name:/Add expense/}).click(); await pg.fill('#f-amount','12'); await pg.fill('#f-desc','Snacks'); await pg.locator('#expForm button[type=submit]').click();
   await pg.getByRole('button',{name:/Settle up/}).click();
   const ov=await pg.evaluate(()=>{ const t=document.querySelector('.toast'); if(!t) return {toast:false}; const tr=t.getBoundingClientRect(); const hit=[...document.querySelectorAll('#sheetForm input,#sheetForm select,#sheetForm button')].filter(x=>{ const r=x.getBoundingClientRect(); return r.width && !(r.right<=tr.left||r.left>=tr.right||r.bottom<=tr.top||r.top>=tr.bottom); }).map(x=>x.id||x.textContent); return {toast:true,text:t.textContent,top:t.classList.contains('top'),hit}; });
   ok(ov.toast && /Expense saved/.test(ov.text) && ov.top && ov.hit.length===0,'Settle up right after "Expense saved": the toast stays, moved to the top, covering none of the sheet\'s fields '+JSON.stringify(ov));
   ok(e.length===0,'no page errors in R2-5 checks '+JSON.stringify(e)); await c.close(); }
 // L3-3 (code): a volume too big for a link gets the Copy code sheet; an invite opened in a running page leaves no dead Back press
 { const c=await b.newContext({...devices['Pixel 7']}); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{if(m.type()==='error')e.push(m.text())}); await pg.goto(URL);
   const big=await pg.evaluate(()=>{ const v=JSON.parse(JSON.stringify(S.volumes[0])); v.id='bigLink1'; v.title='Big'; v.expenses=[];
     for (let i=0;i<2600;i++) v.expenses.push({id:'bl'+i,desc:'Shared thing '+i+' é '+Math.random().toString(36).slice(2,12)+Math.random().toString(36).slice(2,12),amount:1000+i,paidBy:v.meId,category:'food',date:'2026-10-01',splitType:'exact',splits:{[v.meId]:1000+i},inputs:{},note:'',upd:0});
     S.volumes.push(v); ui.volId=v.id; go('volume'); return new TextEncoder().encode(shareCodeOf(v)).length; });
   await pg.evaluate(()=>shareSheet());
   ok(big>512*1024 && await pg.locator('#sheetForm [data-act=copyCode]').first().isVisible() && !(await pg.locator('#sheetForm [data-act=copyLink],#sheetForm [data-act=sendLink],svg.qr').count()) && /tap <b>Import<\/b>|tap Import/.test(await pg.locator('#sheetForm').innerHTML()),`a ${(big/1024|0)} KB volume (over the 512 KB a receiver reads) gets the Copy code sheet, no link`);
   await pg.evaluate(()=>{ dropSheet(); ui.volId=S.volumes[0].id; go('volume'); return shareSheet(); });
   ok(await pg.locator('#sheetForm [data-act=copyLink]').isVisible(),'a normal volume still gets the link');
   const link=await pg.locator('#s-link').inputValue(), pay=link.split('#join=')[1];
   await pg.evaluate(()=>{ dropSheet(); S.volumes=S.volumes.filter(v=>v.id!=='bigLink1'); save(); go('hub'); ui.volId=S.volumes[0].id; go('volume'); });
   const h0=await pg.evaluate(()=>history.length);
   for (const [p,why] of [[pay,'a good invite'],[pay.slice(0,30),'a broken invite']]) {
     await pg.evaluate(p=>{ location.hash='join='+p; },p); await pg.waitForSelector('#sheetForm'); await pg.waitForTimeout(150);
     const at=await pg.evaluate(()=>({view:ui.view,hash:location.hash,sheet:sheetOpen()}));
     await pg.keyboard.press('Escape'); await pg.waitForTimeout(50);
     await pg.goForward(); await pg.waitForTimeout(250);
     const fw=await pg.evaluate(()=>({view:ui.view,hash:location.hash,sheet:sheetOpen()}));
     ok(!fw.sheet && fw.view==='volume' && !/join/.test(fw.hash),`${why} opened in a running page: Forward after closing its sheet does not reopen it `+JSON.stringify(fw));
     await pg.goBack(); await pg.waitForTimeout(150);
     const r=await pg.evaluate(()=>({view:ui.view,hash:location.hash,n:history.length}));
     ok(at.sheet && at.view==='volume' && !/join/.test(at.hash) && r.view==='hub' && !/join/.test(r.hash),`${why} opened in a running page: after closing its sheet, Back goes from the volume to the hub `+JSON.stringify([at,r]));
     await pg.evaluate(()=>{ ui.volId=S.volumes[0].id; go('volume'); }); }
   ok(e.length===0,'no page errors in L3-3 checks '+JSON.stringify(e)); await c.close(); }
 // R2-1 (code): form fields grow with text size. Playwright cannot emulate Android textZoom/min font size, so the 32px case injects the size on .input.
 { const e=[]; const bad=[];
   for (const w of [360,390]) { const c=await b.newContext({viewport:{width:w,height:780},deviceScaleFactor:2,isMobile:true,hasTouch:true}); const pg=await c.newPage(); pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{if(m.type()==='error')e.push(m.text())}); await pg.goto(URL);
     await pg.evaluate(()=>{ ui.volId=S.volumes[0].id; go('volume'); }); await pg.getByRole('button',{name:/Add expense/}).click();
     // Paid by (a designed picker since D0) sits on its own row over Date: ≥44px, same left and right edges as Date
     const at16=await pg.evaluate(()=>{ const r=id=>document.getElementById(id).getBoundingClientRect(); const p=r('f-paid'), d=r('f-date'); return {ph:p.height,dh:d.height,pl:p.left,dl:d.left,pr:p.right,dr:d.right}; });
     if (!(at16.ph>=44 && at16.dh===48 && Math.abs(at16.pl-at16.dl)<0.5 && Math.abs(at16.pr-at16.dr)<0.5)) bad.push({w,at16});
     const fit=async ids=>pg.evaluate(ids=>{ const probe=document.createElement('span'); probe.textContent='Ágy'; return ids.map(id=>{ const el=document.getElementById(id), cs=getComputedStyle(el);
       let lh=parseFloat(cs.lineHeight); if (isNaN(lh)) { probe.style.font=cs.font; probe.style.lineHeight='normal'; probe.style.display='inline-block'; document.body.appendChild(probe); lh=probe.getBoundingClientRect().height; probe.remove(); }
       return {id,fs:cs.fontSize,room:el.clientHeight-parseFloat(cs.paddingTop)-parseFloat(cs.paddingBottom),lh}; }); },ids);
     await pg.addStyleTag({content:'.input{font-size:32px!important}'});
     for (const f of await fit(['f-desc','f-date'])) if (!(f.fs==='32px' && f.room>=f.lh-0.5)) bad.push({w,...f});
     await pg.keyboard.press('Escape'); await pg.waitForTimeout(50); await pg.evaluate(()=>{ dropSheet(); settleSheet(); });
     for (const f of await fit(['s-amt'])) if (!(f.fs==='32px' && f.room>=f.lh-0.5)) bad.push({w,...f});
     await c.close(); }
   ok(bad.length===0,'R2-1: with injected 32px text, #f-desc/#f-date/#s-amt keep a full line of room; at 16px Date is 48px and the Paid by picker (≥44px) lines up with it (360, 390) '+JSON.stringify(bad.slice(0,3)));
   ok(e.length===0,'no page errors in R2-1 checks '+JSON.stringify(e)); }
 // R2-4 (code wiring, design look): the settle sheet marks the suggestion the form holds; editing away from every row clears it
 { const c=await b.newContext({...devices['Pixel 7']}); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{if(m.type()==='error')e.push(m.text())}); await pg.goto(URL);
   const n=await pg.evaluate(()=>{ const v=S.volumes[0]; if (debts(v).length<2) { const [a,b2,c2]=v.members.map(m=>m.id); v.expenses.push({id:'r24x',desc:'Two debts',amount:9000,paidBy:a,category:'food',date:'2026-10-01',splitType:'equal',splits:{[a]:3000,[b2]:3000,[c2]:3000},inputs:{},note:'',upd:Date.now()}); save(); }
     ui.volId=v.id; go('volume'); settleSheet(); return debts(v).length; });
   const st=()=>pg.evaluate(()=>({p:[...document.querySelectorAll('#sheetForm [data-sugg]')].map(x=>x.getAttribute('aria-pressed')), vis:[...document.querySelectorAll('#sheetForm [data-sugg] .ck')].map(x=>getComputedStyle(x).visibility), from:$('#s-from').value, to:$('#s-to').value, amt:$('#s-amt').value}));
   const ds=await pg.evaluate(()=>debts(vol()).map(d=>({from:d.from,to:d.to,amt:(d.amount/100).toFixed(2)})));
   const s0=await st();
   ok(n>=2 && s0.p[0]==='true' && s0.p.slice(1).every(x=>x==='false') && s0.vis[0]==='visible' && s0.vis[1]==='hidden' && s0.from===ds[0].from && s0.amt===ds[0].amt,'R2-4: Settle up opens with row 1 pressed and its check visible, others not pressed and check hidden '+JSON.stringify(s0));
   const geo=await pg.evaluate(()=>[...document.querySelectorAll('#sheetForm [data-sugg] .num')].map(x=>Math.round(x.getBoundingClientRect().right)));
   const col=await pg.evaluate(()=>{ const ck=document.querySelector('#sheetForm [data-sugg] .ck'); return [getComputedStyle(ck).color, getComputedStyle(document.body).color]; });
   ok(new Set(geo).size===1 && col[0]===col[1],'R2-4: amounts line up whether or not the row is chosen, and the check is drawn in --fg '+JSON.stringify([geo,col]));
   await pg.locator('#sheetForm [data-sugg="1"]').click();
   const s1=await st();
   ok(s1.p[0]==='false' && s1.p[1]==='true' && s1.vis[0]==='hidden' && s1.vis[1]==='visible' && s1.from===ds[1].from && s1.to===ds[1].to && s1.amt===ds[1].amt,'R2-4: tapping row 2 flips the marks and fills From/To/Amount from row 2 '+JSON.stringify(s1));
   await pg.fill('#s-amt','1.23');
   const s2=await st();
   ok(s2.p.every(x=>x==='false') && s2.vis.every(x=>x==='hidden'),'R2-4: editing Amount away from every suggestion leaves no row pressed '+JSON.stringify(s2));
   await pg.fill('#s-amt',ds[1].amt);
   ok((await st()).p[1]==='true','R2-4: typing a suggestion back in marks that row again');
   ok(e.length===0,'no page errors in R2-4 checks '+JSON.stringify(e)); await c.close(); }
 // Criterion 6 (hati-sync): live sync between an Android and an iPhone through the (mocked) server, one going offline
 { const fresh=async(dev,url=URL)=>{ const c=await b.newContext({...dev}); const pg=await c.newPage(); const e=[];
     pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{ if (m.type()==='error' && !/Failed to load resource|ERR_INTERNET_DISCONNECTED|WebSocket/.test(m.text())) e.push(m.text()); });
     await pg.goto(url); return {c,pg,e}; };
   const addExp=async (pg,desc,amt)=>{ await pg.getByRole('button',{name:/Add expense/}).click(); await pg.fill('#f-amount',amt); await pg.fill('#f-desc',desc); await pg.locator('#expForm button[type=submit]').click(); await pg.waitForFunction(()=>ui.view==='volume' && !!$('.hero')); };
   const tag=pg=>pg.evaluate(()=>{ const t=$('#syncTag'); return t ? t.textContent : null; });
   const descs=pg=>pg.evaluate(()=>vol().expenses.map(e=>e.desc).sort());
   const has=(pg,d,ms=5000)=>pg.waitForFunction(d=>{ const v=vol(); return !!v && v.expenses.some(e=>e.desc===d); },d,{timeout:ms}).then(()=>true,()=>false);
   const A=await fresh(devices['Pixel 7']), B=await fresh(devices['iPhone 13']);
   // A creates "Beach" as Ana with Ben, adds Rent, and shares it
   await A.pg.locator('[data-act=newVolume]').click(); await A.pg.fill('#s-title','Beach'); await A.pg.fill('#s-me','Ana'); await A.pg.fill('#s-others','Ben'); await A.pg.locator('#sheetForm button.primary').click();
   await addExp(A.pg,'Rent','100');
   ok(await tag(A.pg)===null,'S6-1: a volume nobody shared has no sync line');
   await A.pg.locator('.acts [data-act=share]').click(); await A.pg.waitForSelector('#s-link',{state:'attached'});
   const link=await A.pg.locator('#s-link').inputValue(), [room,key]=link.split('#join=')[1].split('.');
   ok(link===URL+'#join='+room+'.'+key && /^[\w-]{22}$/.test(room) && /^[\w-]{43}$/.test(key) && /keeps this volume in sync/.test(await A.pg.locator('#s-how').innerText()),'S6-1: sharing makes a live invite <page>#join=<room 22>.<key 43> '+link.slice(-70));
   ok(mroom(room).version===1 && !!mroom(room).blob && await A.pg.locator('svg.qr').count()===1,'S6-1: the volume is on the server (version 1) the moment the invite exists, and the QR holds the short link');
   const keyLen=await A.pg.evaluate(k=>unb64url(k).length,key); ok(keyLen===32,'S6-1: the key is 32 random bytes (AES-GCM-256), the room 16 bytes');
   await A.pg.keyboard.press('Escape'); await A.pg.waitForFunction(()=>$('#syncTag') && $('#syncTag').textContent==='Synced');
   ok(await tag(A.pg)==='Synced' && await A.pg.evaluate(()=>$('#syncTag').dataset.s)==='ok','S6-1: the hero says "Synced"');
   // B (iPhone) opens the link cold, picks Ben, joins
   await B.pg.goto(link); await B.pg.waitForSelector('#s-who');
   await pick(B.pg,'s-who',{label:'Ben'}); await B.pg.getByRole('button',{name:'Join'}).click();
   ok(await B.pg.evaluate(()=>ui.view==='volume' && vol().title==='Beach' && nameOf(vol(),vol().meId)==='Ben' && !/join/.test(location.hash)) && JSON.stringify(await descs(B.pg))==='["Rent"]','S6-2: B joins Beach as Ben from the live link; Rent is there');
   ok(await B.pg.evaluate(r=>JSON.parse(localStorage.getItem('hati2.sync.v1'))[ui.volId].room===r,room),'S6-2: B keeps the room with the volume');
   await B.pg.waitForFunction(()=>$('#syncTag') && $('#syncTag').textContent==='Synced');
   // A adds an expense; B sees it live (socket says version n, B pulls and merges), no reload
   const t0=Date.now(); await addExp(A.pg,'Taxi','23.45');
   ok(await has(B.pg,'Taxi',5000),`S6-3: A adds Taxi, B's screen shows it live in ${Date.now()-t0} ms`);
   await B.pg.waitForFunction(()=>/Taxi/.test($('#app').innerText),null,{timeout:2000}).catch(()=>{});
   ok(/Taxi/.test(await B.pg.locator('#app').innerText()) && /Beach: adds 1 expense/.test(await B.pg.evaluate(()=>($('.toast')||{}).textContent||'')),'S6-3: B\'s volume page redraws with Taxi and says "Beach: adds 1 expense."');
   // motion: the row that arrived inks in on its own (accent wash wiped off), within 300 ms
   const ink=await B.pg.evaluate(()=>{ const r=[...document.querySelectorAll('#app .tx.ink-new')]; return {n:r.length,txt:r.map(x=>x.querySelector('.tx-t').textContent).join(','),a:document.getAnimations().filter(a=>a.effect.target&&r.includes(a.effect.target)).map(a=>({n:a.animationName,d:a.effect.getTiming().duration,delay:a.effect.getTiming().delay}))}; });
   ok(ink.n===1 && ink.txt==='Taxi' && ink.a.length>0 && ink.a.every(a=>a.d+a.delay<=320),'S6-3: the live row (Taxi) inks in by itself '+JSON.stringify(ink));
   // Both go offline and each adds an expense; local saves are instant and queued
   await setNet(A.c,false); await setNet(B.c,false);
   await addExp(A.pg,'Lunch','30'); await addExp(B.pg,'Snacks','12.10');
   ok(await tag(A.pg)==='Offline — will sync' && await tag(B.pg)==='Offline — will sync','S6-4: offline, both phones save at once and say "Offline — will sync" ['+await tag(A.pg)+']');
   ok(await A.pg.evaluate(()=>JSON.parse(localStorage.getItem('hati2.sync.v1'))[ui.volId].dirty===true) && await B.pg.evaluate(()=>JSON.parse(localStorage.getItem('hati2.sync.v1'))[ui.volId].dirty===true),'S6-4: the outbox flag is stored on both phones');
   // B reconnects first (pushes), then A (409: someone wrote first → merge → push); both end with all four
   const c409=mock.n409; await setNet(B.c,true); await B.pg.waitForFunction(()=>$('#syncTag').textContent==='Synced',null,{timeout:5000}).catch(()=>{});
   await setNet(A.c,true);
   const both=await has(A.pg,'Snacks',6000) && await has(B.pg,'Lunch',6000);
   await A.pg.waitForFunction(()=>$('#syncTag') && $('#syncTag').textContent==='Synced',null,{timeout:5000}).catch(()=>{}); await B.pg.waitForFunction(()=>$('#syncTag') && $('#syncTag').textContent==='Synced',null,{timeout:5000}).catch(()=>{});
   const dA=await descs(A.pg), dB=await descs(B.pg);
   ok(both && JSON.stringify(dA)==='["Lunch","Rent","Snacks","Taxi"]' && JSON.stringify(dA)===JSON.stringify(dB) && mock.n409>c409,`S6-5: after both reconnect, both phones have all 4 expenses; the second writer got a 409 and merged (${mock.n409-c409} conflicts) `+JSON.stringify([dA,dB]));
   const cv=pg=>pg.evaluate(()=>{ const v=vol(), bl=balances(v); return {canon:canon(v), order:v.expenses.map(e=>e.id).join(), me:bl[v.meId], sum:Object.values(bl).reduce((a,b)=>a+b,0), you:nameOf(v,v.meId)}; });
   const xa=await cv(A.pg), xb=await cv(B.pg);
   ok(xa.canon===xb.canon && xa.order===xb.order && xa.sum===0 && xb.sum===0 && xa.me+xb.me===0 && xa.you==='Ana' && xb.you==='Ben',`S6-5: both phones hold the same volume in the same order, balances opposite to the cent (Ana ${xa.me}, Ben ${xb.me}) and sum to 0`);
   ok(await tag(A.pg)==='Synced' && await tag(B.pg)==='Synced' && await A.pg.evaluate(()=>JSON.parse(localStorage.getItem('hati2.sync.v1'))[ui.volId].dirty===false),'S6-5: both say "Synced" again and the outbox is empty');
   const quiet=mock.puts.length; await A.pg.waitForTimeout(600);
   ok(mock.puts.length===quiet,'S6-5: converged phones stop writing (no ping-pong of PUTs)');
   // reload: the room survives and the app catches up at start
   await B.pg.reload(); await B.pg.waitForFunction(()=>$('#syncTag') && $('#syncTag').textContent==='Synced',null,{timeout:5000}).catch(()=>{});
   ok(await tag(B.pg)==='Synced' && JSON.stringify(await descs(B.pg))===JSON.stringify(dA),'S6-5: after a reload B is still synced with the same 4 expenses '+JSON.stringify(await B.pg.evaluate(()=>({tag:$('#syncTag')&&$('#syncTag').textContent,view:ui.view,sy:SY,rt:Object.fromEntries(Object.entries(RT).map(([k,v])=>[k,{state:v.state,seen:typeof v.seen,want:v.want,ws:!!v.ws}])),on:navigator.onLine}))));
   // The server only ever sees ciphertext: no names, titles or amounts in any request; the key never leaves the phone
   const plain=['Beach','Rent','Taxi','Lunch','Snacks','"amount"','"name"','"desc"','23.45','2345','1210'];
   const leaks=mock.puts.filter(x=>x.room===room).filter(x=>plain.some(w=>x.body.includes(w)));
   const shapes=mock.puts.filter(x=>x.room===room).every(x=>{ const j=JSON.parse(x.body); return Object.keys(j).sort().join()==='baseVersion,blob' && Object.keys(j.blob).sort().join()==='ct,iv,v' && j.blob.v===1 && /^[\w-]{16}$/.test(j.blob.iv); });
   const ivs=mock.puts.filter(x=>x.room===room).map(x=>JSON.parse(x.body).blob.iv);
   ok(mock.puts.filter(x=>x.room===room).length>=4 && leaks.length===0 && shapes && new Set(ivs).size===ivs.length,`S6-6: ${ivs.length} PUT bodies are {baseVersion, blob:{v:1, iv, ct}} with a fresh IV each and no plaintext name, title or amount`);
   ok(!mock.urls.some(u=>u.includes(key)) && mock.urls.some(u=>u.startsWith('wss://')),'S6-6: the key is in no request URL (HTTP or WebSocket)');
   const opened=await A.pg.evaluate(async([r,k,b])=>{ const x=await unseal(r,k,b); return x && x.title+':'+x.expenses.length+':'+(x.meId===undefined); },[room,key,mroom(room).blob]);
   ok(opened==='Beach:4:true','S6-6: the server blob decrypts with the link\'s key to the whole volume (without who "you" are)');
   const wrongRoom=await A.pg.evaluate(async([k,b])=>await unseal('Z'.repeat(22),k,b),[key,mroom(room).blob]);
   ok(wrongRoom===null,'S6-6: the room id is bound in as additional data, so the blob fails in another room');
   // A wrong key or a damaged blob never touches local data
   const C=await fresh(devices['Pixel 7']); const wrongKey=key.slice(0,10)+(key[10]==='A'?'B':'A')+key.slice(11);
   const c0=await C.pg.evaluate(()=>JSON.stringify(S));
   await C.pg.evaluate(p=>{ location.hash='join='+p; },room+'.'+wrongKey); await C.pg.waitForFunction(()=>$('#sheetMsg') && $('#sheetMsg').textContent.length>0);
   ok(await C.pg.locator('#sheetMsg').innerText()==='This invite link is broken. Ask for a new one.' && await C.pg.evaluate(s=>JSON.stringify(S)===s && !localStorage.getItem('hati2.sync.v1'),c0),'S6-7: a link with the wrong key says it is broken and changes nothing');
   const a0=await A.pg.evaluate(()=>JSON.stringify(vol())), bad=mroom(room).version+1;
   mock.rooms.set(room,{version:bad,blob:{v:1,iv:'A'.repeat(16),ct:'garbage'+'x'.repeat(40)}}); mbroadcast(room,bad);
   await A.pg.waitForFunction(()=>$('#syncTag').dataset.s==='bad',null,{timeout:5000}).catch(()=>{});
   ok(await A.pg.evaluate(s=>JSON.stringify(vol())===s,a0) && /Can't read the shared copy/.test(await tag(A.pg)),'S6-7: a blob A can\'t decrypt leaves A\'s volume exactly as it was and says so ['+await tag(A.pg)+']');
   const other=await A.pg.evaluate(async([r])=>{ const n=await newRoom(); const v=JSON.parse(JSON.stringify(vol())); v.expenses=[]; return seal(r,n.key,payload(v)); },[room]);
   mock.rooms.set(room,{version:bad+1,blob:other}); mbroadcast(room,bad+1); await A.pg.waitForTimeout(500);
   ok(await A.pg.evaluate(s=>JSON.stringify(vol())===s,a0) && mroom(room).version===bad+1,'S6-7: a valid blob under someone else\'s key (an empty copy) is not read and not overwritten; nothing is lost');
   // 429 and 413: quiet, honest, local data kept
   mock.rooms.set(room,{version:bad+2,blob:(await (async()=>{ const s0=mock.puts.filter(x=>x.room===room).pop(); return JSON.parse(s0.body).blob; })())});
   await A.pg.evaluate(()=>{ rt(ui.volId).seen=undefined; }); mbroadcast(room,bad+2); await A.pg.waitForFunction(()=>$('#syncTag').dataset.s==='ok',null,{timeout:5000}).catch(()=>{});
   ok(await tag(A.pg)==='Synced','S6-8: once the room holds a readable copy again, A is back to "Synced"');
   mock.status=429; await addExp(A.pg,'Ice cream','4'); await A.pg.waitForFunction(()=>$('#syncTag').dataset.s==='busy',null,{timeout:5000}).catch(()=>{});
   ok(await tag(A.pg)==='Server busy — will sync soon' && await A.pg.evaluate(()=>vol().expenses.some(e=>e.desc==='Ice cream') && rt(ui.volId).until>Date.now()+50000),'S6-8: 429: the expense is saved here, the line says the server is busy, and the next try waits for Retry-After');
   mock.status=413; await A.pg.evaluate(()=>{ rt(ui.volId).until=0; return syncRun(ui.volId); });
   ok(await tag(A.pg)==='Too big to sync. It stays on this phone.' && await A.pg.evaluate(()=>vol().expenses.some(e=>e.desc==='Ice cream')),'S6-8: 413: the line says it is too big and the expense stays');
   mock.status=null; await A.pg.evaluate(()=>syncRun(ui.volId));
   ok(await has(B.pg,'Ice cream',5000) && await tag(A.pg)==='Synced','S6-8: when the server takes writes again, the queued expense reaches B');
   // Hidden page closes its socket; shown again it reopens and catches up
   const socks=()=>[...(mock.socks.get(room)||[])].filter(ws=>ws.__c===B.c).length;
   await B.pg.evaluate(()=>{ Object.defineProperty(document,'hidden',{value:true,configurable:true}); document.dispatchEvent(new Event('visibilitychange')); });
   await B.pg.waitForTimeout(200); const hiddenSocks=socks();
   await addExp(A.pg,'Parking','5');
   await B.pg.waitForTimeout(400); const missed=!(await B.pg.evaluate(()=>vol().expenses.some(e=>e.desc==='Parking')));
   await B.pg.evaluate(()=>{ Object.defineProperty(document,'hidden',{value:false,configurable:true}); document.dispatchEvent(new Event('visibilitychange')); });
   ok(hiddenSocks===0 && missed && await has(B.pg,'Parking',5000) && socks()===1,`S6-9: a hidden page closes its socket (${hiddenSocks} open) and gets nothing; shown again it reopens one and pulls Parking`);
   // a sheet open on the volume holds a live change until it closes, so an edit in progress is not lost
   await B.pg.getByRole('button',{name:/Settle up/}).click(); await B.pg.waitForSelector('#s-amt');
   await addExp(A.pg,'Fuel','8'); await B.pg.waitForTimeout(700);
   const held=!(await B.pg.evaluate(()=>vol().expenses.some(e=>e.desc==='Fuel')));
   await B.pg.fill('#s-amt','1.00'); await B.pg.getByRole('button',{name:'Record payment'}).click();
   ok(held && await has(B.pg,'Fuel',5000) && await B.pg.evaluate(()=>vol().settlements.some(s=>s.amount===100)) && await has(A.pg,'Fuel') && await A.pg.waitForFunction(()=>vol().settlements.some(s=>s.amount===100),null,{timeout:5000}).then(()=>true,()=>false),'S6-10: a change that arrives while Settle up is open waits; after Record payment both the payment and Fuel are on both phones');
   // Reduced motion: the status line does not animate
   const rm=await b.newContext({...devices['iPhone 13'],reducedMotion:'reduce'}); const R=await rm.newPage(); await R.goto(link); await R.waitForSelector('#s-who'); await pick(R,'s-who',{label:'Ana'}); await R.getByRole('button',{name:'Join'}).click();
   await R.waitForFunction(()=>$('#syncTag') && $('#syncTag').textContent==='Synced',null,{timeout:5000}).catch(()=>{});
   const mo=await R.evaluate(()=>{ const t=$('#syncTag'); t.dataset.s=''; paintSync(ui.volId); const n=document.getAnimations().filter(a=>t.contains(a.effect.target)).length; return n; });
   const mn=await A.pg.evaluate(()=>{ const t=$('#syncTag'); t.dataset.s=''; paintSync(ui.volId); const as=document.getAnimations().filter(a=>t.contains(a.effect.target)); return as.map(a=>a.effect.getTiming().duration); });
   ok(mo===0 && mn.length>0 && mn.every(d=>d<=200),'S6-11: the status line stamps and inks in over '+JSON.stringify(mn)+' ms, and not at all under reduced motion');
   await rm.close();
   const errs=[...A.e,...B.e,...C.e]; ok(errs.length===0,'no page errors in the live sync scenario '+JSON.stringify(errs));
   for (const x of [A,B,C]) await x.c.close(); }
 // LP (owner: "long press volume at the home page to edit, delete or archive"; hati-design, hati-motion, hati-a11y,
 // hati-sync, hati-copy): hold a hub card 450 ms for its options sheet (also right-click, Shift+F10, the menu key and a
 // real "Options for …" button); Edit (name, type, currency, members), Archive (per phone, a closed shelf, read-only),
 // Delete (per phone, a tombstone, Undo). And CAT: 12 expense categories, old ids kept, unknown ids are Other.
 { const mk=async (o={},sync=false)=>{ const c=await b.newContext({...devices['iPhone 13'],...o}); if (!sync) await c.addInitScript(noSync);
     await c.addInitScript(()=>{ window.__buzz=[]; navigator.vibrate=ms=>{ window.__buzz.push(ms); return true; }; });
     const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{ if (m.type()==='error' && !/Failed to load resource|ERR_INTERNET_DISCONNECTED|WebSocket/.test(m.text())) e.push(m.text()); });
     await pg.goto(URL); return {c,pg,e}; };
   const twoVols=pg=>pg.evaluate(()=>{ const x=exampleVolume(); x.title='Bali'; x.members.push({id:'zed',name:'Zed'}); S.volumes.unshift(x); save(); render(); return x.id; });
   const sheetState=pg=>pg.evaluate(()=>({view:ui.view,open:sheetOpen(),title:sheetOpen()&&$('#sheetTitle').textContent,acts:[...document.querySelectorAll('#sheetForm [data-act]')].map(x=>x.textContent.trim())}));
   // a real touch through CDP: finger down, hold, (move), up
   const touchHold=async (pg,sel,ms,move=0)=>{ const cdp=await pg.context().newCDPSession(pg); const r=await pg.locator(sel).first().boundingBox(), x=r.x+r.width*0.6, y=r.y+r.height/2;
     await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x,y}]});
     if (move) { await pg.waitForTimeout(100); await cdp.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x,y:y+move}]}); }
     await pg.waitForTimeout(ms); const mid=await sheetState(pg); await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]}); await pg.waitForTimeout(250); return mid; };
   const P=await mk(), pg=P.pg; const baliId=await twoVols(pg);
   // LP-1 hold: the sheet opens at ~450 ms (not before), with a buzz; releasing never opens the volume
   const early=await touchHold(pg,'.vol',200);
   ok(!early.open && (await sheetState(pg)).view==='volume' && !(await sheetState(pg)).open,'LP-1: a 200 ms touch is a tap, not a hold (opens the volume, no sheet) '+JSON.stringify(early));
   await pg.evaluate(()=>{ ui.view='hub'; go('hub',{replace:true}); });
   const mid=await touchHold(pg,'.vol',650), after=await sheetState(pg);
   ok(mid.open && mid.title==='Bali' && JSON.stringify(mid.acts)==='["Edit volume","Archive","Delete volume","Cancel"]','LP-1: holding a hub card ~450 ms opens its sheet: Edit volume, Archive, Delete volume, Cancel '+JSON.stringify(mid));
   ok(after.view==='hub' && after.open && await pg.evaluate(()=>location.hash)==='','LP-1: lifting the finger after the hold does not open the volume or close the sheet '+JSON.stringify(after));
   ok(JSON.stringify(await pg.evaluate(()=>__buzz))==='[12]','LP-1: the hold buzzes once (navigator.vibrate 12 ms)');
   await pg.keyboard.press('Escape');
   // LP-2 a moving finger cancels; a tap opens
   const moved=await touchHold(pg,'.vol',600,40);
   ok(!moved.open && !(await sheetState(pg)).open,'LP-2: moving the finger 40px cancels the hold (no sheet) '+JSON.stringify(moved));
   await pg.evaluate(()=>go('hub',{replace:true}));
   await pg.locator('.vol').first().tap(); await pg.waitForFunction(()=>ui.view==='volume',null,{timeout:2000}).catch(()=>{});
   ok(await pg.evaluate(()=>ui.view==='volume' && vol().title==='Bali' && !sheetOpen()),'LP-2: a plain tap still opens the volume');
   await toHub(pg);
   // LP-3 ink fill while holding (mouse hold): a wash grows from the left over 450 ms, linear; gone on release
   await pg.locator('.vol').first().hover(); await pg.mouse.down(); await pg.waitForTimeout(120);
   const ink=await pg.evaluate(()=>{ const c=$('.vol'); const a=c.getAnimations({subtree:true}).filter(a=>a.animationName==='hold-ink'); return {cls:c.classList.contains('holding'),n:a.length,d:a[0]&&a[0].effect.getTiming().duration,ease:a[0]&&a[0].effect.getTiming().easing,pseudo:a[0]&&a[0].effect.pseudoElement,tf:getComputedStyle(c,'::before').transform}; });
   await pg.mouse.up(); await pg.waitForTimeout(50);
   const gone=await pg.evaluate(()=>({cls:!!document.querySelector('.vol.holding'),open:sheetOpen(),view:ui.view}));
   ok(ink.cls && ink.n===1 && ink.d===450 && ink.pseudo==='::before' && ink.tf!=='none','LP-3: holding inks the card from the left (::before, scaleX, 450 ms) '+JSON.stringify(ink));
   ok(!gone.cls && !gone.open && gone.view==='volume','LP-3: letting go at 120 ms drops the ink and is a plain tap '+JSON.stringify(gone));
   await toHub(pg);
   // LP-4 the same sheet without a hold: right-click, Shift+F10, the menu key, the Options button (keyboard and screen reader)
   await pg.locator('.vol').first().click({button:'right'});
   ok((await sheetState(pg)).title==='Bali' && (await sheetState(pg)).view==='hub','LP-4: right-click opens the sheet');
   await pg.keyboard.press('Escape');
   ok(await pg.evaluate(()=>document.activeElement===$('.vol')),'LP-4: closing it returns focus to the card');
   await pg.keyboard.press('Shift+F10'); ok((await sheetState(pg)).title==='Bali','LP-4: Shift+F10 on the focused card opens the sheet');
   await pg.keyboard.press('Escape'); await pg.keyboard.press('ContextMenu'); ok((await sheetState(pg)).title==='Bali','LP-4: the context-menu key opens the sheet');
   await pg.keyboard.press('Escape');
   const ax=await pg.evaluate(()=>{ const c=$('.vol'), d=document.getElementById(c.getAttribute('aria-describedby')); return {keys:c.getAttribute('aria-keyshortcuts'),desc:d&&d.textContent}; });
   ok(ax.keys==='Shift+F10' && /Press and hold, right-click or press Shift\+F10 for Edit, Archive and Delete/.test(ax.desc),'LP-4: the card describes its options and announces Shift+F10 '+JSON.stringify(ax));
   const opt=pg.getByRole('button',{name:'Options for Bali'});
   ok(await opt.count()===1 && await opt.getAttribute('aria-haspopup')==='dialog','LP-4: screen readers get a real "Options for Bali" button (aria-haspopup=dialog)');
   await pg.keyboard.press('Tab');
   const ob=await pg.evaluate(()=>{ const a=document.activeElement, r=a.getBoundingClientRect(), c=$('.vol').getBoundingClientRect(); return {lbl:a.getAttribute('aria-label'),w:r.width,h:r.height,inCard:r.right<=c.right+1&&r.top>=c.top-1}; });
   ok(ob.lbl==='Options for Bali' && ob.w>=44 && ob.h>=44 && ob.inCard,'LP-4: Tab from the card lands on the Options button, shown 44px on the card '+JSON.stringify(ob));
   await pg.keyboard.press('Enter'); ok((await sheetState(pg)).title==='Bali','LP-4: Enter on it opens the sheet');
   await pg.keyboard.press('Escape');
   const hid=await pg.evaluate(()=>{ const f=document.activeElement===$('.vol-more'); $('.vol').focus(); const r=$('.vol-more').getBoundingClientRect(); return {back:f,w:r.width,h:r.height}; });
   ok(hid.back && hid.w<=1 && hid.h<=1,'LP-4: closing returns focus to the Options button, which is hidden from sight when not focused '+JSON.stringify(hid));
   // LP-5 Edit: name, type, currency, members (rename, add, remove only an unused one)
   await pg.locator('.vol').first().click({button:'right'}); await pg.locator('[data-act=editVol]').click();
   const rows=await pg.evaluate(()=>[...document.querySelectorAll('#sheetForm .mem-row')].map(r=>({n:r.querySelector('[data-mem]').value,x:!!r.querySelector('[data-drop]')})));
   ok(JSON.stringify(rows)==='[{"n":"Alex","x":false},{"n":"Mira","x":false},{"n":"Rio","x":false},{"n":"Zed","x":true}]','LP-5: Edit lists every member; only Zed (no expenses, not you) can be removed '+JSON.stringify(rows));
   await pg.fill('#s-title','Bali 2026'); await pick(pg,'s-type','event'); await pick(pg,'s-cur','€');
   await pg.locator('#sheetForm [data-mem]').nth(1).fill('Mira K'); await pg.locator('[data-drop]').click(); await pg.fill('#s-others','Noor, Ola');
   ok(await pg.evaluate(()=>$('.mem-row.drop [data-mem]').disabled && $('[data-drop]').getAttribute('aria-label')==='Keep Zed'),'LP-5: × marks Zed for removal (struck, "Keep Zed" undoes it)');
   await pg.fill('#s-others','Mira K'); await pg.locator('#sheetForm button.primary').click();
   ok(/Mira K is already in this volume/.test(await pg.locator('#sheetMsg').innerText()) && await pg.evaluate(()=>S.volumes[0].title==='Bali'),'LP-5: a duplicate name is refused and nothing changes');
   await pg.fill('#s-others','Noor, Ola'); await pg.locator('#sheetForm button.primary').click(); await pg.waitForTimeout(100);
   const ed=await pg.evaluate(id=>{ const v=S.volumes.find(x=>x.id===id); return {t:v.title,ty:v.type,c:v.currency,m:v.members.map(m=>m.name),gone:v.deleted.includes('zed'),valid:validVolume(v),sum:Object.values(balances(v)).reduce((a,b)=>a+b,0)}; },baliId);
   ok(ed.t==='Bali 2026' && ed.ty==='event' && ed.c==='€' && JSON.stringify(ed.m)==='["Alex","Mira K","Rio","Noor","Ola"]' && ed.gone && ed.valid && ed.sum===0,'LP-5: Save renames the volume and Mira, sets type and currency, removes Zed (tombstoned), adds Noor and Ola; balances still sum to 0 '+JSON.stringify(ed));
   await pg.reload(); ok(await pg.evaluate(id=>{ const v=S.volumes.find(x=>x.id===id); return v.title==='Bali 2026' && v.members.length===5; },baliId),'LP-5: the edit survives a reload');
   // LP-6 Archive: a closed "Archived (1)" shelf at the bottom, read-only page, survives reload
   await pg.locator('.vol',{hasText:'Bali 2026'}).click({button:'right'}); await pg.locator('#sheetForm [data-act=archive]').click();
   const sh=await pg.evaluate(()=>({live:[...document.querySelectorAll('#app .sect > .stack .vol')].map(x=>x.querySelector('.vol-name').textContent),box:!!$('#archBox'),open:$('#archBox')&&$('#archBox').open,sum:$('#archBox summary').textContent,last:$('#archBox')===$('#archBox').parentElement.querySelector('details:last-of-type'),focus:document.activeElement===$('#archBox summary'),toast:$('.toast').textContent}));
   ok(JSON.stringify(sh.live)==='["Tokyo Trip (example)"]' && sh.box && !sh.open && sh.sum==='Archived (1)' && sh.focus && /Bali 2026 archived/.test(sh.toast),'LP-6: Archive moves it to a closed "Archived (1)" shelf at the bottom, focus on the shelf '+JSON.stringify(sh));
   await pg.reload();
   ok(await pg.evaluate(id=>!!S.volumes.find(x=>x.id===id).archived && $('#archBox summary').textContent==='Archived (1)',baliId),'LP-6: archived survives a reload');
   await pg.locator('#archBox summary').click(); await pg.locator('.vol.arch').click(); await pg.waitForFunction(()=>ui.view==='volume');
   const ro=await pg.evaluate(()=>({add:!!$('[data-act=addExpense]'),settle:!!$('[data-act=settle]'),share:!!$('#app [data-act=share]'),rows:document.querySelectorAll('#app .tx').length,btnRows:document.querySelectorAll('#app button.tx').length,chips:document.querySelectorAll('#app button.chip').length,note:($('.arch-note')||{}).textContent,un:!!$('.arch-note [data-act=unarchive]')}));
   ok(!ro.add && !ro.settle && !ro.share && ro.rows>0 && ro.btnRows===0 && ro.chips===0 && /Archived\. Read-only on this phone\./.test(ro.note) && ro.un,'LP-6: an archived volume opens read-only (no Add, Settle, Share; rows and members are not buttons; Unarchive there) '+JSON.stringify(ro));
   await pg.evaluate(()=>{ location.hash='#v-'+ui.volId+'-new'; }); await pg.waitForTimeout(150);
   ok(await pg.evaluate(()=>ui.view)!=='expense','LP-6: the new-expense address of an archived volume does not open the form');
   await toHub(pg); if (!(await pg.evaluate(()=>$('#archBox').open))) await pg.locator('#archBox summary').click();
   await pg.locator('.vol.arch').click({button:'right'});
   ok(JSON.stringify((await sheetState(pg)).acts)==='["Open","Unarchive","Delete volume","Cancel"]','LP-6: an archived card\'s sheet offers Open, Unarchive, Delete volume');
   await pg.locator('#sheetForm [data-act=unarchive]').click();
   ok(await pg.evaluate(id=>!S.volumes.find(x=>x.id===id).archived && !$('#archBox') && document.activeElement===$(`.vol[data-id="${id}"]`),baliId),'LP-6: Unarchive puts it back with the others and focuses it');
   // LP-7 archive is this phone's: kept by backup/restore and mergeVolumes, never sent in a share code or sync payload
   await pg.evaluate(id=>{ S.volumes.find(x=>x.id===id).archived=Date.now(); save(); render(); },baliId);
   const backup=await pg.evaluate(()=>JSON.stringify({app:'hati2-mock',kind:'backup',v:1,volumes:S.volumes}));
   const mg=await pg.evaluate(id=>{ const v=S.volumes.find(x=>x.id===id), plain={...v}; delete plain.archived;
     return {keep:!!mergeVolumes(v,plain).vol.archived, notTaken:!mergeVolumes(plain,v).vol.archived, payload:!/archived/.test(payload(v)), code:!/archived/.test(shareCodeOf(v)), canon:canon(v)===canon(plain)}; },baliId);
   ok(mg.keep && mg.notTaken && mg.payload && mg.code && mg.canon,'LP-7: mergeVolumes keeps the local archived and never takes it from the other copy; sync payload, share code and canon leave it out '+JSON.stringify(mg));
   const R=await mk(); await R.pg.evaluate(()=>{ S.volumes=[]; save(); render(); });
   await R.pg.evaluate(()=>importSheet({restore:true})); await R.pg.fill('#s-paste',backup); await R.pg.locator('#s-go').click(); await R.pg.waitForTimeout(600); await R.pg.locator('#s-go').click();
   await R.pg.waitForFunction(()=>!sheetOpen(),null,{timeout:3000}).catch(()=>{});
   ok(await R.pg.evaluate(id=>!!(S.volumes.find(x=>x.id===id)||{}).archived && $('#archBox summary').textContent==='Archived (1)',baliId),'LP-7: a backup restored on a fresh phone keeps the volume archived');
   await R.c.close();
   await pg.evaluate(id=>{ delete S.volumes.find(x=>x.id===id).archived; save(); render(); },baliId);
   // LP-8 Delete: confirm in the house voice, Undo for 6 s, a tombstone that a reload or a backup doesn't undo
   const before=await pg.evaluate(()=>S.volumes.map(v=>v.id).join());
   await pg.locator('.vol',{hasText:'Bali 2026'}).click({button:'right'}); await pg.locator('#sheetForm [data-act=delVol]').click();
   const cf=await pg.evaluate(()=>({h:$('#sheetTitle').textContent,p:$('#sheetForm p').textContent,b:$('#sheetForm button.danger').textContent}));
   ok(cf.h==='Delete volume?' && cf.p==='Bali 2026 and all its expenses and payments will be removed from this phone. Anyone you shared it with keeps their copy.' && cf.b==='Delete','LP-8: Delete asks first, plainly '+JSON.stringify(cf));
   await pg.locator('#sheetForm button.danger').click();
   const dl=await pg.evaluate(id=>({there:S.volumes.some(v=>v.id===id),gone:S.gone.includes(id),stored:JSON.parse(localStorage.getItem('hati2.mock.v1')).gone.includes(id),toast:$('.toast').textContent,undo:!!$('.toast button.undo'),hidden:$('.toast').getAttribute('aria-hidden')}),baliId);
   ok(!dl.there && dl.gone && dl.stored && dl.toast==='Volume deletedUndo' && dl.undo && dl.hidden===null,'LP-8: Delete removes it, stores a tombstone, and the toast offers a real Undo button '+JSON.stringify(dl));
   await pg.locator('.toast button.undo').click();
   ok(await pg.evaluate(([b,id])=>S.volumes.map(v=>v.id).join()===b && !S.gone.includes(id) && document.activeElement===$(`.vol[data-id="${id}"]`),[before,baliId]),'LP-8: Undo puts it back in the same place, drops the tombstone, focuses it');
   await pg.locator('.vol',{hasText:'Bali 2026'}).click({button:'right'}); await pg.locator('#sheetForm [data-act=delVol]').click(); await pg.locator('#sheetForm button.danger').click();
   await pg.waitForTimeout(6600);
   ok(await pg.locator('.toast').count()===0,'LP-8: the Undo toast is gone after about 6 s');
   await pg.reload(); ok(await pg.evaluate(id=>!S.volumes.some(v=>v.id===id) && S.gone.includes(id),baliId),'LP-8: after a reload it stays deleted (tombstone kept)');
   await pg.evaluate(()=>importSheet({restore:true})); await pg.fill('#s-paste',backup); await pg.locator('#s-go').click(); await pg.waitForTimeout(300);
   const rt1=await pg.locator('#s-sum').innerText();
   ok(/1 volume you deleted on this phone stays deleted/.test(rt1) && /merges 1 volume/i.test(rt1),'LP-8: restoring a backup that has it says it stays deleted: "'+rt1+'"');
   await pg.waitForTimeout(500); await pg.locator('#s-go').click(); await pg.waitForFunction(()=>!sheetOpen(),null,{timeout:3000}).catch(()=>{});
   ok(await pg.evaluate(id=>!S.volumes.some(v=>v.id===id),baliId),'LP-8: and the restore does not bring it back');
   ok(await pg.evaluate(()=>{ const v=exampleVolume(), raw=JSON.parse(localStorage.getItem('hati2.mock.v1')); raw.volumes.push({...v,id:raw.gone[0]}); return normalize(raw).volumes.every(x=>!raw.gone.includes(x.id)); }),'LP-8: a stored copy of a deleted volume (another tab, an old write) is dropped on load');
   // LP-9 reduced motion: no ink fill, the hold still works
   { const Q=await mk({reducedMotion:'reduce'}); await Q.pg.locator('.vol').first().hover(); await Q.pg.mouse.down(); await Q.pg.waitForTimeout(150);
     const rm=await Q.pg.evaluate(()=>({cls:$('.vol').classList.contains('holding'),an:document.getAnimations().length}));
     await Q.pg.waitForTimeout(450); const op=await sheetState(Q.pg); await Q.pg.mouse.up(); await Q.pg.waitForTimeout(200);
     ok(!rm.cls && rm.an===0 && op.open && (await sheetState(Q.pg)).view==='hub','LP-9: reduced motion: no ink fill or animation, the hold still opens the sheet '+JSON.stringify(rm));
     ok(Q.e.length===0,'LP-9: no page errors '+JSON.stringify(Q.e)); await Q.c.close(); }
   ok(P.e.length===0,'LP: no page errors '+JSON.stringify(P.e)); await P.c.close();
   // LP-10 two phones (mocked sync server): archive is per phone and sends nothing; delete leaves the room, nothing comes back; Undo rejoins
   { const A=await mk({...devices['Pixel 7']},true), B=await mk({},true);
     const addExp=async (pg,desc,amt)=>{ await pg.getByRole('button',{name:/Add expense/}).click(); await pg.fill('#f-amount',amt); await pg.fill('#f-desc',desc); await pg.locator('#expForm button[type=submit]').click(); await pg.waitForFunction(()=>ui.view==='volume' && !!$('.hero')); };
     await A.pg.locator('[data-act=newVolume]').click(); await A.pg.fill('#s-title','Lake'); await A.pg.fill('#s-me','Ana'); await A.pg.fill('#s-others','Ben'); await A.pg.locator('#sheetForm button.primary').click();
     await addExp(A.pg,'Cabin','200');
     await A.pg.locator('.acts [data-act=share]').click(); await A.pg.waitForSelector('#s-link',{state:'attached'});
     const link=await A.pg.locator('#s-link').inputValue(), room=link.split('#join=')[1].split('.')[0]; await A.pg.keyboard.press('Escape');
     await B.pg.goto(link); await B.pg.waitForSelector('#s-who'); await pick(B.pg,'s-who',{label:'Ben'}); await B.pg.getByRole('button',{name:'Join'}).click();
     await B.pg.waitForFunction(()=>$('#syncTag') && $('#syncTag').textContent==='Synced',null,{timeout:5000}).catch(()=>{});
     const lakeId=await A.pg.evaluate(()=>ui.volId); await toHub(A.pg); await A.pg.waitForTimeout(400);
     const puts0=mock.puts.length;
     await A.pg.locator('.vol',{hasText:'Lake'}).click({button:'right'}); await A.pg.locator('#sheetForm [data-act=archive]').click(); await A.pg.waitForTimeout(800);
     ok(mock.puts.length===puts0 && await A.pg.evaluate(id=>!!S.volumes.find(v=>v.id===id).archived,lakeId) && await B.pg.evaluate(()=>!vol().archived),'LP-10: archiving on one phone sends nothing and the other phone keeps it unarchived (per-phone)');
     await addExp(B.pg,'Boat','60');
     const got=await A.pg.waitForFunction(id=>{ const v=S.volumes.find(x=>x.id===id); return v && v.expenses.some(e=>e.desc==='Boat'); },lakeId,{timeout:5000}).then(()=>true,()=>false);
     ok(got && await A.pg.evaluate(id=>!!S.volumes.find(v=>v.id===id).archived,lakeId),'LP-10: an archived volume still receives live changes and stays archived');
     const blob=mroom(room).blob, plain=await A.pg.evaluate(async([r,k,bl])=>JSON.stringify(await unseal(r,k,bl)),[room,link.split('.').pop(),blob]);
     ok(!/archived/.test(plain),'LP-10: the encrypted copy on the server has no archived flag');
     if (!(await A.pg.evaluate(()=>$('#archBox').open))) await A.pg.locator('#archBox summary').click();
     await A.pg.locator('.vol.arch').click({button:'right'}); await A.pg.locator('#sheetForm [data-act=delVol]').click(); await A.pg.locator('#sheetForm button.danger').click();
     ok(await A.pg.evaluate(id=>!SY[id] && !JSON.parse(localStorage.getItem('hati2.sync.v1')||'{}')[id] && S.gone.includes(id),lakeId),'LP-10: deleting leaves the sync room on this phone (tombstone kept)');
     await A.pg.locator('.toast button.undo').click(); await A.pg.waitForTimeout(300);
     ok(await A.pg.evaluate(([id,r])=>!!S.volumes.find(v=>v.id===id) && SY[id] && SY[id].room===r,[lakeId,room]),'LP-10: Undo brings the volume back into the same room');
     await addExp(B.pg,'Fuel stop','15');
     const back=await A.pg.waitForFunction(id=>{ const v=S.volumes.find(x=>x.id===id); return v && v.expenses.some(e=>e.desc==='Fuel stop'); },lakeId,{timeout:5000}).then(()=>true,()=>false);
     ok(back,'LP-10: after Undo, live changes arrive again');
     await A.pg.evaluate(()=>{ if (!$('#archBox').open) $('#archBox summary').click(); });
     await A.pg.locator('.vol.arch').click({button:'right'}); await A.pg.locator('#sheetForm [data-act=delVol]').click(); await A.pg.locator('#sheetForm button.danger').click(); await A.pg.waitForTimeout(6600);
     await addExp(B.pg,'Ice cream','4'); await A.pg.waitForTimeout(1500);
     ok(await A.pg.evaluate(id=>!S.volumes.some(v=>v.id===id),lakeId) && await A.pg.evaluate(()=>!document.body.innerText.includes('Lake')),'LP-10: once deleted, a live change from the other phone does not bring it back');
     await A.pg.reload(); await A.pg.waitForTimeout(800);
     ok(await A.pg.evaluate(id=>!S.volumes.some(v=>v.id===id),lakeId) && await B.pg.evaluate(()=>vol().expenses.length===4),'LP-10: nor does a reload; the other phone keeps the volume with all 4 expenses');
     const bc=[...A.e,...B.e]; ok(bc.length===0,'LP-10: no page errors '+JSON.stringify(bc)); await A.c.close(); await B.c.close(); }
 }
 // D0 (owner: pickers use the app's design; hati-design, hati-code, hati-a11y, hati-lazy-check, hati-ios, hati-motion):
 // the 7 native <select>s are <hati-pick>: a clay segmented row for up to 4 short options, else a clay button that opens
 // an ink sheet list. Same ids, same values saved.
 { const e=[]; const mk=async(o={})=>{ const c=await b.newContext({...devices['iPhone 13'],...o}); await c.addInitScript(noSync); const pg=await c.newPage(); pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{if(m.type()==='error')e.push(m.text())}); await pg.goto(URL); return {c,pg}; };
   const {c,pg}=await mk();
   // D0-1: no <select> in the shipped page, and none rendered by any screen or sheet that had one
   const src=await pg.evaluate(()=>fetch(location.href,{cache:'no-store'}).then(r=>r.text()));
   const seen=await pg.evaluate(()=>{ const n=[]; const v=S.volumes[0]; ui.volId=v.id; go('volume'); openExpense(null); n.push(document.querySelectorAll('select').length);
     go('volume'); volumeSheet(null); n.push(document.querySelectorAll('select').length); dropSheet(); volumeSheet(v); n.push(document.querySelectorAll('select').length); dropSheet(); settleSheet(null); n.push(document.querySelectorAll('select').length); dropSheet();
     sheet('<h2>Import</h2><div id="s-pick"></div>'); fillPick($('#s-pick'),JSON.parse(JSON.stringify(v))); n.push(document.querySelectorAll('select, hati-pick#s-mode').length); dropSheet(); return n; });
   ok(!/<select[\s>]/i.test(src) && seen.slice(0,4).every(x=>x===0) && seen[4]===1,'D0-1: no <select> in the page; expense, volume, settle and import sheets render none '+JSON.stringify(seen));
   // D0-2: a payer is one tap for 2–4 members (segmented row); 5 members get the button + sheet list
   const one=[];
   for (const names of [['Ivy'],['Ivy','Bo'],['Ivy','Bo','Cy']]) {
     await pg.evaluate(ns=>{ const me=uid(); const v={id:uid(),title:'Pay '+ns.length,type:'household',currency:'₱',meId:me,created:Date.now(),members:[{id:me,name:'Al'},...ns.map(n=>({id:uid(),name:n}))],expenses:[],settlements:[]}; S.volumes.unshift(v); save(); ui.volId=v.id; go('volume'); openExpense(null); },names);
     await pg.waitForTimeout(80);
     const mode=await pg.locator('#f-paid').getAttribute('data-mode');
     await pg.fill('#f-amount','12.50'); await pg.fill('#f-desc','Taxi');
     const taps=await pick(pg,'f-paid',{label:'Ivy'});
     const pressed=await pg.evaluate(()=>[...document.querySelectorAll('#f-paid [role=radio]')].map(r=>r.textContent+':'+r.getAttribute('aria-checked')).join(','));
     await pg.getByRole('button',{name:'Save expense'}).click(); await pg.waitForTimeout(80);
     const saved=await pg.evaluate(()=>{ const v=vol(), x=v.expenses[0]; return x && nameOf(v,x.paidBy); });
     one.push({n:names.length+1,mode,taps,pressed,saved}); }
   ok(one.every(r=>r.mode==='grid' && r.taps===1 && r.saved==='Ivy' && /Ivy:true/.test(r.pressed) && /You:false/.test(r.pressed)),'D0-2/X4: Paid by is the balanced grid and Ivy is one tap in volumes of 2, 3 and 4 members; the expense saves paidBy = Ivy '+JSON.stringify(one));
   await pg.evaluate(()=>{ const me=uid(); const v={id:uid(),title:'Big',type:'other',currency:'$',meId:me,created:Date.now(),members:[{id:me,name:'Al'},...['Ivy','Bo','Cy','Di','Ed'].map(n=>({id:uid(),name:n}))],expenses:[],settlements:[]}; S.volumes.unshift(v); save(); ui.volId=v.id; go('volume'); openExpense(null); });
   await pg.waitForTimeout(80);
   const big={mode:await pg.locator('#f-paid').getAttribute('data-mode'),btn:await pg.getByRole('button',{name:'Paid by You'}).count()};
   await pg.fill('#f-amount','30'); await pg.fill('#f-desc','Pizza'); const bt=await pick(pg,'f-paid',{label:'Ed'}); await pg.getByRole('button',{name:'Save expense'}).click(); await pg.waitForTimeout(80);
   const bs=await pg.evaluate(()=>nameOf(vol(),vol().expenses[0].paidBy));
   ok(big.mode==='grid' && big.btn===0 && bt===1 && bs==='Ed','X4: 6 members: Paid by stays the grid (no list button); Ed in 1 tap, saved '+JSON.stringify([big,bt,bs]));
   // D0-3: keyboard and screen reader. Row: radiogroup named by its label, radios with aria-checked, one tab stop,
   // arrows move the choice (wrapping), Home/End. Sheet list: button "Currency ¥" (haspopup listbox), Enter opens a dialog
   // with a listbox named Currency, focus on the chosen option, arrows move focus only, Esc closes and returns focus,
   // Enter picks and closes; the sheet under it stays open.
   await pg.evaluate(()=>{ ui.volId=S.volumes.find(v=>/example/.test(v.title)).id; go('volume'); openExpense(null); }); await pg.waitForTimeout(80);
   const rg=pg.getByRole('radiogroup',{name:'Paid by'});
   const sem=await pg.evaluate(()=>{ const r=[...document.querySelectorAll('#f-paid [role=radio]')]; return {n:r.length,stops:r.filter(x=>x.tabIndex===0).map(x=>x.textContent),checked:r.filter(x=>x.getAttribute('aria-checked')==='true').map(x=>x.textContent)}; });
   await rg.getByRole('radio',{name:'You'}).focus(); await pg.keyboard.press('ArrowRight');
   const k1=await pg.evaluate(()=>({f:document.activeElement.textContent,v:nameOf(vol(),$('#f-paid').value),form:nameOf(vol(),ui.form.paidBy)}));
   await pg.keyboard.press('ArrowLeft'); await pg.keyboard.press('ArrowLeft'); const k2=await pg.evaluate(()=>document.activeElement.textContent+':'+document.activeElement.getAttribute('aria-checked'));
   await pg.keyboard.press('Home'); const k3=await pg.evaluate(()=>document.activeElement.textContent);
   await pg.keyboard.press('End'); const k4=await pg.evaluate(()=>document.activeElement.textContent+':'+[...document.querySelectorAll('#f-paid [role=radio]')].filter(x=>x.tabIndex===0).length);
   ok(await rg.count()===1 && sem.n===3 && sem.stops.join()==='You' && sem.checked.join()==='You' && k1.f==='Mira' && k1.v==='Mira' && k1.form==='Mira' && k2==='Rio:true' && k3==='You' && k4==='Rio:1','D0-3: Paid by is a radiogroup "Paid by" with one tab stop on the checked radio; arrows move focus and choice (wrapping), Home/End '+JSON.stringify([sem,k1,k2,k3,k4]));
   await pg.evaluate(()=>{ go('volume'); volumeSheet(vol()); }); await pg.waitForTimeout(250);
   const cb=pg.getByRole('button',{name:'Currency ¥'});
   const pop=await cb.getAttribute('aria-haspopup'); await cb.focus(); await pg.keyboard.press('Enter'); await pg.waitForTimeout(250);
   const dlg=pg.getByRole('dialog',{name:'Currency'}), lb=dlg.getByRole('listbox',{name:'Currency'});
   const s1=await pg.evaluate(()=>({f:document.activeElement.getAttribute('role')+':'+document.activeElement.textContent+':'+document.activeElement.getAttribute('aria-selected'),n:document.querySelectorAll('#pickLayer [role=option]').length,inert:$('#app').inert&&$('#layer').inert}));
   const s1v=await lb.count();
   await pg.keyboard.press('ArrowDown'); const s2=await pg.evaluate(()=>document.activeElement.textContent+':'+$('#s-cur').value);
   await pg.keyboard.press('Escape'); await pg.waitForTimeout(80);
   const s3=await pg.evaluate(()=>({gone:!$('#pickLayer'),sheet:sheetOpen(),focus:document.activeElement.classList.contains('pick-btn')&&!!document.activeElement.closest('#s-cur'),v:$('#s-cur').value,layer:$('#layer').inert}));
   await pg.keyboard.press('Enter'); await pg.waitForTimeout(250); await pg.keyboard.press('End'); await pg.keyboard.press('Enter'); await pg.waitForTimeout(80);
   const s4=await pg.evaluate(()=>({gone:!$('#pickLayer'),v:$('#s-cur').value,label:document.activeElement.textContent,focus:!!document.activeElement.closest('#s-cur')}));
   await pg.locator('#s-cur .pick-btn').click(); await pg.waitForTimeout(250); { const top=await pg.evaluate(()=>$('#pickLayer .sheet').getBoundingClientRect().top); await pg.mouse.click(12,top-12); } await pg.waitForTimeout(80);
   const s5=await pg.evaluate(()=>({gone:!$('#pickLayer'),sheet:sheetOpen(),v:$('#s-cur').value}));
   // Back (Android) closes only the list: the page under it stays
   await pg.keyboard.press('Escape'); await pg.waitForTimeout(80);
   await pg.evaluate(()=>{ ui.volId=S.volumes.find(v=>v.title==='Big').id; go('volume'); volumeSheet(vol()); }); await pg.waitForTimeout(250);
   await pg.locator('#s-cur .pick-btn').click(); await pg.waitForTimeout(250); await pg.goBack(); await pg.waitForTimeout(150);
   const bk=await pg.evaluate(()=>({gone:!$('#pickLayer'),view:ui.view,sheet:sheetOpen()}));
   ok(bk.gone && bk.view==='volume' && bk.sheet,'D0-3: Back with the Currency list open closes the list and keeps the sheet under it '+JSON.stringify(bk));
   await pg.keyboard.press('Escape'); await pg.waitForTimeout(80);
   await pg.evaluate(()=>{ ui.volId=S.volumes.find(v=>/example/.test(v.title)).id; go('volume'); volumeSheet(vol()); }); await pg.waitForTimeout(250);
   ok(pop==='listbox' && s1v===1 && s1.f==='option:¥:true' && s1.n===10 && s1.inert && s2==='Rp:¥' && s3.gone && s3.sheet && s3.focus && s3.v==='¥' && !s3.layer && s4.gone && s4.v==='RM' && s4.label==='RM' && s4.focus && s5.gone && s5.sheet && s5.v==='RM','D0-3: Currency: button with a listbox popup; Enter opens a dialog + listbox "Currency" focused on ¥; arrows move focus only; Esc closes it (not the volume sheet) and returns focus; End + Enter picks RM; a tap outside closes it '+JSON.stringify([pop,s1,s2,s3,s4,s5]));
   await pg.keyboard.press('Escape'); await pg.waitForTimeout(80);
   // D0-4: values saved are the same as before: volume type/currency, settle from/to (and a suggestion moves the pickers),
   // the import mode and "Choose your name" forcing a choice
   await pg.evaluate(()=>{ go('hub'); volumeSheet(null); }); await pg.waitForTimeout(250);
   await pg.fill('#s-title','Picker trip'); await pick(pg,'s-type','event'); await pick(pg,'s-cur','€'); await pg.fill('#s-me','Al'); await pg.fill('#s-others','Ivy, Bo');
   await pg.getByRole('button',{name:'Create'}).click(); await pg.waitForTimeout(150);
   const nv=await pg.evaluate(()=>({t:vol().type,c:vol().currency,n:vol().members.length}));
   await pg.evaluate(()=>settleSheet(null)); await pg.waitForTimeout(250);
   await pick(pg,'s-from',{label:'Ivy'}); await pick(pg,'s-to',{label:'Bo'}); await pg.fill('#s-amt','7.25'); await pg.getByRole('button',{name:'Record payment'}).click(); await pg.waitForTimeout(150);
   const st=await pg.evaluate(()=>{ const v=vol(), s=v.settlements[0]; return s && [nameOf(v,s.from),nameOf(v,s.to),s.amount].join(); });
   await pg.evaluate(()=>{ const v=vol(), [a,i,o]=v.members.map(m=>m.id); v.settlements=[]; v.expenses.push({id:'d0x',desc:'Two debts',amount:9000,paidBy:a,category:'food',date:'2026-10-01',splitType:'equal',splits:{[a]:3000,[i]:3000,[o]:3000},inputs:{},note:'',upd:Date.now()}); save(); render(); settleSheet(null); });
   await pg.waitForTimeout(250); await pg.locator('#sheetForm [data-sugg="1"]').click();
   const sg=await pg.evaluate(()=>{ const d=debts(vol())[1], ck=id=>[...document.querySelectorAll('#'+id+' [role=radio]')].filter(r=>r.getAttribute('aria-checked')==='true').map(r=>r.dataset.pv).join(); return {ok:$('#s-from').value===d.from && $('#s-to').value===d.to && ck('s-from')===d.from && ck('s-to')===d.to}; });
   await pg.keyboard.press('Escape'); await pg.waitForTimeout(80);
   const im=await pg.evaluate(async()=>{ const v=vol(), x=JSON.parse(JSON.stringify(v)); sheet('<h2>Import</h2><div id="s-pick"></div>'); fillPick($('#s-pick'),x);
     const r={mode:$('#s-mode').value,whoHidden:$('#s-whobox').hidden,labels:$('#s-mode').opts.map(o=>o[1]).join('|'),modeKind:$('#s-mode').dataset.mode}; return r; });
   await pick(pg,'s-mode','replace'); const im2=await pg.evaluate(()=>({whoHidden:$('#s-whobox').hidden,who:nameOf(vol(),$('#s-who').value),sum:$('#s-sum').textContent}));
   await pg.keyboard.press('Escape'); await pg.waitForTimeout(80);
   const im3=await pg.evaluate(()=>{ const x=JSON.parse(JSON.stringify(vol())); x.id=uid(); sheet('<h2>Join</h2><div id="s-pick"></div>'); fillPick($('#s-pick'),x); const p=$('#s-who'); return {v:p.value,checked:p.querySelectorAll('[aria-checked=true]').length,ph:p.dataset.ph,stop:[...p.querySelectorAll('[role=radio]')].filter(r=>r.tabIndex===0).length}; });
   await pg.keyboard.press('Escape'); await pg.waitForTimeout(80);
   ok(nv.t==='event' && nv.c==='€' && nv.n===3,'D0-4: New volume with Type Event and Currency € (picked) saves type "event", currency "€" '+JSON.stringify(nv));
   ok(st==='Ivy,Bo,725','D0-4: Settle up From Ivy To Bo 7.25 saves from/to ids and 725 cents '+st);
   ok(sg.ok,'D0-4: tapping a suggested payment moves the From and To pickers to it (value and checked radio)');
   ok(im.mode==='merge' && im.whoHidden && im.labels==='Merge|Replace|Keep both' && im.modeKind==='seg' && !im2.whoHidden && im2.who==='Al' && /Replaces your copy/.test(im2.sum),'D0-4: import of a volume already here: "What to do" is Merge | Replace | Keep both, Merge by default with the member picker hidden; Replace shows it on who you are '+JSON.stringify([im,im2]));
   ok(im3.v==='' && im3.checked===0 && im3.ph==='Choose your name' && im3.stop===1,'D0-4: a new volume\'s "Which member are you?" starts on no one (one tab stop, nothing checked) '+JSON.stringify(im3));
   await c.close();
   // D0-5: every picker in light and dark at 320/360/390: no sideways scroll, ≥44px targets, nothing cut, the row is a
   // row (no wrap) or became the button; picker text ≥4.5:1 (the list too). Contrast in all 13 palettes at 360.
   const geo=[], low=[];
   const probe=()=>{ const parse=c=>{ const m=c.match(/[\d.]+/g); if (!m) return [0,0,0,0]; const n=m.map(Number); if (/^color\(srgb/.test(c)) { n[0]*=255; n[1]*=255; n[2]*=255; } return n; };
     const lin=v=>{ v/=255; return v<=0.03928?v/12.92:((v+0.055)/1.055)**2.4; }, L=([r,g,b])=>0.2126*lin(r)+0.7152*lin(g)+0.0722*lin(b);
     const bgOf=el=>{ for (let e=el;e;e=e.parentElement) { const c=parse(getComputedStyle(e).backgroundColor); if (c.length<4||c[3]>0.95) return c; } return parse(getComputedStyle(document.body).backgroundColor); };
     const out={over:document.scrollingElement.scrollWidth>innerWidth,picks:[],low:[]};
     const con=el=>{ const a=L(parse(getComputedStyle(el).color)), c2=L(bgOf(el)); return (Math.max(a,c2)+0.05)/(Math.min(a,c2)+0.05); };
     document.querySelectorAll('hati-pick, #pickLayer').forEach(p=>{ if (!p.getClientRects().length) return;
       const t=[...p.querySelectorAll('[role=radio], .pick-btn, [role=option]')];
       const r=p.getBoundingClientRect(); out.picks.push({id:p.id||'list',mode:p.dataset.mode||'list',inView:r.left>=0&&r.right<=innerWidth,small:t.filter(x=>x.getBoundingClientRect().height<44||x.getBoundingClientRect().width<44).length,cut:t.filter(x=>x.matches('[role=radio]')&&x.scrollWidth>x.clientWidth+1).length,rows:new Set(t.filter(x=>x.matches('[role=radio]')).map(x=>Math.round(x.getBoundingClientRect().top))).size});
       for (const x of t) { const el=x.querySelector('.pv')||x.querySelector('span:not(.ck)')||x; const k=con(el); if (k<4.5) out.low.push({id:p.id||'list',t:x.textContent.trim(),k:+k.toFixed(2)}); } });
     return out; };
   const screens=[['expense',()=>{ dropSheet(); ui.volId=S.volumes[0].id; go('volume'); openExpense(null); }],['volume sheet',()=>{ dropSheet(); go('volume'); volumeSheet(vol()); }],['settle',()=>{ dropSheet(); settleSheet(null); }],
     ['import',()=>{ dropSheet(); const x=JSON.parse(JSON.stringify(vol())); sheet('<h2>Import</h2><div id="s-pick"></div>'); fillPick($('#s-pick'),x); $('#s-mode').choose('replace'); }],
     ['join',()=>{ dropSheet(); const x=JSON.parse(JSON.stringify(vol())); x.id=uid(); x.members.push({id:'d0long',name:'Maximiliana Wolfeschlegel'}); sheet('<h2>Join</h2><div id="s-pick"></div>'); fillPick($('#s-pick'),x); }]];
   for (const w of [320,360,390]) for (const mode of ['light','dark']) {
     const {c:c2,pg:q}=await mk({viewport:{width:w,height:740}});
     for (const [name,fn] of screens) {
       await q.evaluate(([m,f])=>{ S.settings.mode=m; applyTheme(); (0,eval)('('+f+')')(); },[mode,fn.toString()]); await q.waitForTimeout(60);
       // measured at rest, like the QR check: mid slide-up a fractional translateY reads a 44px target as 43.99997px
       await q.evaluate(()=>{ const s=$('#sheetForm'); return s && Promise.all(s.getAnimations({subtree:true}).map(a=>a.finished.catch(()=>{}))); });
       const r=await q.evaluate(probe); geo.push({w,mode,name,...r}); low.push(...r.low.map(x=>({w,mode,name,...x})));
       const btn=await q.$('hati-pick[data-mode=list] .pick-btn'); if (btn) { await btn.click(); await q.waitForTimeout(250); const r2=await q.evaluate(probe); geo.push({w,mode,name:name+' list',...r2}); low.push(...r2.low.map(x=>({w,mode,name:name+' list',...x}))); await q.keyboard.press('Escape'); } }
     await c2.close(); }
   const badGeo=geo.filter(g=>g.over||g.picks.some(p=>!p.inView||p.small||p.cut||p.rows>1));
   ok(badGeo.length===0 && geo.length>=30,'D0-5: all 7 pickers (and the sheet list) at 320/360/390, light and dark: no sideways scroll, in view, targets ≥44px, no cut labels, rows never wrap '+JSON.stringify(badGeo.slice(0,2)));
   ok(geo.filter(g=>g.w===320&&g.name==='volume sheet').every(g=>g.picks.find(p=>p.id==='s-type').mode==='list') && geo.filter(g=>g.w===390&&g.name==='volume sheet').every(g=>g.picks.find(p=>p.id==='s-type').mode==='seg') && geo.filter(g=>g.name==='join').every(g=>g.picks.find(p=>p.id==='s-who').mode==='list'),'D0-5: a row whose words do not fit becomes the button + list (Type at 320, a long member name), and stays a row where it fits (Type at 390)');
   { const {c:c3,pg:q}=await mk({viewport:{width:360,height:740}}); const ids=await q.evaluate(()=>PALETTES.map(x=>x.id));
     for (const id of ids) for (const mode of ['light','dark']) for (const [name,fn] of screens.slice(0,3)) {
       await q.evaluate(([id,m,f])=>{ S.settings.palette=id; S.settings.mode=m; applyTheme(); (0,eval)('('+f+')')(); },[id,mode,fn.toString()]);
       const r=await q.evaluate(probe); low.push(...r.low.map(x=>({id,mode,name,...x})));
       const lb=await q.$('hati-pick[data-mode=list] .pick-btn'); if (!lb) continue;
       await lb.click(); await q.waitForTimeout(220);
       const r2=await q.evaluate(probe); low.push(...r2.low.map(x=>({id,mode,name:name+' list',...x}))); await q.keyboard.press('Escape'); }
     await c3.close(); }
   ok(low.length===0,'D0-5: picker text (rows, checked and not, buttons, placeholder, list) is ≥4.5:1 in all 13 palettes, light and dark '+JSON.stringify(low.slice(0,4)));
   // Motion: the list comes up like a sheet (--dur-in, ease-out) and is instant under reduced motion
   const mo=[]; for (const rm of ['no-preference','reduce']) { const {c:c4,pg:q}=await mk({reducedMotion:rm});
     await q.evaluate(()=>{ ui.volId=S.volumes[0].id; go('volume'); volumeSheet(vol()); }); await q.waitForTimeout(300); await q.locator('#s-cur .pick-btn').click();
     mo.push(await q.evaluate(()=>{ const cs=getComputedStyle($('#pickLayer .sheet')); return cs.animationName+' '+cs.animationDuration+' '+cs.animationTimingFunction; })); await c4.close(); }
   ok(/^sheet-up 0\.2s cubic-bezier\(0\.2, 0\.8, 0\.2, 1\)$/.test(mo[0]) && /^none /.test(mo[1]),'D0-5: the sheet list rises in 200 ms ease-out, nothing under reduced motion '+JSON.stringify(mo));
   ok(e.length===0,'D0: no page errors '+JSON.stringify(e)); }
 // CAT (hati-design + hati-copy): 12 categories for trips and households, one tap, readable at 320, old ids kept
 { const c=await b.newContext({...devices['iPhone 13']}); await c.addInitScript(noSync); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{if(m.type()==='error')e.push(m.text())});
   await pg.goto(URL);
   const cats=await pg.evaluate(()=>({ids:CATEGORIES.map(c=>c[0]),labels:CATEGORIES.map(c=>c[1]),icons:CATEGORIES.map(c=>CAT_ICON[c[0]])}));
   ok(JSON.stringify(cats.labels)==='["Eating out","Groceries","Drinks","Transport","Fuel","Stay & rent","Fun","Shopping","Bills","Health","Gifts","Other"]','CAT: the 12 categories '+cats.labels.join(', '));
   ok(['food','transport','entertainment','utilities','shopping','other'].every(id=>cats.ids.includes(id)),'CAT: the six original ids are still categories (old data, links, older phones)');
   ok(cats.icons.every(s=>/^<svg viewBox="0 0 24 24"><path d="[^"]+"\/>/.test(s)) && new Set(cats.icons).size===12,'CAT: every category has its own 24px ink-line icon');
   const un=await pg.evaluate(()=>{ const v=exampleVolume(); v.expenses[0].category='pets'; v.expenses[1].category='groceries'; const cl=cleanVolume(v); S.volumes.unshift(cl); save(); ui.volId=cl.id; go('volume');
     return {pets:cl.expenses[0].category,gro:cl.expenses[1].category,bars:[...document.querySelectorAll('.glance .gl-b:last-child .gl-bars .row > span:first-child')].map(x=>x.textContent),icon:CAT_ICON.other.includes(document.querySelector('.tx[data-id="'+cl.expenses[0].id+'"] .tx-ic path').getAttribute('d'))}; });
   ok(un.pets==='other' && un.gro==='groceries' && un.icon && un.bars.includes('Other') && un.bars.includes('Groceries') && new Set(un.bars).size===un.bars.length,'CAT: an unknown id reads as Other (icon and summary), a new id is kept; the summary has one row per category '+JSON.stringify(un));
   const mg=await pg.evaluate(()=>{ const v=S.volumes[0], o=JSON.parse(JSON.stringify(v)); v.expenses[1].upd=5; o.expenses[1].upd=5; o.expenses[1].category='other';
     const a=mergeVolumes(v,o).vol.expenses[1].category; o.expenses[1].upd=6; const b=mergeVolumes(v,o).vol.expenses[1].category; return [a,b]; });
   ok(mg[0]==='groceries' && mg[1]==='other','CAT: an older phone sending a new category back as Other (same edit time) does not overwrite it; a newer edit does '+JSON.stringify(mg));
   for (const w of [320,360,390]) for (const mode of ['light','dark']) {
     await pg.setViewportSize({width:w,height:780});
     const r=await pg.evaluate(m=>{ S.settings.mode=m; applyTheme(); ui.volId=S.volumes[0].id; ui.view='volume'; openExpense(null); const g=$('.cats'), cs=[...g.querySelectorAll('.cat')];
       return {cols:getComputedStyle(g).gridTemplateColumns.split(' ').length,h:Math.round(g.getBoundingClientRect().height),cut:cs.filter(x=>x.scrollWidth>x.clientWidth+1||x.scrollHeight>x.clientHeight+1).map(x=>x.textContent),minH:Math.min(...cs.map(x=>x.getBoundingClientRect().height)),minW:Math.min(...cs.map(x=>x.getBoundingClientRect().width)),font:parseFloat(getComputedStyle(cs[0]).fontSize),pageX:document.documentElement.scrollWidth<=innerWidth}; },mode);
     ok(r.cols===(w<350?3:4) && r.cut.length===0 && r.minH>=44 && r.minW>=44 && r.font>=12.5 && r.h<=(w<350?280:220) && r.pageX,`CAT: ${w}px ${mode}: ${r.cols} across, all 12 readable (≥12.5px, nothing cut), targets ≥44px, grid ${r.h}px tall, no sideways scroll `+JSON.stringify(r));
   }
   await pg.setViewportSize({width:390,height:844});
   await pg.evaluate(()=>{ ui.volId=S.volumes[0].id; go('volume'); });
   await pg.getByRole('button',{name:/Add expense/}).click();
   ok(await pg.locator('.cat[aria-pressed=true]').innerText()==='Eating out','CAT: a volume with no saved expense starts on Eating out');
   await pg.fill('#f-amount','12'); await pg.fill('#f-desc','Milk'); await pg.locator('.cat',{hasText:'Groceries'}).click();
   ok(await pg.locator('.cat[aria-pressed=true]').innerText()==='Groceries','CAT: one tap picks a category');
   await pg.locator('#expForm button[type=submit]').click(); await pg.waitForFunction(()=>ui.view==='volume');
   await pg.getByRole('button',{name:/Add expense/}).click();
   ok(await pg.locator('.cat[aria-pressed=true]').innerText()==='Groceries','CAT: the next new expense in this volume starts on the last category used (Groceries)');
   await pg.reload(); ok(await pg.locator('.cat[aria-pressed=true]').innerText()==='Groceries','CAT: remembered after a reload');
   await pg.evaluate(()=>{ sessionStorage.clear(); ui.volId=S.volumes[1].id; go('volume'); openExpense(null); });
   ok(await pg.locator('.cat[aria-pressed=true]').innerText()==='Eating out','CAT: another volume keeps its own (Eating out)');
   ok(e.length===0,'CAT: no page errors '+JSON.stringify(e)); await c.close(); }
 // M (hati-motion, owner: "go all out with the animation, but make it still fast and optimized"): manga motion on every
 // change, all inside the timing tokens, transform/opacity/clip-path only, nothing at all under reduced motion, taps never
 // wait, rapid navigation ends in the right state, and a 4x-throttled CPU keeps long tasks out of the animation.
 { const mk=async o=>{ const c=await b.newContext({...devices['iPhone 13'],...o}); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{if(m.type()==='error')e.push(m.text())}); await pg.addInitScript(noSync); await pg.goto(URL); return {c,pg,e}; };
   // every animation on the page and inside the ghosts: what it moves, how long, how late
   const anims=pg=>pg.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>r([...document.getAnimations(),...Object.values(ghosts).flatMap(h=>ghostRoots.get(h)?ghostRoots.get(h).getAnimations():[])].map(a=>{ const t=a.effect.getTiming(), tg=a.effect.target;
     return {name:a.animationName||(a.transitionProperty?'transition '+a.transitionProperty:'js'),on:tg?((tg.getAttribute&&tg.getAttribute('class'))||tg.tagName.toLowerCase())+(a.effect.pseudoElement||''):'',d:+t.duration,delay:+t.delay,state:a.playState,props:[...new Set(a.effect.getKeyframes().flatMap(k=>Object.keys(k)))].filter(k=>!['offset','computedOffset','easing','composite'].includes(k))}; })))));
   const seen=[], step=async(pg,tag,want)=>{ const a=await anims(pg); a.forEach(x=>seen.push({...x,tag})); const hit=a.filter(x=>x.state==='running'&&want(x)); ok(hit.length>0,`M: ${tag}: ${[...new Set(hit.map(x=>x.name+' '+x.d+'ms'))].join(', ')||'no animation'}`); return a; };
   const seed=pg=>pg.evaluate(()=>{ const v=S.volumes[0], ids=v.members.map(m=>m.id); v.members.find(m=>m.id===v.meId).name='Ana'; for (let i=0;i<40;i++){ const amt=1000+i*7; v.expenses.push({id:'m'+i,desc:'Dinner '+i,amount:amt,paidBy:ids[i%ids.length],category:'food',date:'2026-02-'+String(1+i%28).padStart(2,'0'),splitType:'equal',splits:splitEqual(amt,ids),inputs:{},note:'',upd:1}); }
     const e={...exampleVolume(),id:'mEmpty',title:'Empty one',expenses:[],settlements:[],created:Date.now()}; e.members=e.members.map(m=>m.id===e.meId?{...m,name:'Ana'}:m); S.volumes.push(e); commit(); render(); });
   const A=await mk({viewport:{width:390,height:844}}); const pg=A.pg; await seed(pg);
   // screen change forward: the old page leaves as a slanted panel (a ghost that ignores taps); the new page's lists ink in
   await pg.locator('.vol').first().click();
   await step(pg,'hub to volume wipes the old page off to the left',x=>x.on.startsWith('ghost screen')&&x.name==='js');
   await step(pg,'hub to volume inks its rows in, 30 ms apart',x=>x.name==='ink-off');
   // a tap during the wipe lands on the real button under the ghost
   await pg.locator('[data-act=back]').click(); await pg.waitForTimeout(400);
   const tap=await pg.evaluate(()=>{ $('.vol').click(); const r=$('[data-act=addExpense]').getBoundingClientRect(), x=r.left+r.width/2, y=r.top+r.height/2; return {x,y,ghost:!!ghosts.screen,hit:!!(document.elementFromPoint(x,y)||{closest:()=>null}).closest('[data-act=addExpense]')}; });
   await pg.mouse.click(tap.x,tap.y);
   ok(tap.ghost && tap.hit && await pg.evaluate(()=>ui.view)==='expense','M: a tap during the screen wipe lands on the real button under it (Add expense opens) '+JSON.stringify(tap));
   await pg.locator('[data-act=back]').click(); await pg.waitForTimeout(400);
   // a long list: the entrance stagger is capped (row 7 on start together, rows past 12 are simply there)
   await pg.locator('[data-act=history]').first().click();
   const hs=await step(pg,'volume to history inks its rows in',x=>x.name==='ink-off');
   const hd=hs.filter(x=>x.name==='ink-off'); const rows=await pg.locator('#app .ledger > *').count();
   ok(rows>12 && hd.length<=12 && Math.max(...hd.map(x=>x.delay))===180 && Math.max(...hd.map(x=>x.delay+x.d))<=400,`M: stagger is capped: ${rows} rows, ${hd.length} animate, last starts at ${Math.max(...hd.map(x=>x.delay))} ms, all in by ${Math.max(...hd.map(x=>x.delay+x.d))} ms`);
   // Back plays the other way: the ghost leaves to the right and rows ink in from the right
   await pg.locator('[data-act=back]').click();
   const bk=await pg.evaluate(async()=>{ await new Promise(r=>requestAnimationFrame(r)); const h=ghosts.screen; const a=h&&h.getAnimations()[0]; const k=a?a.effect.getKeyframes():[]; return {back:!!h&&h.classList.contains('back'),to:k.length?k[k.length-1].transform:'',turn:$('#app').dataset.turn}; });
   ok(bk.back && /translateX\(\d/.test(bk.to) && bk.turn==='back','M: Back reverses the wipe (old page leaves to the right) '+JSON.stringify(bk));
   await pg.waitForTimeout(400);
   // clay press: the body squashes on the way down
   { const r=await pg.locator('.btn.primary').boundingBox(); await pg.mouse.move(r.x+r.width/2,r.y+r.height/2); await pg.mouse.down();
     await step(pg,'pressing clay squashes it',x=>x.name==='squash'); await pg.mouse.move(1,1); await pg.mouse.up(); }
   // sheets: up from the thumb edge; closing goes back down while the dim fades; the summary inks in
   await pg.getByRole('button',{name:/Settle up/}).click();
   await step(pg,'Settle up sheet slides up',x=>x.name==='sheet-up');
   await pg.waitForTimeout(250); await pg.keyboard.press('Escape');
   await step(pg,'closing the sheet takes it back down',x=>x.on==='sheet'&&x.name==='js');
   ok(!(await pg.evaluate(()=>sheetOpen())) && await pg.evaluate(()=>!document.querySelector('#sheetForm')),'M: a closing sheet is already gone from the page (only its ghost is moving)');
   // number roll: a saved expense rolls the balance (the text is the new amount at once)
   const before=await pg.locator('.hero .amt').innerText();
   await pg.getByRole('button',{name:/Add expense/}).click(); await pg.fill('#f-amount','90'); await pg.fill('#f-desc','Roll'); await pg.locator('#expForm button[type=submit]').click();
   await pg.waitForFunction(()=>ui.view==='volume'&&!!$('.hero .amt'));
   await step(pg,'the balance rolls to its new amount',x=>/^roll-(in|out)/.test(x.name));
   const roll=await pg.evaluate(()=>({txt:$('.hero .amt').innerText,was:$('.hero .amt').dataset.was,fig:$('.hero .amt .fig').textContent}));
   ok(roll.was===before && roll.txt===roll.fig && roll.txt!==before,'M: the number roll keeps the real text (only the new amount is read; the old one is a pseudo-element) '+JSON.stringify(roll));
   await pg.waitForTimeout(300);
   // settle-up done: the hero pulses once; paid off in full, "You're all settled" lands like a stamp
   await pg.getByRole('button',{name:/Settle up/}).click(); await pg.waitForTimeout(250);
   await pg.evaluate(()=>{ const v=vol(), me=balances(v)[v.meId], o=v.members.find(m=>m.id!==v.meId).id; $('#s-from').value=me<0?v.meId:o; $('#s-to').value=me<0?o:v.meId; $('#s-amt').value=(Math.abs(me)/100).toFixed(2); });
   await pg.locator('#sheetForm button.primary').click();
   await step(pg,'settle-up done pulses the balance',x=>x.name==='pulse');
   await step(pg,'all settled lands like a stamp',x=>x.name==='stamp'&&/stamped/.test(x.on));
   await step(pg,'"Payment recorded" toast is brushed in',x=>x.name==='toast-in');
   await pg.waitForFunction(()=>!!document.querySelector('.toast.out'),null,{timeout:7000}); // 6 s with Undo (D2)
   await step(pg,'the toast is wiped off when it ends',x=>x.name==='toast-out');
   await pg.waitForFunction(()=>!document.querySelector('.toast'),null,{timeout:1000}).catch(()=>{});
   ok(await pg.evaluate(()=>!document.querySelector('.toast')),'M: a wiped-off toast is removed');
   // empty state: the kaomoji lands like a stamp
   await pg.evaluate(()=>{ go('hub'); ui.volId='mEmpty'; go('volume'); });
   await step(pg,'an empty state stamps its kaomoji',x=>x.name==='stamp'&&/kao/.test(x.on));
   // share/invite: the summary inks in and the QR prints top to bottom
   await pg.waitForTimeout(400); await pg.getByRole('button',{name:'Share volume'}).click(); await pg.waitForSelector('svg.qr');
   await step(pg,'the QR prints in',x=>x.name==='qr-print');
   await step(pg,'the share summary inks in',x=>x.name==='ink-off'&&/share-sum/.test(x.on));
   await pg.waitForTimeout(300); await pg.keyboard.press('Escape');
   // the bounds: transform/opacity (clip-path only on small things), no interaction animation over 300 ms, a stagger
   // all in by 400 ms; collected from every step above
   const props=new Set(seen.flatMap(x=>x.props)), bad=seen.filter(x=>x.d>300||x.delay+x.d>400||x.props.some(p=>!['transform','opacity','clipPath'].includes(p)));
   const clip=[...new Set(seen.filter(x=>x.props.includes('clipPath')).map(x=>x.on.split(' ')[0]||x.on))];
   ok(seen.length>20 && bad.length===0,`M: ${seen.length} animations measured with getAnimations(): longest ${Math.max(...seen.map(x=>x.d))} ms, all in by ${Math.max(...seen.map(x=>x.delay+x.d))} ms, properties ${[...props].join('/')} ${JSON.stringify(bad.slice(0,3))}`);
   ok(clip.every(c=>['toast','qr','msg','span'].includes(c)),'M: clip-path only on small things (toast, QR, messages, sync words): '+clip.join(','));
   // rapid navigation: taps and history steps without waiting end in a right, settled state
   await pg.evaluate(()=>{ ui.volId=S.volumes[0].id; go('hub'); }); await pg.waitForTimeout(300);
   await pg.locator('.vol').first().click(); await pg.locator('[data-act=history]').first().click(); await pg.goBack(); await pg.goBack(); await pg.goForward();
   await pg.locator('[data-act=settle]').click(); await pg.keyboard.press('Escape'); await pg.locator('[data-act=volMenu]').click(); await pg.keyboard.press('Escape');
   await pg.locator('[data-act=history]').first().click(); await pg.locator('[data-act=back]').click();
   await pg.waitForTimeout(500);
   const fin=await pg.evaluate(()=>({view:ui.view,route:parseHash().view,h1:$('#app h1').textContent,title:vol()&&vol().title,ghosts:document.querySelectorAll('.ghost').length+Object.keys(ghosts).length,sheet:sheetOpen(),inert:$('#app').inert,nav:!!document.querySelector('#nav, nav, .nav'),hist:depth()}));
   ok(fin.view==='volume' && fin.route==='volume' && fin.h1===fin.title && fin.ghosts===0 && !fin.sheet && !fin.inert && !fin.nav,'M: after rapid taps, Back and Forward the app ends on the right screen with no ghost or sheet left '+JSON.stringify(fin));
   // an interrupted sheet: close and open another at once; one sheet, focus in it, nothing left behind
   await pg.evaluate(()=>{ settleSheet(null); closeSheet(); volMenuSheet(); }); await pg.waitForTimeout(400);
   const sh=await pg.evaluate(()=>({n:$('#layer').children.length,title:$('#sheetTitle').textContent,focus:$('#sheetForm').contains(document.activeElement),ghosts:document.querySelectorAll('.ghost').length}));
   ok(sh.n===1 && sh.title===(await pg.evaluate(()=>vol().title)) && sh.focus && sh.ghosts===0,'M: a sheet closed and another opened at once: one sheet, focused, no ghost left '+JSON.stringify(sh));
   await pg.keyboard.press('Escape');
   ok(A.e.length===0,'M: no page errors with motion on '+JSON.stringify(A.e));
   await A.c.close();

   // Reduced motion: the same paths run no animation at all (no ghosts, no CSS animations or transitions, no rolls)
   { const R=await mk({viewport:{width:390,height:844},reducedMotion:'reduce'}); const rp=R.pg; await seed(rp); const left=[];
     const none=async tag=>{ const a=await anims(rp), g=await rp.evaluate(()=>document.querySelectorAll('.ghost,.roll,.done').length); if (a.length||g) left.push(`${tag}: ${a.map(x=>x.name).join(',')} ${g}`); };
     await rp.locator('.vol').first().click(); await none('hub to volume');
     await rp.locator('[data-act=history]').first().click(); await none('history');
     await rp.locator('[data-act=back]').click(); await none('back');
     { const r=await rp.locator('.btn.primary').boundingBox(); await rp.mouse.move(r.x+r.width/2,r.y+r.height/2); await rp.mouse.down(); await rp.waitForTimeout(100); await none('press'); await rp.mouse.move(1,1); await rp.mouse.up(); }
     await rp.getByRole('button',{name:/Settle up/}).click(); await none('sheet open');
     await rp.evaluate(()=>{ const v=vol(), me=balances(v)[v.meId], o=v.members.find(m=>m.id!==v.meId).id; $('#s-from').value=me<0?v.meId:o; $('#s-to').value=me<0?o:v.meId; $('#s-amt').value=(Math.abs(me)/100).toFixed(2); });
     await rp.locator('#sheetForm button.primary').click(); await none('settle done and toast');
     await rp.getByRole('button',{name:/Add expense/}).click(); await rp.fill('#f-amount','90'); await rp.fill('#f-desc','Roll'); await rp.locator('#expForm button[type=submit]').click(); await rp.waitForFunction(()=>ui.view==='volume'); await none('balance update');
     await rp.evaluate(()=>{ go('hub'); ui.volId='mEmpty'; go('volume'); }); await none('empty state');
     await rp.getByRole('button',{name:'Share volume'}).click(); await rp.waitForSelector('svg.qr'); await none('share sheet');
     await rp.keyboard.press('Escape'); await none('sheet close');
     ok(left.length===0,'M: reduced motion: no running animation, ghost or roll on any of these paths '+JSON.stringify(left));
     ok(R.e.length===0,'M: no page errors under reduced motion '+JSON.stringify(R.e));
     await R.c.close(); }

   // N1: the ink-in marks only the first 12 rows of each list (a class on the rows, not an attribute selector on #app that
   // restyles every element), and history rows after the 32nd are only laid out near the screen (content-visibility)
   { const N=await mk({viewport:{width:390,height:844}}); const q=N.pg; await seed(q);
     await q.locator('.vol').first().click(); await q.waitForTimeout(350); await q.locator('[data-act=history]').first().click();
     await q.waitForFunction(()=>document.querySelector('#app .ink'),null,{timeout:2000}).catch(()=>{});
     const r=await q.evaluate(()=>{ const rows=[...document.querySelectorAll('#app .ledger > *')];
       return {rows:rows.length,ink:document.querySelectorAll('#app .ink').length,inkRows:rows.filter(x=>x.classList.contains('ink')).length,cv32:getComputedStyle(rows[31]).contentVisibility,cv33:getComputedStyle(rows[32]).contentVisibility,
         rule:[...document.styleSheets].flatMap(s=>[...s.cssRules]).some(function f(x){ return (x.selectorText||'').includes('data-turn') || [...(x.cssRules||[])].some(f); })}; });
     ok(r.rows>40 && r.inkRows===12 && r.ink<=13 && r.cv32==='visible' && r.cv33==='auto' && !r.rule,'N1: a screen change inks only the first 12 history rows (class, no #app[data-turn] rule), rows after the 32nd use content-visibility '+JSON.stringify(r));
     await q.locator('[data-act=back]').click(); await q.waitForTimeout(40);
     const bk=await q.evaluate(()=>({l:document.querySelectorAll('#app .ink.ink-l').length,all:document.querySelectorAll('#app .ink').length}));
     ok(bk.l>0 && bk.l===bk.all,'N1: Back inks from the left (.ink-l on every inked row) '+JSON.stringify(bk));
     ok(N.e.length===0,'N1: no page errors '+JSON.stringify(N.e)); await N.c.close(); }

   // Performance at 4x CPU throttle (CDP Emulation.setCPUThrottlingRate): a screen change into a 100-row list, a sheet
   // and a Back, 3 times each, with motion and with reduced motion as the baseline. Long tasks come from a
   // PerformanceObserver('longtask'), frame pacing from requestAnimationFrame over the 400 ms after the first frame.
   { const perf={};
     for (const rm of ['no-preference','reduce']) {
       const P=await mk({viewport:{width:390,height:844},reducedMotion:rm}); const q=P.pg; await seed(q);
       await q.evaluate(()=>{ const v=S.volumes[0], ids=v.members.map(m=>m.id); for (let i=40;i<100;i++){ const amt=1000+i*7; v.expenses.push({id:'m'+i,desc:'Dinner '+i,amount:amt,paidBy:ids[i%ids.length],category:'food',date:'2026-03-'+String(1+i%28).padStart(2,'0'),splitType:'equal',splits:splitEqual(amt,ids),inputs:{},note:'',upd:1}); } commit(); render(); }); await q.waitForTimeout(800);
       const cdp=await P.c.newCDPSession(q); await cdp.send('Emulation.setCPUThrottlingRate',{rate:4});
       const run=async(label,fn)=>{
         await q.evaluate(()=>{ window.__lt=[]; window.__fr=[]; if (window.__po) __po.disconnect(); window.__po=new PerformanceObserver(l=>l.getEntries().forEach(e=>__lt.push({s:e.startTime,d:e.duration}))); __po.observe({type:'longtask'}); const gen=(window.__gen=(window.__gen||0)+1); const tick=t=>{ if (gen!==__gen) return; __fr.push(t); if (__fr.length<300) requestAnimationFrame(tick); }; requestAnimationFrame(tick); });
         await q.waitForTimeout(250); const t0=await q.evaluate(fn); await q.waitForTimeout(800);
         const r=await q.evaluate(t0=>{ const f=__fr.filter(t=>t>=t0), first=f[0], win=f.filter(t=>t<=first+400), d=win.slice(1).map((t,i)=>t-win[i]); const s=[...d].sort((a,b)=>a-b);
           const lt=__lt.filter(x=>x.s+x.d>=t0-1); return {input:Math.round(Math.max(0,...lt.filter(x=>x.s<first).map(x=>x.d))),during:lt.filter(x=>x.s>=first).map(x=>Math.round(x.d)),med:+s[s.length>>1].toFixed(1),slow:d.slice(1).filter(x=>x>25).length,fps:Math.round((win.length-1)/((win[win.length-1]-first)/1000))}; },t0);
         (perf[label]=perf[label]||{})[rm]=[...((perf[label]||{})[rm]||[]),r]; };
       for (let k=0;k<3;k++) {
         await run('hub to volume',()=>{ const t=performance.now(); $('.vol').click(); return t; });
         await run('volume to history (100 rows)',()=>{ const t=performance.now(); $('[data-act=history]').click(); return t; });
         await run('Back to volume',()=>{ const t=performance.now(); $('[data-act=back]').click(); return t; });
         await run('Settle up sheet',()=>{ const t=performance.now(); $('[data-act=settle]').click(); return t; });
         await run('sheet close',()=>{ const t=performance.now(); closeSheet(); return t; });
         await run('Back to volumes',()=>{ const t=performance.now(); $('[data-act=back]').click(); return t; }); }
       ok(P.e.length===0,`M-perf: no page errors (${rm}) `+JSON.stringify(P.e)); await P.c.close(); }
     const med=a=>[...a].sort((x,y)=>x-y)[a.length>>1];
     for (const [label,v] of Object.entries(perf)) {
       // medians of 3 runs, so one GC pause on a shared CI machine does not decide it; every run is printed
       const m=v['no-preference'], base=v.reduce, during=m.map(r=>r.during), worst=med(during.map(d=>Math.max(0,...d))), inM=med(m.map(r=>r.input)), inB=med(base.map(r=>r.input));
       console.log(`  perf 4x ${label}: input task ${inM} ms (reduced motion ${inB} ms), long tasks during the animation ${JSON.stringify(during)}, frame median ${med(m.map(r=>r.med))} ms, ${med(m.map(r=>r.fps))} fps, slow frames after the first ${m.map(r=>r.slow).join('/')}`);
       ok(worst<=50 && (inM<=50 || inM-inB<=15) && med(m.map(r=>r.med))<=17.5 && med(m.map(r=>r.slow))<=1,`M-perf 4x CPU: ${label}: no long task over 50 ms from the animation (input task ${inM} ms vs ${inB} ms without motion; during ${JSON.stringify(during)}), frames ${med(m.map(r=>r.med))} ms median, ${med(m.map(r=>r.fps))} fps`); } }
 }
 // D1 Repeat entry: a new expense learns from this volume's past ones (description, category, split; the amount only on its own tap)
 { const c=await b.newContext({...devices['Pixel 7']}); await c.addInitScript(noSync); const pg=await c.newPage(); const errs=[]; pg.on('pageerror',x=>errs.push(x.message)); pg.on('console',m=>{if(m.type()==='error')errs.push(m.text())});
   await pg.goto(URL); await pg.waitForSelector('.vol');
   await pg.evaluate(()=>{ const me='me', iv='ivy1', eq=a=>splitEqual(a,[me,iv]);
     const E=(id,desc,amt,paid,cat,date,x={})=>({id,desc,amount:amt,paidBy:paid,category:cat,date,splitType:'equal',splits:eq(amt),inputs:{},note:'',upd:1,...x});
     S.volumes=[{id:'d1vol',title:'Bebe',type:'household',currency:'₱',meId:me,created:1,members:[{id:me,name:'Gery'},{id:iv,name:'Ivy'}],settlements:[],expenses:[
       E('a','Chowking',31975,iv,'food','2026-10-01'),E('b','Chowking',35235,iv,'food','2026-10-09'),
       E('c','House of Bagnet',33413,iv,'food','2026-10-03'),E('d','House of Bagnet',33413,iv,'food','2026-10-05'),E('d2','house of bagnet ',33413,iv,'food','2026-10-07'),
       E('e','Bus',43500,me,'transport','2026-10-02'),
       E('f','Rent share',1000000,me,'lodging','2026-10-04',{splitType:'shares',splits:splitShares(1000000,{[me]:2,[iv]:1}),inputs:{[me]:'2',[iv]:'1'}}),
       E('g','Uncle John',60000,me,'food','2026-10-06')]}];
     commit(); render(); });
   await pg.locator('.vol').first().click(); await pg.waitForSelector('[data-act=addExpense]'); await pg.waitForTimeout(350);
   const keys0=await pg.evaluate(()=>Object.keys(S.volumes[0].expenses[0]).sort().join());
   await pg.locator('[data-act=addExpense]').click(); await pg.waitForSelector('#expForm'); await pg.waitForTimeout(350);                          // tap 1
   const sug=()=>pg.evaluate(()=>[...document.querySelectorAll('#descSug [data-sug]')].map(b=>b.textContent.trim()));
   const s0=await sug();
   ok(JSON.stringify(s0)===JSON.stringify(['house of bagnet','Chowking','Uncle John']) && await pg.locator('#f-amount').inputValue()==='' && await pg.locator('#lastAmt').innerHTML()==='','D1: a new expense offers the 3 most used descriptions of this volume (latest spelling), most used first; the amount stays empty '+JSON.stringify(s0));
   const box=await pg.evaluate(()=>{ const g=document.getElementById('descSug'), bs=[...g.querySelectorAll('button')]; return {role:g.getAttribute('role'),name:g.getAttribute('aria-label'),h:Math.min(...bs.map(x=>x.getBoundingClientRect().height)),fit:bs.every(x=>x.getBoundingClientRect().right<=innerWidth),tab:bs.every(x=>x.tabIndex===0)}; });
   ok(box.role==='group' && !!box.name && box.h>=44 && box.fit && box.tab,'D1: the suggestions are real buttons in a named group, at least 44 px tall, inside the screen '+JSON.stringify(box));
   await pg.locator('#f-desc').fill('cho'); const s1=await sug();
   ok(JSON.stringify(s1)===JSON.stringify(['Chowking']),'D1: typing narrows the suggestions '+JSON.stringify(s1));
   await pg.locator('#descSug [data-sug]').first().click();                                                                                          // tap 2
   const a1=await pg.evaluate(()=>({desc:document.getElementById('f-desc').value,amt:document.getElementById('f-amount').value,cat:document.querySelector('.cat[aria-pressed=true]').dataset.cat,split:document.querySelector('[data-split][aria-pressed=true]').dataset.split,sug:document.querySelectorAll('#descSug [data-sug]').length,last:document.getElementById('lastAmt').textContent.trim(),focus:document.activeElement.id}));
   ok(a1.desc==='Chowking' && a1.amt==='' && a1.cat==='food' && a1.split==='equal' && a1.sug===0 && /Last time\s*₱352\.35/.test(a1.last) && a1.focus==='f-amount','D1: one tap fills description, category and split, never the amount; "Last time" offers the latest amount and the amount field has focus '+JSON.stringify(a1));
   await pg.locator('#lastAmt [data-last]').click();
   const a2=await pg.evaluate(()=>({amt:document.getElementById('f-amount').value,last:document.getElementById('lastAmt').innerHTML}));
   ok(a2.amt==='352.35' && a2.last==='','D1: the amount is filled only by tapping "Last time", and the hint then goes away '+JSON.stringify(a2));
   await pg.locator('#f-amount').fill('412.00');
   ok(/Last time/.test(await pg.locator('#lastAmt').textContent()),'D1: a different amount brings the "Last time" hint back');
   await pick(pg,'f-paid',{label:'Ivy'});                                                                                                            // tap 3
   await pg.locator('#expForm button[type=submit]').click(); await pg.waitForSelector('[data-act=addExpense]');                                       // tap 4
   const saved=await pg.evaluate(()=>{ const v=S.volumes[0], e=v.expenses.find(x=>x.amount===41200); return e&&{desc:e.desc,paid:e.paidBy,cat:e.category,sum:Object.values(e.splits).reduce((a,x)=>a+x,0),keys:Object.keys(e).sort().join(),bal:Object.values(balances(v)).reduce((a,x)=>a+x,0)}; });
   ok(saved && saved.desc==='Chowking' && saved.paid==='ivy1' && saved.cat==='food' && saved.sum===41200 && saved.keys===keys0 && saved.bal===0,'D1 scenario 16: "Chowking again, Ivy paid" in 4 taps (Add, suggestion, Ivy, Save) plus typing the amount; splits sum to the amount and no new field is stored '+JSON.stringify(saved));
   await pg.waitForTimeout(350); await pg.locator('[data-act=addExpense]').click(); await pg.waitForSelector('#expForm'); await pg.waitForTimeout(350);
   await pg.locator('#f-desc').fill('rent'); await pg.locator('#descSug [data-sug]').first().click(); await pg.locator('#f-amount').fill('900.00');
   const sh=await pg.evaluate(()=>({split:document.querySelector('[data-split][aria-pressed=true]').dataset.split,ins:[...document.querySelectorAll('#splitRows [data-in]')].map(i=>i.value).join()}));
   await pg.locator('#expForm button[type=submit]').click(); await pg.waitForSelector('[data-act=addExpense]');
   const shs=await pg.evaluate(()=>{ const e=S.volumes[0].expenses.find(x=>x.amount===90000); return e&&{t:e.splitType,me:e.splits.me,iv:e.splits.ivy1,inputs:JSON.stringify(e.inputs)}; });
   ok(sh.split==='shares' && sh.ins==='2,1' && shs && shs.t==='shares' && shs.me===60000 && shs.iv===30000,'D1: a shares split carries its weights over and splits the new amount exactly '+JSON.stringify([sh,shs]));
   await pg.waitForTimeout(350); await pg.evaluate(()=>openExpense(S.volumes[0].expenses.find(x=>x.id==='a'))); await pg.waitForSelector('#expForm');
   const ed=await pg.evaluate(()=>document.getElementById('descSug').innerHTML+document.getElementById('lastAmt').innerHTML);
   ok(ed==='','D1: editing an expense shows no suggestions and no "Last time"');
   ok(errs.length===0,'D1: no page errors '+JSON.stringify(errs)); await c.close(); }

 // ── All-out round · features (N5, D9, D2, D3): a couple's household volume with imported "all today" data ──
 const iso=d=>`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
 const dayAgo=n=>{ const d=new Date(); d.setDate(d.getDate()-n); return iso(d); };
 const bebe=async(dev='Pixel 7',sync=false)=>{ const c=await b.newContext({...devices[dev]}); if (!sync) await c.addInitScript(noSync); const pg=await c.newPage(); const errs=[]; pg.on('pageerror',x=>errs.push(x.message)); pg.on('console',m=>{if(m.type()==='error')errs.push(m.text())});
   await pg.goto(URL); await pg.waitForSelector('.vol');
   await pg.evaluate(()=>{ const me='me', iv='ivy1', eq=a=>splitEqual(a,[me,iv]), t=today();
     const E=(id,desc,amt,paid,cat,x={})=>({id,desc,amount:amt,paidBy:paid,category:cat,date:t,splitType:'equal',splits:eq(amt),inputs:{},note:'',upd:1,...x});
     S.volumes=[{id:'bebe',title:'Bebetime',type:'household',currency:'₱',meId:me,created:1,members:[{id:me,name:'Gery'},{id:iv,name:'Ivy'}],settlements:[{id:'p1',from:me,to:iv,amount:50000,date:t,upd:1}],expenses:[
       E('a','Chowking',31975,iv,'food'),E('b','House of Bagnet',33413,iv,'food'),E('c','Bus',43500,me,'transport'),E('d','Groceries',125050,me,'groceries'),
       E('e','Rent share',1000000,me,'lodging',{splitType:'shares',splits:splitShares(1000000,{[me]:2,[iv]:1}),inputs:{[me]:'2',[iv]:'1'}}),
       E('f','Meralco',254017,iv,'utilities'),E('g','Jollibee',41200,iv,'food'),E('h','Grab',24500,me,'transport')]}];
     commit(); render(); });
   return {c,pg,errs}; };
 // money fingerprint: everything but date and upd, plus the balances
 const money=pg=>pg.evaluate(()=>{ const v=S.volumes.find(x=>x.id==='bebe'); return JSON.stringify({e:v.expenses.map(({date,upd,...r})=>r),s:v.settlements.map(({date,upd,...r})=>r),b:balances(v)}); });

 // N5 dates: Today / Yesterday next to the expense date, and "Set date" on several History rows at once
 { const {c,pg,errs}=await bebe();
   await pg.locator('.vol').first().click(); await pg.waitForSelector('[data-act=addExpense]'); await pg.waitForTimeout(350);
   await pg.locator('[data-act=addExpense]').click(); await pg.waitForSelector('#expForm'); await pg.waitForTimeout(350);
   const q0=await pg.evaluate(()=>[...document.querySelectorAll('#expForm [data-day]')].map(b=>({t:b.textContent.trim(),p:b.getAttribute('aria-pressed'),h:Math.round(b.getBoundingClientRect().height),type:b.type})));
   ok(q0.length===2 && q0[0].t==='Today' && q0[0].p==='true' && q0[1].t==='Yesterday' && q0[1].p==='false' && q0.every(x=>x.h>=44 && x.type==='button'),'N5: the date has Today (pressed) and Yesterday buttons, 44 px tall '+JSON.stringify(q0));
   await pg.locator('#f-amount').fill('180.00'); await pg.locator('#f-desc').fill('Taho');
   await pg.getByRole('button',{name:'Yesterday',exact:true}).click();
   const q1=await pg.evaluate(()=>({d:document.getElementById('f-date').value,f:ui.form.date,p:[...document.querySelectorAll('#expForm [data-day]')].map(b=>b.getAttribute('aria-pressed')).join()}));
   ok(q1.d===dayAgo(1) && q1.f===dayAgo(1) && q1.p==='false,true','N5: one tap on Yesterday sets the date (field and form) '+JSON.stringify(q1));
   await pg.locator('#f-date').fill(dayAgo(5)); const q2=await pg.evaluate(()=>[...document.querySelectorAll('#expForm [data-day]')].map(b=>b.getAttribute('aria-pressed')).join());
   ok(q2==='false,false','N5: a picked date that is neither presses neither '+q2);
   await pg.getByRole('button',{name:'Yesterday',exact:true}).click();
   await pg.locator('#expForm button[type=submit]').click(); await pg.waitForSelector('[data-act=addExpense]');
   const sv=await pg.evaluate(()=>{ const e=S.volumes[0].expenses.find(x=>x.desc==='Taho'); return e&&e.date; });
   ok(sv===dayAgo(1),'N5 scenario: an expense dated Yesterday is saved with yesterday\'s date '+sv);
   await pg.evaluate(()=>{ const v=S.volumes[0]; v.expenses=v.expenses.filter(x=>x.desc!=='Taho'); commit(); render(); });
   // History: select 6 imported rows, give them one date
   await pg.waitForTimeout(350); await pg.locator('[data-act=history]').first().click(); await pg.waitForSelector('.ledger.hist'); await pg.waitForTimeout(350);
   const m0=await money(pg), upd0=await pg.evaluate(()=>Object.fromEntries(S.volumes[0].expenses.map(e=>[e.id,e.upd])));
   await pg.getByRole('button',{name:'Set dates',exact:true}).click();
   const sel0=await pg.evaluate(()=>({rows:[...document.querySelectorAll('.ledger.hist .tx')].map(r=>r.getAttribute('aria-pressed')),act:[...document.querySelectorAll('.ledger.hist .tx')].map(r=>r.dataset.act),focus:document.activeElement&&document.activeElement.id}));
   ok(sel0.rows.length===9 && sel0.rows.every(x=>x==='false') && sel0.act.every(a=>a==='selRow'),'N5: Set dates turns every History row into a toggle (aria-pressed), nothing opens the form '+JSON.stringify(sel0));
   const want=['a','b','c','d','f','g'];
   for (const id of want) await pg.locator(`.ledger.hist .tx[data-id="${id}"]`).click();
   const sel1=await pg.evaluate(()=>({n:[...document.querySelectorAll('.ledger.hist .tx[aria-pressed=true]')].map(r=>r.dataset.id).join(),count:document.getElementById('selCount').textContent,view:ui.view}));
   ok(sel1.n==='a,b,c,d,f,g' && /^6 selected/.test(sel1.count) && sel1.view==='history','N5: tapping rows selects them and counts them '+JSON.stringify(sel1));
   await pg.locator('.ledger.hist .tx[data-id="g"]').focus(); await pg.keyboard.press('Enter');
   const kb=await pg.evaluate(()=>({p:document.querySelector('.ledger.hist .tx[data-id="g"]').getAttribute('aria-pressed'),st:$('#status').textContent}));
   await pg.keyboard.press('Space'); await pg.waitForTimeout(60);
   const kb2=await pg.evaluate(()=>({p:document.querySelector('.ledger.hist .tx[data-id="g"]').getAttribute('aria-pressed'),st:$('#status').textContent}));
   ok(kb.p==='false' && kb2.p==='true' && /6 selected/.test(kb2.st),'N5: rows toggle from the keyboard and the count is announced in #status '+JSON.stringify([kb,kb2]));
   await pg.locator('#selApply').click(); const e0=await pg.evaluate(()=>document.getElementById('selMsg').textContent);
   ok(e0==='Pick a date first.','N5: Set date without a date says so '+e0);
   await pg.locator('#sd-date').fill('2026-10-15'); await pg.locator('#selApply').click(); await pg.waitForTimeout(100);
   const after=await pg.evaluate(()=>{ const v=S.volumes[0]; return {d:Object.fromEntries(v.expenses.map(e=>[e.id,e.date])),u:Object.fromEntries(v.expenses.map(e=>[e.id,e.upd])),toast:(document.querySelector('.toast')||{}).textContent,undo:!!document.querySelector('.toast .undo'),sel:!!ui.sel,rows:document.querySelectorAll('.ledger.hist .tx[data-act=editExpense]').length}; });
   const m1=await money(pg);
   ok(want.every(id=>after.d[id]==='2026-10-15' && after.u[id]>upd0[id]) && after.d.e===dayAgo(0) && after.d.h===dayAgo(0) && after.u.e===upd0.e,'N5: Set date gives the 6 chosen expenses the new date and a fresh upd, the others are untouched '+JSON.stringify(after.d));
   ok(m1===m0,'N5: amounts, splits, inputs and balances are byte-identical after Set date (money-parity)');
   ok(/^Dates changed on 6 expenses\./.test(after.toast||'') && after.undo && !after.sel && after.rows===8,'N5: toast "Dates changed on 6 expenses." with Undo, and History is back to normal '+JSON.stringify(after));
   await pg.reload(); await pg.waitForSelector('.ledger.hist');
   const rl=await pg.evaluate(()=>S.volumes[0].expenses.filter(e=>e.date==='2026-10-15').length);
   ok(rl===6,'N5: the new dates survive a reload '+rl);
   // Undo restores the dates (as a normal write: a fresh upd again)
   await pg.getByRole('button',{name:'Set dates',exact:true}).click();
   await pg.locator('.ledger.hist .tx[data-id="e"]').click(); await pg.locator('.ledger.hist .tx[data-id="p1"]').click();
   await pg.locator('#sd-date').fill('2026-10-20'); await pg.locator('#selApply').click(); await pg.waitForTimeout(100);
   const t2=await pg.evaluate(()=>(document.querySelector('.toast')||{}).textContent);
   const u2=await pg.evaluate(()=>S.volumes[0].expenses.find(e=>e.id==='e').upd);
   await pg.locator('.toast .undo').click(); await pg.waitForTimeout(60);
   const un=await pg.evaluate(()=>{ const v=S.volumes[0]; return {e:v.expenses.find(x=>x.id==='e'),p:v.settlements.find(x=>x.id==='p1'),toast:(document.querySelector('.toast')||{}).textContent}; });
   ok(/^Dates changed on 1 expense and 1 payment\./.test(t2||'') && un.e.date===dayAgo(0) && un.p.date===dayAgo(0) && un.e.upd>u2 && /Undone/.test(un.toast||''),'N5: Undo puts the old dates back with a fresh upd (payments too) '+JSON.stringify({t2,e:un.e.date,p:un.p.date,toast:un.toast}));
   ok(await money(pg)===m0,'N5: Undo leaves the money byte-identical');
   // Cancel leaves everything as it was
   await pg.getByRole('button',{name:'Set dates',exact:true}).click(); await pg.locator('.ledger.hist .tx[data-id="a"]').click();
   await pg.locator('#selCancel').click(); const cn=await pg.evaluate(()=>({sel:!!ui.sel,acts:[...new Set([...document.querySelectorAll('.ledger.hist .tx')].map(r=>r.dataset.act))].join(),d:S.volumes[0].expenses.find(e=>e.id==='a').date}));
   ok(!cn.sel && cn.acts==='editExpense,editSettle' && cn.d==='2026-10-15','N5: Cancel leaves select mode and changes nothing '+JSON.stringify(cn));
   const fit=await pg.evaluate(()=>{ ui.sel=new Set(); render(); const i=document.getElementById('sd-date'), r=i.getBoundingClientRect(); return {fs:getComputedStyle(i).fontSize,w:document.documentElement.scrollWidth<=innerWidth,btn:Math.min(...[...document.querySelectorAll('.setdate .btn')].map(b=>b.getBoundingClientRect().height))}; });
   ok(fit.fs==='16px' && fit.w && fit.btn>=44,'N5: the Set date bar has a 16px date input, 44px buttons, no sideways scroll '+JSON.stringify(fit));
   ok(errs.length===0,'N5: no page errors '+JSON.stringify(errs)); await c.close(); }

 // D9 paste a receipt into the amount: desktop, Android and iPhone emulation. The paste is a real ClipboardEvent with
 // clipboardData (what Live Text / Google Lens text gives the page); a plain number is left to the field.
 const paste=(pg,sel,text)=>pg.evaluate(([sel,text])=>{ const el=document.querySelector(sel); el.focus(); const dt=new DataTransfer(); dt.setData('text/plain',text);
   const go=el.dispatchEvent(new ClipboardEvent('paste',{clipboardData:dt,bubbles:true,cancelable:true}));
   return {native:go,amount:el.value,form:ui.form.amount,desc:document.getElementById('f-desc').value,msg:document.getElementById('pasteMsg').textContent,live:document.getElementById('pasteMsg').getAttribute('aria-live'),
     chips:[...document.querySelectorAll('#pasteSug button')].map(b=>({t:b.textContent,type:b.type,h:Math.round(b.getBoundingClientRect().height)}))}; },[sel,text]);
 const JOLLIBEE=['JOLLIBEE FOODS CORPORATION','SM North EDSA','1 Chickenjoy   99.00','2 Jolly Spaghetti   120.00','1 Yumburger   115.13','TOTAL   334.13','CASH   500.00','CHANGE   165.87','VAT 12%   35.80'].join('\n');
 for (const dev of ['Desktop Chrome','Pixel 7','iPhone 13']) { const {c,pg,errs}=await bebe(dev);
   await pg.locator('.vol').first().click(); await pg.waitForSelector('[data-act=addExpense]'); await pg.waitForTimeout(350);
   await pg.locator('[data-act=addExpense]').click(); await pg.waitForSelector('#expForm'); await pg.waitForTimeout(350);
   const r1=await paste(pg,'#f-amount',JOLLIBEE);
   ok(!r1.native && r1.amount==='334.13' && r1.form==='334.13' && r1.msg==='From paste: TOTAL ₱334.13' && r1.live==='polite','D9 ('+dev+'): pasting a receipt fills the amount from the TOTAL line, not CASH or CHANGE, and says so '+JSON.stringify(r1));
   ok(r1.desc==='JOLLIBEE FOODS CORPORATION','D9 ('+dev+'): an empty description takes the first text line '+r1.desc);
   if (dev!=='Pixel 7') { ok(errs.length===0,'D9 ('+dev+'): no page errors '+JSON.stringify(errs)); await c.close(); continue; }
   // the description is kept once it has words; ambiguous text gives chips (real buttons) and fills nothing until one is tapped
   await pg.locator('#f-desc').fill('Snacks'); await pg.locator('#f-amount').fill('');
   const r2=await paste(pg,'#f-amount',['Chickenjoy','Spaghetti','219.00','115.13','334.13'].join('\n'));
   ok(r2.amount==='' && r2.desc==='Snacks' && r2.chips.map(x=>x.t).join()==='₱334.13,₱219.00,₱115.13' && r2.chips.every(x=>x.type==='button' && x.h>=44) && /total/i.test(r2.msg),'D9: unclear text shows up to 3 amounts as 44px buttons, largest first, and keeps the description '+JSON.stringify(r2));
   await pg.locator('#pasteSug button').nth(1).focus(); await pg.keyboard.press('Enter');
   const r3=await pg.evaluate(()=>({a:document.getElementById('f-amount').value,f:ui.form.amount,msg:document.getElementById('pasteMsg').textContent,chips:document.querySelectorAll('#pasteSug button').length,sum:document.getElementById('splitSum').textContent}));
   ok(r3.a==='219.00' && r3.f==='219.00' && r3.msg==='From paste: ₱219.00' && r3.chips===0 && /219\.00/.test(r3.sum),'D9: a chip (by keyboard) fills the amount and the split '+JSON.stringify(r3));
   // garbage: the message, and nothing changes
   const r4=await paste(pg,'#f-amount','Thanks for dining with us!\nSee you again 11/09/2026 12:41');
   ok(!r4.native && r4.amount==='219.00' && r4.desc==='Snacks' && r4.msg==='No amount found in what you pasted.','D9: text with no amount says so and changes nothing '+JSON.stringify(r4));
   // a plain number is the field's own paste (not intercepted)
   const r5=await paste(pg,'#f-amount','1,674.72'); const r5b=await paste(pg,'#f-amount','412');
   ok(r5.native && r5b.native,'D9: a plain number paste is left to the field '+JSON.stringify([r5.native,r5b.native]));
   const r6=await paste(pg,'#f-amount','₱1,674.72'); const r7=await paste(pg,'#f-amount','₱2,000,000,000.00');
   ok(r6.amount==='1674.72' && r7.amount==='1674.72' && r7.msg==='That amount is too large.','D9: "₱1,674.72" fills 1674.72; an amount over the limit is refused '+JSON.stringify([r6.amount,r7.msg]));
   const big='x'.repeat(50000)+'\nTOTAL 99.00', t0=Date.now(); const r8=await paste(pg,'#f-amount',big);
   ok(r8.msg==='No amount found in what you pasted.' && Date.now()-t0<2000,'D9: a huge paste is read only up to 20k characters '+r8.msg);
   const r9=await paste(pg,'#f-amount','<img src=x onerror="window.__x=1">TOTAL ₱88.00');
   const x=await pg.evaluate(()=>window.__x);
   ok(r9.amount==='88.00' && x===undefined,'D9: pasted HTML is read as text '+JSON.stringify(r9));
   await pg.locator('#f-amount').fill('50.00'); const r10=await pg.evaluate(()=>document.getElementById('pasteMsg').textContent);
   ok(r10==='','D9: typing in the amount clears the paste line');
   const fs=await pg.evaluate(()=>getComputedStyle(document.getElementById('f-amount')).fontSize);
   ok(parseFloat(fs)>=16,'D9: the amount input stays at least 16px '+fs);
   ok(errs.length===0,'D9: no page errors '+JSON.stringify(errs)); await c.close(); }

 // D2 Undo on edits and deletes of expenses and payments: a real Undo button in the toast, the money back to the cent,
 // and sync-safe (a normal write with a fresh upd; a deleted entry returns under a new id, which the other phone's
 // copy, still holding the tombstone, takes in a real mergeVolumes).
 { const {c,pg,errs}=await bebe();
   // content fingerprint without ids and upd (a deleted entry comes back under a new id), plus the balances
   const content=()=>pg.evaluate(()=>{ const v=S.volumes.find(x=>x.id==='bebe'), strip=({id,upd,...r})=>JSON.stringify(r); return JSON.stringify({e:v.expenses.map(strip).sort(),s:v.settlements.map(strip).sort(),b:balances(v)}); });
   const toastOf=()=>pg.evaluate(()=>{ const t=document.querySelector('.toast'), u=t&&t.querySelector('.undo'); return t?{t:t.firstChild.textContent,undo:!!u,tag:u&&u.tagName,type:u&&u.type,hidden:t.getAttribute('aria-hidden'),st:$('#status').textContent}:null; });
   const settle=()=>pg.waitForTimeout(400);
   await pg.locator('.vol').first().click(); await pg.waitForSelector('[data-act=history]'); await settle();
   await pg.locator('[data-act=history]').first().click(); await pg.waitForSelector('.ledger.hist'); await settle();
   const c0=await content();
   // 1. edit an expense, then Undo
   const g0=await pg.evaluate(()=>({...S.volumes[0].expenses.find(e=>e.id==='g')}));
   await pg.locator('.ledger.hist .tx[data-id="g"]').click(); await pg.waitForSelector('#expForm'); await settle();
   await pg.locator('#f-amount').fill('450.00'); await pg.locator('#f-desc').fill('Jollibee Bonifacio');
   await pg.locator('#expForm button[type=submit]').click(); await pg.waitForSelector('.ledger.hist'); await settle();
   const t1=await toastOf();
   ok(t1 && t1.t==='Expense updated' && t1.undo && t1.tag==='BUTTON' && t1.type==='button' && t1.hidden===null,'D2: saving an edit shows "Expense updated" with a real Undo button '+JSON.stringify(t1));
   const u1=await pg.evaluate(()=>S.volumes[0].expenses.find(e=>e.id==='g').upd);
   await pg.locator('.toast .undo').click(); await pg.waitForTimeout(80);
   const g1=await pg.evaluate(()=>({...S.volumes[0].expenses.find(e=>e.id==='g')}));
   ok(JSON.stringify({...g1,upd:0})===JSON.stringify({...g0,upd:0}) && g1.upd>u1,'D2: Undo of an edit puts every field back, with a fresh upd '+JSON.stringify(g1));
   ok(await content()===c0,'D2: after Undo of the edit, amounts, splits and balances are as before');
   // 2. delete an expense, then Undo from the keyboard; the other phone (still on the tombstone) takes it back
   await pg.locator('.ledger.hist .tx[data-id="b"]').click(); await pg.waitForSelector('#expForm'); await settle();
   await pg.locator('[data-act=deleteExpense]').click(); await pg.waitForSelector('#sheetForm'); await pg.locator('#sheetForm button.danger').click();
   await pg.waitForSelector('.ledger.hist'); await settle();
   const t2=await toastOf(), other=await pg.evaluate(()=>JSON.stringify(S.volumes[0]));
   ok(t2 && t2.t==='Expense deleted' && t2.undo && /Expense deleted/.test(t2.st),'D2: deleting an expense shows "Expense deleted" with Undo, announced in #status '+JSON.stringify(t2));
   await pg.locator('.toast .undo').focus(); await pg.keyboard.press('Enter'); await pg.waitForTimeout(80);
   const b1=await pg.evaluate(()=>{ const v=S.volumes[0], e=v.expenses.find(x=>x.desc==='House of Bagnet'); return {id:e&&e.id,amount:e&&e.amount,gone:v.deleted.includes('b'),row:!!document.querySelector(`.ledger.hist .tx[data-id="${e&&e.id}"]`),toast:(document.querySelector('.toast')||{}).textContent}; });
   ok(b1.id && b1.id!=='b' && b1.amount===33413 && b1.gone && b1.row && /Undone/.test(b1.toast||''),'D2: Undo (keyboard) brings the deleted expense back under a new id, the old id stays deleted '+JSON.stringify(b1));
   ok(await content()===c0,'D2: after Undo of the delete, the money is as before to the cent');
   const mg=await pg.evaluate(o=>{ const v=S.volumes[0], a=mergeVolumes(JSON.parse(o),v), back=mergeVolumes(v,JSON.parse(o));
     return {a:a.vol&&a.vol.expenses.some(e=>e.desc==='House of Bagnet'),b:back.vol&&back.vol.expenses.some(e=>e.desc==='House of Bagnet'),sum:a.vol&&JSON.stringify(balances(a.vol))===JSON.stringify(balances(v))}; },other);
   ok(mg.a && mg.b && mg.sum,'D2: sync-safe: the other phone\'s copy (with the tombstone) merges the restored expense in, both ways '+JSON.stringify(mg));
   // 3. record a new payment, then Undo
   await pg.locator('#app .topbar [data-act=back]').click(); await pg.waitForSelector('[data-act=settle]'); await settle();
   await pg.locator('[data-act=settle]').first().click(); await pg.waitForSelector('#sheetForm'); await pg.locator('#s-amt').fill('100.00');
   await pg.locator('#sheetForm button.primary').click(); await settle();
   const t3=await toastOf(), n3=await pg.evaluate(()=>S.volumes[0].settlements.length);
   await pg.locator('.toast .undo').click(); await pg.waitForTimeout(80);
   const p3=await pg.evaluate(()=>({n:S.volumes[0].settlements.length,del:S.volumes[0].deleted.length}));
   ok(t3 && t3.t==='Payment recorded' && t3.undo && n3===2 && p3.n===1,'D2: Undo of a new payment takes it away (and tombstones it) '+JSON.stringify([t3,n3,p3]));
   ok(await content()===c0,'D2: after Undo of the payment, balances are as before');
   // 4. edit a payment, then Undo; 5. delete it, then Undo
   await pg.locator('[data-act=history]').first().click(); await pg.waitForSelector('.ledger.hist'); await settle();
   await pg.locator('.ledger.hist .tx[data-id="p1"]').click(); await pg.waitForSelector('#sheetForm'); await pg.locator('#s-amt').fill('321.00');
   await pg.locator('#sheetForm button.primary').click(); await settle();
   await pg.locator('.toast .undo').click(); await pg.waitForTimeout(80);
   const p4=await pg.evaluate(()=>S.volumes[0].settlements.find(s=>s.id==='p1'));
   ok(p4 && p4.amount===50000 && p4.upd>1,'D2: Undo of a payment edit puts the old amount back with a fresh upd '+JSON.stringify(p4));
   await settle(); await pg.locator('.ledger.hist .tx[data-id="p1"]').click(); await pg.waitForSelector('#sheetForm');
   await pg.locator('[data-act=deleteSettle]').click(); await pg.waitForSelector('#sheetForm button.danger'); await pg.locator('#sheetForm button.danger').click(); await settle();
   const t5=await toastOf(); await pg.locator('.toast .undo').click(); await pg.waitForTimeout(80);
   const p5=await pg.evaluate(()=>{ const v=S.volumes[0]; return {n:v.settlements.length,id:v.settlements[0]&&v.settlements[0].id,amt:v.settlements[0]&&v.settlements[0].amount,gone:v.deleted.includes('p1')}; });
   ok(t5 && t5.t==='Payment deleted' && t5.undo && p5.n===1 && p5.id!=='p1' && p5.amt===50000 && p5.gone,'D2: Undo of a payment delete brings it back under a new id '+JSON.stringify([t5,p5]));
   ok(await content()===c0,'D2: after all the undos the volume\'s money is byte-identical to the start');
   await pg.reload(); await pg.waitForSelector('.ledger.hist');
   ok(await content()===c0,'D2: and it survives a reload');
   ok(errs.length===0,'D2: no page errors '+JSON.stringify(errs)); await c.close(); }

 // D3 payments as they happened: a date (Today / Yesterday) and an optional "How" on the settle sheet; `how` survives
 // cleanVolume, mergeVolumes (also against an older phone that drops it) and validVolume, and never touches the money.
 { const {c,pg,errs}=await bebe('iPhone 13');
   await pg.locator('.vol').first().click(); await pg.waitForSelector('[data-act=settle]'); await pg.waitForTimeout(400);
   let taps=0; await pg.locator('[data-act=settle]').first().click(); taps++; await pg.waitForSelector('#s-date');
   const f0=await pg.evaluate(()=>({d:$('#s-date').value,type:$('#s-date').type,q:[...document.querySelectorAll('#sheetForm [data-day]')].map(b=>b.textContent+':'+b.getAttribute('aria-pressed')+':'+(b.getBoundingClientRect().height>=44)),
     how:{label:document.querySelector('label[for=s-via]').textContent,ph:$('#s-via').placeholder,max:$('#s-via').maxLength,fs:getComputedStyle($('#s-via')).fontSize},dfs:getComputedStyle($('#s-date')).fontSize}));
   ok(f0.d===dayAgo(0) && f0.type==='date' && f0.q.join()==='Today:true:true,Yesterday:false:true' && f0.dfs==='16px','D3: the settle sheet has a native date field set to today, with Today (pressed) and Yesterday '+JSON.stringify(f0));
   ok(f0.how.label==='How (optional)' && f0.how.ph==='GCash, cash' && f0.how.max===40 && f0.how.fs==='16px','D3: "How (optional)", placeholder "GCash, cash", up to 40 characters, 16px '+JSON.stringify(f0.how));
   await pg.getByRole('button',{name:'Yesterday',exact:true}).click(); taps++;
   await pg.locator('#s-via').fill('GCash'); taps++;
   await pg.locator('#sheetForm button.primary').click(); taps++; await pg.waitForTimeout(400);
   const p1=await pg.evaluate(()=>{ const v=S.volumes[0]; return v.settlements.find(s=>s.id!=='p1'); });
   ok(p1 && p1.date===dayAgo(1) && p1.how==='GCash' && taps<=4,'D3 scenario: yesterday\'s GCash payment is recorded with its date and how, in '+taps+' taps '+JSON.stringify(p1));
   await pg.locator('[data-act=history]').first().click(); await pg.waitForSelector('.ledger.hist'); await pg.waitForTimeout(400);
   const row=await pg.evaluate(id=>{ const r=document.querySelector(`.ledger.hist .tx[data-id="${id}"]`); return r&&{m:r.querySelector('.tx-m').textContent,d:r.querySelector('.date').textContent}; },p1.id);
   ok(row && row.m==='Payment · via GCash' && row.d==='Yesterday','D3: History shows "via GCash" and the date '+JSON.stringify(row));
   // edit: the sheet opens on the payment's own date and how; clearing How removes it, the date stays
   await pg.locator(`.ledger.hist .tx[data-id="${p1.id}"]`).click(); await pg.waitForSelector('#s-via');
   const e0=await pg.evaluate(()=>({d:$('#s-date').value,h:$('#s-via').value}));
   await pg.locator('#s-via').fill(''); await pg.locator('#sheetForm button.primary').click(); await pg.waitForTimeout(400);
   const e1=await pg.evaluate(id=>S.volumes[0].settlements.find(s=>s.id===id),p1.id);
   ok(e0.d===dayAgo(1) && e0.h==='GCash' && e1.date===dayAgo(1) && !('how' in e1),'D3: editing keeps the date and an emptied How is removed '+JSON.stringify([e0,e1]));
   // the data rules, with the real functions
   const dr=await pg.evaluate(()=>{ const v=S.volumes[0], s=v.settlements[0]; s.how='GCash'; s.upd=5; const b0=JSON.stringify(balances(v));
     const cl=cleanVolume(JSON.parse(JSON.stringify(v))), long=cleanVolume({...v,settlements:[{...s,how:'x'.repeat(41)}]}), bad=cleanVolume({...v,settlements:[{...s,how:42}]});
     const older={...JSON.parse(JSON.stringify(v)),settlements:v.settlements.map(({how,...r})=>r)}; // a phone that doesn't know `how`, same upd
     const m1=mergeVolumes(v,older), m2=mergeVolumes(older,v), newer={...older,settlements:older.settlements.map(r=>({...r,upd:9}))}, m3=mergeVolumes(v,newer);
     const noHow={...v,settlements:v.settlements.map(({how,...r})=>r)};
     return {valid:validVolume(v),clean:cl.settlements[0].how,long:'how' in long.settlements[0],bad:'how' in bad.settlements[0],
       m1:m1.vol.settlements[0].how,m2:m2.vol.settlements[0].how,m3:'how' in m3.vol.settlements[0],
       money:JSON.stringify(balances(noHow))===b0 && JSON.stringify(debts(noHow))===JSON.stringify(debts(v)),code:JSON.parse(shareCodeOf(v)).volume.settlements[0].how,canon:canon(v)!==canon(noHow)}; });
   ok(dr.valid && dr.clean==='GCash' && !dr.long && !dr.bad,'D3: validVolume accepts a payment with how; cleanVolume keeps it, drops one over 40 characters or not text '+JSON.stringify(dr));
   ok(dr.m1==='GCash' && dr.m2==='GCash' && !dr.m3,'D3: mergeVolumes keeps how when an older phone sends the payment back without it at the same upd, and takes a newer edit '+JSON.stringify(dr));
   ok(dr.money && dr.code==='GCash' && dr.canon,'D3: how never changes balances or debts, travels in share codes and backups, and counts as a change for sync '+JSON.stringify(dr));
   await pg.evaluate(()=>{ S.volumes[0].settlements[0].how='<img src=x onerror="window.__h=1">'; commit(); render(); });
   await pg.reload(); await pg.waitForSelector('.ledger.hist');
   const hx=await pg.evaluate(()=>({h:window.__h,t:document.querySelector('.ledger.hist .tx[data-act=editSettle] .tx-m').textContent,kept:S.volumes[0].settlements[0].how}));
   ok(hx.h===undefined && /via <img/.test(hx.t) && /^<img/.test(hx.kept),'D3: how survives a reload and shows as text '+JSON.stringify(hx));
   ok(errs.length===0,'D3: no page errors '+JSON.stringify(errs)); await c.close(); }

 // NEXT round (owner 2026-11-12): X1 At a glance, X2 month totals, X3 search/filter, X4 balanced Paid by, X5 payment
 // methods, X6 Copy as new. Real functions, integer cents.
 { const c=await b.newContext({...devices['Pixel 7']}); await c.addInitScript(noSync); const pg=await c.newPage(); const errs=[]; pg.on('pageerror',x=>errs.push(x.message)); pg.on('console',m=>{if(m.type()==='error')errs.push(m.text())});
   await pg.goto(URL); await pg.waitForSelector('.vol');
   const seed=await pg.evaluate(()=>{ const me='me', ids=['ivy','bo','cy','di','ed','fr'], all=[me,...ids];
     const now=today(), [Y,M]=now.split('-').map(Number), prev=M===1?`${Y-1}-12`:`${Y}-${pad2(M-1)}`;
     const cats=['food','groceries','utilities','lodging','transport','drinks','health'], ex=[];
     for (let i=0;i<24;i++) { const amt=1001+i*137, d=(i%2?prev:now.slice(0,7))+'-'+pad2(1+(i%9)); ex.push({id:'x'+i,desc:['Rent','Groceries','Meralco','Water','Grab','Coffee','Meds'][i%7]+' '+i,amount:amt,paidBy:all[i%7],category:cats[i%7],date:d,splitType:'equal',splits:splitEqual(amt,all),inputs:{},note:i===5?'GCash promo':'',upd:1}); }
     const v={id:'nx',title:'Bebetime',type:'household',currency:'₱',meId:me,created:Date.now(),members:all.map((id,k)=>({id,name:k?['Ivy','Bo','Cy','Di','Ed','Francesca'][k-1]:'Al'})),expenses:ex,settlements:[{id:'s1',from:'bo',to:'me',amount:500,date:now,upd:1,how:'GCash'}]};
     S.volumes.unshift(v); save(); ui.volId=v.id; go('volume'); return {now,prev}; });
   await pg.waitForTimeout(300);
   // X1
   const g=await pg.evaluate(()=>{ const v=vol(), g=glance(v), b=balances(v);
     const pay=id=>v.settlements.reduce((a,s)=>a+(s.from===id?s.amount:0)-(s.to===id?s.amount:0),0);
     return {panel:!!document.querySelector('.glance'),after:!!document.querySelector('.sect:has(.debt) + .sect .glance'),old:[...document.querySelectorAll('h2')].some(h=>/Spending by category/.test(h.textContent)),
       parity:g.who.every(r=>r.diff===(b[r.id]||0)-pay(r.id)), cats:g.cats.reduce((a,c)=>a+c[1],0)===g.tot, tot:g.tot===total(v), months:g.time.cols.reduce((a,c)=>a+c.a,0)===g.tot,
       ints:[g.tot,...g.who.flatMap(r=>[r.paid,r.share,r.diff]),...g.cats.map(c=>c[1]),...g.time.cols.map(c=>c.a)].every(Number.isInteger),
       top:g.who[0].paid===Math.max(...g.who.map(r=>r.paid)), stamp:document.querySelectorAll('.glance .stamp-s').length, catLines:g.cats.length,
       say:document.querySelector('.glance .gl-say').textContent}; });
   ok(g.panel && g.after && !g.old,'X1: "At a glance" sits right after "Who pays whom" and replaces the bottom "Spending by category" '+JSON.stringify(g));
   ok(g.parity && g.cats && g.tot && g.months && g.ints,'X1: money: each "Who paid" difference = that member\'s balance before payments; category lines and this/last month sum to the total; all integer cents '+JSON.stringify(g));
   ok(g.top && g.stamp===1 && g.catLines===6 && /^This month ₱/.test(g.say),'X1: the top payer is first with one "Paid most" stamp; top 5 categories + "Everything else"; a household says this month vs last '+JSON.stringify(g));
   const trip=await pg.evaluate(()=>{ const v=exampleVolume(), g=glance(v); return {kind:g.time.kind,sum:g.time.cols.reduce((a,c)=>a+c.a,0)===g.tot,n:g.time.cols.length}; });
   ok(trip.kind==='days' && trip.sum,'X1: a trip shows one column per day and the days sum to the total '+JSON.stringify(trip));
   // X2
   await pg.evaluate(()=>go('history')); await pg.waitForSelector('.ledger.hist'); await pg.waitForTimeout(300);
   const m=await pg.evaluate(()=>{ const v=vol(), want={}; v.expenses.forEach(e=>{ const k=e.date.slice(0,7); want[k]=(want[k]||0)+e.amount; });
     return [...document.querySelectorAll('.sect.month')].map(s=>({h:s.querySelector('h2').textContent,t:s.querySelector('.month-tot')&&s.querySelector('.month-tot').firstChild.textContent,ok:false,want:null})).map((r,i)=>{ const k=Object.keys(want).sort().reverse()[i]; return {...r,ok:r.t===fmt(want[k],v.currency)}; }); });
   ok(m.length===2 && m.every(r=>r.ok),'X2: every month heading carries that month\'s expense total (payments excluded) '+JSON.stringify(m));
   // X3
   const f0=await pg.evaluate(()=>({bar:!!$('#h-q'),role:$('.hfilter').getAttribute('role')}));
   await pg.fill('#h-q','rent'); await pg.waitForTimeout(150);
   const f1=await pg.evaluate(()=>({n:document.querySelectorAll('.ledger.hist .tx').length,c:$('#histCount').textContent,focus:document.activeElement.id,live:$('#status').textContent}));
   await pg.waitForTimeout(100); const live1=await pg.evaluate(()=>$('#status').textContent);
   await pg.fill('#h-q','gcash'); await pg.waitForTimeout(150);
   const f2=await pg.evaluate(()=>[...document.querySelectorAll('.ledger.hist .tx')].map(r=>r.dataset.id));
   await pg.fill('#h-q',''); await pick(pg,'h-cat',{label:'Groceries'}); await pg.waitForTimeout(150);
   const f3=await pg.evaluate(()=>({n:document.querySelectorAll('.ledger.hist .tx').length,all:vol().expenses.filter(e=>e.category==='groceries').length,tot:document.querySelector('.month-tot')&&document.querySelector('.month-tot').firstChild.textContent}));
   await pick(pg,'h-cat',{label:'All'}); await pick(pg,'h-who',{label:'Bo'}); await pg.waitForTimeout(150);
   const f4=await pg.evaluate(()=>({n:document.querySelectorAll('.ledger.hist .tx').length,want:vol().expenses.filter(e=>e.paidBy==='bo'||has(e.splits,'bo')).length+vol().settlements.filter(s=>s.from==='bo'||s.to==='bo').length}));
   await pg.getByRole('button',{name:'Clear'}).click(); await pg.waitForTimeout(150);
   const f5=await pg.evaluate(()=>({n:document.querySelectorAll('.ledger.hist .tx').length,q:$('#h-q').value,clear:$('#histClear').hidden}));
   ok(f0.bar && f0.role==='search','X3: History over 20 items has a search region '+JSON.stringify(f0));
   ok(f1.n===7 && f1.c==='7 matches' && f1.focus==='h-q' && /7 matches/.test(live1),'X3: words match description and category; the count shows and is announced; focus stays in the field '+JSON.stringify([f1,live1]));
   ok(f2.join()==='s1,x5','X3: "gcash" finds the payment by its method and the expense by its note '+JSON.stringify(f2));
   ok(f3.n===f3.all && f3.n>0,'X3: the Category filter keeps only that category (payments drop out) '+JSON.stringify(f3));
   ok(f4.n===f4.want && f4.n>0,'X3: the Who filter keeps what that member paid or shares, and their payments '+JSON.stringify(f4));
   ok(f5.n===25 && f5.q==='' && f5.clear,'X3: Clear brings every item back '+JSON.stringify(f5));
   const small=await pg.evaluate(()=>{ ui.volId=S.volumes.find(v=>/example/.test(v.title)).id; go('history'); return !!$('#h-q'); });
   ok(!small,'X3: a short history (20 or fewer) has no search bar '+small);
   // X4: balanced rows for 1..8 people; one tap; arrows across rows
   const rows=await pg.evaluate(()=>[1,2,3,4,5,6,7,8].map(n=>pickRows(n).join('+')));
   ok(rows.join()==='1,2,3,4,3+2,3+3,4+3,4+4','X4: rows are at most 4 and as even as possible '+rows.join());
   await pg.evaluate(()=>{ ui.volId='nx'; go('volume'); openExpense(null); }); await pg.waitForTimeout(200);
   const geo=await pg.evaluate(()=>{ const rs=[...document.querySelectorAll('#f-paid .pk-row')].map(r=>r.getBoundingClientRect()), bs=[...document.querySelectorAll('#f-paid .pkg')].map(x=>x.getBoundingClientRect()), d=$('#f-desc').getBoundingClientRect();
     const byRow=rs.map(r=>bs.filter(x=>Math.abs(x.top-r.top)<4||Math.abs(x.top-r.top-3)<4));
     return {rows:byRow.map(x=>x.length),edges:rs.every(r=>Math.abs(r.left-d.left)<1&&Math.abs(r.right-d.right)<1),equal:byRow.every(x=>Math.max(...x.map(y=>y.width))-Math.min(...x.map(y=>y.width))<1),fill:byRow.every((x,i)=>Math.abs(x[x.length-1].right-rs[i].right)<4)}; });
   ok(geo.rows.join('+')==='4+3' && geo.edges && geo.equal && geo.fill,'X4: 7 people = rows of 4 and 3, equal widths per row, each row fills the field width '+JSON.stringify(geo));
   const t1=await pick(pg,'f-paid',{label:'Francesca'});
   await pg.getByRole('radio',{name:'Francesca'}).focus(); await pg.keyboard.press('ArrowRight');
   const k=await pg.evaluate(()=>({f:document.activeElement.textContent,v:ui.form.paidBy}));
   ok(t1===1 && k.f==='You' && k.v==='me','X4: any payer is one tap; arrows walk across rows and wrap '+JSON.stringify([t1,k]));
   // X6 Copy as new
   await pg.evaluate(()=>{ go('volume'); openExpense(vol().expenses.find(e=>e.id==='x3')); }); await pg.waitForTimeout(200);
   await pg.getByRole('button',{name:'Copy as new'}).click(); await pg.waitForTimeout(200);
   const cp=await pg.evaluate(()=>({title:document.querySelector('.topbar h1').textContent,f:{...ui.form},focus:document.activeElement.id}));
   await pg.getByRole('button',{name:'Save expense'}).click(); await pg.waitForTimeout(300);
   const cs=await pg.evaluate(()=>{ const v=vol(), o=v.expenses.find(e=>e.id==='x3'), n=v.expenses.filter(e=>e.desc===o.desc); return {n:n.length,same:n.every(e=>e.amount===o.amount&&e.paidBy===o.paidBy&&e.category===o.category&&JSON.stringify(e.splits)===JSON.stringify(o.splits)),date:n.find(e=>e.id!=='x3').date,orig:o.date}; });
   ok(cp.title==='New expense' && !cp.f.id && cp.f.date===seed.now && cp.focus==='f-amount','X6: Copy as new opens a new expense with the same fields, dated today, focus on the amount '+JSON.stringify(cp));
   ok(cs.n===2 && cs.same && cs.date===seed.now && cs.orig!==seed.now,'X6: saving it adds a second expense with the same amount, payer, category and split; the original is untouched '+JSON.stringify(cs));
   // X5 methods
   await pg.evaluate(()=>{ go('volume'); settleSheet(null); }); await pg.waitForSelector('#s-via');
   await pg.getByRole('button',{name:'GCash',exact:true}).click();
   const h1=await pg.evaluate(()=>({v:$('#s-via').value,p:[...document.querySelectorAll('[data-how]')].map(b=>b.getAttribute('aria-pressed')).join()}));
   await pg.getByRole('button',{name:'GCash',exact:true}).click();
   const h2=await pg.evaluate(()=>$('#s-via').value);
   await pg.getByRole('button',{name:'Cash',exact:true}).click(); await pg.locator('#s-via').fill('Cash ref 77');
   const h3=await pg.evaluate(()=>[...document.querySelectorAll('[data-how]')].map(b=>b.getAttribute('aria-pressed')).join());
   await pg.locator('#sheetForm button.primary').click(); await pg.waitForTimeout(300);
   const h4=await pg.evaluate(()=>vol().settlements.slice(-1)[0].how);
   ok(h1.v==='GCash' && h1.p==='false,true,false' && h2==='' && h3==='false,false,false' && h4==='Cash ref 77','X5: Cash / GCash / Bank fill "How" in one tap (tap again clears); typed words still save as typed '+JSON.stringify([h1,h2,h3,h4]));
   ok(errs.length===0,'NEXT: no page errors '+JSON.stringify(errs)); await c.close(); }

 await b.close();
})();
