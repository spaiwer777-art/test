// Сервер квеста «Протокол „Щит“» для ХОСТИНГА (интернет), а не локальной сети.
//
// Это порт PowerShell-сервера на Node.js (без внешних зависимостей, только встроенный http).
// Node запускается почти на любом хостинге (Render, Railway, Replit, VPS); PowerShell — нет.
//
// Главное отличие от локальной версии:
//   В локальной сети «компьютер штаба» определялся по IP (Test-LocalClient). В интернете
//   доверять IP/сети нельзя — поэтому права ведущего даёт СЕКРЕТНЫЙ КЛЮЧ (HOST_KEY),
//   который проверяется на сервере. Ведущий один раз открывает ссылку вида
//       https://ваш-адрес/?host=СЕКРЕТ
//   сервер ставит cookie — и дальше терминал, конструктор и печать QR работают как раньше.
//   Телефонам игроков и капитана ключ не нужен: их страницы и так открыты всем.
//
// Сайт лежит в папке public/ (её же можно залить на обычный PHP-хостинг — там работает public/backend).
//
// Запуск:   node server.js
// Переменные окружения:
//   PORT        — порт (хостинг обычно задаёт сам; по умолчанию 8080)
//   HOST_KEY    — секретный ключ ведущего (ЗАДАЙТЕ его на хостинге!)
//   PUBLIC_URL  — публичный адрес сайта для QR (необязательно; по умолчанию берётся из браузера)
//
// Главный экран (терминал защиты) можно открыть на ЛЮБОМ компьютере: состояние игры хранится
// на сервере (game.json), поэтому сервер может стоять в каморке, а терминал — на ПК у проектора.
// Главный экран у игры один; открыв терминал на другом компьютере, его можно перенести туда.

"use strict";

const http = require("http");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const os = require("os");

const ROOT = __dirname;
const SITE = path.join(ROOT, "public");           // сайт
const QUEST_DIR = path.join(SITE, "zadaniya");    // задания по QR фигурок (отдаются только по коду)
const BACKEND_DIR = path.join(SITE, "backend");   // PHP-бэкенд для обычного хостинга (Node его не отдаёт)
const SETUP_FILE = path.join(ROOT, "setup.json"); // вне public — не скачать из браузера
const GAME_FILE = path.join(ROOT, "game.json");   // состояние главного экрана (тоже вне public)
const CONFIG_FILE = path.join(SITE, "js", "config.js");
const CODES_FILE = path.join(QUEST_DIR, "kody.txt");

const PORT = parseInt(process.env.PORT, 10) || 8080;
const PUBLIC_URL = (process.env.PUBLIC_URL || "").replace(/\/+$/, "");

const MIME = {
  ".html": "text/html; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".jpg": "image/jpeg", ".jpeg": "image/jpeg", ".png": "image/png",
  ".svg": "image/svg+xml", ".ico": "image/x-icon", ".woff2": "font/woff2",
  ".woff": "font/woff", ".ttf": "font/ttf", ".txt": "text/plain; charset=utf-8"
};

// ---------- ключ ведущего ----------
// Приоритет: переменная окружения HOST_KEY -> сохранённый в setup.json -> случайный новый.
let persisted = {};
try { persisted = JSON.parse(fs.readFileSync(SETUP_FILE, "utf8")) || {}; } catch (e) { persisted = {}; }

let HOST_KEY = process.env.HOST_KEY || persisted.hostKey || "";
if (!HOST_KEY) {
  HOST_KEY = crypto.randomBytes(5).toString("hex"); // 10 символов
  persisted.hostKey = HOST_KEY;
  try { fs.writeFileSync(SETUP_FILE, JSON.stringify(persisted)); } catch (e) {}
}

// ---------- значения по умолчанию из config.js ----------
let configText = "";
try { configText = fs.readFileSync(CONFIG_FILE, "utf8"); } catch (e) {}

