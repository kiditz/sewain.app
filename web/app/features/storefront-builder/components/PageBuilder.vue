<script setup lang="ts">
import { VueDraggable } from "vue-draggable-plus";
import { watch } from "vue";
import BlockSettingsPanel from "~/features/storefront-builder/components/blocks/settings/BlockSettingsPanel.vue";
import BlockSettingsFields from "~/features/storefront-builder/components/blocks/settings/BlockSettingsFields.vue";
import SectionSettingsPanel from "~/features/storefront-builder/components/sections/settings/SectionSettingsPanel.vue";
import SectionSettingsFields from "~/features/storefront-builder/components/sections/settings/SectionSettingsFields.vue";
import PageAutocomplete from "./PageAutocomplete.vue";
import BlockTreeList from "./BlockTreeList.vue";
import { blockRegistry } from "~/features/storefront-builder/utils/block-registry";
import {
  sectionRegistry,
  sectionTypes,
} from "~/features/storefront-builder/utils/section-registry";
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";
import {
  applyOrder,
  builderDndKey,
} from "~/features/storefront-builder/utils/builder-dnd";
import type {
  BlockType,
  StorefrontBlock,
  StorefrontPage,
  StorefrontSection,
  SectionType,
} from "~/types/storefront";
const props = defineProps<{ page: StorefrontPage }>();
const emit = defineEmits<{
  back: [];
  saved: [];
}>();
const store = useStorefront();
await store.loadCollections().catch(() => undefined);
const pageSaveStatus = computed(
  () => store.pageStore.pageStatus(props.page.id).status.value,
);
const saving = computed(() => pageSaveStatus.value.saving);
const saveError = computed(() => pageSaveStatus.value.error);
const hasUnsavedChanges = computed(() => pageSaveStatus.value.dirty);

const selectedId = ref<string | null>(props.page.sections[0]?.id ?? null);
const selectedBlockId = ref<string | null>(
  props.page.sections[0]?.blocks[0]?.id ?? null,
);
const router = useRouter();
function switchPage(selectedPage: StorefrontPage) {
  if (selectedPage.id === props.page.id) return;
  if (!store.pages.value.some((page) => page.id === selectedPage.id)) return;
  router.push(`/merchant/storefront/pages/${selectedPage.slug}`);
}

watch(
  () => props.page.id,
  () => {
    selectedId.value = props.page.sections[0]?.id ?? null;
    selectedBlockId.value = props.page.sections[0]?.blocks[0]?.id ?? null;
  },
);

const selectedSection = computed(
  () =>
    props.page.sections.find((section) => section.id === selectedId.value) ??
    null,
);
function findBlock(
  blocks: StorefrontBlock[],
  id: string,
): StorefrontBlock | null {
  for (const block of blocks) {
    if (block.id === id) return block;
    const nested = findBlock(block.blocks ?? [], id);
    if (nested) return nested;
  }
  return null;
}
function hasProductCardAncestor(
  blocks: StorefrontBlock[],
  id: string,
  insideProductCard = false,
): boolean {
  for (const block of blocks) {
    if (block.id === id) return insideProductCard;
    if (
      hasProductCardAncestor(
        block.blocks ?? [],
        id,
        insideProductCard || block.type === "PRODUCT_CARD",
      )
    ) {
      return true;
    }
  }
  return false;
}
const selectedBlock = computed(() =>
  selectedSection.value
    ? findBlock(selectedSection.value.blocks, selectedBlockId.value ?? "")
    : null,
);
const selectedBlockIsProductImage = computed(
  () =>
    selectedBlock.value?.type === "IMAGE" &&
    selectedSection.value !== null &&
    hasProductCardAncestor(
      selectedSection.value.blocks,
      selectedBlock.value.id,
    ),
);
const addingBlockParentId = ref<string | null>(null);
const addingBlockParent = computed(() =>
  selectedSection.value && addingBlockParentId.value
    ? findBlock(selectedSection.value.blocks, addingBlockParentId.value)
    : null,
);
const availableBlocks = computed(() => {
  if (!selectedSection.value) return [];
  const types = addingBlockParent.value
    ? (blockRegistry[addingBlockParent.value.type].allowedChildren ?? [])
    : sectionRegistry[selectedSection.value.type].allowedBlocks;
  return types.map((type) => blockRegistry[type]);
});
const addingBlockType = ref<BlockType | null>(null);
const showAddBlock = ref(false);
const addingType = ref<SectionType | null>(null);
const showAddSection = ref(false);
const sectionScrollbar = ref<{ $refs: { el?: { update: () => void } } }>();
async function refreshSectionScrollbar() {
  await nextTick();
  sectionScrollbar.value?.$refs.el?.update();
}
const previewDevice = ref<"desktop" | "mobile">("desktop");
const previewScrollbar = ref<{ $refs: { el?: { update: () => void } } }>();
const previewViewport = ref<HTMLElement>();
const previewContent = ref<HTMLElement>();
let previewObserver: ResizeObserver | undefined;
let previewDisposed = false;
function refreshPreviewScrollbar() {
  previewScrollbar.value?.$refs.el?.update();
}
onMounted(async () => {
  await nextTick();
  if (previewDisposed) return;
  previewObserver = new ResizeObserver(refreshPreviewScrollbar);
  if (previewViewport.value) previewObserver.observe(previewViewport.value);
  if (previewContent.value) previewObserver.observe(previewContent.value);
  refreshPreviewScrollbar();
});
watch([() => props.page.id, previewDevice], async () => {
  await nextTick();
  refreshPreviewScrollbar();
});
onBeforeUnmount(() => {
  previewDisposed = true;
  previewObserver?.disconnect();
});
const previewStyle = computed(() => ({
  maxWidth: previewDevice.value === "mobile" ? "390px" : "100%",
}));

