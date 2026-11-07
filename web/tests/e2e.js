const {chromium, devices}=require('playwright');
const URL=process.env.URL||'http://127.0.0.1:8765/';
(async()=>{
 const b=await chromium.launch();
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
 const c2=await b.newContext({...devices['iPhone 13']}); const q=await c2.newPage();
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
   await d.getByText('Show code').click();
   await d.evaluate(()=>{ Object.defineProperty(navigator,'clipboard',{value:{writeText:()=>Promise.reject(new Error('denied'))},configurable:true}); });
   await d.getByRole('button',{name:'Copy code'}).click(); await d.waitForTimeout(100);
   ok(await d.locator('#s-code').isVisible() && await d.evaluate(()=>{const t=$('#s-code'); return document.activeElement===t && t.selectionEnd-t.selectionStart===t.value.length;}),'clipboard denied: Copy opens the code and selects all of it');
   await d.locator('[data-act=closeSheet]').click();
   await d.evaluate(()=>{ window.__shared=null; navigator.share=d=>{window.__shared=d; return Promise.resolve();}; window.__copied=0; Object.defineProperty(navigator,'clipboard',{value:{writeText:()=>{window.__copied++; return Promise.resolve();}},configurable:true}); });
   await d.locator('[data-act=volMenu]').click(); await d.locator('.sheet [data-act=share]').click();
   await d.getByRole('button',{name:/Send/}).click(); await d.waitForTimeout(100);
   ok(await d.evaluate(()=>!!window.__shared && window.__shared.text===$('#s-code').value && window.__copied===0),'Send passes the code to the native share sheet');
   await d.evaluate(()=>{ navigator.share=()=>Promise.reject(new DOMException('cancel','AbortError')); });
   await d.getByRole('button',{name:/Send/}).click(); await d.waitForTimeout(100);
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
 { const fresh=async()=>{ const c=await b.newContext({...devices['iPhone 13'],acceptDownloads:true}); const pg=await c.newPage(); const e=[]; pg.on('pageerror',x=>e.push(x.message)); pg.on('console',m=>{if(m.type()==='error')e.push(m.text())}); await pg.goto(URL); return {c,pg,e}; };
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
   // R1-3: Send hands the code to the share sheet; it imports on another phone
   await openVol(B.pg,'Tokyo'); await B.pg.evaluate(()=>{ window.__shared=null; navigator.share=d=>{window.__shared=d; return Promise.resolve();}; });
   await B.pg.locator('[data-act=volMenu]').click(); await B.pg.locator('.sheet [data-act=share]').click(); await B.pg.getByRole('button',{name:/Send/}).click(); await B.pg.waitForTimeout(100);
   const sent=await B.pg.evaluate(()=>window.__shared && window.__shared.text); await B.pg.keyboard.press('Escape');
   const D=await fresh(); await check(D.pg,sent); await D.pg.selectOption('#s-who',{label:'Alex'}); await doImport(D.pg);
   ok(await D.pg.evaluate(id=>ui.volId===id,volId) && await zs(D.pg),'text passed to navigator.share imports on another phone');
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
   await sb.click(); ok(await pg.locator('.sheet .share-sum').isVisible(),'one tap on Share volume opens the share sheet');
   await pg.evaluate(()=>dropSheet()); await pg.locator('[data-act=volMenu]').click(); await pg.locator('.sheet [data-act=share]').click();
   ok(await pg.locator('.sheet .share-sum').isVisible(),'the ⋮ menu Share entry still opens the share sheet'); await pg.evaluate(()=>dropSheet());
   for (const w of [320,360]) { await pg.setViewportSize({width:w,height:780});
     await pg.evaluate(()=>{ const v=exampleVolume(); v.title='Bartholomew and friends: summer road trip 2026'.slice(0,40); v.members[1].name='Maximilian Featherstonehaugh-Wolfe'; S.volumes.unshift(v); ui.volId=v.id; go('volume'); });
     const m=await pg.evaluate(()=>{ const h=$('.topbar h1'); return {hw:h.getBoundingClientRect().width,lines:Math.round(h.getBoundingClientRect().height/parseFloat(getComputedStyle(h).lineHeight)),sw:document.scrollingElement.scrollWidth,cw:document.scrollingElement.clientWidth}; });
     ok(m.hw>=110 && m.lines>=2 && m.sw<=m.cw,`long title at ${w}: h1 ≥110px wide, 2 lines, no sideways scroll ${JSON.stringify(m)}`);
     await pg.evaluate(()=>{ S.volumes.shift(); }); }
   ok(e.length===0,'no console errors in clay checks '+JSON.stringify(e));
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
 await b.close();
})();
