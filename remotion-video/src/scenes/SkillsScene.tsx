import React from "react";
import { AbsoluteFill, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { Background } from "../Background";
import { Camera } from "../Camera";
import { colors, fontFamily, gradientText } from "../theme";

const SKILLS: { icon: string; name: string; desc: string; color: string }[] = [
  { icon: "✦", name: "Create", desc: "новое видео с нуля", color: colors.cyan },
  { icon: "◐", name: "Markup", desc: "анимации и эффекты", color: colors.blue },
  { icon: "❝", name: "Captions", desc: "субтитры", color: colors.violet },
  { icon: "◎", name: "Maps", desc: "анимация карт", color: colors.pink },
  { icon: "▶", name: "Studio", desc: "живой предпросмотр", color: colors.cyan },
  { icon: "⬇", name: "Render", desc: "экспорт в MP4", color: colors.blue },
];

export const SkillsScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const heading = spring({ frame, fps, config: { damping: 200 } });

  return (
    <AbsoluteFill>
      <Background />
      <Camera
        from={{ zoom: 1.15, rotateX: 28, rotateY: 0, x: 0, y: 140 }}
        to={{ zoom: 0.96, rotateX: 6, rotateY: 0, x: 0, y: 0 }}
      >
        <AbsoluteFill style={{ alignItems: "center", transformStyle: "preserve-3d" }}>
          <div
            style={{
              marginTop: 230,
              fontFamily,
              textAlign: "center",
              opacity: heading,
              transform: `scale(${0.7 + heading * 0.3}) translateZ(100px)`,
            }}
          >
            <div style={{ fontSize: 170, fontWeight: 900, lineHeight: 1, ...gradientText }}>12</div>
            <div style={{ fontSize: 84, fontWeight: 800, color: colors.text }}>навыков</div>
          </div>

          <div
            style={{
              marginTop: 80,
              display: "grid",
              gridTemplateColumns: "1fr 1fr",
              gap: 36,
              width: 920,
              transformStyle: "preserve-3d",
            }}
          >
            {SKILLS.map((s, i) => {
              const p = spring({ frame: frame - 14 - i * 6, fps, config: { damping: 14, mass: 0.7 } });
              const fromLeft = i % 2 === 0;
              const hover = Math.sin((frame + i * 12) / 18) * 8;
              return (
                <div
                  key={s.name}
                  style={{
                    height: 250,
                    borderRadius: 36,
                    padding: 34,
                    background: "rgba(18, 20, 52, 0.8)",
                    border: `2px solid ${s.color}99`,
                    boxShadow: `0 0 50px ${s.color}44, 0 30px 60px rgba(0,0,0,0.5)`,
                    fontFamily,
                    opacity: p,
                    transform: [
                      `translateX(${(1 - p) * (fromLeft ? -700 : 700)}px)`,
                      `translateZ(${(1 - p) * -600 + 30 + hover}px)`,
                      `rotateY(${(1 - p) * (fromLeft ? -70 : 70)}deg)`,
                    ].join(" "),
                  }}
                >
                  <div style={{ fontSize: 64, color: s.color, textShadow: `0 0 24px ${s.color}` }}>{s.icon}</div>
                  <div style={{ fontSize: 54, fontWeight: 800, color: colors.text, marginTop: 10 }}>{s.name}</div>
                  <div style={{ fontSize: 34, color: colors.muted, marginTop: 6 }}>{s.desc}</div>
                </div>
              );
            })}
          </div>
        </AbsoluteFill>
      </Camera>
    </AbsoluteFill>
  );
};
