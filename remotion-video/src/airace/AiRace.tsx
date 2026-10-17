import React from "react";
import { AbsoluteFill, Sequence } from "remotion";
import { Grain } from "../carstory/kit";
import { GridBg, HitFx, Hud, useGlobalShake } from "./rigs";
import { S1Start } from "./scenes/S1Start";
import { S2Idea } from "./scenes/S2Idea";
import { S3FirstLaps } from "./scenes/S3FirstLaps";
import { S4Content, S4Shatter } from "./scenes/S4Winter";
import { S5Return } from "./scenes/S5Return";
import { S6DeepLearning } from "./scenes/S6DeepLearning";
import { S7Transformer } from "./scenes/S7Transformer";
import { S8Scale } from "./scenes/S8Scale";
import { S9Flagships } from "./scenes/S9Flagships";
import { S10Finish } from "./scenes/S10Finish";
import { Soundtrack } from "./Soundtrack";
import { MorphWipe, SceneCam, SHAPE } from "./morph";
import { Particles3D } from "./Particles3D";
import { A, BG, T } from "./theme";

export const AI_RACE_DURATION = T.end;

export const AiRace: React.FC = () => {
  const shake = useGlobalShake();
  return (
    <AbsoluteFill style={{ backgroundColor: BG }}>
      <GridBg />
      <Particles3D opacity={0.7} />
      <AbsoluteFill style={{ transform: shake }}>
        <Sequence name="1 Старт" from={T.s1} durationInFrames={T.s2 - T.s1}>
          <SceneCam dur={T.s2 - T.s1}>
            <S1Start />
          </SceneCam>
        </Sequence>
        <Sequence name="2 Идея" from={T.s2} durationInFrames={T.s3 - T.s2}>
          <SceneCam dur={T.s3 - T.s2}>
            <S2Idea />
          </SceneCam>
        </Sequence>
        <Sequence name="3 Первые круги" from={T.s3} durationInFrames={T.s4 - T.s3}>
          <SceneCam dur={T.s4 - T.s3}>
            <S3FirstLaps />
          </SceneCam>
        </Sequence>
        <Sequence name="4 Зима" from={T.s4} durationInFrames={T.s5 - T.s4}>
          <SceneCam dur={T.s5 - T.s4}>
            <S4Content />
          </SceneCam>
        </Sequence>
        <Sequence name="5 Возвращение" from={T.s5} durationInFrames={T.s6 - T.s5}>
          <SceneCam dur={T.s6 - T.s5}>
            <S5Return />
          </SceneCam>
        </Sequence>
        <Sequence name="4→5 Лёд" from={T.s5} durationInFrames={26}>
          <S4Shatter />
        </Sequence>
        <Sequence name="6 Глубокое обучение" from={T.s6} durationInFrames={T.s7 - T.s6}>
          <SceneCam dur={T.s7 - T.s6}>
            <S6DeepLearning />
          </SceneCam>
        </Sequence>
        <Sequence name="7 Трансформер" from={T.s7} durationInFrames={T.s8 - T.s7}>
          <SceneCam dur={T.s8 - T.s7}>
            <S7Transformer />
          </SceneCam>
        </Sequence>
        <Sequence name="8 Масштаб" from={T.s8} durationInFrames={T.s9 - T.s8}>
          <SceneCam dur={T.s9 - T.s8}>
            <S8Scale />
          </SceneCam>
        </Sequence>
        <Sequence name="9 Флагманы" from={T.s9} durationInFrames={T.s10 - T.s9}>
          <SceneCam dur={T.s10 - T.s9}>
            <S9Flagships />
          </SceneCam>
        </Sequence>
        <Sequence name="10 Финиш" from={T.s10} durationInFrames={T.end - T.s10}>
          <SceneCam dur={T.end - T.s10}>
            <S10Finish />
          </SceneCam>
        </Sequence>
        {/* Morph wipes on the cuts that had no continuous hand-off */}
        <Sequence name="Морф 2→3" from={T.s3 - 15} durationInFrames={30}>
          <MorphWipe from={SHAPE.hexagon} to={SHAPE.square} colorA={A.blue} colorB="#10204a" />
        </Sequence>
        <Sequence name="Морф 3→4" from={T.s4 - 15} durationInFrames={30}>
          <MorphWipe from={SHAPE.square} to={SHAPE.snowflake} colorA="#1a2a4d" colorB={A.ice} />
        </Sequence>
        <Sequence name="Морф 7→8" from={T.s8 - 15} durationInFrames={30}>
          <MorphWipe from={SHAPE.star8} to={SHAPE.triangle} colorA={A.red} colorB={A.warm} />
        </Sequence>
      </AbsoluteFill>
      <Hud />
      <HitFx />
      <Grain opacity={0.08} />
      <Soundtrack />
    </AbsoluteFill>
  );
};
