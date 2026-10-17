import React from "react";
import { AbsoluteFill, Freeze, random, useCurrentFrame } from "remotion";
import { Draw, Letters } from "../../carstory/kit";
import { A, body, display, E, mono, prog } from "../theme";

const C = A.ice;
export const S4_FREEZE = 140;
const CRACK = "M-20,880 L160,930 L250,860 L380,990 L470,940 L560,1060 L690,1000 L760,1120 L880,1060 L1100,1150";

const FLAKES = new Array(140).fill(0).map((_, i) => ({
  x: random(`fx${i}`) * 1080,
  y: random(`fy${i}`) * 1920,
  s: 4 + Math.floor(random(`fs${i}`) * 3) * 4,
  v: 1.5 + (i % 3) * 1.6,
}));

export const S4Content: React.FC = () => {
  const f = useCurrentFrame();
  const frost = prog(f, 0, 90, E.quintOut);
  const pit = f >= 80 && Math.floor(f / 8) % 2 === 0;

  return (
    <AbsoluteFill style={{ overflow: "hidden", background: "#0a0d12" }}>
      <AbsoluteFill style={{ background: `radial-gradient(ellipse at 50% 40%, ${C}22, transparent 65%)` }} />
      {/* frost creeping in from the corners */}
      {[
        ["0%", "0%"],
        ["100%", "0%"],
        ["0%", "100%"],
        ["100%", "100%"],
      ].map(([x, y], i) => (
        <AbsoluteFill key={i} style={{ background: `radial-gradient(circle at ${x} ${y}, rgba(220,235,250,${0.55 * frost}) 0%, rgba(200,220,240,${0.2 * frost}) ${18 + frost * 22}%, transparent ${30 + frost * 30}%)` }} />
      ))}
      {/* pixel snow, three parallax layers */}
      {FLAKES.map((p, i) => (
        <div key={i} style={{ position: "absolute", left: p.x + Math.sin(f / 20 + i) * 10, top: (p.y + f * p.v) % 1960 - 20, width: p.s, height: p.s, background: "#e6f0fa", opacity: 0.35 + (i % 3) * 0.2 }} />
      ))}

      <div style={{ position: "absolute", top: 240, width: 1080, display: "flex", flexDirection: "column", alignItems: "center" }}>
        <div style={{ fontFamily: mono, fontSize: 38, letterSpacing: 6, color: C, opacity: prog(f, 4, 12) }}>1970-е · 1980-е</div>
        <Letters text="ЗИМА ИИ" start={6} stagger={4} mode="drop" style={{ fontFamily: display, fontWeight: 900, fontSize: 150, color: "#dce8f4", marginTop: 10 }} />
      </div>

      {/* funding chart: up, then the crash */}
      <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
        <Draw d="M120,1560 L960,1560" start={40} dur={10} stroke={C} width={3} />
        <Draw d="M120,1560 L120,1000" start={40} dur={10} stroke={C} width={3} />
        <Draw d="M140,1500 L320,1330 L480,1180 L640,1100 L700,1090" start={50} dur={30} stroke="#dce8f4" width={8} ease={E.quintOut} />
        <Draw d="M700,1090 L760,1500 L940,1530" start={84} dur={10} stroke={A.red} width={8} ease={E.expoIn} />
      </svg>
      <div style={{ position: "absolute", left: 140, top: 950, fontFamily: mono, fontSize: 30, letterSpacing: 4, color: C, opacity: prog(f, 44, 10) }}>ФИНАНСИРОВАНИЕ</div>

      {/* pit stop board */}
      {f >= 76 ? (
        <div style={{ position: "absolute", right: 110, top: 640, padding: "18px 34px", border: `5px solid ${pit ? A.red : "#5c6470"}`, color: pit ? A.red : "#8a93a0", fontFamily: mono, fontWeight: 800, fontSize: 56, letterSpacing: 6, background: "#0b0e13" }}>
          PIT STOP
        </div>
      ) : null}
      <div style={{ position: "absolute", top: 1640, width: 1080, textAlign: "center", fontFamily: body, fontSize: 36, color: "#8fa0b2", opacity: prog(f, 60, 12) }}>
        обещания не сбылись — деньги и интерес ушли
      </div>

      {/* crack across the ice */}
      <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
        <Draw d={CRACK} start={128} dur={7} stroke="#ffffff" width={5} ease={E.expoOut} />
      </svg>
    </AbsoluteFill>
  );
};

// Shards: a jittered 3×4 grid of quads cut from the frozen frame.
const COLS = 3;
const ROWS = 4;
const pts: [number, number][][] = [];
for (let r = 0; r <= ROWS; r++) {
  const row: [number, number][] = [];
  for (let c = 0; c <= COLS; c++) {
    const edgeX = c === 0 || c === COLS;
    const edgeY = r === 0 || r === ROWS;
    row.push([
      (c / COLS) * 1080 + (edgeX ? 0 : (random(`px${r}${c}`) - 0.5) * 220),
      (r / ROWS) * 1920 + (edgeY ? 0 : (random(`py${r}${c}`) - 0.5) * 300),
    ]);
  }
  pts.push(row);
}
const SHARDS = [] as { poly: [number, number][]; cx: number; cy: number }[];
for (let r = 0; r < ROWS; r++) {
  for (let c = 0; c < COLS; c++) {
    const poly = [pts[r][c], pts[r][c + 1], pts[r + 1][c + 1], pts[r + 1][c]];
    SHARDS.push({ poly, cx: poly.reduce((s, p) => s + p[0], 0) / 4, cy: poly.reduce((s, p) => s + p[1], 0) / 4 });
  }
}

export const S4Shatter: React.FC = () => {
  // local frame 0 == global 750
  const f = useCurrentFrame();
  return (
    <AbsoluteFill>
      {SHARDS.map((s, i) => {
        const p = prog(f, random(`sd${i}`) * 3, 20, E.expoIn);
        const dx = s.cx - 540;
        const dy = s.cy - 960;
        const len = Math.hypot(dx, dy) || 1;
        const rot = (random(`sr${i}`) - 0.5) * 120 * p;
        return (
          <AbsoluteFill
            key={i}
            style={{
              clipPath: `polygon(${s.poly.map(([x, y]) => `${x}px ${y}px`).join(",")})`,
              transformOrigin: `${s.cx}px ${s.cy}px`,
              transform: `translate(${(dx / len) * 1500 * p}px, ${(dy / len) * 1500 * p + p * p * 600}px) rotate(${rot}deg) scale(${1 - p * 0.3})`,
              opacity: 1 - p * 0.6,
            }}
          >
            <Freeze frame={S4_FREEZE}>
              <S4Content />
            </Freeze>
            <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
              <polygon points={s.poly.map(([x, y]) => `${x},${y}`).join(" ")} fill="rgba(220,235,250,0.08)" stroke="#ffffff" strokeWidth={6} strokeOpacity={0.8} />
            </svg>
          </AbsoluteFill>
        );
      })}
    </AbsoluteFill>
  );
};
