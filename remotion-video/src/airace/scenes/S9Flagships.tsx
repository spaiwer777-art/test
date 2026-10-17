import React from "react";
import { AbsoluteFill, interpolate, random, useCurrentFrame } from "remotion";
import { A, body, display, E, mono, prog } from "../theme";

const C = A.warm;
// Stage = calendar year shown by the odometer: 2023, 2024, 2025, 2026 (local frames).
export const STAGES = [0, 90, 180, 270];
// Names per stage — only well-known releases from each lab in that year.
const TEAMS = [
  { lab: "Anthropic", color: "#D97757", names: ["Claude", "Claude 3.5", "Claude 4", "Claude Opus 5.5"] },
  { lab: "OpenAI", color: "#10A37F", names: ["GPT-4", "GPT-4o", "GPT-5", "GPT-6"] },
  { lab: "Google", color: "#4285F4", names: ["Gemini", "Gemini 1.5", "Gemini 2.5", "Gemini 4"] },
  { lab: "Meta", color: "#0866FF", names: ["Llama 2", "Llama 3", "Llama 4", "Muse"] },
  { lab: "DeepSeek", color: "#4D6BFE", names: ["DeepSeek", "DeepSeek-V3", "DeepSeek-R1", "DeepSeek-V4"] },
  { lab: "xAI", color: "#BBBBBB", names: ["Grok", "Grok 2", "Grok 4", "Grok 4"] },
];
const N = TEAMS.length;
const ROW_H = 132;
const ROW_GAP = 14;
const TOP = 560;
const SWAP = 10;

// Order changes: every 30 frames, then every 15 as the race heats up. Every team leads at some point.
const CHANGE_FRAMES = [30, 60, 90, 120, 150, 180, 210, 240, 270, 285, 300];
const ORDERS: number[][] = [[0, 1, 2, 3, 4, 5]];
const leaders = [1, 2, 0, 4, 1, 3, 2, 5, 0, 1, 2];
CHANGE_FRAMES.forEach((_, k) => {
  const prev = ORDERS[ORDERS.length - 1];
  const rest = prev.filter((t) => t !== leaders[k]);
  // a couple of random neighbour swaps to keep the field moving
  for (let s = 0; s < 2; s++) {
    const i = Math.floor(random(`sw${k}-${s}`) * (rest.length - 1));
    [rest[i], rest[i + 1]] = [rest[i + 1], rest[i]];
  }
  ORDERS.push([leaders[k], ...rest]);
});

const MILESTONES = [
  { at: 24, text: "МУЛЬТИМОДАЛЬНОСТЬ" },
  { at: 96, text: "РАССУЖДЕНИЕ" },
  { at: 168, text: "ОТКРЫТЫЕ МОДЕЛИ" },
  { at: 236, text: "АГЕНТЫ" },
];
const INSERTS = ["код", "фото", "голос", "агент"];

const rankAt = (team: number, f: number) => {
  let idx = 0;
  for (let k = 0; k < CHANGE_FRAMES.length; k++) if (f >= CHANGE_FRAMES[k]) idx = k + 1;
  const now = ORDERS[idx].indexOf(team);
  if (idx === 0) return { pos: now, moved: 0 };
  const before = ORDERS[idx - 1].indexOf(team);
  const t = prog(f, CHANGE_FRAMES[idx - 1], SWAP, E.expoInOut);
  return { pos: before + (now - before) * t, moved: f - CHANGE_FRAMES[idx - 1] < 6 ? Math.sign(before - now) : 0 };
};

const stageAt = (f: number) => STAGES.filter((s) => f >= s).length - 1;

const Insert: React.FC<{ kind: number; f: number }> = ({ kind, f }) => {
  if (kind === 0)
    return (
      <div style={{ fontFamily: mono, fontSize: 18, color: "#9ef0b8", lineHeight: 1.5, padding: 16 }}>
        {"def solve(task):\n  plan = think(task)\n  return run(plan)".split("\n").map((l, i) => (
          <div key={i} style={{ whiteSpace: "pre" }}>{l}</div>
        ))}
      </div>
    );
  if (kind === 1)
    return <div style={{ position: "absolute", inset: 0, background: "linear-gradient(180deg, #f6b26b 0%, #c06c84 45%, #355c7d 46%, #1b2a3a 100%)" }} />;
  if (kind === 2)
    return (
      <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", gap: 6 }}>
        {new Array(12).fill(0).map((_, i) => (
          <div key={i} style={{ width: 8, height: 20 + Math.abs(Math.sin(f * 0.4 + i * 0.9)) * 90, background: C, borderRadius: 4 }} />
        ))}
      </div>
    );
  return (
    <div style={{ fontFamily: mono, fontSize: 18, color: C, padding: 16, lineHeight: 1.5 }}>
      <div>$ agent run</div>
      <div style={{ color: A.green }}>✓ tests passed</div>
      <div style={{ color: A.grey }}>{"█".repeat(Math.floor(f / 3) % 10)}</div>
    </div>
  );
};

