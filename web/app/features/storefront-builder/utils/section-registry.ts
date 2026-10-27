import type { Component } from "vue";
import HeaderSection from "../components/sections/HeaderSection.vue";
import { headerDefaults } from "./header-section";
import HeroSection from "~/features/storefront-builder/components/sections/HeroSection.vue";
import RichTextSection from "~/features/storefront-builder/components/sections/RichTextSection.vue";
import ImageBannerSection from "~/features/storefront-builder/components/sections/ImageBannerSection.vue";
import FeaturedProductsSection from "~/features/storefront-builder/components/sections/FeaturedProductsSection.vue";
import CollectionListSection from "~/features/storefront-builder/components/sections/CollectionListSection.vue";
import CollectionProductsSection from "~/features/storefront-builder/components/sections/CollectionProductsSection.vue";
import ProductGridSection from "~/features/storefront-builder/components/sections/ProductGridSection.vue";
import ProductInformationSection from "~/features/storefront-builder/components/sections/ProductInformationSection.vue";
import ProductDescriptionSection from "~/features/storefront-builder/components/sections/ProductDescriptionSection.vue";
import CartSection from "~/features/storefront-builder/components/sections/CartSection.vue";
import CheckoutSection from "~/features/storefront-builder/components/sections/CheckoutSection.vue";
import {
  allPaddingZero,
  heroLayout,
  type BlockType,
  type SectionLayout,
  type SectionType,
  type StorefrontPadding,
  type StorefrontSection,
} from "~/types/storefront";

export interface SectionDefinition {
  type: SectionType;
  label: string;
  description: string;
  component: Component;
  defaults: StorefrontSection["settings"];
  allowedBlocks: BlockType[];
  defaultBlocks: BlockType[];
}

