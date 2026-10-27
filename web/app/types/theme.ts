export type ThemePageType = "home" | "product" | "category" | "custom";

export type ThemeSectionType =
  | "announcement"
  | "navigation"
  | "hero"
  | "featured-products"
  | "categories"
  | "footer";

export type ThemeGroupId = "header" | "template" | "footer";

export type ThemeBlockType = "text-heading" | "text-description";

export interface Theme {
  settings: ThemeSettings;
  pages: ThemePage[];
}

export interface ThemePage {
  id: string;
  title: string;
  type: ThemePageType;
  groups: ThemeGroup[];
}
export interface ThemeGroup {
  id: ThemeGroupId;
  title: string;
  sections: ThemeSection[];
}

export interface Layout {
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
export interface ThemeSettings {
  fontFamily: string;
  colors: {
    primary: string;
    secondary: string;
    background: string;
    surface: string;
    error: string;
    success: string;
    warning: string;
    info: string;
  };
}

export const defaultSettings: ThemeSettings = {
  fontFamily: "Inter",
  colors: {
    primary: "#6750A4",
    secondary: "#625B71",
    background: "#FFFBFE",
    surface: "#FFFBFE",
    error: "#B3261E",
    success: "#2E7D32",
    warning: "#ED6C02",
    info: "#0288D1",
  },
};
export const heroDefaultLayout: Layout = {
  direction: "vertical",
  alignTextBaseline: false,
  alignment: "center",
  position: "center",
  gap: 16,
  width: "page",
  height: "medium",
  customHeight: 0,
};
export interface ThemeBlock {
  id: string;
  type: ThemeBlockType;
  settings: Record<string, unknown>;
}

export interface ThemeSection {
  id: string;
  type: ThemeSectionType;
  settings: Record<string, unknown>;
  blocks: ThemeBlock[];
}

export type SectionLayoutSettingType =
  | "toggle"
  | "select"
  | "switch"
  | "number";

export interface SectionLayoutSettingItem {
  title: string;
  value: string;
}

export interface SectionLayoutSetting {
  key: keyof Layout;
  label: string;
  type: SectionLayoutSettingType;
  items?: SectionLayoutSettingItem[];
  suffix?: string;
}

export interface Media {
  src: string;
  alt: string;
}

export interface Padding {
  top: number;
  bottom: number;
  left: number;
  right: number;
}

export const defaultPadding: Padding = {
  top: 0,
  bottom: 0,
  left: 0,
  right: 0,
};

export interface TextLayout {
  width: "fit" | "fill";
  maxWidth: "narrow" | "normal" | "none";
  alignment: "left" | "center" | "right";
}
export const defaultTextLayout: TextLayout = {
  width: "fit",
  maxWidth: "none",
  alignment: "center",
};

export interface TextStyle {
  typography: string;
  transform: "text-none" | "text-uppercase" | "text-capitalize";
  color:
    | "text-medium-emphasis"
    | "text-high-emphasis"
    | "text-primary"
    | "text-secondary"
    | "text-disabled"
    | "";
  customColor: string;
  italic: boolean;
  bold: boolean;
  background: boolean;
  backgroundColor: string;
  borderRadius: number;
}

export const defaultTextStyle: TextStyle = {
  typography: "text-body-large",
  transform: "text-none",
  color: "",
  customColor: "",
  italic: false,
  bold: false,
  background: false,
  backgroundColor: "#00000000",
  borderRadius: 0,
};

export const defaultPages = (): ThemePage[] => [
  {
    id: "home",
    title: "Home",
    type: "home",
    groups: defaultGroups(),
  },
];

const defaultGroups = (): ThemeGroup[] => [
  {
    id: "header",
    title: "Header",
    sections: [
      {
        id: "announcement",
        type: "announcement",
        settings: {},
        blocks: [],
      },
      {
        id: "navigation",
        type: "navigation",
        settings: {},
        blocks: [],
      },
    ],
  },
  {
    id: "template",
    title: "Template",
    sections: [
      {
        id: "hero",
        type: "hero",
        settings: {
          medias: [
            {
              src: "https://placehold.co/600x400",
              alt: "Placeholder",
            },
            {
              src: "https://placehold.co/600x400",
              alt: "Placeholder",
            },
          ] as Media[],
          padding: defaultPadding,
          layout: heroDefaultLayout,
        },
        blocks: [
          {
            id: "hero-heading",
            type: "text-heading",
            settings: {
              text: "Sewa apa yang kamu butuhkan",
              layout: defaultTextLayout,
              padding: defaultPadding,
              style: {
                ...defaultTextStyle,
                typography: "text-headline-medium",
              } as TextStyle,
            },
          },
          {
            id: "hero-description",
            type: "text-description",
            settings: {
              text: "Find the right products for your needs.",
              layout: defaultTextLayout,
              padding: defaultPadding,
              style: {
                ...defaultTextStyle,
                typography: "text-body-large",
              } as TextStyle,
            },
          },
        ],
      },
      {
        id: "featured-products",
        type: "featured-products",
        settings: {},
        blocks: [],
      },
      {
        id: "categories",
        type: "categories",
        settings: {},
        blocks: [],
      },
    ],
  },
  {
    id: "footer",
    title: "Footer",
    sections: [
      {
        id: "footer",
        type: "footer",
        settings: {},
        blocks: [],
      },
    ],
  },
];

export const blockLabels = {
  "text-heading": "Text Heading",
  "text-description": "Text Description",
  image: "Image",
};

export const blockIcons = {
  "text-heading": "mdi-format-title",
  "text-description": "mdi-text",
  image: "mdi-image-outline",
};