export const S9Flagships: React.FC = () => {
  const f = useCurrentFrame();
  const white = 1 - prog(f, 0, 10, E.expoOut);
  const lineX = interpolate(prog(f, 6, 22, E.expoInOut), [0, 1], [540, 70]);
  const squash = prog(f, 300, 18, E.expoInOut);
  const converge = prog(f, 318, 16, E.expoInOut);
  const shrink = prog(f, 334, 12, E.expoIn);
  const pulse = f >= 346 ? 1 + Math.sin((f - 346) * 0.9) * 0.4 : 1;
  const extrasOut = prog(f, 296, 10);
  const stage = stageAt(f);

  return (
    <AbsoluteFill style={{ overflow: "hidden" }}>
      {/* Milestone ticker */}
      <div style={{ position: "absolute", top: 230, left: 70, right: 70, height: 140, overflow: "hidden", opacity: 1 - extrasOut }}>
        {MILESTONES.map((m, i) => {
          const next = MILESTONES[i + 1]?.at ?? 9999;
          const pin = prog(f, m.at, 12);
          const pout = prog(f, next, 10, E.expoIn);
          if (f < m.at - 1 || f > next + 12) return null;
          return (
            <div key={m.text} style={{ position: "absolute", left: 0, right: 0, transform: `translateY(${(1 - pin) * 110 - pout * 110}%)`, textAlign: "center" }}>
              <div style={{ fontFamily: mono, fontSize: 28, color: A.grey, letterSpacing: 6 }}>ЭТАП {i + 1}/4</div>
              <div style={{ fontFamily: display, fontWeight: 900, fontSize: m.text.length > 14 ? 62 : 80, color: C, letterSpacing: -1 }}>{m.text}</div>
            </div>
          );
        })}
      </div>

      {/* Leaderboard */}
      <AbsoluteFill style={{ clipPath: `inset(0 0 0 ${Math.max(0, lineX - 70)}px)` }}>
        {TEAMS.map((team, t) => {
          const { pos, moved } = rankAt(t, f);
          const yRow = TOP + pos * (ROW_H + ROW_GAP);
          const y = interpolate(converge, [0, 1], [yRow, 960 - ROW_H / 2]);
          const nameIdx = stage;
          const nameChange = prog(f, STAGES[stage], 10);
          const flash = moved > 0 ? A.green : moved < 0 ? A.red : null;
          return (
            <div
              key={team.lab}
              style={{
                position: "absolute",
                left: 70,
                right: 70,
                top: y,
                height: ROW_H,
                background: flash ? `${flash}33` : "#101119",
                border: `2px solid ${flash ?? "#262838"}`,
                borderRadius: 10,
                display: "flex",
                alignItems: "center",
                transform: `scaleY(${1 - squash * 0.95}) scaleX(${1 - shrink})`,
                opacity: prog(f, 8 + t * 3, 8),
              }}
            >
              <div style={{ width: 96, textAlign: "center", fontFamily: mono, fontWeight: 800, fontSize: 56, color: Math.round(pos) === 0 ? A.red : A.text }}>{Math.round(pos) + 1}</div>
              <div style={{ width: 12, alignSelf: "stretch", background: team.color }} />
              <div style={{ flex: 1, paddingLeft: 30, height: 64, overflow: "hidden", position: "relative" }}>
                {team.names.map((n, i) =>
                  i === nameIdx || (i === nameIdx - 1 && nameChange < 1) ? (
                    <div
                      key={n}
                      style={{
                        position: "absolute",
                        fontFamily: mono,
                        fontWeight: 800,
                        fontSize: 50,
                        color: A.text,
                        whiteSpace: "nowrap",
                        transform: `translateY(${i === nameIdx ? (stage === 0 ? 0 : (1 - nameChange) * 110) : -nameChange * 110}%)`,
                      }}
                    >
                      {n}
                    </div>
                  ) : null,
                )}
              </div>
              <div style={{ width: 230, paddingRight: 28, textAlign: "right", fontFamily: body, fontSize: 30, color: A.grey }}>{team.lab}</div>
            </div>
          );
        })}
      </AbsoluteFill>

      {/* Start line sweeping left to reveal the grid */}
      {f < 40 ? <div style={{ position: "absolute", left: lineX - 4, top: 0, width: 8, height: 1920, background: C, boxShadow: `0 0 40px ${C}`, opacity: 1 - prog(f, 28, 10) }} /> : null}

      {/* What the models do now: four live inserts, one highlighted per beat */}
      <div style={{ position: "absolute", left: 70, right: 70, top: TOP + N * (ROW_H + ROW_GAP) + 40, display: "flex", gap: 18, opacity: prog(f, 40, 12) * (1 - extrasOut) }}>
        {INSERTS.map((label, i) => {
          const active = Math.floor(f / 15) % 4 === i;
          return (
            <div key={label} style={{ flex: 1, height: 230, borderRadius: 16, background: "#0e0f16", border: `3px solid ${active ? A.red : "#262838"}`, position: "relative", overflow: "hidden", transform: `scale(${active ? 1.06 : 1})` }}>
              <Insert kind={i} f={f} />
              <div style={{ position: "absolute", left: 0, right: 0, bottom: 0, padding: "8px 14px", background: "#000a", fontFamily: mono, fontSize: 24, color: active ? A.red : A.grey }}>{label}</div>
            </div>
          );
        })}
      </div>
      <div style={{ position: "absolute", top: 1810, width: 1080, textAlign: "center", fontFamily: body, fontSize: 26, color: A.grey, opacity: prog(f, 60, 10) * (1 - extrasOut) }}>
        порядок на табло — образ гонки, а не рейтинг
      </div>

      {/* All rows collapse into one point */}
      {f >= 334 ? (
        <div style={{ position: "absolute", left: 540 - 14 * pulse, top: 960 - 14 * pulse, width: 28 * pulse, height: 28 * pulse, borderRadius: "50%", background: "#fff", boxShadow: `0 0 ${60 * pulse}px ${C}`, opacity: prog(f, 340, 4) }} />
      ) : null}
      {white > 0 ? <AbsoluteFill style={{ background: C, opacity: white }} /> : null}
    </AbsoluteFill>
  );
};
