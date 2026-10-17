import React from "react";
import { AbsoluteFill, interpolate, random, useCurrentFrame } from "remotion";
import { Letters } from "../../carstory/kit";
import { A, body, display, E, mono, prog } from "../theme";

const C = A.warm;
const PARTS = new Array(320).fill(0).map((_, i) => ({
  a: random(`pa${i}`) * Math.PI * 2,
  d: 200 + random(`pd${i}`) * 900,
  s: 3 + random(`ps${i}`) * 9,
  c: i % 5 === 0 ? A.red : i % 3 === 0 ? A.green : C,
}));
const FLAG_COLS = 18;
const FLAG_ROWS = 9;
const SQ = 52;

export const S10Finish: React.FC = () => {
  const f = useCurrentFrame();
  const burst = prog(f, 0, 40, E.expoOut);
  const flagIn = prog(f, 56, 14);
  const end = prog(f, 120, 14);
  const logo = prog(f, 126, 14);
  const textOut = prog(f, 118, 8, E.expoIn);

  return (
    <AbsoluteFill style={{ overflow: "hidden" }}>
      {/* the point explodes */}
      <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
        {PARTS.map((p, i) => (
          <rect
            key={i}
            x={540 + Math.cos(p.a) * p.d * burst - p.s / 2}
            y={960 + Math.sin(p.a) * p.d * burst - p.s / 2}
            width={p.s}
            height={p.s}
            fill={p.c}
            opacity={(1 - burst) * 0.9 + 0.1 * (1 - end)}
          />
        ))}
      </svg>

      <div style={{ position: "absolute", top: 520, width: 1080, display: "flex", flexDirection: "column", alignItems: "center", gap: 8, opacity: 1 - textOut }}>
        <Letters text="КТО ПЕРВЫМ" start={8} stagger={1.5} mode="scatter" style={{ fontFamily: display, fontWeight: 900, fontSize: 100, color: A.text }} />
        <Letters text="ПЕРЕСЕЧЁТ" start={18} stagger={1.5} mode="scatter" style={{ fontFamily: display, fontWeight: 900, fontSize: 100, color: A.text }} />
        <Letters text="ФИНИШ?" start={28} stagger={2} mode="scatter" style={{ fontFamily: display, fontWeight: 900, fontSize: 130, color: A.red }} />
      </div>

      {/* pixel chequered flag waving */}
      <div style={{ position: "absolute", left: (1080 - FLAG_COLS * SQ) / 2, top: 1180, opacity: flagIn * (1 - textOut) }}>
        {new Array(FLAG_ROWS).fill(0).map((_, r) =>
          new Array(FLAG_COLS).fill(0).map((__, c) => {
            const wave = Math.sin(c * 0.55 - f * 0.35) * 22;
            const shade = 0.75 + 0.25 * Math.sin(c * 0.55 - f * 0.35 + 1.2);
            const black = (r + c) % 2 === 0;
            return (
              <div
                key={`${r}-${c}`}
                style={{
                  position: "absolute",
                  left: c * SQ,
                  top: r * SQ + wave,
                  width: SQ,
                  height: SQ,
                  background: black ? "#0b0b0e" : `rgba(245,241,232,${shade})`,
                  transform: `scale(${interpolate(prog(f, 56 + c * 0.8, 10, E.backOut), [0, 1], [0, 1])})`,
                }}
              />
            );
          }),
        )}
      </div>

      {/* hit, fade, sign-off */}
      <AbsoluteFill style={{ background: "#000", opacity: end * 0.92 }} />
      <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", opacity: logo }}>
        <div style={{ fontFamily: display, fontWeight: 900, fontSize: 96, color: A.text, textAlign: "center", lineHeight: 1.05, transform: `scale(${interpolate(logo, [0, 1], [1.15, 1])})` }}>
          ГОНКА
          <br />
          <span style={{ color: A.red }}>НЕЙРОСЕТЕЙ</span>
        </div>
        <div style={{ marginTop: 40, fontFamily: mono, fontSize: 30, letterSpacing: 8, color: A.grey }}>1943 — ????</div>
      </AbsoluteFill>
      <div style={{ position: "absolute", bottom: 120, width: 1080, textAlign: "center", fontFamily: body, fontSize: 22, lineHeight: 1.5, color: "#8a8fa0", opacity: logo }}>
        Музыка: Kevin MacLeod — «Exit the Premises» (incompetech.com), CC BY 4.0
        <br />
        Звуки: Kenney (kenney.nl), CC0
      </div>
    </AbsoluteFill>
  );
};
