import React from "react";
import { AbsoluteFill, interpolate, random, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { fontFamily } from "../theme";
import { ERAS, INTRO, LAST_ARRIVAL, eraArrival, eraExit, eraPosition, formatYear, lerp, morphProgress } from "./eras";

const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;

const FlyTitle: React.FC<{ text: string; start: number; exit: number; size: number; color?: string; glow: string }> = ({
  text,
  start,
  exit,
  size,
  color = "#ffffff",
  glow,
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return (
    <div style={{ whiteSpace: "nowrap", perspective: 700 }}>
      {Array.from(text).map((ch, i) => {
        const p = spring({ frame: frame - start - i * 2, fps, config: { damping: 13, mass: 0.6 } });
        const q = interpolate(frame - exit - i * 1.2, [0, 10], [0, 1], clamp);
        return (
          <span
            key={i}
            style={{
              display: "inline-block",
              fontFamily,
              fontSize: size,
              fontWeight: 900,
              color,
              textShadow: `0 0 30px ${glow}`,
              opacity: p * (1 - q),
              filter: `blur(${q * 10}px)`,
              transform: `rotateX(${(1 - p) * -100 + q * 80}deg) translateY(${(1 - p) * 60 - q * 80}px) scale(${0.6 + 0.4 * p})`,
            }}
          >
            {ch === " " ? " " : ch}
          </span>
        );
      })}
    </div>
  );
};

const textExit = (k: number) => (k === ERAS.length - 1 ? LAST_ARRIVAL + 90 : eraExit(k));

export const Overlay: React.FC = () => {
  const frame = useCurrentFrame();
  const e = eraPosition(frame);
  const m = morphProgress(frame);
  const swoop = m >= 0 ? Math.sin(Math.PI * m) : 0;
  const nearest = ERAS[Math.round(e)];
  const accent = nearest.accent;

  const i = Math.min(Math.floor(e), ERAS.length - 2);
  const year = Math.round(lerp(ERAS[i].year, ERAS[i + 1].year, e - i));
  const yearIn = interpolate(frame, [INTRO - 10, INTRO + 10], [0, 1], clamp);
  const yearOut = interpolate(frame, [LAST_ARRIVAL + 90, LAST_ARRIVAL + 105], [1, 0], clamp);
  const shift = swoop * 14;

  const sepia = interpolate(e, [0, 1, 2, 3, 4], [0.9, 0.75, 0.55, 0.3, 0], clamp);
  const outroStart = LAST_ARRIVAL + 100;

  return (
    <AbsoluteFill>
      {/* Old-film look that fades as history moves forward */}
      <AbsoluteFill style={{ opacity: sepia * 0.35, mixBlendMode: "overlay" }}>
        <svg width="100%" height="100%">
          <filter id="grain">
            <feTurbulence type="fractalNoise" baseFrequency="0.9" numOctaves="2" seed={frame % 9} />
          </filter>
          <rect width="100%" height="100%" filter="url(#grain)" />
        </svg>
      </AbsoluteFill>
      {[0, 1, 2].map((k) => {
        const show = random(`scr-on-${frame}-${k}`) > 0.55;
        return show ? (
          <div
            key={k}
            style={{
              position: "absolute",
              top: 0,
              bottom: 0,
              left: random(`scr-x-${frame}-${k}`) * 1080,
              width: 2,
              background: "rgba(255,240,210,0.5)",
              opacity: sepia,
            }}
          />
        ) : null;
      })}

      <AbsoluteFill style={{ background: "radial-gradient(ellipse at center, transparent 50%, rgba(0,0,0,0.7) 100%)" }} />

      {/* Intro title */}
      <AbsoluteFill style={{ alignItems: "center", top: 230 }}>
        <FlyTitle text="ИСТОРИЯ" start={0} exit={INTRO - 8} size={110} glow={ERAS[0].accent} />
        <div style={{ height: 10 }} />
        <FlyTitle text="АВТОМОБИЛЯ" start={8} exit={INTRO - 4} size={96} color={ERAS[0].accent} glow={ERAS[0].accent} />
      </AbsoluteFill>

      {/* Year counter with chromatic split during transformations */}
      <AbsoluteFill style={{ alignItems: "center", top: 210, opacity: yearIn * yearOut }}>
        <div
          style={{
            fontFamily,
            fontSize: year < 0 ? 120 : 190,
            fontWeight: 900,
            color: "#ffffff",
            letterSpacing: -4,
            fontVariantNumeric: "tabular-nums",
            textShadow: `${shift}px 0 #ff0050, ${-shift}px 0 #00e5ff, 0 0 40px ${accent}`,
            transform: `scale(${1 + swoop * 0.12}) skewX(${swoop * -8}deg)`,
          }}
        >
          {formatYear(year)}
        </div>
      </AbsoluteFill>

      {/* Era title and fact */}
      {ERAS.map((era, k) => {
        const start = eraArrival(k);
        const exit = textExit(k);
        if (frame < start - 5 || frame > exit + 30) return null;
        const factIn = interpolate(frame, [start + 14, start + 30], [0, 1], clamp);
        const factOut = interpolate(frame, [exit - 6, exit + 4], [1, 0], clamp);
        return (
          <AbsoluteFill key={k} style={{ alignItems: "center", top: 1430 }}>
            <FlyTitle text={era.title} start={start} exit={exit} size={era.title.length > 11 ? 88 : 100} glow={era.accent} />
            <div
              style={{
                marginTop: 24,
                width: 880,
                textAlign: "center",
                fontFamily,
                fontSize: 42,
                fontWeight: 600,
                lineHeight: 1.3,
                color: "rgba(235,240,255,0.85)",
                opacity: factIn * factOut,
                transform: `translateY(${(1 - factIn) * 30}px)`,
              }}
            >
              {era.fact}
            </div>
          </AbsoluteFill>
        );
      })}

      {/* Outro */}
      <AbsoluteFill style={{ alignItems: "center", top: 1400 }}>
        <FlyTitle text="5500 лет" start={outroStart} exit={9999} size={120} color={accent} glow={accent} />
        <div style={{ height: 12 }} />
        <FlyTitle text="от колеса до электрокара" start={outroStart + 14} exit={9999} size={52} glow={accent} />
      </AbsoluteFill>

      {/* Timeline */}
      <div style={{ position: "absolute", left: 110, right: 110, top: 1810, height: 40, opacity: yearIn }}>
        <div style={{ position: "absolute", top: 18, left: 0, right: 0, height: 4, background: "rgba(255,255,255,0.18)", borderRadius: 2 }} />
        <div
          style={{
            position: "absolute",
            top: 18,
            left: 0,
            width: `${(e / (ERAS.length - 1)) * 100}%`,
            height: 4,
            background: accent,
            boxShadow: `0 0 18px ${accent}`,
            borderRadius: 2,
          }}
        />
        {ERAS.map((era, k) => {
          const on = e >= k - 0.01;
          const active = Math.round(e) === k;
          return (
            <div
              key={k}
              style={{
                position: "absolute",
                top: 20,
                left: `${(k / (ERAS.length - 1)) * 100}%`,
                width: active ? 30 : 18,
                height: active ? 30 : 18,
                borderRadius: "50%",
                transform: "translate(-50%, -50%)",
                background: on ? accent : "#2a2a3a",
                boxShadow: active ? `0 0 24px ${accent}` : "none",
              }}
            />
          );
        })}
      </div>

      {/* Flash at the peak of each transformation */}
      <AbsoluteFill style={{ background: "#ffffff", opacity: Math.pow(swoop, 6) * 0.55 }} />
    </AbsoluteFill>
  );
};

export const sepiaAt = (frame: number) =>
  interpolate(eraPosition(frame), [0, 1, 2, 3, 4], [0.9, 0.75, 0.55, 0.3, 0], clamp);
