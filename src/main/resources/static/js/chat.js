/* ═══════════════════════════════════════════
   MediQueue — Staff Chat WebSocket
═══════════════════════════════════════════ */

let chatStompClient = null;
let currentRoomId   = null;
let currentUsername = null;
let typingTimer     = null;
let isTypingSent    = false;

function initChat(roomId, username) {
  currentRoomId   = roomId;
  currentUsername = username;

  const socket = new SockJS('/ws');
  chatStompClient = Stomp.over(socket);
  chatStompClient.debug = null;

  chatStompClient.connect({}, function () {
    // Subscribe to room messages
    chatStompClient.subscribe('/topic/chat/' + roomId, function (frame) {
      const msg = JSON.parse(frame.body);
      appendMessage(msg);
      scrollToBottom();
    });

    // Subscribe to typing indicators
    chatStompClient.subscribe('/topic/chat/' + roomId + '/typing', function (frame) {
      const payload = JSON.parse(frame.body);
      if (payload.username !== currentUsername) {
        showTypingIndicator(payload.username, payload.typing);
      }
    });

    console.info('[MediQueue] Chat connected to room:', roomId);
  }, function (err) {
    console.warn('[MediQueue] Chat disconnected, retrying…', err);
    setTimeout(() => initChat(roomId, username), 4000);
  });
}

// ── Send a chat message ──────────────────────────────────────
function sendMessage() {
  const input   = document.getElementById('chat-input');
  const content = input.value.trim();
  if (!content || !chatStompClient?.connected) return;

  chatStompClient.send('/app/chat/' + currentRoomId, {}, JSON.stringify({ content }));
  input.value = '';
  sendTyping(false);
}

// ── Handle Enter key ─────────────────────────────────────────
function handleChatKey(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    sendMessage();
  }
}

// ── Typing indicator ─────────────────────────────────────────
function sendTyping(typing) {
  if (!chatStompClient?.connected) return;

  if (typing && !isTypingSent) {
    chatStompClient.send('/app/chat/' + currentRoomId + '/typing', {},
      JSON.stringify({ typing: true }));
    isTypingSent = true;
  }

  clearTimeout(typingTimer);
  if (typing) {
    typingTimer = setTimeout(() => {
      chatStompClient.send('/app/chat/' + currentRoomId + '/typing', {},
        JSON.stringify({ typing: false }));
      isTypingSent = false;
    }, 2000);
  } else {
    if (isTypingSent) {
      chatStompClient.send('/app/chat/' + currentRoomId + '/typing', {},
        JSON.stringify({ typing: false }));
      isTypingSent = false;
    }
  }
}

function showTypingIndicator(username, isTyping) {
  const indicator = document.getElementById('typing-indicator');
  const text      = document.getElementById('typing-text');
  if (!indicator) return;

  if (isTyping) {
    text.textContent = username + ' is typing…';
    indicator.style.display = 'flex';
    scrollToBottom();
  } else {
    indicator.style.display = 'none';
  }
}

// ── Append a new message to the chat feed ───────────────────
function appendMessage(msg) {
  const container = document.getElementById('chat-messages');
  if (!container) return;

  // Remove empty state if present
  const empty = container.querySelector('.chat-empty');
  if (empty) empty.remove();

  const isOwn = (msg.senderName === currentUsername);

  const div = document.createElement('div');
  div.className = `chat-msg${isOwn ? ' chat-msg--own' : ''}`;

  const time = msg.sentAt
    ? new Date(msg.sentAt).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })
    : new Date().toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });

  div.innerHTML = `
    <div class="msg-avatar">${msg.avatarInitials || initials(msg.senderName)}</div>
    <div class="msg-bubble">
      <div class="msg-meta">
        <span class="msg-sender">${escapeHtml(msg.senderName)}</span>
        <span class="msg-time">${time}</span>
      </div>
      <div class="msg-content">${escapeHtml(msg.content)}</div>
    </div>
  `;

  container.appendChild(div);
}

// ── Scroll to bottom of messages ─────────────────────────────
function scrollToBottom() {
  const container = document.getElementById('chat-messages');
  if (container) {
    setTimeout(() => {
      container.scrollTop = container.scrollHeight;
    }, 50);
  }
}

// ── Helpers ──────────────────────────────────────────────────
function initials(name) {
  if (!name) return '?';
  const parts = name.split(' ');
  return parts.length >= 2
    ? (parts[0][0] + parts[1][0]).toUpperCase()
    : name.substring(0, 2).toUpperCase();
}

function escapeHtml(str) {
  if (!str) return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// ── Cleanup ──────────────────────────────────────────────────
window.addEventListener('beforeunload', () => {
  if (chatStompClient?.connected) {
    chatStompClient.disconnect();
  }
});
