import type { StorefrontTheme, StorefrontThemeMode } from "~/types/storefront";
import { initialTheme } from "./storefront-defaults";

type Colors = StorefrontTheme["colors"];
export const storefrontPresets = ["Everyday", "Studio", "Sunrise"];
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
