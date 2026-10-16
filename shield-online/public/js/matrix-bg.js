// Фон: цифровой дождь «Матрицы».
// setColor(c) — цвет дождя (таймер красит его по фазам).
// setLevel(0..1) — доля колонок, захваченных вирусом: они идут красным.
// Терминал передаёт сюда процент шифрования, поэтому дождь краснеет по ходу игры.
window.MatrixBg = (function () {
  "use strict";

  const canvas = document.getElementById("matrix-bg");
  if (!canvas) return { setColor: function () {}, setLevel: function () {} };

  const ctx = canvas.getContext("2d");
  const SIZE = 18;
  const GLYPHS = "ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝ0123456789ЖЩЫЮЯ:=*+<>";
  const VIRUS = "#ff2a3d";
  const reduced = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  let color = "#00ff41";
  let level = 0;
  let width = 0;
  let height = 0;
  let drops = [];

  function glyph() {
    return GLYPHS[Math.floor(Math.random() * GLYPHS.length)];
  }

  function newDrop(i, scatter) {
    return {
      x: i * SIZE,
      y: scatter ? Math.random() * height * 1.2 - height * 0.4 : -SIZE * (1 + Math.random() * 20),
      speed: 0.35 + Math.random() * 0.75,
      // порог: колонка становится «вирусной», когда level его превысит
      r: Math.random(),
      last: ""
    };
  }

  function resize() {
    const dpr = Math.min(2, window.devicePixelRatio || 1);
    width = window.innerWidth;
    height = window.innerHeight;
    canvas.width = Math.round(width * dpr);
    canvas.height = Math.round(height * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.fillStyle = "#000400";
    ctx.fillRect(0, 0, width, height);
    const cols = Math.ceil(width / SIZE);
    drops = [];
    for (let i = 0; i < cols; i++) drops.push(newDrop(i, true));
  }

  function step() {
    // Полупрозрачная заливка оставляет за каплями затухающий хвост.
    ctx.fillStyle = "rgba(0, 4, 0, 0.09)";
    ctx.fillRect(0, 0, width, height);
    ctx.font = "600 " + (SIZE - 2) + "px 'Martian Mono', Consolas, monospace";
    ctx.textAlign = "center";
    ctx.textBaseline = "top";

    drops.forEach(function (d, i) {
      const prevY = d.y;
      d.y += d.speed * SIZE * 0.5;
      const row = Math.floor(d.y / SIZE);
      if (row === Math.floor(prevY / SIZE)) return;
      const infected = d.r < level;
      const c = infected ? VIRUS : color;
      const x = d.x + SIZE / 2;
      const y = row * SIZE;
      // предыдущую «голову» перекрашиваем в цвет колонки
      if (d.last) {
        ctx.fillStyle = "#000400";
        ctx.fillRect(d.x, y - SIZE, SIZE, SIZE);
        ctx.fillStyle = c;
        ctx.fillText(d.last, x, y - SIZE);
      }
      d.last = glyph();
      ctx.fillStyle = infected ? "#ffd6da" : "#e6ffec";
      ctx.fillText(d.last, x, y);
      if (y > height && Math.random() > 0.96) drops[i] = newDrop(i, false);
    });
  }

  let last = 0;
  function frame(t) {
    requestAnimationFrame(frame);
    if (t - last < 50) return;
    last = t;
    step();
  }

  window.addEventListener("resize", resize);
  resize();
  // Сразу «прокручиваем» дождь, чтобы экран не начинался пустым.
  for (let k = 0; k < 60; k++) step();
  if (reduced) {
    // Без анимации: один «застывший» кадр дождя.
    for (let k = 0; k < 120; k++) step();
  } else {
    requestAnimationFrame(frame);
  }

  return {
    setColor: function (c) { color = c; },
    setLevel: function (v) { level = Math.max(0, Math.min(1, v)); }
  };
})();
