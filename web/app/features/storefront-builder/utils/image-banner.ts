import type { SectionLayout } from "~/types/storefront";
export function imageBannerDefaults() {
  return {
    background: "#e8e4dc",
    image: { src: "", alt: "" },
    padding: { top: 44, right: 24, bottom: 44, left: 24 },
    layout: { direction: "horizontal", alignment: "space-between", position: "center", gap: 24, width: "page", height: "auto", customHeight: 0, alignTextBaseline: false } as SectionLayout,
  };
}
