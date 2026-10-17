import React from "react";
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { Background } from "../Background";
import { Camera } from "../Camera";
import { colors, fontFamily, gradientText } from "../theme";

const PlayLogo: React.FC<{ progress: number; frame: number }> = ({ progress, frame }) => (
  <div
    style={{
      width: 300,
      height: 300,
      borderRadius: 80,
      background: `linear-gradient(135deg, ${colors.blue}, ${colors.violet})`,
      boxShadow: `0 0 ${80 + Math.sin(frame / 8) * 30}px ${colors.blue}, inset 0 0 40px rgba(255,255,255,0.25)`,
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      transform: `scale(${progress}) rotateY(${(1 - progress) * 180}deg) translateZ(80px)`,
    }}
  >
    <div
      style={{
        width: 0,
        height: 0,
        marginLeft: 30,
        borderTop: "70px solid transparent",
        borderBottom: "70px solid transparent",
        borderLeft: `115px solid ${colors.text}`,
      }}
    />
  </div>
);

export const IntroScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const logo = spring({ frame, fps, config: { damping: 12, mass: 0.8 } });
  const title = spring({ frame: frame - 18, fps, config: { damping: 200 } });
  const sub = spring({ frame: frame - 34, fps, config: { damping: 200 } });
  const ring = interpolate(frame, [0, 40], [0, 1], { extrapolateRight: "clamp" });

  return (
    <AbsoluteFill>
      <Background />
      <Camera
        from={{ zoom: 0.8, rotateX: 18, rotateY: -14, x: 0, y: 60 }}
        to={{ zoom: 1.08, rotateX: -4, rotateY: 8, x: 0, y: -20 }}
      >
        <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", transformStyle: "preserve-3d" }}>
          <div
            style={{
              position: "absolute",
              width: 620,
              height: 620,
              borderRadius: "50%",
              border: `4px solid ${colors.cyan}`,
              opacity: 1 - ring,
              transform: `scale(${0.4 + ring * 1.6}) translateY(-170px)`,
            }}
          />
          <div style={{ transform: "translateY(-170px)", transformStyle: "preserve-3d" }}>
            <PlayLogo progress={logo} frame={frame} />
          </div>
          <div
            style={{
              position: "absolute",
              top: 1160,
              textAlign: "center",
              fontFamily,
              transform: `translateY(${(1 - title) * 120}px) translateZ(120px)`,
              opacity: title,
            }}
          >
            <div style={{ fontSize: 170, fontWeight: 900, letterSpacing: -4, ...gradientText }}>Remotion</div>
          </div>
          <div
            style={{
              position: "absolute",
              top: 1380,
              fontFamily,
              fontSize: 56,
              fontWeight: 600,
              color: colors.muted,
              opacity: sub,
              transform: `translateY(${(1 - sub) * 60}px) translateZ(60px)`,
            }}
          >
            плагин для Claude
          </div>
        </AbsoluteFill>
      </Camera>
    </AbsoluteFill>
  );
};
