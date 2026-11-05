import {commonBlocks} from "./block-catalog";
import { slideDefaults } from "./slideshow";
import { socialLinkDefaults } from "./footer-section";
import { productControlDefaults } from "./product-information-layout";
import { checkoutBlockDefaults } from "./checkout-layout";
import { cartBlockDefaults } from "./cart-layout";
import {
  allPaddingZero,
  defaultTextLayout,
  menuLayout,
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
  ICON: {type:"ICON",label:"Icon",description:"An icon with editable size and color.",defaults:{icon:"StarOutlined",size:48,color:"",padding:allPaddingZero,layout:defaultTextLayout}},
  PAGE: {type:"PAGE",label:"Page",description:"Embed a custom page's content.",defaults:{pageId:"",showTitle:true,padding:allPaddingZero}},
  COLLECTION_CARD: {type:"COLLECTION_CARD",label:"Collection Card",description:"An editable collection card.",defaults:{collectionId:"",padding:allPaddingZero,layout:{...menuLayout,direction:"vertical",width:"full"}},allowedChildren:["IMAGE","COLLECTION_TITLE","TEXT","BUTTON","ICON"],defaultChildren:["IMAGE","COLLECTION_TITLE"]},
  COLLECTION_TITLE: {type:"COLLECTION_TITLE",label:"Collection Title",description:"The current collection's name.",defaults:{padding:allPaddingZero,layout:defaultTextLayout}},
  JUMBO_TEXT: {type:"JUMBO_TEXT",label:"Jumbo Text",description:"Oversized decorative text.",defaults:{text:"MAKE ROOM FOR MORE",fontSize:120,style:{color:"",backgroundColor:"#00000000"},padding:allPaddingZero,layout:defaultTextLayout}},
  COMPARISON_SLIDER: {type:"COMPARISON_SLIDER",label:"Comparison Slider",description:"Compare two images with a slider.",defaults:{layout:{width:"full"},padding:allPaddingZero,aspectRatio:1.6,beforeLabel:"",afterLabel:""},allowedChildren:["IMAGE"],defaultChildren:["IMAGE","IMAGE"],maxChildren:2},
  COPYRIGHT: {type:"COPYRIGHT",label:"Copyright",description:"Store name and the current year.",defaults:{text:"© {year} {store}",padding:allPaddingZero,layout:defaultTextLayout}},
  PAYMENT_ICONS: {type:"PAYMENT_ICONS",label:"Payment Icons",description:"The payment methods accepted by your store.",defaults:{methods:[],size:28,padding:allPaddingZero}},
  POLICY_LINK: {type:"POLICY_LINK",label:"Policy Link",description:"A selected policy page or destination.",defaults:{label:"Policy",url:"",openInNewTab:false,padding:allPaddingZero}},
  CONTACT_FORM: {type:"CONTACT_FORM",label:"Contact Form",description:"Contact details and a message, saved under Customer.",defaults:{formKind:"CONTACT",layout:{width:"full"},padding:allPaddingZero,buttonLabel:"",successMessage:"",requireConsent:false},allowedChildren:["HEADER","TEXT","IMAGE"],defaultChildren:[]},
  EMAIL_SIGNUP: {type:"EMAIL_SIGNUP",label:"Email Signup",description:"Customer email signup with consent.",defaults:{formKind:"EMAIL_SIGNUP",layout:{width:"full"},padding:allPaddingZero,buttonLabel:"",successMessage:"",requireConsent:true},allowedChildren:["HEADER","TEXT"],defaultChildren:[]},
  ACCORDION: {type:"ACCORDION",label:"Accordion",description:"Collapsible editable content.",defaults:{title:"More information",open:false,layout:{...menuLayout,direction:"vertical",width:"full"},padding:allPaddingZero},allowedChildren:commonBlocks.filter(type=>!['CONTACT_FORM','EMAIL_SIGNUP'].includes(type)),defaultChildren:["TEXT"]},
  GROUP: {type:"GROUP",label:"Group",description:"Arrange nested blocks with layout and padding.",defaults:{layout:{...menuLayout,direction:"vertical",width:"full"},padding:allPaddingZero},allowedChildren:commonBlocks,defaultChildren:[]},
  SPACER: {type:"SPACER",label:"Spacer",description:"Adjustable empty space.",defaults:{height:32}},
  POPUP_LINK: {type:"POPUP_LINK",label:"Popup Link",description:"A link that opens editable popup content.",defaults:{label:"Learn more",title:"More information",layout:{...menuLayout,direction:"vertical",width:"full"},padding:allPaddingZero},allowedChildren:commonBlocks.filter(type=>type!=='POPUP_LINK'&&type!=='PAGE'),defaultChildren:["TEXT"]},
  BUY_BUTTONS: {type:"BUY_BUTTONS",label:"Buy Buttons",description:"Buy or choose options for the current product.",defaults:{label:"",style:"primary",...productControlDefaults("ADD_TO_CART")}},
  PRODUCT_DESCRIPTION: {type:"PRODUCT_DESCRIPTION",label:"Description",description:"The current product description.",defaults:{padding:allPaddingZero,layout:defaultTextLayout}},
  RECOMMENDED_PRODUCTS: {type:"RECOMMENDED_PRODUCTS",label:"Recommended Products",description:"Product suggestions with an editable card template.",defaults:{recommendation:true,count:3,columns:3,mobileColumns:1,itemsGap:18,displayType:"grid",productIds:"",layout:{width:"full"},padding:allPaddingZero},allowedChildren:["HEADER","PRODUCT_CARD"],defaultChildren:["PRODUCT_CARD"]},
  SKU: {type:"SKU",label:"SKU",description:"The selected product variant SKU.",defaults:{prefix:"SKU",padding:allPaddingZero,layout:defaultTextLayout}},

  FAQ_ITEM: { type: "FAQ_ITEM", label: "Question", description: "An expandable question and answer.", defaults: { question: "How does renting work?", answer: "Choose a product and rental period, then complete checkout.", padding: allPaddingZero } },
  ICON_TEXT: { type: "ICON_TEXT", label: "Icon with text", description: "A benefit with an icon and description.", defaults: { icon: "SafetyCertificateOutlined", title: "Made for your plans", text: "Reliable products and friendly support.", padding: { top: 16, right: 16, bottom: 16, left: 16 } } },
  COLUMN: { type: "COLUMN", label: "Column", description: "A column with images, text and links.", defaults: { layout: { direction: "vertical", alignment: "left", position: "top", gap: 16, width: "full", height: "auto", customHeight: 0, alignTextBaseline: false }, padding: allPaddingZero }, allowedChildren: ["IMAGE", "HEADER", "TEXT", "BUTTON", "ICON_TEXT"], defaultChildren: ["IMAGE", "HEADER", "BUTTON"] },
  DIVIDER: { type: "DIVIDER", label: "Divider", description: "A separator with adjustable thickness and color.", defaults: { color: "", thickness: 1, padding: { top: 16, right: 0, bottom: 16, left: 0 } } },
  MARQUEE_TEXT: { type: "MARQUEE_TEXT", label: "Marquee text", description: "Scrolling text with reduced-motion support.", defaults: { text: "RENT MORE · OWN LESS · ", speed: 25, reverse: false, padding: { top: 12, right: 0, bottom: 12, left: 0 } } },
  HOTSPOT: { type: "HOTSPOT", label: "Product hotspot", description: "A product marker on an image.", defaults: { productId: "", x: 50, y: 50 } },
  SLIDE: {
    type: "SLIDE", label: "Slide", description: "Background image, content and call to action.",
    defaults: slideDefaults(), allowedChildren: ["HEADER", "BUTTON"], defaultChildren: ["HEADER", "BUTTON"],
  },
  SOCIAL_LINK: { type: "SOCIAL_LINK", label: "Social Media Link", description: "Social account icon and external URL.", defaults: socialLinkDefaults() },
  CHECKOUT_CONTACT: { type: "CHECKOUT_CONTACT", label: "Contact", description: "Checkout contact.", defaults: checkoutBlockDefaults("CHECKOUT_CONTACT") },
  CHECKOUT_FULFILLMENT: { type: "CHECKOUT_FULFILLMENT", label: "Fulfillment", description: "Checkout fulfillment.", defaults: checkoutBlockDefaults("CHECKOUT_FULFILLMENT") },
  CHECKOUT_NOTES: { type: "CHECKOUT_NOTES", label: "Order notes", description: "Checkout order notes.", defaults: checkoutBlockDefaults("CHECKOUT_NOTES") },
  CHECKOUT_PAYMENT: { type: "CHECKOUT_PAYMENT", label: "Payment", description: "Checkout payment.", defaults: checkoutBlockDefaults("CHECKOUT_PAYMENT") },
  CHECKOUT_SUMMARY: { type: "CHECKOUT_SUMMARY", label: "Order summary", description: "Checkout order summary.", defaults: checkoutBlockDefaults("CHECKOUT_SUMMARY") },
  CHECKOUT_SUBMIT: { type: "CHECKOUT_SUBMIT", label: "Place order", description: "Checkout place order.", defaults: checkoutBlockDefaults("CHECKOUT_SUBMIT") },
  LOGO: {
    type: "LOGO", label: "Logo", description: "Store logo displayed in full without cropping.",
    defaults: { src: "", alt: "", url: "/", width: 140, height: 48, padding: allPaddingZero },
  },
  MENU: {
    type: "MENU", label: "Menu", description: "Navigation menu links and mobile drawer.",
    defaults: { menuId: null, padding: allPaddingZero, layout: menuLayout },
  },
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
    allowedChildren: ["EYEBROW", "HEADING", "TEXT", "BUTTON"],
    defaultChildren: ["HEADING", "TEXT", "BUTTON"],
  },
  EYEBROW: {
    type: "EYEBROW",
    label: "Eyebrow",
    description: "Small text above a heading.",
    defaults: {
      borderRadius: { topLeft: 0, topRight: 0, bottomRight: 0, bottomLeft: 0 },
      style: { color: "", backgroundColor: "#00000000" },
      text: "EYEBROW",
      padding: allPaddingZero,
      layout: defaultTextLayout,
    },
  },
  HEADING: {
    type: "HEADING",
    label: "Heading",
    description: "A heading or title.",
    defaults: {
      borderRadius: { topLeft: 0, topRight: 0, bottomRight: 0, bottomLeft: 0 },
      style: { color: "", backgroundColor: "#00000000" },
      text: "HEADING",
      padding: allPaddingZero,
      layout: defaultTextLayout,
    },
  },
  TEXT: {
    type: "TEXT",
    label: "Text",
    description: "Supporting text or description.",
    defaults: {
      borderRadius: { topLeft: 0, topRight: 0, bottomRight: 0, bottomLeft: 0 },
      style: { color: "", backgroundColor: "#00000000" },
      text: "Text",
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
    label: "Price",
    description: "Displays the rental price for the selected plan.",
    defaults: {
      showPeriod: true,
      padding: allPaddingZero,
      layout: defaultTextLayout,
    },
  },
  VARIANT_SELECTOR: {
    type: "VARIANT_SELECTOR",
    label: "Variant Picker",
    description: "Lets customers choose a product variant.",
    defaults: { label: "Choose an option", ...productControlDefaults("VARIANT_SELECTOR") },
  },
  RENTAL_PLAN_SELECTOR: {
    type: "RENTAL_PLAN_SELECTOR",
    label: "Rental duration",
    description: "Lets customers choose a rental duration.",
    defaults: { label: "Rental duration", ...productControlDefaults("RENTAL_PLAN_SELECTOR") },
  },
  DEPOSIT_INFO: {
    type: "DEPOSIT_INFO",
    label: "Deposit information",
    description: "Displays the required refundable deposit.",
    defaults: { label: "Refundable deposit", ...productControlDefaults("DEPOSIT_INFO") },
  },
  QUANTITY_SELECTOR: {
    type: "QUANTITY_SELECTOR",
    label: "Quantity selector",
    description: "Controls the rental quantity.",
    defaults: { label: "Quantity", ...productControlDefaults("QUANTITY_SELECTOR") },
  },
  ADD_TO_CART: {
    type: "ADD_TO_CART",
    label: "Add to cart",
    description: "Adds the selected rental to the cart.",
    defaults: { label: "Sewa Sekarang", style: "primary", ...productControlDefaults("ADD_TO_CART") },
  },
  PRICE_SUMMARY: {
    type: "PRICE_SUMMARY",
    label: "Price summary",
    description: "Summary of pricing",
    defaults: { label: "Ringkasan pembayaran", style: "primary", ...productControlDefaults("PRICE_SUMMARY") },
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
      ...cartBlockDefaults("CART_ITEMS"),
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
      ...cartBlockDefaults("CART_SUMMARY"),
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
      ...cartBlockDefaults("CART_CHECKOUT"),
    },
  },
};

export function createDefaultBlock(type: BlockType): StorefrontBlock {
  const definition = blockRegistry[type];
  const blocks = definition.defaultChildren?.map((childType) => {
    const child = createDefaultBlock(childType);
    if (type === "COLLECTION_CARD" && childType === "IMAGE") { child.settings.aspectRatio = "landscape"; child.settings.alt = ""; }
    if (type === "HEADER" && childType === "BUTTON") {
      child.settings.label = "View all";
      child.settings.url = "/collections/all";
    }
    if (["SLIDE", "COLUMN"].includes(type) && childType === "HEADER") {
      child.settings.layout = { direction: "vertical", alignment: "left", position: "center", gap: 12, width: "full", height: "auto", customHeight: 0, alignTextBaseline: false };
      child.blocks = (["EYEBROW", "HEADING", "TEXT"] as BlockType[]).map(createDefaultBlock);
      const copy = ["NEW COLLECTION", "Make room for more", "Discover products for your next adventure."];
      child.blocks.forEach((block, index) => { block.settings.text = copy[index]; });
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
