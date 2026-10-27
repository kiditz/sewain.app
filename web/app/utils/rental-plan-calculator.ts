import type { RentalPlan } from "~/types/product";

export type RentalPlanSelection = {
  plan: RentalPlan;
  quantity: number;
};

type PlanOption = RentalPlanSelection & { days: number; fee: number };
type PreviousStep = { previousDays: number; planIndex: number };

export const MAX_RENTAL_DAYS = 3650;

export function getRentalDays(
  startDate: string | undefined,
  endDate: string | undefined,
): number {
  if(!startDate || !endDate) return 0;
  const parse = (value: string) => {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
    if (!match) return null;
    const year = Number(match[1]);
    const month = Number(match[2]);
    const day = Number(match[3]);
    const date = new Date(Date.UTC(year, month - 1, day));
    if (
      date.getUTCFullYear() !== year ||
      date.getUTCMonth() !== month - 1 ||
      date.getUTCDate() !== day
    ) {
      return null;
    }
    return date.getTime();
  };

  const start = parse(startDate);
  const end = parse(endDate);
  if (start === null || end === null || end <= start) return 0;
  return (end - start) / 86_400_000;
}

export function getRentalPlanDays(plan: RentalPlan): number {
  let days = 0;
  switch (plan.durationUnit) {
    case "DAY":
      days = plan.durationValue;
      break;
    case "WEEK":
      days = plan.durationValue * 7;
      break;
    case "MONTH":
      days = plan.durationValue * 30;
      break;
    // Storefront date selection does not support hourly plans.
    case "HOUR":
      return 0;
  }
  return Number.isSafeInteger(days) && days > 0 ? days : 0;
}

/** Lowest-cost repeatable plan combination that covers the requested days. */
export function calculateRentalPlanCombination(
  plans: RentalPlan[],
  requestedDays: number,
): RentalPlanSelection[] {
  const options: PlanOption[] = plans
    .filter(
      (plan) =>
        plan.active &&
        Number.isSafeInteger(plan.durationValue) &&
        plan.durationValue > 0 &&
        Number.isFinite(plan.rentalFee) &&
        plan.rentalFee >= 0,
    )
    .map((plan) => ({
      plan,
      quantity: 1,
      days: getRentalPlanDays(plan),
      fee: plan.rentalFee,
    }))
    .filter((option) => option.days > 0);

  if (
    !Number.isSafeInteger(requestedDays) ||
    requestedDays <= 0 ||
    requestedDays > MAX_RENTAL_DAYS ||
    options.length === 0
  ) {
    return [];
  }

  const repeatableOptions = options.filter(
    (option) => option.days <= requestedDays,
  );
  const maxPlanDays = repeatableOptions.length
    ? Math.max(...repeatableOptions.map((option) => option.days))
    : 0;
  const limit = repeatableOptions.length ? requestedDays + maxPlanDays - 1 : 0;
  if (!Number.isSafeInteger(limit)) return [];

  const bestPrice = Array<number>(limit + 1).fill(Number.POSITIVE_INFINITY);
  const previous: Array<PreviousStep | null> = Array(limit + 1).fill(null);
  bestPrice[0] = 0;

  for (let totalDays = 1; totalDays <= limit; totalDays++) {
    for (let index = 0; index < repeatableOptions.length; index++) {
      const option = repeatableOptions[index];
      if (!option) continue;
      const previousDays = totalDays - option.days;
      if (previousDays < 0) continue;
      const previousCost = bestPrice[previousDays];
      if (previousCost === undefined || !Number.isFinite(previousCost))
        continue;

      const cost = previousCost + option.fee;
      if (cost < (bestPrice[totalDays] ?? Number.POSITIVE_INFINITY)) {
        bestPrice[totalDays] = cost;
        previous[totalDays] = { previousDays, planIndex: index };
      }
    }
  }

  let bestDays = -1;
  let bestCost = Number.POSITIVE_INFINITY;
  for (let totalDays = requestedDays; totalDays <= limit; totalDays++) {
    const cost = bestPrice[totalDays];
    if (cost !== undefined && Number.isFinite(cost) && cost < bestCost) {
      bestCost = cost;
      bestDays = totalDays;
    }
  }

  let directPlan: RentalPlan | null = null;
  for (const option of options) {
    if (option.days <= requestedDays) continue;
    if (
      option.fee < bestCost ||
      (option.fee === bestCost && (bestDays < 0 || option.days < bestDays))
    ) {
      bestCost = option.fee;
      bestDays = option.days;
      directPlan = option.plan;
    }
  }
  if (bestDays < 0) return [];
  if (directPlan) return [{ plan: directPlan, quantity: 1 }];

  const quantities = Array<number>(repeatableOptions.length).fill(0);
  for (let currentDays = bestDays; currentDays > 0; ) {
    const step = previous[currentDays];
    if (!step) return [];
    quantities[step.planIndex] = (quantities[step.planIndex] ?? 0) + 1;
    currentDays = step.previousDays;
  }

  return repeatableOptions.flatMap((option, index) => {
    const quantity = quantities[index] ?? 0;
    return quantity > 0 ? [{ plan: option.plan, quantity }] : [];
  });
}
