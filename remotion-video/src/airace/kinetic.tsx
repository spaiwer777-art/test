import React from "react";
import { random, useCurrentFrame } from "remotion";
import { display, E, mono, prog } from "./theme";

const GLYPHS = "01ABCDEFGHJKLMNPRSTUVXYZ#%&@$*<>/\\=+";

export type KMode = "decode" | "flip" | "elastic" | "slice";

// Kinetic text: every letter is animated on its own, with an exit that sends letters flying.
export const KText: React.FC<{
  text: string;
  start: number;
  out?: number;
  mode?: KMode;
  stagger?: number;
  style?: React.CSSProperties;
  font?: string;
}> = ({
  text,
  start,
  out = 99999,
  mode = "flip",
  stagger = 1.6,
  style,
  font,
}) => {
  const f = useCurrentFrame();
  const chars = Array.from(text);
  // group letters into words so lines only break between words
  const words: { ch: string; i: number }[][] = [[]];
  chars.forEach((ch, i) => {
    if (ch === " ") words.push([]);
    else words[words.length - 1].push({ ch, i });
  });
  const renderChar = (ch: string, i: number) => {
    const t0 = start + i * stagger;
    const p = prog(
      f,
      t0,
      mode === "elastic" ? 18 : 14,
      mode === "elastic" ? E.backOut : E.expoOut,
    );
    const q = prog(f, out + (chars.length - i) * 0.8, 10, E.expoIn);
    let shown = ch;
    let transform = "";
    let filter = "";
    let opacity = Math.min(1, p * 1.6) * (1 - q);
    switch (mode) {
      case "decode": {
        // scrambles through random glyphs, then locks onto the real one
        const lock = t0 + 9;
        if (ch !== " " && f < lock)
          shown =
            GLYPHS[
              Math.floor(
                random(`g${text}${i}${Math.floor(f / 2)}`) * GLYPHS.length,
              )
            ];
        opacity = (f >= t0 ? 1 : 0) * (1 - q);
        transform = `translateY(${(1 - p) * 30}%)`;
        break;
      }
      case "flip":
        transform = `rotateX(${(1 - p) * -95}deg) translateY(${(1 - p) * 40}%)`;
        filter = `blur(${(1 - p) * 8}px)`;
        break;
      case "elastic":
        transform = `scale(${p}) rotate(${(1 - p) * (i % 2 ? 25 : -25)}deg)`;
        break;
      case "slice":
        transform = `translateX(${(1 - p) * (i % 2 ? 120 : -120)}px) skewX(${(1 - p) * -30}deg)`;
        filter = `blur(${(1 - p) * 6}px)`;
        break;
    }
    transform += ` translateY(${q * -140}%) rotate(${q * (i % 2 ? 20 : -20)}deg)`;
    if (q > 0) filter += ` blur(${q * 10}px)`;
    return (
      <span
        key={i}
        style={{
          display: "inline-block",
          transformOrigin: "50% 100%",
          transform,
          filter,
          opacity,
        }}
      >
        {shown}
      </span>
    );
  };
  return (
    <div
      style={{
        display: "flex",
        flexWrap: "wrap",
        columnGap: "0.28em",
        perspective: 900,
        fontFamily: font,
        ...style,
      }}
    >
      {words.map((w, k) => (
        <span key={k} style={{ display: "inline-flex", whiteSpace: "nowrap" }}>
          {w.map(({ ch, i }) => renderChar(ch, i))}
        </span>
      ))}
    </div>
  );
};

// Section caption: decoding year + flipping title + a highlight bar that sweeps under it.
export const KCaption: React.FC<{
  year?: string;
  title: string;
  start: number;
  out?: number;
  accent: string;
  color?: string;
  top?: number;
  mode?: KMode;
}> = ({
  year,
  title,
  start,
  out = 99999,
  accent,
  color = "#EEF1F8",
  top = 220,
  mode = "flip",
}) => {
  const f = useCurrentFrame();
  const bar = prog(f, start + 6, 18, E.expoInOut);
  const barOut = prog(f, out, 10, E.expoIn);
  return (
    <div style={{ position: "absolute", left: 80, right: 80, top }}>
      {year ? (
        <KText
          text={year}
          start={start}
          out={out}
          mode="decode"
          stagger={1}
          font={mono}
          style={{
            fontWeight: 700,
            fontSize: 42,
            color: accent,
            letterSpacing: 6,
          }}
        />
      ) : null}
      <KText
        text={title}
        start={start + 3}
        out={out}
        mode={mode}
        stagger={1.1}
        font={display}
        style={{
          fontWeight: 900,
          fontSize: 62,
          color,
          lineHeight: 1.12,
          marginTop: 6,
        }}
      />
      <div
        style={{
          height: 6,
          marginTop: 14,
          width: `${(bar - barOut) * 70}%`,
          marginLeft: `${barOut * 70}%`,
          background: accent,
          boxShadow: `0 0 18px ${accent}`,
        }}
      />
    </div>
  );
};
