import React from "react";
import { AbsoluteFill, Sequence, useCurrentFrame } from "remotion";
import { C, E, Grain, prog } from "./kit";
import { S1_DURATION, S1Intro } from "./scenes/S1Intro";
import { S2_DURATION, S2Wheel } from "./scenes/S2Wheel";
import { S3_DURATION, S3Timeline, WHIP } from "./scenes/S3Timeline";
import { S4_DURATION, S4Benz } from "./scenes/S4Benz";
import { S5_DURATION, S5ModelT } from "./scenes/S5ModelT";
import { IRIS, S6_DURATION, S6Fleet } from "./scenes/S6Fleet";
import { S7_DURATION, S7Speed } from "./scenes/S7Speed";
import { S8_DURATION, S8Morph } from "./scenes/S8Morph";
import { S9_DURATION, S9Electric } from "./scenes/S9Electric";
import { S10_DURATION, S10Outro } from "./scenes/S10Outro";

const STRIPES = 7;
const STRIPE_DUR = 28;

// Angled bars sweep across, cover the cut, then sweep away.
const StripeWipe: React.FC = () => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill style={{ overflow: "hidden" }}>
      {new Array(STRIPES).fill(0).map((_, i) => {
        const pin = prog(frame, i * 1.2, 12, E.expoInOut);
        const pout = prog(frame, 22 + i * 1.2, 12, E.expoInOut);
        const x = (1 - pin) * 2400 - pout * 2400;
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              left: -600 + x,
              top: -200 + i * 340,
              width: 2400,
              height: 340,
              background: i % 2 === 0 ? C.red : C.ink,
              transform: "skewY(-12deg)",
            }}
          />
        );
      })}
    </AbsoluteFill>
  );
};

const T1 = 0;
const T2 = T1 + S1_DURATION;
const T3 = T2 + S2_DURATION;
const T4 = T3 + S3_DURATION - WHIP;
const T_STRIPE = T4 + S4_DURATION - 20;
const T5 = T4 + S4_DURATION;
const T6 = T5 + S5_DURATION - IRIS;
const T7 = T6 + S6_DURATION;
const T8 = T7 + S7_DURATION;
const T9 = T8 + S8_DURATION;
const T10 = T9 + S9_DURATION;
export const CAR_STORY_DURATION = T10 + S10_DURATION;

export const CarStory: React.FC = () => {
  return (
    <AbsoluteFill style={{ backgroundColor: C.ink }}>
      <Sequence name="Intro" from={T1} durationInFrames={S1_DURATION}>
        <S1Intro />
      </Sequence>
      <Sequence name="Wheel" from={T2} durationInFrames={S2_DURATION}>
        <S2Wheel />
      </Sequence>
      <Sequence name="Timeline" from={T3} durationInFrames={S3_DURATION}>
        <S3Timeline />
      </Sequence>
      <Sequence name="Benz 1886" from={T4} durationInFrames={S4_DURATION}>
        <S4Benz />
      </Sequence>
      <Sequence name="Model T" from={T5} durationInFrames={S5_DURATION}>
        <S5ModelT />
      </Sequence>
      <Sequence name="Stripe wipe" from={T_STRIPE} durationInFrames={STRIPE_DUR + 14}>
        <StripeWipe />
      </Sequence>
      <Sequence name="Fleet" from={T6} durationInFrames={S6_DURATION}>
        <S6Fleet />
      </Sequence>
      <Sequence name="Speed" from={T7} durationInFrames={S7_DURATION}>
        <S7Speed />
      </Sequence>
      <Sequence name="Morph" from={T8} durationInFrames={S8_DURATION}>
        <S8Morph />
      </Sequence>
      <Sequence name="Electric" from={T9} durationInFrames={S9_DURATION}>
        <S9Electric />
      </Sequence>
      <Sequence name="Outro" from={T10} durationInFrames={S10_DURATION}>
        <S10Outro />
      </Sequence>
      <Grain opacity={0.1} />
    </AbsoluteFill>
  );
};
