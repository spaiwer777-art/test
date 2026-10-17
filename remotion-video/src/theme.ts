import "@fontsource-variable/inter";
import "@fontsource-variable/jetbrains-mono";

export const fontFamily = "'Inter Variable', sans-serif";
export const monoFamily = "'JetBrains Mono Variable', monospace";

export const colors = {
  bg: "#07071a",
  blue: "#0b84f3",
  cyan: "#3ee6ff",
  violet: "#8b5cf6",
  pink: "#ff4fa3",
  text: "#f4f6ff",
  muted: "#a7b0d6",
};

export const gradientText = {
  backgroundImage: `linear-gradient(100deg, ${colors.cyan}, ${colors.blue} 40%, ${colors.violet} 70%, ${colors.pink})`,
  WebkitBackgroundClip: "text",
  backgroundClip: "text",
  color: "transparent",
} as const;
