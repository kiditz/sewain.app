<script setup lang="ts">
const { formatDate } = useSettings();
import moment from "moment";
import { getBlockSettings, getLayoutStyle, getPaddingStyle } from "../../utils/block-settings";
import { useCartView } from "../../composables/useCartView";
import RentalDateRangeBlock from "./RentalDateRangeBlock.vue";
import QuantitySelectorBlock from "./QuantitySelectorBlock.vue";
import type { CartItemResponse } from "~/types/cart";
import type { SectionLayout, StorefrontPadding, StorefrontBlock } from "~/types/storefront";

import { useStorefront } from "../../composables/useStorefront";
import { cartBlockDefaults } from "../../utils/cart-layout";
const props = defineProps<{
  block: StorefrontBlock;
  preview?: boolean;
}>();

const { theme } = useStorefront();
const blockStyle = computed(() => {
  const defaults = cartBlockDefaults(props.block.type);
  return {
    ...getLayoutStyle({ ...defaults.layout, ...props.block.settings.layout as Partial<SectionLayout> }, theme.value),
    ...getPaddingStyle({ ...defaults.padding, ...props.block.settings.padding as StorefrontPadding }, theme.value),
  };
});

const itemsLayoutStyle = computed(() => ({
  ...getLayoutStyle({ ...cartBlockDefaults(props.block.type).layout, ...props.block.settings.layout as Partial<SectionLayout>, width: "full", height: "auto" }, theme.value),
}));

const settings = computed(() =>
  getBlockSettings<{
    title?: string;
    showClearCart?: boolean;
    emptyTitle?: string;
    emptyText?: string;
    emptyButtonLabel?: string;
    emptyButtonUrl?: string;
    continueLabel?: string;
    continueUrl?: string;
  }>(props.block),
);

const productStore = useStorefrontProductStore();
const { t } = useI18n();
const { cartStore, items, loading, error, usingSample } = useCartView(
  () => props.preview,
);
const { removeItem } = cartStore;

// Edit item

const editDialog = ref(false);
const editingItem = ref<CartItemResponse | null>(null);
const editSaving = ref(false);
const editError = ref<string | null>(null);

function openEdit(item: CartItemResponse) {
  if (usingSample.value) return;

  editingItem.value = item;
  editError.value = null;
  editDialog.value = true;

  // Tanggal dan quantity awal akan diinisialisasi setelah
  // data produk dan variant dimuat ke productStore.
}

function closeEdit() {
  if (editSaving.value) return;

  editDialog.value = false;
  editingItem.value = null;
  editError.value = null;
}

async function saveEdit() {
  const item = editingItem.value;

  if (!item || editSaving.value) return;

  const start = moment(productStore.startDate, "YYYY-MM-DD", "id")
    .utcOffset("+07:00")
    .startOf("day")
    .format("YYYY-MM-DDTHH:mm:ssZ");
  const end = moment(productStore.endDate, "YYYY-MM-DD", "id")
    .utcOffset("+07:00")
    .startOf("day")
    .format("YYYY-MM-DDTHH:mm:ssZ");
  const quantity = Number(productStore.quantity);
  const plans = productStore.rentalPlanCombination;

  if (!start || !end || start >= end) {
    editError.value = "Pilih periode sewa yang valid.";
    return;
  }

  if (
    !Number.isInteger(quantity) ||
    quantity < 1 ||
    quantity > productStore.availableQuantity
  ) {
    editError.value = "Quantity melebihi stok yang tersedia.";
    return;
  }

  if (!productStore.canAddToCart || !plans.length) {
    editError.value = "Periode sewa atau rental plan tidak valid.";
    return;
  }

  editSaving.value = true;
  editError.value = null;

  try {
    await cartStore.updateItem(item.id, {
      productId: item.productId,
      variantId: item.variantId,
      quantity,
      rentalStartAt: start,
      rentalEndAt: end,
      rentalPlans: plans.map(({ plan, quantity }) => ({
        plan: {
          id: plan.id,
          durationUnit: plan.durationUnit,
          durationValue: plan.durationValue,
          rentalFee: plan.rentalFee,
        },
        quantity,
      })),
      rentalSubtotal: productStore.rentalSubtotal,
      depositSubtotal: productStore.depositSubtotal,
      totalAmount: productStore.total,
    });

    await cartStore.getCart();

    editDialog.value = false;
    editingItem.value = null;
  } catch (error) {
    editError.value =
      error instanceof Error ? error.message : "Gagal memperbarui item cart.";
  } finally {
    editSaving.value = false;
  }
}

