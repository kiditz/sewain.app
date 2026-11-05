import type { StorefrontTheme, StorefrontThemeMode } from "~/types/storefront";
import { initialTheme } from "./storefront-defaults";

type Colors = StorefrontTheme["colors"];
export const storefrontPresets = ["Everyday", "Studio", "Sunrise", "Trail", "Coast", "Atelier", "Bloom", "Mono", "Midnight", "Terra"];
const palettes: Record<string, Record<StorefrontThemeMode, Colors>> = {
  everyday: {
    light: { ...initialTheme.colors },
    dark: { primary: "#94cdb0", secondary: "#293b31", background: "#121916", surface: "#1c2721", text: "#ecf3ee" },
  },
  studio: {
    light: { primary: "#343c65", secondary: "#e2e4f1", background: "#fafaff", surface: "#ffffff", text: "#252738" },
    dark: { primary: "#adb7ff", secondary: "#292d43", background: "#14161e", surface: "#1d2030", text: "#eeeefa" },
  },
  sunrise: {
    light: { primary: "#a44e2c", secondary: "#f2dfd1", background: "#fffaf5", surface: "#ffffff", text: "#332820" },
    dark: { primary: "#f7ad88", secondary: "#3a2922", background: "#1b1512", surface: "#29201b", text: "#faeee6" },
  },
  trail: {
    light: { primary: "#34512f", secondary: "#dde5cf", background: "#f5f5ec", surface: "#ffffff", text: "#202b1d" },
    dark: { primary: "#b4cf91", secondary: "#2b3826", background: "#151c13", surface: "#202a1c", text: "#edf3e5" },
  },
  coast: {
    light: { primary: "#17647a", secondary: "#d8edf0", background: "#f6fbfc", surface: "#ffffff", text: "#17323d" },
    dark: { primary: "#8ad5e2", secondary: "#203e48", background: "#101e25", surface: "#1a3039", text: "#e6f5f8" },
  },
  atelier: {
    light: { primary: "#6b4055", secondary: "#e9dde4", background: "#faf6f8", surface: "#ffffff", text: "#33242d" },
    dark: { primary: "#e0b3ca", secondary: "#412d38", background: "#21181e", surface: "#30232c", text: "#f7eaf1" },
  },
  bloom: {
    light: { primary: "#75425f", secondary: "#f1dfeb", background: "#fff8fc", surface: "#ffffff", text: "#382532" },
    dark: { primary: "#edb3d3", secondary: "#452b3d", background: "#241821", surface: "#33232e", text: "#fbeaf5" },
  },
  mono: {
    light: { primary: "#252525", secondary: "#e6e6e6", background: "#fafafa", surface: "#ffffff", text: "#181818" },
    dark: { primary: "#ededed", secondary: "#323232", background: "#121212", surface: "#202020", text: "#f5f5f5" },
  },
  midnight: {
    light: { primary: "#5145a2", secondary: "#e5e0f5", background: "#f9f8ff", surface: "#ffffff", text: "#27213d" },
    dark: { primary: "#c7bbff", secondary: "#312b49", background: "#13111d", surface: "#211d30", text: "#f0ecff" },
  },
  terra: {
    light: { primary: "#925035", secondary: "#ebdacb", background: "#faf6ef", surface: "#fffdf9", text: "#38291f" },
    dark: { primary: "#e5b18e", secondary: "#423025", background: "#201812", surface: "#2d231b", text: "#f5eade" },
  },
};

export function presetColors(preset: string, mode: StorefrontThemeMode): Colors {
  return { ...(palettes[preset.toLowerCase()] ?? palettes.everyday!)[mode] };
}

export function setStorefrontThemeMode(theme: StorefrontTheme, mode: StorefrontThemeMode) {
  if (mode === (theme.mode ?? "light")) return;
  theme.colorSchemes = {
    ...theme.colorSchemes,
    [theme.mode ?? "light"]: { ...theme.colors },
  };
  theme.colors = { ...(theme.colorSchemes[mode] ?? presetColors(theme.preset, mode)) };
  theme.mode = mode;
}

export function applyStorefrontPreset(theme: StorefrontTheme, name: string) {
  theme.name = name;
  theme.preset = name.toLowerCase();
  theme.colorSchemes = { light: presetColors(theme.preset, "light"), dark: presetColors(theme.preset, "dark") };
  theme.colors = { ...theme.colorSchemes[theme.mode ?? "light"]! };
}
