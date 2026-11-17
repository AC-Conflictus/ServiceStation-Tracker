// Let loaded observations expire without reloading or reserving a place.
(() => {
  document.querySelectorAll('[data-fresh-until]').forEach(card => {
    const expires = Number(card.dataset.freshUntil);
    if (!Number.isFinite(expires) || expires <= 0) return;
    const expire = () => {
      const label = card.querySelector('[data-availability-label]');
      label.textContent = 'Availability may be out of date';
      label.classList.remove('text-bg-success', 'text-bg-danger');
      label.classList.add('text-bg-secondary');
      card.querySelector('[data-availability-detail]').textContent = card.dataset.staleDetail;
      const link = card.querySelector('[data-registration-link]');
      const blocked = card.querySelector('[data-registration-blocked]');
      if (link) { link.hidden = false; link.textContent = 'Check registration form'; }
      if (blocked) blocked.hidden = true;
    };
    const delay = expires - Date.now();
    if (delay <= 0) expire();
    else setTimeout(expire, delay + 1);
  });
})();
