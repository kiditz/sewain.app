import {computed,toValue,getCurrentInstance,inject,type MaybeRefOrGetter} from "vue";
import { storeToRefs } from "pinia";
import {themeDemoCatalogKey} from "../utils/catalog-demo";
import type { CartItemResponse } from "~/types/cart";

/**
 * Contoh item yang ditampilkan di preview builder saat keranjang kosong,
 * supaya merchant tetap bisa melihat dan mengatur tampilan block cart.
 */
function createSampleItems(): CartItemResponse[] {
  const start = new Date();
  const end = new Date(start);
  end.setDate(end.getDate() + 3);
  const now = start.toISOString();

  return [
    {
      id: "sample-item",
      productId: "sample-product",
      variantId: "sample-variant",
      productName: "Sample product",
      productSlug: "sample-product",
      variantName: "Standard variant",
      mediaList: [],
      quantity: 1,
      rentalStartAt: start.toISOString(),
      rentalEndAt: end.toISOString(),
      rentalPlans: [],
      rentalSubtotal: 150000,
      depositSubtotal: 100000,
      totalAmount: 250000,
      createdAt: now,
      updatedAt: now,
    },
  ];
}

/**
 * State cart yang dipakai block-block cart (items, summary, checkout).
 *
 * Di mode preview (builder) dengan keranjang kosong, item contoh dipakai
 * sehingga block tidak tampil kosong dan tidak memanggil API.
 */
export function useCartView(preview?: MaybeRefOrGetter<boolean | undefined>) {
  const demo=getCurrentInstance()?inject(themeDemoCatalogKey,null):null;
  const cartStore = useCartStore();
  const {
    items: cartItems,
    loading: cartLoading,
    error: cartError,
  } = storeToRefs(cartStore);

  const isPreview = computed(() => !!toValue(preview));
  const usingSample = computed(
    () => isPreview.value && (!!demo || cartItems.value.length === 0),
  );

  const sampleItems = createSampleItems();
  const product=demo?.products[0];
  if(product)for(const item of sampleItems){item.productId=product.id;item.productName=product.name;item.productSlug=product.slug;item.variantName=product.variants[0]?.name??'';item.mediaList=[product.media];}

  const items = computed<CartItemResponse[]>(() =>
    usingSample.value ? sampleItems : cartItems.value,
  );

  const loading = computed(() => !isPreview.value && cartLoading.value);
  const error = computed(() => (isPreview.value ? null : cartError.value));
  const isEmpty = computed(() => items.value.length === 0);

  const rentalSubtotal = computed(() =>
    items.value.reduce((total, item) => total + item.rentalSubtotal, 0),
  );

  const depositSubtotal = computed(() =>
    items.value.reduce((total, item) => total + item.depositSubtotal, 0),
  );

  const totalAmount = computed(() =>
    items.value.reduce((total, item) => total + item.totalAmount, 0),
  );

  return {
    cartStore,
    items,
    loading,
    error,
    isEmpty,
    isPreview,
    usingSample,
    rentalSubtotal,
    depositSubtotal,
    totalAmount,
  };
}
