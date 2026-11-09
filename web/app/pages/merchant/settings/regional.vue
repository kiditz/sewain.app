<script setup lang="ts">
import type { Country } from "~/composables/useCountry";

const { t, locale, locales, setLocale } = useI18n();
useHead({ title: () => t("menu.regionalSettings") });
definePageMeta({
  layout: "merchant",
  middleware: ["merchant"]
});

const { findCountries } = useCountry();
const { update, create, findByKey, regional } = useSettings();

const {
  data: countries,
  pending,
  error,
  refresh,
} = await useAsyncData("regional-countries", () => findCountries());

const settings = reactive({
  countryId: null as number | null,
  currency: "",
  currencyPosition: "before",
  timezone: "",
  locale: locale.value,
  dateFormat: "DD/MM/YYYY",
  firstDayOfWeek: "monday",
});

const loadingSettings = ref(false);
const saving = ref(false);
const saved = ref(false);
const saveError = ref("");

const selectedCountry = computed<Country | null>(
  () =>
    countries.value?.find((country) => country.id === settings.countryId) ??
    null,
);

const currencyItems = computed(() => {
  const unique = new Map<string, { title: string; value: string }>();

  for (const country of countries.value ?? []) {
    if (country.currency) {
      unique.set(country.currency, {
        title: `${country.currency} — ${country.currencyName}`,
        value: country.currency,
      });
    }
  }

  return [...unique.values()].sort((a, b) => a.value.localeCompare(b.value));
});

const timezoneItems = computed(() =>
  (selectedCountry.value?.timezones ?? []).map((timezone) => ({
    title: `${timezone.zoneName} (${timezone.gmtOffsetName})`,
    value: timezone.zoneName,
  })),
);

const dateFormats = computed(() => {
  const sampleDate = new Date(2026, 9, 12, 12, 0, 0);
  const locale = settings.locale || "id";
  const timezone = settings.timezone || "Asia/Jakarta";

  const options: {
    value: string;
    intl: Intl.DateTimeFormatOptions;
  }[] = [
    {
      value: "DD/MM/YYYY",
      intl: { day: "2-digit", month: "2-digit", year: "numeric" },
    },
    {
      value: "MM/DD/YYYY",
      intl: { month: "2-digit", day: "2-digit", year: "numeric" },
    },
    {
      value: "YYYY-MM-DD",
      intl: { year: "numeric", month: "2-digit", day: "2-digit" },
    },
    {
      value: "DD MMM YYYY",
      intl: { day: "numeric", month: "short", year: "numeric" },
    },
    {
      value: "DD MMMM YYYY",
      intl: { day: "numeric", month: "long", year: "numeric" },
    },
    {
      value: "ddd, DD MMM YYYY",
      intl: {
        weekday: "short",
        day: "numeric",
        month: "short",
        year: "numeric",
      },
    },
    {
      value: "dddd, DD MMMM YYYY",
      intl: {
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric",
      },
    },
  ];

  return options.map(({ value, intl }) => ({
    title: new Intl.DateTimeFormat(locale, {
      ...intl,
      timeZone: timezone,
    }).format(sampleDate),
    value,
  }));
});

