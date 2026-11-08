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
 await p.locator('#nav [data-act=home]').click(); await p.locator('.vol').first().click();
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
 await p.locator('#nav [data-act=home]').click(); await p.getByRole('button',{name:'Import'}).click();
 const evil=JSON.parse(code); evil.volume.id='q"><img src=x onerror=window.__x=1>'; evil.volume.members[1].id='a"><b>';
 await p.fill('#s-paste',JSON.stringify(evil)); await p.locator('#s-go').click();
 ok((await p.locator('#sheetMsg').innerText()).length>0 && !(await p.evaluate(()=>window.__x)),'malicious share code is rejected');
 await p.fill('#s-paste',code); await p.locator('#s-go').click();
 await p.selectOption('#s-mode','copy'); await p.selectOption('#s-who',{label:'Mira'}); await p.waitForTimeout(600); await p.locator('#s-go').click();
 ok(await p.evaluate(()=>S.volumes.length)===2,'share code imported as a second copy');
 ok(await p.evaluate(()=>{const v=S.volumes.find(x=>x.id===ui.volId);return nameOf(v,v.meId)})==='Mira','importer is Mira in the copy');
 await p.reload(); ok(await p.locator('h1').count()===1,'data survives reload');
 // themes
 await p.locator('#nav [data-act=settings]').click(); await p.locator('[data-pal=persimmon]').click(); await p.locator('[data-set=mode][data-val=dark]').click();
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
 await p.locator('#nav [data-act=settings]').click(); await p.locator('[data-pal=lagoon]').click(); await p.locator('[data-set=mode][data-val=light]').click();
 await p.locator('#nav [data-act=home]').click(); await p.screenshot({path:process.argv[2]+'/after-light-hub.png'});
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
   await d.locator('[data-act=closeSheet]').click(); await d.locator('#nav [data-act=home]').click(); await d.getByRole('button',{name:'Import'}).click();
   const ph=await d.locator('#s-paste').getAttribute('placeholder'); ok(!/[{}"]/.test(ph),'import placeholder has no JSON: '+ph);
   ok(await d.getByText('Open file').isVisible(),'file import is a labeled button, not a bare file input');
   await d.locator('[data-act=closeSheet]').click();
   await d.locator('#nav [data-act=settings]').click();
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

 // R1-1/R1-2/R1-3 (code): merge on import, member edit, native share, backup
 { const fresh=async()=>{ const c=await b.newContext({...devices['iPhone 13'],acceptDownloads:true}); await c.addInitScript(noSync); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{if(m.type()==='error')e.push(m.text())}); await pg.goto(URL); return {c,pg,e}; };
   const home=async pg=>{ if(await pg.evaluate(()=>sheetOpen())) await pg.keyboard.press('Escape'); await pg.locator('#nav [data-act=home]').click(); };
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
   await check(B.pg,codeA0); await B.pg.selectOption('#s-who',{label:'Mira'}); await doImport(B.pg);
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
   ok(await A.pg.locator('#s-mode').inputValue()==='merge' && !(await A.pg.locator('#s-who').isVisible()),'existing volume: Merge is the default and the member picker is hidden');
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
   await C.pg.selectOption('#s-who',{label:'Rio'}); await doImport(C.pg);
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
   const D=await fresh(); await check(D.pg,sent); await D.pg.selectOption('#s-who',{label:'Alex'}); await doImport(D.pg);
   ok(await D.pg.evaluate(id=>ui.volId===id,volId) && await zs(D.pg),'the link passed to navigator.share, pasted into Import on another phone, imports '+JSON.stringify([String(sent).slice(0,60),await D.pg.evaluate(()=>[ui.view,ui.volId,$('#sheetMsg')&&$('#sheetMsg').textContent])]));
   // backup: download, restore on a fresh phone and onto a phone that already has one volume
   await home(A.pg); await A.pg.locator('#nav [data-act=settings]').click();
   const [dl]=await Promise.all([A.pg.waitForEvent('download'),A.pg.locator('[data-act=backup]').click()]);
   ok(/^hati2-backup-\d{4}-\d{2}-\d{2}\.json$/.test(dl.suggestedFilename()),'Save backup downloads '+dl.suggestedFilename());
   const file=await dl.path(); const nA=await A.pg.evaluate(()=>S.volumes.length);
   const E=await fresh(); await E.pg.evaluate(()=>{S.volumes=[];save();}); await E.pg.reload();
   await home(E.pg); await E.pg.locator('#nav [data-act=settings]').click(); await E.pg.locator('[data-act=restore]').click(); await E.pg.setInputFiles('#s-file',file); await E.pg.waitForTimeout(100); await E.pg.locator('#s-go').click();
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
     await D.pg.selectOption('#s-mode','replace'); const rs=await sum();
     ok(/1 entry only on this phone will be lost\./.test(rs),'Replace selected: summary counts what will be lost ('+rs+')');
     await D.pg.selectOption('#s-mode','copy'); ok(/Adds a second copy\./.test(await sum()),'Keep both selected: summary says it adds a second copy');
     await D.pg.selectOption('#s-mode','merge'); ok(/Your copy already has everything|Adds|Updates|Removes/.test(await sum()) && !/lost/.test(await sum()),'Merge selected: summary is the merge result again');
     await D.pg.selectOption('#s-mode','replace'); await doImport(D.pg);
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
     ok(!/Backup saved/.test(await toastText(G.pg)),'iOS: cancelling the share sheet does not claim the backup was saved'); }
   // Ponytail R1: Replace counts a newer local edit and a member only on this phone; volume-only edits get their own line
   { const R=await fresh(); all.push(R);
     const code=await R.pg.evaluate(()=>JSON.stringify({app:'hati2-mock',v:1,volume:S.volumes[0]}));
     await R.pg.evaluate(()=>{ const v=S.volumes[0]; v.expenses[0].desc='LOCAL EDIT'; v.expenses[0].upd=Date.now()+5; v.members.push({id:'onlyhere1',name:'OnlyHere',upd:Date.now()}); save(); render(); });
     await importCode(R.pg,code); await R.pg.selectOption('#s-mode','replace'); const rs=await R.pg.locator('#s-sum').innerText();
     ok(/1 entry and 1 member only on this phone will be lost\./.test(rs),'Replace: a newer local edit and a member only on this phone are counted as lost ('+rs+')');
     await R.pg.evaluate(()=>{ dropSheet(); const v=S.volumes[0]; v.members=v.members.filter(m=>m.id!=='onlyhere1'); save(); });
     const code2=await R.pg.evaluate(()=>{ const v=S.volumes[0]; const c=JSON.parse(JSON.stringify(v)); v.title='Renamed here'; v.upd=Date.now()+10; save(); render(); return JSON.stringify({app:'hati2-mock',v:1,volume:c}); });
     await importCode(R.pg,code2); await R.pg.selectOption('#s-mode','replace'); const rs2=await R.pg.locator('#s-sum').innerText();
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
   const CLAY_SET='.btn:not(.quiet):not(.text), .ico, .cat, .seg button, .vol, button.chip:not(.add), .nav [aria-current]';
   for (const id of ids) for (const mode of ['light','dark']) {
     const set=async v=>pg.evaluate(([id,mode,v])=>{ S.settings.palette=id; S.settings.mode=mode; applyTheme(); dropSheet&&dropSheet(); if(v==='expense'){ ui.volId=S.volumes[0].id; ui.view='volume'; openExpense(null); } else { if(v==='volume') ui.volId=S.volumes[0].id; go(v); } },[id,mode,v]);
     const want=async(sel,clay,tag)=>{ const s=await st(pg,sel); if(!s) { bad.push(`${id}/${mode} ${tag||sel}: missing`); return; } if(clay? !isClay(s) : (/inset/.test(s.boxShadow)||s.borderTopLeftRadius!==(tag==='sheet'?'6px':'3px'))) bad.push(`${id}/${mode} ${tag||sel}: ${s.borderTopWidth} r${s.borderTopLeftRadius} ${s.boxShadow}`); };
     await set('hub'); await want('.vol',true); await want('.nav [aria-current]',true);
     await set('volume'); await want('.btn.primary',true); await want('.ico',true); await want('button.chip:not(.add)',true); await want('.ledger',false); await want('.hero',false);
     await want('.chip.add',false,'chip.add (flat, 3px)');
     await set('expense'); await want('.cat[aria-pressed="false"]',true); await want('.cat[aria-pressed="true"]',true); await want('.input',false);
     const pc=await st(pg,'.cat[aria-pressed="true"]'); if(!/^0px 0px 0px/.test(nums(offLayer(pc.boxShadow)).slice(0,3).map(x=>x+'px').join(' '))) bad.push(`${id}/${mode} selected cat not pushed in: ${pc.boxShadow}`);
     await set('settings'); await want('.panel',false); await want('.pal',false,'pal (flat swatch)');
     const pal=await st(pg,'.pal'); if(/inset/.test(pal.boxShadow)) bad.push(`${id}/${mode} .pal has inset`);
     await pg.evaluate(()=>{ ui.volId=S.volumes[0].id; go('volume'); volMenuSheet(); }); await want('.sheet',false,'sheet');
     const grad=await pg.evaluate(sel=>[...document.querySelectorAll(sel)].filter(x=>getComputedStyle(x).backgroundImage!=='none').map(x=>x.className),CLAY_SET); if(grad.length) bad.push(`${id}/${mode} gradient on clay: ${grad}`);
     await pg.evaluate(()=>dropSheet()); }
   ok(bad.length===0,'clay: clay set has 2px ink, 6-10px corners, inset light/shade and a 0-blur offset; reading surfaces stay flat 3px; no gradients; .pal and .chip.add flat, in 13 palettes x light/dark '+JSON.stringify(bad.slice(0,4)));
   // Disabled clay is flat
   await pg.evaluate(()=>{ S.settings.palette='classic'; S.settings.mode='light'; applyTheme(); go('hub'); $('#app').insertAdjacentHTML('beforeend','<button class="btn primary" id="__dis" disabled>x</button>'); });
   ok((await st(pg,'#__dis')).boxShadow==='none','clay: a disabled .btn has no shadow at all');
   // Pressed: squash (normal motion) and squash shading without movement (reduced motion)
   const press=async(P,sel)=>{ await P.evaluate(sel=>document.querySelector(sel).scrollIntoView({block:'center'}),sel); const r=await P.locator(sel).first().boundingBox(); const rest=await st(P,sel); await P.mouse.move(r.x+r.width/2,r.y+r.height/2); await P.mouse.down(); await P.waitForTimeout(150); const held=await st(P,sel); await P.mouse.move(1,1); await P.mouse.up(); return {rest,held}; };
   const shade=s=>{ const ins=layers(s).filter(l=>/inset/.test(l)); return ins.reduce((a,l)=>a+Math.abs(nums(l)[2]||0),0); };
   for (const [sel,view] of [['.btn.primary','volume'],['.vol','hub']]) {
     await pg.evaluate(v=>{ ui.volId=S.volumes[0].id; go(v); },view);
     const {rest,held}=await press(pg,sel); const m=/matrix\(1, 0, 0, 1, ([\d.]+), ([\d.]+)\)/.exec(held.transform)||[];
     ok(+m[1]>=2 && +m[1]<=3 && nums(offLayer(held.boxShadow)).slice(0,3).every(x=>x===0) && shade(held.boxShadow)>shade(rest.boxShadow),`clay: held ${sel} moves 2-3px, loses its offset and its inner shade grows (${held.transform}; ${held.boxShadow})`); }
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
       const px=await pg.evaluate(async b64=>{ const im=new Image(); im.src='data:image/png;base64,'+b64; await im.decode(); const cv=document.createElement('canvas'); cv.width=im.width; cv.height=im.height; const x=cv.getContext('2d'); x.drawImage(im,0,0); const d=x.getImageData(0,0,cv.width,cv.height).data; const at=(i,j)=>{const k=(j*cv.width+i)*4; return [d[k],d[k+1],d[k+2]];}; let min=at(cv.width-6,cv.height-6); const fill=min; for(let j=0;j<cv.height;j++) for(let i=0;i<cv.width;i++){ if((i>=12&&j>=12)||((i<8||i>=cv.width-8)&&(j<8||j>=cv.height-8))) continue; /* the container's rounded corner is its frame, not shade */ const v=at(i,j); if(v[0]+v[1]+v[2]<min[0]+min[1]+min[2]) min=v; } return {fill,min}; },buf.toString('base64'));
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
   const lt=await pg.evaluate(()=>{ S.settings.palette='classic'; S.settings.mode='light'; applyTheme(); ui.volId=S.volumes[0].id; go('volume'); settleSheet(null); const r=[getComputedStyle($('.scrim')).backgroundColor,getComputedStyle($('.sheet')).backgroundColor,getComputedStyle(document.body).backgroundColor,getComputedStyle($('.hero')).boxShadow,document.querySelector('meta[name=theme-color]').content]; dropSheet(); return r; });
   ok(lt[0]==='rgba(10, 10, 14, 0.5)' && lt[1]===lt[2] && /rgb\(0, 0, 0\) 4px 4px 0px/.test(lt[3]) && lt[4]==='#FFFFFF','L3-dark: light scrim, sheet, hero offset and theme-color unchanged '+JSON.stringify(lt));
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
   const j=await B.pg.evaluate(()=>({h:$('#sheetTitle').textContent,sum:$('#sheetForm .share-sum').innerText,opts:[...$('#s-who').options].map(o=>o.textContent),sel:$('#s-who').value,isNew:!!$('#s-new'),hash:location.hash,n:S.volumes.length}));
   ok(j.h==='Join volume' && /Tokyo Trip \(example\)/.test(j.sum) && /3 members/.test(j.sum) && ['Alex','Mira','Rio'].every(n=>j.opts.includes(n)) && j.sel==='' && j.isNew,'cold link: "Join volume" shows Tokyo Trip (example), 3 members, "New volume" and a picker that starts on no one '+JSON.stringify(j));
   ok(!/join/.test(j.hash) && j.n===1,'the invite leaves the address bar as soon as it is read, nothing saved yet '+JSON.stringify([j.hash,j.n]));
   await B.pg.getByRole('button',{name:'Join'}).click(); ok(/Choose which member/.test(await msgOf(B.pg)),'Join without picking a name asks who you are');
   await B.pg.selectOption('#s-who',{label:'Mira'}); await B.pg.getByRole('button',{name:'Join'}).click();
   const r=await B.pg.evaluate(id=>{ const v=vol(); return {view:ui.view,ok:ui.volId===id,me:nameOf(v,v.meId),hash:location.hash,zero:Object.values(balances(v)).reduce((a,b)=>a+b,0),you:/You/.test($('#app').innerText)}; },exId);
   ok(r.view==='volume' && r.ok && r.me==='Mira' && !/join/.test(r.hash) && r.zero===0,'Join as Mira opens the volume from Mira\'s side; no #join in the URL; zero-sum '+JSON.stringify(r));
   await B.pg.goBack(); await B.pg.waitForTimeout(200);
   ok(!(await B.pg.evaluate(()=>sheetOpen())) && await B.pg.evaluate(()=>ui.view)==='hub','Back after Join goes to the volumes, the join sheet does not come back');
   // the same link on a phone that has the volume (running page: a fragment change): merge, no picker, keeps you
   await B.pg.goto(link); await B.pg.waitForSelector('#s-mode');
   ok(await B.pg.locator('#s-mode').inputValue()==='merge' && !(await B.pg.locator('#s-who').isVisible()) && !(await B.pg.locator('#s-new').count()) && /You already have Tokyo/.test(await B.pg.locator('#s-sum').innerText()),'link for a volume already here: merge sheet, no picker, says you already have it');
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
   const opts=await B.pg.evaluate(()=>[...$('#s-who').options].map(o=>o.textContent));
   ok(opts.includes('Ana') && !opts.includes('Me'),'B\'s picker names the sender (Ana), not "Me" '+JSON.stringify(opts));
   await B.pg.selectOption('#s-who',{label:'Ben'}); taps+=2; await B.pg.getByRole('button',{name:'Join'}).click(); taps+=1;
   ok(taps<=3 && await B.pg.evaluate(()=>ui.view==='volume' && vol().title==='Flat' && nameOf(vol(),vol().meId)==='Ben'),`B joins Flat as Ben in ${taps} taps (opening the link counts 0)`);
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
     const at16=await pg.evaluate(()=>{ const r=id=>document.getElementById(id).getBoundingClientRect(); const p=r('f-paid'), d=r('f-date'); return {ph:p.height,dh:d.height,pt:p.top,dt:d.top,pb:p.bottom,db:d.bottom,pl:p.left,dl:d.left}; });
     // side by side: equal bottoms; stacked (the .btns row wraps at 360): equal left edges
     const row=Math.abs(at16.pt-at16.dt)<0.5;
     if (!(at16.ph===48 && at16.dh===48 && (row ? Math.abs(at16.pb-at16.db)<0.5 : Math.abs(at16.pl-at16.dl)<0.5))) bad.push({w,at16});
     const fit=async ids=>pg.evaluate(ids=>{ const probe=document.createElement('span'); probe.textContent='Ágy'; return ids.map(id=>{ const el=document.getElementById(id), cs=getComputedStyle(el);
       let lh=parseFloat(cs.lineHeight); if (isNaN(lh)) { probe.style.font=cs.font; probe.style.lineHeight='normal'; probe.style.display='inline-block'; document.body.appendChild(probe); lh=probe.getBoundingClientRect().height; probe.remove(); }
       return {id,fs:cs.fontSize,room:el.clientHeight-parseFloat(cs.paddingTop)-parseFloat(cs.paddingBottom),lh}; }); },ids);
     await pg.addStyleTag({content:'.input{font-size:32px!important}'});
     for (const f of await fit(['f-desc','f-paid','f-date'])) if (!(f.fs==='32px' && f.room>=f.lh-0.5)) bad.push({w,...f});
     await pg.keyboard.press('Escape'); await pg.waitForTimeout(50); await pg.evaluate(()=>{ dropSheet(); settleSheet(); });
     for (const f of await fit(['s-from','s-to','s-amt'])) if (!(f.fs==='32px' && f.room>=f.lh-0.5)) bad.push({w,...f});
     await c.close(); }
   ok(bad.length===0,'R2-1: with injected 32px text, #f-desc/#f-paid/#f-date/#s-from/#s-to/#s-amt keep a full line of room; at 16px Paid by and Date are 48px and line up (equal bottoms side by side, equal left edges when stacked; 360, 390) '+JSON.stringify(bad.slice(0,3)));
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
   await B.pg.selectOption('#s-who',{label:'Ben'}); await B.pg.getByRole('button',{name:'Join'}).click();
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
   const rm=await b.newContext({...devices['iPhone 13'],reducedMotion:'reduce'}); const R=await rm.newPage(); await R.goto(link); await R.waitForSelector('#s-who'); await R.selectOption('#s-who',{label:'Ana'}); await R.getByRole('button',{name:'Join'}).click();
   await R.waitForFunction(()=>$('#syncTag') && $('#syncTag').textContent==='Synced',null,{timeout:5000}).catch(()=>{});
   const mo=await R.evaluate(()=>{ const t=$('#syncTag'); t.dataset.s=''; paintSync(ui.volId); const n=document.getAnimations().filter(a=>t.contains(a.effect.target)).length; return n; });
   const mn=await A.pg.evaluate(()=>{ const t=$('#syncTag'); t.dataset.s=''; paintSync(ui.volId); const as=document.getAnimations().filter(a=>t.contains(a.effect.target)); return as.map(a=>a.effect.getTiming().duration); });
   ok(mo===0 && mn.length>0 && mn.every(d=>d<=200),'S6-11: the status line stamps and inks in over '+JSON.stringify(mn)+' ms, and not at all under reduced motion');
   await rm.close();
   const errs=[...A.e,...B.e,...C.e]; ok(errs.length===0,'no page errors in the live sync scenario '+JSON.stringify(errs));
   for (const x of [A,B,C]) await x.c.close(); }
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
   await pg.waitForFunction(()=>!!document.querySelector('.toast.out'),null,{timeout:4000});
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
   const fin=await pg.evaluate(()=>({view:ui.view,route:parseHash().view,h1:$('#app h1').textContent,title:vol()&&vol().title,ghosts:document.querySelectorAll('.ghost').length+Object.keys(ghosts).length,sheet:sheetOpen(),inert:$('#app').inert,nav:$('#nav [aria-current]').textContent,hist:depth()}));
   ok(fin.view==='volume' && fin.route==='volume' && fin.h1===fin.title && fin.ghosts===0 && !fin.sheet && !fin.inert && fin.nav==='Volumes','M: after rapid taps, Back and Forward the app ends on the right screen with no ghost or sheet left '+JSON.stringify(fin));
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
 await b.close();
})();