export const sectionRegistry: Record<SectionType, SectionDefinition> = {
  HEADER: {
    type: "HEADER",
    label: "Header",
    description: "Store branding, navigation menu and mobile drawer.",
    component: HeaderSection,
    defaults: headerDefaults(),
    allowedBlocks: [],
    defaultBlocks: [],
  },
  HERO: {
    type: "HERO",
    label: "Hero banner",
    description: "A prominent introduction with a call to action.",
    component: HeroSection,
    defaults: {
      medias: [],
      appearance: {
        backgroundColor: "#00000000",
        overlayColor: "#121212",
        overlayOpacity: 30,
      },
      layout: heroLayout,
      padding: allPaddingZero,
    },
    allowedBlocks: ["EYEBROW", "HEADING", "TEXT", "BUTTON", "IMAGE"],
    defaultBlocks: ["EYEBROW", "HEADING", "TEXT", "BUTTON"],
  },
  RICH_TEXT: {
    type: "RICH_TEXT",
    label: "Rich text",
    description: "Editorial content with text and images.",
    component: RichTextSection,
    defaults: { alignment: "center" },
    allowedBlocks: ["HEADING", "TEXT", "IMAGE", "BUTTON"],
    defaultBlocks: ["HEADING", "TEXT"],
  },
  IMAGE_BANNER: {
    type: "IMAGE_BANNER",
    label: "Image banner",
    description: "A promotional banner with a call to action.",
    component: ImageBannerSection,
    defaults: { background: "#e8e4dc" },
    allowedBlocks: ["HEADING", "TEXT", "BUTTON", "IMAGE"],
    defaultBlocks: ["HEADING", "TEXT", "BUTTON"],
  },
  FEATURED_PRODUCTS: {
    type: "FEATURED_PRODUCTS",
    label: "Featured products",
    description: "Highlight products from your catalogue.",
    component: FeaturedProductsSection,
    defaults: {
      source: null,
      displayType: "grid",
      count: 3,
      columns: 3,
      mobileColumns: 1,
      productIds: "",
      padding: {
        top: 0,
        bottom: 0,
      } as StorefrontPadding,
      layout: {
        gap: 0,
        width: 'page'
      } as SectionLayout,
    },
    allowedBlocks: ["HEADER", "HEADING", "TEXT", "PRODUCT_CARD"],
    defaultBlocks: ["HEADING", "TEXT", "PRODUCT_CARD"],
  },
  COLLECTION_LIST: {
    type: "COLLECTION_LIST",
    label: "Collection list",
    description: "Show groups of products.",
    component: CollectionListSection,
    defaults: {
      collectionIds: "",
      displayType: "grid",
      count: 3,
      columns: 3,
      mobileColumns: 1,
      padding: {
        top: 0,
        bottom: 0,
      } as StorefrontPadding,
      layout: {
        gap: 0,
        width: "page",
      } as SectionLayout,
    },
    allowedBlocks: ["HEADER", "HEADING", "TEXT"],
    defaultBlocks: ["HEADING", "TEXT"],
  },
  COLLECTION_PRODUCTS: {
    type: "COLLECTION_PRODUCTS",
    label: "Collections with products",
    description: "Every collection with its available products.",
    component: CollectionProductsSection,
    defaults: {},
    allowedBlocks: ["HEADER", "EYEBROW", "HEADING", "TEXT", "COLLECTION_PRODUCTS"],
    defaultBlocks: ["HEADING", "TEXT", "COLLECTION_PRODUCTS"],
  },
  PRODUCT_GRID: {
    type: "PRODUCT_GRID",
    label: "Product grid",
    description: "A catalogue-style product listing.",
    component: ProductGridSection,
    defaults: {
      columns: 3,
      productsPerPage: 12,
      showFilters: true,
      showSorting: true,
      showProductCount: true,
    },
    allowedBlocks: ["HEADER", "EYEBROW", "HEADING", "TEXT"],
    defaultBlocks: ["HEADING", "TEXT"],
  },
  PRODUCT_INFORMATION: {
    type: "PRODUCT_INFORMATION",
    label: "Product information",
    description: "Product details and rental purchase controls.",
    component: ProductInformationSection,
    defaults: { layout: "two-column", mediaPosition: "left" },
    allowedBlocks: [
      "PRODUCT_TITLE",
      "PRODUCT_PRICE",
      "VARIANT_SELECTOR",
      "RENTAL_PLAN_SELECTOR",
      "DEPOSIT_INFO",
      "QUANTITY_SELECTOR",
      "PRICE_SUMMARY",
      "ADD_TO_CART",
      "TEXT",
    ],
    defaultBlocks: [
      "PRODUCT_TITLE",
      "PRODUCT_PRICE",
      "VARIANT_SELECTOR",
      "RENTAL_PLAN_SELECTOR",
      "QUANTITY_SELECTOR",
      "PRICE_SUMMARY",
      "ADD_TO_CART",
    ],
  },
  PRODUCT_DESCRIPTION: {
    type: "PRODUCT_DESCRIPTION",
    label: "Product description",
    description: "Detailed product description and supporting media.",
    component: ProductDescriptionSection,
    defaults: { alignment: "left" },
    allowedBlocks: ["HEADING", "TEXT", "IMAGE"],
    defaultBlocks: ["HEADING", "TEXT"],
  },
  CART: {
    type: "CART",
    label: "Shopping cart",
    description:
      "Cart page content: heading, items, order summary and checkout.",
    component: CartSection,
    // summaryPosition: "left" | "right"
    defaults: { summaryPosition: "right" },
    allowedBlocks: [
      "EYEBROW",
      "HEADING",
      "TEXT",
      "CART_ITEMS",
      "CART_SUMMARY",
      "CART_CHECKOUT",
      "BUTTON",
    ],
    defaultBlocks: [
      "EYEBROW",
      "HEADING",
      "TEXT",
      "CART_ITEMS",
      "CART_SUMMARY",
      "CART_CHECKOUT",
    ],
  },
  CHECKOUT: {
    type: "CHECKOUT",
    label: "Checkout",
    description: "Customer details, fulfillment and payment form.",
    component: CheckoutSection,
    defaults: {},
    allowedBlocks: ["EYEBROW", "HEADING", "TEXT"],
    defaultBlocks: ["EYEBROW", "HEADING", "TEXT"],
  },
};

export const sectionTypes = Object.values(sectionRegistry);
