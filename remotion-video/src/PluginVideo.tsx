import React from "react";
import { springTiming, TransitionSeries } from "@remotion/transitions";
import { fade } from "@remotion/transitions/fade";
import { slide } from "@remotion/transitions/slide";
import { wipe } from "@remotion/transitions/wipe";
import { AbsoluteFill, useVideoConfig } from "remotion";
import { CodeScene } from "./scenes/CodeScene";
import { IntroScene } from "./scenes/IntroScene";
import { OutroScene } from "./scenes/OutroScene";
import { SkillsScene } from "./scenes/SkillsScene";
import { StepsScene } from "./scenes/StepsScene";
import { colors } from "./theme";

export const TRANSITION_FRAMES = 20;
const timing = springTiming({ config: { damping: 200 }, durationInFrames: TRANSITION_FRAMES });

export const PluginVideo: React.FC = () => {
  const { fps } = useVideoConfig();

  return (
    <AbsoluteFill style={{ backgroundColor: colors.bg }}>
      <TransitionSeries>
        <TransitionSeries.Sequence name="Intro" durationInFrames={110} premountFor={fps}>
          <IntroScene />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition presentation={slide({ direction: "from-bottom" })} timing={timing} />
        <TransitionSeries.Sequence name="Code" durationInFrames={150} premountFor={fps}>
          <CodeScene />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition presentation={wipe({ direction: "from-top-left" })} timing={timing} />
        <TransitionSeries.Sequence name="Skills" durationInFrames={150} premountFor={fps}>
          <SkillsScene />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition presentation={slide({ direction: "from-right" })} timing={timing} />
        <TransitionSeries.Sequence name="Steps" durationInFrames={150} premountFor={fps}>
          <StepsScene />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition presentation={fade()} timing={timing} />
        <TransitionSeries.Sequence name="Outro" durationInFrames={120} premountFor={fps}>
          <OutroScene />
        </TransitionSeries.Sequence>
      </TransitionSeries>
    </AbsoluteFill>
  );
};
