import React from "react";
import { AbsoluteFill, interpolate, random, useCurrentFrame } from "remotion";
import { Background, Box, cond, ease, Flash, heavy, K, mono, p, sans, Slam, Streak, useHits, Word } from "./fx";
import type { Word as W } from "./vo";

export type SceneProps = { w: W[]; len: number };

// 1 — НЕ / МОЖЕТ / БЫТЬ.
export const S0: React.FC<SceneProps> = ({ w }) => {
  const f = useCurrentFrame();
  const [a, b, c] = w.map((x) => x.f);
  const stack = 92;
  const { transform, flash } = useHits([a, b, c, stack]);
  const bg = f >= stack || f >= c ? "white" : f >= b ? "red" : "black";
  return (
    <AbsoluteFill>
      <Background bg={bg} />
      <AbsoluteFill style={{ transform }}>
        {f < b ? (
          <Slam at={a} from="scale">
            <Word text="НЕ" size={420} />
          </Slam>
        ) : f < c ? (
          <Slam at={b} from="right" ghosts={6}>
            <Word text="МОЖЕТ" size={330} font={cond} weight={700} color={K.black} spacing={0} />
          </Slam>
        ) : f < stack ? (
          <Slam at={c} from="scale">
            <Word text="БЫТЬ" size={250} color={K.black} dot />
          </Slam>
        ) : (
          <>
            <Box top={560} height={260}>
              <Slam at={stack} from="up">
                <Word text="НЕ" size={240} color={K.black} />
              </Slam>
            </Box>
            <Box top={820} height={260}>
              <Slam at={stack + 3} from="left" ghosts={6}>
                <Word text="МОЖЕТ" size={250} font={cond} weight={700} color={K.red} spacing={0} />
              </Slam>
            </Box>
            <Box top={1080} height={260}>
              <Slam at={stack + 6} from="down">
                <Word text="БЫТЬ" size={240} color={K.black} dot />
              </Slam>
            </Box>
          </>
        )}
      </AbsoluteFill>
      <Flash v={flash} />
    </AbsoluteFill>
  );
};

// 2 — ЛАДНО, ЛАДНО.
export const S1: React.FC<SceneProps> = ({ w }) => {
  const f = useCurrentFrame();
  const [a, b] = w.map((x) => x.f);
  const { transform, flash } = useHits([a, b]);
  const letters = Array.from("ЛАДНО,");
  return (
    <AbsoluteFill>
      <Background bg={f < b ? "red" : "black"} />
      <AbsoluteFill style={{ transform }}>
        {f < b ? (
          <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", flexDirection: "row" }}>
            {letters.map((ch, i) => {
              const k = p(f, a + i * 3, 10, ease.back);
              const jit = Math.sin(f * 0.9 + i * 2) * 6;
              return (
                <span key={i} style={{ fontFamily: heavy, fontWeight: 900, fontSize: 150, color: K.black, display: "inline-block", transform: `translateY(${(1 - k) * -400 + jit}px) rotate(${(1 - k) * (i % 2 ? 30 : -30)}deg)`, opacity: k > 0 ? 1 : 0, filter: `blur(${(1 - k) * 8}px)` }}>
                  {ch}
                </span>
              );
            })}
            {[30, 54].map((at, i) => (
              <div key={i} style={{ position: "absolute", right: i ? 640 : 120, top: i ? 1240 : 620, fontFamily: heavy, fontWeight: 900, fontSize: 80, color: K.white, transform: `scale(${p(f, at, 8, ease.back)}) rotate(${i ? -12 : 10}deg)` }}>
                ХА
              </div>
            ))}
          </AbsoluteFill>
        ) : (
          <Slam at={b} from="zoom">
            <Word text="ЛАДНО" size={165} dot />
          </Slam>
        )}
      </AbsoluteFill>
      <Flash v={flash} />
    </AbsoluteFill>
  );
};

