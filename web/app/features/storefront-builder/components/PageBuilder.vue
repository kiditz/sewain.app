<script setup lang="ts">
import {resourceCardLayoutForBlock} from "../utils/resource-list-layout";
import { blockCategories } from "../utils/block-catalog";
import { collectionListTypes } from "../utils/section-catalog";
import { sectionCategories } from "../utils/section-catalog";
import { VueDraggable } from "vue-draggable-plus";
import { watch } from "vue";
import BlockSettingsPanel from "~/features/storefront-builder/components/blocks/settings/BlockSettingsPanel.vue";
import BlockSettingsFields from "~/features/storefront-builder/components/blocks/settings/BlockSettingsFields.vue";
import SectionSettingsPanel from "~/features/storefront-builder/components/sections/settings/SectionSettingsPanel.vue";
import SectionSettingsFields from "~/features/storefront-builder/components/sections/settings/SectionSettingsFields.vue";
import CatalogPicker from "./CatalogPicker.vue";
import CatalogEntryPreview from "./CatalogEntryPreview.vue";
import PageAutocomplete from "./PageAutocomplete.vue";
import BlockTreeList from "./BlockTreeList.vue";
import { blockRegistry } from "~/features/storefront-builder/utils/block-registry";
import {
  sectionRegistry,
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
const collapsedSections = ref<Record<string, boolean>>({});
watch([selectedId, selectedBlockId], ([id]) => {
  if (id) collapsedSections.value[id] = false;
});
const router = useRouter();
const route = useRoute();
function switchPage(selectedPage: StorefrontPage) {
  if (selectedPage.id === props.page.id) return;
  if (!store.pages.value.some((page) => page.id === selectedPage.id)) return;
  router.push({ path: `/merchant/storefront/pages/${selectedPage.slug}`, query: route.query });
}

watch(
  () => props.page.id,
  () => {
    collapsedSections.value = {};
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
  return types.filter(type=>{
    if(type==='VARIANT_SELECTOR')return props.page.kind==='PRODUCT';
    if(['BUY_BUTTONS','PRODUCT_DESCRIPTION','PRODUCT_PRICE','SKU'].includes(type))return props.page.kind==='PRODUCT'||['FEATURED_PRODUCT','PRODUCT_HIGHLIGHT'].includes(selectedSection.value!.type)||addingBlockParent.value?.type==='PRODUCT_CARD'||(!!addingBlockParent.value&&hasProductCardAncestor(selectedSection.value!.blocks,addingBlockParent.value.id));
    return true;
  }).map((type) => blockRegistry[type]);
});
const addingBlockType = ref<BlockType | null>(null);
const showAddBlock = ref(false);
const blockSearch = ref("");
watch(blockSearch, () => { addingBlockType.value=null; });
const availableBlockCategories = computed(() => blockCategories.map(category => ({
  ...category,
  title: translateCatalog(`blockCatalog.categories.${category.id}`),
  entries: category.types.flatMap(type => {const definition=availableBlocks.value.find(block=>block.type===type);return definition?[{...definition,title:translateCatalog(`blockCatalog.labels.${type}`)}]:[];}).filter(block=>`${translateCatalog(`blockCatalog.categories.${category.id}`)} ${block.title} ${block.label}`.toLowerCase().includes((blockSearch.value||'').trim().toLowerCase())),
})).filter(category=>category.entries.length));
const selectedResourceCardLayout=computed(()=>selectedSection.value&&selectedBlock.value?resourceCardLayoutForBlock(selectedSection.value.blocks,selectedBlock.value.id,String(selectedSection.value.settings.displayType??sectionRegistry[selectedSection.value.type].defaults.displayType??'grid')):undefined);
const selectedCollectionCardTemplate=computed(()=>selectedBlock.value?.type==='COLLECTION_CARD'&&!!selectedSection.value&&collectionListTypes.includes(selectedSection.value.type));
const addingType = ref<SectionType | null>(null);
const showAddSection = ref(false);
const sectionSearch = ref("");
const { t: translateCatalog } = useI18n();
const categorizedSections = computed(() => sectionCategories.map(category => ({
  ...category,
  title: translateCatalog(`sectionCatalog.categories.${category.id}`),
  entries: category.types.map(type => ({...sectionRegistry[type], title:translateCatalog(`sectionCatalog.sections.${type}`)})).filter(definition =>
    (!['NAVIGATION','FOOTER'].includes(definition.type) || !props.page.sections.some(section => section.type === definition.type)) &&
    `${translateCatalog(`sectionCatalog.categories.${category.id}`)} ${definition.label} ${definition.title}`.toLowerCase().includes((sectionSearch.value || '').trim().toLowerCase())),
})).filter(category => category.entries.length));
watch(sectionSearch, () => { addingType.value=null; });
const addSectionAfterId = ref<string | null>(null);
const pickerTarget=ref<HTMLElement>();
function openAddSection(afterId: string | null = null,event?:MouseEvent) {
  pickerTarget.value=event?.currentTarget instanceof HTMLElement?event.currentTarget:undefined;
  addSectionAfterId.value = afterId;
  addingType.value = null;
  sectionSearch.value = "";
  showAddSection.value = true;
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
    const section = store.addSection(props.page, addingType.value, addSectionAfterId.value);
    if (!section) return;
    selectedId.value = section.id;
    selectedBlockId.value = section.blocks[0]?.id ?? null;
    addingType.value = null;
    showAddSection.value = false;
  }
}
function selectSection(id: string) {
  selectedId.value = id;
  // selectedBlockId.value =
  //   props.page.sections.find((section) => section.id === id)?.blocks[0]?.id ??
  //   null;
}
function sectionAtBlockLimit(section: StorefrontSection) { const max = sectionRegistry[section.type].maxBlocks; return max != null && section.blocks.length >= max; }
function openAddBlock(section: StorefrontSection, parent?: StorefrontBlock,event?:MouseEvent) {
  pickerTarget.value=event?.currentTarget instanceof HTMLElement?event.currentTarget:undefined;
  selectSection(section.id);
  addingBlockParentId.value = parent?.id ?? null;
  addingBlockType.value = null;
  blockSearch.value = "";
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
          >{{ page.status === "PUBLISHED" ? $t("themeLibrary.pageReady") : $t("themeLibrary.draft") }}</v-chip
        >
        <span v-if="saveError" class="save-status save-status--error">{{
          $t("common.saveFailed")
        }}</span>

        <span v-else class="save-status mr-3">{{
          saving ? "Saving…" : hasUnsavedChanges ? "Unsaved changes" : $t("themeLibrary.draftSaved")
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
          >{{ $t("themeLibrary.includePage") }}</v-btn
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
            @click="openAddSection(null,$event)"
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
            v-for="section in page.sections"
            :key="section.id"
            class="section-tree-item"
          >
            <div
              class="section-row"
              :class="{ active: selectedId === section.id }"
              @click="selectSection(section.id)"
            >
              
              <v-btn
                :icon="collapsedSections[section.id] ? 'mdi-chevron-right' : 'mdi-chevron-down'"
                size="x-small"
                variant="text"
                :aria-label="sectionRegistry[section.type].label"
                :aria-expanded="!collapsedSections[section.id]"
                @click.stop="collapsedSections[section.id] = !collapsedSections[section.id]"
              />
              <v-icon size="17" class="section-type-icon">mdi-view-agenda-outline</v-icon>
              <div class="section-row__text">
                <strong
                  >{{ sectionRegistry[section.type].label }}</strong
                >
                <!-- <small>{{ section.enabled ? "Visible" : "Hidden" }}</small> -->
              </div>
              <div class="tree-row-action">
              <v-btn
                class="tree-visibility"
                :icon="section.enabled ? 'mdi-eye-outline' : 'mdi-eye-off-outline'"
                size="x-small"
                variant="text"
                :aria-label="section.enabled ? 'Hide' : 'Show'"
                @click.stop="section.enabled = !section.enabled; store.touch(page)"
              />
              <v-btn
                class="drag-handle"
                icon="mdi-drag"
                size="x-small"
                variant="text"
                :aria-label="$t('ui.drag_to_reorder')"
                @click.stop
              />
              </div>
            </div>
            <div v-show="!collapsedSections[section.id]" class="section-tree-children">
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
                @add-child="(block,event) => openAddBlock(section, block,event)"
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
                :disabled="sectionAtBlockLimit(section)"
                @click.stop="openAddBlock(section,undefined,$event)"
                >{{ $t("ui.add_block") }}</v-btn
              >
            </div>
            <v-btn
              class="section-insert-button"
              size="x-small"
              variant="text"
              prepend-icon="mdi-plus"
              @click.stop="openAddSection(section.id,$event)"
            >{{ $t('ui.add_section') }}</v-btn>
          </div>
        </VueDraggable>
        <v-btn
          block
          prepend-icon="mdi-plus"
          variant="tonal"
          class="add-section-button"
          @click="openAddSection(null,$event)"
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
              :collection-card-template="selectedCollectionCardTemplate"
              :resource-card-layout="selectedResourceCardLayout"
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
    <CatalogPicker v-model="showAddSection" :target="pickerTarget" :title="$t('ui.add_a_section')" v-model:search="sectionSearch" :selected="addingType" :categories="categorizedSections" :search-label="$t('sectionCatalog.search')" :empty-label="$t('sectionCatalog.noResults')" :add-label="$t('ui.add_section')" @update:selected="addingType=$event as SectionType" @add="addSection">
      <CatalogEntryPreview v-if="addingType" kind="section" :type="addingType" :title="$t(`sectionCatalog.sections.${addingType}`)" />
    </CatalogPicker>
    <CatalogPicker v-model="showAddBlock" :target="pickerTarget" :title="$t('ui.add_a_block')" v-model:search="blockSearch" :selected="addingBlockType" :categories="availableBlockCategories" :search-label="$t('blockCatalog.search')" :empty-label="$t('blockCatalog.noResults')" :add-label="$t('ui.add_block')" @update:selected="addingBlockType=$event as BlockType" @add="addBlock">
      <CatalogEntryPreview v-if="addingBlockType" kind="block" :type="addingBlockType" :title="$t(`blockCatalog.labels.${addingBlockType}`)" />
    </CatalogPicker>
  </div>
</template>

<style scoped>


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
  gap: 4px;
  padding: 6px 4px;
  border: 1px solid transparent;
  border-radius: 9px;
  cursor: pointer;
}

.section-row.active {
  border-color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.07);
}

.tree-row-action { display: grid; flex-shrink: 0; }
.tree-row-action > * { grid-area: 1 / 1; }
.drag-handle { cursor: grab; opacity: 0; pointer-events: none; }
.tree-visibility { opacity: 1; }
.section-row:hover > .tree-row-action > .drag-handle,
.tree-row-action:has(.drag-handle:focus-visible) > .drag-handle { opacity: 1; pointer-events: auto; }
.section-row:hover > .tree-row-action > .tree-visibility,
.tree-row-action:has(.drag-handle:focus-visible) > .tree-visibility { opacity: 0; pointer-events: none; }
.drag-handle:active { cursor: grabbing; }
@media (hover: none) {
  .tree-row-action { display: flex; }
  .tree-row-action > .drag-handle, .section-row:hover > .tree-row-action > .tree-visibility { opacity: 1; pointer-events: auto; }
}

.section-row__text {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
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
  overflow: clip;
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
.preview-canvas :deep(.builder-section.is-navigation-sticky) {
  position: sticky;
  top: 0;
  z-index: 10;
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
  min-width: 0;
}
.section-insert-button {
  display: flex;
  width: 100%;
  justify-content: flex-start;
  padding-inline: 8px;
  margin-top: 4px;
  color: rgb(var(--v-theme-primary));
}

.section-tree-children {
  margin: 2px 0 8px 34px;
  padding-left: 8px;
  border-left: 1px solid rgba(var(--v-theme-on-surface), 0.16);
}

.section-type-icon {
  color: rgb(var(--v-theme-primary));
  flex-shrink: 0;
}

.section-row__text strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nested-add-block {
  margin-top: 3px;
  width: 100%;
  justify-content: flex-start;
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

<style scoped>
.section-category-title { position: sticky; top: 0; z-index: 1; background: rgb(var(--v-theme-surface)); font-weight: 700; }
</style>