function numFrom(name, dflt) {
  const m = configText.match(new RegExp(name + "\\s*:\\s*(\\d+)"));
  return m ? parseInt(m[1], 10) : dflt;
}
function strFrom(name, dflt) {
  const m = configText.match(new RegExp(name + '\\s*:\\s*"([^"]*)"'));
  return m ? m[1] : dflt;
}
const STAGE_COUNT = (configText.match(/vulnerability\s*:/g) || []).length || 11;

const DEFAULT_SETTINGS = {
  teamName: strFrom("teamName", "Отдел ИБ"),
  totalMinutes: numFrom("totalMinutes", 60),
  insideSeconds: numFrom("insideSeconds", 60),
  cooldownSeconds: numFrom("cooldownSeconds", 300),
  warnSeconds: numFrom("warnSeconds", 10),
  runnersPerVisit: numFrom("runnersPerVisit", 2),
  autoPenaltySeconds: numFrom("autoPenaltySeconds", 60),
  wrongPenaltySeconds: numFrom("wrongPenaltySeconds", 60),
  maxWrongBeforeLock: numFrom("maxWrongBeforeLock", 3),
  lockSeconds: numFrom("lockSeconds", 30),
  hintPenaltySeconds: numFrom("hintPenaltySeconds", 120)
};
const DEFAULT_STAGES = Array.from({ length: STAGE_COUNT }, function (_, i) { return i + 1; });

// ---------- состояние игры (в памяти) ----------
const bootId = crypto.randomBytes(4).toString("hex");
let events = [];
let nextEventId = 1;
let stateJson = "null";
let stateAt = 0;

let players = {};                                            // id -> { name, seen, runs }
let captain = { token: "", name: "", seen: 0 };
let run = { phase: "ready", since: 0, runners: [], visits: 0, violations: 0, lastOverMs: 0 };

let setup = {
  stages: Array.isArray(persisted.stages) && persisted.stages.length ? persisted.stages : DEFAULT_STAGES.slice(),
  settings: Object.assign({}, DEFAULT_SETTINGS, persisted.settings || {})
};

function saveSetup() {
  const data = { hostKey: HOST_KEY, stages: setup.stages, settings: setup.settings };
  try { fs.writeFileSync(SETUP_FILE, JSON.stringify(data, null, 2)); } catch (e) {}
}

function nowMs() { return Date.now(); }

// ---------- главный экран (терминал защиты) ----------
// Полное состояние игры присылает тот терминал, который сейчас главный (owner).
// Другой компьютер ведущего может забрать главный экран себе (/api/terminal/claim) —
// тогда прежний получает отказ на сохранение и показывает заставку «экран перенесён».
const TERMINAL_ONLINE_MS = 15000;
let terminal = { owner: "", addr: "", seen: 0, game: "null", clientNow: 0, savedAt: 0 };
try {
  const g = JSON.parse(fs.readFileSync(GAME_FILE, "utf8"));
  if (g && g.game && typeof g.game === "object") {
    terminal.game = JSON.stringify(g.game);
    terminal.clientNow = Number(g.clientNow) || 0;
    terminal.savedAt = Number(g.savedAt) || 0;
  }
} catch (e) { /* файла ещё нет — новой игры */ }

let gameDirty = false;
function flushGame() {
  if (!gameDirty) return;
  gameDirty = false;
  const data = '{"savedAt":' + terminal.savedAt + ',"clientNow":' + terminal.clientNow + ',"game":' + terminal.game + "}";
  try { fs.writeFileSync(GAME_FILE, data); } catch (e) { /* нет прав на запись — игра идёт из памяти */ }
}
setInterval(flushGame, 3000).unref();

function validTerminalId(v) { return /^[a-z0-9]{8,32}$/.test(String(v || "")); }

