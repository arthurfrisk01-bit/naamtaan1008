// naamtaan1008-app smoke tests — static shell integrity + contract guards.
// Run: npm test (node --test)
'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const path = require('path');

const ROOT = path.join(__dirname, '..');
const read = (p) => fs.readFileSync(path.join(ROOT, p), 'utf8');
const exists = (p) => fs.existsSync(path.join(ROOT, p));

test('shell files exist', () => {
  for (const f of ['index.html', 'manifest.json', 'sw.js', 'css/style.css', 'js/app.js',
    'icons/icon-192.png', 'icons/icon-512.png', 'icons/icon-maskable-512.png', 'icons/apple-touch-icon.png']) {
    assert.ok(exists(f), 'missing ' + f);
  }
});

test('manifest.json is valid and icons resolve', () => {
  const m = JSON.parse(read('manifest.json'));
  assert.equal(m.name, '平和日常 naamtaan1008');
  assert.equal(m.display, 'standalone');
  for (const icon of m.icons) {
    assert.ok(exists(icon.src.replace(/^\//, '')), 'missing icon ' + icon.src);
  }
});

test('twa-manifest.json is valid for the app domain', () => {
  const m = JSON.parse(read('twa-manifest.json'));
  assert.equal(m.host, 'app.naamtaan1008.com');
  assert.equal(m.packageId, 'com.naamtaan1008.client');
  assert.ok(m.signingKey && m.signingKey.path);
});

test('index.html references only existing assets', () => {
  const html = read('index.html');
  const refs = [...html.matchAll(/(?:src|href)="\/([^"?]+)(?:\?[^"]*)?"/g)].map(m => m[1]);
  for (const r of refs) {
    assert.ok(exists(r), 'index.html references missing asset: /' + r);
  }
});

test('app.js renders all routes and avoids strict-mode hazards', () => {
  const js = read('js/app.js');
  for (const fn of ['renderHome', 'renderShows', 'renderShowDetail', 'renderScene',
    'renderSceneDetail', 'renderArticles', 'renderArticleDetail', 'renderMore',
    'renderAbout', 'renderRecruitment', 'renderProducts', 'renderContact', 'renderInstructions']) {
    assert.ok(js.includes(fn), 'missing function ' + fn);
  }
  assert.ok(!js.includes('arguments.callee'), 'arguments.callee forbidden in strict mode');
  assert.ok(js.includes("'use strict'"));
  // All tab routes are wired in router()
  for (const r of ['/shows', '/scene', '/articles', '/more', '/about', '/recruitment', '/products', '/contact', '/instructions']) {
    assert.ok(js.includes("top === '" + r + "'"), 'route missing: ' + r);
  }
});

test('sw.js caches the shell and never caches API writes', () => {
  const sw = read('sw.js');
  assert.ok(sw.includes("const CACHE = 'naamtaan1008-app-v1'"));
  assert.ok(sw.includes("req.method !== 'GET'"));
  assert.ok(sw.includes("url.pathname.startsWith('/api/')"));
  assert.ok(sw.includes('skipWaiting'));
});
