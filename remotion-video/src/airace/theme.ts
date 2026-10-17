import "@fontsource-variable/jetbrains-mono";
import { interpolateColors } from "remotion";

export { body, display, E, prog, mix } from "../carstory/kit";

export const mono = "'JetBrains Mono Variable', monospace";
export const BG = "#07070C";

export const A = {
  blue: "#3D7BFF",
  ice: "#9FB4C8",
  violet: "#9B5CFF",
  green: "#39FF88",
  warm: "#F5F1E8",
  red: "#FF3B1F",
  grey: "#7C8091",
  text: "#EEF1F8",
};

// Scene start frames — all on the 15-frame beat grid (120 BPM @ 30 fps).
export const T = {
  s1: 0,
  s2: 150,
  s3: 390,
  s4: 600,
  s5: 750,
  s6: 990,
  s7: 1290,
  s8: 1470,
  s9: 1740,
  s10: 2100,
  end: 2250,
};

export const SCENE_STARTS = [T.s1, T.s2, T.s3, T.s4, T.s5, T.s6, T.s7, T.s8, T.s9, T.s10];

// Big hits: shake + flash + chromatic split.
export const HITS = [90, 750, 930, 1230, 1320, 1410, 1740, 2100, 2220];

// The accent colour is the navigation: it tells which era we are in.
export const accentAt = (f: number) =>
  interpolateColors(
    f,
    [0, 590, 610, 740, 760, 980, 1000, 1280, 1300],
    [A.blue, A.blue, A.ice, A.ice, A.violet, A.violet, A.green, A.green, A.warm],
  );
