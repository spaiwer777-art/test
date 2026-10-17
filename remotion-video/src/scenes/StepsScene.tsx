import React from "react";
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { Background } from "../Background";
import { Camera } from "../Camera";
import { colors, fontFamily, gradientText } from "../theme";

const STEPS = [
  { n: "01", title: "Опиши идею", desc: "одним сообщением", color: colors.cyan },
  { n: "02", title: "Claude пишет код", desc: "React + анимации", color: colors.violet },
  { n: "03", title: "Remotion рендерит", desc: "готовое MP4", color: colors.pink },
];

const STEP_GAP = 34;

export const StepsScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const heading = spring({ frame, fps, config: { damping: 200 } });
  const line = interpolate(frame, [12, 12 + STEP_GAP * 2 + 20], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill>
      <Background />
      <Camera
        from={{ zoom: 0.92, rotateX: -6, rotateY: -14, x: 30, y: 40 }}
        to={{ zoom: 1.0, rotateX: 6, rotateY: 10, x: -20, y: -40 }}
      >
        <AbsoluteFill style={{ alignItems: "center", transformStyle: "preserve-3d" }}>
          <div
            style={{
              marginTop: 280,
              fontFamily,
              fontSize: 96,
              fontWeight: 900,
              textAlign: "center",
              lineHeight: 1.05,
              opacity: heading,
              transform: `translateY(${(1 - heading) * -60}px) translateZ(80px)`,
            }}
          >
            <span style={{ color: colors.text }}>Как это </span>
            <span style={gradientText}>работает</span>
          </div>

          <div style={{ position: "relative", marginTop: 110, width: 880, transformStyle: "preserve-3d" }}>
            <div
              style={{
                position: "absolute",
                left: 78,
                top: 80,
                width: 8,
                height: 760 * line,
                borderRadius: 4,
                background: `linear-gradient(${colors.cyan}, ${colors.violet}, ${colors.pink})`,
                boxShadow: `0 0 30px ${colors.violet}`,
              }}
            />
            {STEPS.map((s, i) => {
              const p = spring({ frame: frame - 12 - i * STEP_GAP, fps, config: { damping: 13 } });
              const pulse = 1 + Math.sin((frame - i * 10) / 7) * 0.04;
              return (
                <div
                  key={s.n}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 44,
                    height: 260,
                    opacity: p,
                    transform: `translateX(${(1 - p) * 300}px) translateZ(${40 + (1 - p) * 300}px) scale(${0.6 + p * 0.4})`,
                    fontFamily,
                  }}
                >
                  <div
                    style={{
                      flexShrink: 0,
                      width: 164,
                      height: 164,
                      borderRadius: 82,
                      background: `radial-gradient(circle at 30% 30%, ${s.color}, ${colors.bg} 90%)`,
                      border: `4px solid ${s.color}`,
                      boxShadow: `0 0 60px ${s.color}`,
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      fontSize: 58,
                      fontWeight: 900,
                      color: colors.text,
                      transform: `scale(${pulse})`,
                    }}
                  >
                    {s.n}
                  </div>
                  <div>
                    <div style={{ fontSize: 66, fontWeight: 800, color: colors.text, lineHeight: 1.1 }}>{s.title}</div>
                    <div style={{ fontSize: 44, color: colors.muted, marginTop: 8 }}>{s.desc}</div>
                  </div>
                </div>
              );
            })}
          </div>
        </AbsoluteFill>
      </Camera>
    </AbsoluteFill>
  );
};
