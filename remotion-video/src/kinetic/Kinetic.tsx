import React from "react";
import { Audio } from "@remotion/media";
import { AbsoluteFill, random, Sequence, staticFile, useCurrentFrame } from "remotion";
import { K } from "./fx";
import { S0, S1, S2, S3, S4, S5, S6, S7, S8, type SceneProps } from "./scenesA";
import { S9, S10, S11, S12, S13, S14 } from "./scenesB";
import { PHRASES, TOTAL } from "./vo";

export const KINETIC_DURATION = TOTAL;
const SCENES: React.FC<SceneProps>[] = [S0, S1, S2, S3, S4, S5, S6, S7, S8, S9, S10, S11, S12, S13, S14];

const sfx = (name: string) => staticFile(`airace/sfx/${name}`);
const PUNCH = "impactPunch_heavy_000.ogg";
const BOOM = "lowFrequency_explosion_000.ogg";
const CRUNCH = "explosionCrunch_000.ogg";

// Sound per phrase: [word index or "+frame", file, volume]
const CUES: [number | string, string, number][][] = [
  [[0, PUNCH, 0.7], [1, "whip.wav", 0.6], [2, PUNCH, 0.8], ["+92", BOOM, 0.8]],
  [[0, PUNCH, 0.6], ["+30", "select_002.ogg", 0.4], ["+54", "select_002.ogg", 0.4], [1, BOOM, 0.7]],
  [[0, "click_002.ogg", 0.5], [1, PUNCH, 0.7], [2, "whip.wav", 0.6], [3, PUNCH, 0.8]],
  [[0, "click_002.ogg", 0.4], [4, PUNCH, 0.8], [4, "whoosh.wav", 0.6]],
  [[0, "whip.wav", 0.6], [2, PUNCH, 0.8], ["s3+30", "glitch_001.ogg", 0.8], ["s3+33", "glitch_004.ogg", 0.8], ["s3+36", "error_004.ogg", 0.5]],
  [["s1-4", "forceField_000.ogg", 0.6], [1, BOOM, 0.8], [3, PUNCH, 0.7]],
  [[0, "whoosh.wav", 0.5], [3, PUNCH, 0.7]],
  [[2, "maximize_006.ogg", 0.5], [5, "whoosh.wav", 0.6], [5, PUNCH, 0.8]],
  [[0, "click_002.ogg", 0.4], [3, BOOM, 0.9], [4, CRUNCH, 0.7]],
  [[0, "whoosh.wav", 0.5], [1, PUNCH, 0.8], [2, "whip.wav", 0.6], ["s3-10", "forceField_000.ogg", 0.5], [3, PUNCH, 0.7], [4, PUNCH, 0.8], [5, "impactMetal_heavy_000.ogg", 0.6], [6, "whoosh.wav", 0.6], [7, PUNCH, 0.7]],
  [[0, PUNCH, 0.7], [1, "whip.wav", 0.7], [2, "whoosh.wav", 0.6], [3, "whoosh.wav", 0.6], [4, "whoosh.wav", 0.6], [5, "whoosh.wav", 0.6]],
  [[1, BOOM, 0.9], [1, "glitch_003.ogg", 0.7]],
  [[5, "switch_003.ogg", 0.5], [6, "tick_002.ogg", 0.5], [7, "phaserUp1.ogg", 0.4], [10, PUNCH, 0.7], ["s10+6", "computerNoise_000.ogg", 0.25]],
  [[1, "whip.wav", 0.7], [1, PUNCH, 0.7], [2, "confirmation_002.ogg", 0.5], [3, "confirmation_002.ogg", 0.5], [4, "confirmation_002.ogg", 0.5], ["s4+40", "powerUp2.ogg", 0.5]],
  [[3, BOOM, 0.9], [3, CRUNCH, 0.7], ["+146", "forceField_000.ogg", 0.5]],
];

const resolve = (phrase: (typeof PHRASES)[number], key: number | string) => {
  if (typeof key === "number") return phrase.words[key].f;
  const m = key.match(/^s(\d+)([+-]\d+)$/);
  if (m) return phrase.words[Number(m[1])].f + Number(m[2]);
  return Number(key);
};

// Typing clicks while the prompt is being typed (phrase 7)
const typing = (() => {
  const ph = PHRASES[6];
  const start = ph.start + ph.words[2].f + 6;
  return new Array(16).fill(0).map((_, i) => start + i * 6 + Math.floor(random(`ty${i}`) * 3));
})();

const Chroma: React.FC = () => {
  const f = useCurrentFrame();
  const cut = PHRASES.some((ph) => f - ph.start >= 0 && f - ph.start < 5);
  return cut ? <AbsoluteFill style={{ boxShadow: "inset 30px 0 80px rgba(255,0,60,0.5), inset -30px 0 80px rgba(0,220,255,0.4)" }} /> : null;
};

const Grain: React.FC = () => {
  const f = useCurrentFrame();
  return (
    <svg width={1080} height={1920} style={{ position: "absolute", inset: 0, opacity: 0.09, mixBlendMode: "overlay" }}>
      <filter id="kgrain">
        <feTurbulence type="fractalNoise" baseFrequency="0.9" numOctaves="2" seed={f % 10} />
      </filter>
      <rect width={1080} height={1920} filter="url(#kgrain)" />
    </svg>
  );
};

export const Kinetic: React.FC = () => (
  <AbsoluteFill style={{ background: K.black }}>
    {PHRASES.map((ph, i) => {
      const Scene = SCENES[i];
      const next = PHRASES[i + 1]?.start ?? TOTAL;
      return (
        <Sequence key={ph.id} name={`${i + 1} ${ph.id}`} from={i === 0 ? 0 : ph.start} durationInFrames={next - (i === 0 ? 0 : ph.start)}>
          <Sequence from={i === 0 ? ph.start : 0} layout="none">
            <Scene w={ph.words} len={ph.len} />
          </Sequence>
        </Sequence>
      );
    })}
    <Chroma />
    <Grain />
    <AbsoluteFill style={{ background: "radial-gradient(ellipse at center, transparent 60%, rgba(0,0,0,0.45) 100%)", pointerEvents: "none" }} />

    <Audio src={staticFile("kinetic/music.mp3")} volume={0.32} />
    {PHRASES.map((ph) => (
      <Sequence key={ph.id} from={ph.start} layout="none">
        <Audio src={staticFile(`kinetic/vo/${ph.id}.mp3`)} volume={1} />
      </Sequence>
    ))}
    {PHRASES.flatMap((ph, i) =>
      CUES[i].map(([key, file, vol], j) => (
        <Sequence key={`${i}-${j}`} from={Math.max(0, ph.start + resolve(ph, key))} layout="none">
          <Audio src={sfx(file)} volume={vol} />
        </Sequence>
      )),
    )}
    {typing.map((at, i) => (
      <Sequence key={`t${i}`} from={at} layout="none">
        <Audio src={sfx("click_002.ogg")} volume={0.3} />
      </Sequence>
    ))}
  </AbsoluteFill>
);