// 3 — ТЫ ПРАВДА / ЭТО СДЕЛАЛ?
const extrude = (n: number, col: string) => new Array(n).fill(0).map((_, i) => `${i * 2}px ${i * 2}px 0 ${col}`).join(",");
export const S2: React.FC<SceneProps> = ({ w }) => {
  const f = useCurrentFrame();
  const [a, b, c, d] = w.map((x) => x.f);
  const { transform, flash } = useHits([a, b, c, d]);
  const tilt = Math.sin(f / 30) * 10;
  const fill = p(f, d + 14, 12);
  return (
    <AbsoluteFill>
      <Background bg={f < c ? "black" : "white"} />
      <AbsoluteFill style={{ transform, perspective: 1200 }}>
        {f < c ? (
          <AbsoluteFill style={{ transform: `rotateY(${tilt}deg) rotateX(${tilt * 0.4}deg)` }}>
            <Box top={640} height={240}>
              <Slam at={a} from="up">
                <div style={{ fontFamily: heavy, fontWeight: 900, fontSize: 220, color: K.white, textShadow: extrude(14, "#7a0e14") }}>ТЫ</div>
              </Slam>
            </Box>
            <Box top={900} height={260}>
              <Slam at={b} from="down" ghosts={5}>
                <div style={{ fontFamily: heavy, fontWeight: 900, fontSize: 148, color: K.white, textShadow: extrude(14, "#7a0e14") }}>ПРАВДА</div>
              </Slam>
            </Box>
          </AbsoluteFill>
        ) : (
          <>
            <Box top={620} height={240}>
              <Slam at={c} from="left" ghosts={6}>
                <Word text="ЭТО" size={230} color={K.black} />
              </Slam>
            </Box>
            <Box top={890} height={260}>
              <Slam at={d} from="right" ghosts={6}>
                <div style={{ position: "relative" }}>
                  <Word text="СДЕЛАЛ?" size={128} stroke={K.black} />
                  <div style={{ position: "absolute", inset: 0, clipPath: `inset(0 ${(1 - fill) * 100}% 0 0)` }}>
                    <Word text="СДЕЛАЛ?" size={128} color={K.red} />
                  </div>
                </div>
              </Slam>
            </Box>
          </>
        )}
      </AbsoluteFill>
      <Flash v={flash} />
    </AbsoluteFill>
  );
};

// 4 — И ПРЕЖДЕ ЧЕМ ТЫ СПРОСИШЬ ?
export const S3: React.FC<SceneProps> = ({ w }) => {
  const f = useCurrentFrame();
  const t = w.map((x) => x.f);
  const { transform, flash } = useHits([t[0], t[4]]);
  const q = p(f, t[4], 14, ease.back);
  return (
    <AbsoluteFill>
      <Background bg="white" />
      <AbsoluteFill style={{ transform }}>
        <div style={{ position: "absolute", left: 90, top: 560, display: "flex", gap: 26, flexWrap: "wrap", width: 900 }}>
          {["И", "ПРЕЖДЕ", "ЧЕМ"].map((s, i) => (
            <div key={s} style={{ fontFamily: heavy, fontWeight: 900, fontSize: 82, color: K.black, transform: `translateY(${(1 - p(f, t[i], 8)) * 60}px)`, opacity: f >= t[i] ? 1 : 0 }}>
              {s}
            </div>
          ))}
        </div>
        <div style={{ position: "absolute", left: 90, top: 700 }}>
          <div style={{ fontFamily: heavy, fontWeight: 900, fontSize: 170, color: K.black, lineHeight: 1, transform: `scale(${interpolate(p(f, t[3], 8), [0, 1], [1.8, 1])})`, transformOrigin: "0 50%", opacity: f >= t[3] ? 1 : 0 }}>ТЫ</div>
          <div style={{ fontFamily: heavy, fontWeight: 900, fontSize: 108, color: K.red, lineHeight: 1.1, transform: `translateX(${(1 - p(f, t[4], 10)) * -700}px)`, opacity: f >= t[4] ? 1 : 0, filter: `blur(${(1 - p(f, t[4], 10)) * 10}px)` }}>СПРОСИШЬ</div>
        </div>
        <div style={{ position: "absolute", right: 30, top: 900, fontFamily: heavy, fontWeight: 900, fontSize: 620, color: K.red, lineHeight: 1, transform: `translateX(${(1 - q) * 900}px) rotate(${(1 - q) * 40 + Math.sin(f / 10) * 3}deg)` }}>?</div>
        <div style={{ position: "absolute", right: 380, top: 1330, fontFamily: heavy, fontWeight: 900, fontSize: 200, color: K.black, transform: `scale(${p(f, t[4] + 10, 10, ease.back)})` }}>?</div>
      </AbsoluteFill>
      <Flash v={flash} />
    </AbsoluteFill>
  );
};

