import React, { useMemo } from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { C, body, condensed, display, Draw, E, Letters, MaskLine, mix, prog, useShake } from "../kit";
import { PRICE_END } from "./S5ModelT";

export const S6_DURATION = 165;
export const IRIS = 18;

const CH = { x: 120, y: 640, w: 860, h: 680 };
// Approximate number of cars in the world, billions.
const DATA: [number, number][] = [
  [1900, 0],
  [1920, 0.01],
  [1950, 0.07],
  [1970, 0.25],
  [1990, 0.58],
  [2010, 1.0],
  [2025, 1.5],
];
const X = (y: number) => CH.x + ((y - 1900) / 125) * CH.w;
const Y = (v: number) => CH.y + CH.h - (v / 1.6) * CH.h;
const LINE_START = 26;
const LINE_DUR = 70;
const MILESTONES = [
  { year: 1950, label: "70 МЛН" },
  { year: 1990, label: "580 МЛН" },
  { year: 2010, label: "1 МЛРД" },
];

const fmt = (v: number) => (v < 0.995 ? `${Math.round(v * 1000)} млн` : `${v.toFixed(1).replace(".", ",")} млрд`);

export const S6Fleet: React.FC = () => {
  const frame = useCurrentFrame();
  const shake = useShake([108]);

  // Dense samples along a smooth curve through the data.
  const samples = useMemo(() => {
    const out: { year: number; v: number }[] = [];
    for (let i = 0; i < DATA.length - 1; i++) {
      const p0 = DATA[Math.max(0, i - 1)];
      const p1 = DATA[i];
      const p2 = DATA[i + 1];
      const p3 = DATA[Math.min(DATA.length - 1, i + 2)];
      for (let s = 0; s < 30; s++) {
        const u = s / 30;
        const cr = (a: number, b: number, c: number, d: number) =>
          0.5 * (2 * b + (-a + c) * u + (2 * a - 5 * b + 4 * c - d) * u * u + (-a + 3 * b - 3 * c + d) * u * u * u);
        out.push({ year: mix(p1[0], p2[0], u), v: Math.max(0, cr(p0[1], p1[1], p2[1], p3[1])) });
      }
    }
    out.push({ year: 2025, v: 1.5 });
    return out;
  }, []);

  const lineP = prog(frame, LINE_START, LINE_DUR, E.expoInOut);
  const idx = Math.round(lineP * (samples.length - 1));
  const tip = samples[idx];
  const d = samples.map((s, i) => `${i === 0 ? "M" : "L"}${X(s.year).toFixed(1)},${Y(s.v).toFixed(1)}`).join(" ");
  const area = `${d} L${X(2025)},${CH.y + CH.h} L${X(1900)},${CH.y + CH.h} Z`;

  const iris = prog(frame, 0, IRIS, E.expoInOut) * 2300;
  const big = prog(frame, 104, 14, E.backOut);

  return (
    <AbsoluteFill style={{ clipPath: `circle(${iris}px at ${PRICE_END.x}px ${PRICE_END.y}px)` }}>
      <AbsoluteFill style={{ backgroundColor: C.blue, overflow: "hidden", transform: shake }}>
        <div style={{ position: "absolute", top: 200, width: 1080, display: "flex", flexDirection: "column", alignItems: "center" }}>
          <Letters text="МАШИН" start={6} stagger={2} mode="scatter" style={{ fontFamily: display, fontWeight: 900, fontSize: 140, color: C.paper }} />
          <Letters text="НА ПЛАНЕТЕ" start={12} stagger={1.5} mode="scatter" style={{ fontFamily: display, fontWeight: 300, fontSize: 80, color: C.paper }} />
        </div>

        <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
          <defs>
            <clipPath id="fleet-reveal">
              <rect x={0} y={0} width={X(tip.year)} height={1920} />
            </clipPath>
            <linearGradient id="fleet-fill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0" stopColor={C.yellow} stopOpacity={0.55} />
              <stop offset="1" stopColor={C.yellow} stopOpacity={0} />
            </linearGradient>
          </defs>
          {[0, 0.5, 1, 1.5].map((v, i) => (
            <g key={v} opacity={prog(frame, 16 + i * 3, 12)}>
              <line x1={CH.x} x2={CH.x + CH.w} y1={Y(v)} y2={Y(v)} stroke={C.paper} strokeOpacity={0.2} strokeWidth={2} strokeDasharray="8 10" />
              <text x={CH.x} y={Y(v) - 12} fill={C.paper} opacity={0.7} fontFamily={body} fontSize={26}>
                {v === 0 ? "" : `${String(v).replace(".", ",")} млрд`}
              </text>
            </g>
          ))}
          {[1900, 1950, 2000, 2025].map((y) => (
            <text key={y} x={X(y)} y={CH.y + CH.h + 50} textAnchor="middle" fill={C.paper} opacity={prog(frame, 22, 12) * 0.8} fontFamily={body} fontSize={28}>
              {y}
            </text>
          ))}
          <Draw d={`M${CH.x},${CH.y + CH.h} L${CH.x + CH.w},${CH.y + CH.h}`} start={14} dur={16} stroke={C.paper} width={4} />
          <path d={area} fill="url(#fleet-fill)" clipPath="url(#fleet-reveal)" />
          <Draw d={d} start={LINE_START} dur={LINE_DUR} stroke={C.yellow} width={10} />
          {MILESTONES.map((m) => {
            const v = DATA.find((x) => x[0] === m.year)![1];
            const at = prog(frame, LINE_START + ((m.year - 1900) / 125) * LINE_DUR * 0.8, 12, E.backOut);
            return (
              <g key={m.year} opacity={at}>
                <line x1={X(m.year)} x2={X(m.year)} y1={Y(v)} y2={CH.y + CH.h} stroke={C.paper} strokeWidth={2} strokeDasharray="6 8" />
                <circle cx={X(m.year)} cy={Y(v)} r={12 * at} fill={C.blue} stroke={C.paper} strokeWidth={5} />
                <text x={X(m.year) - 16} y={Y(v) - 26} textAnchor="end" fill={C.paper} fontFamily={condensed} fontWeight={600} fontSize={36}>
                  {m.label}
                </text>
              </g>
            );
          })}
          <circle cx={X(tip.year)} cy={Y(tip.v)} r={18} fill={C.yellow} opacity={lineP > 0 ? 1 : 0} />
          <circle cx={X(tip.year)} cy={Y(tip.v)} r={18 + (frame % 20) * 2.2} fill="none" stroke={C.yellow} strokeWidth={3} opacity={(1 - (frame % 20) / 20) * (lineP > 0 ? 1 : 0)} />
        </svg>
        <div
          style={{
            position: "absolute",
            left: Math.min(X(tip.year) - 120, 700),
            whiteSpace: "nowrap",
            top: Y(tip.v) - 120,
            fontFamily: condensed,
            fontWeight: 700,
            fontSize: 46,
            color: C.ink,
            background: C.yellow,
            padding: "4px 16px",
            opacity: lineP > 0 && lineP < 1 ? 1 : 0,
          }}
        >
          {Math.round(tip.year)} · {fmt(tip.v)}
        </div>

        <div style={{ position: "absolute", top: 1440, width: 1080, textAlign: "center" }}>
          <div style={{ fontFamily: display, fontWeight: 900, fontSize: 150, color: C.yellow, transform: `scale(${big})`, lineHeight: 1 }}>
            1,5 МЛРД
          </div>
          <MaskLine start={112}>
            <div style={{ fontFamily: body, fontWeight: 600, fontSize: 36, color: C.paper, marginTop: 24 }}>
              автомобилей ездят по миру сегодня (≈)
            </div>
          </MaskLine>
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
