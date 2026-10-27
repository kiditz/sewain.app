<script setup lang="ts">
import type { ComponentPublicInstance } from "vue";
import {
  CloudUploadOutlined,
  DeleteOutlined,
  FileImageOutlined,
  CloseOutlined,
  SearchOutlined,
  CheckOutlined,
} from "@ant-design/icons-vue";

import type { Media } from "~/types/media";
import { useSnackbarStore } from "~/stores/snackbar";
const { t } = useI18n();
const snackbar = useSnackbarStore();
const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    multiple?: boolean;
  }>(),
  {
    multiple: false,
  },
);

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  selected: [media: Media | Media[]];
}>();

const { uploadMedia, getMedia, updateMediaFilename, deleteMedia } = useMedia();
const libraryScrollbar = ref<ComponentPublicInstance | null>(null);

const activeTab = ref("library");

const files = ref<File[]>([]);
const uploading = ref(false);
const loading = ref(false);

const media = ref<Media[]>([]);
const selectedMedia = ref<Media[]>([]);
const activeMedia = ref<Media | null>(null);
const editedFilename = ref("");
const savingFilename = ref(false);
const deletingMedia = ref(false);
const showDeleteConfirmation = ref(false);

const search = ref("");
const page = ref(1);
const pageSize = ref(24);
const total = ref(0);

const pageCount = computed(() =>
  Math.max(1, Math.ceil(total.value / pageSize.value)),
);

const hasFiles = computed(() => files.value.length > 0);
const uploadError = ref<string | null>(null);
const clearFiles = () => {
  files.value = [];
};

const upload = async () => {
  if (!hasFiles.value || uploading.value) {
    return;
  }

  uploading.value = true;
  uploadError.value = null;
  try {
    await uploadMedia(files.value);

    clearFiles();

    activeTab.value = "library";
    page.value = 1;

    await loadMedia();
  } catch (error: any) {
    if (error?.response?.status === 413) {
      uploadError.value = t("media.error.upload");
    } else {
      uploadError.value = t("media.error.permission");
    }
  } finally {
    uploading.value = false;
  }
};

const loadMedia = async (requestedPage = page.value) => {
  loading.value = true;

  try {
    const response = await getMedia({
      page: requestedPage - 1,
      size: pageSize.value,
      filename: search.value || undefined,
    });

    media.value = [...response.items];
    total.value = response.totalItems;
    page.value = requestedPage;
  } finally {
    loading.value = false;
  }
};

const changePage = async (requestedPage: number) => {
  if (requestedPage < 1 || requestedPage > pageCount.value || loading.value) return;
  await loadMedia(requestedPage);
  await nextTick();
  await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));

  const scrollbarRoot = libraryScrollbar.value?.$el as HTMLElement | undefined;
  const scrollElement = scrollbarRoot?.querySelector<HTMLElement>(".ps") ?? scrollbarRoot;
  if (!scrollElement) return;

  scrollElement.scrollTop = 0;
  scrollElement.scrollTo({ top: 0, behavior: "smooth" });
};

const searchMedia = async () => {
  await loadMedia(1);
};

const clearSearch = async () => {
  search.value = "";
  await searchMedia();
};

const toggleSelection = (item: Media) => {
  activeMedia.value = item;
  editedFilename.value = item.filename;
  if (!props.multiple) {
    selectedMedia.value = [item];
    return;
  }

  const index = selectedMedia.value.findIndex((media) => media.id === item.id);

  if (index === -1) {
    selectedMedia.value.push(item);
  } else {
    selectedMedia.value.splice(index, 1);
  }
};

const saveFilename = async () => {
  if (!activeMedia.value || savingFilename.value || !editedFilename.value.trim()) return;
  savingFilename.value = true;
  try {
    const updated = await updateMediaFilename(
      activeMedia.value.id,
      editedFilename.value.trim(),
    );
    media.value = media.value.map((item) =>
      item.id === updated.id ? updated : item,
    );
    selectedMedia.value = selectedMedia.value.map((item) =>
      item.id === updated.id ? updated : item,
    );
    activeMedia.value = updated;
    editedFilename.value = updated.filename;
    snackbar.openSnackbar({
      message: t("media.details.filename_saved"),
      type: "success",
    });
  } catch {
    snackbar.openSnackbar({
      message: t("media.details.filename_save_error"),
      type: "error",
    });
  } finally {
    savingFilename.value = false;
  }
};

const confirmDeleteMedia = async () => {
  if (!activeMedia.value || deletingMedia.value) return;
  deletingMedia.value = true;
  try {
    await deleteMedia(activeMedia.value.id);
    const deletedId = activeMedia.value.id;
    selectedMedia.value = selectedMedia.value.filter((item) => item.id !== deletedId);
    activeMedia.value = null;
    editedFilename.value = "";
    showDeleteConfirmation.value = false;
    const nextTotal = Math.max(0, total.value - 1);
    total.value = nextTotal;
    const lastPage = Math.max(1, Math.ceil(nextTotal / pageSize.value));
    await loadMedia(Math.min(page.value, lastPage));
    snackbar.openSnackbar({
      message: t("media.details.deleted"),
      type: "success",
    });
  } catch {
    snackbar.openSnackbar({
      message: t("media.details.delete_error"),
      type: "error",
    });
  } finally {
    deletingMedia.value = false;
  }
};

