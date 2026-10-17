import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { C, body, condensed, display, E, MaskLine, mix, prog, WheelGlyph } from "../kit";
import { AXIS_Y } from "./S2Wheel";

export const S3_DURATION = 130;
export const WHIP = 12;
const SPACING = 760;

const EVENTS = [
  { year: -3500, title: "КОЛЕСО", desc: "сплошной диск из досок" },
  { year: -2000, title: "КОЛЕСНИЦА", desc: "спицы сделали колесо лёгким" },
  { year: 1769, title: "ПАРОВАЯ ТЕЛЕГА", desc: "Кюньо: первая машина на пару" },
  { year: 1886, title: "АВТОМОБИЛЬ", desc: "бензиновый мотор Карла Бенца" },
];

// Pan keyframes: [frame, event index]
const PAN: [number, number][] = [
  [0, 0],
  [14, 0],
  [34, 1],
  [50, 1],
  [70, 2],
  [86, 2],
  [106, 3],
];

const formatYear = (y: number) => (y < 0 ? `${Math.abs(Math.round(y))} до н.э.` : `${Math.round(y)}`);

const Icon: React.FC<{ kind: number; frame: number }> = ({ kind, frame }) => {
  const s = C.paper;
  switch (kind) {
    case 0:
      return (
        <g>
          <circle r={70} fill={s} />
          {[-35, 0, 35].map((x) => (
            <line key={x} x1={x} y1={-62} x2={x} y2={62} stroke={C.ink} strokeWidth={5} />
          ))}
          <circle r={14} fill={C.red} />
        </g>
      );
    case 1:
      return <WheelGlyph r={70} angle={frame * 3} spokes={6} width={9} />;
    case 2:
      return (
        <g>
          <rect x={-80} y={-20} width={120} height={60} rx={8} fill="none" stroke={s} strokeWidth={8} />
          <circle cx={58} cy={0} r={34} fill="none" stroke={s} strokeWidth={8} />
          {[0, 1, 2].map((i) => {
            const t = ((frame / 30 + i / 3) % 1 + 1) % 1;
            return <circle key={i} cx={58 + t * 20} cy={-50 - t * 50} r={10 + t * 16} fill={s} opacity={1 - t} />;
          })}
          <circle cx={-50} cy={52} r={22} fill="none" stroke={s} strokeWidth={7} />
          <circle cx={40} cy={52} r={22} fill="none" stroke={s} strokeWidth={7} />
        </g>
      );
    default:
      return (
        <g>
          <path d="M-90,10 L20,10 L30,-30 L-60,-30 Z M-80,-30 L-84,-62 L-40,-62" fill="none" stroke={s} strokeWidth={8} strokeLinejoin="round" />
          <line x1={20} y1={10} x2={70} y2={30} stroke={s} strokeWidth={8} />
          <line x1={30} y1={-30} x2={50} y2={-70} stroke={s} strokeWidth={8} strokeLinecap="round" />
          <WheelGlyph r={44} angle={frame * 4} spokes={10} width={6} />
          <g transform="translate(78 40)">
            <WheelGlyph r={26} angle={frame * 6} spokes={8} width={5} hub={false} />
          </g>
        </g>
      );
  }
};

