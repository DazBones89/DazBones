(() => {
  'use strict';
  const menu = document.getElementById('mobileMenu');
  const toggle = document.getElementById('menuToggle');
  const closeMenu = () => { menu.classList.add('hidden'); toggle.setAttribute('aria-expanded', 'false'); };
  toggle.addEventListener('click', () => {
    const open = menu.classList.contains('hidden');
    menu.classList.toggle('hidden', !open); toggle.setAttribute('aria-expanded', String(open));
    if (open) menu.querySelector('a,button')?.focus();
  });
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && !menu.classList.contains('hidden')) { closeMenu(); toggle.focus(); }
  });
  document.addEventListener('click', e => { if (!menu.contains(e.target) && !toggle.contains(e.target)) closeMenu(); });
  const path = location.pathname.replace(/\/$/, '') || '/';
  document.querySelectorAll('nav a[href]').forEach(link => {
    const target = new URL(link.href).pathname;
    const current = target === '/' ? path === '/' || path === '/main' : path === target;
    if (current) link.setAttribute('aria-current', 'page');
  });
  // Associate existing form labels with their next visible control, including repeated row forms.
  document.querySelectorAll('label:not([for])').forEach((label, index) => {
    if (label.querySelector('input,select,textarea')) return;
    let next = label.nextElementSibling;
    while (next && !next.matches('label')) {
      const control = next.matches('input:not([type=hidden]),select,textarea') ? next : next.querySelector('input:not([type=hidden]),select,textarea');
      if (control) { if (!control.id) control.id = 'field-' + index; label.htmlFor = control.id; break; }
      next = next.nextElementSibling;
    }
  });
  let activeDialog = null, returnFocus = null;
  const focusable = dialog => [...dialog.querySelectorAll('button,input,select,textarea,a[href],[tabindex="0"]')].filter(el => !el.disabled && el.getClientRects().length);
  const updateDialog = dialog => {
    const open = !dialog.classList.contains('hidden');
    if (open && activeDialog !== dialog) {
      returnFocus = document.activeElement; activeDialog = dialog;
      document.body.style.overflow = 'hidden';
      document.querySelector('header').inert = true;
      document.querySelector('.db-bottom-nav').inert = true;
      (focusable(dialog)[0] || dialog).focus();
    } else if (!open && activeDialog === dialog) {
      activeDialog = null; document.body.style.overflow = '';
      document.querySelector('header').inert = false;
      document.querySelector('.db-bottom-nav').inert = false;
      if (returnFocus?.isConnected) returnFocus.focus();
    }
  };
  document.querySelectorAll('[role="dialog"]').forEach(dialog => {
    dialog.tabIndex = -1;
    new MutationObserver(() => updateDialog(dialog)).observe(dialog, {attributes:true,attributeFilter:['class']});
    dialog.addEventListener('keydown', e => {
      if (e.key === 'Escape') { if (dialog.id === 'surveyModal') window.closeSurveyModal(); else window.closeScheduleModal(); }
      if (e.key !== 'Tab') return;
      const controls = focusable(dialog), first = controls[0], last = controls[controls.length - 1];
      if (!first) { e.preventDefault(); return; }
      if (e.shiftKey && (document.activeElement === first || document.activeElement === dialog)) { e.preventDefault(); last.focus(); }
      else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
    });
  });
})();
