/* ═══════════════════════════════════════════
   MediQueue — Global App JS
═══════════════════════════════════════════ */

// ── Clock ───────────────────────────────────────────────────
(function startClock() {
  function tick() {
    const el = document.getElementById('clock');
    if (el) {
      const now = new Date();
      el.textContent = now.toLocaleTimeString('en-GB', {
        hour: '2-digit', minute: '2-digit', second: '2-digit'
      });
    }
  }
  tick();
  setInterval(tick, 1000);
})();

// ── Toast notifications ─────────────────────────────────────
function showToast(message, type = 'info', duration = 3500) {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }
  const toast = document.createElement('div');
  toast.className = `toast toast--${type}`;
  toast.innerHTML = `<span>${message}</span>`;
  container.appendChild(toast);
  setTimeout(() => {
    toast.style.animation = 'none';
    toast.style.opacity   = '0';
    toast.style.transform = 'translateX(110%)';
    toast.style.transition = 'all .3s ease';
    setTimeout(() => toast.remove(), 300);
  }, duration);
}

// ── CSRF token helper ───────────────────────────────────────
function getCsrfToken() {
  const meta = document.querySelector('meta[name="_csrf"]');
  if (meta) return meta.content;
  // Try cookie
  const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : '';
}

// ── Sidebar toggle ──────────────────────────────────────────
function toggleSidebar() {
  document.getElementById('sidebar').classList.toggle('open');
}

// ── Auto-dismiss alerts ─────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.alert').forEach(el => {
    setTimeout(() => {
      el.style.transition = 'opacity .4s';
      el.style.opacity = '0';
      setTimeout(() => el.remove(), 400);
    }, 5000);
  });
});

// ── Confirm dialogs ─────────────────────────────────────────
function confirmDelete(message, action) {
  if (confirm(message || 'Are you sure? This cannot be undone.')) {
    if (typeof action === 'function') action();
    else if (typeof action === 'string') window.location.href = action;
  }
}

// ── Fetch with CSRF ─────────────────────────────────────────
async function apiPost(url, body) {
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-XSRF-TOKEN': getCsrfToken()
    },
    body: body ? JSON.stringify(body) : undefined
  });
  return res.json();
}

async function apiDelete(url) {
  const res = await fetch(url, {
    method: 'DELETE',
    headers: { 'X-XSRF-TOKEN': getCsrfToken() }
  });
  return res.json();
}
