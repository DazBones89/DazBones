(() => {
  const mode = document.getElementById('playerMode');
  const picker = document.getElementById('editPlayerPicker');
  const player = document.getElementById('editPlayer');
  const form = document.querySelector('form[data-player-form]');
  if (!mode || !picker || !player || !form) return;
  let dirty = false;
  form.addEventListener('input', () => { dirty = true; });
  const confirmChange = () => !dirty || confirm('未保存の入力があります。画面を切り替えますか？');
  const editing = !!player.value;
  const original = player.value;
  mode.addEventListener('change', () => {
    if (!confirmChange()) { mode.value = editing ? 'edit' : 'add'; return; }
    if (mode.value === 'add' && editing) { location.assign('/players/add'); return; }
    picker.hidden = mode.value !== 'edit';
    form.hidden = mode.value === 'edit' && !player.value;
  });
  player.addEventListener('change', () => {
    if (!confirmChange()) { player.value = original; return; }
    if (player.value) location.assign('/players/' + encodeURIComponent(player.value) + '/edit');
    else form.hidden = true;
  });
})();
