import React from "react";
import { AbsoluteFill, random, useCurrentFrame } from "remotion";
import { Caption } from "../rigs";
import { A, body, E, mono, prog } from "../theme";

const C = A.blue;
const LETTER_A = ["..XXXX..", ".XX..XX.", "XX....XX", "XX....XX", "XXXXXXXX", "XX....XX", "XX....XX", "XX....XX"];
const CELL = 100;
const GX = 540 - 4 * CELL;
const GY = 560;

const CHAT: { who: "user" | "eliza"; text: string }[] = [
  { who: "user", text: "> I feel sad." },
  { who: "eliza", text: "ELIZA: Why do you feel sad?" },
  { who: "user", text: "> Because of my work." },
  { who: "eliza", text: "ELIZA: Tell me more about" },
  { who: "eliza", text: "       your work." },
];
const CHAT_START = 104;
const GLITCH = 180;

export const S3FirstLaps: React.FC = () => {
  const f = useCurrentFrame();
  const percOut = prog(f, 86, 16, E.expoInOut);
  const termIn = prog(f, 90, 18);
  const g = prog(f, GLITCH, 30, E.expoIn);
  const grey = prog(f, GLITCH + 8, 18);

  // typed chat: one character per frame, lines in sequence
  let budget = Math.max(0, f - CHAT_START);
  const lines = CHAT.map((l) => {
    const shown = l.text.slice(0, budget);
    budget = Math.max(0, budget - l.text.length - 4);
    return { ...l, shown };
  });

  const content = (
    <AbsoluteFill>
      <Caption year="1958" title="Перцептрон" start={4} out={86} accent={C} />
      <Caption year="1966" title="ELIZA — первый чат-бот" start={92} accent={C} />

      {/* Perceptron: lamps learn the letter A */}
      <AbsoluteFill style={{ transform: `translateY(${-percOut * 500}px) scale(${1 - percOut * 0.4})`, opacity: 1 - percOut }}>
        {LETTER_A.map((row, r) =>
          row.split("").map((ch, c) => {
            const wave = prog(f, 6 + (r + c) * 1.4, 6);
            const settled = prog(f, 40 + (r + c) * 0.8, 10);
            const on = ch === "X";
            const level = on ? Math.max(wave, settled) : wave * (1 - settled) * 0.9 + 0.08;
            return (
              <div
                key={`${r}-${c}`}
                style={{
                  position: "absolute",
                  left: GX + c * CELL + 14,
                  top: GY + r * CELL + 14,
                  width: CELL - 28,
                  height: CELL - 28,
                  borderRadius: 40,
                  background: `rgba(61,123,255,${0.1 + level * 0.9})`,
                  boxShadow: level > 0.5 ? `0 0 ${30 * level}px ${C}` : "none",
                }}
              />
            );
          }),
        )}
        <div style={{ position: "absolute", left: 140, right: 140, top: GY + 8 * CELL + 70 }}>
          <div style={{ display: "flex", justifyContent: "space-between", fontFamily: mono, fontSize: 32, color: A.grey }}>
            <span>обучение</span>
            <span style={{ color: C }}>{Math.round(prog(f, 16, 50, E.quintOut) * 100)}%</span>
          </div>
          <div style={{ height: 10, background: "#16203f", marginTop: 12 }}>
            <div style={{ height: 10, width: `${prog(f, 16, 50, E.quintOut) * 100}%`, background: C, boxShadow: `0 0 20px ${C}` }} />
          </div>
        </div>
      </AbsoluteFill>

      {/* ELIZA terminal */}
      <div
        style={{
          position: "absolute",
          left: 70,
          right: 70,
          top: 560,
          height: 760,
          background: "#050a07",
          border: "3px solid #1f3b2a",
          borderRadius: 26,
          boxShadow: "0 0 80px rgba(57,255,136,0.12)",
          transform: `translateY(${(1 - termIn) * 900}px)`,
          opacity: termIn,
          overflow: "hidden",
        }}
      >
        <div style={{ height: 70, display: "flex", alignItems: "center", gap: 14, paddingLeft: 28, borderBottom: "2px solid #1f3b2a" }}>
          {["#ff5f57", "#febc2e", "#28c840"].map((c) => (
            <div key={c} style={{ width: 22, height: 22, borderRadius: 11, background: c }} />
          ))}
          <div style={{ marginLeft: 20, fontFamily: mono, fontSize: 28, color: "#5c8f6e" }}>ELIZA · MIT · 1966</div>
        </div>
        <div style={{ padding: "40px 40px", fontFamily: mono, fontSize: 40, lineHeight: 1.7 }}>
          {lines.map((l, i) => (
            <div key={i} style={{ color: l.who === "user" ? "#d8ffe6" : "#39ff88", whiteSpace: "pre", minHeight: 68, textShadow: l.who === "eliza" ? "0 0 14px #39ff8866" : "none" }}>
              {l.shown}
            </div>
          ))}
          <span style={{ color: "#39ff88", opacity: Math.floor(f / 10) % 2 ? 1 : 0, fontSize: 40 }}>▍</span>
        </div>
      </div>
      <div style={{ position: "absolute", top: 1380, width: 1080, textAlign: "center", fontFamily: body, fontSize: 36, color: A.grey, opacity: prog(f, 150, 12) }}>
        отвечает вопросом на вопрос — и кажется, что понимает
      </div>
    </AbsoluteFill>
  );

  if (g <= 0) return <AbsoluteFill style={{ overflow: "hidden" }}>{content}</AbsoluteFill>;

  // Glitch, then freeze into grey
  const slices = 8;
  return (
    <AbsoluteFill style={{ overflow: "hidden", filter: `grayscale(${grey}) brightness(${1 - grey * 0.35})` }}>
      {new Array(slices).fill(0).map((_, i) => {
        const dx = (random(`s3g${Math.floor(f / 2)}-${i}`) - 0.5) * 160 * (1 - grey) * Math.min(1, g * 4);
        return (
          <AbsoluteFill key={i} style={{ clipPath: `inset(${(i * 1920) / slices}px 0 ${1920 - ((i + 1) * 1920) / slices}px 0)`, transform: `translateX(${dx}px)` }}>
            {content}
          </AbsoluteFill>
        );
      })}
      <AbsoluteFill style={{ background: "rgba(255,0,60,0.18)", mixBlendMode: "screen", transform: `translateX(${(1 - grey) * 24}px)`, opacity: 1 - grey }} />
    </AbsoluteFill>
  );
};
