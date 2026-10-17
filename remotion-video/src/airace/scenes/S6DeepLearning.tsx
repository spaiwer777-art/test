import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { Count, Draw } from "../../carstory/kit";
import { KCaption } from "../kinetic";
import { A, body, E, mono, prog } from "../theme";
import { BOARD, CatMosaic } from "./S5Return";

const C = A.green;
export const TOKENS = ["ИИ", "учится", "понимать", "смысл", "наших", "слов"];
export const TOKEN_Y = 1250;
export const tokenX = (i: number) => {
  const widths = TOKENS.map((t) => 44 + t.length * 21);
  const gap = 14;
  const total = widths.reduce((a, b) => a + b, 0) + gap * (TOKENS.length - 1);
  let x = 540 - total / 2;
  for (let k = 0; k < i; k++) x += widths[k] + gap;
  return { x, w: widths[i] };
};

const FEATURE_PALETTES = [
  ["#0c1a12", A.green, "#ffffff"],
  ["#000000", "#1e6b45", "#b6ffd6"],
  ["#39ff88", "#0c1a12", "#0c1a12"],
  ["#06110b", "#7dffb5", "#39ff88"],
  ["#14281c", "#0c1a12", "#ffffff"],
  ["#000000", "#39ff88", "#000000"],
];

// Go: 13×13 board, eight stones then the score
const GO = { x: 180, y: 640, size: 720, n: 13 };
const STONES: [number, number][] = [
  [3, 3], [9, 9], [9, 3], [3, 9], [6, 6], [4, 7], [8, 5], [6, 9],
];

