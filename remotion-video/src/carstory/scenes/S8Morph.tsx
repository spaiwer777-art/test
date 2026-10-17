import React, { useMemo } from "react";
import { interpolate as flubber } from "flubber";
import { AbsoluteFill, interpolateColors, useCurrentFrame } from "remotion";
import { CARS } from "../cars";
import { C, body, condensed, display, E, MaskLine, mix, prog } from "../kit";

export const S8_DURATION = 220;

const SEG = 40;
const MORPH = 14;
const EXIT = 200;
const BGS = [C.paper, C.ink, C.paper, C.ink, C.paper];
const INKS = [C.ink, C.paper, C.ink, C.paper, C.ink];
const NOTES = ["20 Л.С. · 70 КМ/Ч", "ХРОМ И ПЛАВНИКИ", "МОТОР V8", "КЛИНОВИДНЫЙ ДИЗАЙН", "0 ЛИТРОВ БЕНЗИНА"];

// Odometer-style digit that rolls between two values.
const Digit: React.FC<{ from: number; to: number; t: number; color: string }> = ({ from, to, t, color }) => {
  const steps = (to - from + 10) % 10;
  const pos = from + steps * t;
  return (
    <div style={{ height: 300, overflow: "hidden", width: 190 }}>
      <div style={{ transform: `translateY(${-pos * 300}px)` }}>
        {new Array(20).fill(0).map((_, i) => (
          <div key={i} style={{ height: 300, lineHeight: "300px", textAlign: "center", color, opacity: 0.14 }}>
            {i % 10}
          </div>
        ))}
      </div>
    </div>
  );
};

export const S8Morph: React.FC = () => {
  const frame = useCurrentFrame();

  const morphs = useMemo(
    () =>
      CARS.slice(1).map((c, i) => ({
        body: flubber(CARS[i].body, c.body, { maxSegmentLength: 4 }),
        glass: flubber(CARS[i].glass, c.glass, { maxSegmentLength: 4 }),
      })),
    [],
  );

  // Which transition are we in, and how far along?
  const seg = Math.min(CARS.length - 1, Math.floor(frame / SEG));
  const within = frame - seg * SEG;
  const t = seg === 0 ? 1 : prog(within, 0, MORPH, E.expoInOut);
  const from = Math.max(0, seg - 1);
  const to = seg;
  const bodyD = seg === 0 ? CARS[0].body : morphs[seg - 1].body(t);
  const glassD = seg === 0 ? CARS[0].glass : morphs[seg - 1].glass(t);
  const color = interpolateColors(t, [0, 1], [CARS[from].color, CARS[to].color]);

  const enter = prog(frame, 0, 18, E.expoOut);
  const exit = prog(frame, EXIT, S8_DURATION - EXIT, E.expoIn);
  const bounce = Math.sin(t * Math.PI) * 30;

  const bgPrev = BGS[from];
  const bg = BGS[to];
  const ink = t > 0.5 ? INKS[to] : INKS[from];
  const wipe = seg === 0 ? 1 : t;

  const yFrom = String(CARS[from].year);
  const yTo = String(CARS[to].year);

  return (
    <AbsoluteFill style={{ backgroundColor: bgPrev, overflow: "hidden" }}>
      {/* Diagonal wipe into the next era's background */}
      <AbsoluteFill
        style={{
          backgroundColor: bg,
          clipPath: `polygon(0 0, ${wipe * 260}% 0, ${wipe * 260 - 60}% 100%, 0 100%)`,
        }}
      />

      {/* Rolling outline year */}
      <div style={{ position: "absolute", top: 260, width: 1080, display: "flex", justifyContent: "center", fontFamily: display, fontWeight: 900, fontSize: 210 }}>
        {[0, 1, 2, 3].map((i) => (
          <Digit key={i} from={Number(yFrom[i])} to={Number(yTo[i])} t={seg === 0 ? 1 : prog(within, i * 2, MORPH, E.expoInOut)} color={ink} />
        ))}
      </div>

      {/* Ground with streaming dashes */}
      <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
        <line x1={0} x2={1080} y1={1210} y2={1210} stroke={ink} strokeWidth={4} />
        {new Array(10).fill(0).map((_, i) => {
          const x = ((i * 160 - frame * (24 + exit * 60)) % 1600 + 1600) % 1600 - 260;
          return <line key={i} x1={x} x2={x + 90} y1={1250} y2={1250} stroke={ink} strokeWidth={6} opacity={0.5} />;
        })}
      </svg>

      {/* The morphing car */}
      <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
        <g
          transform={`translate(${mix(-1100, 60, enter) + exit * 1500} ${890 - bounce}) scale(${1.6 * (1 + exit * 0.3)} ${1.6 * (1 - exit * 0.08)})`}
        >
          <path d={bodyD} fill={color} />
          <path d={glassD} fill={C.ink} opacity={0.85} />
          {CARS[0].wheels.map((_, i) => {
            const wx = mix(CARS[from].wheels[i].x, CARS[to].wheels[i].x, t);
            const wr = mix(CARS[from].wheels[i].r, CARS[to].wheels[i].r, t);
            return (
              <g key={i} transform={`translate(${wx} ${200 - wr}) rotate(${frame * 14})`}>
                <circle r={wr} fill="#111" stroke={ink} strokeWidth={4} />
                <circle r={wr * 0.55} fill="none" stroke={color} strokeWidth={6} />
                {[0, 72, 144, 216, 288].map((a) => (
                  <line key={a} x1={0} y1={0} x2={wr * 0.5} y2={0} stroke={color} strokeWidth={5} transform={`rotate(${a})`} />
                ))}
              </g>
            );
          })}
        </g>
        {exit > 0
          ? new Array(9).fill(0).map((_, i) => (
              <line key={i} x1={0} x2={1080 * exit} y1={950 + i * 28} y2={950 + i * 28} stroke={ink} strokeWidth={3} opacity={0.4} />
            ))
          : null}
      </svg>

      {/* Name + note */}
      {CARS.map((c, i) => {
        const start = i === 0 ? 8 : i * SEG + 6;
        const out = i === CARS.length - 1 ? EXIT : (i + 1) * SEG - 2;
        if (frame < start - 2 || frame > out + 16) return null;
        return (
          <div key={i} style={{ position: "absolute", top: 1330, width: 1080, textAlign: "center" }}>
            <MaskLine start={start} out={out} dur={12}>
              <div style={{ fontFamily: display, fontWeight: 900, fontSize: 84, color: INKS[i] }}>{c.name}</div>
            </MaskLine>
            <MaskLine start={start + 4} out={out + 2} dur={12}>
              <div style={{ fontFamily: condensed, fontWeight: 600, fontSize: 48, letterSpacing: 6, color: C.red, marginTop: 14 }}>{NOTES[i]}</div>
            </MaskLine>
          </div>
        );
      })}

      {/* Era progress pips */}
      <div style={{ position: "absolute", top: 1700, width: 1080, display: "flex", justifyContent: "center", gap: 18 }}>
        {CARS.map((c, i) => (
          <div key={i} style={{ width: i === to ? 80 : 24, height: 12, borderRadius: 6, background: i <= to ? C.red : ink, opacity: i <= to ? 1 : 0.25 }} />
        ))}
      </div>
      <div style={{ position: "absolute", top: 1740, width: 1080, textAlign: "center", fontFamily: body, fontSize: 28, letterSpacing: 8, color: ink, opacity: 0.6 }}>
        ЭВОЛЮЦИЯ ФОРМЫ
      </div>
    </AbsoluteFill>
  );
};