// Clear cart

const clearCartDialog = ref(false);
const clearCartLoading = ref(false);
const removingItemId = ref<string | null>(null);

async function removeCartItem(item: CartItemResponse) {
  if (usingSample.value || removingItemId.value) return;
  removingItemId.value = item.id;
  try {
    await removeItem(item.id);
  } catch (error) {
    console.error("Failed to remove cart item:", error);
  } finally {
    removingItemId.value = null;
  }
}

async function clearCart() {
  if (clearCartLoading.value) return;

  clearCartLoading.value = true;

  try {
    await cartStore.clearItems();
    clearCartDialog.value = false;
  } catch (error) {
    console.error("Failed to clear cart:", error);
  } finally {
    clearCartLoading.value = false;
  }
}
</script>

<template>
  <div class="cart-items-block" :style="blockStyle">
    <v-dialog v-model="clearCartDialog" max-width="400">
      <v-card>
        <v-card-title>{{ $t("ui.clear_shopping_cart") }}</v-card-title>

        <v-card-text>
          {{ $t("common.all") }} {{ items.length }} {{ $t("ui.item_di_cart_akan_dihapus_tindakan_ini_tidak_dapat_dibatalkan") }}
        </v-card-text>

        <v-card-actions>
          <v-spacer />

          <v-btn
            variant="text"
            :disabled="clearCartLoading"
            @click="clearCartDialog = false"
          >
            {{ $t("common.cancel") }}
          </v-btn>

          <v-btn
            color="error"
            :loading="clearCartLoading"
            :disabled="clearCartLoading"
            @click="clearCart"
          >
            {{ $t("ui.clear_cart") }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="editDialog" max-width="640" :persistent="editSaving">
      <v-card>
        <v-card-title v-if="editingItem" class="cart-edit-dialog__title">
          <div class="cart-edit-dialog__product">
            <strong>{{ editingItem.productName }}</strong>
            <span>{{ editingItem.variantName }}</span>
          </div>
        </v-card-title>

        <v-card-text>
          <template v-if="editingItem">
            <RentalDateRangeBlock :label="$t('ui.ubah_periode_sewa')" />

            <QuantitySelectorBlock
              :block="{
                enabled: true,
                type: 'QUANTITY_SELECTOR',
                id: 'q',
                settings: {},
              }"
            />

            <div class="cart-edit-dialog__summary">
              <div>
                <span>{{ $t("orders.create.rentalSubtotal") }}</span>
                <strong>{{ money(productStore.rentalSubtotal) }}</strong>
              </div>

              <div>
                <span>{{ $t("orders.create.deposit") }}</span>
                <strong>{{ money(productStore.depositSubtotal) }}</strong>
              </div>

              <div class="cart-edit-dialog__total">
                <span>{{ $t("checkout.total") }}</span>
                <strong>{{ money(productStore.total) }}</strong>
              </div>
            </div>
          </template>

          <v-alert v-if="editError" type="error" variant="tonal" class="mt-4">
            {{ editError }}
          </v-alert>
        </v-card-text>

        <v-card-actions>
          <v-spacer />

          <v-btn variant="text" :disabled="editSaving" @click="closeEdit">
            {{ $t("common.cancel") }}
          </v-btn>

          <v-btn
            color="primary"
            :disabled="editSaving"
            :loading="editSaving"
            @click="saveEdit"
          >
            {{ $t("common.saveChange") }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Loading -->
    <div v-if="loading" class="cart-state">
      <v-progress-linear indeterminate class="cart-progress" />
      <p>{{ $t("ui.loading_your_cart") }}</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="cart-state">
      <v-icon icon="mdi-alert-circle-outline" size="40" />
      <h2>{{ $t("checkout.cartLoadError") }}</h2>
      <p>{{ error }}</p>
      <button
        type="button"
        class="cart-button cart-button--outline"
        @click="cartStore.getCart()"
      >
        {{ $t("checkout.retry") }}
      </button>
    </div>

    <!-- Empty -->
    <div v-else-if="!items.length" class="cart-empty">
      <div class="cart-empty__icon">
        <v-icon icon="mdi-cart-outline" size="36" />
      </div>

      <h2>{{ settings.emptyTitle || "Your cart is empty" }}</h2>
      <p>{{ settings.emptyText }}</p>

      <NuxtLink
        v-if="settings.emptyButtonLabel"
        :to="settings.emptyButtonUrl || '/collections'"
        class="cart-button"
      >
        {{ settings.emptyButtonLabel }}
        <v-icon icon="mdi-arrow-right" size="18" />
      </NuxtLink>
    </div>

    <!-- Items -->
    <section v-else class="cart-items" :class="{ 'cart-items--horizontal': block.settings.layout && (block.settings.layout as SectionLayout).direction === 'horizontal' }" :style="itemsLayoutStyle">
      <div class="cart-section-heading">
        <div class="cart-section-heading__title">
          <h2>{{ settings.title || "Rental items" }}</h2>
          <span>
            {{ t("checkout.cartItemsCount", { count: items.length }) }}
          </span>
        </div>

        <div class="cart-section-heading__actions">
          <v-btn
            v-if="settings.showClearCart !== false"
            variant="text"
            color="error"
            size="small"
            prepend-icon="mdi-delete-outline"
            :disabled="clearCartLoading || usingSample"
            @click="clearCartDialog = true"
          >
            {{ $t("ui.clear_cart") }}
          </v-btn>

        </div>
      </div>

      <article
        v-for="item in items"
        :key="item.id"
        class="cart-item"
      >
        <div class="cart-item__selection">
          <NuxtLink
            :to="`/products/${item.productSlug}`"
            class="cart-item__image"
          >
            <v-img
              :src="item.mediaList?.at(0) || undefined"
              :alt="item.productName"
              aspect-ratio="0.82"
              cover
            >
              <template #error>
                <div class="cart-image-placeholder">
                  <v-icon icon="mdi-image-outline" size="32" />
                </div>
              </template>
              <template v-if="!item.mediaList?.length" #default>
                <div class="cart-image-placeholder">
                  <v-icon icon="mdi-image-outline" size="32" />
                </div>
              </template>
            </v-img>
          </NuxtLink>
        </div>

        <div class="cart-item__body">
          <div class="cart-item__top">
            <div class="cart-item__info">
              <NuxtLink
                :to="`/products/${item.productSlug}`"
                class="cart-item__name"
              >
                {{ item.productName }}
              </NuxtLink>

              <p class="cart-item__variant">
                {{ item.variantName || "Standard variant" }}
              </p>
            </div>

            <div class="cart-item__quantity">{{ $t("ui.qty") }} {{ item.quantity }}</div>
            <v-btn
              icon="mdi-delete-outline"
              size="small"
              variant="text"
              color="error"
              :aria-label="t('checkout.removeCartItem', { name: item.productName })"
              :loading="removingItemId === item.id"
              :disabled="usingSample || removingItemId !== null"
              @click="removeCartItem(item)"
            />
          </div>

          <div class="cart-item__period">
            <div class="cart-item__period-info">
              <v-icon icon="mdi-calendar-range" size="18" />

              <span>
                {{ formatDate(item.rentalStartAt) }}
                <span class="cart-item__date-separator">—</span>
                {{ formatDate(item.rentalEndAt) }}
              </span>
            </div>

            <button
              type="button"
              class="cart-edit-period"
              :disabled="usingSample"
              @click="openEdit(item)"
            >
              <v-icon icon="mdi-pencil-outline" size="16" />
              {{ $t("common.edit") }}
            </button>
          </div>

          <div class="cart-item__bottom">
            <div class="cart-item__breakdown">
              <div class="cart-price-row">
                <span>{{ $t("product.fields.rental") }}</span>
                <span>{{ money(item.rentalSubtotal) }}</span>
              </div>

              <div class="cart-price-row">
                <span>{{ $t("orders.create.deposit") }}</span>
                <span>{{ money(item.depositSubtotal) }}</span>
              </div>
            </div>

            <div class="cart-item__total">
              <span>{{ $t("ui.item_total") }}</span>
              <strong>{{ money(item.totalAmount) }}</strong>
            </div>
          </div>
        </div>
      </article>

      <div v-if="settings.continueLabel" class="cart-items__footer">
        <NuxtLink :to="settings.continueUrl || '/'" class="cart-continue">
          <v-icon icon="mdi-arrow-left" size="18" />
          {{ settings.continueLabel }}
        </NuxtLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.cart-items-block {
  container: cart-items / inline-size;
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  color: var(--store-text);
}

.cart-items,
.cart-empty,
.cart-state {
  width: 100%;
  box-sizing: border-box;
  min-width: 0;
  border: 1px solid color-mix(in srgb, var(--store-text) 14%, transparent);
  border-radius: var(--store-radius);
  background: var(--store-surface);
}

.cart-items { flex-wrap: wrap; }
.cart-items--horizontal > .cart-item { width: auto; flex: 1 1 320px; }

.cart-section-heading {
  box-sizing: border-box;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
  border-bottom: 1px solid
    color-mix(in srgb, var(--store-text) 12%, transparent);
}

.cart-section-heading h2 {
  margin: 0;
  font-family: var(--store-heading-font), Georgia, serif;
  font-size: 19px;
  font-weight: 700;
}

.cart-section-heading__title {
  display: grid;
  gap: 6px;
}

.cart-section-heading__title > span {
  font-size: 12px;
  opacity: 0.65;
}

.cart-section-heading__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  flex-shrink: 0;
  gap: 12px;
}

