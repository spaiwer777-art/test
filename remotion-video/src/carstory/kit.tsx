import "@fontsource-variable/unbounded";
import "@fontsource-variable/oswald";
import "@fontsource-variable/inter";
import React from "react";
import { Easing, interpolate, random, useCurrentFrame } from "remotion";

export const C = {
  ink: "#0b0b0d",
  paper: "#f1ede4",
  red: "#ff3b1f",
  yellow: "#ffd23f",
  blue: "#2f6bff",
  grey: "#8a877f",
};

export const display = "'Unbounded Variable', sans-serif";
export const condensed = "'Oswald Variable', sans-serif";
export const body = "'Inter Variable', sans-serif";

export const E = {
  expoOut: Easing.bezier(0.16, 1, 0.3, 1),
  expoIn: Easing.bezier(0.7, 0, 0.84, 0),
  expoInOut: Easing.bezier(0.87, 0, 0.13, 1),
  backOut: Easing.bezier(0.34, 1.56, 0.64, 1),
  quintOut: Easing.bezier(0.22, 1, 0.36, 1),
};

// Clamped, eased progress of an animation that starts at `start` and lasts `dur` frames.
export const prog = (
  frame: number,
  start: number,
  dur: number,
  ease: (t: number) => number = E.expoOut,
) =>
  !Number.isFinite(start)
    ? 0
    : interpolate(frame, [start, start + dur], [0, 1], {
        easing: ease,
        extrapolateLeft: "clamp",
        extrapolateRight: "clamp",
      });

export const mix = (a: number, b: number, t: number) => a + (b - a) * t;

// Classic AE line reveal: text slides up from behind a mask.
export const MaskLine: React.FC<{
  children: React.ReactNode;
  start: number;
  out?: number;
  dur?: number;
  style?: React.CSSProperties;
}> = ({ children, start, out = Infinity, dur = 18, style }) => {
  const frame = useCurrentFrame();
  const pIn = prog(frame, start, dur);
  const pOut = prog(frame, out, 14, E.expoIn);
  return (
    <div
      style={{
        overflow: "hidden",
        lineHeight: 1,
        paddingBottom: "0.08em",
        ...style,
      }}
    >
      <div
        style={{
          transform: `translateY(${(1 - pIn) * 110 - pOut * 110}%) skewY(${(1 - pIn) * 6}deg)`,
        }}
      >
        {children}
      </div>
    </div>
  );
};

type LetterMode = "rise" | "drop" | "scale" | "spin" | "scatter";

// Per-letter kinetic type with staggered timing.
export const Letters: React.FC<{
  text: string;
  start: number;
  out?: number;
  stagger?: number;
  mode?: LetterMode;
  style?: React.CSSProperties;
  letterStyle?: (i: number) => React.CSSProperties;
}> = ({
  text,
  start,
  out = Infinity,
  stagger = 2,
  mode = "rise",
  style,
  letterStyle,
}) => {
  const frame = useCurrentFrame();
  const chars = Array.from(text);
  return (
    <div style={{ display: "flex", whiteSpace: "pre", ...style }}>
      {chars.map((ch, i) => {
        const p = prog(
          frame,
          start + i * stagger,
          16,
          mode === "scale" ? E.backOut : E.expoOut,
        );
        const q = prog(frame, out + i * stagger * 0.6, 12, E.expoIn);
        const r = random(`l${text}${i}`) - 0.5;
        let transform = "";
        switch (mode) {
          case "rise":
            transform = `translateY(${(1 - p) * 120}%) rotate(${(1 - p) * 12}deg)`;
            break;
          case "drop":
            transform = `translateY(${(1 - p) * -140}%)`;
            break;
          case "scale":
            transform = `scale(${p})`;
            break;
          case "spin":
            transform = `perspective(600px) rotateY(${(1 - p) * 90}deg)`;
            break;
          case "scatter":
            transform = `translate(${(1 - p) * r * 900}px, ${(1 - p) * (random(`y${text}${i}`) - 0.5) * 900}px) rotate(${(1 - p) * r * 180}deg)`;
            break;
        }
        transform += ` translateY(${q * -130}%)`;
        return (
          <span
            key={i}
            style={{
              display: "inline-block",
              opacity: Math.min(1, p * 2) * (1 - q),
              transform,
              ...(letterStyle ? letterStyle(i) : {}),
            }}
          >
            {ch}
          </span>
        );
      })}
    </div>
  );
};

