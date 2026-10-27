import type {
  StorefrontBlock,
  StorefrontBorderRadius,
  StorefrontPadding,
  StorefrontTheme,
} from "~/types/storefront";
import type { CSSProperties } from "vue";
import type { SectionLayout } from "~/types/storefront";

export function getBlockSettings<T>(block: StorefrontBlock): T {
  return block.settings as T;
}

export function getLayoutHeight(layout: SectionLayout): string {
  switch (layout.height) {
    case "auto":
      return "auto";
    case "small":
      return "320px";
    case "medium":
      return "480px";
    case "large":
      return "640px";
    case "custom":
      return `${layout.customHeight}px`;
  }
}

export function getLayoutStyle(
  value: SectionLayout | undefined,
  theme: StorefrontTheme,
): CSSProperties {
  const layout: SectionLayout = value ?? {
    direction: "vertical",
    alignment: "left",
    position: "center",
    gap: 16,
    width: "page",
    height: "auto",
    customHeight: 0,
    alignTextBaseline: false,
  };

  const horizontal = layout.direction === "horizontal";

  const horizontalAlignment: CSSProperties["justifyContent"] =
    layout.alignment === "left"
      ? "flex-start"
      : layout.alignment === "right"
        ? "flex-end"
        : layout.alignment;

  const verticalAlignment: CSSProperties["alignItems"] =
    layout.position === "top"
      ? "flex-start"
      : layout.position === "bottom"
        ? "flex-end"
        : "center";

  return {
    display: "flex",
    flexDirection: horizontal ? "row" : "column",

    alignItems: horizontal
      ? layout.alignTextBaseline
        ? "baseline"
        : verticalAlignment
      : layout.alignment === "left"
        ? "flex-start"
        : layout.alignment === "right"
          ? "flex-end"
          : "center",

    justifyContent: horizontal
      ? horizontalAlignment
      : layout.position === "top"
        ? "flex-start"
        : layout.position === "bottom"
          ? "flex-end"
          : "center",

    textAlign:
      layout.alignment === "left" || layout.alignment === "right"
        ? layout.alignment
        : "center",

    gap: `${layout.gap}px`,
    width: layout.width === "full" ? "100%" : undefined,
    maxWidth:
      layout.width === "page"
        ? `${theme.layout.contentMaxWidth}px`
        : undefined,
    height: getLayoutHeight(layout),
    margin: layout.width === "page" ? "0 auto" : undefined,
  };
}

export function getPaddingStyle(
  padding: StorefrontPadding | undefined,
  theme: StorefrontTheme,
  useThemeSpacing = false,
) {
  const defaultPadding = useThemeSpacing ? theme.layout.sectionSpacing : 0;
  return {
    paddingTop: `${padding?.top ?? defaultPadding}px`,
    paddingRight: `${padding?.right ?? defaultPadding}px`,
    paddingBottom: `${padding?.bottom ?? defaultPadding}px`,
    paddingLeft: `${padding?.left ?? defaultPadding}px`,
  };
}

export function getBorderRadiusStyle(
  borderRadius: StorefrontBorderRadius | undefined,
  theme: StorefrontTheme,
): CSSProperties {
  const fallback = `${theme.layout.borderRadius}px`;
  const radius = (value: number | null | undefined) =>
    value == null ? fallback : `${value}px`;

  return {
    borderTopLeftRadius: radius(borderRadius?.topLeft),
    borderTopRightRadius: radius(borderRadius?.topRight),
    borderBottomRightRadius: radius(borderRadius?.bottomRight),
    borderBottomLeftRadius: radius(borderRadius?.bottomLeft),
  };
}

