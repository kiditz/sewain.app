import GroupBlock from "../components/blocks/GroupBlock.vue";
import IconBlock from "../components/blocks/IconBlock.vue";
import PageBlock from "../components/blocks/PageBlock.vue";
import CollectionCardBlock from "../components/blocks/CollectionCardBlock.vue";
import CollectionTitleBlock from "../components/blocks/CollectionTitleBlock.vue";
import PaymentIconsBlock from "../components/blocks/PaymentIconsBlock.vue";
import AccordionBlock from "../components/blocks/AccordionBlock.vue";
import SpacerBlock from "../components/blocks/SpacerBlock.vue";
import PopupLinkBlock from "../components/blocks/PopupLinkBlock.vue";
import PolicyLinkBlock from "../components/blocks/PolicyLinkBlock.vue";
import BuyButtonsBlock from "../components/blocks/BuyButtonsBlock.vue";
import DecorativeTextBlock from "../components/blocks/DecorativeTextBlock.vue";
import ProductDataBlock from "../components/blocks/ProductDataBlock.vue";
import SectionAdapterBlock from "../components/blocks/SectionAdapterBlock.vue";
import FaqItemBlock from "../components/blocks/FaqItemBlock.vue";
import IconTextBlock from "../components/blocks/IconTextBlock.vue";
import DividerBlock from "../components/blocks/DividerBlock.vue";
import MarqueeBlock from "../components/blocks/MarqueeBlock.vue";
import HotspotBlock from "../components/blocks/HotspotBlock.vue";
import SlideBlock from "../components/blocks/SlideBlock.vue";
import SocialLinkBlock from "../components/blocks/SocialLinkBlock.vue";
import CheckoutSubmitBlock from "../components/blocks/CheckoutSubmitBlock.vue";
import CheckoutSummaryBlock from "../components/blocks/CheckoutSummaryBlock.vue";
import CheckoutPaymentBlock from "../components/blocks/CheckoutPaymentBlock.vue";
import CheckoutNotesBlock from "../components/blocks/CheckoutNotesBlock.vue";
import CheckoutFulfillmentBlock from "../components/blocks/CheckoutFulfillmentBlock.vue";
import CheckoutContactBlock from "../components/blocks/CheckoutContactBlock.vue";
import LogoBlock from "../components/blocks/LogoBlock.vue";
import MenuBlock from "../components/blocks/MenuBlock.vue";
import EyebrowBlock from "../components/blocks/EyebrowBlock.vue";
import HeadingBlock from "../components/blocks/HeadingBlock.vue";
import TextBlock from "../components/blocks/TextBlock.vue";
import HeaderBlock from "../components/blocks/HeaderBlock.vue";
import ButtonBlock from "../components/blocks/ButtonBlock.vue";
import ImageBlock from "../components/blocks/ImageBlock.vue";
import ProductCardBlock from "../components/blocks/ProductCardBlock.vue";
import CollectionListBlock from "../components/blocks/CollectionListBlock.vue";
import CollectionProductsBlock from "../components/blocks/CollectionProductsBlock.vue";
import CartItemsBlock from "../components/blocks/CartItemsBlock.vue";
import CartSummaryBlock from "../components/blocks/CartSummaryBlock.vue";
import CartCheckoutBlock from "../components/blocks/CartCheckoutBlock.vue";
import ProductTitleBlock from "../components/blocks/ProductTitleBlock.vue";
import ProductPriceBlock from "../components/blocks/ProductPriceBlock.vue";
import VariantSelectorBlock from "../components/blocks/VariantSelectorBlock.vue";
import DepositInfoBlock from "../components/blocks/DepositInfoBlock.vue";
import QuantitySelectorBlock from "../components/blocks/QuantitySelectorBlock.vue";
import AddToCartBlock from "../components/blocks/AddToCartBlock.vue";
import RentalDateRangeBlock from "../components/blocks/RentalDateRangeBlock.vue";
import PriceSummaryBlock from "../components/blocks/PriceSummaryBlock.vue";

export const blockComponentRegistry = {
  ICON: IconBlock,
  PAGE: PageBlock,
  COLLECTION_CARD: CollectionCardBlock,
  COLLECTION_TITLE: CollectionTitleBlock,
  PAYMENT_ICONS: PaymentIconsBlock,
  ACCORDION: AccordionBlock,
  SPACER: SpacerBlock,
  POPUP_LINK: PopupLinkBlock,
  POLICY_LINK: PolicyLinkBlock,
  BUY_BUTTONS: BuyButtonsBlock,
  GROUP: GroupBlock, COPYRIGHT: DecorativeTextBlock, JUMBO_TEXT: DecorativeTextBlock,
  PRODUCT_DESCRIPTION: ProductDataBlock, SKU: ProductDataBlock,
  CONTACT_FORM: SectionAdapterBlock, EMAIL_SIGNUP: SectionAdapterBlock,
  COMPARISON_SLIDER: SectionAdapterBlock, RECOMMENDED_PRODUCTS: SectionAdapterBlock,
  FAQ_ITEM: FaqItemBlock, ICON_TEXT: IconTextBlock, DIVIDER: DividerBlock,
  MARQUEE_TEXT: MarqueeBlock, HOTSPOT: HotspotBlock, COLUMN: HeaderBlock,
  SLIDE: SlideBlock,
  SOCIAL_LINK: SocialLinkBlock,
  CHECKOUT_SUBMIT: CheckoutSubmitBlock,
  CHECKOUT_SUMMARY: CheckoutSummaryBlock,
  CHECKOUT_PAYMENT: CheckoutPaymentBlock,
  CHECKOUT_NOTES: CheckoutNotesBlock,
  CHECKOUT_FULFILLMENT: CheckoutFulfillmentBlock,
  CHECKOUT_CONTACT: CheckoutContactBlock,
  LOGO: LogoBlock,
  MENU: MenuBlock,
  EYEBROW: EyebrowBlock,
  HEADING: HeadingBlock,
  TEXT: TextBlock,
  HEADER: HeaderBlock,
  BUTTON: ButtonBlock,
  IMAGE: ImageBlock,
  PRODUCT_CARD: ProductCardBlock,
  COLLECTION_LIST: CollectionListBlock,
  COLLECTION_PRODUCTS: CollectionProductsBlock,
  CART_ITEMS: CartItemsBlock,
  CART_SUMMARY: CartSummaryBlock,
  CART_CHECKOUT: CartCheckoutBlock,
  PRODUCT_TITLE: ProductTitleBlock,
  PRODUCT_PRICE: ProductPriceBlock,
  VARIANT_SELECTOR: VariantSelectorBlock,
  RENTAL_PLAN_SELECTOR: RentalDateRangeBlock,
  DEPOSIT_INFO: DepositInfoBlock,
  QUANTITY_SELECTOR: QuantitySelectorBlock,
  ADD_TO_CART: AddToCartBlock,
  PRICE_SUMMARY: PriceSummaryBlock,
} as const;
