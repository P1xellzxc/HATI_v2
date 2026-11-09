// Data in and out of HATI², end to end and adversarially: Save backup / Restore (file), share code (copy/paste),
// snapshot invite link (#join=<data>) and live invite (#join=<room>.<key>), on desktop Chromium (real download and
// file chooser), the Android app (HatiAndroid.saveFile/share stubs, the WebView's file chooser rules) and iPhone
// emulation (navigator.share/canShare with files). Run by web/test.sh; same helpers and style as e2e.js.
const {chromium, devices}=require('playwright');
const fs=require('fs'), os=require('os'), path=require('path');
const URL=process.env.URL||'http://127.0.0.1:8765/';
const TMP=fs.mkdtempSync(path.join(os.tmpdir(),'hati-dio-'));
(async()=>{
 const b=await chromium.launch();
 // The live sync server mock from e2e.js (protocol in sync/README.md), so no test ever reaches the real Worker.
 const SYNC='https://hati-sync.gerymefernando.workers.dev';
 const mock={rooms:new Map(),socks:new Map()};
 const mroom=id=>mock.rooms.get(id)||{version:0,blob:null};
 const mjson={'access-control-allow-origin':'*','access-control-allow-methods':'GET, PUT, OPTIONS','access-control-allow-headers':'content-type','cache-control':'no-store','content-type':'application/json'};
 const wire=async c=>{
   await c.route(u=>u.href.startsWith(SYNC+'/'),async rt=>{ const rq=rt.request(), u=new globalThis.URL(rq.url());
     const say=(status,body)=>rt.fulfill({status,headers:mjson,body:JSON.stringify(body)});
     if (rq.method()==='OPTIONS') return rt.fulfill({status:204,headers:mjson});
     const m=/^\/v1\/rooms\/([A-Za-z0-9_-]{22,64})$/.exec(u.pathname); if (!m) return say(400,{error:'bad_room'});
     const r=mroom(m[1]);
     if (rq.method()==='GET') return say(200,r);
     let j; try { j=JSON.parse(rq.postData()||''); } catch(e) { return say(400,{error:'bad_json'}); }
     if (j.baseVersion!==r.version) return say(409,r);
     const n={version:r.version+1,blob:j.blob}; mock.rooms.set(m[1],n);
     for (const ws of mock.socks.get(m[1])||[]) try { ws.send(JSON.stringify({version:n.version})); } catch(e){}
     return say(200,{version:n.version}); });
   await c.routeWebSocket(u=>u.href.startsWith(SYNC.replace('https','wss')+'/'),ws=>{
     const id=(/\/v1\/rooms\/([\w-]+)\/ws$/.exec(new globalThis.URL(ws.url()).pathname)||[])[1];
     if (!id) { ws.close({code:1011}); return; }
     const set=mock.socks.get(id)||new Set(); mock.socks.set(id,set); set.add(ws);
     ws.onMessage(m=>{ if (m==='ping') ws.send('pong'); }); ws.onClose(()=>set.delete(ws));
     ws.send(JSON.stringify({version:mroom(id).version})); });
 };
 // A phone that can't reach the sync server, quietly: for backups, codes and snapshot links.
 const noSync=()=>{ const f=window.fetch; window.fetch=function(u){ return String(u&&u.url||u).startsWith('https://hati-sync.')?Promise.reject(new TypeError('Failed to fetch')):f.apply(this,arguments); }; window.WebSocket=undefined; };
 const ok=(c,msg)=>{console.log((c?'PASS ':'FAIL ')+msg); if(!c) process.exitCode=1;};
 const errs=[];
 // Platforms. desktop: real download + file chooser. android: the APK's bridge (saveFile/share), no navigator.share.
 // iphone: iPhone UA with navigator.share/canShare for files; the file handed to the share sheet is kept in __shared.
 const androidInit=()=>{ window.__saved=[]; window.__shared=[]; window.HatiAndroid={ saveFile:(n,m,t)=>window.__saved.push({name:n,mime:m,text:t}), share:t=>window.__shared.push(t) }; delete Navigator.prototype.share; delete Navigator.prototype.canShare; };
 const iosInit=()=>{ window.__shared=[]; window.__shareMode='ok';
   navigator.canShare=d=>!!d; navigator.share=async d=>{ if (window.__shareMode==='cancel') throw new DOMException('Share canceled','AbortError');
     if (window.__shareMode==='deny') throw new DOMException('Not allowed','NotAllowedError');
     window.__shared.push(d.files ? {files:await Promise.all(d.files.map(async f=>({name:f.name,type:f.type,text:await f.text()})))} : d); }; };
 const PLAT={desktop:{dev:{viewport:{width:1280,height:900}}},android:{dev:devices['Pixel 7'],init:androidInit},iphone:{dev:devices['iPhone 13'],init:iosInit}};
 const phone=async(kind,{sync=false,store}={})=>{
   const c=await b.newContext({...PLAT[kind].dev,acceptDownloads:true,serviceWorkers:'block',permissions:kind==='desktop'?['clipboard-read','clipboard-write']:[]});
   await wire(c); if (!sync) await c.addInitScript(noSync); if (PLAT[kind].init) await c.addInitScript(PLAT[kind].init);
   if (store!==undefined) await c.addInitScript(s=>{ if (!sessionStorage.getItem('seeded')) { sessionStorage.setItem('seeded','1'); localStorage.setItem('hati2.mock.v1',s); } },store);
   const pg=await c.newPage(); pg.on('pageerror',e=>errs.push(kind+': '+e.message)); pg.on('console',m=>{ if(m.type()==='error') errs.push(kind+': '+m.text()); });
   await pg.goto(URL); return {c,pg,kind};
 };
 const EMPTY=JSON.stringify({settings:{palette:'classic',mode:'system',strategy:'simplify'},volumes:[]});
 // Every volume, member, expense (all split types, categories, notes, dates, 1 cent and the 1,000,000,000.00 cap),
 // payment, tombstone, unicode and emoji, several currencies; made with the app's own split math, then saved.
 const build=()=>{
   const t0=Date.now()-86400e3, mem=n=>({id:uid(),name:n,upd:t0});
   const A=[mem('Zoë'),mem('李雷'),mem('👩🏽‍🚀 Ana'),mem('سارة'),mem('O\'Brien "Bo" <b>')];
   const ids=A.map(m=>m.id), [a,b2,c,d]=ids;
   const ex=(desc,amount,paidBy,splitType,splits,inputs,category,date,note)=>({id:uid(),desc,amount,paidBy,category,date,splitType,splits,inputs,note,upd:t0+1});
   const v1={id:uid(),title:'Tokyo 🍣 Trip — 東京',type:'travel',currency:'¥',meId:b2,created:t0-5,upd:t0,members:A,
     expenses:[ex('Ramen 🍜',330001,a,'equal',splitEqual(330001,[a,b2,c]),{},'food','2026-01-31',''),
       ex('Taxi',300,b2,'exact',{[a]:100,[c]:200},{},'transport','2025-12-31','Airport → hotel, "late"'),
       ex('Hotel',100000000000,c,'percent',splitWeighted(100000000000,{[a]:33.33,[b2]:33.33,[d]:33.34}),{[a]:'33.33',[b2]:'33.33',[d]:'33.34'},'other','2024-02-29','<script>alert(1)</script> & ünïcödé'),
       ex('teamLab',1,d,'shares',splitWeighted(1,{[a]:1,[d]:2}),{[a]:'1',[d]:'2'},'entertainment','2026-11-09','1 cent'),
       ex('Power',12345,a,'equal',splitEqual(12345,ids),{},'utilities','2026-06-15','عربى'),
       ex('Gifts',999,b2,'exact',{[b2]:999},{},'shopping','2026-03-01','')],
     settlements:[{id:uid(),from:a,to:c,amount:50000,date:'2026-02-01',upd:t0+2},{id:uid(),from:d,to:b2,amount:1,date:'2026-02-02',upd:t0+3}],
     deleted:[uid(),uid(),uid()]};
   const B=[mem('Me'),mem('Kai')];
   const v2={id:uid(),title:'Flat',type:'household',currency:'RM',meId:B[0].id,created:t0-9,upd:t0,members:B,
     expenses:[ex('Rent',250000,B[1].id,'equal',splitEqual(250000,[B[0].id,B[1].id]),{},'utilities','2026-10-01','')],settlements:[],deleted:[]};
   const v3={id:uid(),title:'Solo €',type:'event',currency:'€',meId:'x1',created:t0-1,upd:t0,members:[{id:'x1',name:'Ana',upd:t0}],expenses:[],settlements:[],deleted:[]};
   S.volumes=[v1,v2,v3]; S.settings.palette='persimmon'; S.settings.mode='dark'; save(); return v1.id;
 };
 // What a phone holds, compared deeply: every volume by id, as stored after a reload.
 const snap=pg=>pg.evaluate(()=>JSON.stringify([...S.volumes].sort((x,y)=>x.id<y.id?-1:1)));
 // What is stored, read the way the app reads it (a save may rewrite the same data in its normal form).
const stored=pg=>pg.evaluate(()=>JSON.stringify(normalize(JSON.parse(localStorage.getItem('hati2.mock.v1')))));
 const toastText=pg=>pg.evaluate(()=>[...document.querySelectorAll('.toast')].map(n=>n.textContent).join('|'));
 const clearToasts=pg=>pg.evaluate(()=>document.querySelectorAll('.toast').forEach(n=>n.remove()));
 const settings=async pg=>{ await pg.evaluate(()=>{ if (sheetOpen()) closeSheet(); go('hub'); }); await pg.locator('#app .topbar [data-act=settings]').click(); };
 // Save backup the way each platform does it; returns the file's name, type and text.
 const saveBackup=async P=>{ await settings(P.pg); await clearToasts(P.pg);
   if (P.kind==='desktop') { const [dl]=await Promise.all([P.pg.waitForEvent('download'),P.pg.locator('[data-act=backup]').click()]);
     const f=path.join(TMP,'dl-'+Date.now()+'.json'); await dl.saveAs(f); return {name:dl.suggestedFilename(),type:'application/json',text:fs.readFileSync(f,'utf8'),toast:await toastText(P.pg)}; }
   await P.pg.locator('[data-act=backup]').click(); await P.pg.waitForTimeout(150);
   if (P.kind==='android') { const s=await P.pg.evaluate(()=>window.__saved.pop()); return {name:s.name,type:s.mime,text:s.text,toast:await toastText(P.pg)}; }
   const s=await P.pg.evaluate(()=>window.__shared.pop()); const f=s.files[0]; return {name:f.name,type:f.type,text:f.text,toast:await toastText(P.pg)}; };
 // Settings > Restore > Open file (the platform's file chooser) > Check code; returns the sheet's message or summary.
 const openFile=async(P,file)=>{ await settings(P.pg); await P.pg.locator('[data-act=restore]').click();
   const f=path.join(TMP,file.name); fs.writeFileSync(f,file.text);
   const [ch]=await Promise.all([P.pg.waitForEvent('filechooser'),P.pg.locator('label[for=s-file]').click()]); await ch.setFiles(f);
   await P.pg.waitForFunction(()=>$('#s-paste').value.length>0||$('#sheetMsg').textContent.length>0,null,{timeout:20000}).catch(()=>{});
   await P.pg.locator('#s-go').click(); await P.pg.waitForTimeout(100);
   return {msg:await P.pg.locator('#sheetMsg').innerText(),sum:await P.pg.evaluate(()=>($('#s-sum')||{}).textContent||'')}; };
 const restore=async(P,file)=>{ const r=await openFile(P,file); if (r.msg) return r; await P.pg.waitForTimeout(550); await P.pg.locator('#s-go').click(); await P.pg.waitForTimeout(150); r.toast=await toastText(P.pg); r.open=await P.pg.evaluate(()=>sheetOpen()); return r; };

 // ── 1. Save backup on each platform: name, type, contents, toast ──
 const src={};
 for (const kind of ['desktop','android','iphone']) {
   const P=await phone(kind); await P.pg.evaluate(build); await P.pg.reload();
   const before=await snap(P.pg), f=await saveBackup(P), j=JSON.parse(f.text);
   ok(/^hati2-backup-\d{4}-\d{2}-\d{2}\.json$/.test(f.name) && f.type==='application/json','['+kind+'] backup file is '+f.name+' ('+f.type+')');
   ok(j.app==='hati2-mock' && j.kind==='backup' && j.v===1 && j.volumes.length===3 && !('settings' in j),'['+kind+'] backup holds every volume and no per-phone settings');
   ok(kind==='android' ? !/Backup saved/.test(f.toast) : /Backup saved: 3 volumes/.test(f.toast),'['+kind+'] toast after Save backup: '+JSON.stringify(f.toast)+(kind==='android'?' (the app says Saved once a place is picked)':''));
   src[kind]={file:f,before,P};
 }
 // ── 2. Every backup restores losslessly on every platform (Android → iPhone and web, and back) ──
 for (const from of Object.keys(src)) for (const to of ['desktop','android','iphone']) {
   const Q=await phone(to,{store:EMPTY}); await Q.pg.evaluate(()=>{ S.settings.palette='lagoon'; save(); });
   const r=await restore(Q,src[from].file);
   await Q.pg.reload(); const after=await snap(Q.pg);
   ok(!r.msg && /Restores 3 volumes/.test(r.sum) && /Backup restored: 3 volumes/.test(r.toast||''),`[${from} → ${to}] restore into an empty app: ${r.msg||r.sum} / ${r.toast}`);
   ok(after===src[from].before,`[${from} → ${to}] round trip is lossless (volumes, members, every split type, cents, notes, dates, payments, tombstones, who you are)`+(after===src[from].before?'':'\n  before '+src[from].before.slice(0,400)+'\n  after  '+after.slice(0,400)));
   ok(await Q.pg.evaluate(()=>S.settings.palette==='lagoon' && S.volumes.every(v=>Object.values(balances(v)).reduce((a,b)=>a+b,0)===0)),`[${from} → ${to}] this phone keeps its own settings; balances still sum to zero`);
   await Q.c.close();
 }
 // ── 3. Restore twice is idempotent; restore merges into the same volumes, newest edit wins, nothing doubles ──
 { const P=src.desktop.P, f=src.desktop.file;
   const r1=await restore(P,f); ok(/merges 3 volumes you already have/i.test(r1.sum) && !r1.open,'restore onto the phone that made it: '+r1.sum);
   const r2=await restore(P,f); await P.pg.reload();
   ok(await snap(P.pg)===src.desktop.before && /merges 3/i.test(r2.sum),'restoring the same backup twice changes nothing and adds no duplicates');
   // local edits after the backup: one edited (newer), one added, one deleted; the backup is older
   const id=await P.pg.evaluate(()=>{ const v=S.volumes.find(x=>x.title.startsWith('Tokyo')), e=v.expenses[0];
     e.desc='Ramen (edited)'; e.upd=Date.now(); v.expenses.push({...v.expenses[1],id:uid(),desc:'New after backup',upd:Date.now()});
     const gone=v.expenses.find(x=>x.desc==='Gifts'); v.expenses=v.expenses.filter(x=>x!==gone); tombstone(v,gone.id);
     v.members[0].name='Zoë B'; v.members[0].upd=Date.now(); save(); return v.id; });
   const mine=await snap(P.pg); const r3=await restore(P,f); await P.pg.reload();
   const v=await P.pg.evaluate(id=>{ const v=S.volumes.find(x=>x.id===id); return {n:v.expenses.length,descs:v.expenses.map(e=>e.desc),m:v.members[0].name,vols:S.volumes.length}; },id);
   ok(await snap(P.pg)===mine && v.vols===3 && v.descs.includes('Ramen (edited)') && v.descs.includes('New after backup') && !v.descs.includes('Gifts') && v.m==='Zoë B','an older backup never undoes newer edits, re-adds a deleted expense or doubles anything: '+JSON.stringify(v)+' '+r3.sum);
   // the other way: the backup is newer than this phone's copy
   const Q=await phone('iphone',{store:EMPTY}); await restore(Q,f);
   await Q.pg.evaluate(()=>{ const v=S.volumes.find(x=>x.title.startsWith('Tokyo')); v.expenses[0].desc='Old name'; v.expenses[0].upd=1; save(); });
   const newer=await saveBackup(P); await restore(Q,newer); await Q.pg.reload();
   ok(await Q.pg.evaluate(()=>{ const v=S.volumes.find(x=>x.title.startsWith('Tokyo')); return v.expenses.some(e=>e.desc==='Ramen (edited)') && !v.expenses.some(e=>e.desc==='Gifts'||e.desc==='Old name'); }),'a newer backup brings its edits and deletions in');
   // into a phone with different volumes: those stay, the backup's are added
   const R=await phone('android'); await R.pg.reload(); const own=await snap(R.pg); const r4=await restore(R,f); await R.pg.reload();
   const all=JSON.parse(await snap(R.pg)), ownV=JSON.parse(own)[0];
   ok(/Restores 3 volumes/.test(r4.sum) && all.length===4 && JSON.stringify(all.find(x=>x.id===ownV.id))===JSON.stringify(ownV),'restore onto a phone with other volumes adds the 3 and leaves its own untouched');
   await Q.c.close(); await R.c.close(); }
 // ── 4. Bad files: clear message, local data untouched ──
 { const P=await phone('android'); await P.pg.evaluate(build); await P.pg.reload(); const keep=await stored(P.pg);
   const good=JSON.parse(src.desktop.file.text), vol=good.volumes[0];
   const bad={
     'empty file':['empty.json','',/empty/i],
     'truncated backup':['hati2-backup-cut.json',src.desktop.file.text.slice(0,Math.floor(src.desktop.file.text.length/2)),/isn't a HATI² backup|damaged/i],
     'a photo':['photo.json','\x89PNG\r\n\x1a\n\0\0\0\rIHDR{\0}\xff\xd8',/isn't a HATI² backup/i],
     'other app JSON':['other.json','{"name":"x","items":[1,2]}',/isn't a HATI² backup/i],
     'backup from a newer HATI²':['hati2-backup-new.json',JSON.stringify({...good,v:2}),/newer version/i],
     'share code from a newer HATI²':['code.json',JSON.stringify({app:'hati2-mock',v:3,volume:vol}),/newer version/i],
     'backup whose totals do not add up':['hati2-backup-bad.json',JSON.stringify({...good,volumes:[{...vol,expenses:[{...vol.expenses[0],amount:vol.expenses[0].amount+1}]}]}),/can't be read/i],
     'more volumes than a backup can hold':['hati2-backup-many.json',JSON.stringify({...good,volumes:Array.from({length:501},(_,i)=>({...vol,id:'v'+i}))}),/can't be read/i],
   };
   for (const [what,[name,text,want]] of Object.entries(bad)) {
     const r=await restore(P,{name,text}), now=await stored(P.pg);
     ok(want.test(r.msg) && now===keep,`bad file, ${what}: "${r.msg}", data unchanged`);
   }
   // a huge file is refused before it is read into the page
   const big=path.join(TMP,'huge.json'); fs.writeFileSync(big,'{"app":"hati2-mock","kind":"backup","v":1,"volumes":[],"pad":"'+'x'.repeat(40e6)+'"}');
   await settings(P.pg); await P.pg.locator('[data-act=restore]').click(); await P.pg.setInputFiles('#s-file',big); await P.pg.waitForTimeout(300);
   ok(/too big/i.test(await P.pg.locator('#sheetMsg').innerText()) && await P.pg.evaluate(()=>$('#s-paste').value.length)===0 && await stored(P.pg)===keep,'a 40 MB file is refused as too big without loading it: '+await P.pg.locator('#sheetMsg').innerText());
   // typing after a bad file goes back to the paste messages
   await P.pg.fill('#s-paste','hello'); await P.pg.locator('#s-go').click();
   ok(/That code isn't valid/.test(await P.pg.locator('#sheetMsg').innerText()),'pasted text still gets the paste message');
   await P.c.close(); }
 // ── 5. Cancel and failure never claim success ──
 { const I=await phone('iphone'); await settings(I.pg);
   await I.pg.evaluate(()=>{ window.__shareMode='cancel'; }); await clearToasts(I.pg); await I.pg.locator('[data-act=backup]').click(); await I.pg.waitForTimeout(150);
   ok(await toastText(I.pg)==='','iPhone: cancelling the share sheet shows no "Backup saved"');
   const A=await phone('android'); await settings(A.pg); await clearToasts(A.pg); await A.pg.locator('[data-act=backup]').click(); await A.pg.waitForTimeout(150);
   ok(await toastText(A.pg)==='' && await A.pg.evaluate(()=>window.__saved.length)===1,'Android app: Save backup leaves "Saved" to the app (a cancelled save-as says nothing)');
   await I.c.close(); await A.c.close(); }
 // ── 6. The Android app's file chooser: the WebView builds its picker intent from the FIRST accept type only
 // (FileChooserParams.createIntent → setType(accept.split(",")[0])), and ".json" is not a MIME type ──
 for (const kind of ['android','desktop','iphone']) {
   const P=await phone(kind); await settings(P.pg); await P.pg.locator('[data-act=restore]').click();
   const acc=await P.pg.evaluate(()=>$('#s-file').getAttribute('accept')||''), first=acc.split(',')[0].trim();
   ok(kind==='android' ? first==='' : /^[a-z]+\/[\w.+-]+$/.test(first) && /\.json/.test(acc),`[${kind}] file chooser accept="${acc}"`+(kind==='android'?' (Android: every file, so a .json a chat app saved as octet-stream can be picked)':' (a MIME type first)'));
   await P.c.close();
 }
 // ── 7. Share code: copy on one phone, paste on another (desktop clipboard; Android share bridge) ──
 { const P=src.desktop.P; await P.pg.evaluate(()=>{ ui.volId=S.volumes.find(v=>v.title.startsWith('Tokyo')).id; go('volume'); });
   const before=await P.pg.evaluate(()=>JSON.stringify(cleanVolume(vol())));
   await P.pg.locator('[data-act=volMenu]').click(); await P.pg.locator('.sheet [data-act=share]').click();
   await P.pg.locator('#s-codebox summary').click(); await P.pg.locator('#s-codebox [data-act=copyCode]').click();
   const code=await P.pg.evaluate(()=>navigator.clipboard.readText());
   const Q=await phone('android',{store:EMPTY}); await Q.pg.locator('[data-act=import]').first().click(); await Q.pg.fill('#s-paste','Here you go: '+code+' thanks!'); await Q.pg.locator('#s-go').click();
   await Q.pg.waitForTimeout(550); await Q.pg.selectOption('#s-who',{label:'Zoë B'}); await Q.pg.locator('#s-go').click(); await Q.pg.waitForTimeout(100); await Q.pg.reload();
   const got=await Q.pg.evaluate(()=>JSON.stringify(cleanVolume(S.volumes[0])));
   const norm=s=>{ const v=JSON.parse(s); delete v.meId; return JSON.stringify(v); };
   ok(norm(got)===norm(before),'share code pasted with chat text around it brings the volume over losslessly');
   // the Android app's Send for a code too big for its share sheet copies it instead of handing it to the app
   await Q.pg.evaluate(()=>{ const v=S.volumes[0], ids=v.members.map(m=>m.id); for (let i=0;i<4000;i++) v.expenses.push({id:'big'+i,desc:'Expense '+i+' '+Math.random().toString(36),amount:1000+i,paidBy:ids[i%3],category:'food',date:'2026-10-01',splitType:'equal',splits:splitEqual(1000+i,ids),inputs:{},note:Math.random().toString(36).repeat(4),upd:1}); save(); ui.volId=v.id; go('volume'); });
   await Q.pg.locator('[data-act=volMenu]').click(); await Q.pg.locator('.sheet [data-act=share]').click(); await Q.pg.waitForTimeout(300);
   const len=await Q.pg.evaluate(()=>$('#s-code').value.length);
   await Q.pg.locator('[data-act=sendCode]').click(); await Q.pg.waitForTimeout(150);
   const sent=await Q.pg.evaluate(()=>window.__shared.map(t=>t.length));
   ok(len>100000 && !sent.some(n=>n>100000) && (await Q.pg.locator('#sheetMsg').innerText()).length>0,`Android app: a ${len}-character code is not pushed through the share intent (Binder limit, the app would crash); it is copied or selected instead (${await Q.pg.locator('#sheetMsg').innerText()})`);
   await Q.c.close(); }
 // ── 8. Snapshot invite link: near the 512 KB limit, unicode, lossless; just over the limit falls back to the code ──
 { const P=await phone('desktop'); await P.pg.evaluate(build);
   // Pads the first volume until its share code is just under (or, with over, just over) target bytes.
   const fill=async(target,over)=>P.pg.evaluate(([target,over])=>{ const v=S.volumes[0], ids=v.members.map(m=>m.id), size=()=>new TextEncoder().encode(shareCodeOf(v)).length; v.expenses=v.expenses.filter(e=>!e.id.startsWith('pad'));
     let i=0; const add=()=>v.expenses.push({id:'pad'+(i++),desc:'Ünïcødé 🍣 '+Math.random().toString(36).slice(2),amount:777+i,paidBy:ids[i%5],category:'food',date:'2026-09-09',splitType:'equal',splits:splitEqual(777+i,ids),inputs:{},note:'n'+Math.random().toString(36).repeat(3),upd:1});
     for (let n=size(); n<target; n=size()) for (let k=Math.max(1,Math.floor((target-n)/400)); k>0; k--) add();
     if (!over) while (size()>target) v.expenses.pop();
     save(); ui.volId=v.id; go('volume'); return size(); },[target,over]);
   const n=await fill(512*1024-200);
   const before=await P.pg.evaluate(()=>JSON.stringify(cleanVolume(vol())));
   await P.pg.evaluate(()=>shareSheet()); await P.pg.waitForFunction(()=>$('#s-link')||$('#s-code'),null,{timeout:20000});
   const link=await P.pg.evaluate(()=>shareLink);
   ok(link.includes('#join=') && !(await P.pg.locator('svg.qr').count()),`a ${n}-byte volume still goes as a link (${link.length} chars), with no QR`);
   const Q=await phone('iphone',{store:EMPTY}); await Q.pg.goto(link); await Q.pg.waitForSelector('#s-who',{timeout:20000});
   await Q.pg.selectOption('#s-who',{index:1}); await Q.pg.locator('#sheetForm .btns .primary').click(); await Q.pg.waitForTimeout(150); await Q.pg.reload();
   const got=await Q.pg.evaluate(()=>JSON.stringify(cleanVolume(S.volumes[0])));
   const norm=s=>{ const v=JSON.parse(s); delete v.meId; delete v.created; return JSON.stringify(v); };
   ok(norm(got)===norm(before),'snapshot link at the 512 KB limit joins on iPhone with every entry intact');
   await P.pg.evaluate(()=>closeSheet()); const n2=await fill(512*1024+1,true); await P.pg.evaluate(()=>shareSheet()); await P.pg.waitForSelector('#s-code',{state:'attached',timeout:20000});
   ok(!(await P.pg.evaluate(()=>shareLink)) && await P.pg.locator('[data-act=copyCode]').count()>0,`a ${n2}-byte volume is shared as a code instead of a link the receiver would refuse`);
   await P.c.close(); await Q.c.close(); }
 // ── 9. Live invite + Restore: a restore never breaks the room or the key, and sync carries on ──
 { const A=await phone('desktop',{sync:true}), B=await phone('iphone',{sync:true,store:EMPTY});
   await A.pg.evaluate(build); await A.pg.evaluate(()=>{ ui.volId=S.volumes[0].id; go('volume'); });
   const f=await saveBackup(A); // made before the volume was shared
   await A.pg.evaluate(()=>{ ui.volId=S.volumes[0].id; go('volume'); }); await A.pg.locator('[data-act=volMenu]').click(); await A.pg.locator('.sheet [data-act=share]').click();
   await A.pg.waitForFunction(()=>/^.*#join=[\w-]+\.[\w-]+$/.test(shareLink),null,{timeout:10000}); const link=await A.pg.evaluate(()=>shareLink);
   await B.pg.goto(link); await B.pg.waitForSelector('#s-who',{timeout:10000}); await B.pg.selectOption('#s-who',{index:3}); await B.pg.locator('#sheetForm .btns .primary').click(); await B.pg.waitForTimeout(400);
   const sy=pg=>pg.evaluate(()=>localStorage.getItem('hati2.sync.v1'));
   const syA=JSON.parse(await sy(A.pg)), syB=JSON.parse(await sy(B.pg)), vid=Object.keys(syA)[0];
   ok(vid && syB[vid] && syB[vid].room===syA[vid].room && syB[vid].key===syA[vid].key,'live invite: the joined phone holds the same room and key');
   ok(!f.text.includes(syA[vid].key) && !(await saveBackup(A)).text.includes(syA[vid].key),'the sync key never goes into a backup file');
   const r=await restore(B,f); await B.pg.waitForTimeout(800);
   const syB2=JSON.parse(await sy(B.pg));
   ok(/merges 1 volume/.test(r.sum) && /Restores 2 volumes/.test(r.sum) && syB2[vid].room===syA[vid].room && syB2[vid].key===syA[vid].key && Object.keys(syB2).length===1,'restore on a live phone merges the shared volume and keeps its room and key: '+r.sum);
   await A.pg.evaluate(()=>{ const v=S.volumes[0]; v.expenses.push({...v.expenses[0],id:'afterRestore',desc:'After restore',upd:Date.now()}); commit(); });
   await B.pg.waitForFunction(id=>{ const v=S.volumes.find(x=>x.id===id); return v && v.expenses.some(e=>e.id==='afterRestore'); },vid,{timeout:10000}).catch(()=>{});
   ok(await B.pg.evaluate(id=>S.volumes.find(x=>x.id===id).expenses.some(e=>e.id==='afterRestore'),vid),'after the restore, a change on the other phone still arrives by live sync');
   const meB=await B.pg.evaluate(id=>{ const v=S.volumes.find(x=>x.id===id); return nameOf(v,v.meId); },vid);
   ok(meB!=='李雷','restore keeps who you are on this phone in the shared volume (still '+meB+', not the backup maker)');
   await A.c.close(); await B.c.close(); }
 // ── 10. Split inputs survive save → reload → backup → restore (the edit form shows what was typed) ──
 { const P=await phone('android'); await P.pg.evaluate(()=>{ ui.volId=S.volumes[0].id; go('volume'); });
   await P.pg.getByRole('button',{name:/Add expense/}).click(); await P.pg.fill('#f-amount','100'); await P.pg.fill('#f-desc','Thirds');
   await P.pg.locator('[data-split="percent"]').click(); const ins=P.pg.locator('[data-in]');
   await ins.nth(0).fill('33.3333333333'); await ins.nth(1).fill('33.3333333333'); await ins.nth(2).fill('33.3333333334');
   await P.pg.locator('#expForm button[type=submit]').click(); await P.pg.reload();
   const f=await saveBackup(P); const Q=await phone('desktop',{store:EMPTY}); await restore(Q,f); await Q.pg.reload();
   const inputs=await Q.pg.evaluate(()=>Object.values(S.volumes[0].expenses.find(e=>e.desc==='Thirds').inputs));
   ok(inputs.length===3 && inputs.every(x=>/^33\.333333333[34]$/.test(x)),'percent inputs as typed survive reload and a backup round trip '+JSON.stringify(inputs));
   await P.c.close(); await Q.c.close(); }
 for (const k of Object.keys(src)) await src[k].P.c.close();
 ok(errs.length===0,'no console errors in the data in/out checks '+JSON.stringify(errs.slice(0,5)));
 fs.rmSync(TMP,{recursive:true,force:true});
 await b.close();
})().catch(e=>{ console.log('FAIL data-io crashed: '+(e&&e.stack||e)); process.exitCode=1; });
