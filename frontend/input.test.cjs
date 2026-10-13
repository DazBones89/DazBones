const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const { JSDOM } = require('jsdom');

const source = fs.readFileSync('src/main/resources/static/js/input.js', 'utf8');
const markup = fs.readFileSync('src/main/resources/templates/input.html', 'utf8');
const tick = () => new Promise(resolve => setTimeout(resolve, 10));
async function until(check) {
  for (let n = 0; n < 150; n++) { if (check()) return; await tick(); }
  assert.fail('UI did not reach the expected state');
}
async function screen(tab, options = {}) {
  const dom = new JSDOM(markup, { url: `http://localhost/input?tab=${tab}&year=2026&month=2026-09`, runScripts: 'outside-only' });
  const { window } = dom;
  window.HTMLElement.prototype.scrollIntoView = () => {};
  window.confirm = () => true;
  const app = window.document.getElementById('inputApp');
  app.dataset.page = tab; app.dataset.csrf = 'test-token'; app.dataset.csrfHeader = 'X-CSRF-TOKEN';
  const state = {
    players: [{ id: 1, name: '選手A', atBats: 10, hits: 2, version: 0 }, { id: 2, name: '選手B', atBats: 5, hits: 1, version: 0 }],
    fields: { atBats: true, hits: true, rbi: true, average: true }, fees: [], gears: [],
    dates: ['2026-09-05', '2026-09-06'], answers: [], year: 2026, month: '2026-09'
  };
  Object.assign(state, options.state);
  const calls = [];
  window.fetch = async (path, init) => {
    if (!init) return { ok: true, json: async () => structuredClone(state) };
    assert.equal(init.headers['X-CSRF-TOKEN'], 'test-token');
    const values = init.headers['Content-Type'] === 'application/json' ? JSON.parse(init.body) : Object.fromEntries(init.body); calls.push({ path, values });
    if (options.post) return options.post(path, values, state, calls);
    return { ok: true, json: async () => ({ version: Number(values.version) + 1, id: 7 }) };
  };
  window.eval(source);
  await until(() => window.document.querySelector('#loadStatus').textContent === '');
  const q = selector => window.document.querySelector(selector);
  const qa = selector => [...window.document.querySelectorAll(selector)];
  const change = (input, value) => {
    if (input.type === 'checkbox' || input.type === 'radio') input.checked = value;
    else input.value = value;
    input.dispatchEvent(new window.Event('input', { bubbles: true }));
    input.dispatchEvent(new window.Event('change', { bubbles: true }));
  };
  if (options.selectPlayer !== false && tab !== 'gear') {
    change(q('#inputPlayer'), '1');
    await tick();
  }
  return { dom, window, state, calls, q, qa, change };
}

test('player selection starts blank and only selected players can answer', async () => {
  const s = await screen('attendance', { selectPlayer: false });
  try {
    assert.equal(s.q('#inputPlayer').value, '');
    assert.equal(s.q('#inputPlayer option').textContent, '');
    assert.equal(s.q('.attendance-form'), null);
    assert.equal(s.q('.bulk-attendance input[type=checkbox]').disabled, true);
    assert.equal(s.calls.length, 0);
    s.change(s.q('#inputPlayer'), '2');
    await until(() => s.q('.attendance-form'));
    s.change(s.q('input[value="○"]'), true);
    await until(() => s.calls.length === 1);
    assert.equal(s.calls[0].values.playerId, '2');
    s.change(s.q('#inputPlayer'), '');
    await until(() => !s.q('.attendance-form'));
    assert.equal(s.q('#inputPlayer').value, '');
    assert.equal(s.q('.bulk-attendance input[type=checkbox]').disabled, true);
  } finally { s.dom.window.close(); }
});

