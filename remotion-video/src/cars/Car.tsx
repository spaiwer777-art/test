import React, { useEffect, useMemo } from "react";
import * as THREE from "three";
import { CarParams, lerp } from "./eras";

type V2 = [number, number];

const controlPoints = (p: CarParams): V2[] => [
  [p.xNose, p.bottom],
  [p.xNose, (p.noseH + p.bottom) / 2],
  [p.xNose - 0.08, p.noseH],
  [p.xWs, p.hoodH],
  [p.xRf, p.roofH],
  [p.xRr, p.roofH],
  [p.xRw, p.deckH],
  [p.xTail + 0.1, p.tailH],
  [p.xTail, (p.tailH + p.bottom) / 2],
  [p.xTail, p.bottom],
  [p.xTail * 0.5, p.bottom],
  [p.xNose * 0.5, p.bottom],
];

const SAMPLES = 10;

const catmull = (p0: number, p1: number, p2: number, p3: number, u: number) =>
  0.5 * (2 * p1 + (-p0 + p2) * u + (2 * p0 - 5 * p1 + 4 * p2 - p3) * u * u + (-p0 + 3 * p1 - 3 * p2 + p3) * u * u * u);

// Samples the closed silhouette; `smooth` blends sharp polyline → Catmull-Rom curve.
const samplePoint = (pts: V2[], i: number, u: number, smooth: number): V2 => {
  const n = pts.length;
  const a = pts[(i - 1 + n) % n];
  const b = pts[i];
  const c = pts[(i + 1) % n];
  const d = pts[(i + 2) % n];
  const lx = lerp(b[0], c[0], u);
  const ly = lerp(b[1], c[1], u);
  return [lerp(lx, catmull(a[0], b[0], c[0], d[0], u), smooth), lerp(ly, catmull(a[1], b[1], c[1], d[1], u), smooth)];
};

const BEVEL = 0.08;

const useDisposable = <T extends { dispose: () => void }>(obj: T) => {
  useEffect(() => () => obj.dispose(), [obj]);
  return obj;
};

const GlassPane: React.FC<{ from: V2; to: V2; mid: V2; depth: number; opacity: number }> = ({
  from,
  to,
  mid,
  depth,
  opacity,
}) => {
  const dx = to[0] - from[0];
  const dy = to[1] - from[1];
  const len = Math.hypot(dx, dy);
  const nx = -dy / len;
  const ny = dx / len;
  return (
    <mesh position={[mid[0] + nx * 0.04, mid[1] + ny * 0.04, 0]} rotation={[0, 0, Math.atan2(dy, dx)]}>
      <boxGeometry args={[len, 0.03, depth]} />
      <meshPhysicalMaterial color="#0a1520" metalness={0.9} roughness={0.05} transparent opacity={opacity} />
    </mesh>
  );
};

export const Wheel: React.FC<{
  r: number;
  tireT: number;
  solid: number;
  spokes: number;
  wheelColor: string;
  tireColor: string;
  angle: number;
}> = ({ r, tireT, solid, spokes, wheelColor, tireColor, angle }) => {
  const discR = lerp(r * 0.18, r - tireT * 0.55, solid);
  const width = Math.max(tireT * 1.3, 0.1);
  const spokeT = spokes <= 6 ? 0.1 : 0.035;
  return (
    <group rotation={[0, 0, -angle]}>
      <mesh>
        <torusGeometry args={[r - tireT / 2, tireT / 2, 14, 48]} />
        <meshStandardMaterial color={tireColor} roughness={0.85} />
      </mesh>
      <mesh rotation={[Math.PI / 2, 0, 0]}>
        <cylinderGeometry args={[discR, discR, width * 0.7, 40]} />
        <meshStandardMaterial color={wheelColor} metalness={0.4} roughness={0.45} />
      </mesh>
      {new Array(spokes).fill(0).map((_, i) => {
        const a = (i / spokes) * Math.PI * 2;
        const len = r - tireT * 0.6;
        return (
          <mesh key={i} position={[(Math.cos(a) * len) / 2, (Math.sin(a) * len) / 2, 0]} rotation={[0, 0, a]}>
            <boxGeometry args={[len, spokeT, width * 0.8]} />
            <meshStandardMaterial color={wheelColor} metalness={0.5} roughness={0.35} />
          </mesh>
        );
      })}
      <mesh rotation={[Math.PI / 2, 0, 0]}>
        <cylinderGeometry args={[r * 0.14, r * 0.14, width * 0.95, 20]} />
        <meshStandardMaterial color="#b9b9b9" metalness={0.9} roughness={0.2} />
      </mesh>
    </group>
  );
};

