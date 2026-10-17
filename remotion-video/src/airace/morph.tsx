import React, { useMemo } from "react";
import { interpolate as flubber } from "flubber";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { E, prog } from "./theme";

// --- shape library (centred on 0,0, radius ≈ 1)
const polar = (n: number, r: (i: number) => number, rot = -Math.PI / 2) =>
  "M" +
  new Array(n)
    .fill(0)
    .map((_, i) => {
      const a = rot + (i / n) * Math.PI * 2;
      return `${(Math.cos(a) * r(i)).toFixed(4)},${(Math.sin(a) * r(i)).toFixed(4)}`;
    })
    .join(" L") +
  " Z";

export const SHAPE = {
  circle: polar(64, () => 1),
  hexagon: polar(6, () => 1),
  square: polar(4, () => 1.25, -Math.PI / 4),
  snowflake: polar(12, (i) => (i % 2 ? 0.35 : 1)),
  diamond: polar(4, () => 1.15),
  star8: polar(16, (i) => (i % 2 ? 0.45 : 1)),
  triangle: polar(3, () => 1.2),
};

// Cover → morph → reveal: a shape grows from the centre until it fills the screen,
// morphs into the next era's shape while covering, then shrinks away.
export const MorphWipe: React.FC<{ from: string; to: string; colorA: string; colorB: string; dur?: number }> = ({ from, to, colorA, colorB, dur = 30 }) => {
  const f = useCurrentFrame();
  const half = dur / 2;
  const grow = prog(f, 0, half, E.expoIn);
  const shrink = prog(f, half, half, E.expoOut);
  const morph = prog(f, half - 5, 10, E.expoInOut);
  const interp = useMemo(() => flubber(from, to, { maxSegmentLength: 0.05 }), [from, to]);
  const d = morph <= 0 ? from : morph >= 1 ? to : interp(morph);
  const scale = interpolate(grow, [0, 1], [0, 1500]) * (1 - shrink);
  const rot = (grow - shrink) * 90;
  if (scale <= 0.5) return null;
  const fill = morph < 0.5 ? colorA : colorB;
  return (
    <AbsoluteFill style={{ pointerEvents: "none" }}>
      <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
        <g transform={`translate(540 960) rotate(${rot}) scale(${scale})`}>
          <path d={d} fill={fill} />
          <path d={d} fill="none" stroke="#ffffff" strokeWidth={3 / scale} opacity={0.6} />
        </g>
        {/* echo outlines for a trailing, energetic look */}
        {[0.82, 0.64].map((k, i) => (
          <g key={i} transform={`translate(540 960) rotate(${rot * (1.3 + i * 0.3)}) scale(${scale * k})`}>
            <path d={d} fill="none" stroke={fill} strokeWidth={6 / (scale * k)} opacity={0.5} />
          </g>
        ))}
      </svg>
    </AbsoluteFill>
  );
};

// Per-scene camera: punch-in on entry, slow push and roll, small punch-out on exit.
export const SceneCam: React.FC<{ dur: number; children: React.ReactNode; roll?: number }> = ({ dur, children, roll = 1 }) => {
  const f = useCurrentFrame();
  const enter = prog(f, 0, 10, E.expoOut);
  const exit = prog(f, dur - 6, 6, E.expoIn);
  const push = interpolate(f, [0, dur], [1, 1.05]);
  const scale = push * interpolate(enter, [0, 1], [1.1, 1]) * (1 + exit * 0.05);
  const rot = interpolate(f, [0, dur], [-0.6, 0.6]) * roll;
  const blur = (1 - enter) * 6 + exit * 5;
  return (
    <AbsoluteFill style={{ transform: `scale(${scale}) rotate(${rot}deg)`, filter: blur > 0.2 ? `blur(${blur}px)` : undefined }}>{children}</AbsoluteFill>
  );
};
