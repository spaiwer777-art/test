import React from "react";
import { AbsoluteFill, interpolate, random, useCurrentFrame } from "remotion";
import { C, body, condensed, display, E, MaskLine, mix, prog, useShake } from "../kit";
import { RingWord } from "../RingWord";

export const S7_DURATION = 215;

const RING = { x: 600, y: 900, r: 62, w: 18 };
const G = { x: 540, y: 1000, r: 380 };
const ZOOM = [30, 50];
const MAX = 500;
const A0 = 150;
const SWEEP = 240;

const STOPS = [
  { v: 16, year: "1886", name: "Benz Motorwagen" },
  { v: 70, year: "1908", name: "Ford Model T" },
  { v: 250, year: "1955", name: "Mercedes 300 SL" },
  { v: 324, year: "1987", name: "Ferrari F40" },
  { v: 490, year: "2019", name: "Bugatti Chiron SS" },
];
const STOP_START = 74;
const STOP_STEP = 26;
const MOVE = 14;
const GLITCH = 200;

const angleOf = (v: number) => A0 + (v / MAX) * SWEEP;
const polar = (r: number, deg: number) => {
  const a = (deg * Math.PI) / 180;
  return [G.x + Math.cos(a) * r, G.y + Math.sin(a) * r] as const;
};

