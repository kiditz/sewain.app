<script setup lang="ts">
import type { Product } from "~/types/product";

const props = defineProps<{ product: Product; preview?: boolean }>();
const variants = computed(() => props.product.variants ?? []);
const image = computed(
  () =>
    props.product.media ||
    variants.value.find((variant) => variant.active)?.mediaList?.[0] ||
    "",
);
const price = computed(() => {
  const plans = variants.value
    .filter((variant) => variant.active)
    .flatMap((variant) => variant.rentalPlans ?? [])
    .filter((plan) => plan.active && Number.isFinite(plan.rentalFee))
    .sort((left, right) => left.rentalFee - right.rentalFee);
  const plan = plans[0];
  if (!plan) return "";
  const unit = { HOUR: "hour", DAY: "day", WEEK: "week", MONTH: "month" }[
    plan.durationUnit
  ];
  const duration =
    plan.durationValue === 1 ? unit : `${plan.durationValue} ${unit}s`;
  const amount = new Intl.NumberFormat("id-ID", {
    style: "currency",
    currency: "IDR",
    maximumFractionDigits: 0,
  }).format(plan.rentalFee);
  return `${amount} / ${duration}`;
});
</script>

<template>
  <NuxtLink
    :to="preview ? undefined : `/products/${product.slug}`"
    class="product-card"
    @click="preview && $event.preventDefault()"
  >
    <div class="product-card__media">
      <img v-if="image" :src="image" :alt="product.name" loading="lazy" />
      <v-icon v-else size="42" color="medium-emphasis"
        >mdi-image-outline</v-icon
      >
    </div>
    <strong>{{ product.name }}</strong>
    <span v-if="price">{{ price }}</span>
  </NuxtLink>
</template>

<style scoped>
.product-card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 8px;
  color: var(--store-text);
  text-decoration: none;
}
.product-card__media {
  display: grid;
  place-items: center;
  height: clamp(140px, 20vw, 250px);
  overflow: hidden;
  border-radius: var(--store-radius);
  background: var(--store-secondary);
}
.product-card__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.product-card strong {
  font-size: 14px;
}
.product-card span {
  font-size: 13px;
  opacity: 0.72;
}
</style>
