<script setup lang="ts">
import { computed } from "vue";
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
        :class="{ active: selectedBlockId === block.id }"
        @click.stop="emit('select', block.id)"
      >
        <v-icon class="block-drag-handle" size="15">mdi-drag</v-icon>
        <span class="block-tree-label">{{ blockRegistry[block.type].label }}</span>
        <v-btn
          v-if="canAddChild(block)"
          icon="mdi-plus"
          size="x-small"
          variant="text"
          :aria-label="$t('ui.add_nested_block')"
          @click.stop="emit('addChild', block)"
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
.block-tree-list .block-tree-list {
  margin: 2px 0 4px 20px;
  padding-left: 8px;
  border-left: 1px solid #d8dde5;
}
.nested-block-row {
  display: flex;
  align-items: center;
  gap: 7px;
  min-height: 34px;
  padding: 3px 5px;
  border-radius: 6px;
  cursor: pointer;
}
.nested-block-row.active {
  color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.09);
}
.block-tree-label {
  flex: 1;
  overflow: hidden;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.block-drag-handle {
  cursor: grab;
}
</style>
