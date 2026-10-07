import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { Overlay, sepiaAt } from "./Overlay";
import { Scene3D } from "./Scene3D";

export const CarHistory: React.FC = () => {
  const frame = useCurrentFrame();
  const sepia = sepiaAt(frame);

  return (
    <AbsoluteFill style={{ backgroundColor: "#000" }}>
      <AbsoluteFill style={{ filter: `sepia(${sepia}) contrast(${1 + sepia * 0.15})` }}>
        <Scene3D />
      </AbsoluteFill>
      <Overlay />
    </AbsoluteFill>
  );
};