test('stats autosave serializes rapid changes using the returned version', async () => {
  let release;
  const s = await screen('stats', { post: async (path, values, state, calls) => {
    if (calls.length === 1) await new Promise(resolve => { release = resolve; });
    return { ok: true, json: async () => ({ version: Number(values.version) + 1 }) };
  } });
  try {
    const [bats, hits] = s.qa('#inputContent input');
    s.change(bats, '20'); await until(() => s.calls.length === 1);
    s.change(hits, '5'); release();
    await until(() => s.calls.length === 2 && s.q('.attendance-form .save-state, #inputContent > .input-card .save-state').textContent === '保存済み');
    assert.equal(s.calls[1].values.version, '1');
    assert.equal(s.calls[1].values.hits, '7');
    assert.equal(s.calls[1].values.atBats, '30');
    assert.match(s.q('#inputContent').textContent, /0.233/);
  } finally { s.dom.window.close(); }
});

test('current batting corrections apply differences and cumulative values remain editable', async () => {
  const s=await screen('stats');
  try {
    const current=key=>s.q(`[data-kind="current"][data-stat="${key}"]`);
    const total=key=>s.q(`[data-kind="total"][data-stat="${key}"]`);
    const save=async(input,value)=>{const n=s.calls.length;s.change(input,value);await until(()=>s.calls.length===n+1&&s.q('.save-state').textContent==='保存済み');};
    assert.equal(total('atBats').value,'10');
    await save(current('atBats'),'4');assert.equal(total('atBats').value,'14');
    await save(current('hits'),'2');assert.equal(total('hits').value,'4');
    await save(current('rbi'),'3');assert.equal(total('rbi').value,'3');
    await save(current('hits'),'1');assert.equal(total('hits').value,'3');
    await save(total('atBats'),'20');assert.equal(total('atBats').value,'20');
    await save(current('rbi'),'2');assert.equal(total('rbi').value,'2');
    s.qa('button').find(b=>b.textContent==='次の試合分を入力').click();
    await until(()=>current('atBats').value==='0');
    assert.equal(total('atBats').value,'20');
    await save(current('atBats'),'3');assert.equal(total('atBats').value,'23');
  } finally {s.dom.window.close();}
});

test('compact calendar shows only target days as unanswered and tracks selected player answers', async () => {
  const s=await screen('attendance');
  try {
    const day=d=>s.q(`[data-answer-date="2026-09-${d}"]`);
    assert.equal(s.qa('.calendar-day').length,2);
    assert.equal(day('05').dataset.status,'unanswered');
    assert.equal(s.q('.attendance-editor select'),null);
    for(const status of ['○','×','△']){
      s.change(s.q(`input[value="${status}"]`),true);
      await until(()=>day('05').dataset.status===status);
      assert.equal(s.q('.attendance-form').dataset.status,status);
      assert.equal(day('05').querySelector('.calendar-answer').textContent,status);
    }
    day('06').click();await until(()=>day('06').getAttribute('aria-pressed')==='true');
    assert.equal(s.q('.attendance-form').dataset.status,'unanswered');
    s.change(s.q('#inputPlayer'),'2');await until(()=>day('05').dataset.status==='unanswered');
    assert.equal(day('05').querySelector('.calendar-answer').textContent,'−');
  } finally{s.dom.window.close();}
});

test('fee check moves the name between paid and unpaid without navigation', async () => {
  const s = await screen('fee');
  try {
    assert.equal(s.qa('.name-list')[0].textContent, '選手A選手B');
    s.change(s.q('#inputContent input[type=checkbox]'), true);
    await until(() => s.q('.attendance-form .save-state, #inputContent > .input-card .save-state').textContent === '保存済み');
    assert.equal(s.qa('.name-list')[1].textContent, '選手A　0円');
    assert.equal(s.qa('.name-list')[0].textContent, '選手B');
    s.change(s.q('#inputContent textarea'), '5000円');
    await until(() => s.calls.length === 2 && s.q('.attendance-form .save-state, #inputContent > .input-card .save-state').textContent === '保存済み');
    assert.equal(s.calls[1].values.version, '0');
    assert.equal(s.calls[1].values.comment, '5000円');
    s.change(s.q('#inputContent input[type=number]'), '5000');
    await until(() => s.calls.length === 3 && s.qa('.name-list')[1].textContent.includes('5,000円'));
    assert.equal(s.calls[2].values.amount, '5000');
    assert.equal(s.qa('.name-list')[0].textContent, '選手B');
    assert.equal(s.window.location.pathname, '/input');
  } finally { s.dom.window.close(); }
});

