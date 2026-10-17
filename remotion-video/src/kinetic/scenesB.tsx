import React, { useMemo } from "react";
import { interpolate as flubber } from "flubber";
import { AbsoluteFill, interpolate, random, useCurrentFrame } from "remotion";
import { Background, Box, cond, ease, Flash, heavy, K, mono, p, sans, Slam, Streak, useHits, Word } from "./fx";
import type { SceneProps } from "./scenesA";

const polar = (n: number, r: (i: number) => number, rot = -Math.PI / 2) =>
  "M" + new Array(n).fill(0).map((_, i) => {
    const a = rot + (i / n) * Math.PI * 2;
    return `${(Math.cos(a) * r(i)).toFixed(3)},${(Math.sin(a) * r(i)).toFixed(3)}`;
  }).join(" L") + " Z";
const CIRCLE = polar(72, () => 1);
const TRI = polar(3, () => 1.25, -Math.PI / 2);
const SQUARE = polar(4, () => 1.2, -Math.PI / 4);
const STAR = polar(32, (i) => (i % 2 ? 0.55 : 1));

// 10 — feature montage: kinetic type / morphing shapes / 3D objects / fluid camera
export const S9: React.FC<SceneProps> = ({ w }) => {
  const f = useCurrentFrame();
  const t = w.map((x) => x.f);
  const cuts = [0, t[2] - 6, t[4] - 6, t[6] - 6];
  const part = cuts.filter((c) => f >= c).length - 1;
  const { transform, flash } = useHits([t[0], t[1], t[2], t[3], t[4], t[5], t[6], t[7]], 22);
  const m1 = useMemo(() => flubber(CIRCLE, TRI, { maxSegmentLength: 0.05 }), []);
  const m2 = useMemo(() => flubber(TRI, SQUARE, { maxSegmentLength: 0.05 }), []);

  const bg = part === 0 ? "red" : part === 2 ? "white" : "black";
  return (
    <AbsoluteFill>
      <Background bg={bg} />
      <AbsoluteFill style={{ transform }}>
        {part === 0 ? (
          <>
            <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", flexDirection: "row", top: -200 }}>
              {Array.from("КИНЕТИЧЕСКИЙ").map((ch, i) => {
                const k = p(f, t[0] + i * 2, 10, ease.back);
                const r = (random(`kr${i}`) - 0.5) * 80;
                return (
                  <span key={i} style={{ fontFamily: cond, fontWeight: 700, fontSize: 132, color: K.black, display: "inline-block", transform: `scale(${k}) rotate(${(1 - k) * r}deg) translateY(${Math.sin(f / 6 + i) * 6}px)` }}>
                    {ch}
                  </span>
                );
              })}
            </AbsoluteFill>
            <Box top={1000} height={300}>
              <Slam at={t[1]} from="scale">
                <Word text="ТЕКСТ" size={260} />
              </Slam>
            </Box>
          </>
        ) : part === 1 ? (
          (() => {
            const a = p(f, t[3] - 10, 12, ease.inOut);
            const b = p(f, t[3] + 30, 12, ease.inOut);
            const d = b > 0 ? m2(b) : a > 0 ? m1(a) : CIRCLE;
            const s = 330 * p(f, cuts[1], 14, ease.back);
            return (
              <>
                <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
                  <g transform={`translate(540 980) rotate(${f * 0.8}) scale(${s})`}>
                    <path d={d} fill={K.red} style={{ filter: `drop-shadow(0 0 0.1px ${K.red})` }} />
                  </g>
                </svg>
                <AbsoluteFill style={{ mixBlendMode: "difference" }}>
                  <Box top={760} height={220}>
                    <Slam at={t[2]} from="left" ghosts={5}>
                      <Word text="МОРФИНГ" size={128} color="#fff" />
                    </Slam>
                  </Box>
                  <Box top={1000} height={220}>
                    <Slam at={t[3]} from="right" ghosts={5}>
                      <Word text="ФИГУР" size={170} color="#fff" />
                    </Slam>
                  </Box>
                </AbsoluteFill>
              </>
            );
          })()
        ) : part === 2 ? (
          (() => {
            const ry = (f - cuts[2]) * 2.4;
            const rx = -20 + Math.sin(f / 20) * 10;
            const S = 300;
            const face = (tr: string, shade: number) => (
              <div style={{ position: "absolute", width: S, height: S, background: K.red, filter: `brightness(${shade})`, transform: tr, border: "2px solid rgba(0,0,0,0.15)" }} />
            );
            return (
              <>
                <div style={{ position: "absolute", left: 600, top: 640, width: S, height: S, perspective: 1200, transform: `scale(${p(f, cuts[2], 12, ease.back)})` }}>
                  <div style={{ width: S, height: S, position: "relative", transformStyle: "preserve-3d", transform: `rotateX(${rx}deg) rotateY(${ry}deg)` }}>
                    {face(`translateZ(${S / 2}px)`, 1)}
                    {face(`rotateY(180deg) translateZ(${S / 2}px)`, 0.6)}
                    {face(`rotateY(90deg) translateZ(${S / 2}px)`, 0.75)}
                    {face(`rotateY(-90deg) translateZ(${S / 2}px)`, 0.85)}
                    {face(`rotateX(90deg) translateZ(${S / 2}px)`, 1.25)}
                    {face(`rotateX(-90deg) translateZ(${S / 2}px)`, 0.5)}
                  </div>
                </div>
                <div style={{ position: "absolute", left: 90, top: 610, fontFamily: heavy, fontWeight: 900, fontSize: 330, color: K.black, transform: `scale(${interpolate(p(f, t[4], 8), [0, 1], [2, 1])})`, transformOrigin: "0 50%", filter: `blur(${(1 - p(f, t[4], 8)) * 10}px)`, opacity: f >= t[4] ? 1 : 0 }}>
                  3D
                </div>
                <Box top={1080} height={200}>
                  <Slam at={t[5]} from="down" ghosts={5}>
                    <Word text="ОБЪЕКТЫ" size={132} color={K.black} />
                  </Slam>
                </Box>
              </>
            );
          })()
        ) : (
          (() => {
            const spin = (f - cuts[3]) * 3.2;
            const words = ["ПЛАВНАЯ", "КАМЕРА", "ПЛАВНАЯ", "КАМЕРА", "ПЛАВНАЯ", "КАМЕРА"];
            return (
              <AbsoluteFill style={{ perspective: 1100 }}>
                <AbsoluteFill style={{ transformStyle: "preserve-3d", transform: `rotateX(${14 + Math.sin(f / 18) * 8}deg) rotateY(${spin}deg) rotateZ(${Math.sin(f / 25) * 8}deg)` }}>
                  {words.map((s, i) => {
                    const a = (i / words.length) * 360;
                    const on = f >= (i % 2 ? t[7] : t[6]);
                    return (
                      <div key={i} style={{ position: "absolute", left: 0, right: 0, top: 860 + (i % 2 ? 140 : -60), display: "flex", justifyContent: "center", transform: `rotateY(${a}deg) translateZ(520px)`, backfaceVisibility: "hidden", opacity: on ? 1 : 0 }}>
                        <Word text={s} size={104} color={i % 2 ? K.redHot : K.white} />
                      </div>
                    );
                  })}
                </AbsoluteFill>
                <Streak at={t[7] + 10} y={1000} dur={18} />
              </AbsoluteFill>
            );
          })()
        )}
      </AbsoluteFill>
      <Flash v={flash} />
    </AbsoluteFill>
  );
};

