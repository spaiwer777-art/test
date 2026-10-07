import React from "react";
import { AbsoluteFill, random, useCurrentFrame } from "remotion";
import { colors } from "./theme";

const PARTICLES = new Array(70).fill(0).map((_, i) => ({
  x: random(`x${i}`) * 1080,
  y: random(`y${i}`) * 1920,
  size: 2 + random(`s${i}`) * 5,
  speed: 0.4 + random(`v${i}`) * 1.6,
  phase: random(`p${i}`) * Math.PI * 2,
}));

const Orb: React.FC<{ color: string; x: number; y: number; size: number }> = ({ color, x, y, size }) => (
  <div
    style={{
      position: "absolute",
      left: x - size / 2,
      top: y - size / 2,
      width: size,
      height: size,
      borderRadius: "50%",
      background: `radial-gradient(circle, ${color} 0%, transparent 65%)`,
      opacity: 0.55,
      filter: "blur(40px)",
    }}
  />
);

export const Background: React.FC = () => {
  const frame = useCurrentFrame();

  return (
    <AbsoluteFill style={{ backgroundColor: colors.bg, overflow: "hidden" }}>
      <Orb color={colors.blue} x={250 + Math.sin(frame / 60) * 180} y={500 + Math.cos(frame / 70) * 200} size={1000} />
      <Orb color={colors.violet} x={850 + Math.cos(frame / 55) * 160} y={1300 + Math.sin(frame / 65) * 220} size={1100} />
      <Orb color={colors.pink} x={540 + Math.sin(frame / 80) * 300} y={1700} size={700} />

      {/* Perspective grid floor */}
      <AbsoluteFill style={{ perspective: 900, perspectiveOrigin: "50% 30%" }}>
        <div
          style={{
            position: "absolute",
            left: -1000,
            right: -1000,
            bottom: -400,
            height: 1600,
            transform: "rotateX(72deg)",
            transformOrigin: "50% 100%",
            backgroundImage: `linear-gradient(${colors.cyan}55 2px, transparent 2px), linear-gradient(90deg, ${colors.cyan}55 2px, transparent 2px)`,
            backgroundSize: "120px 120px",
            backgroundPosition: `0 ${(frame * 4) % 120}px`,
            maskImage: "linear-gradient(to top, black 10%, transparent 80%)",
            WebkitMaskImage: "linear-gradient(to top, black 10%, transparent 80%)",
          }}
        />
      </AbsoluteFill>

      {PARTICLES.map((p, i) => {
        const y = (p.y - frame * p.speed * 3 + 1920 * 4) % 1920;
        const twinkle = 0.35 + 0.65 * Math.abs(Math.sin(frame / 20 + p.phase));
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              left: p.x + Math.sin(frame / 30 + p.phase) * 12,
              top: y,
              width: p.size,
              height: p.size,
              borderRadius: "50%",
              background: i % 3 === 0 ? colors.pink : colors.cyan,
              boxShadow: `0 0 ${p.size * 4}px ${i % 3 === 0 ? colors.pink : colors.cyan}`,
              opacity: twinkle,
            }}
          />
        );
      })}

      <AbsoluteFill
        style={{ background: "radial-gradient(ellipse at center, transparent 45%, rgba(0,0,0,0.75) 100%)" }}
      />
    </AbsoluteFill>
  );
};
