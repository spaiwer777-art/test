import React from "react";
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { Background } from "../Background";
import { Camera } from "../Camera";
import { colors, fontFamily, gradientText, monoFamily } from "../theme";

const LINES: { text: string; color: string; indent: number }[] = [
  { text: "export const Video = () => {", color: colors.cyan, indent: 0 },
  { text: "const frame = useCurrentFrame();", color: colors.text, indent: 1 },
  { text: "const scale = spring({frame, fps});", color: colors.text, indent: 1 },
  { text: "return (", color: colors.violet, indent: 1 },
  { text: "<Title style={{scale}}>", color: colors.pink, indent: 2 },
  { text: "Привет, мир!", color: colors.text, indent: 3 },
  { text: "</Title>", color: colors.pink, indent: 2 },
  { text: ");", color: colors.violet, indent: 1 },
  { text: "};", color: colors.cyan, indent: 0 },
];

const CHARS_PER_FRAME = 2.6;

export const CodeScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const card = spring({ frame, fps, config: { damping: 18 } });
  const heading = spring({ frame: frame - 6, fps, config: { damping: 200 } });

  let budget = Math.max(0, (frame - 15) * CHARS_PER_FRAME);
  const cursorOn = Math.floor(frame / 8) % 2 === 0;

  return (
    <AbsoluteFill>
      <Background />
      <Camera
        from={{ zoom: 0.92, rotateX: 10, rotateY: 22, x: -40, y: 0 }}
        to={{ zoom: 1.02, rotateX: 4, rotateY: -16, x: 30, y: -10 }}
      >
        <AbsoluteFill style={{ alignItems: "center", transformStyle: "preserve-3d" }}>
          <div
            style={{
              marginTop: 260,
              textAlign: "center",
              fontFamily,
              opacity: heading,
              transform: `translateY(${(1 - heading) * -80}px) translateZ(100px)`,
            }}
          >
            <div style={{ fontSize: 100, fontWeight: 900, color: colors.text, lineHeight: 1.05 }}>Видео —</div>
            <div style={{ fontSize: 130, fontWeight: 900, lineHeight: 1.1, ...gradientText }}>это код</div>
          </div>

          <div
            style={{
              marginTop: 110,
              width: 940,
              borderRadius: 40,
              background: "rgba(16, 18, 48, 0.78)",
              border: `2px solid ${colors.blue}88`,
              boxShadow: `0 40px 120px rgba(0,0,0,0.6), 0 0 80px ${colors.blue}55`,
              padding: "36px 44px 54px",
              transform: `translateY(${(1 - card) * 500}px) rotateX(${(1 - card) * 50}deg) translateZ(40px)`,
              opacity: card,
            }}
          >
            <div style={{ display: "flex", gap: 16, marginBottom: 36 }}>
              {[colors.pink, "#ffbd2e", "#27c93f"].map((c) => (
                <div key={c} style={{ width: 26, height: 26, borderRadius: 13, background: c }} />
              ))}
            </div>
            {LINES.map((line, i) => {
              const shown = Math.min(line.text.length, Math.floor(budget));
              budget -= line.text.length;
              const active = shown > 0 && shown < line.text.length;
              return (
                <div
                  key={i}
                  style={{
                    fontFamily: monoFamily,
                    fontSize: 40,
                    lineHeight: 1.7,
                    color: line.color,
                    paddingLeft: line.indent * 44,
                    whiteSpace: "pre",
                    minHeight: 68,
                  }}
                >
                  {line.text.slice(0, shown)}
                  {active && cursorOn ? <span style={{ color: colors.cyan }}>▍</span> : null}
                </div>
              );
            })}
          </div>

          <div
            style={{
              marginTop: 70,
              fontFamily,
              fontSize: 48,
              fontWeight: 600,
              color: colors.muted,
              opacity: interpolate(frame, [70, 95], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
            }}
          >
            React-компоненты → MP4
          </div>
        </AbsoluteFill>
      </Camera>
    </AbsoluteFill>
  );
};
