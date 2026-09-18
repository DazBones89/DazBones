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
    fields: { atBats: true, hits: true, average: true }, fees: [], gears: [],
    dates: ['2026-09-05', '2026-09-06'], answers: [], year: 2026, month: '2026-09'
  };
  const calls = [];
  window.fetch = async (path, init) => {
    if (!init) return { ok: true, json: async () => structuredClone(state) };
    assert.equal(init.headers['X-CSRF-TOKEN'], 'test-token');
    const values = Object.fromEntries(init.body); calls.push({ path, values });
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
  return { dom, window, state, calls, q, qa, change };
}

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
    await until(() => s.calls.length === 2 && s.q('.save-state').textContent === '保存済み');
    assert.equal(s.calls[1].values.version, '1');
    assert.equal(s.calls[1].values.hits, '5');
    assert.match(s.q('#inputContent').textContent, /0.250/);
  } finally { s.dom.window.close(); }
});

test('fee check moves the name between paid and unpaid without navigation', async () => {
  const s = await screen('fee');
  try {
    assert.equal(s.qa('.name-list')[1].textContent, '選手A選手B');
    s.change(s.q('#inputContent input[type=checkbox]'), true);
    await until(() => s.q('.save-state').textContent === '保存済み');
    assert.equal(s.qa('.name-list')[0].textContent, '選手A');
    assert.equal(s.qa('.name-list')[1].textContent, '選手B');
    s.change(s.q('#inputContent textarea'), '5000円');
    await until(() => s.calls.length === 2 && s.q('.save-state').textContent === '保存済み');
    assert.equal(s.calls[1].values.version, '0');
    assert.equal(s.calls[1].values.comment, '5000円');
    assert.equal(s.window.location.pathname, '/input');
  } finally { s.dom.window.close(); }
});

test('attendance saves cross and updates counts, names and comments', async () => {
  const s = await screen('attendance');
  try {
    assert.equal(s.qa('.attendance-person').length, 0);
    assert.match(s.q('.attendance-detail').textContent, /まだ回答がありません/);
    s.change(s.q('input[value="×"]'), true);
    await until(() => s.q('.save-state').textContent === '保存済み');
    assert.match(s.q('.day-row').textContent, /× 1/);
    assert.equal(s.qa('.attendance-person').length, 1);
    assert.match(s.q('.attendance-detail').textContent, /選手A/);
    assert.doesNotMatch(s.q('.attendance-detail').textContent, /選手B/);
    s.change(s.q('#inputContent textarea'), '午前は不参加');
    await until(() => s.calls.length === 2 && s.q('.save-state').textContent === '保存済み');
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
    await until(() => s.q('.save-state').textContent === '保存済み');
    s.change(s.q('#inputPlayer'),'2');
    await until(() => s.q('#inputContent').textContent.includes('2026年度・選手B'));
  } finally { s.dom.window.close(); }
});

test('new gear is created once and later edits use its id and version', async () => {
  const s = await screen('gear');
  try {
    s.change(s.q('#inputContent input'), 'バット');
    await until(() => s.q('.save-state').textContent === '保存済み');
    s.change(s.q('#inputContent textarea'), '木製');
    await until(() => s.calls.length === 2 && s.q('.save-state').textContent === '保存済み');
    assert.equal(s.calls[1].values.id, '7');
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
    await until(() => s.q('.save-state').textContent === '保存済み');
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