// 5 — ЭТО НЕ AFTER EFFECTS (struck through)
export const S4: React.FC<SceneProps> = ({ w }) => {
  const f = useCurrentFrame();
  const t = w.map((x) => x.f);
  const strike = p(f, t[3] + 30, 10, ease.inOut);
  const { transform, flash } = useHits([t[0], t[2], t[3] + 30]);
  const glitch = f >= t[3] + 30 && f < t[3] + 40;
  return (
    <AbsoluteFill>
      <Background bg="black" />
      <AbsoluteFill style={{ transform }}>
        <Box top={560} height={200}>
          <Slam at={t[0]} from="left" ghosts={5}>
            <div style={{ display: "flex", gap: 40 }}>
              <Word text="ЭТО" size={150} />
              {f >= t[1] ? <Word text="НЕ" size={150} color={K.red} /> : null}
            </div>
          </Slam>
        </Box>
        <Box top={820} height={420}>
          <Slam at={t[2]} from="scale">
            <div style={{ position: "relative", background: K.red, padding: "20px 40px", transform: glitch ? `translateX(${(random(`g${f}`) - 0.5) * 60}px)` : undefined }}>
              <div style={{ fontFamily: cond, fontWeight: 700, fontSize: 150, color: K.black, lineHeight: 1 }}>AFTER</div>
              <div style={{ fontFamily: cond, fontWeight: 700, fontSize: 150, color: K.black, lineHeight: 1, opacity: f >= t[3] ? 1 : 0.15 }}>EFFECTS</div>
              <div style={{ position: "absolute", left: -30, top: "50%", width: `${strike * 112}%`, height: 18, background: K.white, transform: "rotate(-8deg)", boxShadow: "0 0 30px #fff" }} />
            </div>
          </Slam>
        </Box>
      </AbsoluteFill>
      <Flash v={flash} />
    </AbsoluteFill>
  );
};

// 6 — ЭТО CLAUDE × REMOTION (lens-streak logo reveal)
export const S5: React.FC<SceneProps> = ({ w }) => {
  const f = useCurrentFrame();
  const t = w.map((x) => x.f);
  const { transform, flash } = useHits([t[1], t[3]]);
  const ca = (k: number) => `${k * 6}px 0 rgba(255,0,60,0.7), ${-k * 6}px 0 rgba(0,220,255,0.5), 0 0 40px ${K.red}`;
  const shimmer = 1 - p(f, t[1], 20);
  return (
    <AbsoluteFill>
      <Background bg="black" />
      <Streak at={t[1] - 4} y={940} />
      <AbsoluteFill style={{ transform }}>
        <div style={{ position: "absolute", top: 640, width: 1080, textAlign: "center", fontFamily: sans, fontWeight: 600, fontSize: 46, color: K.grey, letterSpacing: 10, opacity: p(f, t[0], 8) }}>ЭТО</div>
        <div style={{ position: "absolute", top: 760, width: 1080, display: "flex", justifyContent: "center", opacity: p(f, t[1], 6), transform: `scale(${interpolate(p(f, t[1], 14), [0, 1], [1.4, 1])})`, filter: `blur(${(1 - p(f, t[1], 10)) * 12}px)` }}>
          <div style={{ fontFamily: heavy, fontWeight: 900, fontSize: 150, color: K.white, textShadow: ca(shimmer + 0.2), fontStyle: "italic" }}>CLAUDE</div>
        </div>
        <div style={{ position: "absolute", top: 960, width: 1080, textAlign: "center", fontFamily: heavy, fontWeight: 900, fontSize: 110, color: K.red, transform: `scale(${p(f, t[2], 8, ease.back)}) rotate(${(1 - p(f, t[2], 8)) * 90}deg)` }}>×</div>
        <div style={{ position: "absolute", top: 1110, width: 1080, display: "flex", justifyContent: "center", opacity: p(f, t[3], 6), transform: `translateY(${(1 - p(f, t[3], 12)) * 120}px)`, filter: `blur(${(1 - p(f, t[3], 10)) * 12}px)` }}>
          <div style={{ fontFamily: heavy, fontWeight: 900, fontSize: 112, color: K.white, textShadow: ca(0.3) }}>REMOTION</div>
        </div>
        <div style={{ position: "absolute", top: 1300, width: 1080, textAlign: "center", fontFamily: mono, fontSize: 36, color: K.redHot, letterSpacing: 6, opacity: p(f, t[3] + 24, 10) }}>моушн-дизайн кодом</div>
      </AbsoluteFill>
      <Flash v={flash} />
    </AbsoluteFill>
  );
};

