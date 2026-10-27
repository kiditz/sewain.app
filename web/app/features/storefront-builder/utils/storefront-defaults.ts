import type { StorefrontTheme } from "~/types/storefront";
export const initialTheme: StorefrontTheme = {
  name: "Everyday",
  preset: "everyday",
  colors: {
    primary: "#315c4c",
    secondary: "#d9e7de",
    background: "#fbfaf7",
    surface: "#ffffff",
    text: "#202a25",
  },
  typography: {
    bodyFont: "Inter, Arial, sans-serif",
    headingFont: "Georgia, serif",
    baseFontSize: 16,
  },
  layout: { contentMaxWidth: 1200, sectionSpacing: 64, borderRadius: 16 },
};
