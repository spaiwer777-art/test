// Телефон игрока: подключается к игре по QR-коду с экрана терминала.
// Показывает время и прогресс игры, а когда капитан отправляет игрока в Матрицу,
// превращается в таймер Матрицы — общий для всех бегунов этого захода (его ведёт сервер).
(function () {
  "use strict";

  const PLAYER_KEY = "shield-quest:player";
  const TASKS_KEY = "shield-quest:my-tasks";
  const GAME_KEY = "shield-quest:game-id";

  const $ = function (id) { return document.getElementById(id); };
  const el = {
    joinForm: $("join-form"),
    joinName: $("join-name"),
    joinError: $("join-error"),
    pult: $("pult"),
    playerName: $("player-name"),
    rename: $("rename"),
    time: $("time"),
    status: $("status"),
    matrixState: $("matrix-state"),
    matrixNote: $("matrix-note"),
    tasks: $("tasks"),
    tasksEmpty: $("tasks-empty"),
    slots: $("slots"),
    doneCount: $("done-count"),
    threats: $("threats"),
    link: $("link"),
    runner: $("runner"),
    runnerTeam: $("runner-team"),
    runnerStatus: $("runner-status"),
    runnerTime: $("runner-time"),
    runnerProgress: $("runner-progress"),
    runnerSub: $("runner-sub"),
    runnerButton: $("runner-button")
  };

  let me = read(PLAYER_KEY, null);   // { id, name }
  let game = null;                   // ответ api/state
  let gameAt = 0;
  let run = null;                    // ответ api/run
  let runAt = 0;
  let wasRunner = false;
  let lastBeep = null;
  let busy = false;
  let wakeLock = null;
  let listsKey = "";

  function read(key, fallback) {
    try {
      const v = JSON.parse(localStorage.getItem(key));
      return v === null ? fallback : v;
    } catch (e) { return fallback; }
  }

  function write(key, value) {
    try { localStorage.setItem(key, JSON.stringify(value)); } catch (e) { /* без сохранения тоже работает */ }
  }

  function pad(n) { return String(n).padStart(2, "0"); }

  function fmt(ms) {
    const total = Math.max(0, Math.ceil(ms / 1000));
    return pad(Math.floor(total / 60)) + ":" + pad(total % 60);
  }

  function node(tag, cls, text) {
    const n = document.createElement(tag);
    if (cls) n.className = cls;
    if (text !== undefined) n.textContent = text;
    return n;
  }

  function post(url, payload) {
    return fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
  }

  // ---------- вход в игру ----------

  function join(name) {
    return post("api/join", { id: me && me.id, name: name })
      .then(function (r) {
        if (!r.ok) throw new Error("join");
        return r.json();
      })
      .then(function (data) {
        me = { id: data.id, name: data.name };
        write(PLAYER_KEY, me);
        render();
        pollRun();
      });
  }

  el.joinForm.addEventListener("submit", function (e) {
    e.preventDefault();
    Sfx.unlock();
    const name = el.joinName.value.trim();
    if (!name) { el.joinName.focus(); return; }
    el.joinError.hidden = true;
    join(name).catch(function () {
      el.joinError.hidden = false;
      el.joinError.textContent = "Не получилось подключиться к игре. Проверьте интернет и попробуйте ещё раз.";
    });
  });

  el.rename.addEventListener("click", function () {
    el.joinName.value = me ? me.name : "";
    el.joinForm.hidden = false;
    el.pult.hidden = true;
    el.joinName.focus();
  });

  // ---------- связь с сервером ----------

  function pollRun() {
    if (!me) return;
    fetch("api/run?id=" + encodeURIComponent(me.id), { cache: "no-store" })
      .then(function (r) { return r.json(); })
      .then(function (data) {
        run = data;
        runAt = Date.now();
        // Сервер перезапускали — регистрируемся заново под тем же id.
        if (!data.players.some(function (p) { return p.id === me.id; })) join(me.name).catch(function () {});
        el.link.textContent = "Связь с игрой: есть";
        el.link.className = "play__link-state";
        renderRun();
      })
      .catch(function () {
        el.link.textContent = "Нет связи с игрой. Проверьте интернет-соединение.";
        el.link.className = "play__link-state play__link-state--bad";
      });
  }

  function pollGame() {
    fetch("api/state", { cache: "no-store" })
      .then(function (r) { return r.json(); })
      .then(function (data) {
        game = data;
        gameAt = Date.now();
        const s = data.state;
        // Новая игра — задания прошлой игры больше не нужны.
        if (s && s.gameId && read(GAME_KEY, 0) !== s.gameId) {
          if (read(GAME_KEY, 0)) write(TASKS_KEY, []);
          write(GAME_KEY, s.gameId);
        }
        renderGame();
      })
      .catch(function () { /* связь покажет pollRun */ });
  }

  // ---------- Матрица ----------

  // Сколько осталось/сколько опоздание сейчас — с поправкой на время с последнего опроса.
  function runNow() {
    if (!run) return null;
    const passed = Date.now() - runAt;
    let phase = run.phase;
    let remaining = run.remainingMs - passed;
    let over = run.overMs + passed;
    if (phase === "inside" && remaining <= 0) {
      phase = "overtime";
      over = -remaining;
    }
    return { phase: phase, remaining: remaining, over: over };
  }

  function runnerNames(exceptMe) {
    return run.runners
      .filter(function (r) { return !exceptMe || r.id !== me.id; })
      .map(function (r) { return r.name; });
  }

  function onRunnerButton() {
    Sfx.unlock();
    requestWakeLock();
    if (busy || !run) return;
    const now = runNow();
    const url = now.phase === "assigned" ? "api/run/enter" : "api/run/exit";
    busy = true;
    post(url, { id: me.id })
      .then(function (r) { return r.json(); })
      .then(function (data) {
        run = data;
        runAt = Date.now();
        if (url === "api/run/enter") Sfx.start();
        else if (data.lastOverMs > 0) Sfx.error();
        else Sfx.ok();
        renderRun();
      })
      .catch(function () { /* попробуйте нажать ещё раз */ })
      .then(function () { busy = false; });
  }

  el.runnerButton.addEventListener("click", onRunnerButton);

  function beepOncePerSecond(ms, fn) {
    const sec = Math.floor(ms / 1000);
    if (sec !== lastBeep) {
      lastBeep = sec;
      fn();
    }
  }

  function renderRunner(now) {
    const others = runnerNames(true);
    el.runnerTeam.textContent = others.length ? "с тобой: " + others.join(", ") : "";
    let phaseClass = now.phase;
    let status = "";
    let time = "";
    let sub = "";
    let button = "";
    let progress = 0;
    if (now.phase === "assigned") {
      status = "Тебя отправили в Матрицу!";
      time = fmt(run.insideMs);
      sub = "Подойди к границе Матрицы" + (others.length ? " вместе с: " + others.join(", ") : "") +
        ". Переступая границу, нажми кнопку — пойдёт отсчёт.";
      button = "Вошли в Матрицу";
      progress = 100;
    } else if (now.phase === "inside") {
      status = "Вы в Матрице";
      time = fmt(now.remaining);
      sub = "Наведи камеру телефона на QR-код фигурки. Не больше 2 фигурок — и назад! На выходе нажми кнопку.";
      button = "Вышли";
      progress = now.remaining / run.insideMs * 100;
      if (now.remaining <= run.warnMs) {
        phaseClass = "warn";
        status = "Выходите!";
        beepOncePerSecond(now.remaining, Sfx.tick);
        if (navigator.vibrate) navigator.vibrate(80);
      }
    } else if (now.phase === "overtime") {
      status = "Время вышло! Нарушение";
      time = "+" + fmt(now.over);
      sub = "Немедленно выходите из Матрицы и нажмите кнопку. Каждая секунда — штраф для всей команды.";
      button = "Вышли";
      progress = 100;
      beepOncePerSecond(now.over, Sfx.alarm);
    }
    el.runner.className = "runner gate gate--" + phaseClass;
    el.runnerStatus.textContent = status;
    el.runnerTime.textContent = time;
    el.runnerSub.textContent = sub;
    el.runnerButton.textContent = button;
    el.runnerProgress.style.width = Math.max(0, Math.min(100, progress)) + "%";
  }

  function renderRun() {
    if (!run || !me) return;
    const now = runNow();
    const isRunner = run.runners.some(function (r) { return r.id === me.id; }) &&
      (now.phase === "assigned" || now.phase === "inside" || now.phase === "overtime");

    if (isRunner && !wasRunner) {
      Sfx.start();
      if (navigator.vibrate) navigator.vibrate([300, 150, 300]);
      requestWakeLock();
    }
    wasRunner = isRunner;
    el.runner.hidden = !isRunner;
    if (isRunner) renderRunner(now);

    // Карточка Матрицы на пульте — для всех.
    const names = runnerNames(false).join(", ");
    let state = "";
    let cls = "";
    if (now.phase === "ready") { state = "Матрица открыта — капитан выбирает, кто пойдёт"; cls = "ok"; }
    else if (now.phase === "assigned") { state = "Идут к Матрице: " + names; cls = "warn"; }
    else if (now.phase === "inside") { state = "МАТРИЦА ЗАПУЩЕНА · внутри: " + names + " · осталось " + fmt(now.remaining); cls = "warn"; }
    else if (now.phase === "overtime") { state = "Опоздание! " + names + " · +" + fmt(now.over); cls = "bad"; }
    else if (now.phase === "cooldown") { state = "Матрица перезагружается · осталось " + fmt(now.remaining); cls = "cool"; }
    el.matrixState.textContent = state;
    el.matrixState.className = "play__matrix-state play__matrix-state--" + cls;

    const mine = run.players.filter(function (p) { return p.id === me.id; })[0];
    const runs = mine ? mine.runs : 0;
    let note = runs ? "Ты был в Матрице: " + runs + " раз" + (runs >= 2 && runs <= 4 ? "а" : "") + "." : "Ты ещё не был в Матрице.";
    if (now.phase === "cooldown" && run.lastOverMs > 0) {
      note = "Последний заход: опоздание " + Math.ceil(run.lastOverMs / 1000) + " сек — штраф команде. " + note;
    }
    el.matrixNote.textContent = note;

    // Баннер «МАТРИЦА ЗАПУЩЕНА» — виден всем участникам, пока идёт заход.
    const bn = document.getElementById("matrix-banner");
    if (bn) {
      const live = now.phase === "assigned" || now.phase === "inside" || now.phase === "overtime";
      let bsub = "";
      if (now.phase === "assigned") bsub = "Идут: " + names;
      else if (now.phase === "inside") bsub = "Идут: " + names + " · осталось " + fmt(now.remaining);
      else if (now.phase === "overtime") bsub = "Опоздание! +" + fmt(now.over);
      bn.hidden = !live;
      bn.className = "matrix-banner" + (now.phase === "overtime" ? " matrix-banner--warn" : "");
      document.getElementById("matrix-banner-sub").textContent = bsub;
    }
  }

  // ---------- игра ----------

  function renderTasks() {
    const list = read(TASKS_KEY, []).slice().sort(function (a, b) { return a.num - b.num; });
    el.tasks.innerHTML = "";
    el.tasksEmpty.hidden = list.length > 0;
    list.forEach(function (t) {
      const li = node("li");
      const a = node("a", "", t.title);
      a.href = t.path;
      li.appendChild(a);
      el.tasks.appendChild(li);
    });
  }

  function renderGame() {
    const s = game && game.state;
    if (!s) {
      el.time.textContent = "--:--";
      el.status.textContent = "Ждём, когда ведущий запустит терминал…";
      return;
    }
    const age = (game.ageMs >= 0 ? game.ageMs : 0) + (Date.now() - gameAt);
    const left = s.status === "running" ? s.remainingMs - age : s.remainingMs;
    el.time.textContent = fmt(left);
    el.time.className = "play__time" + (s.status === "won" ? " play__time--ok" : left < 5 * 60 * 1000 ? " play__time--warn" : "");
    const statusText = {
      intro: "Ты в игре! Твоё имя горит на экране. Ждём, когда ведущий начнёт.",
      running: "Зашифровано данных: " + s.pct + "%",
      paused: "Пауза. Ждите команды ведущего.",
      won: "Сервер защищён! Мастер-ключ: " + (s.finalWord || ""),
      lost: "Данные зашифрованы. Слушайте ведущего."
    };
    el.status.textContent = statusText[s.status] || "";

    // Списки перерисовываем только при изменениях (таймер обновляется 5 раз в секунду).
    const key = JSON.stringify([s.done, s.letters]);
    if (key === listsKey) return;
    listsKey = key;
    el.doneCount.textContent = s.done.length + " / " + s.total;
    el.slots.innerHTML = "";
    s.letters.forEach(function (letter, i) {
      const slot = node("div", "slot" + (letter ? " slot--filled" : ""));
      slot.appendChild(node("div", "slot__letter", letter));
      slot.appendChild(node("div", "slot__num", String(i + 1)));
      el.slots.appendChild(slot);
    });

    el.threats.innerHTML = "";
    s.names.forEach(function (name, i) {
      el.threats.appendChild(node("li", s.done.indexOf(i) !== -1 ? "done" : "", (i + 1) + ". " + name));
    });
  }

  function render() {
    el.joinForm.hidden = !!me;
    el.pult.hidden = !me;
    if (!me) {
      el.joinName.focus();
      return;
    }
    el.playerName.textContent = "Агент " + me.name;
    renderTasks();
    renderGame();
    renderRun();
  }

  function requestWakeLock() {
    if (wakeLock || !("wakeLock" in navigator)) return;
    navigator.wakeLock.request("screen").then(function (lock) {
      wakeLock = lock;
      lock.addEventListener("release", function () { wakeLock = null; });
    }).catch(function () { /* экран может погаснуть — отключите автоблокировку */ });
  }

  if (window.MatrixBg && window.MatrixBg.setLevel) window.MatrixBg.setLevel(0);
  render();
  if (me) join(me.name).catch(function () {});
  pollGame();
  // Опрос раз в секунду: выбранный капитаном игрок сразу видит таймер, а у бегунов он идёт одинаково.
  setInterval(function () { if (me) pollRun(); }, 1000);
  setInterval(pollGame, 2000);
  setInterval(function () { renderRun(); renderGame(); }, 200);
  window.addEventListener("storage", renderTasks);
  document.addEventListener("visibilitychange", function () {
    if (document.visibilityState === "visible") { renderTasks(); pollRun(); pollGame(); }
  });
})();
