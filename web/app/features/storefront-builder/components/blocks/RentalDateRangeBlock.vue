<script setup lang="ts">
import { computed } from "vue";
import { storeToRefs } from "pinia";
import { useStorefrontProductStore } from "~/stores/storefront-product";

const props = defineProps<{
  preview?: boolean;
  label?: string;
}>();

const store = useStorefrontProductStore();

const { startDate, endDate, rentalDays, rentalPlanCombination } =
  storeToRefs(store);

const model = computed<Date[] | null>({
  get() {
    if (!startDate.value) return null;

    const dates = [toDate(startDate.value)];

    if (endDate.value) {
      dates.push(toDate(endDate.value));
    }

    return dates;
  },

  set(value) {
    if (!value?.length) {
      store.selectDateRange([]);
      return;
    }

    store.selectDateRange(value.map(toLocalDateString));
  },
});

const minDate = computed(() => {
  const date = new Date();
  date.setHours(0, 0, 0, 0);
  return date;
});

const formattedStartDate = computed(() => store.formatDate(startDate.value));

const formattedEndDate = computed(() => store.formatDate(endDate.value));

function toDate(value: string): Date {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year!, month! - 1, day!);
}

function toLocalDateString(value: Date): string {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}
</script>

<template>
  <section class="rental-date-range">
    <div class="rental-date-range__header">
      <div class="rental-date-range__heading">
        <v-icon
          icon="mdi-calendar-range"
          size="20"
          class="rental-date-range__icon"
        />

        <span class="rental-date-range__title">
          {{ props.label || "Periode sewa" }}
        </span>
      </div>

      <span class="rental-date-range__hint">
        {{ $t("ui.pilih_tanggal_mulai_dan_selesai") }}
      </span>
    </div>

    <v-date-input
      v-model="model"
      class="rental-date-range__input"
      :label="$t('ui.tanggal_sewa')"
      multiple="range"
      :min="minDate"
      variant="outlined"
      prepend-icon=""
      prepend-inner-icon="mdi-calendar"
      hide-details="auto"
      clearable
    />

    <div
      v-if="startDate && endDate && rentalDays > 0"
      class="rental-date-range__summary"
    >
      <div class="rental-date-range__summary-header">
        <span class="rental-date-range__summary-title">
          {{ $t("ui.ringkasan_periode") }}
        </span>

        <span class="rental-date-range__duration"> {{ rentalDays }} {{ $t("orders.create.days") }} </span>
      </div>

      <div class="rental-date-range__dates">
        <div class="rental-date-range__date-item">
          <span class="rental-date-range__date-label"> {{ $t("ui.mulai_sewa") }} </span>

          <span class="rental-date-range__date-value">
            {{ formattedStartDate }}
          </span>
        </div>

        <v-icon
          icon="mdi-arrow-right"
          size="18"
          class="rental-date-range__date-arrow"
        />

        <div class="rental-date-range__date-item">
          <span class="rental-date-range__date-label"> {{ $t("ui.selesai_sewa") }} </span>

          <span class="rental-date-range__date-value">
            {{ formattedEndDate }}
          </span>
        </div>
      </div>
      <div v-if="rentalPlanCombination.length" class="rental-date-range__plans">
        <div
          v-for="item in rentalPlanCombination"
          :key="item.plan.id"
          class="rental-date-range__plan"
        >
          <span class="rental-date-range__plan-name">
            {{ store.formatDuration(item.plan) }}

            <template v-if="item.quantity > 1">
              × {{ item.quantity }}
            </template>
          </span>

          <span class="rental-date-range__plan-price">
            {{ store.formatPrice(item.plan.rentalFee * item.quantity) }}
          </span>
        </div>
      </div>
    </div>

    <p v-else class="rental-date-range__helper">
      {{ $t("ui.pilih_rentang_tanggal_untuk_melihat_durasi_dan_biaya_sewa") }}
    </p>
  </section>
</template>

<style scoped lang="scss">
.rental-date-range {
  display: flex;
  flex-direction: column;
  gap: calc(var(--store-spacing, 24px) * 0.5);
  width: 100%;
  min-width: 0;
  color: var(--store-text);
  font-family: inherit;

  &__header {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__heading {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__icon {
    color: var(--store-primary);
  }

  &__title {
    font-family: var(--store-heading-font, inherit);
    font-size: 0.9375rem;
    font-weight: 600;
    line-height: 1.5;
  }

  &__hint,
  &__helper {
    color: var(--store-text);
    opacity: 0.65;
    font-size: 0.8125rem;
    line-height: 1.6;
  }

  &__input {
    width: 100%;
    color: var(--store-text);

    :deep(.v-field) {
      border-radius: var(--store-radius, 8px);
      background: var(--store-surface);
      color: var(--store-text);
    }

    :deep(.v-field__outline) {
      color: var(--store-text);
      opacity: 0.3;
    }

    :deep(.v-label),
    :deep(.v-field__input),
    :deep(.v-icon) {
      color: var(--store-text);
    }

    :deep(.v-field--focused .v-field__outline) {
      color: var(--store-primary);
      opacity: 1;
    }
  }

  &__summary {
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 16px;
    border: 1px solid color-mix(in srgb, var(--store-text) 15%, transparent);
    border-radius: var(--store-radius, 8px);
    background: var(--store-surface);
  }

  &__summary-header,
  &__pricing,
  &__plan {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  &__summary-title {
    font-family: var(--store-heading-font, inherit);
    font-size: 0.875rem;
    font-weight: 600;
  }

  &__duration {
    padding: 4px 10px;
    border-radius: var(--store-radius, 8px);
    background: color-mix(in srgb, var(--store-primary) 10%, transparent);
    color: var(--store-primary);
    font-size: 0.75rem;
    font-weight: 600;
    white-space: nowrap;
  }

  &__dates {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
    align-items: center;
    gap: 12px;
  }

  &__date-item {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
  }

  &__date-label {
    font-size: 0.75rem;
    opacity: 0.65;
  }

  &__date-value {
    font-size: 0.8125rem;
    font-weight: 500;
    overflow-wrap: anywhere;
  }

  &__date-arrow {
    opacity: 0.5;
  }

  &__pricing {
    padding-top: 14px;
    border-top: 1px solid color-mix(in srgb, var(--store-text) 12%, transparent);
  }

  &__pricing-label {
    font-size: 0.8125rem;
    opacity: 0.75;
  }

  &__pricing-value {
    color: var(--store-primary);
    font-family: var(--store-heading-font, inherit);
    font-size: 1rem;
    font-weight: 700;
    text-align: right;
  }

  &__plans {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding-top: 12px;
    border-top: 1px dashed
      color-mix(in srgb, var(--store-text) 20%, transparent);
  }

  &__plan {
    align-items: baseline;
  }

  &__plan-name,
  &__plan-price {
    font-size: 0.8125rem;
  }

  &__plan-name {
    opacity: 0.7;
  }

  &__plan-price {
    font-weight: 500;
    text-align: right;
    white-space: nowrap;
  }

  &__helper {
    margin: 0;
  }
}

@media (max-width: 600px) {
  .rental-date-range {
    &__summary {
      padding: 12px;
    }

    &__dates {
      gap: 8px;
    }

    &__date-value {
      font-size: 0.75rem;
    }
  }
}
</style>