.cart-item {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  display: grid;
  grid-template-columns: 136px minmax(0, 1fr);
  gap: 22px;
  padding: 24px;
  border-bottom: 1px solid
    color-mix(in srgb, var(--store-text) 12%, transparent);
}

.cart-item:last-of-type {
  border-bottom: 0;
}

.cart-item__selection {
  display: block;
}

.cart-item__image {
  display: block;
  overflow: hidden;
  align-self: start;
  border-radius: calc(var(--store-radius) * 0.7);
  background: var(--store-background);
}

.cart-image-placeholder {
  display: flex;
  aspect-ratio: 0.82;
  align-items: center;
  justify-content: center;
  color: var(--store-text);
  opacity: 0.45;
}

.cart-item__body {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: space-between;
  gap: 20px;
}

.cart-item__top {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 12px;
}

.cart-item__info {
  min-width: 0;
}

.cart-item__name {
  color: var(--store-text);
  font-family: var(--store-heading-font), Georgia, serif;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.45;
  text-decoration: none;
  overflow-wrap: anywhere;
}

.cart-item__name:hover,
.cart-continue:hover {
  color: var(--store-primary);
}

.cart-item__variant {
  margin: 6px 0 0;
  font-size: 13px;
  opacity: 0.65;
}

.cart-item__quantity {
  flex-shrink: 0;
  font-size: 12px;
  opacity: 0.7;
}

