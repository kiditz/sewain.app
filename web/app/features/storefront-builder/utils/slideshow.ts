import type { SectionLayout, StorefrontBlock, StorefrontPadding } from "~/types/storefront";

function settingsObject(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {};
}

export function slideshowDefaults() {
  return {
    autoplay: false,
    interval: 5,
    showArrows: true,
    showIndicators: true,
    layout: { width: "full", height: "medium", customHeight: 480 } as Pick<SectionLayout, "width" | "height" | "customHeight">,
    padding: { top: 0, right: 0, bottom: 0, left: 0 },
  };
}

export function slideDefaults() {
  return {
    name: "Slide",
    image: { src: "", alt: "" },
    appearance: { backgroundColor: "#00000000", overlayColor: "#121212", overlayOpacity: 0 },
    padding: { top: 48, right: 48, bottom: 48, left: 48 },
    layout: { direction: "vertical", alignment: "left", position: "center", gap: 20, width: "page", alignTextBaseline: false } as Omit<SectionLayout, "height" | "customHeight">,
  };
}

export function slideshowSettings(settings: Record<string, unknown>) {
  const defaults = slideshowDefaults();
  return {
    ...defaults, ...settings,
    layout: { ...defaults.layout, ...settingsObject(settings.layout) } as typeof defaults.layout,
    padding: { ...defaults.padding, ...settingsObject(settings.padding) } as StorefrontPadding,
    autoplay: settings.autoplay === true,
    interval: Math.min(30, Math.max(2, Number(settings.interval) || defaults.interval)),
    showArrows: settings.showArrows !== false,
    showIndicators: settings.showIndicators !== false,
  };
}

export function slideSettings(settings: Record<string, unknown>) {
  const defaults = slideDefaults();
  return {
    ...defaults, ...settings,
    name: typeof settings.name === "string" ? settings.name : defaults.name,
    image: { ...defaults.image, ...settingsObject(settings.image) } as typeof defaults.image,
    appearance: { ...defaults.appearance, ...settingsObject(settings.appearance) } as typeof defaults.appearance,
    padding: { ...defaults.padding, ...settingsObject(settings.padding) } as StorefrontPadding,
    layout: { ...defaults.layout, ...settingsObject(settings.layout) } as typeof defaults.layout,
  };
}

export function slideContentLayout(settings: Record<string, unknown>): SectionLayout {
  return { ...slideSettings(settings).layout, width: "full", height: "auto", customHeight: 0 };
}

export function slideContainingBlock(slides: StorefrontBlock[], id?: string | null): StorefrontBlock | undefined {
  if (!id) return;
  const contains = (block: StorefrontBlock): boolean => block.id === id || !!block.blocks?.some(contains);
  return slides.find(contains);
}

export function nextSlideId(slides: StorefrontBlock[], current: string | null) {
  if (!slides.length) return null;
  const index = slides.findIndex(slide => slide.id === current);
  return slides[(index + 1) % slides.length]!.id;
}
