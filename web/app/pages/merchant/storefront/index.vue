<script setup lang="ts">
import { useStorefrontWriteFlush } from "~/features/storefront-builder/composables/useStorefrontResource";
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";
import {
  themeCatalog,
  createThemeDocument,
} from "~/features/storefront-builder/utils/theme-library";
import ThemeThumbnail from "~/features/storefront-builder/components/ThemeThumbnail.vue";
import type { InstalledTheme, ThemeDocument } from "~/types/theme-library";
import ConfirmDeleteDialog from "~/components/ConfirmDeleteDialog.vue";
definePageMeta({ layout: "merchant", middleware: ["merchant"] });
const api = useApi();
const flushWrites = useStorefrontWriteFlush();
const snackbar = useSnackbarStore();
const { t, te, locale } = useI18n();
useHead({ title: () => t("menu.storefront") });
const previewingPreset = ref<(typeof themeCatalog)[number] | null>(null);
const demoKind = ref("HOME");
const demoDevice = ref("desktop");
const demoLoading = ref(true);
const demoUrl = computed(() =>
  previewingPreset.value
    ? `/merchant/storefront/catalog/${previewingPreset.value.id}?locale=${locale.value}&kind=${demoKind.value}`
    : "",
);
watch(demoUrl, () => {
  demoLoading.value = true;
});
watch(previewingPreset, () => {
  demoKind.value = "HOME";
  demoDevice.value = "desktop";
});
const storefront = useStorefront();
const themes = ref<InstalledTheme[]>([]);
const busy = ref(false);
const failed = ref(false);
const deleting = ref<InstalledTheme | null>(null);
const renaming = ref<InstalledTheme | null>(null);
const newName = ref("");
const importInput = ref<HTMLInputElement>();
const publishing = ref<InstalledTheme | null>(null);
const restoring = ref<{ theme: InstalledTheme; revision: number } | null>(null);
const revisions = ref<InstalledTheme | null>(null);
const current = computed(() => themes.value.find((theme) => theme.active));
const others = computed(() => themes.value.filter((theme) => !theme.active));
async function load() {
  failed.value = false;
  try {
    themes.value = await api<InstalledTheme[]>("/storefront/themes");
  } catch (error) {
    failed.value = true;
    report(error);
  }
}
function report(error: unknown) {
  const data = (error as { data?: { detail?: string; message?: string } }).data;
  const reason = data?.detail ?? data?.message ?? "";
  snackbar.openSnackbar({
    type: "error",
    message: te(reason) ? t(reason) : t("themeLibrary.failed"),
  });
}
async function action(work: () => Promise<unknown>) {
  if (busy.value) return;
  busy.value = true;
  try {
    await flushWrites();
    await work();
    await load();
    storefront.pageStore.initialized.value = false;
    storefront.themeStore.initialized.value = false;
    storefront.menuStore.initialized.value = false;
    snackbar.openSnackbar({
      type: "success",
      message: t("themeLibrary.saved"),
    });
  } catch (error) {
    report(error);
  } finally {
    busy.value = false;
  }
}
async function addTheme(preset: (typeof themeCatalog)[number]) {
  await action(async () => {
    // Read the current store content at install time; custom pages and resource IDs are retained.
    await storefront.load();
    const document = createThemeDocument(
      { theme: storefront.theme.value, pages: storefront.pages.value },
      preset.name,
      locale.value,
    );
    await api("/storefront/themes", {
      method: "POST",
      body: { name: preset.name, source: preset.id, configuration: document },
    });
  });
}
async function publish() {
  const theme = publishing.value;
  publishing.value = null;
  if (!theme) return;
  await action(async () => {
    const draft = await api<ThemeDocument>(`/storefront/themes/${theme.id}`);
    if (!draft.theme?.colors) {
      await api("/storefront/theme", {
        method: "PUT",
        query: { themeId: theme.id },
        body: { configuration: storefront.theme.value },
      });
    }
    await api(`/storefront/themes/${theme.id}/publish`, { method: "POST" });
  });
}
async function remove() {
  const theme = deleting.value;
  deleting.value = null;
  if (theme)
    await action(() =>
      api(`/storefront/themes/${theme.id}`, { method: "DELETE" }),
    );
}
async function rename() {
  const theme = renaming.value;
  if (!theme || !newName.value.trim()) return;
  renaming.value = null;
  await action(() =>
    api(`/storefront/themes/${theme.id}`, {
      method: "PATCH",
      body: { name: newName.value.trim() },
    }),
  );
}
async function rollback() {
  const target = restoring.value;
  restoring.value = null;
  revisions.value = null;
  if (target)
    await action(() =>
      api(`/storefront/themes/${target.theme.id}/rollback/${target.revision}`, {
        method: "POST",
      }),
    );
}
async function exportTheme(theme: InstalledTheme) {
  try {
    await flushWrites();
    const configuration = await api<ThemeDocument>(
      `/storefront/themes/${theme.id}`,
    );
    // A theme package contains design templates, not the store's custom-page content.
    configuration.pages = configuration.pages.filter(
      (page) => page.kind !== "CUSTOM",
    );
    const url = URL.createObjectURL(
      new Blob(
        [
          JSON.stringify(
            {
              schemaVersion: 1,
              version: theme.version,
              name: theme.name,
              configuration,
            },
            null,
            2,
          ),
        ],
        { type: "application/json" },
      ),
    );
    const link = document.createElement("a");
    link.href = url;
    link.download = `${theme.name.replace(/[^a-z0-9_-]/gi, "-") || "theme"}.json`;
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  } catch (error) {
    report(error);
  }
}
async function importTheme(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";
  if (!file) return;
  await action(async () => {
    if (file.size > 4_000_000)
      throw { data: { message: "theme.configurationTooLarge" } };
    let packageData: Record<string, unknown>;
    try {
      packageData = JSON.parse(await file.text());
    } catch {
      throw { data: { message: "theme.invalidConfiguration" } };
    }
    if (!packageData || packageData.schemaVersion !== 1)
      throw { data: { message: "theme.unsupportedVersion" } };
    await api("/storefront/themes", {
      method: "POST",
      body: {
        name: packageData.name,
        source: "import",
        schemaVersion: packageData.schemaVersion,
        configuration: packageData.configuration,
      },
    });
  });
}
const editor = (theme: InstalledTheme) => ({
  path: `/merchant/storefront/pages/${encodeURIComponent(theme.homeSlug || "home")}`,
  query: { themeId: theme.id },
});
const preview = (theme: InstalledTheme) => ({
  path: "/merchant/storefront/preview",
  query: { themeId: theme.id },
});
storefront.pageStore.initialized.value = false;
storefront.themeStore.initialized.value = false;
storefront.menuStore.initialized.value = false;
await storefront.load();
await load();
</script>
<template>
  <v-container class="py-8" style="max-width: 1400px">
    <div class="d-flex align-center justify-space-between flex-wrap ga-3 mb-6">
      <div>
        <h1 class="text-headline-medium">{{ $t("themeLibrary.title") }}</h1>
        <p class="text-medium-emphasis mt-2">{{ $t("themeLibrary.hint") }}</p>
      </div>
      <div class="d-flex ga-2 flex-wrap">
        <v-btn variant="outlined" to="/merchant/customers">{{
          $t("customers.title")
        }}</v-btn>
        <input
          ref="importInput"
          type="file"
          accept="application/json,.json"
          hidden
          @change="importTheme"
        />
        <v-btn
          variant="outlined"
          :disabled="busy"
          prepend-icon="mdi-import"
          @click="importInput?.click()"
          >{{ $t("themeLibrary.import") }}</v-btn
        >
        <v-btn
          variant="outlined"
          to="/merchant/storefront/menus"
          prepend-icon="mdi-menu"
          >{{ $t("ui.menu") }}</v-btn
        >
      </div>
    </div>
    <v-btn v-if="failed" variant="tonal" @click="load">{{
      $t("themeLibrary.retry")
    }}</v-btn>
    <template v-if="themes.length">
      <h2 class="text-title-large mb-4">{{ $t("themeLibrary.current") }}</h2>
      <v-row
        ><v-col
          v-for="theme in current ? [current] : []"
          :key="theme.id"
          cols="12"
        >
          <v-card variant="outlined" rounded="xl" class="theme-current">
            <ThemeThumbnail
              :name="theme.name"
              :source="theme.source"
              :colors="theme.colors"
            />
            <div class="pa-6">
              <div class="d-flex align-center ga-2 mb-2">
                <h3>{{ theme.name }}</h3>
                <v-chip color="success" size="small">{{
                  $t("themeLibrary.live")
                }}</v-chip>
              </div>
              <p class="text-body-small text-medium-emphasis mb-5">
                {{
                  theme.hasChanges
                    ? $t("themeLibrary.unpublished")
                    : $t("themeLibrary.published")
                }}
              </p>
              <div class="d-flex flex-wrap ga-2">
                <v-btn color="primary" :to="editor(theme)">{{
                  $t("themeLibrary.edit")
                }}</v-btn>
                <v-btn variant="outlined" :to="preview(theme)">{{
                  $t("themeLibrary.preview")
                }}</v-btn>
                <v-btn
                  variant="text"
                  :to="{
                    path: '/merchant/storefront/theme',
                    query: { themeId: theme.id },
                  }"
                  >{{ $t("ui.theme_settings") }}</v-btn
                >
                <v-btn
                  :disabled="busy"
                  variant="tonal"
                  @click="publishing = theme"
                  >{{ $t("themeLibrary.publish") }}</v-btn
                >
                <v-menu
                  ><template #activator="{ props }"
                    ><v-btn
                      v-bind="props"
                      icon="mdi-dots-horizontal"
                      variant="text"
                      :aria-label="$t('themeLibrary.actions')"
                  /></template>
                  <v-list>
                    <v-list-item
                      :title="$t('themeLibrary.rename')"
                      @click="
                        renaming = theme;
                        newName = theme.name;
                      "
                    />
                    <v-list-item
                      :disabled="busy"
                      :title="$t('themeLibrary.duplicate')"
                      @click="
                        action(() =>
                          api(`/storefront/themes/${theme.id}/duplicate`, {
                            method: 'POST',
                          }),
                        )
                      "
                    />
                    <v-list-item
                      :title="$t('themeLibrary.history')"
                      @click="revisions = theme"
                    />
                    <v-list-item
                      :disabled="busy"
                      :title="$t('themeLibrary.export')"
                      @click="exportTheme(theme)"
                    />
                  </v-list>
                </v-menu>
              </div>
            </div>
          </v-card> </v-col
      ></v-row>
      <h2 class="text-title-large mt-8 mb-4">
        {{ $t("themeLibrary.library") }}
      </h2>
      <p v-if="!others.length" class="text-medium-emphasis mb-4">
        {{ $t("themeLibrary.empty") }}
      </p>
      <v-row>
        <v-col v-for="theme in others" :key="theme.id" cols="12" sm="6" lg="4">
          <v-card variant="outlined" rounded="xl">
            <ThemeThumbnail
              :name="theme.name"
              :source="theme.source"
              :colors="theme.colors"
            />
            <div class="pa-4">
              <h3>{{ theme.name }}</h3>
              <p class="text-body-small text-medium-emphasis">
                {{ $t("themeLibrary.draft") }} · v{{ theme.version }}
              </p>
            </div>
            <v-card-actions class="flex-wrap pa-4 pt-0">
              <v-btn :to="editor(theme)">{{ $t("themeLibrary.edit") }}</v-btn
              ><v-btn :to="preview(theme)">{{
                $t("themeLibrary.preview")
              }}</v-btn
              ><v-btn
                color="primary"
                :disabled="busy"
                @click="publishing = theme"
                >{{ $t("themeLibrary.publish") }}</v-btn
              >
              <v-menu
                ><template #activator="{ props }"
                  ><v-btn
                    v-bind="props"
                    icon="mdi-dots-horizontal"
                    :aria-label="$t('themeLibrary.actions')" /></template
                ><v-list>
                  <v-list-item
                    :title="$t('ui.theme_settings')"
                    :to="{
                      path: '/merchant/storefront/theme',
                      query: { themeId: theme.id },
                    }"
                  />
                  <v-list-item
                    :title="$t('themeLibrary.rename')"
                    @click="
                      renaming = theme;
                      newName = theme.name;
                    "
                  />
                  <v-list-item
                    :disabled="busy"
                    :title="$t('themeLibrary.duplicate')"
                    @click="
                      action(() =>
                        api(`/storefront/themes/${theme.id}/duplicate`, {
                          method: 'POST',
                        }),
                      )
                    "
                  />
                  <v-list-item
                    :title="$t('themeLibrary.history')"
                    @click="revisions = theme"
                  />
                  <v-list-item
                    :disabled="busy"
                    :title="$t('themeLibrary.export')"
                    @click="exportTheme(theme)"
                  />
                  <v-list-item
                    :disabled="busy"
                    :title="$t('common.remove')"
                    @click="deleting = theme"
                  /> </v-list
              ></v-menu>
            </v-card-actions>
          </v-card>
        </v-col>
      </v-row>
      <h2 class="text-title-large mt-8 mb-4">
        {{ $t("themeLibrary.discover") }}
      </h2>
      <v-row
        ><v-col
          v-for="preset in themeCatalog"
          :key="preset.id"
          cols="12"
          sm="6"
          lg="4"
        >
          <v-card
            variant="outlined"
            rounded="xl"
          >
            <!-- <button
              type="button"
              class="theme-catalog-preview"
              :aria-label="`${$t('themeLibrary.preview')} ${preset.name}`"
              @click="previewingPreset = preset"
            > -->
            <ThemeThumbnail
              :name="preset.name"
              :source="preset.id"
              :colors="preset.colors"
            />
            <!-- <span>{{ $t("themeLibrary.preview") }}</span> -->
            <!-- </button> -->
            <div class="pa-4">
              <h3>{{ preset.name }}</h3>
              <p class="text-medium-emphasis">
                {{ $t(`themeLibrary.catalog.${preset.id}`) }}
              </p>
            </div>
            <v-card-actions class="pa-4 pt-0"
              ><span class="text-body-small"
                >Sewain · {{ $t("themeLibrary.free") }}</span
              ><v-spacer /><v-btn
                variant="text"
                @click="previewingPreset = preset"
                >{{ $t("themeLibrary.preview") }}</v-btn
              ><v-btn
                color="primary"
                variant="tonal"
                :loading="busy"
                @click="addTheme(preset)"
                >{{ $t("themeLibrary.add") }}</v-btn
              ></v-card-actions
            >
          </v-card>
        </v-col></v-row
      >
    </template>
    <ConfirmDeleteDialog
      :model-value="!!deleting"
      :title="$t('themeLibrary.deleteTitle')"
      :message="$t('themeLibrary.deleteHint', { name: deleting?.name })"
      @update:model-value="!$event && (deleting = null)"
      @confirmed="remove"
    />
    <v-dialog
      :model-value="!!previewingPreset"
      fullscreen
      @update:model-value="!$event && (previewingPreset = null)"
    >
      <v-card v-if="previewingPreset" class="theme-demo-dialog">
        <div class="theme-demo-toolbar">
          <v-btn
            icon="mdi-close"
            variant="text"
            :aria-label="$t('common.cancel')"
            @click="previewingPreset = null"
          />
          <strong>{{ previewingPreset.name }}</strong
          ><v-chip size="small">{{ $t("themeLibrary.demo") }}</v-chip>
          <v-select
            v-model="demoKind"
            :items="
              [
                'HOME',
                'PRODUCT',
                'COLLECTIONS',
                'PRODUCT_GRID',
                'CART',
                'CHECKOUT',
              ].map((value) => ({
                value,
                title: $t(`themeLibrary.demoPages.${value}`),
              }))
            "
            hide-details
            density="compact"
            variant="outlined"
            :aria-label="$t('themeLibrary.page')"
            class="theme-demo-page-select"
          />
          <v-btn-toggle v-model="demoDevice" mandatory density="compact"
            ><v-btn
              value="desktop"
              icon="mdi-monitor"
              :aria-label="$t('themeLibrary.desktop')" /><v-btn
              value="mobile"
              icon="mdi-cellphone"
              :aria-label="$t('themeLibrary.mobile')"
          /></v-btn-toggle>
          <v-spacer /><v-btn
            color="primary"
            variant="flat"
            :loading="busy"
            @click="
              addTheme(previewingPreset);
              previewingPreset = null;
            "
            >{{ $t("themeLibrary.add") }}</v-btn
          >
        </div>
        <p class="theme-demo-notice">{{ $t("themeLibrary.demoHint") }}</p>
        <div class="theme-demo-viewport">
          <v-progress-circular
            v-if="demoLoading"
            indeterminate
            class="theme-demo-loading"
            color="primary"
          /><iframe
            @load="demoLoading = false"
            :key="demoUrl"
            :src="demoUrl"
            :title="`${$t('themeLibrary.preview')} ${previewingPreset.name}`"
            :class="{ 'theme-demo-iframe--mobile': demoDevice === 'mobile' }"
            class="theme-demo-iframe"
          />
        </div>
      </v-card>
    </v-dialog>
    <v-dialog
      :model-value="!!renaming"
      max-width="420"
      @update:model-value="!$event && (renaming = null)"
      ><v-card :title="$t('themeLibrary.rename')"
        ><v-card-text
          ><v-text-field
            v-model="newName"
            :label="$t('themeLibrary.name')"
            maxlength="100"
            @keyup.enter="rename" /></v-card-text
        ><v-card-actions
          ><v-btn @click="renaming = null">{{ $t("common.cancel") }}</v-btn
          ><v-btn :disabled="!newName.trim() || busy" @click="rename">{{
            $t("common.save")
          }}</v-btn></v-card-actions
        ></v-card
      ></v-dialog
    >
    <v-dialog
      :model-value="!!publishing"
      max-width="460"
      @update:model-value="!$event && (publishing = null)"
      ><v-card
        :title="$t('themeLibrary.publish')"
        :text="$t('themeLibrary.publishHint', { name: publishing?.name })"
        ><v-card-actions
          ><v-btn @click="publishing = null">{{ $t("common.cancel") }}</v-btn
          ><v-btn color="primary" :disabled="busy" @click="publish">{{
            $t("themeLibrary.publish")
          }}</v-btn></v-card-actions
        ></v-card
      ></v-dialog
    >
    <v-dialog
      :model-value="!!revisions"
      max-width="500"
      scrollable
      @update:model-value="!$event && (revisions = null)"
      ><v-card :title="$t('themeLibrary.history')"
        ><v-card-text
          ><p v-if="!revisions?.history.length">
            {{ $t("themeLibrary.noHistory") }}
          </p>
          <v-list
            ><v-list-item
              v-for="entry in [...(revisions?.history ?? [])].reverse()"
              :key="entry.revision"
              :title="`#${entry.revision}`"
              :subtitle="entry.createdAt"
              ><template #append
                ><v-btn
                  :disabled="busy"
                  @click="
                    restoring = { theme: revisions!, revision: entry.revision }
                  "
                  >{{ $t("themeLibrary.restore") }}</v-btn
                ></template
              ></v-list-item
            ></v-list
          ></v-card-text
        ><v-card-actions
          ><v-btn @click="revisions = null">{{
            $t("themeLibrary.close")
          }}</v-btn></v-card-actions
        ></v-card
      ></v-dialog
    >
    <v-dialog
      :model-value="!!restoring"
      max-width="460"
      @update:model-value="!$event && (restoring = null)"
      ><v-card
        :title="$t('themeLibrary.restore')"
        :text="$t('themeLibrary.restoreHint')"
        ><v-card-actions
          ><v-btn @click="restoring = null">{{ $t("common.cancel") }}</v-btn
          ><v-btn color="primary" :disabled="busy" @click="rollback">{{
            $t("themeLibrary.restore")
          }}</v-btn></v-card-actions
        ></v-card
      ></v-dialog
    >
  </v-container>
