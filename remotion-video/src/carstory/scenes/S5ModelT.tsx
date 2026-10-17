import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { CARS } from "../cars";
import { C, body, condensed, Count, display, Draw, E, MaskLine, mix, prog } from "../kit";

export const S5_DURATION = 180;

const CHART = { x: 130, y: 1000, w: 840, h: 400 };
const PRICES: [number, number][] = [
  [1908, 850],
  [1913, 550],
  [1916, 360],
  [1925, 260],
];
const px = (year: number) => CHART.x + ((year - 1908) / (1925 - 1908)) * CHART.w;
const py = (usd: number) => CHART.y + CHART.h - (usd / 1000) * CHART.h;
export const PRICE_END = { x: px(1925), y: py(260) };

const LINE_START = 64;
const LINE_DUR = 40;

// Position + value at progress t along the price polyline.
const along = (t: number) => {
  const pts = PRICES.map(([y, v]) => ({ x: px(y), y: py(v), v }));
  const lens = pts.slice(1).map((p, i) => Math.hypot(p.x - pts[i].x, p.y - pts[i].y));
  const total = lens.reduce((a, b) => a + b, 0);
  let d = t * total;
  for (let i = 0; i < lens.length; i++) {
    if (d <= lens[i] || i === lens.length - 1) {
      const u = Math.min(1, d / lens[i]);
      return { x: mix(pts[i].x, pts[i + 1].x, u), y: mix(pts[i].y, pts[i + 1].y, u), v: mix(pts[i].v, pts[i + 1].v, u) };
    }
    d -= lens[i];
  }
  return { x: pts[0].x, y: pts[0].y, v: pts[0].v };
};

