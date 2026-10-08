import React from "react";
import { AbsoluteFill, interpolate, interpolateColors, useCurrentFrame } from "remotion";
import { C, body, condensed, display, Draw, E, Letters, MaskLine, mix, prog } from "../kit";

export const S2_DURATION = 125;
export const AXIS_Y = 1100;

const CX = 540;
const CY = 1000;

const Callout: React.FC<{ d: string; start: number; x: number; y: number; align: "left" | "right"; kicker: string; title: string }> = ({
  d,
  start,
  x,
  y,
  align,
  kicker,
  title,
}) => {
  const frame = useCurrentFrame();
  const [, dx, dy] = d.match(/M(\d+),(\d+)/) ?? ["", "0", "0"];
  return (
    <>
      <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
        <Draw d={d} start={start} dur={14} stroke={C.paper} width={3} />
        <circle cx={Number(dx)} cy={Number(dy)} r={10 * prog(frame, start, 10, E.backOut)} fill={C.red} />
      </svg>
      <div style={{ position: "absolute", top: y, [align]: x, textAlign: align }}>
        <MaskLine start={start + 8} out={78}>
          <div style={{ fontFamily: body, fontSize: 30, fontWeight: 600, letterSpacing: 6, color: C.grey }}>{kicker}</div>
        </MaskLine>
        <MaskLine start={start + 11} out={80}>
          <div style={{ fontFamily: condensed, fontSize: 64, fontWeight: 600, color: C.paper }}>{title}</div>
        </MaskLine>
      </div>
    </>
  );
};

export const S2Wheel: React.FC = () => {
  const frame = useCurrentFrame();

  const settle = prog(frame, 0, 16);
  const spokes = prog(frame, 6, 26, E.expoOut);
  const shrink = prog(frame, 84, 18, E.expoInOut);
  const roll = prog(frame, 100, 25, E.expoIn);

  const r = mix(300, 75, shrink);
  const cy = mix(CY, AXIS_Y - 75, shrink);
  const cx = mix(CX, 1300, roll);
  const stroke = interpolate(settle, [0, 1], [103, 22]) * mix(1, 0.6, shrink);
  const angle = frame * 1.2 + ((cx - CX) / r) * (180 / Math.PI);
  const rim = interpolateColors(settle, [0, 1], [C.red, C.paper]);
  const textOut = prog(frame, 80, 10, E.expoIn);

  return (
    <AbsoluteFill style={{ backgroundColor: C.ink, overflow: "hidden" }}>
      {/* Header */}
      <div style={{ position: "absolute", top: 170, width: 1080, display: "flex", flexDirection: "column", alignItems: "center" }}>
        <MaskLine start={8} out={80}>
          <div style={{ fontFamily: display, fontWeight: 800, fontSize: 70, color: C.red }}>≈3500 ДО Н.Э.</div>
        </MaskLine>
        <Letters
          text="МЕСОПОТАМИЯ"
          start={14}
          out={82}
          stagger={1.5}
          mode="spin"
          style={{ fontFamily: condensed, fontWeight: 700, fontSize: 150, color: C.paper, marginTop: 6 }}
        />
      </div>

      {/* The wheel */}
      <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
        {roll > 0 ? (
          <line x1={CX - (cx - CX)} y1={AXIS_Y} x2={cx} y2={AXIS_Y} stroke={C.red} strokeWidth={6} />
        ) : null}
        <g transform={`translate(${cx} ${cy}) rotate(${angle})`}>
          <circle r={r} fill="none" stroke={rim} strokeWidth={stroke} />
          <circle r={r * 0.78} fill="none" stroke={C.paper} strokeWidth={stroke * 0.25} opacity={spokes} />
          {new Array(6).fill(0).map((_, i) => {
            const a = (i / 6) * Math.PI * 2;
            const k = Math.min(1, Math.max(0, spokes * 6 - i));
            return (
              <line
                key={i}
                x1={0}
                y1={0}
                x2={Math.cos(a) * r * 0.78 * k}
                y2={Math.sin(a) * r * 0.78 * k}
                stroke={C.paper}
                strokeWidth={stroke * 0.55}
                strokeLinecap="round"
              />
            );
          })}
          <circle r={r * 0.13} fill={C.red} opacity={spokes} />
        </g>
      </svg>

      {/* Callouts */}
      {frame < 92 ? (
        <>
          <Callout d="M300,1180 L220,1380 L60,1380" start={30} x={60} y={1400} align="left" kicker="СНАЧАЛА" title="ГОНЧАРНЫЙ КРУГ" />
          <Callout d="M780,1180 L860,1540 L1020,1540" start={44} x={60} y={1560} align="right" kicker="ПОТОМ" title="ПОВОЗКА" />
        </>
      ) : null}

      <div style={{ position: "absolute", bottom: 150, width: 1080, textAlign: "center", opacity: 1 - textOut }}>
        <MaskLine start={56}>
          <div style={{ fontFamily: body, fontWeight: 500, fontSize: 38, color: C.grey }}>
            Первые колёса — сплошные деревянные диски
          </div>
        </MaskLine>
      </div>
    </AbsoluteFill>
  );
};