// 7 — ТЫ ПРОСТО ПИШЕШЬ, ЧТО ХОЧЕШЬ + prompt box typing
const PROMPT = "Сделай 15-секундный ролик: кинетический текст, красный, дерзко";
export const PromptBox: React.FC<{ text: string; typedFrom: number; cps?: number; glow?: number; top?: number; big?: boolean }> = ({ text, typedFrom, cps = 1.4, glow = 0.5, top = 980, big = false }) => {
  const f = useCurrentFrame();
  const n = Math.max(0, Math.floor((f - typedFrom) * cps));
  const shown = text.slice(0, n);
  const cursor = Math.floor(f / 15) % 2 === 0;
  return (
    <div
      style={{
        position: "absolute",
        left: 70,
        right: 70,
        top,
        minHeight: big ? 220 : 300,
        borderRadius: 34,
        background: "rgba(24,10,12,0.9)",
        border: `3px solid rgba(232,32,42,${0.5 + glow * 0.5})`,
        boxShadow: `0 0 ${40 + glow * 80}px rgba(232,32,42,${0.35 + glow * 0.4}), inset 0 0 40px rgba(232,32,42,0.12)`,
        padding: big ? "40px 50px" : "44px 50px",
        display: "flex",
        alignItems: big ? "center" : "flex-start",
      }}
    >
      <div style={{ fontFamily: big ? heavy : mono, fontWeight: big ? 900 : 500, fontSize: big ? 92 : 46, color: K.white, lineHeight: 1.35 }}>
        {big ? null : <span style={{ color: K.redHot }}>{"> "}</span>}
        {shown}
        <span style={{ display: "inline-block", width: big ? 12 : 6, height: big ? 90 : 50, background: K.redHot, marginLeft: 6, verticalAlign: "middle", opacity: cursor ? 1 : 0 }} />
      </div>
      {!big ? (
        <div style={{ position: "absolute", right: 30, bottom: 26, width: 70, height: 70, borderRadius: 20, background: K.red, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 40, color: "#fff" }}>↵</div>
      ) : null}
    </div>
  );
};

export const S6: React.FC<SceneProps> = ({ w }) => {
  const f = useCurrentFrame();
  const t = w.map((x) => x.f);
  const { transform, flash } = useHits([t[0], t[3]]);
  return (
    <AbsoluteFill>
      <Background bg="black" />
      <AbsoluteFill style={{ transform }}>
        <div style={{ position: "absolute", left: 80, top: 470, display: "flex", flexWrap: "wrap", gap: "0 28px", width: 920 }}>
          {["ТЫ", "ПРОСТО", "ПИШЕШЬ,"].map((s, i) => (
            <div key={s} style={{ fontFamily: heavy, fontWeight: 900, fontSize: 100, color: K.white, lineHeight: 1.15, transform: `translateY(${(1 - p(f, t[i], 8)) * 80}px)`, opacity: f >= t[i] ? 1 : 0, filter: `blur(${(1 - p(f, t[i], 8)) * 8}px)` }}>
              {s}
            </div>
          ))}
        </div>
        <div style={{ transform: `translateY(${(1 - p(f, t[2], 14)) * 600}px)`, opacity: p(f, t[2], 8) }}>
          <PromptBox text={PROMPT} typedFrom={t[2] + 6} cps={0.8} glow={0.4 + 0.3 * Math.sin(f / 12)} />
        </div>
        <Box top={1420} height={200}>
          <Slam at={t[3]} from="scale">
            <div style={{ display: "flex", gap: 30 }}>
              <Word text="ЧТО" size={100} color={K.red} />
              {f >= t[4] ? <Word text="ХОЧЕШЬ" size={100} color={K.red} /> : null}
            </div>
          </Slam>
        </Box>
      </AbsoluteFill>
      <Flash v={flash} />
    </AbsoluteFill>
  );
};

