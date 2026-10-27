<script setup lang="ts">
import type { StorefrontBlock } from "~/types/storefront";
import { useStorefront } from "../../composables/useStorefront";

const props = defineProps<{ block: StorefrontBlock }>();
const { collections, loadCollections } = useStorefront();
await loadCollections().catch(() => undefined);
const visibleCollections = computed(() => {
  const limit = Math.max(1, Math.min(100, Number(props.block.settings.count) || 3));
  const selectedIds = String(props.block.settings.collectionIds || "")
    .split(",")
    .filter(Boolean);
  const orderedCollections = selectedIds.length
    ? selectedIds
        .map((id) => collections.value.find((collection) => collection.id === id))
        .filter((collection) => collection !== undefined)
    : collections.value;
  return orderedCollections.slice(0, limit);
});
</script>

<template>
  <div v-if="visibleCollections.length" class="block-collection-grid">
    <NuxtLink
      v-for="(collection, index) in visibleCollections"
      :key="collection.id"
      :to="`/collections/${collection.slug}`"
      class="block-collection-card"
      :class="{ 'has-image': collection.imageUrl }"
      :style="collection.imageUrl ? { backgroundImage: `linear-gradient(0deg, #101816b8, #10181605 75%), url('${collection.imageUrl}')` } : undefined"
    >
      <span class="collection-number">{{ String(index + 1).padStart(2, "0") }}</span>
      <strong>{{ collection.name }}</strong>
      <small v-if="collection.productCount > 0">{{ collection.productCount }} {{ $t("ui.products") }}</small>
      <small v-else>{{ collection.description || "Explore collection" }}</small>
    </NuxtLink>
  </div>
  <p v-else class="empty-catalog">{{ $t("ui.no_collections_to_display") }}</p>
</template>

<style scoped>
.block-collection-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  width: 100%;
}
.block-collection-card {
  min-height: 170px;
  padding: 18px;
  border-radius: var(--store-radius);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: var(--store-text);
  text-decoration: none;
  background-color: var(--store-secondary);
  background-size: cover;
  background-position: center;
}
.block-collection-card.has-image {
  color: white;
}
.block-collection-card strong {
  font-family: var(--store-heading-font, Georgia, serif);
  font-size: 20px;
}
.block-collection-card small,
.collection-number {
  opacity: 0.75;
}
.collection-number {
  font-size: 12px;
}
.empty-catalog {
  color: var(--store-text);
  opacity: 0.65;
}
@media (max-width: 600px) {
  .block-collection-grid {
    grid-template-columns: 1fr;
  }
}
</style>
