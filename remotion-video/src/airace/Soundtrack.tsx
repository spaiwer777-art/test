import React from "react";
import { Audio } from "@remotion/media";
import { interpolate, Sequence, staticFile } from "remotion";
import { T } from "./theme";

// Narration: [file, global start frame, length in frames]
const VO: [string, number, number][] = [
  ["01", 8, 131],
  ["02", 155, 240],
  ["03", 398, 189],
  ["04", 612, 122],
  ["05", 762, 202],
  ["06", 1000, 248],
  ["07", 1302, 159],
  ["08", 1480, 185],
  ["09", 1752, 303],
  ["10", 2110, 126],
];

const OVERTAKES = [30, 60, 90, 120, 150, 180, 210, 240, 270, 285, 300].map((f) => T.s9 + f);

// Existing CC0 sounds from Kenney (kenney.nl) plus Remotion's whoosh/whip: [file, frame, volume]
const SFX: [string, number, number][] = [
  // 1 — start
  ...[4, 8, 12, 16, 20, 24].map((f) => ["click_002.ogg", f, 0.35] as [string, number, number]),
  ...[30, 42, 54, 66, 78].map((f) => ["tone1.ogg", f, 0.45] as [string, number, number]),
  ["lowFrequency_explosion_000.ogg", 90, 0.9],
  ["impactPunch_heavy_000.ogg", 90, 0.7],
  ["whoosh.wav", 116, 0.6],
  ["whip.wav", 140, 0.6],
  // 2 — the idea
  ["select_002.ogg", 154, 0.4],
  ["forceField_000.ogg", 214, 0.35],
  ["impactMetal_heavy_000.ogg", 318, 0.7],
  ["maximize_006.ogg", 324, 0.45],
  ["whoosh.wav", 368, 0.6],
  ["whip.wav", 376, 0.5],
  // 3 — first laps
  ...[396, 404, 412, 420].map((f) => ["tick_002.ogg", f, 0.35] as [string, number, number]),
  ["confirmation_002.ogg", 440, 0.45],
  ["switch_003.ogg", 480, 0.45],
  ["glitch_001.ogg", 570, 0.7],
  ["glitch_003.ogg", 574, 0.7],
  ["glitch_004.ogg", 579, 0.7],
  ["error_004.ogg", 584, 0.5],
  // 4 — winter
  ["computerNoise_000.ogg", 602, 0.25],
  ["pepSound1.ogg", 690, 0.4],
  ["pepSound1.ogg", 706, 0.4],
  ["explosionCrunch_002.ogg", 690, 0.55],
  ["impactGlass_heavy_000.ogg", 728, 0.8],
  ["impactGlass_heavy_002.ogg", 750, 0.9],
  ["explosionCrunch_000.ogg", 750, 0.6],
  ["lowFrequency_explosion_001.ogg", 750, 0.7],
  // 5 — the comeback
  ["laserRetro_002.ogg", 758, 0.35],
  ["zapTwoTone.ogg", 790, 0.35],
  ["laserRetro_002.ogg", 820, 0.35],
  ["zapTwoTone.ogg", 842, 0.35],
  ["whoosh.wav", 866, 0.55],
  ["whip.wav", 914, 0.55],
  ["impactWood_heavy_000.ogg", 930, 0.9],
  ["lowFrequency_explosion_000.ogg", 930, 0.7],
  ["whoosh.wav", 962, 0.5],
  // 6 — deep learning
  ["forceField_000.ogg", 1002, 0.45],
  ["phaserUp1.ogg", 1114, 0.5],
  ["highUp.ogg", 1150, 0.4],
  ...[1202, 1208, 1214, 1220, 1226, 1232, 1238, 1244].map((f) => ["impactPlate_light_000.ogg", f, 0.55] as [string, number, number]),
  ["lowFrequency_explosion_000.ogg", 1230, 0.8],
  ["whoosh.wav", 1262, 0.5],
  // 7 — transformer
  ["whoosh.wav", 1292, 0.5],
  ["impactPunch_heavy_000.ogg", 1320, 0.8],
  ...[1348, 1354, 1360, 1366, 1372, 1378, 1384, 1390, 1396, 1402].map((f) => ["select_002.ogg", f, 0.3] as [string, number, number]),
  ["spaceEngineLarge_000.ogg", 1408, 0.7],
  ["lowFrequency_explosion_001.ogg", 1410, 0.75],
  ["thrusterFire_000.ogg", 1440, 0.6],
  ["whip.wav", 1452, 0.6],
  // 8 — scale
  ["tick_001.ogg", 1478, 0.5],
  ["tick_001.ogg", 1494, 0.5],
  ["phaserUp3.ogg", 1516, 0.5],
  ["whoosh.wav", 1562, 0.6],
  ["whip.wav", 1588, 0.55],
  ["whip.wav", 1614, 0.55],
  ["switch_003.ogg", 1640, 0.45],
  ["engineCircular_000.ogg", 1664, 0.35],
  ["confirmation_002.ogg", 1684, 0.5],
  ["powerUp2.ogg", 1704, 0.5],
  ["whoosh.wav", 1716, 0.6],
  // 9 — flagships (drop)
  ["lowFrequency_explosion_000.ogg", 1740, 0.9],
  ["explosionCrunch_000.ogg", 1740, 0.6],
  ...OVERTAKES.map((f) => ["laserLarge_000.ogg", f, 0.3] as [string, number, number]),
  ...[1764, 1836, 1908, 1976].map((f) => ["maximize_006.ogg", f, 0.4] as [string, number, number]),
  ["forceField_000.ogg", 2040, 0.5],
  ["phaserUp1.ogg", 2074, 0.5],
  // morph wipes
  ["forceField_000.ogg", 378, 0.45],
  ["forceField_000.ogg", 588, 0.45],
  ["forceField_000.ogg", 1458, 0.45],
  // 10 — finish
  ["lowFrequency_explosion_001.ogg", 2100, 0.9],
  ["explosionCrunch_000.ogg", 2100, 0.6],
  ["confirmation_002.ogg", 2158, 0.4],
  ["lowFrequency_explosion_000.ogg", 2220, 0.8],
];