function addSection() {
  if (addingType.value) {
    store.addSection(props.page, addingType.value);
    selectedId.value = props.page.sections.at(-1)?.id ?? null;
    selectedBlockId.value = props.page.sections.at(-1)?.blocks[0]?.id ?? null;
    addingType.value = null;
    showAddSection.value = false;
  }
}
function selectSection(id: string) {
  selectedId.value = id;
  selectedBlockId.value =
    props.page.sections.find((section) => section.id === id)?.blocks[0]?.id ??
    null;
}
function openAddBlock(section: StorefrontSection, parent?: StorefrontBlock) {
  selectSection(section.id);
  addingBlockParentId.value = parent?.id ?? null;
  addingBlockType.value = null;
  showAddBlock.value = true;
}
function handlePreviewClick(event: MouseEvent) {
  const target = event.target;
  if (!(target instanceof Element)) return;
  const blockElement = target.closest<HTMLElement>("[data-block-id]");
  const sectionElement = target.closest<HTMLElement>("[data-section-id]");
  if (!sectionElement) return;
  if (target.closest("a")) event.preventDefault();
  selectedId.value = sectionElement.dataset.sectionId ?? null;
  selectedBlockId.value = blockElement?.dataset.blockId ?? null;
}
function addBlock() {
  if (selectedSection.value && addingBlockType.value) {
    const block = store.addBlock(
      props.page,
      selectedSection.value,
      addingBlockType.value,
      addingBlockParentId.value ?? undefined,
    );
    selectedBlockId.value = block?.id ?? null;
    addingBlockParentId.value = null;
    addingBlockType.value = null;
    showAddBlock.value = false;
  }
}
function deleteSelectedBlock() {
  if (!selectedSection.value || !selectedBlock.value) return;
  store.removeBlock(props.page, selectedSection.value, selectedBlockId.value!);
  selectedBlockId.value = null;
}
function updateBlockSetting(
  block: StorefrontBlock,
  key: string,
  value: unknown,
) {
  block.settings[key] = value;
  store.touch(props.page);
}
function deleteSelected() {
  if (!selectedSection.value) return;
  const index = props.page.sections.findIndex(
    (section) => section.id === selectedId.value,
  );
  store.removeSection(props.page, selectedId.value!);
  const nextSection =
    props.page.sections[Math.max(0, index - 1)] ?? props.page.sections[0];
  selectedId.value = nextSection?.id ?? null;
  selectedBlockId.value = nextSection?.blocks[0]?.id ?? null;
}
function save() {
  store.touch(props.page);
  emit("saved");
}
function updateSlug(value: string) {
  props.page.slug = store.slugify(value);
  props.page.targetUrl = store.targetUrlFor(props.page);
  store.touch(props.page);
}
function updateSetting(
  section: StorefrontSection,
  key: string,
  value: unknown,
) {
  section.settings[key] = value;
  store.touch(props.page);
}
const { theme } = store;