const datePreview = computed(() => {
  const sampleDate = new Date(2026, 11, 31, 12, 0, 0);
  const locale = settings.locale || "id";
  const timezone = settings.timezone || "Asia/Jakarta";

  const parts = new Intl.DateTimeFormat(locale, {
    timeZone: timezone,
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).formatToParts(sampleDate);

  const values = Object.fromEntries(
    parts.map(({ type, value }) => [type, value]),
  );

  const day = String(Number(values.day));
  const monthShort = new Intl.DateTimeFormat(locale, {
    timeZone: timezone,
    month: "short",
  }).format(sampleDate);
  const monthLong = new Intl.DateTimeFormat(locale, {
    timeZone: timezone,
    month: "long",
  }).format(sampleDate);
  const weekdayShort = new Intl.DateTimeFormat(locale, {
    timeZone: timezone,
    weekday: "short",
  }).format(sampleDate);
  const weekdayLong = new Intl.DateTimeFormat(locale, {
    timeZone: timezone,
    weekday: "long",
  }).format(sampleDate);

  const formats: Record<string, string> = {
    "DD/MM/YYYY": `${values.day}/${values.month}/${values.year}`,
    "MM/DD/YYYY": `${values.month}/${values.day}/${values.year}`,
    "YYYY-MM-DD": `${values.year}-${values.month}-${values.day}`,
    "DD MMM YYYY": `${day} ${monthShort} ${values.year}`,
    "DD MMMM YYYY": `${day} ${monthLong} ${values.year}`,
    "ddd, DD MMM YYYY": `${weekdayShort}, ${day} ${monthShort} ${values.year}`,
    "dddd, DD MMMM YYYY": `${weekdayLong}, ${day} ${monthLong} ${values.year}`,
  };

  return {
    formatted: formats[settings.dateFormat] ?? "",
    long: new Intl.DateTimeFormat(locale, {
      timeZone: timezone,
      dateStyle: "full",
    }).format(sampleDate),
  };
});
function isNotFound(error: unknown) {
  if (!error || typeof error !== "object") return false;

  const err = error as {
    status?: number;
    statusCode?: number;
    response?: { status?: number; statusCode?: number };
    data?: { statusCode?: number };
  };

  return [
    err.status,
    err.statusCode,
    err.response?.status,
    err.response?.statusCode,
    err.data?.statusCode,
  ].includes(404);
}

async function getRegionalSetting() {
  try {
    return await findByKey("REGIONAL");
  } catch (error) {
    if (isNotFound(error)) return null;
    throw error;
  }
}

function applySettings(value: unknown) {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return;
  }

  const regional = value as Record<string, unknown>;

  settings.countryId =
    typeof regional.countryId === "number" ? regional.countryId : null;
  settings.currency =
    typeof regional.currency === "string" ? regional.currency : "";
  settings.currencyPosition =
    regional.currencyPosition === "after" ? "after" : "before";
  settings.timezone =
    typeof regional.timezone === "string" ? regional.timezone : "";
  settings.locale =
    locales.value.find(({ code }) => code === regional.locale)?.code ??
    locale.value;
  settings.dateFormat =
    typeof regional.dateFormat === "string"
      ? regional.dateFormat
      : "DD/MM/YYYY";
  settings.firstDayOfWeek =
    regional.firstDayOfWeek === "sunday" ? "sunday" : "monday";
}

async function loadSettings() {
  loadingSettings.value = true;
  saveError.value = "";

  try {
    const existing = await getRegionalSetting();

    if (existing) {
      applySettings(existing.value);
    }
  } catch {
    saveError.value = "Failed to load regional settings.";
  } finally {
    loadingSettings.value = false;
  }
}

await loadSettings();

watch(selectedCountry, (country, previousCountry) => {
  if (!country) {
    settings.currency = "";
    settings.timezone = "";
    return;
  }

  if (!previousCountry || country.id !== previousCountry.id) {
    settings.currency = country.currency ?? "";
    settings.timezone = country.timezones?.[0]?.zoneName ?? "";
  }
});


function markChanged() {
  saved.value = false;
  saveError.value = "";
}

async function updateLocale(value: string | null) {
  if (!value) return;

  markChanged();

  const localeCode = locales.value.find(({ code }) => code === value)?.code;
  if (localeCode) await setLocale(localeCode);
}

async function saveSettings() {
  if (saving.value) return;

  saving.value = true;
  saved.value = false;
  saveError.value = "";

  const command = {
    key: "REGIONAL",
    value: {
      countryId: settings.countryId,
      currency: settings.currency,
      currencyPosition: settings.currencyPosition,
      timezone: settings.timezone,
      locale: settings.locale,
      dateFormat: settings.dateFormat,
      firstDayOfWeek: settings.firstDayOfWeek,
    },
  };

  try {
    const existing = await getRegionalSetting();

    if (existing) {
      await update(existing.id, command);
    } else {
      await create(command);
    }

    saved.value = true;
  } catch {
    saveError.value = "Failed to save regional settings. Please try again.";
  } finally {
    saving.value = false;
  }
}

