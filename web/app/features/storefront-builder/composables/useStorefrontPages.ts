import { synchronizeThemeGlobals } from "../utils/theme-globals";
import { createFooterSection, ensureFooterSection } from "../utils/footer-section";
import { checkoutBlockDefaults, normalizeCheckoutSection } from "../utils/checkout-layout";
import { cartBlockDefaults, normalizeCartSection } from "../utils/cart-layout";
import { createNavigationSection, ensureNavigationSection } from "../utils/navigation-section";
import { useStorefrontState as useState } from "./useStorefrontState";
import { computed } from "vue";
import { useStorefrontResourceFactory } from "./useStorefrontResource";
import type { BlockType, StorefrontBlock, StorefrontPage, StorefrontSection, SectionType, PageKind } from "~/types/storefront";
import { blockRegistry, createDefaultBlock } from "../utils/block-registry";
import { sectionRegistry } from "../utils/section-registry";

function cloneBlockWithNewIds(block: StorefrontBlock): StorefrontBlock {
  return {
    ...structuredClone(block),
    id: crypto.randomUUID(),
    blocks: block.blocks?.map(cloneBlockWithNewIds),
  };
}

export function createSection(type: SectionType): StorefrontSection {
  if (type === "NAVIGATION") return createNavigationSection();
  if (type === "FOOTER") return createFooterSection();
  const definition = sectionRegistry[type];
  const blocks = definition.defaultBlocks.map((blockType, index) => {
    const block = createDefaultBlock(blockType);
    if (["SLIDESHOW", "SLIDESHOW_INSET", "LAYERED_SLIDESHOW"].includes(type)) block.settings.name = `Slide ${index + 1}`;
    if (["CART", "CHECKOUT"].includes(type) && blockType === "HEADER") {
      block.blocks = (["EYEBROW", "HEADING", "TEXT"] as BlockType[]).map(createDefaultBlock);
      block.settings = type === "CHECKOUT" ? checkoutBlockDefaults("HEADER") : cartBlockDefaults("HEADER");
    }
    if (blockType === "LOGO" && type === "LARGE_LOGO") { block.settings.width = 1200; block.settings.height = 280; }
    if (blockType === "HEADER" && definition.content) {
      block.settings.layout = { direction: "vertical", alignment: "left", position: "center", gap: 12, width: "full", height: "auto", customHeight: 0, alignTextBaseline: false };
      block.blocks = (["EYEBROW", "HEADING", "TEXT"] as BlockType[]).map(createDefaultBlock);
      for (const child of block.blocks) {
        child.settings.text = (child.type === "HEADING" ? definition.content.heading : child.type === "TEXT" ? definition.content.text : definition.content.eyebrow) ?? "";
        if (type === "PULLQUOTE") child.settings.layout = { ...child.settings.layout as object, alignment: "center" };
      }
    }
    if (blockType === "HOTSPOT") { block.settings.x = index % 2 ? 70 : 30; block.settings.y = 50; }
    return block;
  });
  const copyText: Partial<
    Record<SectionType, { heading?: string; text?: string; eyebrow?: string }>
  > = {
    HERO: {
      eyebrow: "NEW COLLECTION",
      heading: "Make room for more",
      text: "Rent the things you need, only for as long as you need them.",
    },
    RICH_TEXT: {
      heading: "A little about us",
      text: "Tell your customers what makes your store different. Share your story, values, or helpful information.",
    },
    IMAGE_BANNER: {
      heading: "Ready for your next adventure?",
      text: "Find the right gear for your plans.",
    },
    FEATURED_PRODUCTS: {
      heading: "Popular rentals",
      text: "Customer favourites, ready when you are.",
    },
    PRODUCT_GRID: {
      heading: "Products",
      text: "Browse our complete range of rental products.",
    },
    COLLECTION_LIST: {
      heading: "Explore by collection",
      text: "Find what you need for your next plan.",
    },
    COLLECTION_PRODUCTS: {
      heading: "Shop by collection",
      text: "Browse every collection and the products available right now.",
    },

    PRODUCT_INFORMATION: {},
    PRODUCT_DESCRIPTION: {
      heading: "Product Description",
      text: "Everything you need to know about this product.",
    },
    CART: {
      eyebrow: "YOUR ORDER",
      heading: "Shopping Cart",
      text: "Review your rental items before proceeding to checkout.",
    },
    CHECKOUT: {
      eyebrow: "ALMOST THERE",
      heading: "Checkout",
      text: "Enter your details to complete your rental order.",
    },
  };
  const defaults = copyText[type] ?? definition.content;
  const contentBlocks = ["CART", "CHECKOUT"].includes(type) ? blocks.flatMap(block => [block, ...(block.blocks ?? [])]) : blocks;
  for (const block of contentBlocks) {
    if (block.type === "HEADING" && defaults?.heading)
      block.settings.text = defaults.heading;
    if (block.type === "TEXT" && defaults?.text)
      block.settings.text = defaults.text;
    if (block.type === "EYEBROW" && defaults?.eyebrow)
      block.settings.text = defaults.eyebrow;
    if (block.type === "BUTTON" && type === "IMAGE_BANNER")
      block.settings.label = "Browse collection";
    // Judul halaman cart adalah h1.
    if (block.type === "HEADING" && type === "CART")
      block.settings.level = "h1";
  }
  return {
    id: crypto.randomUUID(),
    type,
    enabled: true,
    settings: structuredClone(definition.defaults),
    blocks,
  };
}