const groupThousands = (n: number) =>
  Math.round(n)
    .toString()
    .replace(/\B(?=(\d{3})+(?!\d))/g, " ");

export const Count: React.FC<{
  from?: number;
  to: number;
  start: number;
  dur: number;
  decimals?: number;
  style?: React.CSSProperties;
  suffix?: string;
}> = ({ from = 0, to, start, dur, decimals = 0, style, suffix = "" }) => {
  const frame = useCurrentFrame();
  const v = mix(from, to, prog(frame, start, dur, E.quintOut));
  const txt =
    decimals > 0 ? v.toFixed(decimals).replace(".", ",") : groupThousands(v);
  return (
    <span style={{ fontVariantNumeric: "tabular-nums", ...style }}>
      {txt}
      {suffix}
    </span>
  );
};

// Animated stroke ("trim paths").
export const Draw: React.FC<{
  d: string;
  start: number;
  dur: number;
  stroke?: string;
  width?: number;
  fill?: string;
  fillStart?: number;
  ease?: (t: number) => number;
}> = ({
  d,
  start,
  dur,
  stroke = C.paper,
  width = 6,
  fill = "none",
  fillStart,
  ease = E.expoInOut,
}) => {
  const frame = useCurrentFrame();
  const p = prog(frame, start, dur, ease);
  const f = fillStart === undefined ? 0 : prog(frame, fillStart, 12);
  return (
    <path
      d={d}
      pathLength={1}
      strokeDasharray="1 1"
      strokeDashoffset={1 - p}
      stroke={stroke}
      strokeWidth={width}
      strokeLinecap="round"
      strokeLinejoin="round"
      fill={fill}
      fillOpacity={f}
      opacity={p > 0 ? 1 : 0}
    />
  );
};

// Decaying camera shake triggered at given frames.
export const useShake = (hits: number[], strength = 22) => {
  const frame = useCurrentFrame();
  let x = 0;
  let y = 0;
  let r = 0;
  hits.forEach((h) => {
    const t = frame - h;
    if (t >= 0 && t < 14) {
      const k = Math.pow(1 - t / 14, 2) * strength;
      x += (random(`sx${h}-${t}`) - 0.5) * k;
      y += (random(`sy${h}-${t}`) - 0.5) * k;
      r += (random(`sr${h}-${t}`) - 0.5) * k * 0.05;
    }
  });
  return `translate(${x}px, ${y}px) rotate(${r}deg)`;
};

export const Grain: React.FC<{ opacity?: number }> = ({ opacity = 0.12 }) => {
  const frame = useCurrentFrame();
  return (
    <svg
      width="1080"
      height="1920"
      style={{
        position: "absolute",
        inset: 0,
        opacity,
        mixBlendMode: "overlay",
        pointerEvents: "none",
      }}
    >
      <filter id="kit-grain">
        <feTurbulence
          type="fractalNoise"
          baseFrequency="0.85"
          numOctaves="2"
          seed={frame % 12}
        />
      </filter>
      <rect width="1080" height="1920" filter="url(#kit-grain)" />
    </svg>
  );
};

// Spinning spoked wheel, drawn in SVG units around (0,0).
export const WheelGlyph: React.FC<{
  r: number;
  angle: number;
  spokes?: number;
  stroke?: string;
  width?: number;
  hub?: boolean;
  draw?: number;
}> = ({
  r,
  angle,
  spokes = 8,
  stroke = C.paper,
  width = 6,
  hub = true,
  draw = 1,
}) => (
  <g transform={`rotate(${angle})`}>
    <circle
      r={r}
      fill="none"
      stroke={stroke}
      strokeWidth={width}
      pathLength={1}
      strokeDasharray="1 1"
      strokeDashoffset={1 - draw}
    />
    <circle
      r={r * 0.82}
      fill="none"
      stroke={stroke}
      strokeWidth={width * 0.4}
      opacity={draw}
    />
    {new Array(spokes).fill(0).map((_, i) => {
      const a = (i / spokes) * Math.PI * 2;
      const k = Math.min(1, Math.max(0, draw * spokes - i));
      return (
        <line
          key={i}
          x1={0}
          y1={0}
          x2={Math.cos(a) * r * 0.82 * k}
          y2={Math.sin(a) * r * 0.82 * k}
          stroke={stroke}
          strokeWidth={width * 0.6}
          strokeLinecap="round"
        />
      );
    })}
    {hub ? <circle r={r * 0.12} fill={stroke} opacity={draw} /> : null}
  </g>
);
