import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { C, body, condensed, Count, display, Draw, E, Letters, MaskLine, prog, WheelGlyph } from "../kit";
import { WHIP } from "./S3Timeline";

export const S4_DURATION = 155;

// Drawing lives in a 600×300 box, placed at (60, 520) and scaled ×1.6.
const OX = 60;
const OY = 520;
const K = 1.6;
const toScreen = (x: number, y: number) => [OX + x * K, OY + y * K] as const;

const PARTS = [
  "M170,190 L300,186 L450,225",
  "M100,150 L265,150 L275,112 L112,112 Z",
  "M115,112 L108,62 L180,62 L186,112",
  "M95,165 Q135,150 175,165 Q135,180 95,165 Z",
  "M300,186 L330,92 L364,86",
  "M300,150 L420,160 L450,225",
  "M85,112 Q170,40 255,112",
];

const STATS = [
  { value: 0.75, decimals: 2, unit: "Л.С.", label: "мощность", anchor: [135, 165] },
  { value: 16, decimals: 0, unit: "КМ/Ч", label: "скорость", anchor: [450, 225] },
  { value: 954, decimals: 0, unit: "СМ³", label: "объём мотора", anchor: [190, 150] },
  { value: 3, decimals: 0, unit: "КОЛЕСА", label: "конструкция", anchor: [330, 92] },
];

export const S4Benz: React.FC = () => {
  const frame = useCurrentFrame();
  const enter = 1 - prog(frame, 0, WHIP, E.expoOut);
  const wheelsDraw = prog(frame, 8, 24, E.expoInOut);

  return (
    <AbsoluteFill style={{ backgroundColor: C.paper, overflow: "hidden" }}>
      <AbsoluteFill style={{ transform: `translateX(${enter * 1080}px)`, filter: `blur(${enter * 24}px)` }}>
        {/* Blueprint grid */}
        <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
          {new Array(24).fill(0).map((_, i) => (
            <line
              key={i}
              x1={0}
              x2={1080 * prog(frame, 4 + i, 20)}
              y1={i * 80 + 40}
              y2={i * 80 + 40}
              stroke={C.ink}
              strokeOpacity={0.06}
              strokeWidth={2}
            />
          ))}
        </svg>

        <div style={{ position: "absolute", top: 150, width: 1080, display: "flex", flexDirection: "column", alignItems: "center" }}>
          <Letters text="1886" start={6} stagger={3} mode="drop" style={{ fontFamily: display, fontWeight: 900, fontSize: 210, color: C.ink, letterSpacing: -8 }} />
          <MaskLine start={18}>
            <div style={{ fontFamily: condensed, fontWeight: 600, fontSize: 60, letterSpacing: 4, color: C.red }}>BENZ PATENT-MOTORWAGEN</div>
          </MaskLine>
        </div>

        {/* Line-art car */}
        <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
          <g transform={`translate(${OX} ${OY}) scale(${K})`}>
            <line x1={0} x2={600 * prog(frame, 10, 30)} y1={272} y2={272} stroke={C.ink} strokeWidth={3} />
            <g transform="translate(170 190)">
              <WheelGlyph r={80} angle={frame * 2} spokes={12} stroke={C.ink} width={5} draw={wheelsDraw} />
            </g>
            <g transform="translate(450 225)">
              <WheelGlyph r={45} angle={frame * 3.5} spokes={10} stroke={C.ink} width={4} draw={wheelsDraw} />
            </g>
            {PARTS.map((d, i) => (
              <Draw key={i} d={d} start={22 + i * 4} dur={20} stroke={C.ink} width={5} fill={i === 1 ? C.red : "none"} fillStart={i === 1 ? 50 : undefined} />
            ))}
          </g>
        </svg>

        {/* Stat callouts */}
        <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
          {STATS.map((s, i) => {
            const [ax, ay] = toScreen(s.anchor[0], s.anchor[1]);
            const bx = i % 2 === 0 ? 290 : 790;
            const by = 1170 + Math.floor(i / 2) * 290;
            return (
              <g key={i}>
                <Draw d={`M${ax},${ay} L${ax},${by - 40} L${bx},${by - 40} L${bx},${by - 20}`} start={56 + i * 6} dur={16} stroke={C.ink} width={2.5} />
                <circle cx={ax} cy={ay} r={9 * prog(frame, 56 + i * 6, 10, E.backOut)} fill={C.red} />
              </g>
            );
          })}
        </svg>
        {STATS.map((s, i) => {
          const bx = i % 2 === 0 ? 290 : 790;
          const by = 1170 + Math.floor(i / 2) * 290;
          const start = 64 + i * 6;
          return (
            <div key={i} style={{ position: "absolute", left: bx - 240, width: 480, top: by, textAlign: "center" }}>
              <MaskLine start={start}>
                <div style={{ fontFamily: display, fontWeight: 900, fontSize: 104, color: C.ink, lineHeight: 1 }}>
                  <Count to={s.value} start={start} dur={30} decimals={s.decimals} />
                </div>
              </MaskLine>
              <MaskLine start={start + 4}>
                <div style={{ fontFamily: condensed, fontWeight: 700, fontSize: 46, color: C.red }}>
                  {s.unit} <span style={{ fontFamily: body, fontWeight: 500, fontSize: 30, color: C.grey }}>· {s.label}</span>
                </div>
              </MaskLine>
            </div>
          );
        })}

        <div style={{ position: "absolute", top: 1760, width: 1080, textAlign: "center" }}>
          <MaskLine start={96}>
            <div style={{ fontFamily: body, fontWeight: 700, fontSize: 32, letterSpacing: 6, color: C.ink }}>
              29 ЯНВАРЯ 1886 · ПАТЕНТ № 37435
            </div>
          </MaskLine>
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
