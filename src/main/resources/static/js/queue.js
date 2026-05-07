/* ═══════════════════════════════════════════
   MediQueue — Real-Time Queue WebSocket
═══════════════════════════════════════════ */

let queueStompClient = null;

function connectQueueSocket(departmentId) {
  if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
    loadSockJS(() => loadStomp(() => _connectQueueSocket(departmentId)));
    return;
  }
  _connectQueueSocket(departmentId);
}

function _connectQueueSocket(departmentId) {
  const socket = new SockJS('/ws');
  queueStompClient = Stomp.over(socket);
  queueStompClient.debug = null; // silence debug logs

  queueStompClient.connect({}, function () {
    // Subscribe to department-specific queue
    if (departmentId && departmentId !== 'global') {
      queueStompClient.subscribe('/topic/queue/' + departmentId, function (frame) {
        const payload = JSON.parse(frame.body);
        handleQueueUpdate(payload);
      });
    }

    // Always subscribe to global queue events
    queueStompClient.subscribe('/topic/queue/global', function (frame) {
      const payload = JSON.parse(frame.body);
      handleQueueUpdate(payload);
    });

    // Doctor status updates
    queueStompClient.subscribe('/topic/doctor-status', function (frame) {
      const payload = JSON.parse(frame.body);
      handleDoctorStatusUpdate(payload);
    });

    console.info('[MediQueue] Queue WebSocket connected');
  }, function (error) {
    console.warn('[MediQueue] Queue WebSocket disconnected, retrying in 5s…', error);
    setTimeout(() => _connectQueueSocket(departmentId), 5000);
  });
}

function handleQueueUpdate(payload) {
  const { event, departmentId } = payload;

  // Refresh queue counts
  refreshQueueStats();

  // Flash visual indicator
  const boards = document.querySelectorAll('#queue-board, #queue-tbody');
  boards.forEach(b => {
    b.style.transition = 'opacity .2s';
    b.style.opacity = '.6';
    setTimeout(() => { b.style.opacity = '1'; }, 300);
  });

  // Show toast for new patients
  if (event === 'PATIENT_ADDED') {
    showToast('New patient added to queue', 'info', 2500);
  } else if (event === 'STATUS_CHANGED') {
    refreshQueueStats();
  }
}

function handleDoctorStatusUpdate(payload) {
  const { doctorId, newStatus, statusLabel } = payload;

  // Update doctor card if visible on page
  const card = document.querySelector(`[data-doctor-id="${doctorId}"]`);
  if (card) {
    // Remove old status classes
    card.classList.remove(
      'doctor-card--available', 'doctor-card--busy',
      'doctor-card--break', 'doctor-card--offline'
    );
    const classMap = {
      AVAILABLE: 'doctor-card--available',
      BUSY:      'doctor-card--busy',
      ON_BREAK:  'doctor-card--break',
      OFFLINE:   'doctor-card--offline'
    };
    if (classMap[newStatus]) card.classList.add(classMap[newStatus]);

    const badge = card.querySelector('.status-badge');
    if (badge) {
      badge.className = `status-badge ${newStatus}`;
      badge.textContent = statusLabel;
    }
  }
}

async function refreshQueueStats() {
  try {
    // Update waiting/in-progress counters if displayed on page
    const waitingEl    = document.getElementById('stat-waiting');
    const inProgressEl = document.getElementById('stat-inprogress');
    const waitCountEl  = document.getElementById('waiting-count');
    const ipCountEl    = document.getElementById('inprogress-count');
    const queueCount   = document.getElementById('queue-count');

    // We'll just refresh the visible numbers by re-reading the board
    const rows = document.querySelectorAll('#queue-tbody tr, .queue-card');
    let waiting = 0, inProgress = 0;

    rows.forEach(row => {
      const text = row.textContent || '';
      if (text.includes('WAITING'))     waiting++;
      if (text.includes('IN_PROGRESS')) inProgress++;
    });

    if (waitingEl)    waitingEl.textContent   = waiting;
    if (inProgressEl) inProgressEl.textContent = inProgress;
    if (waitCountEl)  waitCountEl.textContent  = waiting;
    if (ipCountEl)    ipCountEl.textContent    = inProgress;
    if (queueCount)   queueCount.textContent   = rows.length + ' patients';
  } catch (e) {
    // silently fail — not critical
  }
}

// ── Lazy-load SockJS and STOMP if not already present ───────
function loadSockJS(callback) {
  if (typeof SockJS !== 'undefined') { callback(); return; }
  const s = document.createElement('script');
  s.src = 'https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.6.1/sockjs.min.js';
  s.onload = callback;
  document.head.appendChild(s);
}

function loadStomp(callback) {
  if (typeof Stomp !== 'undefined') { callback(); return; }
  const s = document.createElement('script');
  s.src = 'https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js';
  s.onload = callback;
  document.head.appendChild(s);
}

// ── Cleanup on page unload ───────────────────────────────────
window.addEventListener('beforeunload', () => {
  if (queueStompClient && queueStompClient.connected) {
    queueStompClient.disconnect();
  }
});
