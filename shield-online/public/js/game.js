(function () {
  "use strict";

  const C = window.QUEST_CONFIG;
  // Без ключа ведущего сервер отдаёт config.js без ответов — показываем вход ведущего.
  if (!C || !C.stages) {
    window.HostLogin.show(document.body, "Терминал ведущего",
      "Терминал с ответами открывает только ведущий. Введите ключ ведущего — его задают при размещении сайта.");
    return;
  }
  const STORAGE_KEY = "shield-quest:state-v2";
  // Настройки из конструктора (setup.json) заменяют значения из config.js до старта игры.
  applySettings(loadSettings());
  const TOTAL_MS = C.totalMinutes * 60 * 1000;

  function loadSettings() {
    if (location.protocol.indexOf("http") !== 0) return null;
    try {
      const x = new XMLHttpRequest();
      x.open("GET", "api/setup", false);
      x.send(null);
      const data = JSON.parse(x.responseText);
      return x.status === 200 ? data.settings || null : null;
    } catch (e) { return null; }
  }

  function applySettings(s) {
    if (!s) return;
    C.totalMinutes = s.totalMinutes;
    C.wrongPenaltySeconds = s.wrongPenaltySeconds;
    C.maxWrongBeforeLock = s.maxWrongBeforeLock;
    C.lockSeconds = s.lockSeconds;
    C.hintPenaltySeconds = s.hintPenaltySeconds;
    C.teamName = s.teamName;
    C.matrix = Object.assign({}, C.matrix, {
      insideSeconds: s.insideSeconds, cooldownSeconds: s.cooldownSeconds, warnSeconds: s.warnSeconds,
      runnersPerVisit: s.runnersPerVisit, autoPenaltySeconds: s.autoPenaltySeconds
    });
  }
  // Угрозы, выбранные в конструкторе (setup.html). Без выбора играют все 11.
  const ALL_STAGES = C.stages;
  const SETUP = loadSetup();
  const STAGES = ALL_STAGES.filter(function (s, i) { return SETUP.indexOf(i + 1) !== -1; });
  const COUNT = STAGES.length;
  // Номер фигурки (он же номер QR-наклейки), а не порядковый номер в текущей игре.
  function numOf(s) { return ALL_STAGES.indexOf(s) + 1; }
  // Если выбраны не все угрозы, мастер-ключ собирается из букв выбранных угроз по порядку.
  const FINAL = C.finalKey ? (COUNT === ALL_STAGES.length ? C.finalKey : partialFinal()) : null;

  function loadSetup() {
    const all = ALL_STAGES.map(function (s, i) { return i + 1; });
    if (location.protocol.indexOf("http") !== 0) return all;
    try {
      const x = new XMLHttpRequest();
      x.open("GET", "api/setup", false);
      x.send(null);
      const data = JSON.parse(x.responseText);
      if (x.status === 200 && data.stages && data.stages.length) return data.stages;
    } catch (e) { /* сервер недоступен — играем все угрозы */ }
    return all;
  }

  function partialFinal() {
    const letters = STAGES.map(function (s) { return s.letter || ""; }).join("");
    return {
      answers: [letters.toLowerCase()],
      word: letters,
      text: C.finalKey.text,
      hint: C.finalKey.hint
    };
  }
  const FINAL_INDEX = -1;
  const ALERT_MINUTES = [10, 5, 1];
  const HINT_MS = (C.hintPenaltySeconds || 0) * 1000;
  const HINT_CONFIRM_MS = 4000;
  const TEAM = C.teamName || "Отдел ИБ";
  const ONLINE = location.protocol.indexOf("http") === 0;
  const AUTO_PENALTY_MS = ((C.matrix && C.matrix.autoPenaltySeconds) || 0) * 1000;
  // Если таймер не присылал состояние дольше этого времени, считаем, что связи нет.
  const GATE_STALE_MS = 10000;

  const $ = function (id) { return document.getElementById(id); };
  const el = {
    brandTitle: $("brand-title"),
    serverName: $("server-name"),
    teamName: $("team-name"),
    serverStatus: $("server-status"),
    gateMini: $("gate-mini"),
    gateMiniValue: $("gate-mini-value"),
    clockBox: $("clock-box"),
    clock: $("clock"),
    stages: $("stages"),
    stagesCount: $("stages-count"),
    mission: $("mission"),
    report: $("report"),
    reportImg: $("report-img"),
    reportTitle: $("report-title"),
    reportText: $("report-text"),
    overview: $("overview"),
    missionLabel: $("mission-label"),
    missionTitle: $("mission-title"),
    missionText: $("mission-text"),
    missionHint: $("mission-hint"),
    howto: $("howto"),
    task: $("task"),
    taskImg: $("task-img"),
    taskLabel: $("task-label"),
    taskTitle: $("task-title"),
    taskWhere: $("task-where"),
    taskText: $("task-text"),
    taskFormat: $("task-format"),
    taskHint: $("task-hint"),
    taskHintBtn: $("task-hint-btn"),
    taskBack: $("task-back"),
    slots: $("slots"),
    masterkey: $("masterkey"),
    form: $("key-form"),
    input: $("key-input"),
    submit: $("key-submit"),
    feedback: $("feedback"),
    virusName: $("virus-name"),
    pct: $("infection-pct"),
    bar: $("infection-bar"),
    log: $("log"),
    intro: $("intro"),
    introTitle: $("intro-title"),
    briefing: $("briefing"),
    introForm: $("intro-form"),
    introStart: $("intro-start"),
    paused: $("paused"),
    win: $("win"),
    winLead: $("win-lead"),
    winStats: $("win-stats"),
    winKeys: $("win-keys"),
    lost: $("lost"),
    lostLead: $("lost-lead"),
    admin: $("admin"),
    adminLogin: $("admin-login"),
    adminPin: $("admin-pin"),
    adminControls: $("admin-controls"),
    adminInfo: $("admin-info"),
    adminStage: $("admin-stage"),
    adminAnswer: $("admin-answer"),
    joinMini: $("join-mini"),
    teamsOnline: $("teams-online"),
    joinOpen: $("join-open"),
    join: $("join"),
    joinClose: $("join-close"),
    lobby: $("lobby"),
    lobbyNext: $("lobby-next"),
    lobbyCount: $("lobby-count")
  };

  let state = load();
  let feedback = { text: "", kind: "" };
  let lockWasActive = false;
  // Угроза, открытая в карточке (только на этом экране, не сохраняется).
  let selected = null;
  let hintConfirmUntil = 0;
  // Экран перед игрой: сначала агенты и капитан подключаются по QR («lobby»), потом брифинг.
  let introStep = "lobby";
  let lobbyReady = false;

  // ---------- состояние ----------

  function defaultState() {
    return {
      status: "intro", // intro | running | paused | won | lost
      team: "",
      done: [],
      lastSolved: null,
      elapsedMs: 0,
      runningSince: null,
      penaltyMs: 0,
      wrongStreak: 0,
      wrongTotal: 0,
      hints: [],
      lockUntil: 0,
      alerted: [],
      log: [],
      lastEventId: 0,
      eventsBoot: "",
      startedAt: 0
    };
  }

  // Новая игра, но без повторного применения уже обработанных событий таймера.
  function freshState() {
    const next = defaultState();
    next.lastEventId = state.lastEventId;
    next.eventsBoot = state.eventsBoot;
    return next;
  }

  function load() {
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY));
      if (saved && typeof saved === "object") return Object.assign(defaultState(), saved);
    } catch (e) { /* повреждённое сохранение — начинаем заново */ }
    return defaultState();
  }

  function save() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch (e) { /* хранилище недоступно — игра работает без сохранения */ }
  }

  function elapsed() {
    return state.elapsedMs + (state.runningSince ? Date.now() - state.runningSince : 0);
  }

  function used() {
    return elapsed() + state.penaltyMs;
  }

  function remaining() {
    return TOTAL_MS - used();
  }

  function isLocked() {
    return Date.now() < state.lockUntil;
  }

  function isDone(i) {
    return state.done.indexOf(i) !== -1;
  }

  function allThreatsDone() {
    return state.done.length >= COUNT;
  }

  function freezeClock() {
    state.elapsedMs = elapsed();
    state.runningSince = null;
  }

  function runClock() {
    if (!state.runningSince) state.runningSince = Date.now();
  }

  // ---------- проверка ответа ----------

  const EN_TO_RU = {
    q: "й", w: "ц", e: "у", r: "к", t: "е", y: "н", u: "г", i: "ш", o: "щ", p: "з", "[": "х", "]": "ъ",
    a: "ф", s: "ы", d: "в", f: "а", g: "п", h: "р", j: "о", k: "л", l: "д", ";": "ж", "'": "э",
    z: "я", x: "ч", c: "с", v: "м", b: "и", n: "т", m: "ь", ",": "б", ".": "ю", "`": "ё"
  };

  function normalize(s) {
    return String(s).toLowerCase().replace(/ё/g, "е").replace(/[^a-zа-я0-9]/g, "");
  }

  function fromEnLayout(s) {
    return String(s).toLowerCase().split("").map(function (ch) {
      return EN_TO_RU[ch] || ch;
    }).join("");
  }

  function check(raw, answers) {
    const variants = answers.map(normalize);
    if (variants.indexOf(normalize(raw)) !== -1) return "ok";
    if (variants.indexOf(normalize(fromEnLayout(raw))) !== -1) return "layout";
    return "wrong";
  }

  // ---------- утилиты ----------

  function pad(n) {
    return String(n).padStart(2, "0");
  }

  function fmt(ms, roundUp) {
    const total = Math.max(0, roundUp ? Math.ceil(ms / 1000) : Math.floor(ms / 1000));
    return pad(Math.floor(total / 60)) + ":" + pad(total % 60);
  }

  function node(tag, cls, text) {
    const n = document.createElement(tag);
    if (cls) n.className = cls;
    if (text !== undefined) n.textContent = text;
    return n;
  }

  function addLog(text, kind) {
    state.log.push({ at: fmt(elapsed()), text: text, kind: kind || "" });
    if (state.log.length > 80) state.log.splice(0, state.log.length - 80);
  }

  function setFeedback(text, kind) {
    feedback = { text: text, kind: kind || "" };
    el.feedback.textContent = text;
    el.feedback.className = "feedback" + (kind ? " feedback--" + kind : "");
  }

  function flash(cls) {
    el.mission.classList.remove(cls);
    void el.mission.offsetWidth;
    el.mission.classList.add(cls);
  }

  // ---------- игровые действия ----------

  function start() {
    state = freshState();
    state.team = TEAM;
    state.status = "running";
    state.startedAt = Date.now();
    selected = null;
    runClock();
    addLog("Подключение к " + C.serverName + " установлено. " + state.team + " в сети.", "good");
    addLog("Обнаружен вирус " + C.virusName + ". Началось шифрование данных!", "bad");
    save();
    setFeedback("", "");
    renderAll();
    Sfx.start();
    el.input.focus();
  }

  function win() {
    freezeClock();
    state.status = "won";
    addLog("Мастер-ключ принят. Вирус " + C.virusName + " удалён. Сервер защищён!", "good");
    Sfx.win();
  }

  function lose() {
    freezeClock();
    state.status = "lost";
    addLog("Данные зашифрованы на 100%. Сервер потерян.", "bad");
    save();
    renderAll();
    Sfx.alarm();
  }

  function solveThreat(i, byOperator) {
    if (isDone(i)) return;
    const s = STAGES[i];
    state.done.push(i);
    state.lastSolved = i;
    state.wrongStreak = 0;
    state.lockUntil = 0;
    addLog((byOperator ? "Оператор обезвредил угрозу «" : "Ключ принят. Угроза «") + s.vulnerability +
      "» обезврежена." + (s.letter ? " Получена буква «" + s.letter + "»." : ""), "good");
    if (allThreatsDone()) {
      if (FINAL) {
        addLog("Все угрозы обезврежены. Введите мастер-ключ!", "warn");
      } else {
        win();
      }
    }
  }

  function hasHint(i) {
    return state.hints.indexOf(i) !== -1;
  }

  function onHintClick() {
    Sfx.unlock();
    if (selected === null || state.status !== "running" || hasHint(selected) || isDone(selected)) return;
    // Первое нажатие только предупреждает о цене, второе — берёт подсказку.
    if (Date.now() > hintConfirmUntil) {
      hintConfirmUntil = Date.now() + HINT_CONFIRM_MS;
      renderTask();
      return;
    }
    hintConfirmUntil = 0;
    const s = STAGES[selected];
    state.hints.push(selected);
    state.penaltyMs += HINT_MS;
    addLog("Взята подсказка к угрозе «" + s.vulnerability + "»" +
      (HINT_MS ? ": вирус ускорился на " + Math.round(HINT_MS / 60000) + " мин." : "."), "warn");
    save();
    renderAll();
    Sfx.error();
    el.input.focus();
  }

  function selectThreat(i) {
    selected = selected === i ? null : i;
    hintConfirmUntil = 0;
    renderStages();
    renderMission();
    if (state.status === "running" && !isLocked()) el.input.focus();
  }

  function onSubmit(e) {
    e.preventDefault();
    Sfx.unlock();
    if (state.status !== "running" || isLocked()) return;

    const raw = el.input.value;
    if (!normalize(raw)) {
      el.input.focus();
      return;
    }
    el.input.value = "";

    let result = "wrong";
    let solvedIndex = null;

    if (allThreatsDone() && FINAL) {
      result = check(raw, FINAL.answers);
      if (result !== "wrong") solvedIndex = FINAL_INDEX;
    } else {
      for (let i = 0; i < COUNT && solvedIndex === null; i++) {
        const r = check(raw, STAGES[i].answers);
        if (r === "wrong") continue;
        if (isDone(i)) {
          setFeedback("Угроза «" + STAGES[i].vulnerability + "» уже обезврежена. Этот ключ больше не нужен.", "");
          el.input.focus();
          return;
        }
        result = r;
        solvedIndex = i;
      }
    }

    if (solvedIndex === null) {
      state.wrongTotal++;
      state.wrongStreak++;
      state.penaltyMs += C.wrongPenaltySeconds * 1000;
      addLog("Ключ «" + raw.trim() + "» отклонён. Вирус ускорился на " + C.wrongPenaltySeconds + " сек.", "bad");
      let msg = "Доступ запрещён. Неверный ключ — вирус ускорился на " + C.wrongPenaltySeconds + " сек.";
      if (C.maxWrongBeforeLock && state.wrongStreak >= C.maxWrongBeforeLock) {
        state.wrongStreak = 0;
        state.lockUntil = Date.now() + C.lockSeconds * 1000;
        addLog("Сработала защита от подбора: ввод заблокирован на " + C.lockSeconds + " сек.", "bad");
        msg = "Слишком много ошибок подряд! Защита от подбора заблокировала ввод.";
      }
      setFeedback(msg, "bad");
      flash("shake");
      Sfx.error();
    } else if (solvedIndex === FINAL_INDEX) {
      win();
      setFeedback("", "");
    } else {
      solveThreat(solvedIndex, false);
      selected = null;
      const layoutNote = result === "layout" ? " (набрано в английской раскладке — засчитано)" : "";
      if (state.status === "won") {
        setFeedback("", "");
      } else {
        const s = STAGES[solvedIndex];
        setFeedback("Доступ разрешён. «" + s.vulnerability + "» обезврежена!" +
          (s.letter ? " Буква «" + s.letter + "» добавлена в мастер-ключ." : "") + layoutNote, "good");
        flash("glow");
        Sfx.ok();
      }
    }

    save();
    renderAll();
    if (state.status === "running") el.input.focus();
  }

  // ---------- отрисовка ----------

  function renderStatic() {
    document.title = C.title + " — терминал защиты";
    el.brandTitle.textContent = C.title;
    el.serverName.textContent = C.serverName;
    el.virusName.textContent = C.virusName;
    el.introTitle.textContent = "Сервер " + C.serverName + " атакован";
    el.briefing.innerHTML = "";
    C.briefing.forEach(function (p, i) {
      const para = node("p", "briefing__line", p);
      para.style.animationDelay = (i * 0.6) + "s";
      el.briefing.appendChild(para);
    });

    el.adminStage.innerHTML = "";
    STAGES.forEach(function (s, i) {
      const opt = node("option", "", (i + 1) + ". " + s.vulnerability);
      opt.value = String(i);
      el.adminStage.appendChild(opt);
    });
    if (FINAL) {
      const opt = node("option", "", "* Мастер-ключ");
      opt.value = String(FINAL_INDEX);
      el.adminStage.appendChild(opt);
    }
  }

  function renderStages() {
    el.stages.innerHTML = "";
    el.stagesCount.textContent = state.done.length + " / " + COUNT;
    STAGES.forEach(function (s, i) {
      const done = isDone(i);
      const li = node("li", "stage " + (done ? "stage--done" : "stage--open") + (selected === i ? " stage--selected" : ""));
      li.setAttribute("data-index", String(i));
      li.title = "Показать задание";
      if (s.image) {
        const img = node("img", "stage__img");
        img.src = s.image;
        img.alt = "";
        li.appendChild(img);
      } else {
        li.appendChild(node("span", "stage__img"));
      }
      li.appendChild(node("span", "stage__num", String(numOf(s))));
      li.appendChild(node("span", "stage__name", s.vulnerability));
      li.appendChild(node("span", "stage__status", done ? "обезврежена" : "активна"));
      el.stages.appendChild(li);
    });
    if (FINAL) {
      const won = state.status === "won";
      const ready = allThreatsDone();
      const li = node("li", "stage " + (won ? "stage--done" : ready ? "stage--active" : "stage--locked"));
      li.appendChild(node("span", "stage__img stage__img--key", "★"));
      li.appendChild(node("span", "stage__num", ""));
      li.appendChild(node("span", "stage__name", "Мастер-ключ"));
      li.appendChild(node("span", "stage__status", won ? "принят" : ready ? "введите!" : "закрыт"));
      el.stages.appendChild(li);
    }
  }

  // Какие буквы уже были на экране: новая буква «расшифровывается» на глазах.
  let shownSlots = null;
  const DECODE_GLYPHS = "ｱｲｳｴｵｶｷｸｹｺ0123456789ЖЩЫЮЯ#%&";

  function decode(target, text) {
    const reduced = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduced) { target.textContent = text; return; }
    let n = 0;
    const timer = setInterval(function () {
      n++;
      if (n >= 18 || !target.isConnected) {
        clearInterval(timer);
        target.textContent = text;
        return;
      }
      target.textContent = DECODE_GLYPHS[Math.floor(Math.random() * DECODE_GLYPHS.length)];
    }, 55);
  }

  function renderSlots() {
    el.masterkey.hidden = !FINAL;
    if (!FINAL) return;
    el.slots.innerHTML = "";
    const firstRender = shownSlots === null;
    if (firstRender) shownSlots = [];
    STAGES.forEach(function (s, i) {
      const done = isDone(i) || state.status === "won";
      const isNew = done && !firstRender && !shownSlots[i];
      shownSlots[i] = done;
      const slot = node("div", "slot" + (done ? " slot--filled" : "") + (isNew ? " slot--new" : ""));
      const letter = node("div", "slot__letter", done ? (s.letter || "?") : "");
      slot.appendChild(letter);
      if (isNew) decode(letter, s.letter || "?");
      slot.appendChild(node("div", "slot__num", String(numOf(s))));
      el.slots.appendChild(slot);
    });
  }

  function renderTask() {
    const i = selected;
    const s = STAGES[i];
    const done = isDone(i);
    el.taskImg.hidden = !s.image;
    if (s.image) el.taskImg.src = s.image;
    el.taskLabel.textContent = "Угроза №" + (i + 1) + (done ? ", обезврежена" : ", активна");
    el.taskTitle.textContent = s.vulnerability;
    el.taskWhere.textContent = "Задание — по QR-коду на фигурке №" + (i + 1) + " " + (s.figure || "") + ".";
    el.taskText.textContent = s.task || "Отсканируйте QR-код на фигурке и следуйте инструкции.";
    el.taskFormat.textContent = s.answerFormat || "слово или число";

    if (done) {
      el.taskHint.hidden = !s.fact;
      el.taskHint.className = "task__hint task__hint--fact";
      el.taskHint.textContent = s.fact || "";
      el.taskHintBtn.hidden = true;
      return;
    }
    el.taskHint.className = "task__hint";
    const opened = hasHint(i);
    el.taskHint.hidden = !opened;
    el.taskHint.textContent = opened ? "Подсказка: " + (s.hint || "спросите ведущего.") : "";
    el.taskHintBtn.hidden = opened || !s.hint;
    const confirming = Date.now() <= hintConfirmUntil;
    const cost = HINT_MS ? " (−" + Math.round(HINT_MS / 60000) + " мин)" : "";
    el.taskHintBtn.textContent = confirming ? "Точно? Нажмите ещё раз" + cost : "Подсказка" + cost;
    el.taskHintBtn.classList.toggle("btn--confirm", confirming);
    el.taskHintBtn.disabled = state.status !== "running";
  }

  function renderMission() {
    const won = state.status === "won";
    const last = state.lastSolved !== null ? STAGES[state.lastSolved] : null;
    const showTask = selected !== null && !won;

    el.task.hidden = !showTask;
    el.overview.hidden = showTask;
    if (showTask) renderTask();

    if (last && !won && !showTask) {
      el.report.hidden = false;
      el.reportImg.hidden = !last.image;
      if (last.image) el.reportImg.src = last.image;
      el.reportTitle.textContent = "Досье: «" + last.vulnerability + "» обезврежена";
      el.reportText.textContent = last.fact || "";
    } else {
      el.report.hidden = true;
    }

    if (won) {
      el.missionLabel.textContent = "Миссия выполнена";
      el.missionTitle.textContent = "Сервер защищён";
      el.missionText.textContent = "Вирус " + C.virusName + " удалён. Данные спасены.";
      el.missionHint.hidden = true;
    } else if (allThreatsDone() && FINAL) {
      el.missionLabel.textContent = "Финальный этап";
      el.missionTitle.textContent = "Мастер-ключ";
      el.missionText.textContent = FINAL.text;
      el.missionHint.hidden = !FINAL.hint;
      el.missionHint.textContent = FINAL.hint ? "Подсказка: " + FINAL.hint : "";
    } else {
      const flashes = (C.matrix && C.matrix.flashesPerVisit) || 1;
      el.missionLabel.textContent = "Обезврежено " + state.done.length + " из " + COUNT;
      el.missionTitle.textContent = "Обезвредьте угрозы в Матрице";
      el.missionText.textContent = "Сканируйте QR-коды на фигурках — в любом порядке, не больше " + flashes +
        (flashes === 1 ? " фигурки" : " фигурок") + " за заход. Нажмите на угрозу слева, чтобы увидеть, что нужно сделать.";
      el.missionHint.hidden = true;
    }

    const showHowTo = !won && !(allThreatsDone() && FINAL) && C.howTo && C.howTo.length > 0;
    el.howto.hidden = !showHowTo;
    if (showHowTo && !el.howto.childElementCount) {
      C.howTo.forEach(function (line) { el.howto.appendChild(node("li", "", line)); });
    }

    el.form.hidden = won;
    renderSlots();
    renderInputLock();
    el.feedback.textContent = feedback.text;
    el.feedback.className = "feedback" + (feedback.kind ? " feedback--" + feedback.kind : "");
  }

  function renderInputLock() {
    const locked = isLocked();
    const disabled = state.status !== "running" || locked;
    el.input.disabled = disabled;
    el.submit.disabled = disabled;
    if (locked) {
      const sec = Math.ceil((state.lockUntil - Date.now()) / 1000);
      el.input.placeholder = "ввод заблокирован: " + sec + " сек";
    } else {
      el.input.placeholder = allThreatsDone() && FINAL ? "введите мастер-ключ" : "введите ключ";
    }
    if (lockWasActive && !locked && state.status === "running") {
      setFeedback("Блокировка снята. Можно вводить ключ.", "");
      el.input.focus();
    }
    lockWasActive = locked;
  }

  function renderLog() {
    el.log.innerHTML = "";
    state.log.slice(-30).forEach(function (entry) {
      const line = node("div", "log__line" + (entry.kind ? " log__line--" + entry.kind : ""));
      line.appendChild(node("span", "log__time", "[" + entry.at + "]"));
      line.appendChild(document.createTextNode(" " + entry.text));
      el.log.appendChild(line);
    });
    el.log.scrollTop = el.log.scrollHeight;
  }

  function infectionPct() {
    return Math.min(100, Math.max(0, Math.floor(used() / TOTAL_MS * 100)));
  }

  function renderOverlays() {
    el.intro.hidden = state.status !== "intro";
    el.paused.hidden = state.status !== "paused";
    el.lost.hidden = state.status !== "lost";
    el.win.hidden = state.status !== "won";
    renderIntro();

    if (state.status === "lost") {
      el.lostLead.textContent = "Вирус " + C.virusName + " успел зашифровать все данные. Обезврежено угроз: " +
        state.done.length + " из " + COUNT + ".";
    }

    if (state.status === "won") {
      const saved = Math.max(0, 100 - infectionPct());
      el.winLead.textContent = (state.team || TEAM) + " остановил " + C.virusName +
        ". Спасено " + saved + "% данных.";
      el.winStats.innerHTML = "";
      [
        ["Время операции", fmt(state.elapsedMs)],
        ["Неверных ключей", String(state.wrongTotal)],
        ["Подсказок", String(state.hints.length)],
        ["Спасено данных", saved + "%"]
      ].forEach(function (row) {
        const box = node("div", "stat");
        box.appendChild(node("div", "stat__value", row[1]));
        box.appendChild(node("div", "stat__label", row[0]));
        el.winStats.appendChild(box);
      });
      el.winKeys.innerHTML = "";
      if (FINAL) {
        el.winKeys.appendChild(node("span", "key-chip key-chip--final", FINAL.word || String(FINAL.answers[0]).toUpperCase()));
      }
    }
  }

  function renderClock() {
    const rem = remaining();
    el.clock.textContent = fmt(rem, true);
    el.clockBox.classList.toggle("clock--warn", state.status === "running" && rem < 5 * 60 * 1000);
    el.clockBox.classList.toggle("clock--secured", state.status === "won");

    const pct = infectionPct();
    el.pct.textContent = pct + "%";
    el.bar.style.width = pct + "%";

    const secured = state.status === "won";
    el.serverStatus.textContent = secured ? "Сервер защищён" : state.status === "lost" ? "Сервер потерян" : "Сервер заражён";
    el.serverStatus.className = "server-status" + (secured ? " server-status--ok" : "");

    el.teamName.textContent = state.team || TEAM;

    // Фон-«тетрадь»: доля зашифрованных клеток = процент шифрования.
    if (window.MatrixBg.setLevel) {
      const level = secured ? 0 : state.status === "intro" ? 0.12 : Math.max(0.03, pct / 100);
      window.MatrixBg.setLevel(level);
    }
  }

  function renderBackground() {
    window.MatrixBg.setColor("#00ff41");
  }

  function renderAll() {
    renderClock();
    renderStages();
    renderMission();
    renderLog();
    renderOverlays();
    renderBackground();
  }

  // ---------- таймер ----------

  function tick() {
    if (state.status === "running") {
      const rem = remaining();
      ALERT_MINUTES.forEach(function (m) {
        if (rem <= m * 60 * 1000 && rem > 0 && state.alerted.indexOf(m) === -1) {
          state.alerted.push(m);
          addLog("ВНИМАНИЕ: до полного шифрования осталось " + m + " мин!", "warn");
          save();
          renderLog();
          Sfx.alarm();
        }
      });
      if (rem <= 0) {
        lose();
        return;
      }
    }
    if (hintConfirmUntil && Date.now() > hintConfirmUntil) {
      hintConfirmUntil = 0;
      if (selected !== null) renderTask();
    }
    renderClock();
    renderInputLock();
    renderGate();
  }

  // ---------- связь с таймером Матрицы (только при запуске через сервер) ----------

  // Баннер «МАТРИЦА ЗАПУЩЕНА» на терминале — пока идёт заход (назначен, внутри или опоздание).
  function renderBanner() {
    const box = document.getElementById("matrix-banner");
    if (!box) return;
    let live = false;
    let warn = false;
    let sub = "";
    if (ONLINE && lastGate && lastGate.gate && lastGate.ageMs >= 0) {
      const age = lastGate.ageMs + (Date.now() - lastGateAt);
      const g = lastGate.gate;
      if (age < GATE_STALE_MS) {
        const names = (g.runners || []).join(", ");
        if (g.phase === "assigned") { live = true; sub = "Идут: " + names; }
        else if (g.phase === "inside") { live = true; sub = "Идут: " + names + " · осталось " + fmt(g.remainingMs - age, true); }
        else if (g.phase === "overtime") { live = true; warn = true; sub = "Опоздание! +" + fmt(g.overMs + age); }
      }
    }
    box.hidden = !live;
    box.className = "matrix-banner" + (warn ? " matrix-banner--warn" : "");
    document.getElementById("matrix-banner-sub").textContent = sub;
  }
  setInterval(renderBanner, 200);

  let lastGate = null;
  let lastGateAt = 0;
  let syncBusy = false;
  // Пока веб-таймер ни разу не выходил на связь (например, используется Arduino), плашку не показываем.
  let gateSeen = false;

  function renderGate() {
    if (!ONLINE || !gateSeen) return;
    el.gateMini.hidden = false;
    let phase = "offline";
    let text = "нет связи";
    if (lastGate && lastGate.gate && lastGate.ageMs >= 0) {
      const age = lastGate.ageMs + (Date.now() - lastGateAt);
      const g = lastGate.gate;
      if (age < GATE_STALE_MS) {
        phase = g.phase;
        if (g.phase === "ready") text = "открыта";
        else if (g.phase === "assigned") text = "идут: " + (g.runners || []).join(", ");
        else if (g.phase === "inside") text = "внутри " + fmt(g.remainingMs - age, true);
        else if (g.phase === "overtime") text = "опоздание +" + fmt(g.overMs + age);
        else if (g.phase === "cooldown") text = "закрыта " + fmt(g.remainingMs - age, true);
      }
    }
    el.gateMini.className = "gate-mini gate-mini--" + phase;
    el.gateMiniValue.textContent = text;
  }

  function onViolation(ev) {
    if (state.status !== "running" && state.status !== "paused") return;
    const sec = ev.overSeconds || 0;
    if (AUTO_PENALTY_MS > 0) {
      state.penaltyMs += AUTO_PENALTY_MS;
      addLog("Нарушение в Матрице: опоздание на " + sec + " сек. Штраф −" + (AUTO_PENALTY_MS / 1000) + " сек.", "bad");
      setFeedback("Нарушение в Матрице! Вирус ускорился на " + (AUTO_PENALTY_MS / 1000) + " сек.", "bad");
    } else {
      addLog("Нарушение в Матрице: опоздание на " + sec + " сек.", "warn");
    }
    Sfx.alarm();
  }

  // Что видят телефоны агентов: время, прогресс и буквы мастер-ключа. Ответов здесь нет.
  function publicState() {
    const won = state.status === "won";
    return {
      gameId: state.startedAt || 0,
      status: state.status,
      remainingMs: remaining(),
      pct: infectionPct(),
      total: COUNT,
      done: state.done.slice(),
      names: STAGES.map(function (s) { return s.vulnerability; }),
      letters: STAGES.map(function (s, i) { return isDone(i) || won ? (s.letter || "") : ""; }),
      finalWord: won && FINAL ? (FINAL.word || "") : "",
      title: C.title
    };
  }

  // Кто подключился к игре: имена агентов на экране подключения и капитан.
  function renderPlayers(run) {
    const players = (run && run.players) || [];
    el.teamsOnline.innerHTML = "";
    el.teamsOnline.appendChild(node("span", "team-chip", String(players.length)));
    el.teamsOnline.appendChild(node("span", "join-mini__cap", run && run.captain ? "капитан ✓" : "без капитана"));

    const sorted = players.slice().sort(function (a, b) { return a.name.localeCompare(b.name, "ru"); });
    document.querySelectorAll("[data-lobby-players]").forEach(function (list) {
      const key = JSON.stringify(sorted.map(function (p) { return [p.id, p.name, p.online]; }));
      if (list.getAttribute("data-key") === key) return;
      list.setAttribute("data-key", key);
      list.innerHTML = "";
      sorted.forEach(function (p) {
        list.appendChild(node("li", "lobby__player" + (p.online ? "" : " lobby__player--off"), p.name));
      });
    });
    document.querySelectorAll("[data-lobby-captain]").forEach(function (box) {
      box.textContent = run && run.captain ? (run.captainName || "Капитан") + " — на связи ✓" : "Ждём капитана…";
      box.className = "lobby__captain-state" + (run && run.captain ? " lobby__captain-state--on" : "");
    });
    el.lobbyCount.textContent = players.length ? "Агентов в игре: " + players.length : "Пока никто не подключился.";
  }

  function syncLan() {
    if (syncBusy) return;
    syncBusy = true;
    const stateReq = fetch("api/state", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(publicState())
    }).then(function () {
      return fetch("api/run", { cache: "no-store" });
    }).then(function (r) { return r.json(); })
      .then(function (data) { renderPlayers(data); })
      .catch(function () { /* старый сервер без api/state — телефоны агентов недоступны */ });

    const gateReq = fetch("api/gate", { cache: "no-store" }).then(function (r) { return r.json(); })
      .then(function (data) {
        lastGate = data;
        lastGateAt = Date.now();
        if (data && data.gate) gateSeen = true;
      })
      .catch(function () { lastGate = null; });

    const eventsReq = fetch("api/events?since=" + state.lastEventId, { cache: "no-store" })
      .then(function (r) { return r.json(); })
      .then(function (data) {
        if (data.boot !== state.eventsBoot) {
          // Сервер перезапущен: нумерация событий началась заново.
          state.eventsBoot = data.boot;
          state.lastEventId = 0;
          save();
          return;
        }
        let changed = false;
        data.events.forEach(function (ev) {
          if (ev.id <= state.lastEventId) return;
          state.lastEventId = ev.id;
          changed = true;
          if (ev.type === "violation") onViolation(ev);
        });
        if (changed) {
          save();
          renderAll();
        }
      })
      .catch(function () { /* сервер недоступен — попробуем в следующий раз */ });

    Promise.all([gateReq, eventsReq, stateReq]).then(function () {
      syncBusy = false;
      renderGate();
    });
  }

  // ---------- подключение телефонов по QR ----------

  function renderJoin(base) {
    const playUrl = base + "play.html";
    const wifi = C.wifi && C.wifi.ssid ? C.wifi : null;
    document.querySelectorAll("[data-join-qr]").forEach(function (box) {
      const captain = box.getAttribute("data-join-qr") === "captain";
      const url = captain ? base + "captain.html" : playUrl;
      box.innerHTML = "";
      const qr = node("div", "join-card__qr");
      qr.innerHTML = Join.svg(url);
      box.appendChild(qr);
      if (captain) return;
      box.appendChild(node("div", "join-card__note", url));
      if (wifi) {
        box.appendChild(node("div", "lobby__wifi", "Wi-Fi: " + wifi.ssid + (wifi.password ? ", пароль: " + wifi.password : "")));
      }
    });
    lobbyReady = true;
    el.joinMini.hidden = false;
    renderPlayers(null);
    renderIntro();
    if (state.status === "intro") el.lobbyNext.focus();
  }

  // Экран перед стартом: шаг «подключение по QR» или шаг «брифинг».
  function renderIntro() {
    const showLobby = state.status === "intro" && lobbyReady && introStep === "lobby";
    el.lobby.hidden = !showLobby;
    el.briefing.hidden = showLobby;
    el.lobbyNext.hidden = !showLobby;
    el.introStart.hidden = showLobby;
    el.introTitle.textContent = showLobby ? "Отдел ИБ, подключайтесь к игре" : "Сервер " + C.serverName + " атакован";
    updateScanlines();
  }

  function updateScanlines() {
    const qrVisible = !el.join.hidden || (!el.intro.hidden && !el.lobby.hidden);
    document.body.classList.toggle("no-scan", qrVisible);
  }

  function openJoin() {
    el.join.hidden = false;
    updateScanlines();
  }

  function closeJoin() {
    el.join.hidden = true;
    updateScanlines();
    if (state.status === "running") el.input.focus();
  }

  // ---------- панель организатора ----------

  function openAdmin() {
    el.admin.hidden = false;
    el.adminControls.hidden = true;
    el.adminLogin.hidden = false;
    el.adminAnswer.textContent = "";
    el.adminPin.value = "";
    el.adminPin.focus();
  }

  function closeAdmin() {
    el.admin.hidden = true;
    if (state.status === "running") el.input.focus();
  }

  function renderAdminInfo() {
    el.adminInfo.textContent =
      "Статус: " + state.status + " · обезврежено " + state.done.length + "/" + COUNT +
      " · осталось " + fmt(remaining(), true) +
      " · штрафы/бонусы: " + Math.round(state.penaltyMs / 1000) + " сек · ошибок: " + state.wrongTotal;
  }

  function adjustTime(deltaMs, label) {
    state.penaltyMs += deltaMs;
    addLog(label, deltaMs > 0 ? "bad" : "good");
    if (state.status === "lost" && remaining() > 0) {
      state.status = "running";
      runClock();
      addLog("Оператор подключил резервный канал. Шифрование продолжается!", "warn");
    }
  }

  function selectedStage() {
    return parseInt(el.adminStage.value, 10);
  }

  function adminAction(action) {
    switch (action) {
      case "close":
        closeAdmin();
        return;
      case "pause":
        if (state.status === "running") {
          freezeClock();
          state.status = "paused";
          addLog("Оператор поставил игру на паузу.", "warn");
        } else if (state.status === "paused") {
          state.status = "running";
          runClock();
          addLog("Игра продолжена.", "warn");
        }
        break;
      case "plus1":
        adjustTime(-60000, "Оператор выделил команде +1 мин.");
        break;
      case "plus5":
        adjustTime(-300000, "Оператор выделил команде +5 мин.");
        break;
      case "minus1":
        adjustTime(60000, "Штраф от оператора: −1 мин.");
        break;
      case "minus2":
        adjustTime(120000, "Подсказка от оператора: −2 мин.");
        break;
      case "answer": {
        const i = selectedStage();
        const target = i === FINAL_INDEX ? FINAL : STAGES[i];
        const name = i === FINAL_INDEX ? "Мастер-ключ" : STAGES[i].vulnerability;
        el.adminAnswer.textContent = "Ответ «" + name + "»: " + target.answers.join(" / ");
        return;
      }
      case "solve": {
        if (state.status !== "running" && state.status !== "paused") return;
        const i = selectedStage();
        if (i === FINAL_INDEX) {
          if (!allThreatsDone()) {
            el.adminAnswer.textContent = "Сначала должны быть обезврежены все угрозы.";
            return;
          }
          win();
          setFeedback("", "");
        } else {
          solveThreat(i, true);
        }
        break;
      }
      case "unsolve": {
        const i = selectedStage();
        if (i === FINAL_INDEX) {
          if (state.status === "won") {
            state.status = "running";
            runClock();
            addLog("Оператор отменил ввод мастер-ключа.", "warn");
          }
        } else if (isDone(i)) {
          state.done = state.done.filter(function (x) { return x !== i; });
          if (state.lastSolved === i) state.lastSolved = null;
          if (state.status === "won") {
            state.status = "running";
            runClock();
          }
          addLog("Оператор вернул угрозу «" + STAGES[i].vulnerability + "».", "warn");
        }
        break;
      }
      case "unlock":
        state.lockUntil = 0;
        state.wrongStreak = 0;
        break;
      case "matrix-open":
        fetch("api/run/open", { method: "POST" }).catch(function () {});
        addLog("Оператор открыл Матрицу досрочно.", "warn");
        break;
      case "captain-reset":
        if (!window.confirm("Сменить капитана? Новый капитан отсканирует QR капитана на экране «QR подключения».")) return;
        fetch("api/captain/reset", { method: "POST" }).catch(function () {});
        addLog("Оператор освободил место капитана.", "warn");
        break;
      case "reset":
        if (!window.confirm("Начать новую игру? Весь прогресс текущей команды будет удалён.")) return;
        // Список агентов и счётчики Матрицы тоже начинаются заново.
        if (ONLINE) fetch("api/players/reset", { method: "POST" }).catch(function () {});
        state = freshState();
        selected = null;
        introStep = "lobby";
        setFeedback("", "");
        save();
        renderAll();
        closeAdmin();
        el.introStart.focus();
        return;
      default:
        return;
    }
    save();
    renderAll();
    renderAdminInfo();
  }

  // ---------- события ----------

  el.form.addEventListener("submit", onSubmit);

  el.introForm.addEventListener("submit", function (e) {
    e.preventDefault();
    Sfx.unlock();
    start();
  });

  el.stages.addEventListener("click", function (e) {
    const li = e.target.closest("[data-index]");
    if (li) selectThreat(parseInt(li.getAttribute("data-index"), 10));
  });

  el.taskHintBtn.addEventListener("click", onHintClick);
  el.joinOpen.addEventListener("click", openJoin);
  el.lobbyNext.addEventListener("click", function () {
    introStep = "briefing";
    renderIntro();
    el.introStart.focus();
  });
  el.joinClose.addEventListener("click", closeJoin);
  el.join.addEventListener("click", function (e) { if (e.target === el.join) closeJoin(); });
  el.taskBack.addEventListener("click", function () { selectThreat(null); });

  el.adminLogin.addEventListener("submit", function (e) {
    e.preventDefault();
    if (el.adminPin.value === String(C.adminPin)) {
      el.adminLogin.hidden = true;
      el.adminControls.hidden = false;
      renderAdminInfo();
    } else {
      el.adminPin.value = "";
      Sfx.error();
    }
  });

  el.admin.addEventListener("click", function (e) {
    const btn = e.target.closest("[data-admin]");
    if (btn) adminAction(btn.getAttribute("data-admin"));
  });

  document.addEventListener("keydown", function (e) {
    if (e.ctrlKey && e.altKey && e.code === "KeyA") {
      e.preventDefault();
      openAdmin();
    } else if (e.key === "Escape" && !el.admin.hidden) {
      closeAdmin();
    } else if (e.key === "Escape" && !el.join.hidden) {
      closeJoin();
    }
  });

  let brandClicks = [];
  el.brandTitle.addEventListener("click", function () {
    const now = Date.now();
    brandClicks = brandClicks.filter(function (t) { return now - t < 2000; });
    brandClicks.push(now);
    if (brandClicks.length >= 5) {
      brandClicks = [];
      openAdmin();
    }
  });

  window.addEventListener("storage", function (e) {
    if (e.key === STORAGE_KEY) {
      state = load();
      renderAll();
    }
  });

  // ---------- запуск ----------

  renderStatic();
  if (state.status === "running" && remaining() <= 0) {
    lose();
  } else {
    renderAll();
  }
  if (state.status === "intro") el.introStart.focus();
  setInterval(tick, 250);
  if (ONLINE) {
    Join.lanBase().then(function (base) { if (base) renderJoin(base); });
    syncLan();
    setInterval(syncLan, 1000);
  }
})();
