<script setup lang="ts">
import { VueDraggable } from "vue-draggable-plus";
import { sectionRegistry } from "~/features/storefront-builder/utils/section-registry";
import { builderDndKey } from "~/features/storefront-builder/utils/builder-dnd";
import "@vueup/vue-quill/dist/vue-quill.snow.css";

import type { Product } from "~/types/product";
import type { StorefrontSection } from "~/types/storefront";
const props = defineProps<{
  sections: StorefrontSection[];
  preview?: boolean;
  selectedSectionId?: string | null;
  selectedBlockId?: string | null;
  product?: Product;
}>();
const visibleSections = computed(() =>
  props.sections.filter(
    (section) => section.enabled && sectionRegistry[section.type],
  ),
);

// Drag & drop section langsung di preview. Hanya aktif jika dirender di dalam
// PageBuilder (ada provider-nya) dan dalam mode preview.
const builder = inject(builderDndKey, null);
const canDrag = computed(() => !!props.preview && !!builder);

const dragSections = computed<StorefrontSection[]>({
  get: () => visibleSections.value,
  set: (next) => builder?.reorderSections(next.map((section) => section.id)),
});
</script>
<template>
  <VueDraggable
    v-model="dragSections"
    class="storefront-sections"
    :disabled="!canDrag"
    handle=".builder-section-handle"
    ghost-class="builder-ghost"
    :animation="180"
  >
    <div
      v-for="section in dragSections"
      :key="section.id"
      class="builder-section"
      :class="{
        'is-selected': preview && section.id === selectedSectionId,
        'is-preview': preview,
        'is-navigation-sticky': section.type === 'NAVIGATION' && section.settings.sticky === true,
      }"
      :data-section-id="section.id"
      :data-section-type="section.type"
      :data-section-label="sectionRegistry[section.type].label"
      :data-section-selected="
        preview && section.id === selectedSectionId ? 'true' : 'false'
      "
    >
      <div
        v-if="canDrag"
        class="builder-handle builder-section-handle"
        :title="$t('ui.drag_to_reorder_section')"
      >
        <v-icon size="14">mdi-drag</v-icon>
        {{ sectionRegistry[section.type].label }}
      </div>

      <component
        :is="sectionRegistry[section.type].component"
        :settings="section.settings"
        :blocks="section.blocks"
        :preview="preview ?? false"
        :selected-block-id="selectedBlockId"
        :product="product"
      />
    </div>
  </VueDraggable>
</template>

<style>
.builder-section.is-navigation-sticky {
  position: sticky;
  top: 0;
  z-index: 10;
}
/* Style handle drag di preview builder (global, class ber-prefix `builder-`). */
.builder-handle {
  position: absolute;
  z-index: 10000;
  display: none;
  align-items: center;
  gap: 2px;
  padding: 0 8px 0 3px;
  background: #2563eb;
  color: #fff;
  font-size: 10px;
  line-height: 20px;
  white-space: nowrap;
  cursor: grab;
  user-select: none;
}

.builder-handle:active {
  cursor: grabbing;
}

.builder-handle .v-icon {
  color: inherit;
}

.builder-section-handle {
  top: 0;
  left: 0;
}

/* Tampil saat section dipilih, atau saat di-hover (kecuali sedang hover block). */
.builder-section.is-selected > .builder-section-handle {
  display: inline-flex;
}

.builder-section.is-preview:hover:not(:has(.builder-block:hover))
  > .builder-section-handle {
  display: inline-flex;
  opacity: 0.85;
}

.builder-section.is-selected > .builder-section-handle {
  opacity: 1;
}

.builder-ghost {
  opacity: 0.35;
  outline: 2px dashed #2563eb;
  outline-offset: -2px;
}
</style>