function samplePages(): StorefrontPage[] {
  const homeSections = [
    createSection("HERO"),
    createSection("FEATURED_PRODUCTS"),
    createSection("COLLECTION_LIST"),
    createSection("IMAGE_BANNER"),
  ];
  const aboutSections = [
    createSection("HERO"),
    createSection("RICH_TEXT"),
    createSection("IMAGE_BANNER"),
  ];
  const productsSections = [
    createSection("PRODUCT_INFORMATION"),
    createSection("PRODUCT_DESCRIPTION"),
    createSection("FEATURED_PRODUCTS"),
  ];
  const pages: StorefrontPage[] = [
    {
      id: "home",
      title: "Home",
      slug: "home",
      targetUrl: "/",
      kind: "HOME",
      icon: "HomeOutlined",
      status: "PUBLISHED",
      sections: homeSections,
      updatedAt: new Date().toISOString(),
    },
    {
      id: "product",
      title: "Product",
      slug: "product",
      targetUrl: "/products/:slug",
      kind: "PRODUCT",
      icon: "ShoppingOutlined",
      status: "PUBLISHED",
      sections: productsSections,
      updatedAt: new Date().toISOString(),
    },
    {
      id: "collections",
      title: "Collections",
      slug: "collections",
      targetUrl: "/collections",
      kind: "COLLECTIONS",
      icon: "AppstoreOutlined",
      status: "PUBLISHED",
      sections: [createSection("COLLECTION_LIST")],
      updatedAt: new Date().toISOString(),
    },
    {
      id: "all-products",
      title: "All products",
      slug: "all",
      targetUrl: "/collections/all",
      kind: "PRODUCT_GRID",
      icon: "AppstoreOutlined",
      status: "PUBLISHED",
      sections: [createSection("PRODUCT_GRID")],
      updatedAt: new Date().toISOString(),
    },
    {
      id: "cart",
      title: "Shopping Cart",
      slug: "cart",
      targetUrl: "/cart",
      kind: "CART",
      icon: "ShoppingCartOutlined",
      status: "PUBLISHED",
      sections: [createSection("CART")],
      updatedAt: new Date().toISOString(),
    },
    {
      id: "checkout",
      title: "Checkout",
      slug: "checkout",
      targetUrl: "/checkout",
      kind: "CHECKOUT",
      icon: "CreditCardOutlined",
      status: "PUBLISHED",
      sections: [createSection("CHECKOUT")],
      updatedAt: new Date().toISOString(),
    },
    {
      id: "contact",
      title: "Contact",
      slug: "contact",
      targetUrl: "/pages/contact",
      kind: "CUSTOM",
      icon: "PhoneOutlined",
      status: "DRAFT",
      sections: [createSection("RICH_TEXT")],
      updatedAt: new Date().toISOString(),
    },
    {
      id: "about",
      title: "About",
      slug: "about",
      targetUrl: "/pages/about",
      kind: "CUSTOM",
      icon: "FileTextOutlined",
      status: "DRAFT",
      sections: aboutSections,
      updatedAt: new Date().toISOString(),
    },
  ];
  pages.forEach(page => { ensureNavigationSection(page); ensureFooterSection(page); });
  return pages;
}

