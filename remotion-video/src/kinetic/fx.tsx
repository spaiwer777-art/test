import "@fontsource-variable/unbounded";
import "@fontsource-variable/oswald";
import "@fontsource-variable/inter";
import "@fontsource-variable/jetbrains-mono";
import React from "react";
import { AbsoluteFill, Easing, interpolate, random, useCurrentFrame } from "remotion";

export const K = {
  red: "#E8202A",
  redHot: "#FF3B3B",
  black: "#0A0A0B",
  white: "#F2F1EE",
  grey: "#8A8A90",
};
export const heavy = "'Unbounded Variable', sans-serif";
export const cond = "'Oswald Variable', sans-serif";
export const sans = "'Inter Variable', sans-serif";
export const mono = "'JetBrains Mono Variable', monospace";

export const ease = {
  out: Easing.bezier(0.16, 1, 0.3, 1),
  in: Easing.bezier(0.7, 0, 0.84, 0),
  inOut: Easing.bezier(0.87, 0, 0.13, 1),
  back: Easing.bezier(0.34, 1.7, 0.64, 1),
};

export const p = (f: number, start: number, dur: number, e: (t: number) => number = ease.out) =>
  interpolate(f, [start, start + dur], [0, 1], { easing: e, extrapolateLeft: "clamp", extrapolateRight: "clamp" });

export type Bg = "black" | "red" | "white";

export const Background: React.FC<{ bg: Bg; glow?: boolean }> = ({ bg, glow = true }) => {
  const f = useCurrentFrame();
  const base = bg === "black" ? K.black : bg === "red" ? K.red : K.white;
  return (
    <AbsoluteFill style={{ background: base }}>
      {bg === "black" && glow ? (
        <>
          <AbsoluteFill style={{ background: `radial-gradient(ellipse at 50% 55%, rgba(232,32,42,${0.38 + Math.sin(f / 20) * 0.06}) 0%, rgba(232,32,42,0.06) 45%, transparent 70%)` }} />
          <Embers />
        </>
      ) : null}
      {bg === "white" ? <Scratches /> : null}
      {bg === "red" ? <AbsoluteFill style={{ background: "radial-gradient(ellipse at center, transparent 40%, rgba(0,0,0,0.35) 100%)" }} /> : null}
    </AbsoluteFill>
  );
};

const EMBERS = new Array(60).fill(0).map((_, i) => ({ x: random(`ex${i}`), y: random(`ey${i}`), s: 3 + random(`es${i}`) * 9, v: 0.4 + random(`ev${i}`) * 1.4 }));
export const Embers: React.FC = () => {
  const f = useCurrentFrame();
  return (
    <AbsoluteFill>
      {EMBERS.map((e, i) => (
        <div
          key={i}
          style={{
            position: "absolute",
            left: e.x * 1080 + Math.sin(f / 40 + i) * 20,
            top: ((e.y * 1920 - f * e.v) % 1920 + 1920) % 1920,
            width: e.s,
            height: e.s,
            borderRadius: "50%",
            background: K.redHot,
            boxShadow: `0 0 ${e.s * 3}px ${K.red}`,
            opacity: 0.25 + 0.5 * Math.abs(Math.sin(f / 25 + i)),
            filter: `blur(${e.s > 8 ? 2 : 0}px)`,
          }}
        />
      ))}
    </AbsoluteFill>
  );
};

const SCR = new Array(40).fill(0).map((_, i) => ({ x: random(`sx${i}`), y: random(`sy${i}`), a: random(`sa${i}`) * 180, l: 10 + random(`sl${i}`) * 40 }));
const Scratches: React.FC = () => {
  const f = useCurrentFrame();
  const seed = Math.floor(f / 4);
  return (
    <AbsoluteFill>
      {SCR.map((s, i) => (
        <div
          key={i}
          style={{
            position: "absolute",
            left: ((s.x + random(`sj${seed}${i}`) * 0.02) * 1080),
            top: s.y * 1920,
            width: s.l,
            height: 3,
            background: i % 5 === 0 ? K.red : "#222",
            opacity: 0.35,
            transform: `rotate(${s.a}deg)`,
          }}
        />
      ))}
    </AbsoluteFill>
  );
};