export const S7Speed: React.FC = () => {
  const frame = useCurrentFrame();
  const shake = useShake([10, STOP_START + 4 * STOP_STEP + MOVE], 30);

  const zoom = prog(frame, ZOOM[0], ZOOM[1] - ZOOM[0], E.expoIn);
  const k = interpolate(zoom, [0, 1], [1, G.r / RING.r]);
  const gauge = frame >= ZOOM[1];

  // Needle value: step through the stops with snappy moves.
  let v = 0;
  let active = -1;
  STOPS.forEach((s, i) => {
    const t0 = STOP_START + i * STOP_STEP;
    const p = prog(frame, t0, MOVE, E.expoInOut);
    if (p > 0) {
      v = mix(i === 0 ? 0 : STOPS[i - 1].v, s.v, p);
      active = i;
    }
  });
  const wobble = active >= 0 ? Math.sin(frame * 1.7) * 1.2 : 0;
  const needleA = angleOf(v + wobble);
  const [nx, ny] = polar(G.r - 70, needleA);
  const arcP = prog(frame, ZOOM[1], 14, E.expoOut);
  const ticksP = prog(frame, ZOOM[1] + 4, 22, E.expoOut);
  const ringW = mix(RING.w * (G.r / RING.r), 14, arcP);

  const g = frame >= GLITCH ? prog(frame, GLITCH, S7_DURATION - GLITCH, E.expoIn) : 0;
  const slices = g > 0 ? new Array(6).fill(0).map((_, i) => (random(`gl${frame}-${i}`) - 0.5) * 220 * g) : [];

  const [ax, ay] = polar(G.r - 36, A0);
  const [bx, by] = polar(G.r - 36, needleA);
  const large = needleA - A0 > 180 ? 1 : 0;

  const content = (
    <AbsoluteFill style={{ transform: shake }}>
      {!gauge ? (
        <AbsoluteFill style={{ transformOrigin: `${RING.x}px ${RING.y}px`, transform: `translate(${zoom * (G.x - RING.x)}px, ${zoom * (G.y - RING.y)}px) scale(${k})` }}>
          <RingWord left="СКОР" right="СТЬ" cx={RING.x} cy={RING.y} size={190} font={condensed} weight={700} start={2} ringR={RING.r} ringW={RING.w} gap={8} />
        </AbsoluteFill>
      ) : (
        <>
          <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
            <circle cx={G.x} cy={G.y} r={G.r} fill="none" stroke={C.red} strokeWidth={ringW} />
            {new Array(51).fill(0).map((_, i) => {
              const val = i * 10;
              const major = val % 50 === 0;
              const show = Math.min(1, Math.max(0, ticksP * 51 - i));
              const [x1, y1] = polar(G.r - 40, angleOf(val));
              const [x2, y2] = polar(G.r - (major ? 90 : 62), angleOf(val));
              const [tx, ty] = polar(G.r - 130, angleOf(val));
              return (
                <g key={i} opacity={show}>
                  <line x1={x1} y1={y1} x2={x2} y2={y2} stroke={val <= v ? C.red : C.paper} strokeWidth={major ? 7 : 3} />
                  {major ? (
                    <text x={tx} y={ty + 12} textAnchor="middle" fill={C.grey} fontFamily={condensed} fontWeight={600} fontSize={34}>
                      {val}
                    </text>
                  ) : null}
                </g>
              );
            })}
            {v > 0.5 ? (
              <path d={`M${ax},${ay} A${G.r - 36} ${G.r - 36} 0 ${large} 1 ${bx},${by}`} fill="none" stroke={C.red} strokeWidth={14} strokeLinecap="round" opacity={0.85} />
            ) : null}
            <line x1={G.x} y1={G.y} x2={nx} y2={ny} stroke={C.yellow} strokeWidth={10} strokeLinecap="round" opacity={ticksP} />
            <circle cx={G.x} cy={G.y} r={28 * ticksP} fill={C.yellow} />
          </svg>
          <div style={{ position: "absolute", top: G.y + 90, width: 1080, textAlign: "center", opacity: ticksP }}>
            <div style={{ fontFamily: display, fontWeight: 900, fontSize: 120, color: C.paper, fontVariantNumeric: "tabular-nums", lineHeight: 1 }}>
              {Math.round(v)}
            </div>
            <div style={{ fontFamily: condensed, fontWeight: 600, fontSize: 40, color: C.red, letterSpacing: 8 }}>КМ/Ч</div>
          </div>
          <div style={{ position: "absolute", top: 230, width: 1080, textAlign: "center" }}>
            <MaskLine start={ZOOM[1] + 4}>
              <div style={{ fontFamily: body, fontWeight: 700, fontSize: 34, letterSpacing: 10, color: C.grey }}>МАКСИМАЛЬНАЯ СКОРОСТЬ</div>
            </MaskLine>
          </div>
          {STOPS.map((s, i) => {
            const t0 = STOP_START + i * STOP_STEP;
            const out = i === STOPS.length - 1 ? GLITCH : t0 + STOP_STEP;
            if (frame < t0 - 2 || frame > out + 16) return null;
            return (
              <div key={i} style={{ position: "absolute", top: 1490, width: 1080, textAlign: "center" }}>
                <MaskLine start={t0 + 4} out={out} dur={12}>
                  <div style={{ fontFamily: condensed, fontWeight: 700, fontSize: 60, color: C.red }}>{s.year}</div>
                </MaskLine>
                <MaskLine start={t0 + 7} out={out + 2} dur={12}>
                  <div style={{ fontFamily: display, fontWeight: 800, fontSize: 64, color: C.paper }}>{s.name}</div>
                </MaskLine>
              </div>
            );
          })}
        </>
      )}
    </AbsoluteFill>
  );

  return (
    <AbsoluteFill style={{ backgroundColor: C.ink, overflow: "hidden" }}>
      {g > 0 ? (
        <>
          {slices.map((dx, i) => (
            <AbsoluteFill key={i} style={{ clipPath: `inset(${(i * 1920) / 6}px 0 ${1920 - ((i + 1) * 1920) / 6}px 0)`, transform: `translateX(${dx}px)` }}>
              {content}
            </AbsoluteFill>
          ))}
          <AbsoluteFill style={{ mixBlendMode: "screen", transform: `translateX(${g * 30}px)`, opacity: g, background: "rgba(255,0,60,0.25)" }} />
          <AbsoluteFill style={{ background: C.paper, opacity: Math.pow(g, 3) }} />
        </>
      ) : (
        content
      )}
    </AbsoluteFill>
  );
};
