import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { Letters } from "../../carstory/kit";
import { KText } from "../kinetic";
import { A, display, E, mono, prog } from "../theme";

// The "I" of "AI RACE" (mono font → exact geometry) is the doorway into scene 2.
const LINE_SIZE = 90;
const LINE_TOP = 1115;
const CELL = LINE_SIZE * 0.6;
const LINE_LEFT = 540 - (7 * CELL) / 2;
const I_X = LINE_LEFT + 1.5 * CELL;
const I_Y = LINE_TOP + LINE_SIZE * 0.55;
const LIGHTS = [30, 42, 54, 66, 78];
const OUT = 90;
const TYPED = "> start_race()";

export const S1Start: React.FC = () => {
  const f = useCurrentFrame();

  const typed = TYPED.slice(0, Math.max(0, Math.floor((f - 4) / 2)));
  const cursorOn = Math.floor(f / 8) % 2 === 0;
  const termOut = prog(f, 28, 10, E.expoIn);

  const slam = prog(f, OUT, 14);
  const zoom = prog(f, 118, 22, E.expoIn);
  const k = interpolate(zoom, [0, 1], [1, 28]);
  const bar = prog(f, 134, 6);
  const turn = prog(f, 140, 10, E.expoInOut);

  return (
    <AbsoluteFill style={{ overflow: "hidden" }}>
      {/* Terminal prompt */}
      <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", opacity: 1 - termOut, transform: `translateY(${-termOut * 120}px)` }}>
        <div style={{ fontFamily: mono, fontSize: 64, fontWeight: 600, color: A.blue, textShadow: `0 0 30px ${A.blue}88` }}>
          {typed}
          <span style={{ opacity: cursorOn ? 1 : 0 }}>▍</span>
        </div>
      </AbsoluteFill>

      {/* F1 start lights */}
      {f >= 26 && f < 118 ? (
        <div style={{ position: "absolute", top: 700, width: 1080, display: "flex", justifyContent: "center", gap: 28, opacity: prog(f, 26, 8) * (1 - prog(f, OUT, 6)) }}>
          {LIGHTS.map((at, i) => {
            const lit = f >= at && f < OUT;
            return (
              <div key={i} style={{ width: 150, height: 330, borderRadius: 26, background: "#111117", border: "3px solid #23232c", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "space-evenly" }}>
                {[0, 1].map((j) => (
                  <div
                    key={j}
                    style={{
                      width: 104,
                      height: 104,
                      borderRadius: 52,
                      background: lit ? A.red : "#2a1010",
                      boxShadow: lit ? `0 0 50px ${A.red}, inset 0 0 20px #ffffff55` : "none",
                    }}
                  />
                ))}
              </div>
            );
          })}
        </div>
      ) : null}

      {/* Title slam and the zoom through the "I" */}
      {f >= OUT ? (
        <AbsoluteFill style={{ transformOrigin: `${I_X}px ${I_Y}px`, transform: `translate(${zoom * (540 - I_X)}px, ${zoom * (960 - I_Y)}px) scale(${k})`, opacity: 1 - bar }}>
          <div style={{ position: "absolute", top: 560, width: 1080, textAlign: "center", fontFamily: display, fontWeight: 900, fontSize: 170, color: A.text, letterSpacing: -6, transform: `scale(${interpolate(slam, [0, 1], [3, 1])})`, opacity: slam }}>
            ГОНКА
          </div>
          <div style={{ position: "absolute", top: 780, width: 1080, display: "flex", justifyContent: "center", overflow: "hidden" }}>
            <Letters text="НЕЙРОСЕТЕЙ" start={OUT + 4} stagger={1.5} mode="drop" style={{ fontFamily: display, fontWeight: 900, fontSize: 106, color: A.blue }} />
          </div>
          <div style={{ position: "absolute", left: LINE_LEFT, top: LINE_TOP, fontFamily: mono, fontWeight: 700, fontSize: LINE_SIZE, color: A.text, whiteSpace: "pre", lineHeight: 1.1, opacity: prog(f, 102, 10) }}>
            {"AI RACE".split("").map((ch, i) => (
              <span key={i} style={{ display: "inline-block", width: CELL, textAlign: "center", color: i === 1 ? A.blue : A.text }}>
                {ch}
              </span>
            ))}
          </div>
          <div style={{ position: "absolute", top: LINE_TOP + 120, width: 1080, display: "flex", justifyContent: "center" }}>
            <KText text="1943 — 2026" start={104} mode="decode" stagger={1} font={mono} style={{ fontSize: 34, letterSpacing: 8, color: A.grey }} />
          </div>
        </AbsoluteFill>
      ) : null}

      {/* The stem of the I becomes the race track line */}
      {bar > 0 ? (
        <div
          style={{
            position: "absolute",
            left: 540 - interpolate(turn, [0, 1], [90, 3]),
            top: 0,
            width: interpolate(turn, [0, 1], [180, 6]),
            height: 1920,
            background: A.blue,
            boxShadow: `0 0 40px ${A.blue}`,
            opacity: bar,
            transform: `rotate(${turn * 90}deg)`,
          }}
        />
      ) : null}
    </AbsoluteFill>
  );
};
