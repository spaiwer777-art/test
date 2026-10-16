window.Sfx = (function () {
  "use strict";

  let ctx = null;

  function unlock() {
    if (!ctx) {
      const AC = window.AudioContext || window.webkitAudioContext;
      if (!AC) return null;
      ctx = new AC();
    }
    if (ctx.state === "suspended") ctx.resume();
    return ctx;
  }

  function tone(freq, dur, opts) {
    const o = opts || {};
    const a = unlock();
    if (!a) return;
    const t = a.currentTime + (o.delay || 0);
    const osc = a.createOscillator();
    const gain = a.createGain();
    osc.type = o.type || "sine";
    osc.frequency.setValueAtTime(freq, t);
    gain.gain.setValueAtTime(0.0001, t);
    gain.gain.exponentialRampToValueAtTime(o.vol || 0.15, t + 0.01);
    gain.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    osc.connect(gain).connect(a.destination);
    osc.start(t);
    osc.stop(t + dur + 0.05);
  }

  return {
    unlock: unlock,
    tone: tone,
    ok: function () {
      tone(660, 0.12);
      tone(880, 0.12, { delay: 0.12 });
      tone(1320, 0.3, { delay: 0.24 });
    },
    error: function () {
      tone(180, 0.25, { type: "sawtooth", vol: 0.12 });
      tone(130, 0.4, { type: "sawtooth", vol: 0.12, delay: 0.22 });
    },
    tick: function () {
      tone(1000, 0.06, { type: "square", vol: 0.08 });
    },
    alarm: function () {
      tone(880, 0.22, { type: "square", vol: 0.18 });
      tone(620, 0.22, { type: "square", vol: 0.18, delay: 0.25 });
    },
    start: function () {
      tone(440, 0.1, { type: "triangle" });
      tone(660, 0.1, { type: "triangle", delay: 0.1 });
      tone(990, 0.2, { type: "triangle", delay: 0.2 });
    },
    win: function () {
      [523, 659, 784, 1047, 784, 1047].forEach(function (f, i) {
        tone(f, 0.25, { type: "triangle", vol: 0.18, delay: i * 0.15 });
      });
    }
  };
})();
