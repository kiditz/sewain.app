<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { VueDraggable } from "vue-draggable-plus";
import type { StorefrontBlock } from "~/types/storefront";
import { blockRegistry } from "../utils/block-registry";

const props = defineProps<{
  blocks: StorefrontBlock[];
  selectedBlockId: string | null;
}>();

const emit = defineEmits<{
  select: [id: string];
  toggle: [block: StorefrontBlock];
  addChild: [block: StorefrontBlock];
  reorder: [blocks: StorefrontBlock[]];
  reorderChildren: [value: { parent: StorefrontBlock; blocks: StorefrontBlock[] }];
}>();

const orderedBlocks = computed({
  get: () => props.blocks,
  set: (blocks: StorefrontBlock[]) => emit("reorder", blocks),
});
const collapsedBlocks = ref<Record<string, boolean>>({});

function containsSelection(block: StorefrontBlock, id: string): boolean {
  return block.id === id || !!block.blocks?.some(child => containsSelection(child, id));
}

watch(() => props.selectedBlockId, (id) => {
  if (!id) return;
  for (const block of props.blocks) {
    if (block.blocks?.some(child => containsSelection(child, id))) {
      collapsedBlocks.value[block.id] = false;
    }
  }
});

function canAddChild(block: StorefrontBlock) {
  const definition = blockRegistry[block.type];
  return (
    !!definition.allowedChildren?.length &&
    (!definition.maxChildren ||
      (block.blocks?.length ?? 0) < definition.maxChildren)
  );
}
</script>

<template>
  <VueDraggable
    v-model="orderedBlocks"
    item-key="id"
    :group="{ name: 'block-tree', pull: false, put: false }"
    handle=".block-drag-handle"
    class="block-tree-list"
    :animation="180"
  >
    <div v-for="block in orderedBlocks" :key="block.id" class="block-tree-node">
      <div
        class="nested-block-row"
        :class="{ active: selectedBlockId === block.id, 'is-hidden': !block.enabled }"
        @click.stop="emit('select', block.id)"
      >
        <v-icon class="block-drag-handle" size="15">mdi-drag</v-icon>
        <v-btn
          v-if="block.blocks?.length"
          :icon="collapsedBlocks[block.id] ? 'mdi-chevron-right' : 'mdi-chevron-down'"
          size="x-small"
          variant="text"
          :aria-label="block.type === 'SLIDE' ? String(block.settings.name || blockRegistry[block.type].label) : blockRegistry[block.type].label"
          :aria-expanded="!collapsedBlocks[block.id]"
          @click.stop="collapsedBlocks[block.id] = !collapsedBlocks[block.id]"
        />
        <v-icon v-else size="15" class="block-leaf-icon">mdi-shape-outline</v-icon>
        <span class="block-tree-label">{{ block.type === 'SLIDE' ? String(block.settings.name || blockRegistry[block.type].label) : blockRegistry[block.type].label }}</span>
        <v-btn
          v-if="canAddChild(block)"
          icon="mdi-plus"
          size="x-small"
          variant="text"
          :aria-label="$t('ui.add_nested_block')"
          @click.stop="collapsedBlocks[block.id] = false; emit('addChild', block)"
        />
        <v-btn
          :icon="block.enabled ? 'mdi-eye-outline' : 'mdi-eye-off-outline'"
          size="x-small"
          variant="text"
          :aria-label="block.enabled ? 'Hide block' : 'Show block'"
          @click.stop="emit('toggle', block)"
        />
      </div>
      <BlockTreeList
        v-if="block.blocks?.length"
        v-show="!collapsedBlocks[block.id]"
        :blocks="block.blocks"
        :selected-block-id="selectedBlockId"
        @select="emit('select', $event)"
        @toggle="emit('toggle', $event)"
        @add-child="emit('addChild', $event)"
        @reorder="emit('reorderChildren', { parent: block, blocks: $event })"
        @reorder-children="emit('reorderChildren', $event)"
      />
    </div>
  </VueDraggable>
</template>

<style scoped>
.block-tree-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-height: 4px;
}
.block-tree-node {
  min-width: 0;
}
.block-tree-node > .block-tree-list {
  margin: 2px 0 4px 26px;
  padding-left: 8px;
  border-left: 1px solid rgba(var(--v-theme-on-surface), 0.16);
}
.nested-block-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 4px;
  min-height: 34px;
  padding: 3px 5px;
  border-radius: 6px;
  cursor: pointer;
}
.nested-block-row::before {
  content: "";
  position: absolute;
  left: -8px;
  top: 50%;
  width: 8px;
  border-top: 1px solid rgba(var(--v-theme-on-surface), 0.16);
}
.nested-block-row:hover {
  background: rgba(var(--v-theme-on-surface), 0.04);
}
.nested-block-row.is-hidden .block-tree-label {
  opacity: 0.5;
}
.nested-block-row.active {
  color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.09);
}
.block-tree-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.block-drag-handle {
  cursor: grab;
  color: rgba(var(--v-theme-on-surface), 0.4);
}
.block-leaf-icon {
  color: rgba(var(--v-theme-on-surface), 0.6);
  flex-shrink: 0;
}
</style>
