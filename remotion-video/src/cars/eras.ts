import { Easing, interpolate } from "remotion";

// One morphable car. Every era is a set of numeric parameters; the video
// interpolates between neighbouring eras, so the same model continuously
// transforms from a lone wheel into a modern EV.
export type CarParams = {
  bodyScale: number;
  xNose: number;
  xTail: number;
  bottom: number;
  noseH: number;
  hoodH: number;
  xWs: number;
  roofH: number;
  xRf: number;
  xRr: number;
  xRw: number;
  deckH: number;
  tailH: number;
  width: number;
  smooth: number;
  glass: number;
  metalness: number;
  roughness: number;
  xF: number;
  xR: number;
  rF: number;
  rR: number;
  tireT: number;
  track: number;
  solid: number;
  headlights: number;
  lightBar: number;
  speed: number;
};

export type Era = {
  year: number;
  title: string;
  fact: string;
  bodyColor: string;
  wheelColor: string;
  tireColor: string;
  accent: string;
  bg: string;
  spokes: number;
  cam: { az: number; el: number; dist: number; lookY: number };
  p: CarParams;
};

const cart: CarParams = {
  bodyScale: 1,
  xNose: 1.4,
  xTail: -1.2,
  bottom: 0.75,
  noseH: 1.8,
  hoodH: 1.1,
  xWs: 0.9,
  roofH: 1.1,
  xRf: 0.5,
  xRr: -0.6,
  xRw: -0.9,
  deckH: 1.1,
  tailH: 1.1,
  width: 1.4,
  smooth: 0.1,
  glass: 0,
  metalness: 0,
  roughness: 0.85,
  xF: 0.75,
  xR: -0.7,
  rF: 0.75,
  rR: 0.75,
  tireT: 0.1,
  track: 0.85,
  solid: 0,
  headlights: 0,
  lightBar: 0,
  speed: 0.05,
};

