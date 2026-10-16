// Вход ведущего по ключу (для онлайн-хостинга).
// Ключ хранится в cookie «host» этого браузера. По нему сервер отдаёт полный config.js
// (с ответами) и разрешает команды ведущего. Телефонам игроков и капитана ключ не нужен.
window.HostLogin = (function () {
  "use strict";

  function getKey() {
    const m = document.cookie.match(/(?:^|;\s*)host=([^;]*)/);
    return m ? decodeURIComponent(m[1]) : "";
  }

  function setKey(value) {
    document.cookie = "host=" + encodeURIComponent(value) + "; path=/; max-age=" + (7 * 24 * 3600) +
      "; samesite=lax" + (location.protocol === "https:" ? "; secure" : "");
  }

  function clearKey() {
    document.cookie = "host=; path=/; max-age=0; samesite=lax";
  }

  // Ссылка вида ...?host=КЛЮЧ: запоминаем ключ и убираем его из адресной строки,
  // чтобы он не светился на проекторе. Если страница уже открылась без прав — перезагрузим её.
  let keyFromUrl = false;
  (function stripKeyFromUrl() {
    if (!/[?&]host=/.test(location.search) || !window.history || !history.replaceState) return;
    const p = new URLSearchParams(location.search);
    const key = p.get("host");
    if (key) { setKey(key); keyFromUrl = true; }
    p.delete("host");
    const q = p.toString();
    history.replaceState(null, "", location.pathname + (q ? "?" + q : "") + location.hash);
  })();

  function node(tag, cls, text) {
    const n = document.createElement(tag);
    if (cls) n.className = cls;
    if (text !== undefined) n.textContent = text;
    return n;
  }

  // Показать форму входа вместо страницы ведущего.
  function show(container, title, lead) {
    if (keyFromUrl) { keyFromUrl = false; location.reload(); return; }
    const tried = getKey();
    container.innerHTML = "";
    const box = node("div", "start host-login");
    box.appendChild(node("div", "brand__title", title || "Вход ведущего"));
    box.appendChild(node("p", "start__lead", lead || "Эта страница — для ведущего. Введите ключ ведущего."));

    const form = node("form", "host-login__form");
    const input = node("input", "key-input");
    input.type = "password";
    input.autocomplete = "current-password";
    input.placeholder = "Ключ ведущего";
    input.required = true;
    const btn = node("button", "btn btn--primary", "Войти");
    btn.type = "submit";
    form.appendChild(input);
    form.appendChild(btn);
    box.appendChild(form);

    const msg = node("p", "host-login__msg");
    box.appendChild(msg);
    const back = node("a", "btn", "На старт");
    back.href = "start.html";
    box.appendChild(back);
    container.appendChild(box);

    if (tried) {
      clearKey();
      msg.classList.add("host-login__msg--bad");
      msg.textContent = "Ключ не подошёл. Проверьте его и попробуйте снова.";
    }
    // Подсказка, если ключ на сервере ещё не задан.
    fetch("api/host", { cache: "no-store" })
      .then(function (r) { return r.json(); })
      .then(function (info) {
        if (info && info.configured === false) {
          msg.classList.add("host-login__msg--bad");
          msg.textContent = "На сервере ещё не задан ключ ведущего. Задайте его по инструкции " +
            "(файл backend/key.php или переменная HOST_KEY) и обновите страницу.";
        }
      })
      .catch(function () {
        msg.classList.add("host-login__msg--bad");
        msg.textContent = "Сервер игры не отвечает. Сайт должен быть запущен через server.js (Node) " +
          "или с папкой backend (PHP) — простой статический хостинг не подойдёт.";
      });

    form.addEventListener("submit", function (e) {
      e.preventDefault();
      const v = input.value.trim();
      if (!v) return;
      setKey(v);
      location.reload();
    });
    input.focus();
  }

  function logout() {
    clearKey();
    location.href = "start.html";
  }

  return { show: show, logout: logout, has: function () { return !!getKey(); } };
})();