export const S6DeepLearning: React.FC = () => {
  const f = useCurrentFrame();

  const fan = prog(f, 12, 30, E.expoInOut);
  const alexOut = prog(f, 108, 14, E.expoIn);
  const chartIn = f >= 112 && f < 212;
  const chartP = prog(f, 126, 46, E.expoInOut);
  const goIn = prog(f, 206, 12);
  const toTokens = prog(f, 270, 24, E.expoInOut);

  // compute curve (log scale: straight-ish, steepening)
  const pts = new Array(41).fill(0).map((_, i) => {
    const u = i / 40;
    return [150 + u * 780, 1460 - (u * 0.75 + u * u * 0.25) * 640] as const;
  });
  const d = pts.map(([x, y], i) => `${i ? "L" : "M"}${x.toFixed(1)},${y.toFixed(1)}`).join(" ");
  const tip = pts[Math.round(chartP * 40)];

  return (
    <AbsoluteFill style={{ overflow: "hidden" }}>
      <KCaption mode="flip" year="2012" title="AlexNet — прорыв в зрении" start={4} out={108} accent={C} />
      <KCaption mode="slice" year="2012 → 2024" title="Вычисления растут взрывом" start={116} out={200} accent={C} />
      <KCaption mode="elastic" year="2016" title="AlphaGo — Ли Седоль" start={210} out={268} accent={C} />

      {/* AlexNet: the image splits into feature maps fanning into depth */}
      <AbsoluteFill style={{ perspective: 1600, opacity: 1 - alexOut }}>
        <AbsoluteFill style={{ transformStyle: "preserve-3d", transform: `rotateY(${-42 * fan + Math.sin(f / 25) * 4}deg) rotateX(${8 * fan}deg) translateX(${-fan * 140}px)` }}>
          {FEATURE_PALETTES.map((pal, i) => (
            <AbsoluteFill
              key={i}
              style={{
                transform: `translateZ(${-i * 210 * fan}px) translateX(${i * 80 * fan}px) rotateZ(${i * 2 * fan}deg)`,
                opacity: i === 0 ? 1 : fan,
                filter: i === 0 ? "none" : `drop-shadow(0 0 20px ${C}55)`,
              }}
            >
              <CatMosaic palette={i === 0 ? ["#0c1a12", A.green, "#ffffff"] : pal} />
            </AbsoluteFill>
          ))}
        </AbsoluteFill>
        <div style={{ position: "absolute", top: BOARD.y + BOARD.size + 110, width: 1080, textAlign: "center" }}>
          <div style={{ fontFamily: mono, fontSize: 30, color: A.grey, letterSpacing: 4 }}>ImageNet · ошибка top-5</div>
          <div style={{ fontFamily: mono, fontWeight: 800, fontSize: 110, color: C, textShadow: `0 0 30px ${C}` }}>
            <Count from={26.2} to={15.3} start={40} dur={40} decimals={1} suffix="%" />
          </div>
          <div style={{ fontFamily: body, fontSize: 32, color: A.grey }}>лучший результат до AlexNet — 26,2%</div>
        </div>
      </AbsoluteFill>

      {/* Training compute, log scale */}
      {chartIn ? (
        <AbsoluteFill style={{ transform: `rotate(${-8 * chartP}deg) scale(${1 + chartP * 0.05})`, opacity: 1 - prog(f, 198, 10) }}>
          <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
            <Draw d="M150,1460 L940,1460" start={114} dur={10} stroke={A.grey} width={3} />
            <Draw d="M150,1460 L150,740" start={114} dur={10} stroke={A.grey} width={3} />
            {["×1", "×10³", "×10⁶", "×10⁹"].map((t, i) => (
              <text key={t} x={130} y={1460 - i * 220 + 10} textAnchor="end" fill={A.grey} fontFamily={mono} fontSize={26} opacity={prog(f, 118 + i * 2, 8)}>
                {t}
              </text>
            ))}
            {["2012", "2016", "2020", "2024"].map((t, i) => (
              <text key={t} x={150 + i * 260} y={1510} textAnchor="middle" fill={A.grey} fontFamily={mono} fontSize={26} opacity={prog(f, 120, 8)}>
                {t}
              </text>
            ))}
            <Draw d={d} start={126} dur={46} stroke={C} width={9} />
            <circle cx={tip[0]} cy={tip[1]} r={20} fill={C} opacity={chartP > 0 ? 1 : 0} style={{ filter: `drop-shadow(0 0 16px ${C})` }} />
          </svg>
          <div style={{ position: "absolute", top: 1570, width: 1080, textAlign: "center", fontFamily: mono, fontWeight: 700, fontSize: 52, color: C, opacity: prog(f, 160, 10) }}>
            ≈ ×4–5 каждый год
          </div>
        </AbsoluteFill>
      ) : null}

      {/* Go board */}
      {goIn > 0 ? (
        <AbsoluteFill style={{ opacity: goIn }}>
          <div style={{ position: "absolute", left: GO.x, top: GO.y, width: GO.size, height: GO.size, background: "#c79a4f", borderRadius: 10, opacity: 1 - toTokens, boxShadow: `0 0 60px ${C}33` }}>
            <svg width={GO.size} height={GO.size}>
              {new Array(GO.n).fill(0).map((_, i) => {
                const p = 30 + (i * (GO.size - 60)) / (GO.n - 1);
                return (
                  <g key={i}>
                    <line x1={30} x2={GO.size - 30} y1={p} y2={p} stroke="#3b2a10" strokeWidth={2} />
                    <line y1={30} y2={GO.size - 30} x1={p} x2={p} stroke="#3b2a10" strokeWidth={2} />
                  </g>
                );
              })}
            </svg>
          </div>
          {STONES.map(([c, r], i) => {
            const at = 212 + i * 6;
            const drop = prog(f, at, 8, E.backOut);
            const step = (GO.size - 60) / (GO.n - 1);
            const sx = GO.x + 30 + c * step;
            const sy = GO.y + 30 + r * step;
            const target = i < TOKENS.length ? tokenX(i) : null;
            const tx = target ? target.x + target.w / 2 : sx;
            const x = interpolate(toTokens, [0, 1], [sx, tx]);
            const y = interpolate(toTokens, [0, 1], [sy, TOKEN_Y]);
            const w = interpolate(toTokens, [0, 1], [50, target ? target.w : 50]);
            const black = i % 2 === 0;
            return (
              <div
                key={i}
                style={{
                  position: "absolute",
                  left: x - w / 2,
                  top: y - 25,
                  width: w,
                  height: 50 + toTokens * 22,
                  marginTop: -toTokens * 11,
                  borderRadius: interpolate(toTokens, [0, 1], [25, 14]),
                  background: toTokens > 0.5 ? "#141414" : black ? "#141414" : "#f4f1e8",
                  border: toTokens > 0.5 ? `3px solid ${A.warm}` : "none",
                  transform: `scale(${interpolate(drop, [0, 1], [1.8, 1])})`,
                  opacity: Math.min(1, drop * 2) * (target ? 1 : 1 - toTokens),
                  boxShadow: "0 8px 18px rgba(0,0,0,0.5)",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontFamily: mono,
                  fontSize: 34,
                  color: A.warm,
                }}
              >
                {target && toTokens > 0.6 ? TOKENS[i] : ""}
              </div>
            );
          })}
          <div style={{ position: "absolute", top: GO.y + GO.size + 40, width: 1080, textAlign: "center", fontFamily: mono, fontWeight: 800, fontSize: 200, color: A.text, textShadow: `0 0 40px ${C}`, transform: `scale(${interpolate(prog(f, 240, 10, E.backOut), [0, 1], [2.2, 1])})`, opacity: prog(f, 240, 6) * (1 - toTokens) }}>
            4 : 1
          </div>
        </AbsoluteFill>
      ) : null}
    </AbsoluteFill>
  );
};
