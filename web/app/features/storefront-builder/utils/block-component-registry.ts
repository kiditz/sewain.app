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
