(()=>{'use strict';
const app=document.getElementById('inputApp');if(!app)return;
const q=s=>app.querySelector(s),content=q('#inputContent'),notice=q('#loadStatus'),yearInput=q('#inputYear'),playerSelect=q('#inputPlayer');
const params=new URLSearchParams(location.search),today=new Date();let year=Number(params.get('year'))||today.getFullYear(),month=params.get('month')||`${year}-${String(today.getMonth()+1).padStart(2,'0')}`,tab=app.dataset.page||'attendance',selected=Number(params.get('playerId'))||null,data,day=null;
let controllers=[];
const el=(tag,text,cls)=>{const x=document.createElement(tag);if(text!==undefined)x.textContent=text;if(cls)x.className=cls;return x;};
const card=title=>{const c=el('div',undefined,'input-card');if(title)c.append(el('h2',title));return c;};
function field(parent,title,type,value){const l=el('label',title),i=el(type==='textarea'?'textarea':'input');if(type!=='textarea')i.type=type;if(type==='checkbox')i.checked=Boolean(value);else i.value=value??'';l.append(i);parent.append(l);return i;}
function button(text,fn,cls){const b=el('button',text,cls);b.type='button';b.addEventListener('click',fn);return b;}
function select(parent,title,options,value){const l=el('label',title),s=el('select');for(const [v,text] of options){const o=el('option',text);o.value=v;s.append(o);}s.value=value??'';l.append(s);parent.append(l);return s;}
async function post(path,values){const body=new URLSearchParams();Object.entries(values).forEach(([k,v])=>{if(v!==null&&v!==undefined)body.set(k,String(v));});const r=await fetch(path,{method:'POST',body,headers:{[app.dataset.csrfHeader]:app.dataset.csrf}});let result;try{result=await r.json();}catch{}if(!r.ok)throw Error(r.status===409?'他の画面で更新されています。入力を控えて再読み込みしてください。':r.status===401||r.status===403?'ログインが失効したか、権限が変更されました。':result?.message||'保存できませんでした。入力内容・通信状態を確認してください。');return result;}
// Each form serializes saves; changes made during a request remain queued with the returned version.
function autosave(parent,inputs,path,values,onSaved){let dirty=false,busy=false,timer=null,failure=false;const status=el('output','', 'save-state');status.setAttribute('aria-live','polite');const retry=button('再試行',()=>{failure=false;return save();},'retry');retry.hidden=true;parent.append(status,retry);
 async function save(){clearTimeout(timer);if(busy||!dirty)return !failure;busy=true;dirty=false;failure=false;retry.hidden=true;status.classList.remove('error');status.textContent='保存中…';try{const payload=values();const result=await post(path,payload);onSaved(result,payload);status.textContent='保存済み';}catch(e){dirty=true;failure=true;status.textContent=e.message;status.classList.add('error');retry.hidden=false;}finally{busy=false;}if(dirty&&!failure)return save();return !failure;}
 for(const i of inputs){const changed=()=>{dirty=true;failure=false;status.textContent='未保存';clearTimeout(timer);timer=setTimeout(save,i.type==='checkbox'||i.type==='radio'||i.tagName==='SELECT'?0:700);};i.addEventListener('input',changed);i.addEventListener('change',()=>{if(dirty)save();});}
 const ctl={flush:async()=>{clearTimeout(timer);while(busy)await new Promise(r=>setTimeout(r,40));return save();},pending:()=>dirty||busy};controllers.push(ctl);return ctl;
}
async function flush(){for(const c of controllers)if(!await c.flush())return false;return true;}
window.addEventListener('beforeunload',e=>{if(controllers.some(c=>c.pending())){e.preventDefault();e.returnValue='';}});
async function load(){notice.textContent='読み込み中…';notice.className='';try{const r=await fetch(`/api/input?year=${year}&month=${month}`);if(!r.ok)throw Error('読み込めませんでした。ログインと通信状態を確認してください。');data=await r.json();if(!data.players.some(p=>p.id===selected))selected=data.players[0]?.id??null;playerSelect.replaceChildren();for(const p of data.players){const o=el('option',p.name);o.value=p.id;playerSelect.append(o);}playerSelect.value=selected??'';notice.textContent='';render();}catch(e){notice.textContent=e.message;notice.className='error';}}
function render(){controllers=[];content.replaceChildren();yearInput.value=year;q('#yearField').hidden=!['fee','attendance'].includes(tab);q('#playerField').hidden=tab==='gear';app.querySelectorAll('[data-tab]').forEach(b=>b.setAttribute('aria-selected',String(b.dataset.tab===tab)));if(tab==='stats')stats();else if(tab==='fee')fee();else if(tab==='gear')gear();else attendance();}
function stats(){const p=data.players.find(p=>p.id===selected);if(!p){content.append(el('p','表示中の選手がいません。'));return;}const c=card('打撃成績'),inputs=[];let bats,hits;
 if(data.fields.atBats){bats=field(c,'打数','number',p.atBats??0);bats.min='0';inputs.push(bats);}if(data.fields.hits){hits=field(c,'安打','number',p.hits??0);hits.min='0';inputs.push(hits);}const avg=el('p');const average=()=>{if(data.fields.average&&bats&&hits)avg.textContent=`打率：${Number(bats.value)>0?(Number(hits.value)/Number(bats.value)).toFixed(3):'---'}`;};inputs.forEach(i=>i.addEventListener('input',average));average();c.append(avg);
 if(inputs.length)autosave(c,inputs,'/api/input/stats',()=>{if([bats,hits].filter(Boolean).some(i=>i.value===''||!Number.isInteger(Number(i.value))||Number(i.value)<0))throw Error('打数・安打は0以上の整数を入力してください。');return {playerId:p.id,version:p.version,atBats:bats?bats.value:null,hits:hits?hits.value:null};},(r,payload)=>{p.version=r.version;if(bats)p.atBats=Number(payload.atBats);if(hits)p.hits=Number(payload.hits);});else c.append(el('p','打数・安打は非表示に設定されています。'));content.append(c);}
function fee(){const grid=el('div',undefined,'input-grid');const paid=card('支払い済み'),unpaid=card('未払い');const paidList=el('ul',undefined,'name-list'),unpaidList=el('ul',undefined,'name-list');paid.append(paidList);unpaid.append(unpaidList);grid.append(paid,unpaid);const refresh=()=>{paidList.replaceChildren();unpaidList.replaceChildren();for(const p of data.players){const f=data.fees.find(f=>f.playerId===p.id);(f?.paid?paidList:unpaidList).append(el('li',p.name));}if(!paidList.children.length)paidList.append(el('li','該当者なし'));if(!unpaidList.children.length)unpaidList.append(el('li','該当者なし'));};refresh();content.append(grid);
 const p=data.players.find(p=>p.id===selected);if(!p)return;let f=data.fees.find(f=>f.playerId===p.id);if(!f){f={playerId:p.id,paid:false,comment:'',version:-1};data.fees.push(f);}const c=card(`${year}年度・${p.name}`),check=field(c,'支払い済み','checkbox',f.paid),memo=field(c,'コメント（金額など）','textarea',f.comment);memo.maxLength=1000;autosave(c,[check,memo],'/api/input/fee',()=>({playerId:p.id,year,paid:check.checked,comment:memo.value,version:f.version}),(r,v)=>{Object.assign(f,{version:r.version,paid:v.paid,comment:v.comment});refresh();});content.append(c);}
function gear(){content.append(el('p','道具の種類・持ち主を設定します。道具名を入力すると自動登録されます。','muted'));for(const g of [...data.gears,{id:null,name:'',ownerId:null,comment:'',version:-1}]){const c=card(g.id?'道具':'道具を追加'),name=field(c,'道具の種類','text',g.name);name.maxLength=100;const opts=[['','未設定'],...data.players.map(p=>[p.id,p.name])];if(g.ownerId&&!data.players.some(p=>p.id===g.ownerId))opts.push([g.ownerId,'非表示の選手（現在の持ち主）']);const owner=select(c,'持ち主',opts,g.ownerId),memo=field(c,'コメント','textarea',g.comment);memo.maxLength=1000;
 const ctl=autosave(c,[name,owner,memo],'/api/input/gear',()=>({id:g.id,name:name.value,ownerId:owner.value||null,comment:memo.value,version:g.version}),(r,v)=>{const fresh=g.id===null;Object.assign(g,v,{id:r.id,version:r.version,ownerId:v.ownerId?Number(v.ownerId):null});if(fresh){data.gears.push(g);c.querySelector('h2').textContent='道具';add.hidden=false;}});
 const add=button('さらに道具を追加',async()=>{if(await flush())render();});add.hidden=g.id!==null;c.append(add);
 c.append(button('削除',async()=>{if(!g.id){name.value='';memo.value='';return;}if(!await flush()||!confirm('この道具を削除しますか？'))return;try{await post('/api/input/gear',{id:g.id,version:g.version,delete:true});data.gears=data.gears.filter(x=>x.id!==g.id);render();}catch(e){notice.textContent=e.message;}},'danger'));content.append(c);}}
const dateLabel=d=>new Date(d+'T12:00:00').toLocaleDateString('ja-JP',{month:'2-digit',day:'2-digit',weekday:'short'});
function attendance(){
 async function changeMonth(value){if(!await flush())return;month=value;year=Number(value.slice(0,4));day=null;await load();}
 function adjacentMonth(offset){const [y,m]=month.split('-').map(Number),d=new Date(y,m-1+offset,1);return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}`;}
 const monthPicker=el('div',undefined,'month-picker');
 const monthSelect=select(monthPicker,'月',Array.from({length:12},(_,i)=>[`${year}-${String(i+1).padStart(2,'0')}`,`${i+1}月`]),month);
 monthSelect.addEventListener('change',async()=>{const target=monthSelect.value;await changeMonth(target);monthSelect.value=month;});content.append(monthPicker);
 const editor=card();editor.classList.add('attendance-editor');content.append(editor);
 const summary=card(`${Number(month.slice(0,4))}年${Number(month.slice(5))}月の出欠確認`);const rows=new Map();function refresh(){for(const [date,row] of rows){row.replaceChildren(el('span',dateLabel(date)));const counts=el('span',undefined,'day-counts'),answers=data.answers.filter(a=>a.date===date);for(const s of ['○','△','×'])counts.append(el('span',`${s} ${answers.filter(a=>a.status===s).length}`));row.append(counts);}if(detail.isConnected)renderDetail();}
 for(const date of data.dates){const row=button('',async()=>{if(!await flush())return;day=date;renderDetail();detail.scrollIntoView({behavior:'smooth',block:'start'});},'day-row');rows.set(date,row);summary.append(row);}content.append(summary);
 const edit=el('details',undefined,'input-card');edit.append(el('summary','対象日を追加'));const extra=field(edit,'追加する日付','date','');extra.min=`${year}-01-01`;extra.max=`${year}-12-31`;const msg=el('p');edit.append(button('日付を追加',async()=>{if(!extra.value)return;if(!await flush())return;try{await post('/api/input/date',{date:extra.value});month=extra.value.slice(0,7);day=null;await load();}catch(e){msg.textContent=e.message;}},'primary'),msg);content.append(edit);
 const detail=card();detail.classList.add('attendance-detail');content.append(detail);if(!day||!data.dates.includes(day))day=data.dates[0]??null;
 const detailControllers=[];
 function renderRespondents(){
  detail.querySelectorAll('.attendance-person,.no-answers').forEach(row=>row.remove());
  let count=0;
  for(const p of data.players){const a=data.answers.find(a=>a.date===day&&a.playerId===p.id&&['○','△','×'].includes(a.status));if(!a)continue;
   const row=el('div',undefined,'attendance-person'),head=el('div',undefined,'person-head');head.append(el('span',p.name),el('span',a.status));row.append(head);if(a.memo)row.append(el('p',a.memo,'person-memo'));detail.append(row);count++;
  }
  if(!count)detail.append(el('p','まだ回答がありません。','no-answers muted'));
 }

 function renderDetail(){for(const c of detailControllers){const i=controllers.indexOf(c);if(i>=0)controllers.splice(i,1);}detailControllers.length=0;detail.replaceChildren();editor.replaceChildren();
 const monthNav=el('div',undefined,'day-nav month-nav');
 const previousMonth=button('←',()=>changeMonth(adjacentMonth(-1))),nextMonth=button('→',()=>changeMonth(adjacentMonth(1)));
 previousMonth.setAttribute('aria-label','前の月');nextMonth.setAttribute('aria-label','次の月');
 previousMonth.disabled=month==='1900-01';nextMonth.disabled=month==='2100-12';
 monthNav.append(previousMonth,el('h2',`${Number(month.slice(0,4))}年${Number(month.slice(5))}月の回答`),nextMonth);editor.append(monthNav);
 if(!day){editor.append(el('p','対象日がありません。'));return;}
 const answerDay=select(editor,'回答する日付',data.dates.map(d=>[d,dateLabel(d)]),day);
 answerDay.addEventListener('change',async()=>{const value=answerDay.value;if(!await flush()){answerDay.value=day;return;}day=value;renderDetail();});
 const nav=el('div',undefined,'day-nav'),index=data.dates.indexOf(day);const prev=button('←',async()=>{if(await flush()){day=data.dates[index-1];renderDetail();}}),next=button('→',async()=>{if(await flush()){day=data.dates[index+1];renderDetail();}});prev.disabled=index===0;next.disabled=index===data.dates.length-1;nav.append(prev,el('h2',dateLabel(day)),next);detail.append(nav);
 renderRespondents();
 const p=data.players.find(p=>p.id===selected);if(!p)return;const targetDay=day;let a=data.answers.find(a=>a.date===targetDay&&a.playerId===p.id);if(!a){a={playerId:p.id,date:targetDay,status:'',memo:'',version:-1};data.answers.push(a);}const form=el('div',undefined,'attendance-form');form.append(el('h2',`${p.name} の回答`));const choices=el('div',undefined,'answer-choices');const radios=[];for(const s of ['○','△','×']){const l=el('label'),radio=el('input');radio.type='radio';radio.name='answer';radio.value=s;radio.checked=a.status===s;radio.className='answer-radio';const label=el('span',s,'answer-option');l.append(radio,label);choices.append(l);radios.push(radio);}form.append(choices);const memo=field(form,'コメント','textarea',a.memo);memo.maxLength=500;
 const ctl=autosave(form,[...radios,memo],'/api/input/attendance',()=>{const status=radios.find(r=>r.checked)?.value;if(!status)throw Error('○・△・×を選択してください。');return {playerId:p.id,date:targetDay,status,memo:memo.value,version:a.version};},(r,v)=>{Object.assign(a,v,{version:r.version});for(const [date,row] of rows){const answers=data.answers.filter(a=>a.date===date);row.querySelectorAll('.day-counts span').forEach((span,i)=>{const s=['○','△','×'][i];span.textContent=`${s} ${answers.filter(a=>a.status===s).length}`;});}renderRespondents();});detailControllers.push(ctl);editor.append(form);
 }
 refresh();}

playerSelect.addEventListener('change',async()=>{const next=Number(playerSelect.value);if(!await flush()){playerSelect.value=selected;return;}selected=next;render();});
yearInput.addEventListener('change',async()=>{const next=Number(yearInput.value);if(!Number.isInteger(next)||next<1900||next>2100||!await flush()){yearInput.value=year;return;}year=next;month=`${year}-${month.slice(5)}`;day=null;await load();});
if(!['stats','fee','gear','attendance'].includes(tab))tab='stats';load();
})();