.cart-item__period {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  line-height: 1.6;
}

.cart-item__period-info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.cart-item__period :deep(.v-icon) {
  flex-shrink: 0;
  color: var(--store-primary);
}

.cart-item__date-separator {
  padding-inline: 3px;
  opacity: 0.6;
}

.cart-edit-period {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 5px;
  padding: 5px 0;
  border: 0;
  background: transparent;
  color: var(--store-primary);
  font: inherit;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.cart-edit-period:hover {
  opacity: 0.7;
}

.cart-edit-period:disabled {
  cursor: default;
  opacity: 0.5;
}

.cart-item__bottom {
  display: grid;
  gap: 12px;
}

.cart-item__breakdown {
  display: grid;
  gap: 8px;
}

.cart-price-row,
.cart-item__total {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  font-size: 13px;
}

.cart-price-row > span:first-child,
.cart-item__total > span {
  opacity: 0.7;
}

.cart-price-row > span:last-child {
  text-align: right;
}

.cart-item__total {
  padding-top: 12px;
  border-top: 1px solid color-mix(in srgb, var(--store-text) 12%, transparent);
}

.cart-item__total strong {
  font-size: 14px;
  text-align: right;
}

.cart-items__footer {
  box-sizing: border-box;
  width: 100%;
  padding: 20px 24px;
  border-top: 1px solid color-mix(in srgb, var(--store-text) 12%, transparent);
}

.cart-continue {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--store-text);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
}