function terminalInfo() {
  const now = nowMs();
  const t = terminal;
  return '{"owner":' + JSON.stringify(t.owner) +
    ',"addr":' + JSON.stringify(t.addr) +
    ',"ownerOnline":' + (t.owner !== "" && t.seen > 0 && now - t.seen < TERMINAL_ONLINE_MS) +
    ',"ownerAgoMs":' + (t.seen ? now - t.seen : -1) +
    ',"ageMs":' + (t.savedAt ? now - t.savedAt : -1) +
    ',"clientNow":' + t.clientNow +
    ',"game":' + t.game + "}";
}

function clientAddr(req) {
  const fwd = String(req.headers["x-forwarded-for"] || "").split(",")[0].trim();
  const a = fwd || (req.socket && req.socket.remoteAddress) || "";
  return a.replace(/^::ffff:/, "");
}

// Адреса этого компьютера в локальной сети — их видно в консоли и в QR,
// если терминал открыт прямо на сервере через localhost.
function lanUrls() {
  const list = [];
  const nets = os.networkInterfaces();
  Object.keys(nets).forEach(function (name) {
    (nets[name] || []).forEach(function (n) {
      if ((n.family === "IPv4" || n.family === 4) && !n.internal) list.push("http://" + n.address + ":" + PORT + "/");
    });
  });
  return list;
}
function isLoopbackHost(req) {
  const h = String(req.headers.host || "").replace(/:\d+$/, "").replace(/^\[|\]$/g, "");
  return h === "localhost" || h === "::1" || /^127\./.test(h);
}
// Адрес сайта для QR: PUBLIC_URL; если терминал открыт через localhost — адрес в локальной сети;
// иначе пусто, и страница возьмёт адрес, с которого её открыли.
function siteUrls(req) {
  if (PUBLIC_URL) return [PUBLIC_URL + "/"];
  return isLoopbackHost(req) ? lanUrls().slice(0, 1) : [];
}
function matrixCfg() {
  const s = setup.settings;
  return {
    inside: (s.insideSeconds || 60) * 1000,
    cool: (s.cooldownSeconds || 300) * 1000,
    warn: (s.warnSeconds || 10) * 1000,
    runners: s.runnersPerVisit || 2
  };
}

// ---------- машина фаз Матрицы ----------
function updateRun() {
  const cfg = matrixCfg();
  const now = nowMs();
  if (run.phase === "inside" && now - run.since >= cfg.inside) {
    run.phase = "overtime";
    run.since = run.since + cfg.inside;
  }
  if (run.phase === "cooldown" && now - run.since >= cfg.cool) {
    run.phase = "ready";
    run.since = now;
    run.runners = [];
  }
}

function runObject(playerId) {
  updateRun();
  const cfg = matrixCfg();
  const elapsed = nowMs() - run.since;
  let remaining = 0, over = 0;
  if (run.phase === "inside") remaining = cfg.inside - elapsed;
  else if (run.phase === "cooldown") remaining = cfg.cool - elapsed;
  else if (run.phase === "overtime") over = elapsed;
  const now = nowMs();
  const list = Object.keys(players).map(function (id) {
    const p = players[id];
    return { id: id, name: p.name, runs: p.runs, online: (now - p.seen) < 15000 };
  });
  const runners = run.runners
    .filter(function (id) { return players[id]; })
    .map(function (id) { return { id: id, name: players[id].name }; });
  return {
    boot: bootId,
    phase: run.phase,
    remainingMs: remaining,
    overMs: over,
    insideMs: cfg.inside,
    cooldownMs: cfg.cool,
    warnMs: cfg.warn,
    maxRunners: cfg.runners,
    runners: runners,
    visits: run.visits,
    violations: run.violations,
    lastOverMs: run.lastOverMs,
    players: list,
    captain: captain.token !== "",
    captainName: captain.name,
    captainOnline: (now - captain.seen) < 15000,
    isRunner: run.runners.indexOf(playerId) !== -1
  };
}

// ---------- вспомогательное ----------
function cleanName(value) {
  let name = String(value == null ? "" : value).trim().replace(/[\x00-\x1F<>]/g, "");
  if (name.length > 24) name = name.slice(0, 24);
  return name;
}
function testCaptain(body) {
  return !!body && captain.token !== "" && String(body.token) === captain.token;
}
function addEvent(obj) {
  obj.id = nextEventId++;
  events.push(JSON.stringify(obj));
  if (events.length > 200) events.shift();
}

