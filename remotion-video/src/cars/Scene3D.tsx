import React, { useMemo } from "react";
import { useThree } from "@react-three/fiber";
import { ThreeCanvas } from "@remotion/three";
import * as THREE from "three";
import { RoomEnvironment } from "three/examples/jsm/environments/RoomEnvironment.js";
import { random, useCurrentFrame, useVideoConfig } from "remotion";
import { Car } from "./Car";
import { ERAS, LAST_ARRIVAL, eraPosition, lerp, morphProgress, paramsAt, pickAt } from "./eras";

const mixColor = (a: string, b: string, t: number) => "#" + new THREE.Color(a).lerp(new THREE.Color(b), t).getHexString();

const Environment: React.FC = () => {
  const { gl, scene } = useThree();
  useMemo(() => {
    const pmrem = new THREE.PMREMGenerator(gl);
    scene.environment = pmrem.fromScene(new RoomEnvironment(), 0.04).texture;
    scene.environmentIntensity = 0.8;
    pmrem.dispose();
  }, [gl, scene]);
  return null;
};

const CameraRig: React.FC<{ e: number; m: number; frame: number }> = ({ e, m, frame }) => {
  const { camera } = useThree();
  const [a, b, t] = pickAt(e, (era) => era.cam);
  const swoop = m >= 0 ? Math.sin(Math.PI * m) : 0;
  const outro = Math.max(0, frame - (LAST_ARRIVAL + 30));
  const az = lerp(a.az, b.az, t) + Math.sin(frame / 90) * 0.1 + swoop * 0.9 + outro * 0.011;
  const el = lerp(a.el, b.el, t) + swoop * 0.15 + Math.sin(frame / 70) * 0.03;
  const dist = lerp(a.dist, b.dist, t) - swoop * 0.8 + Math.min(outro, 120) * 0.02;
  const lookY = lerp(a.lookY, b.lookY, t) + swoop * 0.5;
  camera.position.set(Math.sin(az) * Math.cos(el) * dist, lookY + Math.sin(el) * dist, Math.cos(az) * Math.cos(el) * dist);
  camera.lookAt(0, lookY, 0);
  return null;
};

const SHAPES = new Array(18).fill(0).map((_, i) => ({
  angle: (i / 18) * Math.PI * 2 + random(`sa${i}`) * 0.3,
  radius: 8 + random(`sr${i}`) * 8,
  y: 0.8 + random(`sy${i}`) * 6,
  size: 0.35 + random(`ss${i}`) * 0.8,
  kind: i % 5,
  wire: i % 2 === 1,
  spin: 0.5 + random(`sp${i}`),
}));

const Shape: React.FC<{ kind: number; size: number }> = ({ kind, size }) => {
  switch (kind) {
    case 0:
      return <icosahedronGeometry args={[size, 0]} />;
    case 1:
      return <torusKnotGeometry args={[size * 0.6, size * 0.18, 80, 12]} />;
    case 2:
      return <octahedronGeometry args={[size, 0]} />;
    case 3:
      return <torusGeometry args={[size * 0.8, size * 0.12, 12, 40]} />;
    default:
      return <dodecahedronGeometry args={[size, 0]} />;
  }
};

const PARTICLES = new Array(80).fill(0).map((_, i) => {
  const u = random(`pu${i}`) * 2 - 1;
  const th = random(`pt${i}`) * Math.PI * 2;
  const s = Math.sqrt(1 - u * u);
  return { dir: [s * Math.cos(th), Math.abs(u) * 0.8 + 0.1, s * Math.sin(th)], speed: 3 + random(`ps${i}`) * 5 };
});

const ROAD_SPACING = 3;

