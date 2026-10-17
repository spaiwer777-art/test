import React from "react";
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { Draw } from "../../carstory/kit";
import { Caption } from "../rigs";
import { A, body, display, E, mono, prog } from "../theme";

const C = A.blue;
const NODE = { x: 600, y: 960, r: 86 };
const INPUTS = [800, 960, 1120];
// Abstract profile of a head, drawn as one line
const PROFILE =
  "M300,40 C200,40 140,100 130,190 C126,228 116,250 98,276 C90,288 98,296 112,298 L122,300 C116,315 118,326 128,331 C120,342 124,353 136,357 C132,376 140,392 165,395 C192,398 210,404 214,432 L220,560 M300,40 C405,40 455,125 444,232 C436,322 402,364 332,384 L330,560";
const QUOTE = "«Может ли машина мыслить?»";

export const S2Idea: React.FC = () => {
  const f = useCurrentFrame();
  const { fps } = useVideoConfig();

  const trackFade = 1 - prog(f, 4, 14);
  const neuronAway = prog(f, 60, 20, E.expoInOut);
  const turing = f >= 64 && f < 214;
  const plateDrop = spring({ frame: f - 150, fps, config: { damping: 11, mass: 0.9 } });
  const collapse = prog(f, 210, 14, E.expoIn);
  const fly = prog(f, 222, 18, E.expoIn);
  const quoteChars = Math.max(0, Math.floor((f - 100) / 1.2));
  const reveal = prog(f, 172, 22, E.expoOut);

  return (
    <AbsoluteFill style={{ overflow: "hidden" }}>
      {/* Track line from scene 1 */}
      <div style={{ position: "absolute", left: 0, right: 0, top: 957, height: 6, background: C, boxShadow: `0 0 40px ${C}`, opacity: trackFade }} />

      <AbsoluteFill style={{ transform: `scaleY(${1 - collapse * 0.985})`, transformOrigin: "50% 960px" }}>
        <Caption year="1943" title="Модель нейрона" start={6} out={58} accent={C} />
        <Caption year="1950" title="Алан Тьюринг" start={66} out={146} accent={C} />
        <Caption year="1956" title="У науки появляется имя" start={152} out={214} accent={C} />

        {/* McCulloch–Pitts neuron */}
        <AbsoluteFill style={{ transformOrigin: "540px 960px", transform: `translateY(${-neuronAway * 420}px) scale(${1 - neuronAway * 0.45})`, opacity: 1 - prog(f, 146, 10) }}>
          <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
            {INPUTS.map((y, i) => (
              <Draw key={i} d={`M120,${y} C320,${y} 380,${NODE.y} ${NODE.x - NODE.r},${NODE.y}`} start={2 + i * 4} dur={14} stroke={C} width={5} />
            ))}
            <circle cx={NODE.x} cy={NODE.y} r={NODE.r * prog(f, 12, 14, E.backOut)} fill="#0b0f22" stroke={C} strokeWidth={6} />
            <Draw d={`M${NODE.x + NODE.r},${NODE.y} L960,${NODE.y}`} start={20} dur={12} stroke={C} width={5} />
            <Draw d="M820,1010 L870,1010 L870,910 L930,910" start={26} dur={12} stroke={A.text} width={6} />
            {INPUTS.map((y, i) => (
              <text key={i} x={120} y={y - 24} fill={A.text} fontFamily={mono} fontSize={40} opacity={prog(f, 8 + i * 4, 8)}>
                x{i + 1}
              </text>
            ))}
            <text x={NODE.x} y={NODE.y + 26} textAnchor="middle" fill={A.text} fontFamily={mono} fontSize={76} fontWeight={700} opacity={prog(f, 18, 8)}>
              Σ
            </text>
            <text x={880} y={880} fill={C} fontFamily={mono} fontSize={40} opacity={prog(f, 30, 8)}>
              θ
            </text>
          </svg>
          <div style={{ position: "absolute", top: 1240, width: 1080, textAlign: "center", fontFamily: body, fontSize: 38, color: A.grey, opacity: prog(f, 24, 12) * (1 - neuronAway) }}>
            Маккаллок и Питтс: нейрон как логическая схема
          </div>
        </AbsoluteFill>

        {/* Turing: profile drawn in one line, the question typed below */}
        {turing ? (
          <AbsoluteFill style={{ opacity: 1 - prog(f, 140, 12) }}>
            <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
              <g transform="translate(290 700)">
                <Draw d={PROFILE} start={66} dur={40} stroke={A.text} width={5} />
              </g>
            </svg>
            <div style={{ position: "absolute", top: 1330, width: 1080, textAlign: "center", fontFamily: body, fontWeight: 700, fontSize: 56, color: A.text }}>
              {QUOTE.slice(0, quoteChars)}
              <span style={{ color: C, opacity: Math.floor(f / 6) % 2 ? 1 : 0 }}>▍</span>
            </div>
          </AbsoluteFill>
        ) : null}

        {/* Dartmouth plate */}
        {f >= 148 ? (
          <AbsoluteFill style={{ alignItems: "center" }}>
            <div
              style={{
                position: "absolute",
                top: interpolate(plateDrop, [0, 1], [-400, 760]),
                padding: "36px 60px",
                border: `5px solid ${C}`,
                background: "#0a0f24",
                boxShadow: `0 0 60px ${C}55`,
                fontFamily: display,
                fontWeight: 900,
                fontSize: 74,
                color: A.text,
              }}
            >
              ДАРТМУТ · 1956
            </div>
            <div style={{ position: "absolute", top: 1010, width: 1080, textAlign: "center", fontFamily: mono, fontWeight: 700, fontSize: 56, color: C, clipPath: `inset(0 ${(1 - reveal) * 100}% 0 0)`, textShadow: `0 0 30px ${C}` }}>
              ARTIFICIAL
              <br />
              INTELLIGENCE
            </div>
            <div style={{ position: "absolute", top: 1220, width: 1080, textAlign: "center", fontFamily: body, fontSize: 38, color: A.grey, opacity: prog(f, 186, 12) }}>
              термин «искусственный интеллект»
            </div>
          </AbsoluteFill>
        ) : null}
      </AbsoluteFill>

      {/* Everything straightens into one line that shoots off to the right */}
      {collapse > 0.6 ? (
        <div
          style={{
            position: "absolute",
            left: fly * 1300,
            width: 1080,
            top: 957,
            height: 6,
            background: C,
            boxShadow: `0 0 40px ${C}`,
            filter: `blur(${fly * 14}px)`,
            transform: `scaleX(${1 + fly * 0.6})`,
            transformOrigin: "0 50%",
          }}
        />
      ) : null}
    </AbsoluteFill>
  );
};
