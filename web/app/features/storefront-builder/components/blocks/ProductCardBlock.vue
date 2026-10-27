<script setup lang="ts">
import type { Product } from "~/types/product";
import type {
  StorefrontBlock,
  StorefrontBorderRadius,
  StorefrontProductCardStyle,
} from "~/types/storefront";
import { getBorderRadiusStyle } from "../../utils/block-settings";
import { useStorefront } from "../../composables/useStorefront";

const props = defineProps<{
  block: StorefrontBlock;
  productContext?: Product;
  preview?: boolean;
  selectedBlockId?: string | null;
  productDescription?: boolean;
}>();

const storefront = useStorefront();
const router = useRouter();

function openProduct(event: MouseEvent | KeyboardEvent) {
  if (props.preview || !props.productContext?.slug) return;
  if (
    (event.type === "click" &&
      (event.target as HTMLElement).closest("a, button")) ||
    (event.type === "keydown" && event.target !== event.currentTarget)
  ) {
    return;
  }
  void router.push(`/products/${props.productContext.slug}`);
}

const cardStyle = computed(() => {
  const value = props.block.settings.style;
  const style =
    value !== null && typeof value === "object" && !Array.isArray(value)
      ? (value as Partial<StorefrontProductCardStyle>)
      : {};

  return {
    variant: style.variant ?? "elevated",
    backgroundColor: {
      "store-primary": "var(--store-primary)",
      "store-secondary": "var(--store-secondary)",
      none: "transparent",
      white: "#ffffff",
    }[style.color ?? "store-primary"],
  };
});

const borderRadiusStyle = computed(() =>
  getBorderRadiusStyle(
    props.block.settings.borderRadius as StorefrontBorderRadius,
    storefront.theme.value,
  ),
);
</script>

<template>
  <v-card
    v-if="productContext"
    class="product-card-template"
    :class="{ 'product-card-template--clickable': !preview }"
    :variant="cardStyle.variant"
    :style="{
      backgroundColor: cardStyle.backgroundColor,
      color: 'var(--store-text)',
      ...borderRadiusStyle,
    }"
    :role="preview ? undefined : 'link'"
    :tabindex="preview ? undefined : 0"
    @click="openProduct"
    @keydown.enter="openProduct"
  >
    <slot />
  </v-card>
</template>

<style scoped>
.product-card-template {
  display: flex;
  width: 100%;
  min-width: 0;
  flex-direction: column;
  gap: 8px;
}

.product-card-template--clickable {
  cursor: pointer;
}

.product-card-template :deep(.v-card__overlay) {
  border-radius: inherit;
}

.product-card-template :deep(.block-image:not(.block-image--fixed-ratio)) {
  height: clamp(160px, 30vw, 280px);
  max-height: 280px;
}

.product-card-template :deep(.block-image img) {
  height: 100%;
  object-fit: cover;
}

</style>
