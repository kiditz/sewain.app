import { defineStore } from "pinia";
import { computed, ref, watch } from "vue";
import type { Product, ProductVariant, RentalPlan } from "~/types/product";
import {
  calculateRentalPlanCombination,
  getRentalDays,
  getRentalPlanDays as getPlanDays,
  MAX_RENTAL_DAYS,
  type RentalPlanSelection,
} from "~/utils/rental-plan-calculator";

export const useStorefrontProductStore = defineStore(
  "storefront-product",
  () => {
    const product = ref<Product | null>(null);
    const selectedVariantId = ref<string | null>(null);
    const selectedPlanId = ref<string | null>(null);
    const selectedMedia = ref("");
    const startDate = ref("");
    const endDate = ref("");
    const quantity = ref(1);
    const selectedAttributes = ref<Record<string, string>>({});

    const activeVariants = computed(() =>
      (product.value?.variants ?? []).filter((variant) => variant.active),
    );

    const availableAttributes = computed(() => {
      const attributes = new Map<string, Set<string>>();

      for (const variant of activeVariants.value) {
        for (const attribute of variant.values) {
          if (!attributes.has(attribute.attributeName)) {
            attributes.set(attribute.attributeName, new Set<string>());
          }

          attributes.get(attribute.attributeName)!.add(attribute.value);
        }
      }

      return Array.from(attributes, ([name, values]) => ({
        name,
        values: Array.from(values),
      }));
    });

    const selectedVariant = computed<ProductVariant | null>(() => {
      const selections = Object.entries(selectedAttributes.value);
      const attributes = availableAttributes.value;

      if (attributes.length === 0) {
        return (
          activeVariants.value.find(
            (variant) => variant.id === selectedVariantId.value,
          ) ??
          activeVariants.value[0] ??
          null
        );
      }

      if (selections.length !== attributes.length) {
        return null;
      }

      const variant =
        activeVariants.value.find(
          (candidate) =>
            candidate.values.length === selections.length &&
            selections.every(([name, value]) =>
              candidate.values.some(
                (attribute) =>
                  attribute.attributeName === name && attribute.value === value,
              ),
            ),
        ) ?? null;

      return variant;
    });

    const activePlans = computed(() =>
      (selectedVariant.value?.rentalPlans ?? []).filter(
        (plan) =>
          plan.active &&
          Number.isSafeInteger(plan.durationValue) &&
          plan.durationValue > 0 &&
          Number.isFinite(plan.rentalFee) &&
          plan.rentalFee >= 0,
      ),
    );

    const availableQuantity = computed(
      () =>
        selectedVariant.value?.inventoryUnits.filter(
          (unit) => unit.status === "AVAILABLE",
        ).length ?? 0,
    );

    // Compatibility for components that still display a selected plan.
    // Automatic period pricing uses rentalPlanCombination.
    const selectedPlan = computed<RentalPlan | null>(
      () =>
        activePlans.value.find((plan) => plan.id === selectedPlanId.value) ??
        null,
    );

    const gallery = computed(() => {
      const images = [
        ...(selectedVariant.value?.mediaList ?? []),
        product.value?.media ?? "",
      ].filter(Boolean);

      return [...new Set(images)];
    });

    function formatLocalDate(date: Date, includeTime = false): string {
      const pad = (value: number) => String(value).padStart(2, "0");

      const day = [
        date.getFullYear(),
        pad(date.getMonth() + 1),
        pad(date.getDate()),
      ].join("-");

      return includeTime
        ? `${day}T${pad(date.getHours())}:${pad(date.getMinutes())}`
        : day;
    }

    function parseLocalDate(value: string): Date | null {
      if (!value) return null;

      const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);

      if (match) {
        const year = Number(match[1]);
        const month = Number(match[2]);
        const day = Number(match[3]);
        const date = new Date(year, month - 1, day);

        if (
          date.getFullYear() !== year ||
          date.getMonth() !== month - 1 ||
          date.getDate() !== day
        ) {
          return null;
        }

        return date;
      }

      const date = new Date(value);

      return Number.isNaN(date.getTime()) ? null : date;
    }

    function toUtcDay(date: Date): number {
      return Date.UTC(date.getFullYear(), date.getMonth(), date.getDate());
    }

    const rentalDays = computed(() =>
      getRentalDays(startDate.value, endDate.value),
    );

    /**
     * Finds the lowest-cost combination of repeatable plans that
     * covers the rental period. Extra covered days are allowed.
     */
    const rentalPlanCombination = computed<RentalPlanSelection[]>(() =>
      calculateRentalPlanCombination(activePlans.value, rentalDays.value),
    );

    // Rental price for one product unit, including all selected plans.
    const rentalFee = computed(() =>
      rentalPlanCombination.value.reduce(
        (sum, item) => sum + item.plan.rentalFee * item.quantity,
        0,
      ),
    );

    const deposit = computed(() => {
      const amount = selectedVariant.value?.depositAmount ?? 0;

      return Number.isFinite(amount) && amount >= 0 ? amount : 0;
    });

    // Subtotals account for the number of product units.
    const rentalSubtotal = computed(() => rentalFee.value * quantity.value);

    const depositSubtotal = computed(() => deposit.value * quantity.value);

    const total = computed(() => rentalSubtotal.value + depositSubtotal.value);

    const coveredDays = computed(() =>
      rentalPlanCombination.value.reduce(
        (sum, item) => sum + getPlanDays(item.plan) * item.quantity,
        0,
      ),
    );

    const dateInputType = computed(() => "date");

    const canAddToCart = computed(
      () =>
        product.value?.status === "ACTIVE" && !!selectedVariant.value &&
        rentalDays.value > 0 &&
        rentalDays.value <= MAX_RENTAL_DAYS &&
        rentalPlanCombination.value.length > 0 &&
        Number.isSafeInteger(quantity.value) &&
        quantity.value >= 1,
    );

    function formatPrice(value: number): string {
      return new Intl.NumberFormat("id-ID", {
        style: "currency",
        currency: "IDR",
        maximumFractionDigits: 0,
      }).format(value);
    }

    function formatDuration(plan: RentalPlan): string {
      const units: Record<string, string> = {
        HOUR: "jam",
        DAY: "hari",
        WEEK: "minggu",
        MONTH: "bulan",
      };

      return `${plan.durationValue} ${units[plan.durationUnit] ?? plan.durationUnit.toLowerCase()}`;
    }

    function formatDate(value: string): string {
      if (!value) return "—";

      const date = parseLocalDate(value);

      if (!date) return "—";

      return new Intl.DateTimeFormat("id-ID", {
        dateStyle: "medium",
        ...(value.includes("T") ? { timeStyle: "short" as const } : {}),
      }).format(date);
    }

    function selectDateRange(range: string[]): void {
      const start = range[0] ?? "";
      const end = range.length > 1 ? (range[range.length - 1] ?? "") : "";

      const parsedStart = parseLocalDate(start);
      const parsedEnd = parseLocalDate(end);

      if (!parsedStart) {
        startDate.value = "";
        endDate.value = "";
        return;
      }

      startDate.value = start;

      endDate.value =
        parsedEnd && toUtcDay(parsedEnd) > toUtcDay(parsedStart) ? end : "";
    }

    function selectVariant(variant: ProductVariant): void {
      selectedVariantId.value = variant.id;

      selectedAttributes.value = Object.fromEntries(
        variant.values.map((attribute) => [
          attribute.attributeName,
          attribute.value,
        ]),
      );

      selectedPlanId.value =
        variant.rentalPlans.find((plan) => plan.active)?.id ?? null;

      selectedMedia.value = variant.mediaList[0] || product.value?.media || "";

      startDate.value = "";
      endDate.value = "";
    }

    function selectPlan(planId: string | null): void {
      if (planId === selectedPlanId.value) return;

      selectedPlanId.value = planId;
      startDate.value = "";
      endDate.value = "";
    }

    function setProduct(value: Product): void {
      product.value = value;
      selectedAttributes.value = {};

      const firstVariant = value.variants?.find((variant) => variant.active);

      if (firstVariant) {
        selectedVariantId.value = firstVariant.id;

        selectedAttributes.value = Object.fromEntries(
          firstVariant.values.map((attribute) => [
            attribute.attributeName,
            attribute.value,
          ]),
        );

        selectedMedia.value = firstVariant.mediaList[0] || value.media || "";

        selectedPlanId.value =
          firstVariant.rentalPlans.find((plan) => plan.active)?.id ?? null;
      } else {
        selectedVariantId.value = null;
        selectedPlanId.value = null;
        selectedMedia.value = value.media || "";
      }

      startDate.value = "";
      endDate.value = "";
      quantity.value = 1;
    }

    function reset(): void {
      product.value = null;
      selectedVariantId.value = null;
      selectedPlanId.value = null;
      selectedMedia.value = "";
      startDate.value = "";
      endDate.value = "";
      quantity.value = 1;
      selectedAttributes.value = {};
    }

    function selectAttribute(name: string, value: string): void {
      selectedAttributes.value = {
        ...selectedAttributes.value,
        [name]: value,
      };

      const variant = selectedVariant.value;

      startDate.value = "";
      endDate.value = "";

      if (variant) {
        selectedVariantId.value = variant.id;

        selectedPlanId.value =
          variant.rentalPlans.find((plan) => plan.active)?.id ?? null;

        selectedMedia.value =
          variant.mediaList[0] || product.value?.media || "";
      } else {
        selectedVariantId.value = null;
        selectedPlanId.value = null;
        selectedMedia.value = product.value?.media ?? "";
      }
    }

    watch(quantity, (value) => {
      if (!Number.isSafeInteger(value) || value < 1) {
        quantity.value = 1;
      }
    });

    return {
      product,
      selectedVariantId,
      selectedPlanId,
      selectedMedia,
      startDate,
      endDate,
      quantity,
      activeVariants,
      selectedVariant,
      activePlans,
      selectedPlan,
      gallery,
      rentalDays,
      rentalPlanCombination,
      rentalFee,
      coveredDays,
      deposit,
      rentalSubtotal,
      depositSubtotal,
      total,
      dateInputType,
      canAddToCart,
      availableAttributes,
      selectedAttributes,
      availableQuantity,
      selectAttribute,
      formatPrice,
      formatDuration,
      formatDate,
      selectDateRange,
      selectVariant,
      selectPlan,
      setProduct,
      reset,
    };
  },
);
