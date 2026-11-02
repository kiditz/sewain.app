<script setup lang="ts">
import type { CollectionSummary } from "~/types/collection";
import type {
  SectionLayout,
  StorefrontBlock,
  StorefrontPadding,
  StorefrontProduct,
} from "~/types/storefront";
import BlockRenderer from "~/components/storefront/BlockRenderer.vue";
import { useStorefront } from "../../composables/useStorefront";
import { getLayoutStyle, getPaddingStyle } from "../../utils/block-settings";

const props = defineProps<{
  settings: Record<string, unknown>;
  blocks: StorefrontBlock[];
  preview?: boolean;
  selectedBlockId?: string | null;
  product?: StorefrontProduct;
}>();

const storefront = useStorefront();
await storefront.loadCollections().catch(() => undefined);
const sectionStyle = computed(() => ({
  ...getLayoutStyle(props.settings.layout as SectionLayout, storefront.theme.value),
  ...getPaddingStyle(props.settings.padding as StorefrontPadding, storefront.theme.value, true),
}));
const contentBlocks = computed(() =>
  props.blocks.filter((block) => block.type !== "COLLECTION_LIST"),
);
const selectedCollections = computed<CollectionSummary[]>(() => {
  const ids = String(props.settings.collectionIds ?? "")
    .split(",")
    .filter(Boolean);
  const collections = storefront.collections.value;
  const orderedCollections = ids.length
    ? ids
        .map((id) => collections.find((collection) => collection.id === id))
        .filter((collection): collection is CollectionSummary => collection !== undefined)
    : collections;
  const count = Math.max(1, Math.min(100, Number(props.settings.count) || 3));
  return orderedCollections.slice(0, count);
});
const displayType = computed(() => {
  const value = String(props.settings.displayType ?? "grid");
  return ["carousel", "grid", "editorial", "bento", "spotlight", "text"].includes(value) ? value : "grid";
});
const gridStyle = computed(() => ({
  "--collection-list-columns": String(
    Math.max(1, Math.min(8, Number(props.settings.columns) || 3)),
  ),
  "--collection-list-mobile-columns": String(
    Number(props.settings.mobileColumns) === 2 ? 2 : 1,
  ),
}));
</script>

<template>
  <section :style="sectionStyle">
    <div class="collections-inner">
      <BlockRenderer
        :blocks="contentBlocks"
        :preview="preview"
        :selected-block-id="selectedBlockId"
        :product="product"
      />
      <div
        v-if="selectedCollections.length"
        class="collection-list-grid"
        :class="`collection-list-grid--${displayType}`"
        :style="gridStyle"
      >
        <NuxtLink
          v-for="(collection, index) in selectedCollections"
          :key="collection.id"
          :to="preview ? undefined : `/collections/${collection.slug}`"
          class="collection-list-card"
          :class="{ 'has-image': collection.imageUrl && displayType !== 'text' }"
          :style="collection.imageUrl && displayType !== 'text' ? { backgroundImage: `linear-gradient(0deg, #101816b8, #10181605 75%), url('${collection.imageUrl}')` } : undefined"
        >
          <span class="collection-number">{{ String(index + 1).padStart(2, "0") }}</span>
          <strong>{{ collection.name }}</strong>
          <small v-if="collection.productCount > 0">
            {{ collection.productCount }} {{ $t("ui.products") }}
          </small>
          <small v-else>{{ collection.description || $t("ui.explore_collection") }}</small>
        </NuxtLink>
      </div>
      <p v-else class="empty-catalog">{{ $t("ui.no_collections_to_display") }}</p>
    </div>
  </section>
</template>

<style scoped>
.collections-inner {
  width: 100%;
  min-width: 0;
}
.collections-inner :deep(.block-heading) {
  font-size: clamp(30px, 4vw, 44px);
}
.collections-inner :deep(.block-text) {
  margin-top: -8px;
}
.collection-list-grid {
  --collection-list-current-columns: var(--collection-list-columns, 3);
  display: grid;
  grid-template-columns: repeat(
    var(--collection-list-current-columns),
    minmax(0, 1fr)
  );
  gap: 18px;
  width: 100%;
  margin-top: 10px;
}
.collection-list-grid--carousel {
  display: flex;
  overflow-x: auto;
  overscroll-behavior-inline: contain;
  scroll-snap-type: x mandatory;
  scrollbar-width: thin;
}
.collection-list-grid--carousel .collection-list-card {
  flex: 0 0 calc(100% / var(--collection-list-current-columns, 3));
  scroll-snap-align: start;
}
.collection-list-grid--editorial {
  grid-auto-rows: 110px;
  align-items: stretch;
}
.collection-list-grid--editorial .collection-list-card:nth-child(5n + 1) {
  grid-row: span 3;
}
.collection-list-grid--editorial .collection-list-card:nth-child(5n + 3) {
  grid-row: span 2;
}
.collection-list-card {
  min-width: 0;
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
.collection-list-card.has-image {
  color: white;
}
.collection-list-card strong {
  font-family: var(--store-heading-font, Georgia, serif);
  font-size: 20px;
}
.collection-list-card small,
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
  .collection-list-grid {
    --collection-list-current-columns: var(--collection-list-mobile-columns, 1);
    grid-template-columns: repeat(
      var(--collection-list-current-columns),
      minmax(0, 1fr)
    );
  }
}
.collection-list-grid--bento {grid-auto-rows:180px}
.collection-list-grid--bento .collection-list-card:first-child {grid-column:span 2;grid-row:span 2}
.collection-list-grid--spotlight {grid-template-columns:repeat(2,minmax(0,1fr))}
.collection-list-grid--spotlight .collection-list-card {min-height:340px;justify-content:flex-end;gap:12px}
.collection-list-grid--spotlight strong {font-size:clamp(28px,4vw,54px)}
.collection-list-grid--text {display:flex;flex-direction:column;gap:0}
.collection-list-grid--text .collection-list-card {min-height:0;flex-direction:row;align-items:center;padding:24px 0;background:none;border-radius:0;border-bottom:1px solid var(--store-secondary)}
.collection-list-grid--text strong {font-size:clamp(24px,4vw,48px);flex:1;padding-inline:20px}
@media(max-width:600px){.collection-list-grid--bento .collection-list-card:first-child{grid-column:auto;grid-row:span 2}.collection-list-grid--spotlight{grid-template-columns:1fr}}
</style>
