import type { Product } from "~/types/product";

export interface StorefrontMenuItem {
  id: string;
  pageId?: string;
  label?: string;
  url?: string;
  target?: { type: "PRODUCT" | "COLLECTION"; id: string; title: string; slug: string };
}

export interface StorefrontMenu {
  id: string;
  name: string;
  items: StorefrontMenuItem[];
}

export type PageStatus = "DRAFT" | "PUBLISHED";
export type PageKind =
  | "HOME"
  | "CUSTOM"
  | "PRODUCT"
  | "PRODUCT_GRID"
  | "COLLECTIONS"
  | "CART"
  | "CHECKOUT";
export type SectionType =
  | "FOOTER"
  | "NAVIGATION"
  | "HERO"
  | "RICH_TEXT"
  | "IMAGE_BANNER"
  | "FEATURED_PRODUCTS"
  | "COLLECTION_LIST"
  | "COLLECTION_PRODUCTS"
  | "PRODUCT_GRID"
  | "PRODUCT_INFORMATION"
  | "PRODUCT_DESCRIPTION"
  | "CART"
  | "CHECKOUT";

export type BlockType =
  | "SOCIAL_LINK"
  | "CHECKOUT_CONTACT"
  | "CHECKOUT_FULFILLMENT"
  | "CHECKOUT_NOTES"
  | "CHECKOUT_PAYMENT"
  | "CHECKOUT_SUMMARY"
  | "CHECKOUT_SUBMIT"
  | "LOGO"
  | "MENU"
  | "EYEBROW"
  | "HEADING"
  | "TEXT"
  | "BUTTON"
  | "HEADER"
  | "PRODUCT_CARD"
  | "COLLECTION_LIST"
  | "COLLECTION_PRODUCTS"
  | "IMAGE"
  | "PRODUCT_TITLE"
  | "PRODUCT_PRICE"
  | "VARIANT_SELECTOR"
  | "RENTAL_PLAN_SELECTOR"
  | "DEPOSIT_INFO"
  | "QUANTITY_SELECTOR"
  | "PRICE_SUMMARY"
  | "ADD_TO_CART"
  | "CART_ITEMS"
  | "CART_SUMMARY"
  | "CART_CHECKOUT";

export type SettingValue = string | number | boolean | null;
export type SectionSettings = Record<string, unknown>;
export type BlockSettings = Record<string, unknown>;

export interface StorefrontBlock {
  id: string;
  type: BlockType;
  enabled: boolean;
  settings: BlockSettings;
  blocks?: StorefrontBlock[];
}

export interface StorefrontSection {
  id: string;
  type: SectionType;
  enabled: boolean;
  settings: SectionSettings;
  blocks: StorefrontBlock[];
}

export interface StorefrontPage {
  id: string;
  title: string;
  slug: string;
  targetUrl: string;
  kind: PageKind;
  icon?: string;
  status: PageStatus;
  sections: StorefrontSection[];
  updatedAt: string;
}
export interface StorefrontPadding {
  top?: number | null;
  bottom?: number | null;
  left?: number | null;
  right?: number| null;
}

export interface StorefrontBorderRadius {
  topLeft?: number | null;
  topRight?: number | null;
  bottomLeft?: number | null;
  bottomRight?: number | null;
}

export interface StorefrontAppearance {
  backgroundColor?: string | null;
  overlayColor?: string | null;
  overlayOpacity?: number | null;
}

export interface StorefrontProductCardStyle {
  variant: "elevated" | "flat" | "tonal" | "outlined" | "text" | "plain";
  color: "store-primary" | "store-secondary" | "none" | "white";
}

export type StorefrontThemeMode = "light" | "dark";
export interface StorefrontTheme {
  name: string;
  preset: string;
  mode?: StorefrontThemeMode;
  colorSchemes?: Partial<Record<StorefrontThemeMode, StorefrontTheme["colors"]>>;
  colors: {
    primary: string;
    secondary: string;
    background: string;
    surface: string;
    text: string;
  };
  typography: { bodyFont: string; headingFont: string; baseFontSize: number };
  layout: {
    contentMaxWidth: number;
    sectionSpacing: number;
    borderRadius: number;
  };
}

export interface StorefrontProduct {
  id: string;
  slug: string;
  title: string;
  description: string;
  sku: string;
  image?: string;
  pricePerDay: number;
  deposit: number;
  variants: StorefrontProductVariant[];
}

export interface StorefrontProductVariant {
  id: string;
  name: string;
  sku: string;
  pricePerDay: number;
  deposit: number;
}

export interface SectionLayout {
  direction: "vertical" | "horizontal";
  alignTextBaseline: boolean;
  alignment:
    | "left"
    | "center"
    | "right"
    | "space-between"
    | "space-around"
    | "space-evenly";
  position: "top" | "center" | "bottom";
  gap: number;
  width: "page" | "full";
  height: "auto" | "small" | "medium" | "large" | "custom";
  customHeight: number;
}

export type TextWidth = "match" | "fill";
export type TextMaxWidth = "narrow" | "normal" | "none";
export type TextAlignment = "left" | "center" | "right";
export interface TextLayout {
  width: TextWidth;
  maxWidth: TextMaxWidth;
  alignment: TextAlignment;
}

export const heroLayout: SectionLayout = {
  direction: "vertical",
  alignTextBaseline: false,
  alignment: "center",
  position: "center",
  gap: 16,
  width: "page",
  height: "medium",
  customHeight: 0,
};

export const menuLayout: SectionLayout = {
  direction: "horizontal",
  alignTextBaseline: false,
  alignment: "center",
  gap: 16,
  position: "center",
  width: "page",
  height: "medium",
  customHeight: 0,
};

export const allPaddingZero: StorefrontPadding = {
  top: 0,
  bottom: 0,
  left: 0,
  right: 0,
};

export const defaultTextLayout: TextLayout = {
  maxWidth: "normal",
  width: "match",
  alignment: "left",
};
