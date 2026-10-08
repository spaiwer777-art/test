import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { C, display, E, Letters, MaskLine, body, prog, useShake } from "../kit";
import { RingWord } from "../RingWord";

// Ring of "КОЛЕСО" — scene 2 picks the wheel up at exactly this spot.
export const RING = { x: 790, y: 1060, r: 58, w: 20 };
export const S1_DURATION = 110;
const ZOOM_START = 88;

export const S1Intro: React.FC = () => {
  const frame = useCurrentFrame();
  const shake = useShake([9, 50]);

  const line = prog(frame, 0, 8, E.expoOut);
  const open = prog(frame, 6, 14, E.expoOut);
  const slam = prog(frame, 7, 10, E.expoOut);

  const zoom = prog(frame, ZOOM_START, S1_DURATION - ZOOM_START, E.expoIn);
  const scale = interpolate(zoom, [0, 1], [1, 300 / RING.r]);
  const shift = zoom * (540 - RING.x);
  const lift = zoom * (1000 - RING.y);

  return (
    <AbsoluteFill style={{ backgroundColor: C.ink, overflow: "hidden" }}>
      <AbsoluteFill style={{ transform: shake }}>
        {/* Opening red line that splits into two frame bars */}
        {[-1, 1].map((s) => (
          <div
            key={s}
            style={{
              position: "absolute",
              left: 0,
              top: 958 + s * open * 520,
              width: 1080 * line,
              height: 6,
              background: C.red,
              opacity: 1 - prog(frame, 40, 12),
            }}
          />
        ))}

        {/* Beat 1: 5500 ЛЕТ */}
        {frame < 50 ? (
          <AbsoluteFill style={{ alignItems: "center", justifyContent: "center" }}>
            <div
              style={{
                fontFamily: display,
                fontWeight: 900,
                fontSize: 250,
                color: C.paper,
                letterSpacing: -10,
                transform: `scale(${interpolate(slam, [0, 1], [3, 1])}) translateY(${prog(frame, 40, 10, E.expoIn) * -900}px)`,
                opacity: slam,
              }}
            >
              5500
            </div>
            <MaskLine start={14} out={40} style={{ marginTop: 10 }}>
              <div style={{ fontFamily: body, fontWeight: 800, fontSize: 64, letterSpacing: 24, color: C.red }}>ЛЕТ ДВИЖЕНИЯ</div>
            </MaskLine>
          </AbsoluteFill>
        ) : null}

        {/* Beat 2: ИСТОРИЯ АВТОМОБИЛЯ */}
        {frame >= 46 && frame < 84 ? (
          <AbsoluteFill style={{ alignItems: "center", justifyContent: "center" }}>
            <Letters
              text="ИСТОРИЯ"
              start={48}
              out={76}
              stagger={2}
              mode="drop"
              style={{ fontFamily: display, fontWeight: 900, fontSize: 132, color: C.paper }}
            />
            <Letters
              text="АВТОМОБИЛЯ"
              start={54}
              out={78}
              stagger={1.5}
              mode="rise"
              style={{ fontFamily: display, fontWeight: 300, fontSize: 92, color: C.red, marginTop: 10 }}
            />
          </AbsoluteFill>
        ) : null}

        {/* Beat 3: ВСЁ НАЧАЛОСЬ С КОЛЕСО → fly through the ring */}
        {frame >= 74 ? (
          <AbsoluteFill
            style={{
              transformOrigin: `${RING.x}px ${RING.y}px`,
              transform: `translate(${shift}px, ${lift}px) scale(${scale})`,
            }}
          >
            <div style={{ position: "absolute", top: 900, width: 1080, textAlign: "center", opacity: 1 - zoom * 3 }}>
              <MaskLine start={74} dur={12}>
                <div style={{ fontFamily: body, fontWeight: 700, fontSize: 44, letterSpacing: 10, color: C.grey }}>ВСЁ НАЧАЛОСЬ С</div>
              </MaskLine>
            </div>
            <RingWord
              left="КОЛЕС"
              cx={RING.x}
              cy={RING.y}
              size={128}
              font={display}
              weight={900}
              start={77}
              ringR={RING.r}
              ringW={RING.w}
            />
          </AbsoluteFill>
        ) : null}
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