const DUCK = 8;
const speaking = (f: number) => {
  let s = 0;
  for (const [, start, len] of VO) {
    s = Math.max(s, interpolate(f, [start - DUCK, start, start + len, start + len + DUCK], [0, 1, 1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
  }
  return s;
};
const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
// Winter: the full mix crossfades into a muffled copy of the same track.
const winter = (f: number) => interpolate(f, [T.s4, T.s4 + 15, T.s5 - 8, T.s5], [0, 1, 1, 0], clamp);
// A short gap before the drop at the start of scene 9.
const gap = (f: number) => interpolate(f, [T.s9 - 30, T.s9 - 22, T.s9 - 1, T.s9], [1, 0.12, 0.12, 1], clamp);
const outro = (f: number) => interpolate(f, [T.end - 40, T.end], [1, 0], clamp);
const base = (f: number) => interpolate(speaking(f), [0, 1], [0.6, 0.28]) * (f >= T.s9 && f < T.s10 ? 1.25 : 1);

export const Soundtrack: React.FC = () => (
  <>
    <Audio src={staticFile("airace/music.mp3")} volume={(f) => base(f) * (1 - winter(f)) * gap(f) * outro(f)} />
    <Audio src={staticFile("airace/music_muffled.mp3")} volume={(f) => base(f) * winter(f)} />
    {VO.map(([file, start]) => (
      <Sequence key={file} name={`VO ${file}`} from={start} layout="none">
        <Audio src={staticFile(`airace/vo/${file}.mp3`)} volume={1} />
      </Sequence>
    ))}
    {SFX.map(([file, at, vol], i) => (
      <Sequence key={i} name={`SFX ${file}`} from={at} layout="none">
        <Audio src={staticFile(`airace/sfx/${file}`)} volume={vol} />
      </Sequence>
    ))}
  </>
);
