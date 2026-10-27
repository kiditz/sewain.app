import {
  allPaddingZero,
  defaultTextLayout,
  type BlockType,
  type SectionLayout,
  type StorefrontBlock,
} from "~/types/storefront";
import { defaultPadding } from "~/types/theme";

export interface BlockDefinition {
  type: BlockType;
  label: string;
  description: string;
  defaults: StorefrontBlock["settings"];
  allowedChildren?: BlockType[];
  defaultChildren?: BlockType[];
  maxChildren?: number;
}

export const blockRegistry: Record<BlockType, BlockDefinition> = {
  HEADER: {
    type: "HEADER",
    label: "Header",
    description: "A heading, supporting text and view-all button layout.",
    defaults: {
      layout: {
        direction: "horizontal",
        alignment: "space-between",
        position: "center",
        gap: 16,
        width: "page",
      } as SectionLayout,
      padding: allPaddingZero,
    },
    allowedChildren: ["HEADING", "TEXT", "BUTTON"],
    defaultChildren: ["HEADING", "TEXT", "BUTTON"],
  },
  EYEBROW: {
    type: "EYEBROW",
    label: "Eyebrow",
    description: "Small text above a heading.",
    defaults: {
      text: "NEW COLLECTION",
      padding: allPaddingZero,
      layout: defaultTextLayout,
    },
  },
  HEADING: {
    type: "HEADING",
    label: "Heading",
    description: "A heading or title.",
    defaults: {
      text: "Make room for more",
      padding: allPaddingZero,
      layout: defaultTextLayout,
    },
  },
  TEXT: {
    type: "TEXT",
    label: "Text",
    description: "Supporting text or description.",
    defaults: {
      text: "Tell customers what makes your store different.",
      layout: defaultTextLayout,
      padding: allPaddingZero,
    },
  },
  BUTTON: {
    type: "BUTTON",
    label: "Button",
    description: "A call-to-action link.",
    defaults: {
      label: "Explore products",
      url: "/collections/all",
      style: "primary",
    },
  },
  PRODUCT_CARD: {
    type: "PRODUCT_CARD",
    label: "Product card",
    description: "Template used to display each product in a product list.",
    defaults: {
      style: {
        variant: "plain",
        color: "",

      },
      borderRadius: {
        topLeft: null,
        topRight: null,
        bottomRight: null,
        bottomLeft: null,
      },
    },
    allowedChildren: ["IMAGE", "PRODUCT_TITLE", "PRODUCT_PRICE"],
    defaultChildren: ["IMAGE", "PRODUCT_TITLE", "PRODUCT_PRICE"],
  },
  COLLECTION_LIST: {
    type: "COLLECTION_LIST",
    label: "Collection list",
    description: "Display product collections.",
    defaults: { source: "all", count: 3, collectionIds: "" },
  },
  COLLECTION_PRODUCTS: {
    type: "COLLECTION_PRODUCTS",
    label: "Collection products",
    description:
      "Display collections together with the products available in them.",
    defaults: {
      collectionIds: "",
      collectionCount: 12,
      productsPerCollection: 8,
      hideEmpty: true,
      showDescription: true,
      showViewAll: true,
    },
  },
  IMAGE: {
    type: "IMAGE",
    label: "Image",
    description: "An image with editable alt text.",
    defaults: {
      src: "",
      alt: "Store image",
      aspectRatio: "auto",
      borderRadius: {
        topLeft: null,
        topRight: null,
        bottomRight: null,
        bottomLeft: null,
      },
      padding: defaultPadding,
    },
  },
  PRODUCT_TITLE: {
    type: "PRODUCT_TITLE",
    label: "Product title",
    description: "Displays the current product name.",
    defaults: { padding: allPaddingZero, layout: defaultTextLayout },
  },
  PRODUCT_PRICE: {
    type: "PRODUCT_PRICE",
    label: "Rental price",
    description: "Displays the rental price for the selected plan.",
    defaults: {
      showPeriod: true,
      padding: allPaddingZero,
      layout: defaultTextLayout,
    },
  },
  VARIANT_SELECTOR: {
    type: "VARIANT_SELECTOR",
    label: "Variant selector",
    description: "Lets customers choose a product variant.",
    defaults: { label: "Choose an option" },
  },
  RENTAL_PLAN_SELECTOR: {
    type: "RENTAL_PLAN_SELECTOR",
    label: "Rental duration",
    description: "Lets customers choose a rental duration.",
    defaults: { label: "Rental duration" },
  },
  DEPOSIT_INFO: {
    type: "DEPOSIT_INFO",
    label: "Deposit information",
    description: "Displays the required refundable deposit.",
    defaults: { label: "Refundable deposit" },
  },
  QUANTITY_SELECTOR: {
    type: "QUANTITY_SELECTOR",
    label: "Quantity selector",
    description: "Controls the rental quantity.",
    defaults: { label: "Quantity" },
  },
  ADD_TO_CART: {
    type: "ADD_TO_CART",
    label: "Add to cart",
    description: "Adds the selected rental to the cart.",
    defaults: { label: "Sewa Sekarang", style: "primary" },
  },
  PRICE_SUMMARY: {
    type: "PRICE_SUMMARY",
    label: "Price summary",
    description: "Summary of pricing",
    defaults: { label: "Ringkasan pembayaran", style: "primary" },
  },
  CART_ITEMS: {
    type: "CART_ITEMS",
    label: "Cart items",
    description: "The customer's rental items with editing and removal.",
    defaults: {
      title: "Rental items",
      showClearCart: true,
      emptyTitle: "Your cart is empty",
      emptyText:
        "You haven't added any rental items yet. Explore our collection to get started.",
      emptyButtonLabel: "Explore products",
      emptyButtonUrl: "/collections",
      continueLabel: "Continue shopping",
      continueUrl: "/",
    },
  },
  CART_SUMMARY: {
    type: "CART_SUMMARY",
    label: "Cart summary",
    description: "Rental subtotal, deposit and total of the cart.",
    defaults: {
      title: "Order summary",
      rentalLabel: "Rental subtotal",
      depositLabel: "Refundable deposit",
      showDeposit: true,
      totalLabel: "Total",
      totalHint: "Rental + deposit",
      noteText:
        "Deposits are subject to the store's rental and return policies.",
    },
  },
  CART_CHECKOUT: {
    type: "CART_CHECKOUT",
    label: "Checkout button",
    description: "Takes the customer to checkout.",
    defaults: {
      label: "Proceed to checkout",
      url: "/checkout",
      style: "primary",
    },
  },
};

export function createDefaultBlock(type: BlockType): StorefrontBlock {
  const definition = blockRegistry[type];
  const blocks = definition.defaultChildren?.map((childType) => {
    const child = createDefaultBlock(childType);
    if (type === "HEADER" && childType === "BUTTON") {
      child.settings.label = "View all";
      child.settings.url = "/collections/all";
    }
    return child;
  });

  return {
    id: crypto.randomUUID(),
    type,
    enabled: true,
    settings: structuredClone(definition.defaults),
    ...(blocks?.length ? { blocks } : {}),
  };
}