// A word that slams in: overscale + blur + a motion trail of ghost copies, then a tiny drift.
export const Slam: React.FC<{
  at: number;
  children: React.ReactNode;
  from?: "scale" | "left" | "right" | "up" | "down" | "zoom";
  dur?: number;
  ghosts?: number;
  style?: React.CSSProperties;
  out?: number;
}> = ({ at, children, from = "scale", dur = 8, ghosts = 4, style, out = 99999 }) => {
  const f = useCurrentFrame();
  if (f < at) return null;
  const pose = (t: number) => {
    const k = p(t, at, dur, ease.out);
    const q = p(t, out, 6, ease.in);
    let tx = 0;
    let ty = 0;
    let s = 1;
    switch (from) {
      case "scale":
        s = interpolate(k, [0, 1], [2.4, 1]);
        break;
      case "zoom":
        s = interpolate(k, [0, 1], [0.15, 1]);
        break;
      case "left":
        tx = (1 - k) * -900;
        break;
      case "right":
        tx = (1 - k) * 900;
        break;
      case "up":
        ty = (1 - k) * -700;
        break;
      case "down":
        ty = (1 - k) * 700;
        break;
    }
    s *= 1 + (t - at) * 0.0012;
    tx += q * 1400;
    return { k, tx, ty, s, q };
  };
  const now = pose(f);
  const moving = now.k < 0.98 || now.q > 0;
  const layer = (t: number, op: number, key: number) => {
    const ps = pose(t);
    return (
      <div
        key={key}
        style={{
          position: "absolute",
          inset: 0,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          transform: `translate(${ps.tx}px, ${ps.ty}px) scale(${ps.s})`,
          opacity: op,
          filter: key === 0 ? `blur(${(1 - ps.k) * 10 + ps.q * 14}px)` : "blur(6px)",
        }}
      >
        {children}
      </div>
    );
  };
  return (
    <div style={{ position: "relative", width: "100%", height: "100%", ...style }}>
      {moving ? new Array(ghosts).fill(0).map((_, g) => layer(f - (g + 1) * 1.2, 0.28 - g * 0.06, g + 1)) : null}
      {layer(f, 1, 0)}
    </div>
  );
};

// Positioned block helper for stacking words.
export const Box: React.FC<{ top: number; height: number; children: React.ReactNode; left?: number; right?: number }> = ({ top, height, children, left = 0, right = 0 }) => (
  <div style={{ position: "absolute", top, left, right, height }}>{children}</div>
);

export const Word: React.FC<{ text: string; size: number; color?: string; font?: string; weight?: number; dot?: boolean; spacing?: number; stroke?: string }> = ({
  text,
  size,
  color = K.white,
  font = heavy,
  weight = 900,
  dot = false,
  spacing = -2,
  stroke,
}) => (
  <div
    style={{
      fontFamily: font,
      fontWeight: weight,
      fontSize: size,
      color: stroke ? "transparent" : color,
      WebkitTextStroke: stroke ? `4px ${stroke}` : undefined,
      letterSpacing: spacing,
      lineHeight: 1,
      whiteSpace: "nowrap",
      textShadow: color === K.white && !stroke ? `0 0 40px rgba(232,32,42,0.45)` : undefined,
      display: "flex",
      alignItems: "baseline",
    }}
  >
    {text}
    {dot ? <span style={{ display: "inline-block", width: size * 0.2, height: size * 0.2, borderRadius: "50%", background: K.red, marginLeft: size * 0.06 }} /> : null}
  </div>
);

// Camera shake + flash on given local frames.
export const useHits = (hits: number[], amp = 26) => {
  const f = useCurrentFrame();
  let x = 0;
  let y = 0;
  let r = 0;
  let flash = 0;
  for (const h of hits) {
    const t = f - h;
    if (t >= 0 && t < 14) {
      const k = Math.pow(1 - t / 14, 2) * amp;
      x += (random(`hx${h}-${t}`) - 0.5) * k;
      y += (random(`hy${h}-${t}`) - 0.5) * k;
      r += (random(`hr${h}-${t}`) - 0.5) * k * 0.05;
    }
    if (t >= 0 && t < 4) flash = Math.max(flash, 1 - t / 4);
  }
  return { transform: `translate(${x}px, ${y}px) rotate(${r}deg)`, flash };
};

export const Flash: React.FC<{ v: number; color?: string }> = ({ v, color = "#fff" }) =>
  v > 0 ? <AbsoluteFill style={{ background: color, opacity: v * 0.5, mixBlendMode: "screen" }} /> : null;

// Horizontal light streak (lens line) used for reveals.
export const Streak: React.FC<{ at: number; y?: number; dur?: number }> = ({ at, y = 960, dur = 22 }) => {
  const f = useCurrentFrame();
  const k = p(f, at, dur, ease.out);
  if (f < at) return null;
  const fade = 1 - p(f, at + dur, 20);
  return (
    <>
      <div style={{ position: "absolute", left: 540 - 700 * k, width: 1400 * k, top: y - 3, height: 6, background: "#fff", boxShadow: `0 0 30px #fff, 0 0 80px ${K.red}, 0 0 160px ${K.red}`, opacity: fade }} />
      <div style={{ position: "absolute", left: 540 - 60, top: y - 60, width: 120, height: 120, borderRadius: "50%", background: `radial-gradient(circle, #fff 0%, ${K.redHot} 30%, transparent 70%)`, opacity: fade * k }} />
    </>
  );
};