// 11 — БУКВЫ ТЯНУТСЯ / КАМЕРА ЛЕТИТ СКВОЗЬ ТЕКСТ
export const S10: React.FC<SceneProps> = ({ w }) => {
  const f = useCurrentFrame();
  const t = w.map((x) => x.f);
  const tunnel = f >= t[2] - 4;
  const { transform, flash } = useHits([t[0], t[1], t[2]]);
  return (
    <AbsoluteFill>
      <Background bg={tunnel ? "black" : "white"} glow={tunnel} />
      <AbsoluteFill style={{ transform }}>
        {!tunnel ? (
          <>
            <div style={{ position: "absolute", top: 560, width: 1080, display: "flex", justifyContent: "center" }}>
              {Array.from("БУКВЫ").map((ch, i) => {
                const k = p(f, t[0] + i * 3, 18, ease.back);
                return (
                  <span key={i} style={{ fontFamily: heavy, fontWeight: 900, fontSize: 190, color: K.black, display: "inline-block", transform: `scaleY(${interpolate(k, [0, 1], [4, 1])})`, transformOrigin: "50% 100%", opacity: k > 0 ? 1 : 0 }}>
                    {ch}
                  </span>
                );
              })}
            </div>
            <div style={{ position: "absolute", top: 950, left: 0, right: 0, height: 280, background: K.red, display: "flex", alignItems: "center", justifyContent: "center", overflow: "hidden", transform: `scaleY(${p(f, t[1] - 4, 8)})` }}>
              <div style={{ fontFamily: heavy, fontWeight: 900, fontSize: 170, color: K.white, transform: `scaleX(${interpolate(p(f, t[1], 24, ease.out), [0, 1], [3.6, 1]) + Math.sin(f / 5) * 0.02})`, filter: `blur(${(1 - p(f, t[1], 12)) * 8}px)` }}>
                ТЯНУТСЯ
              </div>
            </div>
          </>
        ) : (
          <>
            {/* speed lines from the centre */}
            <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
              {new Array(36).fill(0).map((_, i) => {
                const a = (i / 36) * Math.PI * 2 + random(`sl${i}`) * 0.1;
                const u = ((f * 0.045 + random(`su${i}`)) % 1);
                const r1 = 120 + u * 1500;
                const r2 = r1 + 120 + u * 400;
                return <line key={i} x1={540 + Math.cos(a) * r1} y1={960 + Math.sin(a) * r1} x2={540 + Math.cos(a) * r2} y2={960 + Math.sin(a) * r2} stroke={i % 4 ? "#fff" : K.red} strokeWidth={2 + u * 4} opacity={0.15 + u * 0.5} />;
              })}
            </svg>
            {["КАМЕРА", "ЛЕТИТ", "СКВОЗЬ", "ТЕКСТ"].map((s, i) => {
              const at = t[2 + i];
              const life = (f - at) / 46;
              if (life < -0.05 || life > 1) return null;
              const z = Math.pow(Math.max(0, life), 2.4);
              const scale = 0.15 + z * 9;
              return (
                <AbsoluteFill key={s} style={{ alignItems: "center", justifyContent: "center", transform: `scale(${scale})`, opacity: life < 0 ? 0 : life > 0.75 ? (1 - life) * 4 : 1, filter: `blur(${z * 6}px)` }}>
                  <Word text={s} size={150} color={i % 2 ? K.redHot : K.white} />
                </AbsoluteFill>
              );
            })}
          </>
        )}
      </AbsoluteFill>
      <Flash v={flash} />
    </AbsoluteFill>
  );
};

