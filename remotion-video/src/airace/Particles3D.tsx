import React, { useMemo } from "react";
import { useThree } from "@react-three/fiber";
import { ThreeCanvas } from "@remotion/three";
import * as THREE from "three";
import { interpolate, random, useCurrentFrame, useVideoConfig } from "remotion";
import { accentAt, E, SCENE_STARTS, T } from "./theme";

const N = 1800;
const MORPH = 30; // frames, centred on each scene cut

type Shape = Float32Array;
const make = (fn: (i: number, r: (k: string) => number) => [number, number, number]): Shape => {
  const a = new Float32Array(N * 3);
  for (let i = 0; i < N; i++) {
    const [x, y, z] = fn(i, (k) => random(`${k}${i}`));
    a[i * 3] = x;
    a[i * 3 + 1] = y;
    a[i * 3 + 2] = z;
  }
  return a;
};

const golden = Math.PI * (3 - Math.sqrt(5));
// One shape per scene: the particle cloud morphs between them at every cut.
const SHAPES: Shape[] = [
  // 1 sphere
  make((i) => {
    const y = 1 - (i / (N - 1)) * 2;
    const r = Math.sqrt(1 - y * y);
    return [Math.cos(golden * i) * r * 2.6, y * 2.6, Math.sin(golden * i) * r * 2.6];
  }),
  // 2 torus (a neuron's ring)
  make((i, r) => {
    const u = r("u") * Math.PI * 2;
    const v = r("v") * Math.PI * 2;
    return [(2.4 + 0.7 * Math.cos(v)) * Math.cos(u), 0.7 * Math.sin(v), (2.4 + 0.7 * Math.cos(v)) * Math.sin(u)];
  }),
  // 3 lattice cube (perceptron grid)
  make((i) => {
    const s = 12;
    const x = i % s;
    const y = Math.floor(i / s) % s;
    const z = Math.floor(i / (s * s)) % s;
    return [(x / (s - 1) - 0.5) * 4, (y / (s - 1) - 0.5) * 4, (z / (s - 1) - 0.5) * 4];
  }),
  // 4 snowflake (six arms in a plane)
  make((i, r) => {
    const arm = i % 6;
    const t = r("t");
    const side = (r("s") - 0.5) * 0.5 * t;
    const a = (arm / 6) * Math.PI * 2;
    const len = t * 3.2;
    return [Math.cos(a) * len - Math.sin(a) * side, Math.sin(a) * len + Math.cos(a) * side, (r("z") - 0.5) * 0.3];
  }),
  // 5 network layers
  make((i, r) => {
    const layer = i % 4;
    return [(layer - 1.5) * 1.6, (r("y") - 0.5) * 4.2, (r("z") - 0.5) * 2.2];
  }),
  // 6 spiral galaxy (the deep-learning explosion)
  make((i, r) => {
    const t = r("t");
    const arm = i % 3;
    const a = t * 7 + (arm / 3) * Math.PI * 2;
    const rad = 0.2 + t * 3.3;
    return [Math.cos(a) * rad + (r("x") - 0.5) * 0.35, (r("y") - 0.5) * 0.4 * (1 - t), Math.sin(a) * rad + (r("z") - 0.5) * 0.35];
  }),
  // 7 torus knot (attention)
  make((i) => {
    const t = (i / N) * Math.PI * 2;
    const p = 2;
    const q = 3;
    const rr = 1.6 + 0.6 * Math.cos(q * t);
    const jit = (random(`k${i}`) - 0.5) * 0.25;
    return [rr * Math.cos(p * t) + jit, 0.6 * Math.sin(q * t) * 1.8 + jit, rr * Math.sin(p * t) + jit];
  }),
  // 8 tower (scale)
  make((i, r) => {
    const t = i / N;
    const a = t * 40;
    const rad = 0.9 + r("r") * 0.25;
    return [Math.cos(a) * rad, t * 7 - 3.5, Math.sin(a) * rad];
  }),
  // 9 race track
  make((i, r) => {
    const a = (i / N) * Math.PI * 2;
    const lane = (r("l") - 0.5) * 0.5;
    return [Math.cos(a) * (3.3 + lane), (r("y") - 0.5) * 0.15, Math.sin(a) * (1.8 + lane)];
  }),
  // 10 burst
  make((i, r) => {
    const y = 1 - (i / (N - 1)) * 2;
    const rad = Math.sqrt(1 - y * y);
    const d = 1.5 + r("d") * 3.5;
    return [Math.cos(golden * i) * rad * d, y * d, Math.sin(golden * i) * rad * d];
  }),
];