const isSelected = (item: Media) => {
  return selectedMedia.value.some((media) => media.id === item.id);
};

const confirmSelection = () => {
  if (selectedMedia.value.length === 0) {
    return;
  }

  const selected = props.multiple
    ? [...selectedMedia.value]
    : selectedMedia.value[0];

  if (!selected) {
    return;
  }

  emit("selected", selected);

  close();
};

const close = () => {
  emit("update:modelValue", false);
};

const formatFileSize = (size: number) => {
  if (size < 1024) {
    return `${size} B`;
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }

  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) {
      return;
    }

    selectedMedia.value = [];
    activeMedia.value = null;
    editedFilename.value = "";
    activeTab.value = "library";
    page.value = 1;

    await loadMedia();
  },
);
</script>

<template>
  <v-dialog
    :model-value="modelValue"
    max-width="1280"
    scrollable
    @update:model-value="emit('update:modelValue', $event)"
  >
    <v-card>
      <!-- Header -->
      <v-card-title class="d-flex align-center justify-space-between pa-5">
        <div class="text-title-medium">
          {{ t("media.title") }}
        </div>

        <v-btn icon variant="text" @click="close">
          <CloseOutlined />
        </v-btn>
      </v-card-title>

      <v-divider />

      <!-- Tabs -->
      <v-tabs v-model="activeTab" grow>
        <v-tab value="library">
          <FileImageOutlined class="mr-2" />
          {{ t("media.tabs.library") }}
        </v-tab>

        <v-tab value="upload">
          <CloudUploadOutlined class="mr-2" />
          {{ t("media.tabs.upload") }}
        </v-tab>
      </v-tabs>

      <v-divider />

      <nuxt-scrollbar ref="libraryScrollbar" style="max-height: 72vh">
        <v-card-text class="pa-5">
          <v-window v-model="activeTab">
            <!-- ========================= -->
            <!-- Media Library -->
            <!-- ========================= -->
            <v-window-item value="library">
              <v-row class="media-library-layout">
                <v-col cols="12" md="8" class="media-library-main">
                  <div class="d-flex align-center mb-5">
                    <v-text-field
                      v-model="search"
                      :placeholder="t('media.library.search')"
                      variant="outlined"
                      density="compact"
                      hide-details
                      clearable
                      @keyup.enter="searchMedia"
                      @click:clear="clearSearch"
                    >
                      <template #prepend-inner>
                        <SearchOutlined />
                      </template>
                    </v-text-field>

                    <v-btn
                      color="primary"
                      class="ml-3"
                      @click="activeTab = 'upload'"
                    >
                      <CloudUploadOutlined class="mr-2" />
                      {{ t("media.upload.button") }}
                    </v-btn>
                  </div>

                  <div v-if="loading" class="d-flex justify-center py-12">
                    <v-progress-circular indeterminate />
                  </div>

                  <div v-else-if="media.length === 0" class="media-empty">
                    <FileImageOutlined class="media-empty-icon" />
                    <div class="text-title-medium mt-3">
                      {{ t("media.library.empty") }}
                    </div>
                    <v-btn
                      class="mt-4"
                      variant="outlined"
                      @click="activeTab = 'upload'"
                    >
                      {{ t("media.upload.button") }}
                    </v-btn>
                  </div>

                  <v-row v-else>
                    <v-col
                      v-for="item in media"
                      :key="item.id"
                      cols="6"
                      sm="4"
                      lg="3"
                    >
                      <v-card
                        variant="outlined"
                        class="media-card"
                        :class="{
                          'media-card-selected': isSelected(item),
                          'media-card-active': activeMedia?.id === item.id,
                        }"
                        @click="toggleSelection(item)"
                      >
                        <div class="media-image">
                          <v-img :src="item.url" height="140" cover />
                          <div
                            v-if="isSelected(item)"
                            class="media-selected-overlay"
                          >
                            <CheckOutlined class="media-check-icon" />
                          </div>
                        </div>
                        <v-card-text class="pa-2">
                          <div class="text-body-medium text-truncate">
                            {{ item.filename }}
                          </div>
                          <div class="text-body-small text-medium-emphasis">
                            {{ formatFileSize(item.size) }}
                          </div>
                        </v-card-text>
                      </v-card>
                    </v-col>
                  </v-row>

                  <div
                    v-if="!loading && pageCount > 1"
                    class="d-flex align-center justify-center mt-5"
                  >
                    <v-pagination
                      :model-value="page"
                      :length="pageCount"
                      :total-visible="5"
                      :disabled="loading"
                      @update:model-value="changePage"
                    />
                  </div>
                </v-col>

                <v-col cols="12" md="4">
                  <v-card variant="outlined" class="media-details h-100">
                    <template v-if="activeMedia">
                      <v-img
                        :src="activeMedia.url"
                        height="210"
                        contain
                        class="media-details-preview"
                      />
                      <v-card-text>
                        <div class="text-title-medium mb-1">{{ t("media.details.title") }}</div>
                        <div class="text-body-small text-medium-emphasis mb-4">
                          {{ formatFileSize(activeMedia.size) }} ·
                          {{ activeMedia.contentType }}
                        </div>
                        <v-text-field
                          v-model="editedFilename"
                          :label="t('media.details.filename')"
                          variant="outlined"
                          density="compact"
                          hide-details
                          maxlength="255"
                        />
                        <v-btn
                          block
                          color="primary"
                          class="mt-3"
                          :loading="savingFilename"
                          :disabled="
                            !editedFilename.trim() ||
                            editedFilename.trim() === activeMedia.filename
                          "
                          @click="saveFilename"
                        >
                          {{ t("common.save") }}
                        </v-btn>
                        <v-divider class="my-4" />
                        <v-btn
                          block
                          color="error"
                          variant="outlined"
                          :loading="deletingMedia"
                          @click="showDeleteConfirmation = true"
                        >
                          <DeleteOutlined class="mr-2" />
                          {{ t("media.details.delete") }}
                        </v-btn>
                      </v-card-text>
                    </template>
                    <div v-else class="media-details-empty">
                      <FileImageOutlined class="media-empty-icon" />
                      <span>{{ t("media.details.select_hint") }}</span>
                    </div>
                  </v-card>
                </v-col>
              </v-row>
            </v-window-item>

            <!-- ========================= -->
            <!-- Upload -->
            <!-- ========================= -->
            <v-window-item value="upload">
              <v-file-upload
                :loading="uploading"
                v-model="files"
                accept="image/*"
                multiple
                clearable
                show-size
                :error="uploadError != null"
                :error-messages="uploadError"
                
                :title="$t('ui.drop_or_paste_images_here')"
                browse-text="click to browse"
                inset-file-list
              />
              <!-- Selected files -->
              <div v-if="files.length" class="mt-5">
                <div class="d-flex align-center justify-space-between mb-3">
                  <div class="text-title-medium">
                    {{
                      t("media.upload.selected", {
                        count: files.length,
                      })
                    }}
                  </div>

                  <v-btn variant="text" size="small" @click="clearFiles">
                    {{ t("common.clear") }}
                  </v-btn>
                </div>
              </div>
            </v-window-item>
          </v-window>
        </v-card-text>
      </nuxt-scrollbar>

      <v-divider />

      <!-- Footer -->
      <v-card-actions class="pa-4">
        <v-spacer />

        <v-btn variant="text" @click="close">
          {{ t("common.cancel") }}
        </v-btn>
        <v-btn
          v-if="files.length > 0 && activeTab === 'upload'"
          color="primary"
          :loading="uploading"
          :disabled="uploading"
          @click="upload"
        >
          <CloudUploadOutlined class="mr-2" />
          {{ t("media.upload.button") }}
        </v-btn>
        <v-btn
          v-if="activeTab === 'library'"
          color="primary"
          :disabled="selectedMedia.length === 0"
          @click="confirmSelection"
        >
          {{ t("common.select") }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>

  <v-dialog v-model="showDeleteConfirmation" max-width="420">
    <v-card>
      <v-card-title>{{ t("media.details.confirm_delete_title") }}</v-card-title>
      <v-card-text>{{ t("media.details.confirm_delete_message") }}</v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn variant="text" @click="showDeleteConfirmation = false">
          {{ t("common.cancel") }}
        </v-btn>
        <v-btn
          color="error"
          :loading="deletingMedia"
          @click="confirmDeleteMedia"
        >
          {{ t("media.details.delete") }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.media-upload-zone {
  min-height: 280px;
  border: 2px dashed rgba(var(--v-theme-on-surface), 0.2);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.media-upload-zone:hover {
  border-color: rgb(var(--v-theme-primary));
}

.media-upload-icon {
  font-size: 48px;
  opacity: 0.6;
}

.media-card {
  cursor: pointer;
  overflow: hidden;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.media-card:hover {
  border-color: rgb(var(--v-theme-primary));
}

.media-card-selected {
  border: 2px solid rgb(var(--v-theme-primary));
}

.media-card-active {
  box-shadow: 0 0 0 2px rgba(var(--v-theme-primary), 0.28);
}

.media-image {
  position: relative;
}

.media-selected-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.35);
}

.media-check-icon {
  color: white;
  font-size: 32px;
}

.media-preview {
  position: relative;
  height: 140px;
  overflow: hidden;
}

.media-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-remove-button {
  position: absolute;
  top: 8px;
  right: 8px;
}

.media-empty {
  min-height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.media-library-layout {
  min-height: 460px;
}

.media-library-main {
  min-width: 0;
}

.media-details {
  min-height: 440px;
}

.media-details-preview {
  background: rgba(var(--v-theme-on-surface), 0.04);
}

.media-details-empty {
  display: flex;
  min-height: 440px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: rgba(var(--v-theme-on-surface), 0.6);
  text-align: center;
}

.media-empty-icon {
  font-size: 56px;
  opacity: 0.35;
}
</style>