// 12 — ТЫ СЕРЬЁЗНО?!
export const S11: React.FC<SceneProps> = ({ w }) => {
  const f = useCurrentFrame();
  const t = w.map((x) => x.f);
  const { transform, flash } = useHits([t[1]], 40);
  const jit = f >= t[1] ? `translate(${(random(`j${f}`) - 0.5) * 14}px, ${(random(`k${f}`) - 0.5) * 14}px)` : "";
  return (
    <AbsoluteFill>
      <Background bg="white" />
      <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
        <g transform={`translate(${620 + Math.sin(f / 8) * 10} 1000) rotate(${f * 1.4}) scale(${430 * p(f, t[1] - 4, 10, ease.back) * (1 + Math.sin(f / 4) * 0.03)})`}>
          <path d={STAR} fill={K.red} />
        </g>
      </svg>
      <AbsoluteFill style={{ transform }}>
        <div style={{ position: "absolute", left: 80, top: 640, fontFamily: heavy, fontWeight: 900, fontSize: 200, color: K.black, transform: `scale(${interpolate(p(f, t[0], 6), [0, 1], [2, 1])})`, transformOrigin: "0 50%", opacity: f >= t[0] ? 1 : 0 }}>ТЫ</div>
        <div style={{ position: "absolute", left: 80, top: 880, fontFamily: heavy, fontWeight: 900, fontSize: 110, color: K.black, transform: `${jit} scale(${interpolate(p(f, t[1], 6), [0, 1], [2.2, 1])})`, transformOrigin: "0 50%", opacity: f >= t[1] ? 1 : 0, lineHeight: 1 }}>
          СЕРЬЁЗНО
          <br />
          <span style={{ color: K.white, WebkitTextStroke: `5px ${K.black}`, fontSize: 220 }}>?!</span>
        </div>
      </AbsoluteFill>
      <Flash v={flash} color={K.red} />
    </AbsoluteFill>
  );
};

