<script setup lang="ts">
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";
import { storefrontPresets, presetColors, setStorefrontThemeMode, applyStorefrontPreset } from "~/features/storefront-builder/utils/storefront-theme";
import type { StorefrontThemeMode } from "~/types/storefront";
const storefront = useStorefront();
storefront.pageStore.initialized.value = false;
storefront.themeStore.initialized.value = false;
storefront.menuStore.initialized.value = false;
await storefront.load();
const { theme } = storefront;
const { saving, saveError, hasUnsavedChanges } = storefront.themeStore;
definePageMeta({
  key: route => route.fullPath,
  layout: "merchant",
  middleware: ["merchant"],
});
watch(theme, () => storefront.themeStore.scheduleSave(), { deep: true });
const mode = computed<StorefrontThemeMode>(() => theme.value.mode === "dark" ? "dark" : "light");
const presets = computed(() => storefrontPresets.map(name => ({ name, ...presetColors(name, mode.value) })));
function changeMode(value: StorefrontThemeMode) {
  if (value === "light" || value === "dark") setStorefrontThemeMode(theme.value, value);
}
function applyPreset(preset: { name: string }) {
  applyStorefrontPreset(theme.value, preset.name);
}
</script>
<template>
  <v-container style="max-width: 1000px" class="py-8"
    ><div class="d-flex align-center ga-3 mb-8">
      <v-btn icon="mdi-arrow-left" variant="text" to="/merchant/storefront" />
      <div>
        <div class="text-overline text-medium-emphasis">{{ $t("ui.storefront") }}</div>
        <h1 class="text-headline-medium font-weight-bold">{{ $t("ui.theme_settings") }}</h1>
        <p class="text-body-medium text-medium-emphasis mt-2">
          {{ $t("ui.customize_the_visual_identity_of_your_store") }}
        </p>
      </div>
    </div>
    <div class="text-body-small text-medium-emphasis mb-4" aria-live="polite">
      {{ saving ? "Menyimpan tema…" : hasUnsavedChanges ? "Perubahan belum tersimpan" : "Tema tersimpan" }}
    </div>
    <v-alert v-if="saveError" type="error" class="mb-4">
      Tema gagal disimpan.
      <v-btn variant="text" @click="storefront.themeStore.scheduleSave()">Coba lagi</v-btn>
    </v-alert>
    <v-row
      ><v-col cols="12" md="7"
        ><v-card border flat rounded="lg" class="pa-5 mb-5">
          <h2 class="text-title-medium mb-3">{{ $t('storefrontTheme.mode') }}</h2>
          <v-btn-toggle :model-value="mode" mandatory divided color="primary" :aria-label="$t('storefrontTheme.mode')" @update:model-value="changeMode">
            <v-btn value="light" prepend-icon="mdi-white-balance-sunny">{{ $t('storefrontTheme.light') }}</v-btn>
            <v-btn value="dark" prepend-icon="mdi-weather-night">{{ $t('storefrontTheme.dark') }}</v-btn>
          </v-btn-toggle>
          <p class="text-body-small text-medium-emphasis mt-3">{{ $t('storefrontTheme.modeHint') }}</p>
        </v-card><v-card border flat rounded="lg" class="pa-5 mb-5"
          ><h2 class="text-title-medium mb-4">{{ $t("ui.theme_presets") }}</h2>
          <v-row
            ><v-col
              v-for="preset in presets"
              :key="preset.name"
              cols="12"
              sm="4"
              ><button
                class="preset-card"
                :class="{
                  selected: theme.preset === preset.name.toLowerCase(),
                }"
                @click="applyPreset(preset)"
              >
                <div
                  class="preset-swatch"
                  :style="{ background: preset.background }"
                >
                  <span :style="{ background: preset.primary }" /><span
                    :style="{ background: preset.secondary }"
                  /><span
                    :style="{ background: preset.primary, opacity: 0.35 }"
                  />
                </div>
                <strong>{{ preset.name }}</strong
                ><v-icon
                  v-if="theme.preset === preset.name.toLowerCase()"
                  color="primary"
                  size="small"
                  >mdi-check-circle</v-icon
                >
              </button></v-col
            ></v-row
          ></v-card
        ><v-card border flat rounded="lg" class="pa-5"
          ><h2 class="text-title-medium mb-4">{{ $t("ui.colors") }}</h2>
          <v-row
            ><v-col cols="12" sm="6"
              ><label>{{ $t("merchant.name") }}</label
              ><v-text-field
                v-model="theme.name"
                variant="outlined"
                
                class="mt-2" /></v-col
            ><v-col
              v-for="(color, key) in theme.colors"
              :key="key"
              cols="12"
              sm="6"
              ><label class="text-capitalize">{{ key }} color</label>
              <div class="d-flex align-center ga-2 mt-2">
                <input
                  v-model="theme.colors[key as keyof typeof theme.colors]"
                  type="color"
                  class="color-input"
                /><v-text-field
                  v-model="theme.colors[key as keyof typeof theme.colors]"
                  variant="outlined"
                  
                  hide-details
                /></div></v-col></v-row></v-card></v-col
      ><v-col cols="12" md="5"
        ><v-card border flat rounded="lg" class="pa-5"
          ><h2 class="text-title-medium mb-4">{{ $t("ui.typography_layout") }}</h2>
          <label>{{ $t("ui.body_font") }}</label
          ><v-select
            v-model="theme.typography.bodyFont"
            :items="[
              { title: 'Inter', value: 'Inter, Arial, sans-serif' },
              { title: 'Arial', value: 'Arial, sans-serif' },
              { title: 'Georgia', value: 'Georgia, serif' },
            ]"
            item-title="title"
            item-value="value"
            variant="outlined"
            
            class="mt-2"
          /><label>{{ $t("ui.heading_font") }}</label
          ><v-select
            v-model="theme.typography.headingFont"
            :items="[
              { title: 'Georgia', value: 'Georgia, serif' },
              { title: 'Arial', value: 'Arial, sans-serif' },
              { title: 'Inter', value: 'Inter, Arial, sans-serif' },
            ]"
            item-title="title"
            item-value="value"
            variant="outlined"
            
            class="mt-2"
          /><label>{{ $t("ui.base_font_size") }} {{ theme.typography.baseFontSize }}px</label
          ><v-slider
            v-model="theme.typography.baseFontSize"
            min="14"
            max="20"
            step="1"
            thumb-label
            class="mt-4"
          /><label
            >{{ $t("ui.content_max_width") }} {{ theme.layout.contentMaxWidth }}px</label
          ><v-slider
            v-model="theme.layout.contentMaxWidth"
            min="800"
            max="1440"
            step="40"
            thumb-label
            class="mt-4"
          /><label>{{ $t("ui.section_spacing") }} {{ theme.layout.sectionSpacing }}px</label
          ><v-slider
            v-model="theme.layout.sectionSpacing"
            min="24"
            max="120"
            step="8"
            thumb-label
            class="mt-4"
          /><label>{{ $t("ui.corner_radius") }} {{ theme.layout.borderRadius }}px</label
          ><v-slider
            v-model="theme.layout.borderRadius"
            min="0"
            max="32"
            step="2"
            thumb-label
            class="mt-4"
          /><v-btn block color="primary" prepend-icon="mdi-eye-outline" to="/merchant/storefront/pages/home"
            >{{ $t("ui.preview_storefront") }}</v-btn
          ></v-card
        ></v-col
      ></v-row
    ></v-container
  >
</template>
<style scoped>
.preset-card {
  position: relative;
  width: 100%;
  text-align: left;
  padding: 8px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  background: transparent;
  cursor: pointer;
}
.preset-card.selected {
  border: 2px solid rgb(var(--v-theme-primary));
  padding: 7px;
}
.preset-card strong {
  display: inline-block;
  padding: 8px 3px 2px;
}
.preset-card > i {
  float: right;
  margin-top: 7px;
}
.preset-swatch {
  height: 88px;
  border-radius: 7px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.preset-swatch span {
  display: block;
  height: 10px;
  border-radius: 3px;
}
.preset-swatch span:first-child {
  width: 70%;
}
.preset-swatch span:nth-child(2) {
  width: 90%;
  height: 20px;
}
.preset-swatch span:nth-child(3) {
  width: 55%;
}
.color-input {
  width: 48px;
  height: 42px;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 3px;
  background: white;
}
label {
  font-size: 13px;
  font-weight: 500;
  color: rgba(var(--v-theme-on-surface), 0.8);
}
</style>