export function useStorefrontPages() {
  const pages = useState<StorefrontPage[]>("storefront-pages", () => []);
  const initialized = useState("storefront-pages-initialized", () => false);
  const loading = useState("storefront-pages-loading", () => false);
  const api = useApi();
  const themeId = useRequestURL().searchParams.get("themeId");
  const resolvedTheme = useState<string | null>("storefront-resolved-theme", () => themeId);
  let targetThemeId = resolvedTheme.value;
  const endpoint = (id: string) => {
    if (!targetThemeId) throw new Error("Load the storefront before saving pages");
    return `/storefront/pages/${encodeURIComponent(id)}?themeId=${encodeURIComponent(targetThemeId)}`;
  };
  const createResource = useStorefrontResourceFactory("pages");
  const resource = (id: string) => createResource(id, () => endpoint(id));
  const statuses = computed(() => pages.value.map(page => resource(page.id).status.value));
  const publishedPages = computed(() => pages.value.filter(page => page.status === "PUBLISHED"));
  const schedulePage = (page: StorefrontPage) => { synchronizeThemeGlobals(pages.value, page); resource(page.id).schedule(page); };
  const savePage = (page: StorefrontPage) => { synchronizeThemeGlobals(pages.value, page); return resource(page.id).save(page); };
  async function load() {
    if (initialized.value) return;
    loading.value = true;
    try {
      const response = await api<{ configuration: StorefrontPage[]; themeId: string }>("/storefront/pages", { query: themeId ? { themeId } : undefined });
      targetThemeId = response.themeId;
      resolvedTheme.value = targetThemeId;
      pages.value = response.configuration ?? [];
      if (!pages.value.length) {
        pages.value = samplePages();
        synchronizeThemeGlobals(pages.value, pages.value.find(page => page.kind === "HOME"));
        await Promise.all(pages.value.map(savePage));
      } else {
        const previous = new Map(pages.value.map(page => [page.id, JSON.stringify(page)]));
        applyConfiguration({ pages: pages.value });
        await Promise.all(pages.value.filter(page => previous.get(page.id) !== JSON.stringify(page)).map(savePage));
      }
      initialized.value = true;
    } finally { loading.value = false; }
  }
  function getPage(id: string) {
    return pages.value.find((page) => page.id === id);
  }

  function getPageByKind(kind: PageKind) {
    return pages.value.find((page) => page.kind === kind);
  }

  function getPageBySlug(slug: string) {
    return pages.value.find(
      (page) => page.slug === slug && page.status === "PUBLISHED",
    );
  }

  function getPageBySlugForEditing(slug: string) {
    return pages.value.find((page) => page.slug === slug);
  }
  function targetUrlFor(page: Pick<StorefrontPage, "kind" | "slug">) {
    switch (page.kind) {
      case "HOME":
        return "/";
      case "PRODUCT":
        return "/products/:slug";
      case "PRODUCT_GRID":
        return "/collections/all";
      case "COLLECTIONS":
        return "/collections";
      case "CART":
        return "/cart";
      case "CHECKOUT":
        return "/checkout";
      default:
        return `/pages/${page.slug}`;
    }
  }
  function migrateFeaturedProductsSettings(section: StorefrontSection): boolean {
    if (section.type !== "FEATURED_PRODUCTS") return false;

    const productListIndex = section.blocks.findIndex(
      (block) => String(block.type) === "PRODUCT_LIST",
    );
    if (productListIndex < 0) return false;
    const productListBlock = section.blocks[productListIndex]!;

    const settingsKeys = [
      "source",
      "displayType",
      "count",
      "columns",
      "mobileColumns",
      "productIds",
      "showPrice",
    ];
    for (const key of settingsKeys) {
      if (Object.hasOwn(productListBlock.settings, key)) {
        if (key !== "showPrice" && !Object.hasOwn(section.settings, key)) {
          const value = productListBlock.settings[key];
          section.settings[key] =
            key === "source" && ["", "all", "featured"].includes(String(value))
              ? null
              : value;
        }
        delete productListBlock.settings[key];
      }
    }
    const cardTemplate = productListBlock.blocks?.find(
      (block) => block.type === "PRODUCT_CARD",
    );
    section.blocks.splice(
      productListIndex,
      1,
      cardTemplate ?? createDefaultBlock("PRODUCT_CARD"),
    );
    return true;
  }

  function migrateCollectionListSettings(section: StorefrontSection): boolean {
    if (section.type !== "COLLECTION_LIST") return false;

    const collectionListBlockIndex = section.blocks.findIndex(
      (block) => block.type === "COLLECTION_LIST",
    );
    const collectionListBlock =
      collectionListBlockIndex >= 0
        ? section.blocks[collectionListBlockIndex]
        : undefined;
    const defaults = sectionRegistry.COLLECTION_LIST.defaults;
    const settings = { ...defaults, ...section.settings };
    let changed = false;

    if (collectionListBlock) {
      for (const key of ["collectionIds", "count"] as const) {
        if (
          !Object.hasOwn(section.settings, key) &&
          Object.hasOwn(collectionListBlock.settings, key)
        ) {
          settings[key] = collectionListBlock.settings[key];
        }
      }
      section.blocks.splice(collectionListBlockIndex, 1);
      changed = true;
    }

    if (Object.keys(defaults).some((key) => !Object.hasOwn(section.settings, key))) {
      changed = true;
    }
    section.settings = settings;
    return changed;
  }

  function applyConfiguration(configuration: { pages: StorefrontPage[] }) {
    if (!configuration) return false;
    let addedProductGridPage = false;
    let addedDefaultChildBlocks = false;
    let migratedFeaturedSettings = false;
    let migratedCollectionListSettings = false;
    if (Array.isArray(configuration.pages)) {
      pages.value = configuration.pages.map((page) => {
        const normalizedPage = {
          ...page,
          slug: page.slug || (page.kind === "HOME" ? "home" : page.slug),
        };
        return {
          ...normalizedPage,
          targetUrl: page.targetUrl || targetUrlFor(normalizedPage),
        };
      });
      for (const page of pages.value) {
        ensureNavigationSection(page);
        for (const section of page.sections) {
          normalizeCartSection(section);
          normalizeCheckoutSection(section);
          if (migrateFeaturedProductsSettings(section)) {
            migratedFeaturedSettings = true;
          }
          if (migrateCollectionListSettings(section)) {
            migratedCollectionListSettings = true;
          }
          for (const block of section.blocks) {
            if (ensureDefaultChildBlocks(block)) addedDefaultChildBlocks = true;
          }
        }
      }
      if (!pages.value.some((page) => page.kind === "PRODUCT_GRID")) {
        const allProductsPage = samplePages().find(
          (page) => page.kind === "PRODUCT_GRID",
        );
        if (allProductsPage) {
          pages.value.push(allProductsPage);
          addedProductGridPage = true;
        }
      }
    }
    return (
      addedProductGridPage ||
      addedDefaultChildBlocks ||
      migratedFeaturedSettings ||
      migratedCollectionListSettings
    );
  }

  function ensureDefaultChildBlocks(block: StorefrontBlock): boolean {
    let changed = false;
    const defaults = blockRegistry[block.type].defaultChildren;
    if (!Array.isArray(block.blocks) && defaults?.length) {
      block.blocks = defaults.map(createDefaultBlock);
      changed = true;
    }
    for (const child of block.blocks ?? []) {
      if (ensureDefaultChildBlocks(child)) changed = true;
    }
    return changed;
  }
  function createPage(title: string, slug: string) {
    const normalizedSlug = slugify(slug || title);
    if (pages.value.some((page) => page.slug === normalizedSlug))
      throw new Error("Slug sudah digunakan.");
    const page: StorefrontPage = {
      id: crypto.randomUUID(),
      title: title.trim(),
      slug: normalizedSlug,
      targetUrl: `/pages/${normalizedSlug}`,
      kind: "CUSTOM",
      icon: "FileTextOutlined",
      status: "DRAFT",
      sections: [createNavigationSection(), createFooterSection()],
      updatedAt: new Date().toISOString(),
    };
    const home = pages.value.find(page => page.kind === "HOME");
    if (home) synchronizeThemeGlobals([page], home);
    pages.value.push(page);
    schedulePage(page);
    return page;
  }
  function duplicatePage(id: string) {
    const source = getPage(id);
    if (!source) return;
    const title = `${source.title} copy`;
    let slug = slugify(title);
    let suffix = 2;
    while (pages.value.some((page) => page.slug === slug))
      slug = `${slugify(title)}-${suffix++}`;
    const page: StorefrontPage = {
      ...structuredClone(source),
      id: crypto.randomUUID(),
      title,
      slug,
      targetUrl: `/pages/${slug}`,
      kind: "CUSTOM",
      status: "DRAFT",
      sections: source.sections.map((section) => ({
        ...structuredClone(section),
        id: crypto.randomUUID(),
        blocks: section.blocks.map(cloneBlockWithNewIds),
      })),
      updatedAt: new Date().toISOString(),
    };
    const home = pages.value.find(page => page.kind === "HOME");
    if (home) synchronizeThemeGlobals([page], home);
    pages.value.push(page);
    schedulePage(page);
    return page;
  }
  async function deletePage(id: string) {
    const page = getPage(id);
    if (!page || page.kind !== "CUSTOM") return;
    await resource(id).remove();
    pages.value = pages.value.filter(page => page.id !== id);
  }
  function touch(page?: StorefrontPage) {
    if (!page) return;
    page.updatedAt = new Date().toISOString();
    schedulePage(page);
  }
  function addSection(page: StorefrontPage, type: SectionType, afterId?: string | null) {
    if (["NAVIGATION", "FOOTER"].includes(type) && page.sections.some(section => section.type === type)) return;
    const section = createSection(type);
    const afterIndex = afterId ? page.sections.findIndex(section => section.id === afterId) : -1;
    const footerIndex = page.sections.findIndex(section => section.type === "FOOTER");
    if (afterIndex >= 0) page.sections.splice(afterIndex + 1, 0, section);
    else if (footerIndex >= 0 && type !== "FOOTER") page.sections.splice(footerIndex, 0, section);
    else page.sections.push(section);
    touch(page);
    return section;
  }
  function removeSection(page: StorefrontPage, id: string) {
    const header = page.sections.find(section => section.id === id && section.type === "NAVIGATION");
    if (header) { header.enabled = false; touch(page); return; }
    page.sections = page.sections.filter((section) => section.id !== id);
    touch(page);
  }
  function addBlock(
    page: StorefrontPage,
    section: StorefrontSection,
    type: BlockType,
    parentBlockId?: string,
  ) {
    const parent = parentBlockId
      ? findNestedBlock(section.blocks, parentBlockId)
      : null;
    if (parentBlockId && !parent) return;
    const allowedTypes = parent
      ? (blockRegistry[parent.type].allowedChildren ?? [])
      : sectionRegistry[section.type].allowedBlocks;
    if (!allowedTypes.includes(type)) return;
    const maxChildren = parent
      ? blockRegistry[parent.type].maxChildren
      : sectionRegistry[section.type].maxBlocks;
    if (maxChildren && (parent ? parent.blocks?.length ?? 0 : section.blocks.length) >= maxChildren)
      return;
    const block = createDefaultBlock(type);
    if (["CART", "CHECKOUT"].includes(section.type) && type === "HEADER") {
      block.blocks = (["EYEBROW", "HEADING", "TEXT"] as BlockType[]).map(createDefaultBlock);
      block.settings = section.type === "CHECKOUT" ? checkoutBlockDefaults("HEADER") : cartBlockDefaults("HEADER");
    }
    if (parent) {
      parent.blocks ??= [];
      parent.blocks.push(block);
    } else {
      section.blocks.push(block);
    }
    touch(page);
    return block;
  }
  function removeBlock(
    page: StorefrontPage,
    section: StorefrontSection,
    id: string,
  ) {
    removeNestedBlock(section.blocks, id);
    touch(page);
  }

  function findNestedBlock(
    blocks: StorefrontBlock[],
    id: string,
  ): StorefrontBlock | null {
    for (const block of blocks) {
      if (block.id === id) return block;
      const nested = findNestedBlock(block.blocks ?? [], id);
      if (nested) return nested;
    }
    return null;
  }

  function removeNestedBlock(blocks: StorefrontBlock[], id: string): boolean {
    const index = blocks.findIndex((block) => block.id === id);
    if (index >= 0) {
      blocks.splice(index, 1);
      return true;
    }
    for (const block of blocks) {
      if (removeNestedBlock(block.blocks ?? [], id)) return true;
    }
    return false;
  }
  function slugify(value: string) {
    return value
      .toLowerCase()
      .trim()
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-|-$/g, "");
  }
  return {
    pages, publishedPages, initialized, loading, load, pageStatus: resource, schedulePage, savePage,
    saving: computed(() => statuses.value.some(status => status.saving)),
    saveError: computed(() => statuses.value.some(status => status.error)),
    hasUnsavedChanges: computed(() => statuses.value.some(status => status.dirty)),
    getPage, getPageByKind, getPageBySlug, getPageBySlugForEditing, targetUrlFor,
    createPage, duplicatePage, deletePage, touch, addSection, removeSection, addBlock, removeBlock, slugify,
  };
}