test('attendance saves cross and updates counts, names and comments', async () => {
  const s = await screen('attendance');
  try {
    assert.equal(s.qa('.attendance-person').length, 0);
    assert.match(s.q('.attendance-detail').textContent, /まだ回答がありません/);
    s.change(s.q('input[value="×"]'), true);
    await until(() => s.q('.attendance-form .save-state, #inputContent > .input-card .save-state').textContent === '保存済み');
    assert.match(s.q('.day-row').textContent, /× 1/);
    assert.equal(s.qa('.attendance-person').length, 1);
    assert.match(s.q('.attendance-detail').textContent, /選手A/);
    assert.doesNotMatch(s.q('.attendance-detail').textContent, /選手B/);
    s.change(s.q('#inputContent textarea'), '午前は不参加');
    await until(() => s.calls.length === 2 && s.q('.attendance-form .save-state, #inputContent > .input-card .save-state').textContent === '保存済み');
    assert.equal(s.q('.person-memo').textContent, '午前は不参加');
    s.q('.attendance-detail .day-nav button:last-child').click();
    await until(() => s.q('.attendance-detail .day-nav h2').textContent.includes('06'));
    assert.equal(s.q('#inputContent textarea').value, '');
  } finally { s.dom.window.close(); }
});

test('failure preserves input and retry saves it; switching players waits for saving', async () => {
  let fail = true;
  const s = await screen('fee', { post: async () => {
    if (fail) throw Error('offline');
    return { ok: true, json: async () => ({ version: 0 }) };
  } });
  try {
    s.change(s.q('#inputContent textarea'), '入力を保持');
    await until(() => !s.q('.retry').hidden);
    s.change(s.q('#inputPlayer'),'2'); await tick();
    assert.equal(s.q('#inputContent textarea').value, '入力を保持');
    fail = false; s.q('.retry').click();
    await until(() => s.q('.attendance-form .save-state, #inputContent > .input-card .save-state').textContent === '保存済み');
    s.change(s.q('#inputPlayer'),'2');
    await until(() => s.q('#inputContent').textContent.includes('2026年度・選手B'));
  } finally { s.dom.window.close(); }
});

test('new gear is created once and later edits use its id and version', async () => {
  const s = await screen('gear');
  try {
    s.change(s.q('#inputContent input'), 'バット');
    await until(() => s.q('.attendance-form .save-state, #inputContent > .input-card .save-state').textContent === '保存済み');
    s.change(s.q('#inputContent textarea'), '木製');
    await until(() => s.calls.length === 2 && s.q('.attendance-form .save-state, #inputContent > .input-card .save-state').textContent === '保存済み');
    assert.equal(s.calls[1].values.id, '7');
    assert.equal(s.q('.gear-shortcuts a').getAttribute('href'), '#gear-7');
    assert.ok(s.q('#gear-7'));
    assert.equal(s.calls[1].values.version, '0');
  } finally { s.dom.window.close(); }
});


test('attendance input sits before summary and supports month arrows across years', async () => {
  const s = await screen('attendance');
  try {
    const editor = s.q('.attendance-editor');
    assert.equal(editor.previousElementSibling.className, 'month-picker');
    assert.match(editor.nextElementSibling.textContent, /出欠確認/);
    const choice = s.q('.answer-option');
    choice.click();
    await until(() => s.q('.attendance-form .save-state, #inputContent > .input-card .save-state').textContent === '保存済み');
    assert.equal(s.q('.answer-radio:checked').value, '○');
    assert.equal(s.calls[0].values.status, '○');
    s.q('[aria-label="次の月"]').click();
    await until(() => s.q('.month-nav h2').textContent.includes('10月'));
    s.q('[aria-label="前の月"]').click();
    await until(() => s.q('.month-nav h2').textContent.includes('9月'));
    s.change(s.q('.month-picker select'), '2026-12');
    await until(() => s.q('.month-nav h2').textContent.includes('12月'));
    s.q('[aria-label="次の月"]').click();
    await until(() => s.q('.month-nav h2').textContent.includes('2027年1月'));
    assert.equal(s.q('#inputYear').value, '2027');
  } finally { s.dom.window.close(); }
});


