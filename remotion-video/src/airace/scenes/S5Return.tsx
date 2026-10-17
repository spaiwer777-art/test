import React from "react";
import { AbsoluteFill, interpolate, random, useCurrentFrame } from "remotion";
import { Caption } from "../rigs";
import { A, body, E, mono, prog } from "../theme";

const C = A.violet;
const LAYERS = [3, 5, 5, 2];
const LX = [170, 410, 670, 910];
const NY = (count: number, i: number) => 960 + (i - (count - 1) / 2) * 150;
const EDGES: { l: number; a: number; b: number }[] = [];
for (let l = 0; l < LAYERS.length - 1; l++) {
  for (let a = 0; a < LAYERS[l]; a++) {
    for (let b = 0; b < LAYERS[l + 1]; b++) EDGES.push({ l, a, b });
  }
}
// [start, duration, direction] — forward pulses violet, error flowing back red; the second cycle is faster.
const PASSES: [number, number, 1 | -1][] = [
  [8, 30, 1],
  [40, 24, -1],
  [70, 20, 1],
  [92, 16, -1],
];

// Cat mosaic that the chess board turns into (and scene 6 starts from).
export const CAT = [
  "................",
  "..1..........1..",
  "..11........11..",
  "..111......111..",
  "..111111111111..",
  ".11111111111111.",
  ".11122111122111.",
  ".11122111122111.",
  ".11111111111111.",
  ".11111122111111.",
  "..111111111111..",
  "..11.111111.11..",
  "...1111111111...",
  "....11111111....",
  ".....111111.....",
  "................",
];
export const BOARD = { x: 220, y: 760, size: 640 };

export const CatMosaic: React.FC<{ opacity?: number; palette?: string[] }> = ({ opacity = 1, palette = ["#0c1a12", A.green, "#ffffff"] }) => (
  <div style={{ position: "absolute", left: BOARD.x, top: BOARD.y, width: BOARD.size, height: BOARD.size, display: "grid", gridTemplateColumns: "repeat(16, 1fr)", opacity }}>
    {CAT.join("").split("").map((ch, i) => (
      <div key={i} style={{ background: ch === "." ? palette[0] : ch === "1" ? palette[1] : palette[2], outline: "1px solid rgba(0,0,0,0.25)" }} />
    ))}
  </div>
);

export const S5Return: React.FC = () => {
  const f = useCurrentFrame();
  const netOut = prog(f, 112, 12, E.expoIn);
  const boardIn = prog(f, 120, 20);
  const flatten = prog(f, 210, 24, E.expoInOut);
  const tilt = interpolate(boardIn, [0, 1], [80, 52]) * (1 - flatten);
  const spin = 45 * (1 - flatten);
  const queen = prog(f, 165, 12, E.expoInOut);
  const kingFall = prog(f, 180, 12);
  const mosaic = prog(f, 222, 12);

  // passes & per-edge weights (re-rolled after every backward pass)
  const passIdx = PASSES.filter(([s]) => f >= s).length;
  const weight = (i: number) => 1.5 + random(`w${i}-${Math.floor(passIdx / 2)}`) * 5;

  return (
    <AbsoluteFill style={{ overflow: "hidden" }}>
      <Caption year="1986" title="Обратное распространение ошибки" start={4} out={112} accent={C} />
      <Caption year="1997" title="Deep Blue — Каспаров" start={124} accent={C} />

      {/* Neural network training */}
      <AbsoluteFill style={{ opacity: 1 - netOut, transform: `scale(${1 - netOut * 0.2})` }}>
        <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
          {EDGES.map((e, i) => {
            const x1 = LX[e.l];
            const y1 = NY(LAYERS[e.l], e.a);
            const x2 = LX[e.l + 1];
            const y2 = NY(LAYERS[e.l + 1], e.b);
            let glow = 0;
            let col = C;
            for (const [s, d, dir] of PASSES) {
              const u = (f - s) / d; // 0..1 across the whole network
              if (u < 0 || u > 1) continue;
              const pos = dir === 1 ? u * 3 : 3 - u * 3;
              const dist = Math.abs(pos - (e.l + 0.5));
              if (dist < 0.6) {
                glow = Math.max(glow, 1 - dist / 0.6);
                col = dir === 1 ? C : A.red;
              }
            }
            return (
              <line
                key={i}
                x1={x1}
                y1={y1}
                x2={x2}
                y2={y2}
                stroke={glow > 0 ? col : "#3a2d63"}
                strokeWidth={weight(i) * (1 + glow)}
                opacity={(0.35 + glow * 0.65) * prog(f, 2 + e.l * 4, 10)}
              />
            );
          })}
          {LAYERS.map((n, l) =>
            new Array(n).fill(0).map((_, i) => (
              <circle key={`${l}-${i}`} cx={LX[l]} cy={NY(n, i)} r={30 * prog(f, l * 4, 10, E.backOut)} fill="#120c24" stroke={C} strokeWidth={5} />
            )),
          )}
        </svg>
        <div style={{ position: "absolute", top: 1420, width: 1080, display: "flex", justifyContent: "center", gap: 40, fontFamily: mono, fontSize: 34 }}>
          <span style={{ color: C }}>→ прямой проход</span>
          <span style={{ color: A.red }}>← ошибка назад</span>
        </div>
      </AbsoluteFill>

      {/* Chess: queen takes the king */}
      {f >= 118 ? (
        <AbsoluteFill style={{ perspective: 1400 }}>
          <div
            style={{
              position: "absolute",
              left: BOARD.x,
              top: BOARD.y,
              width: BOARD.size,
              height: BOARD.size,
              transformStyle: "preserve-3d",
              transform: `rotateX(${tilt}deg) rotateZ(${spin}deg) scale(${interpolate(boardIn, [0, 1], [0.6, 1])})`,
              opacity: boardIn,
            }}
          >
            <div style={{ position: "absolute", inset: 0, display: "grid", gridTemplateColumns: "repeat(8, 1fr)", opacity: 1 - mosaic }}>
              {new Array(64).fill(0).map((_, i) => (
                <div key={i} style={{ background: (Math.floor(i / 8) + i) % 2 ? "#2a1d4d" : "#c9b8ff" }} />
              ))}
            </div>
            {/* queen slides along the diagonal */}
            <div style={{ position: "absolute", left: (1 + queen * 4) * 80, top: (6 - queen * 4) * 80, width: 80, height: 80, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: "'DejaVu Sans', sans-serif", fontSize: 70, color: "#ffffff", textShadow: `0 0 20px ${C}`, opacity: 1 - mosaic }}>
              ♛
            </div>
            <div style={{ position: "absolute", left: 5 * 80, top: 2 * 80, width: 80, height: 80, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: "'DejaVu Sans', sans-serif", fontSize: 70, color: "#111", transform: `rotate(${kingFall * 100}deg) translate(${kingFall * 60}px, ${kingFall * 30}px)`, opacity: (1 - kingFall * 0.8) * (1 - mosaic) }}>
              ♚
            </div>
          </div>
          <CatMosaic opacity={mosaic} />
        </AbsoluteFill>
      ) : null}
      <div style={{ position: "absolute", top: 1500, width: 1080, textAlign: "center", fontFamily: body, fontSize: 38, color: A.grey, opacity: prog(f, 184, 12) * (1 - prog(f, 214, 10)) }}>
        машина впервые обыгрывает чемпиона мира
      </div>
    </AbsoluteFill>
  );
};
