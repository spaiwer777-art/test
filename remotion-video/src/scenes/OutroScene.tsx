import React from "react";
import { AbsoluteFill, interpolate, random, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { Background } from "../Background";
import { Camera } from "../Camera";
import { colors, fontFamily, gradientText } from "../theme";

const BURST = new Array(36).fill(0).map((_, i) => ({
  angle: (i / 36) * Math.PI * 2 + random(`a${i}`) * 0.2,
  dist: 380 + random(`d${i}`) * 380,
  size: 8 + random(`z${i}`) * 14,
  color: [colors.cyan, colors.violet, colors.pink, colors.blue][i % 4],
}));

export const OutroScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const title = spring({ frame, fps, config: { damping: 10, mass: 0.9 } });
  const button = spring({ frame: frame - 26, fps, config: { damping: 12 } });
  const burst = interpolate(frame, [0, 45], [0, 1], { extrapolateRight: "clamp" });
  const glow = 0.6 + Math.sin(frame / 6) * 0.4;
  const shine = interpolate(frame, [45, 85], [-120, 220], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });

  return (
    <AbsoluteFill>
      <Background />
      <Camera
        from={{ zoom: 1.25, rotateX: 0, rotateY: 0, x: 0, y: 0 }}
        to={{ zoom: 1.0, rotateX: 4, rotateY: -6, x: 0, y: 0 }}
      >
        <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", transformStyle: "preserve-3d" }}>
          {BURST.map((b, i) => (
            <div
              key={i}
              style={{
                position: "absolute",
                width: b.size,
                height: b.size,
                borderRadius: "50%",
                background: b.color,
                boxShadow: `0 0 20px ${b.color}`,
                opacity: 1 - burst,
                transform: `translate(${Math.cos(b.angle) * b.dist * burst}px, ${Math.sin(b.angle) * b.dist * burst - 120}px)`,
              }}
            />
          ))}

          <div
            style={{
              fontFamily,
              textAlign: "center",
              transform: `scale(${title}) translateY(-120px) translateZ(100px)`,
            }}
          >
            <div style={{ fontSize: 96, fontWeight: 800, color: colors.text, lineHeight: 1.1 }}>Создавай видео</div>
            <div style={{ fontSize: 120, fontWeight: 900, lineHeight: 1.15, ...gradientText }}>словами</div>
          </div>

          <div
            style={{
              position: "absolute",
              top: 1180,
              padding: "38px 74px",
              borderRadius: 60,
              background: `linear-gradient(100deg, ${colors.blue}, ${colors.violet})`,
              boxShadow: `0 0 ${60 * glow + 30}px ${colors.violet}`,
              fontFamily,
              fontSize: 52,
              fontWeight: 800,
              color: colors.text,
              overflow: "hidden",
              opacity: button,
              transform: `scale(${0.5 + button * 0.5}) translateZ(60px)`,
            }}
          >
            Подключи Remotion
            <div
              style={{
                position: "absolute",
                top: 0,
                bottom: 0,
                left: `${shine}%`,
                width: 120,
                background: "linear-gradient(90deg, transparent, rgba(255,255,255,0.55), transparent)",
                transform: "skewX(-20deg)",
              }}
            />
          </div>

          <div
            style={{
              position: "absolute",
              top: 1400,
              fontFamily,
              fontSize: 44,
              color: colors.muted,
              opacity: interpolate(frame, [45, 70], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
            }}
          >
            remotion.dev
          </div>
        </AbsoluteFill>
      </Camera>
    </AbsoluteFill>
  );
};
