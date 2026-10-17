import React from "react";
import { AbsoluteFill, interpolate, random, useCurrentFrame } from "remotion";
import { A, accentAt, body, E, HITS, mono, prog, SCENE_STARTS } from "./theme";

// Year shown by the odometer: [global frame, year]. -1 means "????".
const YEARS: [number, number][] = [
  [150, 1943], [210, 1950], [300, 1956], [390, 1958], [480, 1966], [600, 1974], [750, 1986],
  [870, 1997], [990, 2012], [1110, 2014], [1200, 2016], [1290, 2017], [1480, 2018], [1500, 2019],
  [1525, 2020], [1620, 2022], [1740, 2023], [1830, 2024], [1920, 2025], [2010, 2026], [2160, -1],
];

const yearAt = (f: number) => {
  let cur = YEARS[0][1];
  let prev = cur;
  let since = 999;
  for (let i = 0; i < YEARS.length; i++) {
    if (f >= YEARS[i][0]) {
      prev = i > 0 ? YEARS[i - 1][1] : YEARS[i][1];
      cur = YEARS[i][1];
      since = f - YEARS[i][0];
    }
  }
  return { cur, prev, since };
};

const DigitRoll: React.FC<{ from: string; to: string; t: number; size: number; color: string }> = ({ from, to, t, size, color }) => {
  if (from === to || t >= 1) return <span style={{ display: "inline-block", width: size * 0.6, textAlign: "center" }}>{to}</span>;
  return (
    <span style={{ display: "inline-block", width: size * 0.6, height: size * 1.1, overflow: "hidden", verticalAlign: "top", position: "relative" }}>
      <span style={{ position: "absolute", left: 0, right: 0, textAlign: "center", transform: `translateY(${-t * 110}%)`, color }}>{from}</span>
      <span style={{ position: "absolute", left: 0, right: 0, textAlign: "center", transform: `translateY(${(1 - t) * 110}%)`, color }}>{to}</span>
    </span>
  );
};

export const Odometer: React.FC<{ size?: number; color?: string }> = ({ size = 64, color }) => {
  const f = useCurrentFrame();
  const { cur, prev, since } = yearAt(f);
  const c = color ?? accentAt(f);
  const fmt = (y: number) => (y < 0 ? "????" : String(y).padStart(4, "0"));
  const a = fmt(prev);
  const b = fmt(cur);
  return (
    <div style={{ fontFamily: mono, fontWeight: 700, fontSize: size, color: c, lineHeight: 1.1, display: "flex", textShadow: `0 0 24px ${c}66` }}>
      {[0, 1, 2, 3].map((i) => (
        <DigitRoll key={i} from={a[i]} to={b[i]} t={prog(since, i * 2, 14, E.expoInOut)} size={size} color={c} />
      ))}
    </div>
  );
};

export const GridBg: React.FC = () => {
  const f = useCurrentFrame();
  const c = accentAt(f);
  const off = (f * 0.6) % 40;
  // the grid pulses on every beat (15 frames at 120 BPM)
  const beat = Math.exp(-(f % 15) / 4);
  return (
    <AbsoluteFill style={{ opacity: 0.75 + beat * 0.25 }}>
      <AbsoluteFill
        style={{
          backgroundImage: `linear-gradient(${c}10 2px, transparent 2px), linear-gradient(90deg, ${c}10 2px, transparent 2px)`,
          backgroundSize: "40px 40px",
          backgroundPosition: `${off}px ${off * 0.5}px`,
        }}
      />
      <AbsoluteFill style={{ background: `radial-gradient(ellipse at 50% 45%, ${c}1c 0%, transparent 60%)` }} />
      <AbsoluteFill style={{ background: "radial-gradient(ellipse at center, transparent 55%, rgba(0,0,0,0.75) 100%)" }} />
    </AbsoluteFill>
  );
};

