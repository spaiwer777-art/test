import React from "react";
import { AbsoluteFill, Easing, interpolate, useCurrentFrame, useVideoConfig } from "remotion";

type CameraKey = {
  zoom: number;
  rotateX: number;
  rotateY: number;
  x: number;
  y: number;
};

// A smooth virtual camera: interpolates between a start and end pose over the
// scene, with a gentle handheld-like drift layered on top.
export const Camera: React.FC<{
  from: CameraKey;
  to: CameraKey;
  children: React.ReactNode;
}> = ({ from, to, children }) => {
  const frame = useCurrentFrame();
  const { durationInFrames } = useVideoConfig();

  const t = interpolate(frame, [0, durationInFrames], [0, 1], {
    easing: Easing.bezier(0.45, 0, 0.2, 1),
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const lerp = (a: number, b: number) => a + (b - a) * t;

  const driftX = Math.sin(frame / 37) * 6;
  const driftY = Math.cos(frame / 43) * 5;
  const driftRot = Math.sin(frame / 51) * 0.6;

  return (
    <AbsoluteFill style={{ perspective: 1600, perspectiveOrigin: "50% 50%" }}>
      <AbsoluteFill
        style={{
          transformStyle: "preserve-3d",
          transform: [
            `translate3d(${lerp(from.x, to.x) + driftX}px, ${lerp(from.y, to.y) + driftY}px, 0)`,
            `scale(${lerp(from.zoom, to.zoom)})`,
            `rotateX(${lerp(from.rotateX, to.rotateX) + driftRot}deg)`,
            `rotateY(${lerp(from.rotateY, to.rotateY)}deg)`,
          ].join(" "),
        }}
      >
        {children}
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