export const S3Timeline: React.FC = () => {
  const frame = useCurrentFrame();

  const pos = interpolate(
    frame,
    PAN.map((k) => k[0]),
    PAN.map((k) => k[1]),
    { easing: E.expoInOut, extrapolateLeft: "clamp", extrapolateRight: "clamp" },
  );
  const prevPos = interpolate(
    frame - 1,
    PAN.map((k) => k[0]),
    PAN.map((k) => k[1]),
    { easing: E.expoInOut, extrapolateLeft: "clamp", extrapolateRight: "clamp" },
  );
  const velocity = Math.abs(pos - prevPos) * SPACING;
  const offset = 540 - pos * SPACING;

  const i = Math.min(Math.floor(pos), EVENTS.length - 2);
  const year = mix(EVENTS[i].year, EVENTS[i + 1].year, pos - i);

  const whip = prog(frame, S3_DURATION - WHIP, WHIP, E.expoIn);

  return (
    <AbsoluteFill style={{ backgroundColor: C.ink, overflow: "hidden" }}>
      <AbsoluteFill style={{ transform: `translateX(${-whip * 1080}px)`, filter: `blur(${whip * 24}px)` }}>
        {/* Year counter */}
        <div style={{ position: "absolute", top: 200, width: 1080, textAlign: "center" }}>
          <div
            style={{
              fontFamily: display,
              fontWeight: 900,
              fontSize: year < 0 ? 104 : 170,
              color: C.paper,
              fontVariantNumeric: "tabular-nums",
              transform: `skewX(${-Math.min(velocity, 60) * 0.3}deg)`,
            }}
          >
            {formatYear(year)}
          </div>
          <MaskLine start={4}>
            <div style={{ fontFamily: body, fontWeight: 700, fontSize: 32, letterSpacing: 14, color: C.red, marginTop: 12 }}>ЛИНИЯ ВРЕМЕНИ</div>
          </MaskLine>
        </div>

        {/* Axis */}
        <div style={{ position: "absolute", left: 0, right: 0, top: AXIS_Y - 3, height: 6, background: C.red }} />

        {/* Moving strip */}
        <AbsoluteFill style={{ transform: `translateX(${offset}px)`, filter: `blur(${Math.min(velocity, 80) * 0.08}px)` }}>
          <svg width={SPACING * 4 + 1080} height={1920} style={{ position: "absolute", left: -540, top: 0, overflow: "visible" }}>
            {new Array(60).fill(0).map((_, k) => (
              <line
                key={k}
                x1={k * 76}
                x2={k * 76}
                y1={AXIS_Y + 12}
                y2={AXIS_Y + (k % 5 === 0 ? 44 : 26)}
                stroke={C.grey}
                strokeWidth={3}
              />
            ))}
          </svg>
          {EVENTS.map((ev, k) => {
            const appear = k === 0 ? -10 : PAN[k * 2][0] - 6;
            const p = prog(frame, appear, 22, E.expoOut);
            const pole = prog(frame, appear, 16, E.expoOut);
            return (
              <div key={k} style={{ position: "absolute", left: k * SPACING, top: 0 }}>
                <div
                  style={{
                    position: "absolute",
                    left: -3,
                    top: AXIS_Y - 300 * pole,
                    width: 6,
                    height: 300 * pole,
                    background: C.paper,
                  }}
                />
                <div
                  style={{
                    position: "absolute",
                    left: -22,
                    top: AXIS_Y - 22,
                    width: 44,
                    height: 44,
                    borderRadius: 22,
                    background: C.paper,
                    border: `8px solid ${C.red}`,
                    boxSizing: "border-box",
                    transform: `scale(${p})`,
                  }}
                />
                <svg width={300} height={300} style={{ position: "absolute", left: -150, top: AXIS_Y - 560, overflow: "visible" }}>
                  <g transform={`translate(150 150) scale(${p})`}>
                    <Icon kind={k} frame={frame} />
                  </g>
                </svg>
                <div style={{ position: "absolute", left: -400, width: 800, top: AXIS_Y + 80, textAlign: "center" }}>
                  <MaskLine start={appear + 6}>
                    <div style={{ fontFamily: condensed, fontWeight: 700, fontSize: 92, color: C.paper }}>{ev.title}</div>
                  </MaskLine>
                  <MaskLine start={appear + 10}>
                    <div style={{ fontFamily: body, fontWeight: 500, fontSize: 38, color: C.grey, marginTop: 10 }}>{ev.desc}</div>
                  </MaskLine>
                </div>
              </div>
            );
          })}
        </AbsoluteFill>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