export const ERAS: Era[] = [
  {
    year: -3500,
    title: "Колесо",
    fact: "Месопотамия. Первые колёса — сплошные деревянные диски",
    bodyColor: "#8a5a2b",
    wheelColor: "#7a5230",
    tireColor: "#5a3a1c",
    accent: "#f0b46a",
    bg: "#1d140c",
    spokes: 0,
    cam: { az: 0.35, el: 0.12, dist: 9.5, lookY: 1.2 },
    p: {
      ...cart,
      bodyScale: 0,
      xF: 0,
      xR: 0,
      rF: 1.15,
      rR: 1.15,
      tireT: 0.24,
      track: 0,
      solid: 1,
      speed: 0.03,
    },
  },
  {
    year: -2000,
    title: "Колесница",
    fact: "Спицы сделали колесо лёгким, а повозку — быстрой",
    bodyColor: "#8a5a2b",
    wheelColor: "#b07a40",
    tireColor: "#5a3a1c",
    accent: "#e2a04f",
    bg: "#20160d",
    spokes: 6,
    cam: { az: 0.65, el: 0.3, dist: 11, lookY: 1.0 },
    p: cart,
  },
  {
    year: 1886,
    title: "Motorwagen",
    fact: "Карл Бенц патентует первый автомобиль с бензиновым мотором",
    bodyColor: "#1e1e1e",
    wheelColor: "#e2d3a8",
    tireColor: "#222222",
    accent: "#d9c38a",
    bg: "#17130f",
    spokes: 12,
    cam: { az: -0.55, el: 0.22, dist: 10, lookY: 0.9 },
    p: {
      ...cart,
      xNose: 1.3,
      xTail: -1.3,
      bottom: 0.55,
      noseH: 0.75,
      hoodH: 0.85,
      xWs: 0.0,
      roofH: 1.5,
      xRf: -0.3,
      xRr: -0.75,
      xRw: -0.9,
      deckH: 1.0,
      tailH: 0.9,
      width: 1.3,
      smooth: 0.2,
      metalness: 0.3,
      roughness: 0.5,
      xF: 1.0,
      xR: -0.8,
      rF: 0.45,
      rR: 0.7,
      tireT: 0.06,
      track: 0.75,
      speed: 0.08,
    },
  },
  {
    year: 1908,
    title: "Ford Model T",
    fact: "Конвейер сделал автомобиль доступным для миллионов",
    bodyColor: "#141414",
    wheelColor: "#c8a26b",
    tireColor: "#1a1a1a",
    accent: "#c9d2e0",
    bg: "#121519",
    spokes: 12,
    cam: { az: 0.9, el: 0.33, dist: 12, lookY: 1.0 },
    p: {
      ...cart,
      xNose: 1.9,
      xTail: -1.7,
      bottom: 0.5,
      noseH: 1.15,
      hoodH: 1.2,
      xWs: 0.55,
      roofH: 2.1,
      xRf: 0.45,
      xRr: -1.35,
      xRw: -1.55,
      deckH: 1.3,
      tailH: 1.2,
      width: 1.6,
      smooth: 0.15,
      glass: 1,
      metalness: 0.5,
      roughness: 0.3,
      xF: 1.25,
      xR: -1.15,
      rF: 0.55,
      rR: 0.55,
      tireT: 0.12,
      track: 0.85,
      headlights: 0.6,
      speed: 0.12,
    },
  },
  {
    year: 1955,
    title: "Эпоха хрома",
    fact: "Обтекаемые кузова, хвостовые плавники и блеск хрома",
    bodyColor: "#3fb7b0",
    wheelColor: "#e6e6e6",
    tireColor: "#161616",
    accent: "#5ce0d0",
    bg: "#1b1330",
    spokes: 0,
    cam: { az: -0.85, el: 0.25, dist: 12.5, lookY: 0.8 },
    p: {
      ...cart,
      xNose: 2.5,
      xTail: -2.5,
      bottom: 0.35,
      noseH: 0.95,
      hoodH: 1.05,
      xWs: 0.6,
      roofH: 1.55,
      xRf: 0.2,
      xRr: -0.9,
      xRw: -1.4,
      deckH: 1.05,
      tailH: 1.15,
      width: 1.9,
      smooth: 0.85,
      glass: 1,
      metalness: 0.6,
      roughness: 0.22,
      xF: 1.55,
      xR: -1.55,
      rF: 0.42,
      rR: 0.42,
      tireT: 0.13,
      track: 0.95,
      solid: 0.9,
      headlights: 1,
      speed: 0.18,
    },
  },
  {
    year: 1969,
    title: "Маслкары",
    fact: "Мотор V8, рёв выхлопа и культ скорости",
    bodyColor: "#e0601f",
    wheelColor: "#d0d0d0",
    tireColor: "#121212",
    accent: "#ff8a3d",
    bg: "#250c0a",
    spokes: 5,
    cam: { az: 0.45, el: 0.06, dist: 11, lookY: 0.7 },
    p: {
      ...cart,
      xNose: 2.45,
      xTail: -2.4,
      bottom: 0.3,
      noseH: 0.85,
      hoodH: 0.95,
      xWs: 0.45,
      roofH: 1.32,
      xRf: -0.05,
      xRr: -0.95,
      xRw: -1.6,
      deckH: 0.98,
      tailH: 0.92,
      width: 1.95,
      smooth: 0.5,
      glass: 1,
      metalness: 0.55,
      roughness: 0.28,
      xF: 1.5,
      xR: -1.45,
      rF: 0.45,
      rR: 0.47,
      tireT: 0.16,
      track: 1.0,
      solid: 0.4,
      headlights: 1,
      speed: 0.26,
    },
  },
  {
    year: 1984,
    title: "Эпоха клина",
    fact: "Острые грани и суперкары с плакатов на стене",
    bodyColor: "#ff3355",
    wheelColor: "#c9c9d6",
    tireColor: "#111111",
    accent: "#ff3ea5",
    bg: "#0e0b2a",
    spokes: 5,
    cam: { az: -1.15, el: 0.5, dist: 12, lookY: 0.6 },
    p: {
      ...cart,
      xNose: 2.25,
      xTail: -2.15,
      bottom: 0.22,
      noseH: 0.5,
      hoodH: 0.85,
      xWs: 0.55,
      roofH: 1.12,
      xRf: -0.15,
      xRr: -0.75,
      xRw: -1.6,
      deckH: 0.95,
      tailH: 0.9,
      width: 2.0,
      smooth: 0.03,
      glass: 1,
      metalness: 0.5,
      roughness: 0.25,
      xF: 1.4,
      xR: -1.35,
      rF: 0.42,
      rR: 0.44,
      tireT: 0.17,
      track: 1.0,
      solid: 0.5,
      headlights: 1,
      speed: 0.34,
    },
  },
  {
    year: 2025,
    title: "Электромобиль",
    fact: "Тихий электромотор, автопилот и идеальная аэродинамика",
    bodyColor: "#dfe9f0",
    wheelColor: "#2a2f36",
    tireColor: "#0d0d0d",
    accent: "#3ee6ff",
    bg: "#03121a",
    spokes: 5,
    cam: { az: 0.7, el: 0.12, dist: 10.5, lookY: 0.7 },
    p: {
      ...cart,
      xNose: 2.35,
      xTail: -2.3,
      bottom: 0.28,
      noseH: 0.6,
      hoodH: 0.85,
      xWs: 0.95,
      roofH: 1.4,
      xRf: 0.15,
      xRr: -0.9,
      xRw: -2.0,
      deckH: 1.0,
      tailH: 0.95,
      width: 2.0,
      smooth: 1,
      glass: 1,
      metalness: 0.7,
      roughness: 0.14,
      xF: 1.5,
      xR: -1.45,
      rF: 0.48,
      rR: 0.48,
      tireT: 0.15,
      track: 1.0,
      solid: 0.85,
      headlights: 0.4,
      lightBar: 1,
      speed: 0.42,
    },
  },
];