provide(builderDndKey, {
  reorderSections(orderedIds) {
    const next = applyOrder(props.page.sections, orderedIds);
    if (!next) return;

    props.page.sections = next;
    store.touch(props.page);
  },
  reorderBlocks(orderedIds) {
    const findSiblingList = (
      blocks: StorefrontBlock[],
      id: string,
    ): StorefrontBlock[] | null => {
      if (blocks.some((block) => block.id === id)) return blocks;
      for (const block of blocks) {
        const nested = findSiblingList(block.blocks ?? [], id);
        if (nested) return nested;
      }
      return null;
    };
    const section = props.page.sections.find((item) =>
      findSiblingList(item.blocks, orderedIds[0]!),
    );
    if (!section) return;

    const siblings = findSiblingList(section.blocks, orderedIds[0]!);
    if (!siblings) return;
    const next = applyOrder(siblings, orderedIds);
    if (!next) return;

    siblings.splice(0, siblings.length, ...next);
    store.touch(props.page);
  },
});
</script>

<template>
  <div class="builder">
    <v-app-bar elevation="0">
      <template #prepend>
        <v-btn
          icon="mdi-arrow-left"
          variant="text"
          :aria-label="$t('common.back')"
          @click="emit('back')"
        />
      </template>
      <template #title>
        {{ page.title }}
      </template>
      <v-spacer />
      <page-autocomplete
        :model-value="page.id"
        :label="page.title"
        @select="switchPage"
      />
      <v-spacer />
      <template #append>
        <v-chip
          :color="page.status === 'PUBLISHED' ? 'success' : 'default'"
          size="small"
          >{{ page.status === "PUBLISHED" ? "Published" : "Draft" }}</v-chip
        >
        <span v-if="saveError" class="save-status save-status--error">{{
          $t("common.saveFailed")
        }}</span>

        <span v-else class="save-status mr-3">{{
          saving ? "Saving…" : hasUnsavedChanges ? "Unsaved changes" : "Saved"
        }}</span>
        <v-btn class="mr-2" color="primary" variant="outlined" @click="save">{{
          $t("common.save")
        }}</v-btn>
        <v-btn
          color="primary"
          @click="
            page.status = 'PUBLISHED';
            save();
          "
          >{{ $t("ui.publish") }}</v-btn
        >
      </template>
    </v-app-bar>
    <div class="builder-body">
      <aside class="builder-sidebar section-sidebar">
        <div class="sidebar-title">
          <div>
            <strong>{{ $t("ui.page_sections") }}</strong
            ><small>{{ $t("ui.drag_to_reorder") }}</small>
          </div>
          <v-btn
            icon="mdi-plus"
            size="small"
            variant="tonal"
            :aria-label="$t('ui.add_section')"
            @click="showAddSection = true"
          />
        </div>
        <VueDraggable
          v-model="page.sections"
          item-key="id"
          handle=".drag-handle"
          class="section-list"
          :animation="180"
          @end="store.touch(page)"
        >
          <div
            v-for="(section, index) in page.sections"
            :key="section.id"
            class="section-tree-item"
          >
            <div
              class="section-row"
              :class="{ active: selectedId === section.id }"
              @click="selectSection(section.id)"
            >
              <v-icon class="drag-handle" size="18">mdi-drag</v-icon>
              <div class="section-row__text">
                <strong
                  >{{ index + 1 }}.
                  {{ sectionRegistry[section.type].label }}</strong
                >
                <small>{{ section.enabled ? "Visible" : "Hidden" }}</small>
              </div>
              <v-btn
                :icon="
                  section.enabled ? 'mdi-eye-outline' : 'mdi-eye-off-outline'
                "
                size="x-small"
                variant="text"
                :aria-label="section.enabled ? 'Hide section' : 'Show section'"
                @click.stop="
                  section.enabled = !section.enabled;
                  store.touch(page);
                "
              />
            </div>
            <BlockTreeList
              :blocks="section.blocks"
              :selected-block-id="
                selectedId === section.id ? selectedBlockId : null
              "
              @select="
                (id) => {
                  selectedId = section.id;
                  selectedBlockId = id;
                }
              "
              @toggle="
                (block) => {
                  block.enabled = !block.enabled;
                  store.touch(page);
                }
              "
              @add-child="(block) => openAddBlock(section, block)"
              @reorder="
                (blocks) => {
                  section.blocks = blocks;
                  store.touch(page);
                }
              "
              @reorder-children="
                ({ parent, blocks }) => {
                  parent.blocks = blocks;
                  store.touch(page);
                }
              "
            />
            <v-btn
              size="x-small"
              variant="text"
              prepend-icon="mdi-plus"
              v-if="sectionRegistry[section.type].allowedBlocks.length"
              class="nested-add-block"
              @click.stop="openAddBlock(section)"
              >{{ $t("ui.add_block") }}</v-btn
            >
          </div>
        </VueDraggable>
        <v-btn
          block
          prepend-icon="mdi-plus"
          variant="tonal"
          class="add-section-button"
          @click="showAddSection = true"
          >{{ $t("ui.add_section") }}</v-btn
        >
        <div class="page-settings">
          <strong>{{ $t("ui.page_settings") }}</strong
          ><v-text-field
            :model-value="page.title"
            :label="$t('ui.page_title')"
            density="compact"
            variant="outlined"
            hide-details
            class="mt-3"
            @update:model-value="
              page.title = String($event || '');
              store.touch(page);
            "
          /><v-text-field
            :model-value="page.slug"
            :label="$t('collections.fields.slug')"
            :prefix="page.kind === 'CUSTOM' ? '/pages/' : '/'"
            density="compact"
            variant="outlined"
            hide-details
            class="mt-3"
            :disabled="page.kind !== 'CUSTOM'"
            @update:model-value="updateSlug(String($event || ''))"
          />
        </div>
      </aside>
      <main class="preview-area">
        <div class="preview-toolbar">
          <span>{{ $t("ui.preview") }}</span>
          <div>
            <v-btn-toggle
              v-model="previewDevice"
              mandatory
              density="compact"
              divided
              ><v-btn
                value="desktop"
                icon="mdi-monitor"
                :aria-label="$t('ui.desktop_preview')" /><v-btn
                value="mobile"
                icon="mdi-cellphone"
                :aria-label="$t('ui.mobile_preview')"
            /></v-btn-toggle>
          </div>
        </div>
        <div ref="previewViewport" class="preview-scroll-viewport">
          <nuxt-scrollbar
            ref="previewScrollbar"
            style="position: absolute; inset: 0; width: 100%; height: 100%"
            class="preview-scroll"
            :options="{ suppressScrollX: true, wheelPropagation: false }"
          >
            <div ref="previewContent" class="preview-scroll-content">
            <div
              class="preview-canvas"
              :class="{ 'is-mobile-preview': previewDevice === 'mobile' }"
              :style="previewStyle"
              @click="handlePreviewClick"
            >
              <StorefrontLayout :theme="theme" preview>
                <StorefrontRenderer
                  :sections="page.sections"
                  :preview="true"
                  :selected-section-id="selectedId"
                  :selected-block-id="selectedBlockId"
                />
              </StorefrontLayout>
            </div>
            </div>
          </nuxt-scrollbar>
        </div>
      </main>
      <aside class="builder-sidebar settings-sidebar">
        <template v-if="selectedSection">
          <SectionSettingsPanel
            v-if="!selectedBlockId"
            :section="selectedSection"
            :section-label="sectionRegistry[selectedSection.type].label"
            @delete="deleteSelected"
            @touch="store.touch(page)"
          />

          <SectionSettingsFields
            v-if="!selectedBlockId"
            :section="selectedSection"
            @update="
              (key, value) => updateSetting(selectedSection!, key, value)
            "
          />

          <template v-if="selectedBlock">
            <BlockSettingsPanel
              :block="selectedBlock"
              :block-label="blockRegistry[selectedBlock.type].label"
              @delete="deleteSelectedBlock"
              @touch="store.touch(page)"
            />

            <BlockSettingsFields
              :key="selectedBlock.id"
              :block="selectedBlock"
              :collections="store.collections.value"
              :product-image="selectedBlockIsProductImage"
              @update="
                (key, value) => updateBlockSetting(selectedBlock!, key, value)
              "
            />
          </template>
        </template>
        <!-- <div v-else class="empty-settings">
          <v-icon size="32">mdi-cursor-default-click-outline</v-icon
          ><strong>Select a section</strong
          ><span>Choose a section to edit its settings.</span>
        </div> -->
      </aside>
    </div>
    <v-dialog
      v-model="showAddSection"
      max-width="680"
      @after-enter="refreshSectionScrollbar"
    >
      <v-card rounded="xl" class="add-section-dialog">
        <v-card-title class="pa-5">{{ $t("ui.add_a_section") }}</v-card-title>
        <v-divider />
        <v-card-text class="add-section-dialog__body pa-0">
          <nuxt-scrollbar
            ref="sectionScrollbar"
            style="position: relative; height: min(60dvh, calc(100dvh - 220px))"
            :options="{ suppressScrollX: true, wheelPropagation: false }"
          >
            <div class="pa-5">
              <v-radio-group v-model="addingType" hide-details>
                <v-list lines="two" select-strategy="single-independent">
                  <v-list-item
                    v-for="definition in sectionTypes.filter(
                      (item) =>
                        !['NAVIGATION', 'FOOTER'].includes(item.type) ||
                        !page.sections.some(
                          (section) => section.type === item.type,
                        ),
                    )"
                    :key="definition.type"
                    :title="definition.label"
                    :subtitle="definition.description"
                    @click="addingType = definition.type"
                  >
                    <template #prepend
                      ><v-radio :value="definition.type"
                    /></template>
                    <template #append
                      ><v-icon>mdi-plus-circle-outline</v-icon></template
                    >
                  </v-list-item>
                </v-list>
              </v-radio-group>
            </div>
          </nuxt-scrollbar>
        </v-card-text>
        <v-divider />
        <v-card-actions class="pa-5">
          <v-spacer />
          <v-btn variant="text" @click="showAddSection = false">{{
            $t("common.cancel")
          }}</v-btn>
          <v-btn color="primary" :disabled="!addingType" @click="addSection">{{
            $t("ui.add_section")
          }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-dialog v-model="showAddBlock" max-width="600">
      <v-card rounded="xl"
        ><v-card-title class="pa-5">{{ $t("ui.add_a_block") }}</v-card-title
        ><v-card-text>
          <p class="text-body-medium text-medium-emphasis mb-3">
            {{ $t("ui.available_blocks_for") }}
            {{
              selectedSection
                ? sectionRegistry[selectedSection.type].label
                : "this section"
            }}.
          </p>
          <v-radio-group v-model="addingBlockType">
            <v-list lines="two" select-strategy="single-independent">
              <v-list-item
                v-for="definition in availableBlocks"
                :key="definition.type"
                :title="definition.label"
                :subtitle="definition.description"
                @click="addingBlockType = definition.type"
              >
                <template #prepend
                  ><v-radio :value="definition.type" /></template
                ><template #append
                  ><v-icon>mdi-plus-circle-outline</v-icon></template
                >
              </v-list-item>
            </v-list>
          </v-radio-group> </v-card-text
        ><v-card-actions class="pa-5 pt-0"
          ><v-spacer /><v-btn variant="text" @click="showAddBlock = false">{{
            $t("common.cancel")
          }}</v-btn
          ><v-btn
            color="primary"
            :disabled="!addingBlockType"
            @click="addBlock"
            >{{ $t("ui.add_block") }}</v-btn
          ></v-card-actions
        ></v-card
      >
    </v-dialog>
  </div>
</template>

<style scoped>
.add-section-dialog {
  max-height: calc(100dvh - 48px);
  overflow: hidden;
}
.add-section-dialog__body {
  min-height: 0;
  overflow: hidden;
}
.add-section-dialog > .v-card-title,
.add-section-dialog > .v-card-actions {
  flex-shrink: 0;
}

.builder {
  height: calc(100dvh - var(--v-layout-top, 0px) - var(--v-layout-bottom, 0px));
  box-sizing: border-box;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #f5f6f8;
  color: #20252b;
}

.builder-topbar {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 18px;
  background: white;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.page-heading {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.page-heading span {
  font-size: 12px;
  color: #7a818b;
}

.save-status {
  color: #707780;
  font-size: 12px;
  white-space: nowrap;
}

.save-status--error {
  color: #b3261e;
}

.builder-body {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 340px;
  grid-template-rows: minmax(0, 1fr);
  overflow: hidden;
  min-height: 0;
  flex: 1;
}

.builder-sidebar {
  background: white;
  overflow: auto;
}

.section-sidebar {
  border-right: 1px solid #e5e7eb;
  padding: 18px 14px;
}

.settings-sidebar {
  border-left: 1px solid #e5e7eb;
}

.sidebar-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.sidebar-title > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sidebar-title strong {
  font-size: 14px;
}

.sidebar-title small {
  font-size: 12px;
  color: #858b94;
}

.section-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.section-row {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 10px 6px;
  border: 1px solid transparent;
  border-radius: 9px;
  cursor: pointer;
}

.section-row.active {
  border-color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.07);
}

.drag-handle {
  cursor: grab;
  color: #858b94;
}

.section-row__text {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.section-row__text strong {
  font-size: 12px;
  font-weight: 600;
}

.section-row__text small {
  font-size: 11px;
  color: #858b94;
}

.add-section-button {
  margin-top: 12px;
}

.page-settings {
  border-top: 1px solid #e5e7eb;
  margin-top: 22px;
  padding-top: 18px;
}

.page-settings > strong {
  font-size: 14px;
}

.preview-area {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.preview-toolbar {
  height: 52px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  font-size: 13px;
  font-weight: 600;
}

.preview-scroll-viewport {
  position: relative;
  flex: 1 1 0;
  min-height: 0;
  overflow: hidden;
}

.preview-scroll-viewport :deep(.preview-scroll) {
  box-sizing: border-box;
}

.preview-scroll-content {
  width: 100%;
  box-sizing: border-box;
  padding: 24px;
  display: flow-root;
}

.preview-canvas {
  width: 100%;
  margin-inline: auto;
  background: white;
  box-shadow: 0 4px 22px #17203312;
  overflow: hidden;
  transition: max-width 0.2s;
}
.preview-canvas.is-mobile-preview :deep(.product-list-grid) {
  --product-list-current-columns: var(--product-list-mobile-columns, 1);
  grid-template-columns: repeat(
    var(--product-list-current-columns),
    minmax(0, 1fr)
  );
}
.preview-canvas :deep(.builder-section) {
  position: relative;
  cursor: pointer;
}

.preview-canvas :deep(.builder-section.is-selected) {
  z-index: 3;
}

.preview-canvas :deep(.builder-section.is-selected::after) {
  content: "";
  position: absolute;
  inset: 0;
  border: 2px solid #2563eb;
  pointer-events: none;
  z-index: 3;
}

.setting-field {
  margin: 14px 0;
}

.section-tree-item {
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f1f3;
}

.nested-block-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin: 2px 0 0 20px;
  padding-left: 8px;
  border-left: 1px solid #e5e7eb;
}

.nested-block-row {
  display: flex;
  align-items: center;
  gap: 4px;
  min-height: 30px;
  padding: 2px 3px;
  border: 1px solid transparent;
  border-radius: 6px;
  color: #626a75;
  cursor: pointer;
  font-size: 11px;
}

.nested-block-row.active {
  border-color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.07);
  color: #20252b;
}

.nested-block-row span {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nested-add-block {
  margin: 3px 0 0 20px;
  width: calc(100% - 20px);
}

.block-drag-handle {
  cursor: grab;
  color: #858b94;
}

.empty-settings {
  height: 60%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  text-align: center;
  color: #858b94;
}

.empty-settings strong {
  color: #353b43;
}

.empty-settings span {
  font-size: 12px;
  max-width: 190px;
}

@media (max-width: 1000px) {
  .builder-body {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .settings-sidebar {
    display: none;
  }
}

@media (max-width: 680px) {
  .builder-body {
    grid-template-columns: 160px minmax(0, 1fr);
  }

  .builder-sidebar {
    padding: 12px 8px;
  }

  .preview-scroll-content {
    padding: 10px;
  }

  .builder-topbar {
    padding: 0 8px;
    gap: 6px;
  }

  .page-heading span {
    display: none;
  }
}
</style>
