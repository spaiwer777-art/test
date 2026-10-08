import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { CARS } from "../cars";
import { C, body, display, Draw, E, Letters, MaskLine, prog } from "../kit";
import { RingWord } from "../RingWord";

export const S10_DURATION = 160;

export const S10Outro: React.FC = () => {
  const frame = useCurrentFrame();
  const up = prog(frame, 92, 22, E.expoInOut);
  const fadeOut = prog(frame, S10_DURATION - 12, 12);
  const bar = prog(frame, 108, 20, E.expoInOut);

  return (
    <AbsoluteFill style={{ backgroundColor: C.yellow, overflow: "hidden" }}>
      <AbsoluteFill style={{ transform: `translateY(${-up * 1920}px)` }}>
        <RingWord left="ЧТ" cx={700} cy={640} size={210} font={display} weight={900} start={4} ringR={78} ringW={26} color={C.ink} />
        <div style={{ position: "absolute", top: 780, width: 1080, display: "flex", justifyContent: "center" }}>
          <Letters text="ДАЛЬШЕ?" start={14} stagger={2} mode="rise" style={{ fontFamily: display, fontWeight: 900, fontSize: 140, color: C.ink }} />
        </div>

        {/* Mini timeline of every era */}
        <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
          <Draw d="M90,1320 L990,1320" start={30} dur={24} stroke={C.ink} width={5} />
          {CARS.map((c, i) => {
            const p = prog(frame, 36 + i * 6, 16, E.backOut);
            const x = 90 + i * 225;
            return (
              <g key={i} transform={`translate(${x - 90} ${1180}) scale(${0.3 * p})`}>
                <path d={c.body} fill={i === CARS.length - 1 ? C.red : C.ink} />
                {c.wheels.map((w, j) => (
                  <circle key={j} cx={w.x} cy={200 - w.r} r={w.r} fill={C.ink} stroke={C.yellow} strokeWidth={8} />
                ))}
              </g>
            );
          })}
        </svg>
        {CARS.map((c, i) => (
          <div
            key={i}
            style={{
              position: "absolute",
              left: i * 225,
              width: 180,
              top: 1350,
              textAlign: "center",
              fontFamily: display,
              fontWeight: 800,
              fontSize: 34,
              color: C.ink,
              opacity: prog(frame, 40 + i * 6, 10),
            }}
          >
            {c.year}
          </div>
        ))}
      </AbsoluteFill>

      {/* Sign-off */}
      <AbsoluteFill style={{ transform: `translateY(${(1 - up) * 1920}px)`, alignItems: "center", justifyContent: "center" }}>
        <MaskLine start={104}>
          <div style={{ fontFamily: display, fontWeight: 900, fontSize: 110, color: C.ink }}>ИСТОРИЯ</div>
        </MaskLine>
        <MaskLine start={108}>
          <div style={{ fontFamily: display, fontWeight: 300, fontSize: 82, color: C.ink }}>АВТОМОБИЛЯ</div>
        </MaskLine>
        <div style={{ width: 760 * bar, height: 14, background: C.red, marginTop: 30 }} />
        <MaskLine start={118}>
          <div style={{ fontFamily: body, fontWeight: 700, fontSize: 32, letterSpacing: 10, color: C.ink, marginTop: 30 }}>
            ОТ КОЛЕСА ДО ЭЛЕКТРОКАРА
          </div>
        </MaskLine>
      </AbsoluteFill>

      <AbsoluteFill style={{ background: C.ink, opacity: fadeOut }} />
    </AbsoluteFill>
  );
};