function resetSettings() {
  settings.countryId = null;
  settings.currency = "";
  settings.currencyPosition = "before";
  settings.timezone = "";
  settings.locale = "id";
  settings.dateFormat = "DD/MM/YYYY";
  settings.firstDayOfWeek = "monday";
  markChanged();
}
const formatCurrencyPreview = (position: "before" | "after"): string => {
  const locale = settings.locale || "id";
  const currency = settings.currency || "IDR";

  const parts = new Intl.NumberFormat(locale, {
    style: "currency",
    currency,
    currencyDisplay: "symbol",
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).formatToParts(100000);

  const number = parts
    .filter((part) => !["currency", "literal"].includes(part.type))
    .map((part) => part.value)
    .join("");

  const symbol = parts.find((part) => part.type === "currency")?.value;

  if (!symbol) return number;

  return position === "before" ? `${symbol} ${number}` : `${number} ${symbol}`;
};


</script>
<template>
  <v-container fluid class="pa-0">
    <div class="d-flex flex-wrap align-center justify-space-between ga-4 mb-6">
      <div>
        <span class="text-headline-small font-weight-bold">
          {{ t("regional.title") }}
        </span>
        <p class="text-body-medium text-medium-emphasis mt-1">
          {{ t("regional.subtitle") }}
        </p>
      </div>
    </div>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
      {{ t("regional.alerts.loadFailed") }}
      <v-btn variant="text" size="small" @click="refresh">
        {{ t("regional.alerts.retry") }}
      </v-btn>
    </v-alert>

    <v-alert v-if="saveError" type="error" variant="tonal" class="mb-4">
      {{ saveError }}
    </v-alert>

    <v-alert v-if="saved" type="success" variant="tonal" class="mb-4">
      {{ t("regional.alerts.savedSuccess") }}
    </v-alert>

    <v-row>
      <v-col cols="12" lg="8">
        <v-card border flat>
          <v-card-title class="pa-5 text-title-large">{{
            t("regional.region.title")
          }}</v-card-title>
          <v-divider />

          <v-card-text class="pa-5">
            <v-row>
              <v-col cols="12">
                <v-autocomplete
                  v-model="settings.countryId"
                  :items="countries ?? []"
                  :loading="pending"
                  :disabled="pending || !!error || loadingSettings"
                  item-title="name"
                  item-value="id"
                  :label="t('regional.region.countryLabel')"
                  :placeholder="t('regional.region.countryPlaceholder')"
                  prepend-inner-icon="mdi-earth"
                  variant="outlined"
                  :no-data-text="t('regional.region.noCountries')"
                  clearable
                  hide-details="auto"
                  @update:model-value="markChanged"
                >
                  <template #item="{ props, item }">
                    <v-list-item v-bind="props">
                      <template #prepend>
                        <span class="text-headline-small mr-3">
                          {{ item.emoji }}
                        </span>
                      </template>

                      <template #title>
                        <span class="text-body-large">{{ item.name }}</span>
                      </template>

                      <v-list-item-subtitle class="text-body-small">
                        {{ item.iso2 }} · +{{ item.phonecode }}
                      </v-list-item-subtitle>
                    </v-list-item>
                  </template>

                  <template #selection="{ item }">
                    <span class="text-body-medium"
                      >{{ item.emoji }} {{ item.name }}</span
                    >
                  </template>
                </v-autocomplete>
              </v-col>

              <v-col cols="12" md="6">
                <v-select
                  v-model="settings.locale"
                  :items="locales"
                  item-title="name"
                  item-value="code"
                  :label="t('regional.region.languageLabel')"
                  prepend-inner-icon="mdi-translate"
                  variant="outlined"
                  hide-details="auto"
                  @update:model-value="updateLocale"
                />
              </v-col>

              <v-col cols="12" md="6">
                <v-autocomplete
                  v-model="settings.timezone"
                  :items="timezoneItems"
                  item-title="title"
                  item-value="value"
                  :label="t('regional.region.timezoneLabel')"
                  prepend-inner-icon="mdi-clock-outline"
                  variant="outlined"
                  :disabled="
                    !selectedCountry ||
                    timezoneItems.length === 0 ||
                    loadingSettings
                  "
                  :no-data-text="t('regional.region.noTimezones')"
                  hide-details="auto"
                  @update:model-value="markChanged"
                />
              </v-col>
            </v-row>
          </v-card-text>

          <v-divider />

          <v-card-title class="pa-5 text-title-large">{{
            t("regional.currency.title")
          }}</v-card-title>
          <v-divider />

          <v-card-text class="pa-5">
            <v-row>
              <v-col cols="12" md="7">
                <v-select
                  v-model="settings.currency"
                  :items="currencyItems"
                  item-title="title"
                  item-value="value"
                  :label="t('regional.currency.storeCurrencyLabel')"
                  prepend-inner-icon="mdi-cash"
                  variant="outlined"
                  :disabled="!selectedCountry || loadingSettings"
                  hide-details="auto"
                  @update:model-value="markChanged"
                />
              </v-col>

              <v-col cols="12" md="5">
                <v-select
                  v-model="settings.currencyPosition"
                  :items="[
                    {
                      title: t('regional.currency.positions.before', {
                        preview: formatCurrencyPreview('before'),
                      }),
                      value: 'before',
                    },
                    {
                      title: t('regional.currency.positions.after', {
                        preview: formatCurrencyPreview('after'),
                      }),
                      value: 'after',
                    },
                  ]"
                  item-title="title"
                  item-value="value"
                  :label="t('regional.currency.currencyPositionLabel')"
                  variant="outlined"
                  hide-details="auto"
                  @update:model-value="markChanged"
                />
              </v-col>
            </v-row>
          </v-card-text>

          <v-divider />

          <v-card-title class="pa-5 text-title-large">{{
            t("regional.dateCalendar.title")
          }}</v-card-title>
          <v-divider />

          <v-card-text class="pa-5">
            <v-row>
              <v-col cols="12" md="6">
                <v-select
                  v-model="settings.dateFormat"
                  :items="dateFormats"
                  item-title="title"
                  item-value="value"
                  :label="t('regional.dateCalendar.dateFormatLabel')"
                  variant="outlined"
                  hide-details="auto"
                  @update:model-value="markChanged"
                />
              </v-col>

              <v-col cols="12" md="6">
                <v-select
                  v-model="settings.firstDayOfWeek"
                  :items="[
                    {
                      title: t('regional.dateCalendar.days.monday'),
                      value: 'monday',
                    },
                    {
                      title: t('regional.dateCalendar.days.sunday'),
                      value: 'sunday',
                    },
                  ]"
                  item-title="title"
                  item-value="value"
                  :label="t('regional.dateCalendar.firstDayLabel')"
                  variant="outlined"
                  hide-details="auto"
                  @update:model-value="markChanged"
                />
              </v-col>
            </v-row>
          </v-card-text>

          <v-divider />

          <v-card-actions class="pa-5 justify-end">
            <v-btn variant="text" :disabled="saving" @click="resetSettings">
              {{ t("regional.actions.reset") }}
            </v-btn>

            <v-btn
              color="primary"
              prepend-icon="mdi-content-save"
              :loading="saving"
              :disabled="loadingSettings"
              @click="saveSettings"
            >
              {{ t("regional.actions.save") }}
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-col>

      <v-col cols="12" lg="4">
        <v-card border flat>
          <v-card-title class="pa-5 text-title-large">{{
            t("regional.preview.title")
          }}</v-card-title>
          <v-divider />

          <v-card-text class="pa-5">
            <div class="text-body-small text-medium-emphasis mb-1">
              {{ t("regional.region.countryLabel") }}
            </div>
            <div class="text-body-medium font-weight-medium mb-4">
              {{
                selectedCountry
                  ? `${selectedCountry.emoji} ${selectedCountry.name} (${selectedCountry.iso2})`
                  : t("regional.preview.notSelected")
              }}
            </div>

            <div class="text-body-small text-medium-emphasis mb-1">
              {{ t("regional.currency.storeCurrencyLabel") }}
            </div>
            <div class="text-body-medium font-weight-medium mb-4">
              {{ selectedCountry?.currencySymbol ?? "" }}
              {{ settings.currency || t("regional.preview.notSelected") }}
              <span
                v-if="selectedCountry?.currencyName"
                class="text-medium-emphasis"
              >
                — {{ selectedCountry.currencyName }}
              </span>
            </div>

            <div class="text-body-small text-medium-emphasis mb-1">
              {{ t("regional.region.timezoneLabel") }}
            </div>
            <div class="text-body-medium font-weight-medium mb-4">
              {{ settings.timezone || t("regional.preview.notSelected") }}
            </div>

            <div class="text-body-small text-medium-emphasis mb-1">
              {{ t("regional.region.languageLabel") }}
            </div>
            <div class="text-body-medium font-weight-medium mb-4">
              {{
                locales.find((item) => item.value === settings.locale)?.title
              }}
            </div>

            <div class="text-body-small text-medium-emphasis mb-1">
              {{ t("regional.dateCalendar.dateFormatLabel") }}
            </div>

            <div class="text-body-medium font-weight-medium">
              {{ datePreview.formatted }}
            </div>

            <div class="text-body-small text-medium-emphasis mb-4">
              {{ datePreview.long }}
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
