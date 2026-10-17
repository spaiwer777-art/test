// Side-view silhouettes in a 600×220 box, nose pointing right, ground at y=200.
// Bodies share wheel-arch construction so flubber can morph between them cleanly.
export type Silhouette = {
  year: number;
  name: string;
  body: string;
  glass: string;
  wheels: { x: number; r: number }[];
  color: string;
};

export const CARS: Silhouette[] = [
  {
    year: 1908,
    name: "Ford Model T",
    body: "M70,160 L70,108 L92,104 L96,42 L300,42 L312,98 L470,102 L515,104 L520,160 L491,160 A46 46 0 0 0 399,160 L191,160 A46 46 0 0 0 99,160 Z",
    glass: "M112,54 L286,54 L296,96 L112,100 Z",
    wheels: [
      { x: 145, r: 40 },
      { x: 445, r: 40 },
    ],
    color: "#1b1b1f",
  },
  {
    year: 1955,
    name: "Эпоха хрома",
    body: "M30,165 L30,122 L20,98 L84,106 Q150,100 190,96 L235,62 Q250,54 300,54 L360,56 Q380,58 400,96 L520,104 Q565,108 572,130 L572,165 L510,165 A40 40 0 0 0 430,165 L170,165 A40 40 0 0 0 90,165 Z",
    glass: "M204,94 L242,66 L300,62 L352,64 L382,96 Z",
    wheels: [
      { x: 130, r: 34 },
      { x: 470, r: 34 },
    ],
    color: "#34b3a8",
  },
  {
    year: 1969,
    name: "Маслкар",
    body: "M28,164 L28,122 L45,112 L160,104 L215,68 Q230,60 270,60 L330,62 Q345,64 370,98 L540,108 Q568,112 572,128 L574,164 L505,164 A40 40 0 0 0 425,164 L165,164 A40 40 0 0 0 85,164 Z",
    glass: "M182,102 L222,72 L328,70 L354,100 Z",
    wheels: [
      { x: 125, r: 36 },
      { x: 465, r: 36 },
    ],
    color: "#ff6a1f",
  },
  {
    year: 1984,
    name: "Суперкар-клин",
    body: "M30,166 L30,118 L120,108 L230,62 L330,62 L560,128 L575,140 L575,166 L493,166 A38 38 0 0 0 417,166 L168,166 A38 38 0 0 0 92,166 Z",
    glass: "M150,106 L236,70 L324,70 L410,94 Z",
    wheels: [
      { x: 130, r: 34 },
      { x: 455, r: 34 },
    ],
    color: "#ff2d55",
  },
  {
    year: 2025,
    name: "Электромобиль",
    body: "M30,162 L30,120 Q32,104 60,100 L140,92 Q200,52 290,50 Q380,52 440,96 L540,110 Q575,116 578,140 L578,162 L502,162 A42 42 0 0 0 418,162 L172,162 A42 42 0 0 0 88,162 Z",
    glass: "M162,90 Q212,62 290,60 Q368,62 418,92 Z",
    wheels: [
      { x: 130, r: 38 },
      { x: 460, r: 38 },
    ],
    color: "#2f6bff",
  },
];