// Timeline
export const INTRO = 50;
export const HOLD = 100;
export const MORPH = 40;
export const SEGMENT = HOLD + MORPH;
export const OUTRO = 200;
export const LAST_ARRIVAL = INTRO + (ERAS.length - 1) * SEGMENT;
export const CAR_HISTORY_DURATION = LAST_ARRIVAL + OUTRO;

const morphEase = Easing.bezier(0.65, 0, 0.35, 1);

// Continuous era position: integer while holding an era, fractional while morphing.
export const eraPosition = (frame: number): number => {
  const g = frame - INTRO;
  if (g < 0) return 0;
  const seg = Math.floor(g / SEGMENT);
  if (seg >= ERAS.length - 1) return ERAS.length - 1;
  const within = g - seg * SEGMENT;
  if (within < HOLD) return seg;
  return seg + morphEase((within - HOLD) / MORPH);
};

// Raw (un-eased) morph progress 0..1, or -1 when not morphing.
export const morphProgress = (frame: number): number => {
  const g = frame - INTRO;
  if (g < 0) return -1;
  const seg = Math.floor(g / SEGMENT);
  if (seg >= ERAS.length - 1) return -1;
  const within = g - seg * SEGMENT;
  return within < HOLD ? -1 : (within - HOLD) / MORPH;
};

export const eraArrival = (index: number) => (index === 0 ? INTRO : INTRO + index * SEGMENT);
export const eraExit = (index: number) => (index === ERAS.length - 1 ? Infinity : eraArrival(index) + HOLD);

export const lerp = (a: number, b: number, t: number) => a + (b - a) * t;

export const paramsAt = (e: number): CarParams => {
  const i = Math.min(Math.floor(e), ERAS.length - 2);
  const t = e - i;
  const a = ERAS[i].p;
  const b = ERAS[i + 1].p;
  const out = {} as CarParams;
  (Object.keys(a) as (keyof CarParams)[]).forEach((k) => {
    out[k] = lerp(a[k], b[k], t);
  });
  return out;
};

export const pickAt = <T>(e: number, get: (era: Era) => T): [T, T, number] => {
  const i = Math.min(Math.floor(e), ERAS.length - 2);
  return [get(ERAS[i]), get(ERAS[i + 1]), e - i];
};

export const clamp01 = (v: number) => interpolate(v, [0, 1], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });

export const formatYear = (y: number) => (y < 0 ? `${Math.abs(y)} до н.э.` : `${y}`);
