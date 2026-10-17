import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { KText } from "../kinetic";
import { A, body, display, E, mono, prog } from "../theme";
import { TOKEN_Y, TOKENS, tokenX } from "./S6DeepLearning";

const C = A.warm;
// attention arcs: [from, to, weight]
const ARCS: [number, number, number][] = [
  [0, 1, 0.6], [1, 2, 0.9], [0, 3, 0.7], [2, 3, 1], [3, 5, 0.8], [4, 5, 0.9], [1, 5, 0.5], [0, 2, 0.4], [2, 5, 0.6], [3, 4, 0.5],
];
const ARC_START = 58;
const ALL_ON = 120; // global 1410

// Simple F1 car silhouette, side view, nose to the right
const CAR = "M0,70 L40,40 L170,34 L210,10 L260,10 L280,34 L420,40 L470,58 L470,76 L0,76 Z";

export const S7Transformer: React.FC = () => {
  const f = useCurrentFrame();
  const land = prog(f, 4, 26, E.expoOut);
  const paperUp = prog(f, 48, 16, E.expoInOut);
  const all = prog(f, ALL_ON, 8);
  const stretch = prog(f, 148, 14, E.expoIn);
  const car = prog(f, 156, 8);
  const go = prog(f, 162, 16, E.expoIn);

  const centre = (i: number) => {
    const t = tokenX(i);
    return t.x + t.w / 2;
  };

  return (
    <AbsoluteFill style={{ overflow: "hidden" }}>
      {/* The paper */}
      <AbsoluteFill style={{ perspective: 1400 }}>
        <div
          style={{
            position: "absolute",
            left: 160,
            top: 300,
            width: 760,
            height: 820,
            background: "#f6f3ea",
            borderRadius: 6,
            boxShadow: "0 40px 90px rgba(0,0,0,0.6)",
            transformOrigin: "50% 0%",
            transform: `translateY(${interpolate(land, [0, 1], [-700, 0]) - paperUp * 200}px) rotateX(${interpolate(land, [0, 1], [-70, 0])}deg) scale(${1 - paperUp * 0.55})`,
            padding: "70px 60px",
            opacity: 1 - stretch,
          }}
        >
          <div style={{ fontFamily: body, fontWeight: 800, fontSize: 60, color: "#111", lineHeight: 1.15 }}>Attention Is All You Need</div>
          <div style={{ fontFamily: body, fontSize: 28, color: "#555", marginTop: 26 }}>Vaswani et al. · Google · 2017</div>
          {new Array(9).fill(0).map((_, i) => (
            <div key={i} style={{ height: 14, marginTop: 26, background: "#d9d4c8", width: `${90 - (i % 3) * 14}%` }} />
          ))}
          <div style={{ position: "absolute", right: 60, bottom: 60, fontFamily: mono, fontSize: 26, color: "#888" }}>arXiv:1706.03762</div>
        </div>
      </AbsoluteFill>

      <div style={{ position: "absolute", top: 1440, width: 1080, display: "flex", justifyContent: "center", opacity: 1 - stretch }}>
        <KText text="ТРАНСФОРМЕР" start={60} mode="elastic" stagger={1.6} font={display} style={{ fontWeight: 900, fontSize: 78, color: C, letterSpacing: -2 }} />
      </div>

      {/* Attention arcs over the token row */}
      <svg width={1080} height={1920} style={{ position: "absolute", inset: 0, opacity: 1 - stretch }}>
        {ARCS.map(([a, b, w], i) => {
          const x1 = centre(a);
          const x2 = centre(b);
          const h = 90 + Math.abs(x2 - x1) * 0.55;
          const p = prog(f, ARC_START + i * 6, 10, E.expoOut);
          const glow = Math.max(all, i * 6 + ARC_START <= f && f < ARC_START + i * 6 + 12 ? 1 : 0.4);
          return (
            <path
              key={i}
              d={`M${x1},${TOKEN_Y - 40} Q${(x1 + x2) / 2},${TOKEN_Y - 40 - h} ${x2},${TOKEN_Y - 40}`}
              pathLength={1}
              strokeDasharray="1 1"
              strokeDashoffset={1 - p}
              fill="none"
              stroke={i % 3 === 0 ? A.red : C}
              strokeWidth={(2 + w * 7) * (1 + all * 0.6)}
              strokeLinecap="round"
              opacity={glow}
              style={{ filter: all > 0 ? `drop-shadow(0 0 ${10 + all * 14}px ${C})` : "none" }}
            />
          );
        })}
      </svg>
      {TOKENS.map((t, i) => {
        const { x, w } = tokenX(i);
        return (
          <div
            key={t}
            style={{
              position: "absolute",
              left: x,
              top: TOKEN_Y - 36,
              width: w,
              height: 72,
              borderRadius: 14,
              background: "#141414",
              border: `3px solid ${C}`,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontFamily: mono,
              fontSize: 34,
              color: C,
              boxShadow: all > 0 ? `0 0 ${30 * all}px ${C}aa` : "0 8px 18px rgba(0,0,0,0.5)",
              transform: `scaleX(${1 + stretch * 3}) scaleY(${1 - stretch * 0.9})`,
              opacity: 1 - car,
            }}
          >
            {t}
          </div>
        );
      })}
      <div style={{ position: "absolute", top: 1560, width: 1080, textAlign: "center", fontFamily: body, fontSize: 38, color: A.grey, opacity: prog(f, 70, 12) * (1 - stretch) }}>
        каждое слово «смотрит» на все остальные
      </div>

      {/* Speed lines + the car shooting off */}
      {stretch > 0 ? (
        <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
          {new Array(14).fill(0).map((_, i) => {
            const y = 1090 + i * 22;
            const len = 300 + ((i * 137) % 500);
            const x = 1080 - (stretch + go) * 1400 - ((i * 91) % 300);
            return <line key={i} x1={x} x2={x + len} y1={y} y2={y} stroke={i % 3 === 0 ? A.red : C} strokeWidth={4} opacity={0.7} />;
          })}
          <g transform={`translate(${interpolate(go, [0, 1], [305, 1500])} ${1150}) scale(${1 + go * 0.3} 1)`} opacity={car} style={{ filter: `blur(${go * 10}px)` }}>
            <path d={CAR} fill={C} />
            <circle cx={90} cy={78} r={34} fill="#141414" stroke={C} strokeWidth={6} />
            <circle cx={390} cy={78} r={34} fill="#141414" stroke={C} strokeWidth={6} />
            <rect x={440} y={50} width={60} height={12} fill={A.red} />
          </g>
        </svg>
      ) : null}
    </AbsoluteFill>
  );
};
