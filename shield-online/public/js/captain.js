// Телефон капитана: подключается по своему QR-коду с экрана терминала.
// Капитан видит всех игроков и отправляет выбранных в Матрицу —
// на их телефонах сразу включается таймер Матрицы.
(function () {
  "use strict";

  const CAPTAIN_KEY = "shield-quest:captain";

  const $ = function (id) { return document.getElementById(id); };
  const el = {
    claimForm: $("claim-form"),
    claimName: $("claim-name"),
    claimError: $("claim-error"),
    cap: $("cap"),
    capName: $("cap-name"),
    capClock: $("cap-clock"),
    phase: $("cap-phase"),
    time: $("cap-time"),
    note: $("cap-note"),
    actions: $("cap-actions"),
    picked: $("cap-picked"),
    players: $("cap-players"),
    send: $("cap-send"),
    link: $("link")
  };

  let me = read(CAPTAIN_KEY, null);  // { token, name }
  let run = null;
  let runAt = 0;
  let game = null;
  let gameAt = 0;
  let selected = [];
  let busy = false;
  let listKey = "";
  let boot = "";

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

  // ---------- назначение капитана ----------

  function claim(name) {
    return post("api/captain/claim", { token: me && me.token, name: name })
      .then(function (r) {
        if (r.status === 409) throw new Error("taken");
        if (!r.ok) throw new Error("net");
        return r.json();
      })
      .then(function (data) {
        me = { token: data.token, name: data.name || name };
        write(CAPTAIN_KEY, me);
        render();
        poll();
      });
  }

  function claimFailed(e) {
    me = null;
    write(CAPTAIN_KEY, null);
    render();
    el.claimError.hidden = false;
    el.claimError.textContent = e.message === "taken"
      ? "Капитан уже назначен" + (run && run.captainName ? ": " + run.captainName : "") +
        ". Если капитан сменился, ведущий нажмёт «Сменить капитана» в панели организатора."
      : "Не получилось подключиться к игре. Проверьте интернет и попробуйте ещё раз.";
  }

  el.claimForm.addEventListener("submit", function (e) {
    e.preventDefault();
    Sfx.unlock();
    const name = el.claimName.value.trim();
    if (!name) { el.claimName.focus(); return; }
    el.claimError.hidden = true;
    claim(name).catch(claimFailed);
  });

  // ---------- связь ----------

  function poll() {
    const q = me ? "?captain=" + encodeURIComponent(me.token) : "";
    fetch("api/run" + q, { cache: "no-store" })
      .then(function (r) { return r.json(); })
      .then(function (data) {
        const restarted = boot && data.boot !== boot;
        boot = data.boot;
        run = data;
        runAt = Date.now();
        if (me && !data.captain) {
          if (restarted) {
            // Сервер перезапускали — снова занимаем место капитана.
            claim(me.name).catch(claimFailed);
          } else {
            // Ведущий нажал «Сменить капитана».
            me = null;
            write(CAPTAIN_KEY, null);
            render();
            el.claimError.hidden = false;
            el.claimError.textContent = "Ведущий передал роль капитана. Если капитаном снова будешь ты — введи имя.";
          }
        }
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
      .then(function (data) { game = data; gameAt = Date.now(); })
      .catch(function () {});
  }

  function action(url, payload, sound) {
    if (busy) return;
    busy = true;
    Sfx.unlock();
    post(url, Object.assign({ token: me.token }, payload || {}))
      .then(function (r) {
        if (!r.ok) return r.text().then(function (t) { throw new Error(t); });
        return r.json();
      })
      .then(function (data) {
        run = data;
        runAt = Date.now();
        if (sound) sound();
        selected = [];
        renderRun();
      })
      .catch(function (e) {
        el.note.textContent = e.message || "Не получилось. Попробуйте ещё раз.";
        Sfx.error();
      })
      .then(function () { busy = false; });
  }

  // ---------- отрисовка ----------

  function runNow() {
    const passed = Date.now() - runAt;
    let phase = run.phase;
    let remaining = run.remainingMs - passed;
    let over = run.overMs + passed;
    if (phase === "inside" && remaining <= 0) { phase = "overtime"; over = -remaining; }
    return { phase: phase, remaining: remaining, over: over };
  }

  function button(text, cls, onClick) {
    const b = node("button", "btn " + (cls || ""), text);
    b.type = "button";
    b.addEventListener("click", onClick);
    return b;
  }

  function renderMatrix(now) {
    const names = run.runners.map(function (r) { return r.name; }).join(", ");
    let phase = "";
    let time = "";
    let note = "";
    let key = now.phase;
    if (now.phase === "ready") {
      phase = "Открыта";
      note = "Выбери, кто пойдёт, — ниже.";
    } else if (now.phase === "assigned") {
      phase = "Идут к Матрице";
      time = names;
      note = "У бегунов на телефонах таймер. Отсчёт начнётся, когда они нажмут «Вошли».";
    } else if (now.phase === "inside") {
      phase = "Внутри: " + names;
      time = fmt(now.remaining);
      note = "Если у бегуна сел телефон — нажми «Вышли» сам, когда они вернутся.";
    } else if (now.phase === "overtime") {
      phase = "Опоздание! " + names;
      time = "+" + fmt(now.over);
      note = "Каждая секунда — штраф. Срочно зови их назад!";
    } else if (now.phase === "cooldown") {
      phase = "Перезагрузка";
      time = fmt(now.remaining);
      note = run.lastOverMs > 0
        ? "Прошлый заход: опоздание " + Math.ceil(run.lastOverMs / 1000) + " сек. Пока Матрица закрыта — решайте задания."
        : "Пока Матрица закрыта — решайте задания.";
    }
    el.phase.textContent = phase;
    el.time.textContent = time;
    if (!busy) el.note.textContent = note;
    el.phase.parentNode.className = "panel cap__matrix cap__matrix--" + now.phase;

    // Кнопки управления заходом перерисовываем только при смене фазы.
    if (el.actions.getAttribute("data-phase") !== key) {
      el.actions.setAttribute("data-phase", key);
      el.actions.innerHTML = "";
      if (now.phase === "assigned") {
        el.actions.appendChild(button("Вошли", "", function () { action("api/run/enter", {}, Sfx.start); }));
        el.actions.appendChild(button("Отменить заход", "btn--danger", function () { action("api/run/cancel"); }));
      } else if (now.phase === "inside" || now.phase === "overtime") {
        el.actions.appendChild(button("Бегуны вышли", "", function () { action("api/run/exit", {}, Sfx.ok); }));
      }
    }
  }

  function renderPlayers(now) {
    const ready = now.phase === "ready";
    const max = run.maxRunners || 2;
    const ids = run.players.map(function (p) { return p.id; });
    selected = selected.filter(function (id) { return ids.indexOf(id) !== -1; });

    const list = run.players.slice().sort(function (a, b) {
      return (a.runs - b.runs) || a.name.localeCompare(b.name, "ru");
    });
    // Список перерисовываем, только если он поменялся: иначе палец «промахивается» по кнопкам.
    const key = JSON.stringify([ready, selected, list.map(function (p) { return [p.id, p.name, p.runs, p.online]; })]);
    if (key !== listKey) {
      listKey = key;
      el.players.innerHTML = "";
      if (!list.length) el.players.appendChild(node("li", "muted", "Игроки ещё не подключились. Они сканируют QR-код на экране."));
      list.forEach(function (p) {
        const on = selected.indexOf(p.id) !== -1;
        const li = node("li");
        const b = node("button", "cap__player" + (on ? " cap__player--on" : "") + (p.online ? "" : " cap__player--off"));
        b.type = "button";
        b.disabled = !ready;
        b.appendChild(node("span", "cap__player-name", (on ? "✓ " : "") + p.name));
        b.appendChild(node("span", "cap__player-runs", p.runs ? "в Матрице: " + p.runs : "ещё не был"));
        b.addEventListener("click", function () {
          const i = selected.indexOf(p.id);
          if (i !== -1) selected.splice(i, 1);
          else if (selected.length < max) selected.push(p.id);
          else { selected.shift(); selected.push(p.id); }
          renderPlayers(runNow());
        });
        li.appendChild(b);
        el.players.appendChild(li);
      });
    }
    el.picked.textContent = "выбрано " + selected.length + " из " + max;
    el.send.disabled = !ready || !selected.length || busy;
    el.send.textContent = ready ? "Отправить в Матрицу" : "Матрица занята";
  }

  function renderClock() {
    const s = game && game.state;
    if (!s) { el.capClock.textContent = ""; return; }
    const age = (game.ageMs >= 0 ? game.ageMs : 0) + (Date.now() - gameAt);
    const left = s.status === "running" ? s.remainingMs - age : s.remainingMs;
    el.capClock.textContent = "до шифрования " + fmt(left);
  }

  function renderRun() {
    if (!run || !me) return;
    const now = runNow();
    renderMatrix(now);
    renderPlayers(now);
    renderClock();
  }

  el.send.addEventListener("click", function () {
    if (!selected.length) return;
    action("api/run/send", { runners: selected.slice() }, Sfx.start);
  });

  function render() {
    el.claimForm.hidden = !!me;
    el.cap.hidden = !me;
    if (me) el.capName.textContent = "Капитан " + me.name;
  }

  if (window.MatrixBg && window.MatrixBg.setLevel) window.MatrixBg.setLevel(0);
  render();
  function startPolling() {
    poll();
    pollGame();
    setInterval(poll, 1000);
    setInterval(pollGame, 3000);
    setInterval(renderRun, 250);
  }
  // Сначала снова занимаем место капитана (после перезагрузки страницы), потом начинаем опрос.
  if (me) claim(me.name).catch(claimFailed).then(startPolling);
  else { el.claimName.focus(); startPolling(); }
})();