test('common tabs return without adding a combined-input menu', async () => {
  const s=await screen('fee');
  try {
    s.q('[data-tab=attendance]').click();
    await until(()=>s.q('h1').textContent==='出欠確認');
    assert.equal(s.window.document.title,'出欠確認 | DazBones');
    assert.equal(s.window.location.pathname,'/survey/attendance');
    assert.equal(s.qa('[data-tab]').length,4);
  } finally {s.dom.window.close();}
});

test('bulk check saves the selected player month and updates counts and selection', async () => {
  const s=await screen('attendance',{post:async(path,v,state)=>{
    assert.equal(path,'/api/input/attendance/bulk');
    assert.equal(v.playerId,1);assert.equal(v.month,'2026-09');
    assert.deepEqual(v.versions,{'2026-09-05':-1,'2026-09-06':-1});
    return {ok:true,json:async()=>({answers:state.dates.map(date=>({date,playerId:v.playerId,status:v.status,memo:'',version:0}))})};
  }});
  try {
    s.change(s.q('.bulk-attendance select'),'△');
    s.change(s.q('.bulk-attendance input[type=checkbox]'),true);
    await until(()=>s.q('#loadStatus').textContent.includes('2日分'));
    assert.equal(s.calls.length,1);
    assert.equal(s.q('.answer-radio:checked').value,'△');
    s.qa('.day-counts').forEach(row=>assert.match(row.textContent,/△ 1/));
    assert.equal(s.q('.bulk-attendance input').checked,false);
    assert.equal(s.qa('.attendance-person').length,1);
  } finally {s.dom.window.close();}
});

test('bulk failure leaves existing answers unchanged and permits retry', async () => {
  const s=await screen('attendance',{post:async()=>({ok:false,status:409,json:async()=>({})})});
  try {
    s.change(s.q('.bulk-attendance select'),'○');s.change(s.q('.bulk-attendance input'),true);
    await until(()=>s.q('.bulk-attendance .save-state').classList.contains('error'));
    assert.equal(s.qa('.attendance-person').length,0);
    assert.equal(s.q('.bulk-attendance input').checked,false);
    assert.equal(s.q('.bulk-attendance input').disabled,false);
  } finally {s.dom.window.close();}
});


test('attendance highlights all best days, with circles before triangles, and updates after saving', async () => {
 const dates=['2026-09-05','2026-09-06','2026-09-12'];
 const answers=[{playerId:2,date:dates[0],status:'○',version:0},{playerId:2,date:dates[1],status:'○',version:0},{playerId:1,date:dates[1],status:'△',version:0},{playerId:1,date:dates[2],status:'△',version:0},{playerId:2,date:dates[2],status:'△',version:0}];
 const s=await screen('attendance',{state:{dates,answers}});
 try {
  const highlighted=()=>s.qa('.day-row').map((r,i)=>r.classList.contains('best-attendance-day')?i:-1).filter(i=>i>=0);
  assert.deepEqual(highlighted(),[1]);
  s.change(s.q('input[value="△"]'),true);
  await until(()=>highlighted().length===2);
  assert.deepEqual(highlighted(),[0,1]);
  s.change(s.q('input[value="○"]'),true);
  await until(()=>highlighted().length===1&&highlighted()[0]===0);
  assert.deepEqual(highlighted(),[0]);
 } finally {s.dom.window.close();}
});
test('unanswered days are not highlighted as most participants',async()=>{
 const s=await screen('attendance');
 try{assert.equal(s.qa('.best-attendance-day').length,0);}finally{s.dom.window.close();}
});