/* Edit dialog */

.cart-edit-dialog__title {
  padding: 24px 24px 12px;
  font-family: var(--store-heading-font), Georgia, serif;
  font-size: 20px;
  font-weight: 700;
}

.cart-edit-dialog__product {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 24px;
}

.cart-edit-dialog__product span {
  font-size: 13px;
  opacity: 0.7;
}

.cart-edit-dialog__summary {
  display: grid;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid color-mix(in srgb, var(--store-text) 15%, transparent);
}

.cart-edit-dialog__summary > div {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.cart-edit-dialog__total {
  padding-top: 12px;
  border-top: 1px solid color-mix(in srgb, var(--store-text) 15%, transparent);
}

/* Empty / loading / error */

.cart-empty,
.cart-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64px 24px;
  text-align: center;
}

.cart-empty__icon {
  display: flex;
  width: 76px;
  height: 76px;
  align-items: center;
  justify-content: center;
  margin-bottom: 22px;
  border-radius: 50%;
  background: var(--store-background);
  color: var(--store-primary);
}

.cart-empty h2,
.cart-state h2 {
  margin: 0;
  font-family: var(--store-heading-font), Georgia, serif;
  font-size: 24px;
}

.cart-empty > p,
.cart-state > p {
  max-width: 420px;
  margin: 12px 0 0;
  font-size: 14px;
  line-height: 1.7;
  opacity: 0.7;
}

.cart-empty > .cart-button,
.cart-state > .cart-button {
  margin-top: 24px;
}

.cart-progress {
  max-width: 240px;
  margin-bottom: 20px;
  color: var(--store-primary);
}

.cart-button {
  display: inline-flex;
  min-height: 48px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 12px 18px;
  border: 1px solid var(--store-primary);
  border-radius: var(--store-radius);
  background: var(--store-primary);
  color: var(--store-on-primary, white);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
  text-decoration: none;
  cursor: pointer;
  transition: opacity 160ms ease;
}

.cart-button:hover {
  opacity: 0.86;
}

.cart-button--outline {
  background: transparent;
  color: var(--store-primary);
}

@container cart-items (max-width: 600px) {
  .cart-items { flex-direction: column !important; align-items: stretch !important; }
  .cart-items--horizontal > .cart-item { flex: 0 1 auto; width: 100%; }

  .cart-section-heading {
    align-items: flex-start;
    flex-wrap: wrap;
    padding: 18px 14px;
  }

  .cart-section-heading__actions {
    width: 100%;
    justify-content: space-between;
  }

  .cart-item {
    grid-template-columns: minmax(0, 1fr);
    gap: 14px;
    padding: 18px 14px;
  }

  .cart-item__selection {
    max-width: 112px;
  }

  .cart-item__period, .cart-item__period-info { flex-wrap: wrap; }

  .cart-item__top {
    flex-direction: column;
    gap: 6px;
  }

  .cart-item__name {
    font-size: 15px;
  }

  .cart-items__footer {
    padding: 18px 14px;
  }

  .cart-empty,
  .cart-state {
    padding: 48px 20px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .cart-button {
    transition: none;
  }
}
</style>