// 13 — the usual way: layers, keyframes, graphs… then render
export const S12: React.FC<SceneProps> = ({ w }) => {
  const f = useCurrentFrame();
  const t = w.map((x) => x.f);
  const { transform, flash } = useHits([t[5], t[6], t[7], t[10]], 22);
  const endPart = f >= t[8] - 4;
  const hours = Math.min(4, (f - t[10]) / 30);
  return (
    <AbsoluteFill>
      <AbsoluteFill style={{ background: "#151517" }} />
      <AbsoluteFill style={{ background: "radial-gradient(ellipse at 50% 50%, rgba(255,255,255,0.06), transparent 70%)" }} />
      <AbsoluteFill style={{ transform, filter: "grayscale(1)" }}>
        {!endPart ? (
          <>
            <div style={{ position: "absolute", left: 80, top: 330, width: 920, opacity: 1 - p(f, t[5] - 6, 8) * 0.75 }}>
              <div style={{ fontFamily: heavy, fontWeight: 900, fontSize: 104, color: K.white, opacity: p(f, t[0], 6) }}>ПРЕДСТАВЬ,</div>
              <div style={{ display: "flex", gap: 20, marginTop: 10 }}>
                {["КАК", "ДЕЛАТЬ", "ЭТО"].map((s, i) => (
                  <div key={s} style={{ fontFamily: sans, fontWeight: 300, fontSize: 70, color: K.white, opacity: f >= t[1 + i] ? 1 : 0 }}>{s}</div>
                ))}
              </div>
              <div style={{ fontFamily: "Georgia, serif", fontStyle: "italic", fontSize: 92, color: "#bbb", opacity: p(f, t[4], 10) }}>по-старому</div>
            </div>
            {/* layers */}
            {f >= t[5] ? new Array(9).fill(0).map((_, i) => (
              <div key={i} style={{ position: "absolute", left: 80 + (i % 3) * 30, right: 80, top: 760 + i * 46, height: 38, background: i % 3 === 0 ? "#555" : "#3a3a3d", borderLeft: "10px solid #888", transform: `translateX(${(1 - p(f, t[5] + i * 2, 10)) * 1100}px) rotate(${(random(`lr${i}`) - 0.5) * 3}deg)`, fontFamily: mono, fontSize: 22, color: "#ccc", paddingLeft: 14, lineHeight: "38px" }}>
                Layer {i + 1} · Shape {Math.floor(random(`ls${i}`) * 40)}
              </div>
            )) : null}
            {/* keyframes */}
            {f >= t[6] ? new Array(5).fill(0).map((_, r) =>
              new Array(9).fill(0).map((__, c) => (
                <div key={`${r}-${c}`} style={{ position: "absolute", left: 120 + c * 96 + (r % 2) * 30, top: 820 + r * 70, width: 26, height: 26, background: "#ddd", transform: `rotate(45deg) scale(${p(f, t[6] + (r + c) * 1.2, 6, ease.back)})` }} />
              )),
            ) : null}
            {/* graph editor curves */}
            {f >= t[7] ? (
              <svg width={1080} height={1920} style={{ position: "absolute", inset: 0 }}>
                {new Array(5).fill(0).map((_, i) => {
                  const d = `M80,${1100 - i * 30} C${300 + i * 40},${600 + i * 60} ${600 - i * 30},${1300 - i * 50} 1000,${800 + i * 50}`;
                  return <path key={i} d={d} fill="none" stroke={i % 2 ? "#eee" : "#999"} strokeWidth={5} pathLength={1} strokeDasharray="1 1" strokeDashoffset={1 - p(f, t[7] + i * 3, 16)} />;
                })}
              </svg>
            ) : null}
            {/* word pile */}
            {[
              { s: "СЛОИ.", at: t[5], x: 80, y: 1330, r: -4 },
              { s: "КЛЮЧИ.", at: t[6], x: 160, y: 1470, r: 3 },
              { s: "ГРАФИКИ.", at: t[7], x: 50, y: 1610, r: -2 },
            ].map((o) => (
              <div key={o.s} style={{ position: "absolute", left: o.x, top: o.y, fontFamily: heavy, fontWeight: 900, fontSize: 112, color: K.white, transform: `rotate(${o.r}deg) scale(${interpolate(p(f, o.at, 8), [0, 1], [2.2, 1])})`, opacity: f >= o.at ? 1 : 0, filter: `blur(${(1 - p(f, o.at, 8)) * 10}px)` }}>
                {o.s}
              </div>
            ))}
          </>
        ) : (
          <>
            <div style={{ position: "absolute", top: 560, width: 1080, display: "flex", justifyContent: "center", gap: 24 }}>
              {["А", "ПОТОМ —"].map((s, i) => (
                <div key={s} style={{ fontFamily: heavy, fontWeight: 900, fontSize: 96, color: K.white, opacity: f >= t[8 + i] ? 1 : 0 }}>{s}</div>
              ))}
            </div>
            <Box top={760} height={260}>
              <Slam at={t[10]} from="scale">
                <Word text="РЕНДЕР" size={150} dot />
              </Slam>
            </Box>
            <div style={{ position: "absolute", top: 1100, width: 1080, textAlign: "center", fontFamily: mono, fontWeight: 700, fontSize: 110, color: K.white, opacity: f >= t[10] + 6 ? 1 : 0 }}>
              ⌛ {String(Math.floor(hours)).padStart(2, "0")}:{String(Math.floor((hours * 60) % 60)).padStart(2, "0")}:{String(Math.floor((hours * 3600) % 60)).padStart(2, "0")}
            </div>
            <div style={{ position: "absolute", left: 140, right: 140, top: 1280, height: 16, background: "#333", opacity: f >= t[10] + 6 ? 1 : 0 }}>
              <div style={{ width: `${Math.min(3, hours * 0.75)}%`, height: 16, background: "#ccc" }} />
            </div>
            <div style={{ position: "absolute", top: 1320, width: 1080, textAlign: "center", fontFamily: mono, fontSize: 36, color: "#888", opacity: f >= t[10] + 6 ? 1 : 0 }}>рендер: {Math.min(3, Math.floor(hours * 0.75))}%</div>
          </>
        )}
      </AbsoluteFill>
      <Flash v={flash} />
    </AbsoluteFill>
  );
};

