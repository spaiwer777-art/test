(function () {
  "use strict";

  const M = (window.QUEST_CONFIG && window.QUEST_CONFIG.matrix) || {};
  const INSIDE_MS = (M.insideSeconds || 60) * 1000;
  const COOLDOWN_MS = (M.cooldownSeconds || 300) * 1000;
  const WARN_MS = (M.warnSeconds || 10) * 1000;
  const FLASHES = M.flashesPerVisit || 1;
  const FLASH_RULE = FLASHES === 1 ? "QR-код одной фигурки" : "QR-коды не больше чем " + FLASHES + " фигурок";
  const STORAGE_KEY = "shield-quest:gate";
  // Защита от случайного двойного нажатия: вошли и тут же «вышли».
  const PRESS_GUARD_MS = 1500;

  const $ = function (id) { return document.getElementById(id); };
  const el = {
    body: $("gate"),
    status: $("status"),
    time: $("time"),
    progress: $("progress"),
    sub: $("sub"),
    violation: $("violation"),
    button: $("main-button"),
    stats: $("stats"),
    fullscreen: $("btn-fullscreen"),
    open: $("btn-open"),
    reset: $("btn-reset")
  };

  // Запасной таймер на отдельном устройстве с игрой не связан: основной таймер Матрицы
  // идёт на телефонах бегунов, которых отправил капитан. Штрафы по этому таймеру даёт ведущий.
  const ONLINE = false;

  let s = load();
  let lastPress = 0;
  let lastBeepSecond = null;
  let wakeLock = null;
  let lastReportAt = 0;
  let lastReportPhase = null;
  let linkOk = null;

  function post(url, payload) {
    if (!ONLINE) return Promise.resolve();
    return fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    }).then(function (r) {
      linkOk = r.ok;
    }).catch(function () {
      linkOk = false;
    });
  }

  function reportGate() {
    const now = Date.now();
    if (now - lastReportAt < 1000 && s.phase === lastReportPhase) return;
    lastReportAt = now;
    lastReportPhase = s.phase;
    const elapsed = now - s.since;
    const payload = { phase: s.phase, visits: s.visits, violations: s.violations };
    if (s.phase === "inside") payload.remainingMs = INSIDE_MS - elapsed;
    if (s.phase === "cooldown") payload.remainingMs = COOLDOWN_MS - elapsed;
    if (s.phase === "overtime") payload.overMs = elapsed;
    post("api/gate", payload);
  }

  function defaultState() {
    return { phase: "ready", since: 0, visits: 0, violations: 0, lastOvertimeMs: 0 };
  }

  function load() {
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY));
      if (saved && typeof saved === "object") return Object.assign(defaultState(), saved);
    } catch (e) { /* начинаем с чистого состояния */ }
    return defaultState();
  }

  function save() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(s));
    } catch (e) { /* без сохранения тоже работает */ }
  }

  function pad(n) {
    return String(n).padStart(2, "0");
  }

  function fmt(ms, roundUp) {
    const total = Math.max(0, roundUp ? Math.ceil(ms / 1000) : Math.floor(ms / 1000));
    return pad(Math.floor(total / 60)) + ":" + pad(total % 60);
  }

  function setPhase(phase, since) {
    s.phase = phase;
    s.since = since;
    lastBeepSecond = null;
    save();
  }

  function advanceByTime() {
    const now = Date.now();
    if (s.phase === "inside" && now - s.since >= INSIDE_MS) {
      setPhase("overtime", s.since + INSIDE_MS);
      Sfx.alarm();
    }
    if (s.phase === "cooldown" && now - s.since >= COOLDOWN_MS) {
      setPhase("ready", now);
      Sfx.ok();
    }
  }

  function press() {
    Sfx.unlock();
    requestWakeLock();
    const now = Date.now();
    if (now - lastPress < PRESS_GUARD_MS) return;
    lastPress = now;
    advanceByTime();

    switch (s.phase) {
      case "ready":
        s.visits++;
        s.lastOvertimeMs = 0;
        setPhase("inside", now);
        Sfx.start();
        break;
      case "inside":
        s.lastOvertimeMs = 0;
        setPhase("cooldown", now);
        Sfx.ok();
        break;
      case "overtime":
        s.lastOvertimeMs = now - s.since;
        s.violations++;
        setPhase("cooldown", now);
        Sfx.error();
        post("api/event", {
          type: "violation",
          overSeconds: Math.ceil(s.lastOvertimeMs / 1000),
          visits: s.visits
        });
        break;
      case "cooldown":
        Sfx.error();
        el.button.classList.remove("denied");
        void el.button.offsetWidth;
        el.button.classList.add("denied");
        return;
    }
    render();
  }

  function beepOncePerSecond(ms, fn) {
    const sec = Math.floor(ms / 1000);
    if (sec !== lastBeepSecond) {
      lastBeepSecond = sec;
      fn();
    }
  }

  function render() {
    advanceByTime();
    const now = Date.now();
    const elapsed = now - s.since;
    let phaseClass = s.phase;
    let status = "";
    let time = "";
    let sub = "";
    let button = "";
    let progress = 0;
    let violation = "";

    switch (s.phase) {
      case "ready":
        status = "Матрица открыта";
        time = fmt(INSIDE_MS);
        sub = "Нажмите кнопку, когда входите. Отсканируйте " + FLASH_RULE +
          " и выходите. На выходе — нажмите кнопку ещё раз.";
        button = "Войти в Матрицу";
        progress = 100;
        break;
      case "inside": {
        const left = INSIDE_MS - elapsed;
        status = "Вы в Матрице";
        time = fmt(left, true);
        sub = "Сканируйте " + FLASH_RULE + " и возвращайтесь! Не забудьте нажать кнопку на выходе.";
        button = "Я вышел";
        progress = left / INSIDE_MS * 100;
        if (left <= WARN_MS) {
          phaseClass = "warn";
          status = "Выходите!";
          beepOncePerSecond(left, Sfx.tick);
        }
        break;
      }
      case "overtime":
        status = "Время вышло! Нарушение";
        time = "+" + fmt(elapsed);
        sub = "Немедленно покиньте Матрицу и нажмите кнопку. Нарушение будет передано оператору.";
        button = "Я вышел";
        progress = 100;
        beepOncePerSecond(elapsed, Sfx.alarm);
        break;
      case "cooldown": {
        const left = COOLDOWN_MS - elapsed;
        status = "Матрица перезагружается";
        time = fmt(left, true);
        sub = "Вход заблокирован. Решайте задание, пока идёт перезагрузка.";
        button = "Вход закрыт";
        progress = (1 - left / COOLDOWN_MS) * 100;
        if (s.lastOvertimeMs > 0) {
          violation = "Нарушение: превышение на " + Math.ceil(s.lastOvertimeMs / 1000) +
            (ONLINE && linkOk ? " сек. Штраф уже передан на терминал." : " сек. Сообщите оператору — будет штраф.");
        }
        break;
      }
    }

    el.body.className = "gate gate--" + phaseClass;
    el.status.textContent = status;
    el.time.textContent = time;
    el.sub.textContent = sub;
    el.button.textContent = button;
    el.violation.textContent = violation;
    el.progress.style.width = Math.max(0, Math.min(100, progress)) + "%";
    let link = "";
    if (ONLINE) {
      link = linkOk === null ? " · Связь с терминалом: …" : linkOk ? " · Связь с терминалом: есть" : " · Связь с терминалом: НЕТ";
    }
    el.stats.textContent = "Входов: " + s.visits + " · Нарушений: " + s.violations + link;
    reportGate();

    const colors = { ready: "#00ff41", inside: "#00ff41", warn: "#ff2a3d", overtime: "#ff2a3d", cooldown: "#3d8bff" };
    window.MatrixBg.setColor(colors[phaseClass]);
    if (window.MatrixBg.setLevel) {
      window.MatrixBg.setLevel(0);
    }
  }

  function requestWakeLock() {
    if (wakeLock || !("wakeLock" in navigator)) return;
    navigator.wakeLock.request("screen").then(function (lock) {
      wakeLock = lock;
      lock.addEventListener("release", function () { wakeLock = null; });
    }).catch(function () { /* экран может погаснуть — отключите сон в настройках устройства */ });
  }

  el.button.addEventListener("click", press);

  // Иначе пробел «нажимает» кнопку, на которой остался фокус, вместо главной.
  document.querySelectorAll("button").forEach(function (b) {
    b.addEventListener("click", function () { b.blur(); });
  });

  document.addEventListener("keydown", function (e) {
    if (e.repeat) return;
    if (e.code === "Space" || e.code === "Enter" || e.code === "NumpadEnter" ||
        e.code === "ArrowRight" || e.code === "PageDown") {
      e.preventDefault();
      press();
    }
  });

  el.fullscreen.addEventListener("click", function () {
    requestWakeLock();
    if (document.fullscreenElement) {
      document.exitFullscreen();
    } else if (document.documentElement.requestFullscreen) {
      document.documentElement.requestFullscreen().catch(function () {});
    }
  });

  el.open.addEventListener("click", function () {
    if (!window.confirm("Открыть Матрицу прямо сейчас, не дожидаясь конца перезагрузки?")) return;
    setPhase("ready", Date.now());
    render();
  });

  el.reset.addEventListener("click", function () {
    if (!window.confirm("Обнулить таймер и счётчики входов и нарушений? (для новой команды)")) return;
    s = defaultState();
    save();
    render();
  });

  document.addEventListener("visibilitychange", function () {
    if (document.visibilityState === "visible" && wakeLock === null && s.visits > 0) requestWakeLock();
  });

  render();
  setInterval(render, 100);
})();