export const Hud: React.FC = () => {
  const f = useCurrentFrame();
  const c = accentAt(f);
  const lap = SCENE_STARTS.filter((s) => f >= s).length;
  const fadeOut = 1 - prog(f, 2205, 20);
  const on = prog(f, 150, 20);
  const corner = (style: React.CSSProperties) => (
    <div style={{ position: "absolute", width: 56, height: 56, borderColor: c, borderStyle: "solid", borderWidth: 0, opacity: 0.8, ...style }} />
  );
  const secs = f / 30;
  return (
    <AbsoluteFill style={{ opacity: on * fadeOut, pointerEvents: "none" }}>
      {corner({ left: 40, top: 40, borderLeftWidth: 4, borderTopWidth: 4 })}
      {corner({ right: 40, top: 40, borderRightWidth: 4, borderTopWidth: 4 })}
      {corner({ left: 40, bottom: 40, borderLeftWidth: 4, borderBottomWidth: 4 })}
      {corner({ right: 40, bottom: 40, borderRightWidth: 4, borderBottomWidth: 4 })}
      <div style={{ position: "absolute", left: 84, top: 72 }}>
        <Odometer size={60} />
      </div>
      <div style={{ position: "absolute", right: 84, top: 80, textAlign: "right", fontFamily: mono, color: c }}>
        <div style={{ fontSize: 30, fontWeight: 700, letterSpacing: 4 }}>LAP {String(lap).padStart(2, "0")}/10</div>
        <div style={{ fontSize: 22, color: A.grey, marginTop: 6 }}>
          {String(Math.floor(secs / 60)).padStart(2, "0")}:{(secs % 60).toFixed(2).padStart(5, "0")}
        </div>
      </div>
      <div style={{ position: "absolute", left: 84, bottom: 78, fontFamily: body, fontSize: 22, letterSpacing: 6, color: A.grey }}>
        ГОНКА НЕЙРОСЕТЕЙ
      </div>
      <div style={{ position: "absolute", right: 84, bottom: 72, display: "flex", gap: 6 }}>
        {SCENE_STARTS.map((s, i) => (
          <div key={i} style={{ width: 14, height: 14, background: f >= s ? c : "transparent", border: `2px solid ${c}`, opacity: f >= s ? 1 : 0.4 }} />
        ))}
      </div>
    </AbsoluteFill>
  );
};

// Flash + chromatic fringe on every big hit.
export const HitFx: React.FC = () => {
  const f = useCurrentFrame();
  let flash = 0;
  let chroma = 0;
  for (const h of HITS) {
    const t = f - h;
    if (t >= 0 && t < 3) flash = Math.max(flash, 1 - t / 3);
    if (t >= 0 && t < 5) chroma = Math.max(chroma, 1 - t / 5);
  }
  return (
    <AbsoluteFill style={{ pointerEvents: "none" }}>
      <AbsoluteFill style={{ background: "#fff", opacity: flash * 0.35, mixBlendMode: "screen" }} />
      <AbsoluteFill
        style={{
          boxShadow: `inset ${chroma * 30}px 0 60px rgba(255,0,60,${chroma * 0.5}), inset ${-chroma * 30}px 0 60px rgba(0,229,255,${chroma * 0.5})`,
        }}
      />
    </AbsoluteFill>
  );
};

export const useGlobalShake = () => {
  const f = useCurrentFrame();
  let x = 0;
  let y = 0;
  let r = 0;
  for (const h of HITS) {
    const t = f - h;
    if (t >= 0 && t < 10) {
      const k = Math.pow(1 - t / 10, 2) * 18;
      x += (random(`gx${h}-${t}`) - 0.5) * k;
      y += (random(`gy${h}-${t}`) - 0.5) * k;
      r += (random(`gr${h}-${t}`) - 0.5) * k * 0.04;
    }
  }
  const breathe = interpolate(f % 300, [0, 300], [1, 1.02]);
  return `translate(${x}px, ${y}px) rotate(${r}deg) scale(${breathe})`;
};

export const Caption: React.FC<{ year?: string; title: string; start: number; out?: number; color?: string; accent: string; top?: number }> = ({
  year,
  title,
  start,
  out = 9999,
  color,
  accent,
  top = 220,
}) => {
  const f = useCurrentFrame();
  const pIn = prog(f, start, 16);
  const pOut = prog(f, out, 12, E.expoIn);
  const c = color ?? A.text;
  const line = (child: React.ReactNode, delay: number) => (
    <div style={{ overflow: "hidden", paddingBottom: 6 }}>
      <div style={{ transform: `translateY(${(1 - prog(f, start + delay, 16)) * 110 - pOut * 110}%) skewY(${(1 - prog(f, start + delay, 16)) * 6}deg)` }}>
        {child}
      </div>
    </div>
  );
  return (
    <div style={{ position: "absolute", left: 80, right: 80, top, opacity: pIn > 0 ? 1 : 0 }}>
      {year ? line(<div style={{ fontFamily: mono, fontWeight: 700, fontSize: 40, color: accent, letterSpacing: 6 }}>{year}</div>, 0) : null}
      {line(<div style={{ fontFamily: "'Unbounded Variable', sans-serif", fontWeight: 900, fontSize: 62, color: c, lineHeight: 1.1 }}>{title}</div>, 3)}
    </div>
  );
};
