// Главный экран игры (терминал защиты) на любом компьютере.
//
// Состояние игры (время, решённые угрозы, подсказки, журнал) хранится на сервере, а не только
// в браузере. Поэтому сервер может стоять в каморке, а терминал — на компьютере у проектора,
// и его можно перенести на другой компьютер посреди игры.
//
// Главный экран у игры один. Если терминал уже открыт и на связи на другом компьютере,
// этот компьютер спросит, перенести ли главный экран сюда. Прежний компьютер после переноса
// показывает заставку «Главный экран перенесён» с кнопкой «Вернуть сюда».
window.Terminal = (function () {
  "use strict";

  const ID_KEY = "shield-quest:terminal-id";
  const ONLINE = location.protocol.indexOf("http") === 0;

  // Номер этого браузера: по нему сервер узнаёт, какой компьютер сейчас главный экран.
  let id = "";
  try { id = localStorage.getItem(ID_KEY) || ""; } catch (e) { /* без хранилища — новый номер */ }
  if (!/^[a-z0-9]{8,32}$/.test(id)) {
    id = "";
    for (let i = 0; i < 16; i++) id += "abcdefghijklmnopqrstuvwxyz0123456789"[Math.floor(Math.random() * 36)];
    try { localStorage.setItem(ID_KEY, id); } catch (e) { /* ничего */ }
  }

  function request(method, url, data) {
    try {
      const x = new XMLHttpRequest();
      x.open(method, url, false);
      if (data) x.setRequestHeader("Content-Type", "application/json");
      x.send(data ? JSON.stringify(data) : null);
      if (x.status !== 200) return null;
      return JSON.parse(x.responseText);
    } catch (e) { return null; }
  }

  // Сохранение с другого компьютера: его часы могут отличаться от наших.
  // Переводим моменты времени из сохранения в часы этого компьютера.
  function adopt(info) {
    if (!info || !info.game || typeof info.game !== "object") return null;
    const game = info.game;
    if (info.clientNow > 0 && info.ageMs >= 0) {
      const shift = (Date.now() - info.ageMs) - info.clientNow;
      if (game.runningSince) game.runningSince += shift;
      if (game.lockUntil) game.lockUntil += shift;
    }
    return game;
  }

  function where(info) {
    const a = info && info.addr ? String(info.addr) : "";
    if (!a) return "на другом компьютере";
    if (a === "127.0.0.1" || a === "::1") return "на самом сервере";
    return "на компьютере " + a;
  }

  function claim() {
    return request("POST", "api/terminal/claim", { id: id });
  }

  function node(tag, cls, text) {
    const n = document.createElement(tag);
    if (cls) n.className = cls;
    if (text !== undefined) n.textContent = text;
    return n;
  }

  function moveHereButton(label) {
    const btn = node("button", "btn btn--primary", label);
    btn.type = "button";
    btn.addEventListener("click", function () {
      btn.disabled = true;
      if (claim()) { location.reload(); return; }
      btn.disabled = false;
      btn.textContent = "Сервер не ответил — попробуйте ещё раз";
    });
    return btn;
  }

  function backLink() {
    const a = node("a", "btn", "На старт");
    a.href = "start.html";
    return a;
  }

  // Терминал уже открыт на другом компьютере: спросить, переносить ли его сюда.
  function showBusy(info) {
    document.body.innerHTML = "";
    const box = node("div", "start host-login");
    box.appendChild(node("div", "brand__title", "Главный экран уже открыт"));
    const sec = Math.max(1, Math.round((info.ownerAgoMs || 0) / 1000));
    box.appendChild(node("p", "start__lead",
      "Терминал защиты сейчас работает " + where(info) + " (на связи " + sec + " с назад). " +
      "Главный экран у игры один."));
    box.appendChild(node("p", "muted",
      "Можно перенести его на этот компьютер: время, решённые угрозы, подсказки и подключённые агенты " +
      "сохранятся. На прежнем компьютере появится заставка «Главный экран перенесён»."));
    const row = node("div", "terminal-move__actions");
    row.appendChild(moveHereButton("Перенести главный экран сюда"));
    row.appendChild(backLink());
    box.appendChild(row);
    document.body.appendChild(box);
  }

  // Главный экран забрал другой компьютер: заставка поверх игры.
  function showMoved(addr) {
    if (document.getElementById("terminal-moved")) return;
    const overlay = node("div", "overlay overlay--moved");
    overlay.id = "terminal-moved";
    const modal = node("div", "modal");
    modal.appendChild(node("div", "modal__alert modal__alert--warn", "Главный экран перенесён"));
    modal.appendChild(node("h2", "modal__title", "Игра продолжается " + where({ addr: addr })));
    modal.appendChild(node("p", "",
      "Здесь терминал остановлен, чтобы два экрана не мешали друг другу. " +
      "Если нужно, верните главный экран на этот компьютер — игра продолжится с того же места."));
    const row = node("div", "terminal-move__actions");
    row.appendChild(moveHereButton("Вернуть главный экран сюда"));
    row.appendChild(backLink());
    modal.appendChild(row);
    overlay.appendChild(modal);
    document.body.appendChild(overlay);
  }

  // Запуск терминала. Возвращает:
  //   { mode: "local" }            — нет сервера (файл открыт с диска или старый сервер): как раньше;
  //   { mode: "busy" }             — главный экран на другом компьютере, показан вопрос о переносе;
  //   { mode: "main", game: {...}} — этот компьютер — главный экран; game — сохранение с сервера или null.
  function start() {
    if (!ONLINE) return { mode: "local", game: null };
    const info = request("GET", "api/terminal");
    if (!info) return { mode: "local", game: null };
    if (info.owner && info.owner !== id && info.ownerOnline) {
      showBusy(info);
      return { mode: "busy", game: null };
    }
    const claimed = claim();
    if (!claimed) return { mode: "local", game: null };
    window.addEventListener("pagehide", release);
    return { mode: "main", game: adopt(claimed) };
  }

  // Отправить состояние игры на сервер. Ответ: "ok", "moved" (главный экран забрали) или "error".
  let movedAddr = "";
  function push(game) {
    return fetch("api/terminal/save", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ id: id, now: Date.now(), game: game })
    }).then(function (r) {
      if (r.status === 409) {
        return r.json().catch(function () { return {}; }).then(function (d) {
          movedAddr = (d && d.addr) || "";
          return "moved";
        });
      }
      return r.ok ? "ok" : "error";
    }).catch(function () { return "error"; });
  }

  // Терминал закрыли — главный экран свободен, другой компьютер займёт его без вопроса.
  function release() {
    if (!navigator.sendBeacon) return;
    try {
      navigator.sendBeacon("api/terminal/release", new Blob([JSON.stringify({ id: id })], { type: "text/plain" }));
    } catch (e) { /* не страшно: через 15 с без связи экран и так считается свободным */ }
  }

  function stopped() {
    window.removeEventListener("pagehide", release);
    showMoved(movedAddr);
  }

  return { start: start, push: push, stopped: stopped, info: function () { return request("GET", "api/terminal"); },
    where: where, id: function () { return id; } };
})();
