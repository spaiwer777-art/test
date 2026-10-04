#pragma once
// Web UI served at "/". Parsing (RTTTL, MIDI) runs in the browser; the board
// only receives "freq,ms;freq,ms;..." pairs.

const char INDEX_HTML[] PROGMEM = R"HTML(<!doctype html>
<html lang="ru">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>ESP32 Melody</title>
<style>
:root{--bg:#f4f5f7;--card:#fff;--text:#1d2129;--muted:#6b7280;--line:#e3e5e8;--accent:#2563eb;--accent-text:#fff;--ok:#15803d;--err:#b91c1c}
@media (prefers-color-scheme:dark){:root{--bg:#111318;--card:#1b1e25;--text:#e8eaed;--muted:#9aa0a6;--line:#2c313a;--accent:#4f8cff;--accent-text:#0b1020;--ok:#4ade80;--err:#f87171}}
*{box-sizing:border-box}
[hidden]{display:none!important}
body{margin:0;background:var(--bg);color:var(--text);font:15px/1.45 system-ui,-apple-system,Segoe UI,Roboto,sans-serif}
main{max-width:640px;margin:0 auto;padding:16px}
h1{font-size:20px;margin:4px 0 14px}
.card{background:var(--card);border:1px solid var(--line);border-radius:12px;padding:14px;margin-bottom:14px}
.card h2{font-size:14px;margin:0 0 10px;color:var(--muted);font-weight:600;text-transform:uppercase;letter-spacing:.04em}
.now{font-size:17px;font-weight:600;word-break:break-word}
.sub{color:var(--muted);font-size:13px}
.bar{height:6px;background:var(--line);border-radius:3px;overflow:hidden;margin:10px 0}
.bar>div{height:100%;width:0;background:var(--accent);transition:width .3s}
.row{display:flex;gap:8px;flex-wrap:wrap;align-items:center}
.row+.row{margin-top:10px}
button{font:inherit;border:1px solid var(--line);background:var(--card);color:var(--text);border-radius:8px;padding:8px 14px;cursor:pointer}
button.primary{background:var(--accent);border-color:var(--accent);color:var(--accent-text);font-weight:600}
button:disabled{opacity:.5;cursor:default}
textarea,input[type=text],input[type=number],select{font:inherit;color:var(--text);background:var(--bg);border:1px solid var(--line);border-radius:8px;padding:8px;width:100%}
textarea{min-height:110px;font-family:ui-monospace,Menlo,Consolas,monospace;font-size:13px;resize:vertical}
input[type=number]{width:90px}
input[type=range]{flex:1;min-width:120px}
label{display:flex;gap:6px;align-items:center}
.presets{display:flex;gap:6px;flex-wrap:wrap}
.presets button{padding:6px 10px;font-size:13px}
.msg{font-size:13px;min-height:1.4em;margin-top:8px}
.msg.ok{color:var(--ok)}.msg.err{color:var(--err)}
details{font-size:13px;color:var(--muted)}
code{font-family:ui-monospace,Menlo,Consolas,monospace;font-size:12px}
</style>
</head>
<body>
<main>
<h1>🎵 ESP32-S3 Melody</h1>

<section class="card">
  <h2>Сейчас на плате</h2>
  <div class="now" id="stName">—</div>
  <div class="sub" id="stInfo">подключение…</div>
  <div class="bar"><div id="stBar"></div></div>
  <div class="row">
    <button class="primary" id="btnReplay">▶ Играть</button>
    <button id="btnStop">■ Стоп</button>
    <button id="btnDefault">↺ К Элизе</button>
  </div>
  <div class="row">
    <span>Громкость</span><input type="range" id="vol" min="1" max="100" value="50"><span id="volVal">50%</span>
  </div>
  <div class="row">
    <label><input type="checkbox" id="loop" checked> Повторять</label>
    <label>пауза <input type="number" id="pause" min="0" max="10000" step="100" value="1000"> мс</label>
  </div>
</section>

<section class="card">
  <h2>Своя мелодия</h2>
  <div class="row">
    <input type="file" id="file" accept=".rtttl,.rtx,.txt,.mid,.midi">
  </div>
  <div class="row" id="trackRow" hidden>
    <label style="flex:1">Дорожка MIDI <select id="track"></select></label>
  </div>
  <div class="row">
    <textarea id="src" spellcheck="false" placeholder="Вставьте RTTTL, например:&#10;Nokia:d=4,o=5,b=225:8e6,8d6,f#,g#,8c#6,8b,d,e,8b,8a,c#,e,2a"></textarea>
  </div>
  <div class="row">
    <span class="sub">Пресеты:</span>
    <div class="presets" id="presets"></div>
  </div>
  <div class="row">
    <button id="btnPreview">🔊 Прослушать здесь</button>
    <button class="primary" id="btnSend">⬆ Играть на ESP32</button>
  </div>
  <div class="row">
    <label><input type="checkbox" id="save" checked> Сохранить в плату (играть при включении)</label>
  </div>
  <div class="msg" id="msg"></div>
  <details>
    <summary>Какие форматы подходят</summary>
    <p><b>RTTTL</b> (рингтоны Nokia): <code>Имя:d=4,o=5,b=120:8e6,8d#6,…</code>. Тысячи готовых мелодий находятся поиском «rtttl collection».</p>
    <p><b>MIDI</b> (.mid): из нескольких голосов берётся самая верхняя нота (обычно это мелодия). Ударные пропускаются. Если звучит не то, выберите отдельную дорожку.</p>
    <p>Пьезо/динамик играет один голос, на плату влезает до <span id="maxN">1500</span> нот.</p>
    <p><b>Защита платы:</b> ток пина ограничен в прошивке (~10 мА), громкость не выше 50 % ШИМ, ноты вне 100–8000 Гц переносятся по октаве, длина ноты 10 мс – 10 с, пауза до 10 с.</p>
  </details>
</section>
</main>

<script>
const $ = id => document.getElementById(id);
const PRESETS = {
  "К Элизе": "FurElise:d=8,o=5,b=125:32p,e6,d#6,e6,d#6,e6,b,d6,c6,4a.,32p,c,e,a,4b.,32p,e,g#,b,4c.6,32p,e,e6,d#6,e6,d#6,e6,b,d6,c6,4a.,32p,c,e,a,4b.,32p,d,c6,b,2a",
  "Nokia": "Nokia:d=4,o=5,b=225:8e6,8d6,f#,g#,8c#6,8b,d,e,8b,8a,c#,e,2a",
  "Mario": "Mario:d=4,o=5,b=100:16e6,16e6,32p,8e6,16c6,8e6,8g6,8p,8g,8p,8c6,16p,8g,16p,8e,16p,8a,8b,16a#,8a,16g.,16e6,16g6,8a6,16f6,8g6,8e6,16c6,16d6,8b,16p,8c6,16p,8g,16p,8e,16p,8a,8b,16a#,8a,16g.,16e6,16g6,8a6,16f6,8g6,8e6,16c6,16d6,8b",
  "Тетрис": "Tetris:d=4,o=5,b=160:e6,8b,8c6,8d6,16e6,16d6,8c6,8b,a,8a,8c6,e6,8d6,8c6,b,8b,8c6,d6,e6,c6,a,2a,8p,d6,8f6,a6,8g6,8f6,e6,8e6,8c6,e6,8d6,8c6,b,8b,8c6,d6,e6,c6,a,a",
  "Имперский марш": "Imperial:d=4,o=5,b=100:e,e,e,8c,16p,16g,e,8c,16p,16g,e,p,b,b,b,8c6,16p,16g,d#,8c,16p,16g,e,8p",
  "С днём рождения": "HappyBday:d=4,o=5,b=125:8g.,16g,a,g,c6,2b,8g.,16g,a,g,d6,2c6,8g.,16g,g6,e6,c6,b,a,8f6.,16f6,e6,c6,d6,2c6"
};

let midiData = null;   // parsed MIDI {name, tracks:[{name, segs}]}
// Safety limits; the real values come from the board in /api/status.
let lim = { maxNotes: 1500, maxVol: 100, minFreq: 100, maxFreq: 8000, minNoteMs: 10, maxNoteMs: 10000, maxPause: 10000 };

function msg(text, kind) { const m = $("msg"); m.textContent = text; m.className = "msg " + (kind || ""); }
const midiToFreq = n => Math.round(440 * Math.pow(2, (n - 69) / 12));
const fmtTime = ms => { const s = Math.round(ms / 1000); return Math.floor(s / 60) + ":" + String(s % 60).padStart(2, "0"); };

// ---------- RTTTL ----------
function parseRTTTL(text) {
  const parts = text.trim().split(":");
  if (parts.length < 3) throw new Error("RTTTL должен иметь вид Имя:d=4,o=5,b=120:ноты");
  const name = parts[0].trim() || "RTTTL";
  let d = 4, o = 6, b = 63;
  parts[1].replace(/\s/g, "").split(",").forEach(kv => {
    const [k, v] = kv.split("=");
    const n = parseInt(v, 10);
    if (isNaN(n)) return;
    if (k.toLowerCase() === "d") d = n;
    if (k.toLowerCase() === "o") o = n;
    if (k.toLowerCase() === "b") b = n;
  });
  const whole = 60000 / b * 4;
  const semis = { c: 0, d: 2, e: 4, f: 5, g: 7, a: 9, b: 11, h: 11 };
  const notes = [];
  parts.slice(2).join(":").replace(/\s/g, "").split(",").forEach(tok => {
    if (!tok) return;
    const m = tok.toLowerCase().match(/^(\d{1,2})?([a-hp])(#?)(\.?)(\d)?(#?)(\.?)$/);
    if (!m) throw new Error("Не понял ноту «" + tok + "»");
    let ms = whole / (m[1] ? parseInt(m[1], 10) : d);
    if (m[4] || m[7]) ms *= 1.5;
    let freq = 0;
    if (m[2] !== "p") {
      const oct = m[5] ? parseInt(m[5], 10) : o;
      const midi = 12 * (oct + 1) + semis[m[2]] + (m[3] || m[6] ? 1 : 0);
      freq = midiToFreq(midi);
    }
    notes.push([freq, Math.round(ms)]);
  });
  if (!notes.length) throw new Error("В RTTTL нет нот");
  return { name, notes };
}

// ---------- MIDI ----------
function parseMIDI(buf) {
  const v = new DataView(buf);
  let p = 0;
  const str = n => { let s = ""; for (let i = 0; i < n; i++) s += String.fromCharCode(v.getUint8(p + i)); return s; };
  if (str(4) !== "MThd") throw new Error("Это не MIDI-файл");
  const hdrLen = v.getUint32(4);
  const ntrk = v.getUint16(10), div = v.getUint16(12);
  p = 8 + hdrLen;
  const tempos = [];  // [tick, usPerQuarter]
  const tracks = [];
  for (let t = 0; t < ntrk && p + 8 <= buf.byteLength; t++) {
    if (str(4) !== "MTrk") break;
    const len = v.getUint32(p + 4);
    let q = p + 8; const end = Math.min(q + len, buf.byteLength);
    p = end;
    const vlq = () => { let n = 0, c; do { c = v.getUint8(q++); n = (n << 7) | (c & 0x7f); } while (c & 0x80); return n; };
    let tick = 0, status = 0, name = "";
    const open = {}, segs = [];
    while (q < end) {
      tick += vlq();
      let b = v.getUint8(q);
      if (b & 0x80) { status = b; q++; }
      const type = status & 0xf0, ch = status & 0x0f;
      if (status === 0xff) {
        const mt = v.getUint8(q++), ml = vlq();
        if (mt === 0x51 && ml === 3) tempos.push([tick, (v.getUint8(q) << 16) | (v.getUint8(q + 1) << 8) | v.getUint8(q + 2)]);
        if (mt === 0x03 && !name) { for (let i = 0; i < ml; i++) name += String.fromCharCode(v.getUint8(q + i)); }
        q += ml;
        if (mt === 0x2f) break;
      } else if (status === 0xf0 || status === 0xf7) {
        q += vlq();
      } else if (type === 0x90 || type === 0x80) {
        const note = v.getUint8(q), vel = v.getUint8(q + 1); q += 2;
        const key = ch * 128 + note;
        if (type === 0x90 && vel > 0) {
          if (open[key] !== undefined) segs.push({ s: open[key], e: tick, p: note, ch });
          open[key] = tick;
        } else if (open[key] !== undefined) {
          segs.push({ s: open[key], e: tick, p: note, ch }); delete open[key];
        }
      } else if (type === 0xc0 || type === 0xd0) { q += 1; }
      else { q += 2; }
    }
    const melodic = segs.filter(s => s.ch !== 9 && s.e > s.s);
    if (melodic.length) tracks.push({ name: name.trim() || ("Дорожка " + (t + 1)), segs: melodic });
  }
  if (!tracks.length) throw new Error("В MIDI нет мелодических нот");
  // tick -> ms through the tempo map
  tempos.sort((a, b) => a[0] - b[0]);
  let toMs;
  if (div & 0x8000) {
    const fps = 256 - (div >> 8), tpf = div & 0xff;
    toMs = tk => tk * 1000 / (fps * tpf);
  } else {
    const map = [[0, 0, 500000]];  // [tick, ms, us/qn]
    tempos.forEach(([tk, us]) => {
      const last = map[map.length - 1];
      const ms = last[1] + (tk - last[0]) * last[2] / div / 1000;
      if (tk === last[0]) { last[2] = us; } else map.push([tk, ms, us]);
    });
    toMs = tk => {
      let i = map.length - 1; while (i > 0 && map[i][0] > tk) i--;
      return map[i][1] + (tk - map[i][0]) * map[i][2] / div / 1000;
    };
  }
  tracks.forEach(tr => tr.segs.forEach(s => { s.s = toMs(s.s); s.e = toMs(s.e); }));
  return { tracks };
}

// Collapse polyphony to a single voice: at every moment play the highest note.
function monophonic(segs) {
  if (!segs.length) return [];
  const pts = [...new Set(segs.flatMap(s => [s.s, s.e]))].sort((a, b) => a - b);
  const sorted = segs.slice().sort((a, b) => a.s - b.s);
  const out = [];
  let lastStart = -1, lastP = -1, j = 0;
  const active = [];
  for (let i = 0; i < pts.length - 1; i++) {
    const t0 = pts[i], t1 = pts[i + 1];
    while (j < sorted.length && sorted[j].s <= t0) active.push(sorted[j++]);
    for (let k = active.length - 1; k >= 0; k--) if (active[k].e <= t0) active.splice(k, 1);
    let top = null;
    active.forEach(s => { if (!top || s.p > top.p || (s.p === top.p && s.s > top.s)) top = s; });
    const p = top ? top.p : 0, st = top ? top.s : -1;
    const dur = t1 - t0;
    if (out.length && p === lastP && st === lastStart) out[out.length - 1][1] += dur;
    else out.push([p, dur]);
    lastP = p; lastStart = st;
  }
  // to [freq, ms], drop very short slivers, split long rests
  const res = [];
  out.forEach(([p, d]) => {
    d = Math.round(d);
    if (d < 15) { if (res.length) res[res.length - 1][1] += d; return; }
    const f = p ? midiToFreq(p) : 0;
    while (d > lim.maxNoteMs) { res.push([f, lim.maxNoteMs]); d -= lim.maxNoteMs; }
    res.push([f, d]);
  });
  return res;
}

function midiNotes() {
  const sel = $("track").value;
  const segs = sel === "all" ? midiData.tracks.flatMap(t => t.segs) : midiData.tracks[+sel].segs;
  return monophonic(segs);
}

// ---------- current input -> notes ----------
function currentSong() {
  if (midiData) return { name: midiData.name, notes: midiNotes() };
  const text = $("src").value.trim();
  if (!text) throw new Error("Вставьте RTTTL, выберите пресет или загрузите файл");
  return parseRTTTL(text);
}

// Bring notes into the board's safe range (same rules as the firmware), so
// the browser preview sounds exactly like the speaker.
function safeNotes(notes) {
  const out = [];
  let moved = 0;
  notes.forEach(([f, ms]) => {
    if (f > 0) {
      const f0 = f;
      while (f < lim.minFreq) f *= 2;
      while (f > lim.maxFreq) f /= 2;
      f = Math.round(f);
      if (f !== f0) moved++;
    } else f = 0;
    ms = Math.round(ms);
    if (ms < lim.minNoteMs) { if (out.length) out[out.length - 1][1] = Math.min(lim.maxNoteMs, out[out.length - 1][1] + ms); return; }
    if (f === 0) { while (ms > lim.maxNoteMs) { out.push([0, lim.maxNoteMs]); ms -= lim.maxNoteMs; } }
    out.push([f, Math.min(ms, lim.maxNoteMs)]);
  });
  return { notes: out, moved };
}

function prepared() {
  const s = currentSong();
  const safe = safeNotes(s.notes);
  s.notes = safe.notes;
  let warn = "";
  if (safe.moved) warn += ` ${safe.moved} нот перенесено по октаве в диапазон ${lim.minFreq}–${lim.maxFreq} Гц.`;
  if (s.notes.length > lim.maxNotes) { warn += ` Обрезано до ${lim.maxNotes} нот из ${s.notes.length}.`; s.notes = s.notes.slice(0, lim.maxNotes); }
  s.name = s.name.slice(0, 20);
  s.warn = warn;
  return s;
}

// ---------- browser preview ----------
let actx = null, previewNodes = [];
function stopPreview() { previewNodes.forEach(o => { try { o.stop(); } catch (e) {} }); previewNodes = []; $("btnPreview").textContent = "🔊 Прослушать здесь"; }
function preview() {
  if (previewNodes.length) { stopPreview(); return; }
  let s;
  try { s = prepared(); } catch (e) { msg(e.message, "err"); return; }
  actx = actx || new (window.AudioContext || window.webkitAudioContext)();
  const gain = actx.createGain(); gain.gain.value = 0.08; gain.connect(actx.destination);
  let t = actx.currentTime + 0.05;
  s.notes.forEach(([f, ms]) => {
    if (f) {
      const o = actx.createOscillator(); o.type = "square"; o.frequency.value = f;
      o.connect(gain); o.start(t); o.stop(t + ms * 0.9 / 1000); previewNodes.push(o);
    }
    t += ms / 1000;
  });
  if (!previewNodes.length) return;
  previewNodes[previewNodes.length - 1].onended = stopPreview;
  $("btnPreview").textContent = "■ Остановить прослушивание";
  msg(`«${s.name}»: ${s.notes.length} нот, ${fmtTime(s.notes.reduce((a, n) => a + n[1], 0))}.${s.warn}`, "ok");
}

// ---------- board API ----------
async function api(path, opts) {
  const r = await fetch(path, Object.assign({ method: "POST" }, opts || {}));
  const text = await r.text();
  if (!r.ok) throw new Error(text || ("HTTP " + r.status));
  const st = JSON.parse(text); showStatus(st); return st;
}
function settingsQuery() {
  const clamp = (v, lo, hi) => Math.min(hi, Math.max(lo, Math.round(+v || 0)));
  const pause = clamp($("pause").value, 0, lim.maxPause);
  const vol = clamp($("vol").value, 1, lim.maxVol);
  return `loop=${$("loop").checked ? 1 : 0}&pause=${pause}&vol=${vol}`;
}
async function send() {
  let s;
  try { s = prepared(); } catch (e) { msg(e.message, "err"); return; }
  stopPreview();
  const body = s.notes.map(n => n[0] + "," + n[1]).join(";");
  const q = `name=${encodeURIComponent(s.name.slice(0, 60))}&save=${$("save").checked ? 1 : 0}&${settingsQuery()}`;
  $("btnSend").disabled = true;
  try {
    await api("/api/play?" + q, { body, headers: { "Content-Type": "text/plain" } });
    msg(`Играет «${s.name}» (${s.notes.length} нот)${$("save").checked ? ", сохранено в плату" : ""}.${s.warn}`, "ok");
  } catch (e) { msg("Ошибка: " + e.message, "err"); }
  $("btnSend").disabled = false;
}

let editing = false;
function showStatus(st) {
  ["maxNotes", "maxVol", "minFreq", "maxFreq", "minNoteMs", "maxNoteMs", "maxPause"].forEach(k => { if (st[k] !== undefined) lim[k] = st[k]; });
  $("maxN").textContent = lim.maxNotes;
  $("vol").max = lim.maxVol; $("pause").max = lim.maxPause;
  $("stName").textContent = st.name || "—";
  $("stInfo").textContent = `${st.playing ? "играет" : "остановлено"} · ${st.notes} нот · ${fmtTime(st.durationMs)}`;
  $("stBar").style.width = (st.playing && st.notes ? Math.min(100, st.index / st.notes * 100) : 0) + "%";
  if (!editing) {
    $("vol").value = st.vol; $("volVal").textContent = st.vol + "%";
    $("loop").checked = st.loop; $("pause").value = st.pause;
  }
}
async function poll() {
  try { const r = await fetch("/api/status"); showStatus(await r.json()); }
  catch (e) { $("stInfo").textContent = "нет связи с платой"; }
}

let settingsTimer = null;
function settingsChanged() {
  editing = true; $("volVal").textContent = $("vol").value + "%";
  clearTimeout(settingsTimer);
  settingsTimer = setTimeout(async () => {
    try { await api("/api/settings?" + settingsQuery()); } catch (e) { msg("Ошибка: " + e.message, "err"); }
    editing = false;
  }, 250);
}

// ---------- wiring ----------
Object.keys(PRESETS).forEach(k => {
  const b = document.createElement("button"); b.textContent = k;
  b.onclick = () => { midiData = null; $("trackRow").hidden = true; $("file").value = ""; $("src").value = PRESETS[k]; msg(""); };
  $("presets").appendChild(b);
});
$("src").addEventListener("input", () => { if (midiData) { midiData = null; $("trackRow").hidden = true; $("file").value = ""; } });
$("file").addEventListener("change", async () => {
  const f = $("file").files[0]; if (!f) return;
  midiData = null; $("trackRow").hidden = true;
  try {
    if (/\.midi?$/i.test(f.name)) {
      midiData = parseMIDI(await f.arrayBuffer());
      midiData.name = f.name.replace(/\.midi?$/i, "");
      const sel = $("track"); sel.innerHTML = "";
      const all = document.createElement("option"); all.value = "all"; all.textContent = "Все (верхний голос)"; sel.appendChild(all);
      midiData.tracks.forEach((t, i) => { const o = document.createElement("option"); o.value = i; o.textContent = `${t.name} (${t.segs.length} нот)`; sel.appendChild(o); });
      $("trackRow").hidden = midiData.tracks.length < 2;
      $("src").value = "";
      const n = midiNotes();
      msg(`MIDI загружен: ${midiData.tracks.length} дорож., ${n.length} нот после сведения в один голос.`, "ok");
    } else {
      $("src").value = (await f.text()).trim();
      parseRTTTL($("src").value);
      msg("Файл загружен.", "ok");
    }
  } catch (e) { midiData = null; msg(e.message, "err"); }
});
$("track").addEventListener("change", () => msg(`Выбрано: ${midiNotes().length} нот.`, "ok"));
$("btnPreview").onclick = preview;
$("btnSend").onclick = send;
$("btnReplay").onclick = () => api("/api/replay").catch(e => msg(e.message, "err"));
$("btnStop").onclick = () => api("/api/stop").catch(e => msg(e.message, "err"));
$("btnDefault").onclick = () => api("/api/default").then(() => msg("Вернул «К Элизе» по умолчанию.", "ok")).catch(e => msg(e.message, "err"));
["vol", "loop", "pause"].forEach(id => $(id).addEventListener("input", settingsChanged));
poll(); setInterval(poll, 1000);
</script>
</body>
</html>)HTML";