const sceneAt = (f: number) => {
  let k = 0;
  for (let i = 0; i < SCENE_STARTS.length; i++) if (f >= SCENE_STARTS[i] - MORPH / 2) k = i;
  const start = SCENE_STARTS[k];
  const t = k === 0 ? 1 : interpolate(f, [start - MORPH / 2, start + MORPH / 2], [0, 1], { easing: E.expoInOut, extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return { k, t };
};

const Cloud: React.FC = () => {
  const f = useCurrentFrame();
  const { k, t } = sceneAt(f);
  const from = SHAPES[Math.max(0, k - 1)];
  const to = SHAPES[k];
  const arr = useMemo(() => new Float32Array(N * 3), []);
  // during a morph the cloud swirls outward a little, then settles
  const swell = Math.sin(t * Math.PI) * 0.35;
  for (let i = 0; i < N * 3; i += 3) {
    const x = from[i] + (to[i] - from[i]) * t;
    const y = from[i + 1] + (to[i + 1] - from[i + 1]) * t;
    const z = from[i + 2] + (to[i + 2] - from[i + 2]) * t;
    const n = Math.sin(f * 0.05 + i * 0.37) * 0.04;
    arr[i] = x * (1 + swell) + n;
    arr[i + 1] = y * (1 + swell) + n;
    arr[i + 2] = z * (1 + swell);
  }
  const geom = useMemo(() => new THREE.BufferGeometry(), []);
  geom.setAttribute("position", new THREE.BufferAttribute(arr.slice(), 3));

  // track scene: particles race around the loop; finale: burst outward
  const spin = f * 0.006 + (k === 8 ? (f - T.s9) * 0.03 : 0);
  const burst = k === 9 ? 1 + Math.min(1, (f - T.s10) / 40) * 0.6 : 1;
  return (
    <points geometry={geom} rotation={[0.35 + Math.sin(f / 120) * 0.15, spin, 0.08 * Math.sin(f / 90)]} scale={burst}>
      <pointsMaterial color={accentAt(f)} size={0.05} sizeAttenuation transparent opacity={0.75} depthWrite={false} blending={THREE.AdditiveBlending} />
    </points>
  );
};

export const Particles3D: React.FC<{ opacity?: number }> = ({ opacity = 1 }) => {
  const { width, height } = useVideoConfig();
  const f = useCurrentFrame();
  // pull the camera in and out with each scene for a sense of travel
  const { k, t } = sceneAt(f);
  const z = 10 - Math.sin(t * Math.PI) * 2.5 + (k % 2 ? 0.6 : 0);
  return (
    <div style={{ position: "absolute", inset: 0, opacity }}>
      <ThreeCanvas width={width} height={height} camera={{ fov: 45, position: [0, 0, z] }} gl={{ antialias: true, alpha: true }}>
        <CameraZ z={z} />
        <Cloud />
      </ThreeCanvas>
    </div>
  );
};

const CameraZ: React.FC<{ z: number }> = ({ z }) => {
  // ThreeCanvas creates the camera once; move it every frame.
  const { camera } = useThree();
  camera.position.set(0, 0, z);
  camera.lookAt(0, 0, 0);
  return null;
};