// Коды QR-наклеек: код -> папка задания (zadaniya/kody.txt). Если папки нет — просто пусто.
function loadCodes() {
  const codes = {};
  try {
    const lines = fs.readFileSync(CODES_FILE, "utf8").split(/\r?\n/);
    for (const line of lines) {
      const t = line.trim();
      if (!t || t.startsWith("#")) continue;
      const parts = t.split(/\s+/);
      if (parts.length >= 2 && /^[A-Za-z0-9]+$/.test(parts[0])) {
        codes[parts[0].toUpperCase()] = t.slice(parts[0].length).trim();
      }
    }
  } catch (e) {}
  return codes;
}

// config.js для обычных устройств — без ответов, только блок matrix.
function publicConfig() {
  const m = configText.match(/matrix\s*:\s*\{[^{}]*\}/);
  const inner = m ? m[0] : "";
  return "// Для устройств без ключа ведущего: только настройки таймера Матрицы, без ответов.\n" +
    "window.QUEST_CONFIG = { " + inner + " };\n";
}

// Сравнение ключа за постоянное время (чтобы ключ нельзя было подобрать по времени ответа).
function sameKey(value) {
  if (!value) return false;
  const a = Buffer.from(String(value));
  const b = Buffer.from(HOST_KEY);
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

function parseCookies(req) {
  const out = {};
  const raw = req.headers.cookie;
  if (!raw) return out;
  raw.split(";").forEach(function (part) {
    const i = part.indexOf("=");
    if (i > -1) out[part.slice(0, i).trim()] = decodeURIComponent(part.slice(i + 1).trim());
  });
  return out;
}

// ---------- HTTP ----------
function sendRaw(res, code, type, body, extraHeaders) {
  const headers = Object.assign({
    "Content-Type": type,
    "Content-Length": Buffer.byteLength(body),
    "Cache-Control": "no-store",
    "Access-Control-Allow-Origin": "*"
  }, extraHeaders || {});
  res.writeHead(code, headers);
  res.end(body);
}
function sendJson(res, obj, extraHeaders) {
  sendRaw(res, 200, "application/json; charset=utf-8", typeof obj === "string" ? obj : JSON.stringify(obj), extraHeaders);
}
function sendText(res, code, text) {
  sendRaw(res, code, "text/plain; charset=utf-8", text);
}

function inside(dir, full) {
  const d = path.resolve(dir);
  return full === d || full.startsWith(d + path.sep);
}

function resolveStatic(urlPath) {
  if (urlPath === "/" || urlPath === "") urlPath = "/start.html";
  let baseDir = SITE;
  let viaCode = false;
  const q = urlPath.match(/^\/q\/([A-Za-z0-9]+)(\/.*)$/);
  if (q) {
    const folder = loadCodes()[q[1].toUpperCase()];
    if (!folder) return null;
    baseDir = path.join(QUEST_DIR, folder);
    urlPath = q[2] === "/" ? "/ZADANIE.html" : q[2];
    viaCode = true;
  }
  const rel = urlPath.replace(/^\/+/, "").split("/").join(path.sep);
  const full = path.resolve(baseDir, rel);
  if (!inside(baseDir, full) || full === path.resolve(baseDir)) return null;
  // Закрыто: папка заданий напрямую (только по коду с наклейки), PHP-бэкенд, служебные и скрытые файлы.
  if (!viaCode && inside(QUEST_DIR, full)) return null;
  if (inside(BACKEND_DIR, full)) return null;
  if (/\.php$/i.test(full) || path.basename(full).startsWith(".")) return null;
  try { if (!fs.statSync(full).isFile()) return null; } catch (e) { return null; }
  return full;
}

function readBody(req, cb) {
  let data = "";
  req.on("data", function (c) { data += c; if (data.length > 1048576) req.destroy(); });
  req.on("end", function () { cb(data); });
  req.on("error", function () { cb(""); });
}

function parseJson(text) { try { return JSON.parse(text); } catch (e) { return null; } }

function handleApi(req, res, pathName, query, isHost, body) {
  switch (pathName) {
    case "/api/gate": {
      if (req.method === "POST") { sendJson(res, { ok: true }); return; }
      const r = runObject("");
      const names = r.runners.map(function (x) { return x.name; });
      sendJson(res, { boot: bootId, ageMs: 0, gate: {
        phase: r.phase, remainingMs: r.remainingMs, overMs: r.overMs,
        runners: names, visits: r.visits, violations: r.violations } });
      return;
    }
    case "/api/event": {
      if (req.method !== "POST") { sendText(res, 405, "POST only"); return; }
      const obj = parseJson(body) || {};
      addEvent(obj);
      sendJson(res, { ok: true });
      return;
    }
    case "/api/events": {
      const since = (query.match(/since=(\d+)/) || [])[1];
      const sinceN = since ? parseInt(since, 10) : 0;
      const list = events.filter(function (e) { return JSON.parse(e).id > sinceN; });
      sendJson(res, '{"boot":"' + bootId + '","events":[' + list.join(",") + "]}");
      return;
    }
    case "/api/host": {
      sendJson(res, { configured: true, host: !!isHost });
      return;
    }
    case "/api/info": {
      sendJson(res, { boot: bootId, urls: siteUrls(req) });
      return;
    }
    case "/api/terminal": {
      if (!isHost) { sendText(res, 403, "Только ведущий (нужен ключ)"); return; }
      sendJson(res, terminalInfo());
      return;
    }
    case "/api/terminal/claim": {
      // Сделать этот компьютер главным экраном (в том числе забрать его у другого компьютера).
      if (req.method !== "POST") { sendText(res, 405, "POST only"); return; }
      if (!isHost) { sendText(res, 403, "Только ведущий (нужен ключ)"); return; }
      const b = parseJson(body);
      if (!b || !validTerminalId(b.id)) { sendText(res, 400, "Нужен id терминала"); return; }
      terminal.owner = String(b.id);
      terminal.addr = clientAddr(req);
      terminal.seen = nowMs();
      sendJson(res, terminalInfo());
      return;
    }
    case "/api/terminal/save": {
      if (req.method !== "POST") { sendText(res, 405, "POST only"); return; }
      if (!isHost) { sendText(res, 403, "Только ведущий (нужен ключ)"); return; }
      const b = parseJson(body);
      if (!b || !validTerminalId(b.id) || !b.game || typeof b.game !== "object") { sendText(res, 400, "Bad JSON"); return; }
      // Главный экран уже на другом компьютере — этот должен остановиться.
      if (terminal.owner !== "" && terminal.owner !== String(b.id)) {
        sendRaw(res, 409, "application/json; charset=utf-8", JSON.stringify({ moved: true, addr: terminal.addr }));
        return;
      }
      if (terminal.owner === "") terminal.addr = clientAddr(req); // например, после перезапуска сервера
      terminal.owner = String(b.id);
      terminal.seen = nowMs();
      terminal.game = JSON.stringify(b.game);
      terminal.clientNow = Number(b.now) || nowMs();
      terminal.savedAt = nowMs();
      gameDirty = true;
      sendJson(res, { ok: true });
      return;
    }
    case "/api/terminal/release": {
      // Терминал закрыли: главный экран свободен, следующий компьютер займёт его без вопроса.
      if (req.method !== "POST") { sendText(res, 405, "POST only"); return; }
      const b = parseJson(body);
      if (isHost && b && terminal.owner !== "" && String(b.id) === terminal.owner) terminal.seen = 0;
      sendJson(res, { ok: true });
      return;
    }
    case "/api/setup": {
      if (req.method === "POST") {
        if (!isHost) { sendText(res, 403, "Только ведущий (нужен ключ)"); return; }
        const b = parseJson(body);
        if (!b) { sendText(res, 400, "Bad JSON"); return; }
        if (Array.isArray(b.stages)) {
          const ids = b.stages.map(Number).filter(function (n) { return n >= 1 && n <= STAGE_COUNT; });
          if (ids.length) setup.stages = Array.from(new Set(ids)).sort(function (a, b) { return a - b; });
        }
        if (b.settings && typeof b.settings === "object") {
          const s = Object.assign({}, setup.settings);
          Object.keys(DEFAULT_SETTINGS).forEach(function (k) {
            if (b.settings[k] !== undefined && b.settings[k] !== null && b.settings[k] !== "") s[k] = b.settings[k];
          });
          setup.settings = s;
        }
        saveSetup();
        sendJson(res, { ok: true });
      } else {
        sendJson(res, { stages: setup.stages, settings: setup.settings });
      }
      return;
    }
    case "/api/state": {
      if (req.method === "POST") {
        if (!isHost) { sendText(res, 403, "Только терминал ведущего"); return; }
        if (!parseJson(body)) { sendText(res, 400, "Bad JSON"); return; }
        stateJson = body;
        stateAt = nowMs();
        sendJson(res, { ok: true });
      } else {
        const age = stateAt ? (nowMs() - stateAt) : -1;
        sendJson(res, '{"boot":"' + bootId + '","ageMs":' + age + ',"state":' + stateJson + "}");
      }
      return;
    }
    case "/api/join": {
      if (req.method !== "POST") { sendText(res, 405, "POST only"); return; }
      const b = parseJson(body);
      const name = b ? cleanName(b.name) : "";
      if (!name) { sendText(res, 400, "Нужно имя"); return; }
      let id = (b && /^[a-z0-9]{8}$/.test(String(b.id))) ? String(b.id) : "";
      if (!id) id = crypto.randomBytes(4).toString("hex");
      if (players[id]) { players[id].name = name; players[id].seen = nowMs(); }
      else { players[id] = { name: name, seen: nowMs(), runs: 0 }; }
      sendJson(res, { id: id, name: name });
      return;
    }
    case "/api/run": {
      const pid = (query.match(/id=([a-z0-9]{8})/) || [])[1] || "";
      if (pid && players[pid]) players[pid].seen = nowMs();
      const cap = (query.match(/captain=([a-z0-9]+)/) || [])[1];
      if (cap && captain.token !== "" && cap === captain.token) captain.seen = nowMs();
      sendJson(res, runObject(pid));
      return;
    }
    case "/api/captain/claim": {
      if (req.method !== "POST") { sendText(res, 405, "POST only"); return; }
      const b = parseJson(body);
      if (captain.token !== "" && !testCaptain(b)) { sendText(res, 409, "Капитан уже назначен"); return; }
      if (captain.token === "") captain.token = crypto.randomBytes(6).toString("hex");
      if (b && cleanName(b.name)) captain.name = cleanName(b.name);
      captain.seen = nowMs();
      sendJson(res, { token: captain.token, name: captain.name });
      return;
    }
    case "/api/captain/reset": {
      if (!isHost) { sendText(res, 403, "Только ведущий (нужен ключ)"); return; }
      captain = { token: "", name: "", seen: 0 };
      sendJson(res, { ok: true });
      return;
    }
    case "/api/run/send": {
      const b = parseJson(body);
      if (!testCaptain(b)) { sendText(res, 403, "Только капитан"); return; }
      updateRun();
      if (run.phase !== "ready") { sendText(res, 409, "Матрица сейчас закрыта"); return; }
      const cfg = matrixCfg();
      const ids = Array.from(new Set((b.runners || []).map(String).filter(function (id) { return players[id]; })));
      if (ids.length < 1 || ids.length > cfg.runners) { sendText(res, 400, "Выберите от 1 до " + cfg.runners + " игроков"); return; }
      run.phase = "assigned";
      run.since = nowMs();
      run.runners = ids;
      sendJson(res, runObject(""));
      return;
    }
    case "/api/run/cancel": {
      const b = parseJson(body);
      if (!testCaptain(b)) { sendText(res, 403, "Только капитан"); return; }
      if (run.phase === "assigned") { run.phase = "ready"; run.since = nowMs(); run.runners = []; }
      sendJson(res, runObject(""));
      return;
    }
    case "/api/run/enter": {
      const b = parseJson(body);
      const allowed = testCaptain(b) || (b && run.runners.indexOf(String(b.id)) !== -1);
      if (!allowed) { sendText(res, 403, "Только бегуны или капитан"); return; }
      updateRun();
      if (run.phase === "assigned") {
        run.phase = "inside";
        run.since = nowMs();
        run.visits++;
        run.lastOverMs = 0;
        run.runners.forEach(function (id) { if (players[id]) players[id].runs++; });
      }
      sendJson(res, runObject(b ? String(b.id) : ""));
      return;
    }
    case "/api/run/exit": {
      const b = parseJson(body);
      const allowed = testCaptain(b) || (b && run.runners.indexOf(String(b.id)) !== -1);
      if (!allowed) { sendText(res, 403, "Только бегуны или капитан"); return; }
      updateRun();
      const now = nowMs();
      if (run.phase === "inside") {
        run.phase = "cooldown"; run.since = now;
      } else if (run.phase === "overtime") {
        const over = now - run.since;
        run.violations++;
        run.lastOverMs = over;
        run.phase = "cooldown";
        run.since = now;
        addEvent({ type: "violation", overSeconds: Math.ceil(over / 1000), visits: run.visits });
      }
      sendJson(res, runObject(b ? String(b.id) : ""));
      return;
    }
    case "/api/run/open": {
      if (!isHost) { sendText(res, 403, "Только ведущий (нужен ключ)"); return; }
      run.phase = "ready"; run.since = nowMs(); run.runners = [];
      sendJson(res, { ok: true });
      return;
    }
    case "/api/players/reset": {
      if (!isHost) { sendText(res, 403, "Только ведущий (нужен ключ)"); return; }
      players = {};
      captain = { token: "", name: "", seen: 0 };
      run = { phase: "ready", since: nowMs(), runners: [], visits: 0, violations: 0, lastOverMs: 0 };
      sendJson(res, { ok: true });
      return;
    }
    case "/api/qr": {
      if (!isHost) { sendText(res, 403, "Только ведущий (нужен ключ)"); return; }
      const codes = loadCodes();
      const items = Object.keys(codes).sort(function (a, b) { return codes[a].localeCompare(codes[b]); })
        .map(function (code) { return '{"code":"' + code + '","folder":"' + codes[code].replace(/["\\]/g, "") + '"}'; });
      sendJson(res, '{"urls":' + JSON.stringify(siteUrls(req)) + ',"items":[' + items.join(",") + "]}");
      return;
    }
    default:
      sendText(res, 404, "Unknown API");
  }
}

const server = http.createServer(function (req, res) {
  let target = req.url;
  let query = "";
  const qi = target.indexOf("?");
  if (qi >= 0) { query = target.slice(qi + 1); target = target.slice(0, qi); }
  let pathName;
  try { pathName = decodeURIComponent(target); } catch (e) { pathName = target; }

  const cookies = parseCookies(req);
  const hostFromQuery = (query.match(/(?:^|&)host=([^&]+)/) || [])[1];
  const hostParam = hostFromQuery ? decodeURIComponent(hostFromQuery) : "";
  const isHost = sameKey(cookies.host) || sameKey(hostParam);

  // Ссылка-вход для ведущего: /?host=КЛЮЧ ставит cookie. /__host?key=КЛЮЧ — то же с редиректом.
  let setCookie = null;
  if (sameKey(hostParam)) {
    setCookie = "host=" + encodeURIComponent(HOST_KEY) + "; Path=/; Max-Age=" + (7 * 24 * 3600) + "; SameSite=Lax";
  }
  if (pathName === "/__host") {
    const key = (query.match(/key=([^&]+)/) || [])[1];
    if (key && sameKey(decodeURIComponent(key))) {
      res.writeHead(302, {
        "Location": "/index.html",
        "Set-Cookie": "host=" + encodeURIComponent(HOST_KEY) + "; Path=/; Max-Age=" + (7 * 24 * 3600) + "; SameSite=Lax"
      });
      res.end();
    } else {
      sendText(res, 403, "Неверный ключ ведущего.");
    }
    return;
  }

  // API
  if (pathName.startsWith("/api/")) {
    if (req.method === "POST") {
      readBody(req, function (body) { handleApi(req, res, pathName, query, isHost, body); });
    } else {
      handleApi(req, res, pathName, query, isHost, "");
    }
    return;
  }

  if (req.method !== "GET" && req.method !== "HEAD") { sendText(res, 405, "GET only"); return; }

  // /q/КОД -> /q/КОД/
  if (/^\/q\/[A-Za-z0-9]+$/.test(pathName)) {
    res.writeHead(302, { "Location": pathName + "/" });
    res.end();
    return;
  }

  const file = resolveStatic(pathName);
  if (!file) { sendText(res, 404, "404: страница не найдена"); return; }

  // config.js: с ключом ведущего — полный (с ответами); без ключа — только таймер Матрицы.
  // Сравниваем итоговый путь к файлу, а не адрес: /js/./config.js и //js//config.js тоже сюда.
  if (path.resolve(file) === path.resolve(CONFIG_FILE) && !isHost) {
    const js = publicConfig();
    sendRaw(res, 200, MIME[".js"], req.method === "HEAD" ? "" : js, setCookie ? { "Set-Cookie": setCookie } : null);
    return;
  }

  const ext = path.extname(file).toLowerCase();
  const type = MIME[ext] || "application/octet-stream";
  let data;
  try { data = fs.readFileSync(file); } catch (e) { sendText(res, 404, "404"); return; }
  const headers = Object.assign({
    "Content-Type": type,
    "Content-Length": req.method === "HEAD" ? 0 : data.length,
    "Cache-Control": "no-store"
  }, setCookie ? { "Set-Cookie": setCookie } : {});
  res.writeHead(200, headers);
  res.end(req.method === "HEAD" ? undefined : data);
});

function shutdown() { flushGame(); process.exit(0); }
process.on("SIGINT", shutdown);
process.on("SIGTERM", shutdown);

server.on("error", function (e) {
  if (e && e.code === "EADDRINUSE") {
    console.error("");
    console.error("  Порт " + PORT + " уже занят: наверное, сервер уже запущен в другом окне.");
    process.exit(2); // ЗАПУСК.bat по этому коду не перезапускает сервер по кругу
  }
  throw e;
});

server.listen(PORT, function () {
  console.log("");
  console.log("  ПРОТОКОЛ «ЩИТ» — сервер для хостинга запущен");
  console.log("  -------------------------------------------------");
  console.log("  Порт: " + PORT);
  const lan = lanUrls();
  if (lan.length) {
    console.log("  Адрес в локальной сети (открывайте на любом компьютере и телефоне):");
    lan.forEach(function (u) { console.log("    " + u); });
  }
  console.log("  Игроки и капитан открывают обычный адрес сайта.");
  console.log("  Терминал защиты (главный экран) можно открыть на любом компьютере");
  console.log("  и перенести на другой — прогресс игры хранится на сервере.");
  console.log("  Ведущий вводит ключ на терминале или открывает:  /?host=" + HOST_KEY);
  if (!process.env.HOST_KEY) {
    console.log("");
    console.log("  ВНИМАНИЕ: HOST_KEY не задан в переменных окружения.");
    console.log("  Сейчас используется ключ: " + HOST_KEY);
    console.log("  На хостинге задайте переменную HOST_KEY своим секретом.");
  }
  console.log("");
});
