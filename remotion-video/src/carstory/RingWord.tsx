import React from "react";
import { useCurrentFrame } from "remotion";
import { C, E, Letters, prog } from "./kit";

// A word whose letter "О" is replaced by a drawn red ring (the wheel motif).
// The ring sits at a fixed screen point so the camera can fly through it.
export const RingWord: React.FC<{
  left: string;
  right?: string;
  cx: number;
  cy: number;
  size: number;
  font: string;
  weight?: number;
  start: number;
  ringR: number;
  ringW: number;
  color?: string;
  gap?: number;
}> = ({ left, right = "", cx, cy, size, font, weight = 800, start, ringR, ringW, color = C.paper, gap = 10 }) => {
  const frame = useCurrentFrame();
  const ring = prog(frame, start + left.length * 2, 20, E.expoOut);
  const text: React.CSSProperties = { fontFamily: font, fontSize: size, fontWeight: weight, color, lineHeight: 1 };
  return (
    <>
      <div
        style={{
          position: "absolute",
          right: 1080 - (cx - ringR - ringW / 2 - gap),
          top: cy - size * 0.55,
          overflow: "hidden",
        }}
      >
        <Letters text={left} start={start} stagger={2} style={text} />
      </div>
      {right ? (
        <div style={{ position: "absolute", left: cx + ringR + ringW / 2 + gap, top: cy - size * 0.55, overflow: "hidden" }}>
          <Letters text={right} start={start + left.length * 2 + 4} stagger={2} style={text} />
        </div>
      ) : null}
      <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
        <circle
          cx={cx}
          cy={cy}
          r={ringR}
          fill="none"
          stroke={C.red}
          strokeWidth={ringW}
          pathLength={1}
          strokeDasharray="1 1"
          strokeDashoffset={1 - ring}
          transform={`rotate(-90 ${cx} ${cy})`}
        />
      </svg>
    </>
  );
};