export const Car: React.FC<{
  p: CarParams;
  bodyColor: string;
  wheelColor: string;
  tireColor: string;
  accent: string;
  spokes: number;
  wheelAngle: number;
  extraWheels: number;
}> = ({ p, bodyColor, wheelColor, tireColor, accent, spokes, wheelAngle, extraWheels }) => {
  const outline = useMemo(() => {
    const pts = controlPoints(p);
    const out: V2[] = [];
    for (let i = 0; i < pts.length; i++) {
      for (let s = 0; s < SAMPLES; s++) out.push(samplePoint(pts, i, s / SAMPLES, p.smooth));
    }
    return { pts, out };
  }, [p]);

  const body = useDisposable(
    useMemo(() => {
      const shape = new THREE.Shape(outline.out.map(([x, y]) => new THREE.Vector2(x, y)));
      const g = new THREE.ExtrudeGeometry(shape, {
        depth: p.width,
        bevelEnabled: true,
        bevelThickness: BEVEL,
        bevelSize: 0.03 + 0.07 * p.smooth,
        bevelSegments: 3,
      });
      g.translate(0, 0, -p.width / 2);
      g.computeVertexNormals();
      return g;
    }, [outline, p.width, p.smooth]),
  );

  const glass = useDisposable(
    useMemo(() => {
      const cabin = [3, 4, 5, 6].map((i) => outline.pts[i]);
      const cx = cabin.reduce((s, v) => s + v[0], 0) / 4;
      const cy = cabin.reduce((s, v) => s + v[1], 0) / 4;
      const shape = new THREE.Shape(
        cabin.map(([x, y]) => new THREE.Vector2(cx + (x - cx) * 0.8, cy + (y - cy) * 0.78 + 0.03)),
      );
      const depth = p.width + BEVEL * 2 + 0.04;
      const g = new THREE.ExtrudeGeometry(shape, { depth, bevelEnabled: false });
      g.translate(0, 0, -depth / 2);
      return g;
    }, [outline, p.width]),
  );

  const seg = (i: number, u: number) => samplePoint(outline.pts, i, u, p.smooth);
  const half = p.width / 2;
  const lightY = lerp(p.bottom, p.noseH, 0.6);

  const wheels: { x: number; z: number; r: number; extra: boolean }[] = [
    { x: p.xF, z: p.track, r: p.rF, extra: false },
    { x: p.xF, z: -p.track, r: p.rF, extra: true },
    { x: p.xR, z: p.track, r: p.rR, extra: true },
    { x: p.xR, z: -p.track, r: p.rR, extra: true },
  ];

  return (
    <group>
      <group scale={[1, p.bodyScale, p.bodyScale]} visible={p.bodyScale > 0.01}>
        <mesh geometry={body}>
          <meshPhysicalMaterial
            color={bodyColor}
            metalness={p.metalness}
            roughness={p.roughness}
            clearcoat={p.metalness}
            clearcoatRoughness={0.1}
          />
        </mesh>
        {p.glass > 0.02 ? (
          <>
            <mesh geometry={glass}>
              <meshPhysicalMaterial color="#0a1520" metalness={0.9} roughness={0.05} transparent opacity={p.glass} />
            </mesh>
            <GlassPane from={seg(3, 0.15)} to={seg(3, 0.85)} mid={seg(3, 0.5)} depth={p.width * 0.9} opacity={p.glass} />
            <GlassPane from={seg(5, 0.15)} to={seg(5, 0.85)} mid={seg(5, 0.5)} depth={p.width * 0.9} opacity={p.glass} />
          </>
        ) : null}

        {p.headlights > 0.02
          ? [1, -1].map((side) => (
              <React.Fragment key={side}>
                <mesh position={[p.xNose + 0.02, lightY, side * half * 0.62]} scale={p.headlights}>
                  <sphereGeometry args={[0.13, 20, 20]} />
                  <meshStandardMaterial color="#fff6d8" emissive="#fff1c0" emissiveIntensity={2.5} />
                </mesh>
                <mesh position={[p.xTail - 0.02, lerp(p.bottom, p.tailH, 0.6), side * half * 0.62]} scale={p.headlights}>
                  <boxGeometry args={[0.05, 0.12, 0.3]} />
                  <meshStandardMaterial color="#ff2a2a" emissive="#ff1a1a" emissiveIntensity={2.5} />
                </mesh>
              </React.Fragment>
            ))
          : null}

        {p.lightBar > 0.02 ? (
          <>
            <mesh position={[p.xNose - 0.02, p.noseH - 0.06, 0]} scale={[1, 1, p.lightBar]}>
              <boxGeometry args={[0.06, 0.035, p.width * 0.95]} />
              <meshStandardMaterial color={accent} emissive={accent} emissiveIntensity={4} />
            </mesh>
            <mesh position={[p.xTail + 0.06, p.tailH - 0.06, 0]} scale={[1, 1, p.lightBar]}>
              <boxGeometry args={[0.06, 0.035, p.width * 0.95]} />
              <meshStandardMaterial color="#ff2040" emissive="#ff2040" emissiveIntensity={4} />
            </mesh>
          </>
        ) : null}
      </group>

      {wheels.map((w, i) => {
        const s = w.extra ? extraWheels : 1;
        if (s < 0.01) return null;
        return (
          <group key={i} position={[w.x, w.r, w.z]} scale={s}>
            <Wheel
              r={w.r}
              tireT={p.tireT}
              solid={p.solid}
              spokes={spokes}
              wheelColor={wheelColor}
              tireColor={tireColor}
              angle={wheelAngle / w.r}
            />
          </group>
        );
      })}
    </group>
  );
};