// 8 — А ОН ПРЕВРАЩАЕТ ЭТО В АНИМАЦИЮ + video card
export const S7: React.FC<SceneProps> = ({ w, len }) => {
  const f = useCurrentFrame();
  const t = w.map((x) => x.f);
  const { transform, flash } = useHits([t[2], t[5]]);
  const card = p(f, t[2], 14, ease.back);
  const play = (f - t[2]) / (len - t[2]);
  const st = p(f, t[5], 16, ease.out);
  return (
    <AbsoluteFill>
      <Background bg="black" />
      <AbsoluteFill style={{ transform }}>
        <div style={{ position: "absolute", left: 80, top: 430, display: "flex", flexWrap: "wrap", gap: "0 24px", width: 920 }}>
          {["А", "ОН", "ПРЕВРАЩАЕТ", "ЭТО"].map((s, i) => (
            <div key={i} style={{ fontFamily: heavy, fontWeight: 900, fontSize: 88, color: i === 2 ? K.redHot : K.white, lineHeight: 1.15, opacity: f >= t[i < 3 ? i : 3] ? 1 : 0, transform: `translateX(${(1 - p(f, t[i < 3 ? i : 3], 8)) * -200}px)` }}>
              {s}
            </div>
          ))}
        </div>
        <div style={{ position: "absolute", left: 70, right: 70, top: 690, height: 560, borderRadius: 30, background: "#120708", border: "3px solid #3a1215", overflow: "hidden", transform: `scale(${card})`, boxShadow: `0 0 80px rgba(232,32,42,0.3)` }}>
          <div style={{ position: "absolute", left: `${10 + ((f * 3) % 70)}%`, top: 180 + Math.abs(Math.sin(f / 9)) * -120 + 120, width: 120, height: 120, borderRadius: "50%", background: K.red, boxShadow: `0 0 60px ${K.red}` }} />
          <div style={{ position: "absolute", left: 50, top: 60, fontFamily: heavy, fontWeight: 900, fontSize: 70, color: K.white, opacity: 0.85 }}>КАДР {Math.max(1, Math.floor((f - t[2]) / 2))}</div>
          <div style={{ position: "absolute", left: 40, right: 40, bottom: 40, height: 10, background: "#3a1215", borderRadius: 5 }}>
            <div style={{ width: `${Math.min(100, Math.max(0, play * 100))}%`, height: 10, background: K.red, borderRadius: 5 }} />
          </div>
          <div style={{ position: "absolute", left: "50%", top: "50%", width: 150, height: 150, marginLeft: -75, marginTop: -75, borderRadius: 40, background: "rgba(255,255,255,0.12)", display: "flex", alignItems: "center", justifyContent: "center", opacity: 1 - p(f, t[2] + 20, 10) }}>
            <div style={{ width: 0, height: 0, borderTop: "40px solid transparent", borderBottom: "40px solid transparent", borderLeft: `66px solid ${K.white}`, marginLeft: 16 }} />
          </div>
        </div>
        <div style={{ position: "absolute", top: 1330, width: 1080, display: "flex", justifyContent: "center", gap: 30, opacity: f >= t[4] ? 1 : 0 }}>
          <div style={{ fontFamily: heavy, fontWeight: 900, fontSize: 94, color: K.white, transform: `scaleX(${interpolate(st, [0, 1], [3, 1])})`, filter: `blur(${(1 - st) * 10}px)` }}>
            {f >= t[5] ? "В АНИМАЦИЮ" : "В"}
          </div>
        </div>
      </AbsoluteFill>
      <Flash v={flash} />
    </AbsoluteFill>
  );
};

// 9 — ВСЁ, ЧТО НУЖНО — ОДИН ПРОМПТ
export const S8: React.FC<SceneProps> = ({ w }) => {
  const f = useCurrentFrame();
  const t = w.map((x) => x.f);
  const { transform, flash } = useHits([t[0], t[3], t[4]], 34);
  return (
    <AbsoluteFill>
      <Background bg="black" />
      <AbsoluteFill style={{ transform }}>
        <div style={{ position: "absolute", top: 520, left: 80, right: 80, display: "flex", flexWrap: "wrap", justifyContent: "center", gap: "0 26px" }}>
          {["ВСЁ,", "ЧТО", "НУЖНО —"].map((s, i) => (
            <div key={i} style={{ fontFamily: heavy, fontWeight: 900, fontSize: 96, color: K.white, lineHeight: 1.2, opacity: f >= t[i] ? 1 : 0, transform: `scale(${interpolate(p(f, t[i], 8), [0, 1], [1.8, 1])})`, filter: `blur(${(1 - p(f, t[i], 8)) * 8}px)` }}>
              {s}
            </div>
          ))}
        </div>
        {f >= t[3] ? (
          <div style={{ transform: `scale(${interpolate(p(f, t[3], 12, ease.back), [0, 1], [0.4, 1])})`, opacity: p(f, t[3], 6) }}>
            <PromptBox text={f >= t[4] ? "ОДИН ПРОМПТ" : "ОДИН"} typedFrom={-999} glow={0.8 + 0.2 * Math.sin(f / 6)} top={820} big />
          </div>
        ) : null}
      </AbsoluteFill>
      <Flash v={flash} color={f >= t[4] ? K.red : "#fff"} />
    </AbsoluteFill>
  );
};