export const Scene3D: React.FC = () => {
  const frame = useCurrentFrame();
  const { width, height, durationInFrames } = useVideoConfig();

  const e = eraPosition(frame);
  const m = morphProgress(frame);
  const p = paramsAt(e);
  const [, , t] = pickAt(e, (x) => x);
  const i = Math.min(Math.floor(e), ERAS.length - 2);
  const A = ERAS[i];
  const B = ERAS[i + 1];
  const col = (get: (era: (typeof ERAS)[number]) => string) => mixColor(get(A), get(B), t);

  const accent = col((x) => x.accent);
  const bg = col((x) => x.bg);

  // Integrated travel distance, so wheels and road stay in sync while speed changes.
  const travel = useMemo(() => {
    const arr = [0];
    for (let f = 1; f < durationInFrames; f++) arr.push(arr[f - 1] + paramsAt(eraPosition(f)).speed);
    return arr;
  }, [durationInFrames]);
  const dist = travel[Math.min(frame, durationInFrames - 1)];

  const swoop = m >= 0 ? Math.sin(Math.PI * m) : 0;
  const spin = m >= 0 ? (1 - Math.cos(Math.PI * m)) * Math.PI : 0;
  const introScale = Math.min(1, frame / 25);
  const introPop = 1 - Math.pow(1 - introScale, 3);

  return (
    <ThreeCanvas width={width} height={height} camera={{ fov: 44, near: 0.1, far: 200 }} gl={{ antialias: true }}>
      <color attach="background" args={[bg]} />
      <fog attach="fog" args={[bg, 14, 34]} />
      <Environment />
      <CameraRig e={e} m={m} frame={frame} />

      <ambientLight intensity={0.35} />
      <hemisphereLight args={[accent, "#000000", 0.6]} />
      <directionalLight position={[6, 10, 6]} intensity={2.2} />
      <pointLight position={[-5, 3, -4]} intensity={30} color={accent} distance={20} />
      <pointLight position={[0, 1.5, 0]} intensity={swoop * 60} color={accent} distance={12} />

      {/* Ground, contact shadow and moving road markings */}
      <mesh rotation={[-Math.PI / 2, 0, 0]}>
        <circleGeometry args={[60, 64]} />
        <meshStandardMaterial color={bg} metalness={0.6} roughness={0.35} />
      </mesh>
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.01, 0]}>
        <ringGeometry args={[3.2, 3.28, 96]} />
        <meshBasicMaterial color={accent} transparent opacity={0.5} />
      </mesh>
      {new Array(14).fill(0).map((_, k) => {
        const x = ((k * ROAD_SPACING - dist) % (14 * ROAD_SPACING) + 14 * ROAD_SPACING) % (14 * ROAD_SPACING) - 7 * ROAD_SPACING;
        return (
          <React.Fragment key={k}>
            <mesh rotation={[-Math.PI / 2, 0, 0]} position={[x, 0.012, 2.4]}>
              <planeGeometry args={[1.4, 0.12]} />
              <meshBasicMaterial color={accent} transparent opacity={0.65} />
            </mesh>
            <mesh rotation={[-Math.PI / 2, 0, 0]} position={[x, 0.012, -2.4]}>
              <planeGeometry args={[1.4, 0.12]} />
              <meshBasicMaterial color={accent} transparent opacity={0.65} />
            </mesh>
          </React.Fragment>
        );
      })}

      {/* Floating 3D shapes */}
      {SHAPES.map((s, k) => (
        <mesh
          key={k}
          position={[Math.cos(s.angle + frame * 0.002) * s.radius, s.y + Math.sin(frame / 40 + k) * 0.35, Math.sin(s.angle + frame * 0.002) * s.radius]}
          rotation={[frame * 0.01 * s.spin, frame * 0.013 * s.spin, 0]}
          scale={1 + swoop * 0.5}
        >
          <Shape kind={s.kind} size={s.size} />
          <meshStandardMaterial
            color={accent}
            emissive={accent}
            emissiveIntensity={s.wire ? 1.4 : 0.35}
            metalness={0.7}
            roughness={0.25}
            wireframe={s.wire}
          />
        </mesh>
      ))}

      {/* The car: lifts, spins and rebuilds itself during each transformation */}
      <group position={[0, swoop * 0.9, 0]} rotation={[0, spin, swoop * 0.08]} scale={introPop}>
        <Car
          p={p}
          bodyColor={col((x) => x.bodyColor)}
          wheelColor={col((x) => x.wheelColor)}
          tireColor={col((x) => x.tireColor)}
          accent={accent}
          spokes={t < 0.5 ? A.spokes : B.spokes}
          wheelAngle={dist}
          extraWheels={Math.min(1, e / 0.35)}
        />
        {m >= 0 ? (
          <mesh position={[lerp(p.xTail - 0.6, p.xNose + 0.6, m), 1.2, 0]}>
            <boxGeometry args={[0.03, 2.4, p.width + 0.8]} />
            <meshBasicMaterial color={accent} transparent opacity={swoop * 0.45} />
          </mesh>
        ) : null}
      </group>

      {/* Transformation burst: energy rings and sparks */}
      {m >= 0
        ? [0, 1, 2].map((k) => {
            const r = Math.max(0, m * 1.4 - k * 0.15);
            return (
              <mesh key={k} rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.05 + k * 0.5, 0]} scale={1 + r * 7}>
                <torusGeometry args={[1, 0.025, 8, 80]} />
                <meshBasicMaterial color={accent} transparent opacity={Math.max(0, 1 - r)} />
              </mesh>
            );
          })
        : null}
      {m >= 0
        ? PARTICLES.map((pt, k) => {
            const d = m * pt.speed;
            return (
              <mesh key={k} position={[pt.dir[0] * d, 1 + pt.dir[1] * d, pt.dir[2] * d]}>
                <sphereGeometry args={[0.05, 8, 8]} />
                <meshBasicMaterial color={k % 3 === 0 ? "#ffffff" : accent} transparent opacity={1 - m} />
              </mesh>
            );
          })
        : null}
    </ThreeCanvas>
  );
};
