import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { Count } from "../../carstory/kit";
import { KCaption } from "../kinetic";
import { A, body, display, E, mono, prog } from "../theme";

const C = A.warm;
const BASE = 1640;
// Bar heights keep the real ratio 0.117 : 1.5 : 175 — GPT-3 punches far through the top of the frame.
const PX = [6, 77, 9000];
const BARS = [
  { name: "GPT-1", year: "2018", label: [117, " млн"], start: 8 },
  { name: "GPT-2", year: "2019", label: [1.5, " млрд"], start: 24 },
  { name: "GPT-3", year: "2020", label: [175, " млрд"], start: 44 },
] as const;

const FIELD = { x: 90, y: 560, w: 900, h: 130 };
const CURSOR_X = FIELD.x + 52;
const CURSOR_Y = FIELD.y + FIELD.h / 2;

const RACE = [
  { name: "ChatGPT", months: 2, label: "≈ 2 мес." },
  { name: "TikTok", months: 9, label: "≈ 9 мес." },
  { name: "Instagram", months: 30, label: "≈ 2,5 года" },
];
const RACE_START = 196;
const RACE_DUR = 22; // frames for ChatGPT to reach 100 M

export const S8Scale: React.FC = () => {
  const f = useCurrentFrame();
  const camUp = prog(f, 90, 26, E.expoIn);
  const card1 = prog(f, 118, 10) * (1 - prog(f, 142, 8, E.expoIn));
  const card2 = prog(f, 144, 10) * (1 - prog(f, 166, 8, E.expoIn));
  const chatIn = prog(f, 170, 12);
  const zoom = prog(f, 242, 26, E.expoIn);
  const k = interpolate(zoom, [0, 1], [1, 40]);

  return (
    <AbsoluteFill style={{ overflow: "hidden" }}>
      {/* Parameter count bars; the camera chases GPT-3 upward */}
      {f < 120 ? (
        <AbsoluteFill style={{ transform: `translateY(${camUp * 1700}px)`, filter: `blur(${camUp * 10}px)` }}>
          <KCaption mode="slice" year="2018 → 2020" title="Параметры моделей" start={2} accent={C} />
          {BARS.map((b, i) => {
            const grow = prog(f, b.start, b.name === "GPT-3" ? 50 : 14, b.name === "GPT-3" ? E.expoIn : E.backOut);
            const h = Math.max(4, PX[i] * grow);
            const x = 150 + i * 300;
            const col = i === 2 ? A.red : C;
            const D = 90;
            return (
              <React.Fragment key={b.name}>
                <div style={{ position: "absolute", left: x, top: BASE - h, width: 180, height: h, transformStyle: "preserve-3d", transform: `perspective(1600px) rotateY(${-28 + Math.sin(f / 30) * 4}deg) rotateX(-8deg)`, transformOrigin: "50% 100%", opacity: prog(f, b.start, 4) }}>
                  <div style={{ position: "absolute", inset: 0, background: col, boxShadow: `0 0 40px ${col}66` }} />
                  <div style={{ position: "absolute", top: 0, left: 180, width: D, height: h, background: col, filter: "brightness(0.55)", transformOrigin: "0 0", transform: "rotateY(90deg)" }} />
                  <div style={{ position: "absolute", top: 0, left: 0, width: 180, height: D, background: col, filter: "brightness(1.25)", transformOrigin: "0 0", transform: "rotateX(90deg)" }} />
                </div>
                <div style={{ position: "absolute", left: x - 40, width: 260, top: BASE + 24, textAlign: "center", fontFamily: mono, fontWeight: 700, fontSize: 40, color: A.text, opacity: prog(f, b.start, 6) }}>
                  {b.name}
                  <div style={{ fontSize: 26, color: A.grey }}>{b.year}</div>
                </div>
                <div style={{ position: "absolute", left: x - 40, width: 260, top: Math.max(400, BASE - h - 80), textAlign: "center", fontFamily: mono, fontWeight: 800, fontSize: 46, color: i === 2 ? A.red : C, opacity: prog(f, b.start + 4, 6) }}>
                  <Count to={b.label[0]} start={b.start} dur={b.name === "GPT-3" ? 50 : 14} decimals={b.label[0] % 1 ? 1 : 0} suffix={b.label[1]} />
                </div>
              </React.Fragment>
            );
          })}
        </AbsoluteFill>
      ) : null}

      {/* Quick cards: generated images, AlphaFold */}
      <div style={{ position: "absolute", left: 90, top: 560, width: 900, height: 900, borderRadius: 30, overflow: "hidden", transform: `translateX(${(1 - card1) * -1200}px) rotate(${(1 - card1) * -6}deg)`, opacity: card1 > 0 ? 1 : 0, background: "conic-gradient(from 210deg at 40% 55%, #ff5f6d, #ffc371, #47cacc, #6a3093, #ff5f6d)" }}>
        <div style={{ position: "absolute", left: 120, top: 180, width: 420, height: 420, borderRadius: "50%", background: "radial-gradient(circle at 35% 35%, #fff6, #0000 60%)" }} />
        <div style={{ position: "absolute", bottom: 0, left: 0, right: 0, padding: 40, background: "linear-gradient(transparent, #000c)", fontFamily: display, fontWeight: 900, fontSize: 60, color: "#fff" }}>
          Картинки по тексту
          <div style={{ fontFamily: mono, fontSize: 30, fontWeight: 600, marginTop: 8 }}>2022</div>
        </div>
      </div>
      <div style={{ position: "absolute", left: 90, top: 560, width: 900, height: 900, borderRadius: 30, overflow: "hidden", transform: `translateX(${(1 - card2) * 1200}px) rotate(${(1 - card2) * 6}deg)`, opacity: card2 > 0 ? 1 : 0, background: "#0b0d14", border: `3px solid ${C}55` }}>
        <svg width={900} height={900}>
          {[0, 1].map((s) => (
            <path
              key={s}
              d={new Array(60).fill(0).map((_, i) => `${i ? "L" : "M"}${450 + Math.sin(i * 0.42 + f * 0.15 + s * Math.PI) * 170 * Math.cos(i * 0.05)},${120 + i * 10}`).join(" ")}
              fill="none"
              stroke={s ? A.red : "#5ec8ff"}
              strokeWidth={22}
              strokeLinecap="round"
              opacity={0.9}
            />
          ))}
        </svg>
        <div style={{ position: "absolute", bottom: 0, left: 0, right: 0, padding: 40, background: "linear-gradient(transparent, #000c)", fontFamily: display, fontWeight: 900, fontSize: 60, color: "#fff" }}>
          AlphaFold
          <div style={{ fontFamily: body, fontSize: 32, fontWeight: 500, marginTop: 8 }}>предсказывает форму белков</div>
        </div>
      </div>

      {/* ChatGPT: the input field, then the race to 100 M users */}
      {f >= 168 ? (
        <AbsoluteFill style={{ transformOrigin: `${CURSOR_X}px ${CURSOR_Y}px`, transform: `translate(${zoom * (540 - CURSOR_X)}px, ${zoom * (960 - CURSOR_Y)}px) scale(${k})`, opacity: chatIn }}>
          <div style={{ position: "absolute", left: FIELD.x, top: 380, fontFamily: mono, fontSize: 34, color: A.grey, letterSpacing: 4 }}>30.11.2022</div>
          <div style={{ position: "absolute", left: FIELD.x, top: 430, fontFamily: display, fontWeight: 900, fontSize: 80, color: C }}>ChatGPT</div>
          <div style={{ position: "absolute", left: FIELD.x, top: FIELD.y, width: FIELD.w, height: FIELD.h, borderRadius: 65, border: `3px solid ${C}88`, background: "#111218", display: "flex", alignItems: "center", paddingLeft: 48 }}>
            <div style={{ width: 6, height: 60, background: C, opacity: Math.floor(f / 8) % 2 || zoom > 0 ? 1 : 0 }} />
            <div style={{ marginLeft: 18, fontFamily: body, fontSize: 40, color: "#6b6f7d", opacity: 1 - zoom * 4 }}>Спросите что-нибудь…</div>
          </div>
          <div style={{ position: "absolute", left: FIELD.x, top: 820, fontFamily: mono, fontSize: 34, color: A.grey, letterSpacing: 3, opacity: prog(f, RACE_START - 6, 8) }}>
            ДО 100 МЛН ПОЛЬЗОВАТЕЛЕЙ
          </div>
          {RACE.map((r, i) => {
            const t = Math.max(0, f - RACE_START);
            const p = Math.min(1, (t / RACE_DUR) * (2 / r.months));
            const done = p >= 1;
            return (
              <div key={r.name} style={{ position: "absolute", left: FIELD.x, top: 890 + i * 170, width: FIELD.w, opacity: prog(f, RACE_START - 6 + i * 3, 8) }}>
                <div style={{ display: "flex", justifyContent: "space-between", fontFamily: mono, fontSize: 38, color: i === 0 ? C : A.grey }}>
                  <span style={{ fontWeight: 700 }}>{r.name}</span>
                  <span>{r.label}</span>
                </div>
                <div style={{ height: 34, background: "#1a1b22", marginTop: 14, borderRadius: 6, overflow: "hidden" }}>
                  <div style={{ height: 34, width: `${p * 100}%`, background: i === 0 ? A.red : "#555a68", boxShadow: done ? `0 0 30px ${A.red}` : "none" }} />
                </div>
              </div>
            );
          })}
          <div style={{ position: "absolute", left: FIELD.x, top: 1430, fontFamily: body, fontSize: 32, color: A.grey, opacity: prog(f, 226, 10) }}>
            по широко цитируемым оценкам
          </div>
        </AbsoluteFill>
      ) : null}

      {/* The cursor fills the screen and becomes the start line of the final race */}
      {zoom > 0.75 ? <AbsoluteFill style={{ background: C, opacity: prog(f, 262, 6) }} /> : null}
    </AbsoluteFill>
  );
};
