const puppeteer = require('puppeteer');
const path = require('path');

const BASE = 'http://localhost:8080';
const OUT  = __dirname;

/**
 * Log in via the REST API (no CSRF required), extract the JWT from the
 * Set-Cookie header, then inject it into the Puppeteer page's cookie jar.
 */
async function loginViaApi(page, username, password) {
  // Hit the API login endpoint from Node (not from the browser page)
  const http = require('http');
  const body = JSON.stringify({ username, password });

  const jwt = await new Promise((resolve, reject) => {
    const req = http.request({
      hostname: 'localhost',
      port: 8080,
      path: '/api/auth/login',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(body),
      },
    }, (res) => {
      let raw = '';
      res.on('data', chunk => raw += chunk);
      res.on('end', () => {
        // Extract token from JSON body
        try {
          const json = JSON.parse(raw);
          resolve(json.data?.token || null);
        } catch { resolve(null); }
      });
    });
    req.on('error', reject);
    req.write(body);
    req.end();
  });

  if (!jwt) throw new Error(`Login failed for ${username}`);

  // Navigate to any page first so the domain is set, then plant the cookie
  await page.goto(`${BASE}/auth/login`, { waitUntil: 'networkidle2' });
  await page.setCookie({
    name: 'HQ_TOKEN',
    value: jwt,
    domain: 'localhost',
    path: '/',
    httpOnly: true,
    sameSite: 'Lax',
  });
  console.log(`  ✓ logged in as ${username}`);
}

async function shot(page, name, url, waitSel) {
  await page.goto(`${BASE}${url}`, { waitUntil: 'networkidle2' });
  if (waitSel) {
    await page.waitForSelector(waitSel, { timeout: 8000 }).catch(() => {});
  }
  // Small settle pause for WebSocket / JS rendering
  await new Promise(r => setTimeout(r, 800));
  await page.screenshot({ path: path.join(OUT, `${name}.png`), fullPage: true });
  console.log(`  ✓ ${name}.png`);
}

(async () => {
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
    defaultViewport: { width: 1440, height: 900 },
  });

  // ── 1. Login page (unauthenticated) ──────────────────────────────────────────
  console.log('\n[Public pages]');
  {
    const page = await browser.newPage();
    await page.goto(`${BASE}/auth/login`, { waitUntil: 'networkidle2' });
    await new Promise(r => setTimeout(r, 500));
    await page.screenshot({ path: path.join(OUT, '01-login.png'), fullPage: true });
    console.log('  ✓ 01-login.png');

    await page.goto(`${BASE}/auth/register`, { waitUntil: 'networkidle2' });
    await new Promise(r => setTimeout(r, 500));
    await page.screenshot({ path: path.join(OUT, '02-register.png'), fullPage: true });
    console.log('  ✓ 02-register.png');
    await page.close();
  }

  // ── ADMIN session ─────────────────────────────────────────────────────────────
  console.log('\n[Admin]');
  {
    const page = await browser.newPage();
    await loginViaApi(page, 'admin', 'Password@123');

    await shot(page, '03-admin-dashboard',   '/admin/dashboard',   '.stat-card, table, h1');
    await shot(page, '04-admin-users',       '/admin/users',       'table');
    await shot(page, '05-admin-departments', '/admin/departments', 'table');
    await shot(page, '06-admin-audit-logs',  '/admin/audit-logs',  'table');
    await shot(page, '07-reports',           '/reports',           'form, .card');
    await shot(page, '08-chat',              '/chat',              '#chat-messages, .messages-feed, #messages');
    await page.close();
  }

  // ── RECEPTIONIST session ──────────────────────────────────────────────────────
  console.log('\n[Receptionist]');
  {
    const page = await browser.newPage();
    await loginViaApi(page, 'rec.aisha', 'Password@123');

    await shot(page, '09-receptionist-dashboard',        '/receptionist/dashboard',        'table, h1');
    await shot(page, '10-receptionist-register-patient', '/receptionist/register-patient', 'form');
    await page.close();
  }

  // ── DOCTOR session ────────────────────────────────────────────────────────────
  console.log('\n[Doctor]');
  {
    const page = await browser.newPage();
    await loginViaApi(page, 'dr.adewale', 'Password@123');

    await shot(page, '11-doctor-dashboard', '/doctor/dashboard', 'h1, .queue-card, .profile-card');
    await shot(page, '12-doctor-queue',     '/doctor/queue',     'h1, .queue-card, .empty-state');
    await page.close();
  }

  await browser.close();
  console.log('\n✅ All screenshots saved to docs/screenshots/');
})().catch(err => { console.error(err); process.exit(1); });