// 14 — А ЗДЕСЬ? ПРОМПТ. КОД. РЕНДЕР.
export const S13: React.FC<SceneProps> = ({ w }) => {
  const f = useCurrentFrame();
  const t = w.map((x) => x.f);
  const list = f >= t[2] - 6;
  const { transform, flash } = useHits([t[1], t[2], t[3], t[4]], 30);
  const rows = [
    { icon: ">_", s: "ПРОМПТ", at: t[2] },
    { icon: "✦", s: "КОД", at: t[3] },
    { icon: "▶", s: "РЕНДЕР", at: t[4] },
  ];
  const all = p(f, t[4] + 40, 10);
  return (
    <AbsoluteFill>
      <Background bg={list ? "black" : "red"} />
      <AbsoluteFill style={{ transform }}>
        {!list ? (
          <Slam at={t[1]} from="left" ghosts={7}>
            <div style={{ transform: "rotate(-6deg)", textAlign: "center" }}>
              <Word text="А ЗДЕСЬ?" size={118} />
            </div>
          </Slam>
        ) : (
          <>
            <div style={{ position: "absolute", left: 160, top: 640, width: 6, height: 640, background: `linear-gradient(${K.red}, ${K.redHot})`, transform: `scaleY(${p(f, t[2], 30)})`, transformOrigin: "50% 0", boxShadow: `0 0 20px ${K.red}` }} />
            {rows.map((r, i) => {
              const active = f >= r.at && (i === 2 || f < rows[i + 1].at);
              const on = Math.max(active ? 1 : 0.35, all);
              const k = p(f, r.at, 10, ease.back);
              return (
                <div key={r.s} style={{ position: "absolute", left: 110, top: 600 + i * 300, display: "flex", alignItems: "center", gap: 46, opacity: f >= r.at ? on : 0.12, transform: `translateX(${(1 - k) * -300}px) scale(${active ? 1.06 : 1})`, transformOrigin: "0 50%" }}>
                  <div style={{ width: 110, height: 110, borderRadius: 30, background: active || all > 0 ? K.red : "#2a0d10", display: "flex", alignItems: "center", justifyContent: "center", fontFamily: mono, fontWeight: 700, fontSize: 48, color: "#fff", boxShadow: active ? `0 0 50px ${K.red}` : "none" }}>{r.icon}</div>
                  <Word text={r.s} size={118} dot />
                </div>
              );
            })}
            <div style={{ position: "absolute", top: 1560, width: 1080, textAlign: "center", fontFamily: mono, fontSize: 40, color: K.redHot, letterSpacing: 6, opacity: all }}>минуты, а не дни</div>
          </>
        )}
      </AbsoluteFill>
      <Flash v={flash} color={K.red} />
    </AbsoluteFill>
  );
};

