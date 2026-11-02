import type {
  StorefrontMenu,
  StorefrontPage,
  StorefrontSection,
  SectionLayout,
  TextLayout,
} from "~/types/storefront";

export function navigationDefaults() {
  return {
    sticky: false,
    padding: { top: 12, right: 24, bottom: 12, left: 24 },
    layout: {
      direction: "horizontal",
      alignment: "space-between",
      position: "center",
      gap: 24,
      width: "page",
      height: "auto",
      customHeight: 0,
      
    } as SectionLayout,
  };
}

export function navigationBlocks(
  menuId: string | null = null,
): StorefrontSection["blocks"] {
  const block = (
    type: "LOGO" | "HEADING" | "MENU",
    settings: Record<string, unknown>,
  ) => ({
    id: crypto.randomUUID(),
    type,
    enabled: true,
    settings,
  });
  const padding = () => ({ top: 0, right: 0, bottom: 0, left: 0 });
  return [
    block("LOGO", { url: "/", width: 140, height: 48, padding: padding() }),
    block("MENU", {
      menuId,
      padding: padding(),
      layout: {
        direction: "horizontal",
        alignment: "center",
        width: "full",
      } as SectionLayout,
    }),
  ];
}

export function createNavigationSection(): StorefrontSection {
  return {
    id: crypto.randomUUID(),
    type: "NAVIGATION",
    enabled: true,
    settings: navigationDefaults(),
    blocks: navigationBlocks(),
  };
}

/** Upgrade legacy headers once, retaining visibility, IDs, layout and menu choice. */
export function ensureNavigationSection(page: StorefrontPage): boolean {
  let changed = false;
  for (const section of page.sections) {
    if (section.type === "NAVIGATION") {
      for (const block of section.blocks) {
        if (block.type !== "IMAGE") continue;
        block.type = "LOGO";
        const { aspectRatio, displayType, ...settings } = block.settings;
        block.settings = {
          ...settings,
          width: settings.width ?? 140,
          height: settings.height ?? 48,
        };
        changed = true;
      }
      continue;
    }
    if (String(section.type) !== "HEADER") continue;
    section.type = "NAVIGATION";
    if (!section.blocks.length)
      section.blocks = navigationBlocks(
        (section.settings.menuId as string | null) ?? null,
      );
    delete section.settings.menuId;
    changed = true;
  }
  if (page.sections.some((section) => section.type === "NAVIGATION"))
    return changed;
  page.sections.unshift(createNavigationSection());
  return true;
}

export function resolveNavigationMenu(
  menus: StorefrontMenu[],
  selectedId: unknown,
  defaultId: string | null,
) {
  if (selectedId === "__none__") return undefined;
  const id =
    typeof selectedId === "string" && selectedId ? selectedId : defaultId;
  return menus.find((menu) => menu.id === id);
}
