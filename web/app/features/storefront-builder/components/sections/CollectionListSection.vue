<script setup lang="ts">
import ResourceListLayout from "../ResourceListLayout.vue";
import {resourceListItemStyles} from "../../utils/resource-list-layout";
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
  props.blocks.filter((block) => !["COLLECTION_LIST", "COLLECTION_CARD"].includes(block.type)),
);
const collectionCardTemplate = computed(() => props.blocks.find(block => block.type === "COLLECTION_CARD"));
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
const itemStyles = computed(() => resourceListItemStyles(displayType.value, selectedCollections.value.length, 'collections'));
</script>

<template>
  <section class="collection-list-section" :style="sectionStyle">
    <div class="collections-inner">
      <BlockRenderer
        :blocks="contentBlocks"
        :preview="preview"
        :selected-block-id="selectedBlockId"
        :product="product"
      />
      <ResourceListLayout
        v-if="selectedCollections.length && (!collectionCardTemplate || collectionCardTemplate.enabled)"
        class="collection-list-grid"
        :class="`collection-list-grid--${displayType}`"
        :layout="displayType"
        :columns="Number(settings.columns) || 3"
        :mobile-columns="Number(settings.mobileColumns) || 1"
        :gap="Number(settings.itemsGap ?? (displayType === 'editorial' ? 32 : 18))"
      >
        <template v-if="collectionCardTemplate">
          <div v-for="(collection, index) in selectedCollections" :key="collection.id" class="resource-layout__item collection-list-card collection-list-card--template" :style="itemStyles[index]">
            <BlockRenderer :blocks="[collectionCardTemplate]" :collection-context="collection" :preview="preview" :selected-block-id="selectedBlockId" />
          </div>
        </template>
        <template v-else>
        <NuxtLink
          v-for="(collection, index) in selectedCollections"
          :key="collection.id"
          :to="preview ? undefined : `/collections/${collection.slug}`"
          class="resource-layout__item collection-list-card"
          :class="{ 'has-image': collection.imageUrl && displayType !== 'text' }"
          :style="{...itemStyles[index], ...(collection.imageUrl && displayType !== 'text' ? {backgroundImage: `linear-gradient(0deg, #101816b8, #10181605 75%), url('${collection.imageUrl}')`} : {})}"
        >
          <span class="collection-number">{{ String(index + 1).padStart(2, "0") }}</span>
          <strong>{{ collection.name }}</strong>
          <small v-if="collection.productCount > 0">
            {{ collection.productCount }} {{ $t("ui.products") }}
          </small>
          <small v-else>{{ collection.description || $t("ui.explore_collection") }}</small>
        </NuxtLink>
        </template>
      </ResourceListLayout>
      <p v-else-if="!selectedCollections.length && (!collectionCardTemplate || collectionCardTemplate.enabled)" class="empty-catalog">{{ $t("ui.no_collections_to_display") }}</p>
    </div>
  </section>
</template>

<style scoped>
.collection-list-section { container: collection-list / inline-size; }
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
.collection-list-grid { width:100%;min-width:0;margin-top:10px; }
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
.collection-list-grid--spotlight .collection-list-card {min-height:340px;justify-content:flex-end;gap:12px}
.collection-list-grid--spotlight strong {font-size:clamp(28px,4vw,54px)}
.collection-list-grid--text .collection-list-card {min-height:0;flex-direction:row;align-items:center;padding:24px 0;background:none;border-radius:0;border-bottom:1px solid var(--store-secondary)}
.collection-list-grid--text strong {font-size:clamp(24px,4vw,48px);flex:1;padding-inline:20px}
.collection-list-card--template {padding:0;background:none;color:var(--store-text)}
.collection-list-card--template > :deep(.block-renderer) {min-height:100%}
</style>
