import React from "react";
import { AbsoluteFill, random, useCurrentFrame } from "remotion";
import { C, body, condensed, Count, display, Draw, E, Letters, MaskLine, mix, prog, useShake } from "../kit";

export const S9_DURATION = 180;

const BOLT = "M600,160 L470,420 L560,420 L480,640 L700,340 L600,340 L680,160 Z";
const SHARE: [string, number][] = [
  ["2015", 1],
  ["2018", 2],
  ["2020", 4],
  ["2022", 14],
  ["2024", 22],
];
const BAR = { x: 130, base: 1420, h: 520, w: 130, gap: 37 };
const BATT = { x: 330, y: 1560, w: 380, h: 150 };
const FILL_START = 150;

export const S9Electric: React.FC = () => {
  const frame = useCurrentFrame();
  const shake = useShake([16]);
  const glow = Math.max(0, 1 - prog(frame, 16, 20));
  const jitter = frame >= 18 && frame < 34 ? (random(`j${frame}`) - 0.5) * 30 : 0;
  const rgb = frame >= 18 && frame < 34 ? 10 : 0;
  const charge = prog(frame, 92, 40, E.quintOut);
  const fill = prog(frame, FILL_START, S9_DURATION - FILL_START, E.expoInOut);

  const fx = mix(BATT.x + 12, 0, fill);
  const fy = mix(BATT.y + 12, 0, fill);
  const fw = mix((BATT.w - 24) * charge, 1080, fill);
  const fh = mix(BATT.h - 24, 1920, fill);

  return (
    <AbsoluteFill style={{ backgroundColor: C.ink, overflow: "hidden" }}>
      <AbsoluteFill style={{ transform: shake }}>
        <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
          <g transform="translate(-35 0) scale(0.75) translate(180 40)">
            <Draw d={BOLT} start={0} dur={14} stroke={C.yellow} width={10} fill={C.yellow} fillStart={14} />
          </g>
        </svg>
        <AbsoluteFill style={{ background: `radial-gradient(circle at 50% 22%, rgba(255,210,63,${glow * 0.6}) 0%, transparent 45%)` }} />

        <div style={{ position: "absolute", top: 560, width: 1080, display: "flex", justifyContent: "center", transform: `translateX(${jitter}px)` }}>
          <Letters
            text="ЭЛЕКТРО"
            start={16}
            stagger={1.5}
            mode="scale"
            style={{ fontFamily: display, fontWeight: 900, fontSize: 140, color: C.paper, textShadow: `${rgb}px 0 #ff0050, ${-rgb}px 0 #00e5ff` }}
          />
        </div>
        <div style={{ position: "absolute", top: 760, width: 1080, textAlign: "center" }}>
          <MaskLine start={30}>
            <div style={{ fontFamily: body, fontWeight: 700, fontSize: 34, letterSpacing: 8, color: C.grey }}>
              ДОЛЯ В ПРОДАЖАХ НОВЫХ МАШИН
            </div>
          </MaskLine>
        </div>

        {/* Bar chart */}
        <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
          <Draw d={`M${BAR.x - 20},${BAR.base} L${1080 - BAR.x + 20},${BAR.base}`} start={34} dur={14} stroke={C.paper} width={4} />
          {SHARE.map(([year, v], i) => {
            const p = prog(frame, 42 + i * 6, 24, E.expoOut);
            const h = (v / 24) * BAR.h * p;
            const x = BAR.x + i * (BAR.w + BAR.gap);
            const last = i === SHARE.length - 1;
            return (
              <g key={year}>
                <rect x={x} y={BAR.base - h} width={BAR.w} height={h} fill={last ? C.yellow : C.paper} opacity={last ? 1 : 0.85} />
                <text x={x + BAR.w / 2} y={BAR.base + 50} textAnchor="middle" fill={C.grey} fontFamily={body} fontSize={30} opacity={p}>
                  {year}
                </text>
              </g>
            );
          })}
        </svg>
        {SHARE.map(([year, v], i) => {
          const start = 42 + i * 6;
          const h = (v / 24) * BAR.h * prog(frame, start, 24, E.expoOut);
          const x = BAR.x + i * (BAR.w + BAR.gap);
          return (
            <div
              key={year}
              style={{
                position: "absolute",
                left: x - 30,
                width: BAR.w + 60,
                top: BAR.base - h - 76,
                textAlign: "center",
                fontFamily: condensed,
                fontWeight: 700,
                fontSize: 56,
                color: i === SHARE.length - 1 ? C.yellow : C.paper,
                opacity: prog(frame, start + 4, 10),
              }}
            >
              <Count to={v} start={start} dur={24} suffix="%" />
            </div>
          );
        })}

        {/* Battery */}
        <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
          <rect x={BATT.x} y={BATT.y} width={BATT.w} height={BATT.h} rx={20} fill="none" stroke={C.paper} strokeWidth={8} opacity={prog(frame, 84, 10)} />
          <rect x={BATT.x + BATT.w + 4} y={BATT.y + 45} width={22} height={60} rx={6} fill={C.paper} opacity={prog(frame, 84, 10)} />
        </svg>
        <div
          style={{
            position: "absolute",
            top: BATT.y + BATT.h + 20,
            width: 1080,
            textAlign: "center",
            fontFamily: display,
            fontWeight: 800,
            fontSize: 44,
            color: C.paper,
            opacity: prog(frame, 92, 10) * (1 - fill * 4),
          }}
        >
          <Count to={100} start={92} dur={40} suffix="%" />
        </div>
      </AbsoluteFill>
      {/* Battery charge — becomes the next scene's background */}
      <div style={{ position: "absolute", left: fx, top: fy, width: fw, height: fh, background: C.yellow, borderRadius: mix(10, 0, fill) }} />
    </AbsoluteFill>
  );
};
