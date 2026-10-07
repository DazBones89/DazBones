(() => {
  const role = document.getElementById('codeRole');
  const label = document.getElementById('currentCodeLabel');
  const current = document.getElementById('currentCode');
  if (!role || !label || !current) return;
  const update = () => {
    label.textContent = role.value === 'player' ? '現在の選手コード' : '現在のmasterコード';
  };
  role.addEventListener('change', () => { current.value = ''; update(); });
  update();
})();
