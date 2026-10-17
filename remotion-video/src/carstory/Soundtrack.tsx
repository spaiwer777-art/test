import React from "react";
import { Audio } from "@remotion/media";
import { interpolate, Sequence, staticFile } from "remotion";

// Voice-over lines: [file, start frame, length in frames]
const VO: [string, number, number][] = [
  ["01", 6, 94],
  ["02", 102, 71],
  ["03", 176, 154],
  ["05", 366, 171],
  ["06", 542, 126],
  ["07", 692, 109],
  ["08", 888, 168],
  ["09", 1066, 174],
  ["10", 1278, 175],
  ["11", 1468, 115],
];

// Sound effects: [file, frame, volume]
const SFX: [string, number, number][] = [
  ["whip", 0, 0.5],
  ["boom", 9, 0.7],
  ["swoosh", 44, 0.4],
  ["switch", 48, 0.3],
  ["riser", 72, 0.45],
  ["swoosh", 98, 0.5],
  ["boom", 110, 0.75],
  ["tick", 140, 0.5],
  ["tick", 154, 0.5],
  ["swoosh", 208, 0.5],
  ["whip", 249, 0.55],
  ["whip", 285, 0.55],
  ["whip", 321, 0.55],
  ["swoosh", 350, 0.6],
  ["tick", 417, 0.45],
  ["tick", 423, 0.45],
  ["tick", 429, 0.45],
  ["tick", 435, 0.45],
  ["swoosh", 488, 0.55],
  ["boom", 506, 0.6],
  ["swoosh", 514, 0.5],
  ["switch", 542, 0.3],
  ["switch", 548, 0.3],
  ["riser", 532, 0.25],
  ["ding", 612, 0.3],
  ["swoosh", 668, 0.5],
  ["tick", 676, 0.4],
  ["boom", 774, 0.65],
  ["boom", 845, 0.6],
  ["riser", 826, 0.35],
  ["swoosh", 878, 0.55],
  ["tick", 923, 0.5],
  ["tick", 949, 0.5],
  ["tick", 975, 0.5],
  ["tick", 1001, 0.5],
  ["boom", 1027, 0.7],
  ["glitch", 1035, 0.55],
  ["swoosh", 1050, 0.5],
  ["swoosh", 1090, 0.45],
  ["swoosh", 1130, 0.45],
  ["swoosh", 1170, 0.45],
  ["swoosh", 1210, 0.45],
  ["whip", 1250, 0.6],
  ["zap", 1284, 0.6],
  ["glitch", 1288, 0.3],
  ["tick", 1312, 0.4],
  ["tick", 1318, 0.4],
  ["tick", 1324, 0.4],
  ["tick", 1330, 0.4],
  ["tick", 1336, 0.4],
  ["riser", 1404, 0.45],
  ["boom", 1450, 0.7],
  ["switch", 1464, 0.3],
  ["swoosh", 1542, 0.45],
  ["boom", 1558, 0.5],
];

const DUCK = 8;

// Music sits lower whenever the narrator speaks, with short ramps.
const musicVolume = (f: number) => {
  let speaking = 0;
  for (const [, start, len] of VO) {
    const k = interpolate(f, [start - DUCK, start, start + len, start + len + DUCK], [0, 1, 1, 0], {
      extrapolateLeft: "clamp",
      extrapolateRight: "clamp",
    });
    speaking = Math.max(speaking, k);
  }
  return interpolate(speaking, [0, 1], [0.32, 0.13]);
};

export const Soundtrack: React.FC = () => (
  <>
    <Audio src={staticFile("sfx/music.mp3")} volume={musicVolume} />
    {VO.map(([file, start]) => (
      <Sequence key={file} name={`VO ${file}`} from={start} layout="none">
        <Audio src={staticFile(`vo/${file}.mp3`)} volume={1} />
      </Sequence>
    ))}
    {SFX.map(([file, at, vol], i) => (
      <Sequence key={i} name={`SFX ${file}`} from={at} layout="none">
        <Audio src={staticFile(`sfx/${file}.wav`)} volume={vol} />
      </Sequence>
    ))}
  </>
);