// 15 — И ЭТО ВИДЕО — ТОЖЕ. + outro
export const S14: React.FC<SceneProps> = ({ w, len }) => {
  const f = useCurrentFrame();
  const t = w.map((x) => x.f);
  const { transform, flash } = useHits([t[3]], 44);
  const frameIn = p(f, t[0], 16, ease.out);
  const outro = len - 6;
  const o = p(f, outro, 12);
  return (
    <AbsoluteFill>
      <Background bg="black" />
      <AbsoluteFill style={{ transform }}>
        <div style={{ position: "absolute", inset: interpolate(frameIn, [0, 1], [400, 60]), border: `4px solid ${K.red}`, borderRadius: 40, boxShadow: `0 0 60px ${K.red}55, inset 0 0 60px ${K.red}33`, opacity: 1 - o }}>
          <div style={{ position: "absolute", left: 40, bottom: 40, right: 40, height: 10, background: "#3a1215", borderRadius: 5 }}>
            <div style={{ width: `${Math.min(100, (f / (len + 120)) * 100)}%`, height: 10, background: K.red, borderRadius: 5 }} />
          </div>
          <div style={{ position: "absolute", left: 40, top: 40, fontFamily: mono, fontSize: 30, color: K.redHot }}>● REC · ЭТО ВИДЕО</div>
        </div>
        <div style={{ position: "absolute", top: 600, width: 1080, display: "flex", justifyContent: "center", gap: 24, opacity: 1 - o }}>
          {["И", "ЭТО", "ВИДЕО —"].map((s, i) => (
            <div key={s} style={{ fontFamily: heavy, fontWeight: 900, fontSize: 74, color: K.white, opacity: f >= t[i] ? 1 : 0, transform: `translateY(${(1 - p(f, t[i], 8)) * 60}px)` }}>{s}</div>
          ))}
        </div>
        <Box top={820} height={340}>
          <div style={{ opacity: 1 - o, width: "100%", height: "100%" }}>
            <Slam at={t[3]} from="scale" ghosts={6}>
              <Word text="ТОЖЕ" size={215} dot />
            </Slam>
          </div>
        </Box>
        {/* outro card */}
        {o > 0 ? (
          <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", opacity: o }}>
            <div style={{ fontFamily: mono, fontSize: 34, letterSpacing: 8, color: K.redHot }}>СДЕЛАНО ОДНИМ ПРОМПТОМ</div>
            <div style={{ fontFamily: heavy, fontWeight: 900, fontSize: 110, color: K.white, marginTop: 30, textAlign: "center", lineHeight: 1.05 }}>
              CLAUDE
              <br />
              <span style={{ color: K.red }}>×</span> REMOTION
            </div>
            <Streak at={outro + 4} y={1240} dur={20} />
            <div style={{ position: "absolute", bottom: 150, width: 1080, textAlign: "center", fontFamily: sans, fontSize: 24, lineHeight: 1.6, color: "#777" }}>
              Музыка: Kevin MacLeod — «Cyborg Ninja» (incompetech.com), CC BY 4.0
              <br />
              Звуки: Kenney (kenney.nl), CC0
            </div>
          </AbsoluteFill>
        ) : null}
      </AbsoluteFill>
      <Flash v={flash} />
    </AbsoluteFill>
  );
};