export const S5ModelT: React.FC = () => {
  const frame = useCurrentFrame();
  const car = CARS[0];

  const drive = prog(frame, 8, 26, E.expoOut);
  const squash = Math.sin(prog(frame, 8, 30, E.quintOut) * Math.PI) * 0.12;
  const carX = mix(-1000, 90, drive);
  const lineP = prog(frame, LINE_START, LINE_DUR, E.expoInOut);
  const tip = along(lineP);
  const marquee = -(frame * 6) % 1300;

  return (
    <AbsoluteFill style={{ backgroundColor: C.ink, overflow: "hidden" }}>
      {/* Outlined marquee */}
      <div
        style={{
          position: "absolute",
          top: 40,
          left: marquee,
          whiteSpace: "nowrap",
          fontFamily: display,
          fontWeight: 900,
          fontSize: 400,
          color: "transparent",
          WebkitTextStroke: `2px ${C.paper}`,
          opacity: 0.18,
        }}
      >
        1908 1908 1908 1908
      </div>

      <div style={{ position: "absolute", top: 210, left: 90 }}>
        <MaskLine start={4}>
          <div style={{ fontFamily: display, fontWeight: 900, fontSize: 132, color: C.paper }}>FORD</div>
        </MaskLine>
        <MaskLine start={9}>
          <div style={{ fontFamily: display, fontWeight: 900, fontSize: 132, color: C.red }}>MODEL T</div>
        </MaskLine>
      </div>

      {/* Car slides in with speed lines */}
      <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
        {new Array(7).fill(0).map((_, i) => {
          const len = (1 - drive) * 700 + 60;
          const y = 600 + i * 34;
          return (
            <line
              key={i}
              x1={carX + 80 - len - (i % 3) * 60}
              x2={carX + 80 - (i % 3) * 60}
              y1={y}
              y2={y}
              stroke={C.paper}
              strokeWidth={4}
              opacity={0.25 + (1 - drive) * 0.6}
            />
          );
        })}
        <g transform={`translate(${carX} 470) scale(${1.5 * (1 + squash)} ${1.5 * (1 - squash)})`}>
          <path d={car.body} fill={C.paper} />
          <path d={car.glass} fill={C.ink} opacity={0.9} />
          {car.wheels.map((w, i) => (
            <g key={i} transform={`translate(${w.x} ${200 - w.r}) rotate(${frame * 12})`}>
              <circle r={w.r} fill={C.ink} stroke={C.paper} strokeWidth={6} />
              {[0, 60, 120].map((a) => (
                <line key={a} x1={-w.r * 0.7} x2={w.r * 0.7} y1={0} y2={0} stroke={C.paper} strokeWidth={4} transform={`rotate(${a})`} />
              ))}
            </g>
          ))}
        </g>
      </svg>

      {/* Assembly-line chips */}
      <div style={{ position: "absolute", top: 820, left: 90, display: "flex", gap: 20 }}>
        {["КОНВЕЙЕР · 1913", "93 МИН НА МАШИНУ"].map((t, i) => {
          const p = prog(frame, 34 + i * 6, 16, E.backOut);
          return (
            <div
              key={t}
              style={{
                fontFamily: condensed,
                fontWeight: 600,
                fontSize: 40,
                color: i === 0 ? C.ink : C.paper,
                background: i === 0 ? C.yellow : "transparent",
                border: `3px solid ${C.yellow}`,
                padding: "8px 22px",
                transform: `scale(${p})`,
              }}
            >
              {t}
            </div>
          );
        })}
      </div>

      {/* Price chart */}
      <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
        {[0, 250, 500, 750, 1000].map((v, i) => (
          <g key={v} opacity={prog(frame, 48 + i * 2, 12)}>
            <line x1={CHART.x} x2={CHART.x + CHART.w} y1={py(v)} y2={py(v)} stroke={C.paper} strokeOpacity={0.12} strokeWidth={2} />
            <text x={CHART.x - 18} y={py(v) + 10} textAnchor="end" fill={C.grey} fontFamily={body} fontSize={26}>
              {v}
            </text>
          </g>
        ))}
        <Draw d={`M${CHART.x},${CHART.y + CHART.h} L${CHART.x + CHART.w},${CHART.y + CHART.h}`} start={46} dur={16} stroke={C.paper} width={3} />
        {PRICES.map(([y]) => (
          <text key={y} x={px(y)} y={CHART.y + CHART.h + 48} textAnchor="middle" fill={C.grey} fontFamily={body} fontSize={28} opacity={prog(frame, 52, 12)}>
            {y}
          </text>
        ))}
        <Draw
          d={PRICES.map(([y, v], i) => `${i === 0 ? "M" : "L"}${px(y)},${py(v)}`).join(" ")}
          start={LINE_START}
          dur={LINE_DUR}
          stroke={C.yellow}
          width={9}
        />
        {PRICES.map(([y, v], i) => {
          const at = LINE_START + (i / (PRICES.length - 1)) * LINE_DUR * 0.9;
          return <circle key={y} cx={px(y)} cy={py(v)} r={14 * prog(frame, at, 10, E.backOut)} fill={C.ink} stroke={C.yellow} strokeWidth={6} />;
        })}
      </svg>
      <div
        style={{
          position: "absolute",
          left: tip.x - 90,
          top: tip.y - 100,
          width: 180,
          textAlign: "center",
          fontFamily: display,
          fontWeight: 800,
          fontSize: 50,
          color: C.ink,
          background: C.yellow,
          padding: "6px 0",
          opacity: prog(frame, LINE_START, 6),
        }}
      >
        ${Math.round(tip.v)}
      </div>
      <div style={{ position: "absolute", top: 930, left: CHART.x }}>
        <MaskLine start={50}>
          <div style={{ fontFamily: body, fontWeight: 700, fontSize: 30, letterSpacing: 6, color: C.grey }}>ЦЕНА MODEL T, $</div>
        </MaskLine>
      </div>

      {/* Totals */}
      <div style={{ position: "absolute", top: 1540, width: 1080, textAlign: "center" }}>
        <MaskLine start={104}>
          <div style={{ fontFamily: display, fontWeight: 900, fontSize: 104, color: C.paper }}>
            <Count to={15000000} start={104} dur={40} />
          </div>
        </MaskLine>
        <MaskLine start={110}>
          <div style={{ fontFamily: condensed, fontWeight: 600, fontSize: 48, color: C.red, letterSpacing: 4 }}>
            МАШИН ВЫПУЩЕНО · ЦЕНА УПАЛА В 3 РАЗА
          </div>
        </MaskLine>
      </div>
    </AbsoluteFill>
  );
};