</template>
<style scoped>
.theme-catalog-preview {
  display: block;
  position: relative;
  width: 100%;
  border-radius: inherit;
  text-align: inherit;
  overflow: hidden;
}
.theme-catalog-preview > span {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: #0005;
  color: white;
  opacity: 0;
  transition: opacity 0.15s;
}
.theme-catalog-preview:hover > span,
.theme-catalog-preview:focus-visible > span {
  opacity: 1;
}
.theme-demo-dialog {
  height: 100dvh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.theme-demo-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px;
  flex: none;
}
.theme-demo-page-select {
  max-width: 220px;
  min-width: 160px;
}
.theme-demo-notice {
  padding: 0 20px 12px;
  font-size: 13px;
  opacity: 0.7;
  flex: none;
}
.theme-demo-viewport {
  position: relative;
  display: flex;
  justify-content: center;
  flex: 1;
  min-height: 0;
  background: rgba(var(--v-theme-on-surface), 0.06);
  overflow: hidden;
  padding: 12px;
}
.theme-demo-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
.theme-demo-iframe {
  height: 100%;
  width: 100%;
  border: 0;
  background: white;
  min-width: 0;
}
.theme-demo-iframe--mobile {
  max-width: 390px;
  border-radius: 16px;
}

.theme-current {
  display: grid;
  grid-template-columns: minmax(240px, 400px) 1fr;
  align-items: center;
}
@media (max-width: 700px) {
  .theme-current {
    grid-template-columns: 1fr;
  }
}
</style>
