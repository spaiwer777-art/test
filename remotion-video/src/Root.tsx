import { Composition, Folder } from "remotion";
import { CarHistory } from "./cars/CarHistory";
import { CAR_HISTORY_DURATION } from "./cars/eras";
import { PluginVideo, TRANSITION_FRAMES } from "./PluginVideo";
import { CodeScene } from "./scenes/CodeScene";
import { IntroScene } from "./scenes/IntroScene";
import { OutroScene } from "./scenes/OutroScene";
import { SkillsScene } from "./scenes/SkillsScene";
import { StepsScene } from "./scenes/StepsScene";

const WIDTH = 1080;
const HEIGHT = 1920;
const FPS = 30;

export const RemotionRoot: React.FC = () => {
  return (
    <>
      <Folder name="Scenes">
        <Composition id="Intro" component={IntroScene} width={WIDTH} height={HEIGHT} fps={FPS} durationInFrames={110} />
        <Composition id="Code" component={CodeScene} width={WIDTH} height={HEIGHT} fps={FPS} durationInFrames={150} />
        <Composition id="Skills" component={SkillsScene} width={WIDTH} height={HEIGHT} fps={FPS} durationInFrames={150} />
        <Composition id="Steps" component={StepsScene} width={WIDTH} height={HEIGHT} fps={FPS} durationInFrames={150} />
        <Composition id="Outro" component={OutroScene} width={WIDTH} height={HEIGHT} fps={FPS} durationInFrames={120} />
      </Folder>
      <Composition
        id="RemotionPlugin"
        component={PluginVideo}
        width={WIDTH}
        height={HEIGHT}
        fps={FPS}
        durationInFrames={110 + 150 + 150 + 150 + 120 - 4 * TRANSITION_FRAMES}
      />
      <Composition
        id="CarHistory"
        component={CarHistory}
        width={WIDTH}
        height={HEIGHT}
        fps={FPS}
        durationInFrames={CAR_HISTORY_DURATION}
      />
    </>
  );
};
