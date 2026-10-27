package app.sewain.catalogs;


import app.sewain.exception.ValidationException;

import app.sewain.exception.ErrorCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class RentalPriceCalculator {

	private static final int MAX_RENTAL_DAYS = 3650;

	private RentalPriceCalculator() {}

	record RentalPlan(UUID id, String durationUnit, int durationValue, BigDecimal rentalFee) {}

	record PlanSelection(UUID rentalPlanId, int quantity, long durationDays, BigDecimal unitPrice) {}

	record RentalPriceResult(
			long requestedDurationDays,
			long coveredDurationDays,
			List<PlanSelection> selections,
			BigDecimal totalPrice
	) {}

	private record Step(int previousDays, int planIndex) {}

	static RentalPriceResult calculate(
			OffsetDateTime startDateTime,
			OffsetDateTime endDateTime,
			ZoneId timezone,
			List<RentalPlan> plans
	) {
		int requestedDays = calculateDurationDays(startDateTime, endDateTime, timezone);
		List<RentalPlan> validPlans = getValidPlans(plans);
		List<RentalPlan> repeatablePlans = validPlans.stream()
				.filter(plan -> toDays(plan) <= requestedDays)
				.toList();

		int maxPlanDays = repeatablePlans.stream()
				.mapToInt(plan -> Math.toIntExact(toDays(plan)))
				.max()
				.orElse(0);
		int limit = repeatablePlans.isEmpty()
				? 0
				: Math.addExact(requestedDays, maxPlanDays - 1);
		BigDecimal[] bestPrice = new BigDecimal[limit + 1];
		Step[] steps = new Step[limit + 1];
		bestPrice[0] = BigDecimal.ZERO;
		calculateBestPrices(limit, repeatablePlans, bestPrice, steps);

		long bestDays = -1;
		BigDecimal bestCost = null;
		for (int days = requestedDays; days <= limit; days++) {
			if (bestPrice[days] != null && isBetter(days, bestPrice[days], bestDays, bestCost)) {
				bestDays = days;
				bestCost = bestPrice[days];
			}
		}

		RentalPlan directPlan = null;
		for (RentalPlan plan : validPlans) {
			long planDays = toDays(plan);
			if (planDays <= requestedDays) continue;
			if (isBetter(planDays, plan.rentalFee(), bestDays, bestCost)) {
				bestDays = planDays;
				bestCost = plan.rentalFee();
				directPlan = plan;
			}
		}

		if (bestDays < 0 || bestCost == null) {
			throw new ValidationException(ErrorCode.RENTAL_PLAN_COMBINATION_NOT_FOUND);
		}

		List<PlanSelection> selections = directPlan == null
				? buildSelections(Math.toIntExact(bestDays), repeatablePlans, steps)
				: List.of(new PlanSelection(directPlan.id(), 1, toDays(directPlan), directPlan.rentalFee()));
		return new RentalPriceResult(requestedDays, bestDays, List.copyOf(selections), bestCost);
	}

	private static int calculateDurationDays(OffsetDateTime start, OffsetDateTime end, ZoneId timezone) {
		if (start == null || end == null || timezone == null) {
			throw new ValidationException(ErrorCode.RENTAL_DATE_INVALID);
		}
		long days = ChronoUnit.DAYS.between(
				start.atZoneSameInstant(timezone).toLocalDate(),
				end.atZoneSameInstant(timezone).toLocalDate()
		);
		if (days <= 0 || days > MAX_RENTAL_DAYS) {
			throw new ValidationException(ErrorCode.RENTAL_DURATION_INVALID);
		}
		return Math.toIntExact(days);
	}

	private static List<RentalPlan> getValidPlans(List<RentalPlan> plans) {
		return plans.stream()
				.filter(plan -> toDays(plan) > 0)
				.filter(plan -> plan.rentalFee() != null && plan.rentalFee().signum() >= 0)
				.toList();
	}

	private static long toDays(RentalPlan plan) {
		long multiplier = switch (plan.durationUnit().toUpperCase(Locale.ROOT)) {
			case "DAY" -> 1L;
			case "WEEK" -> 7L;
			case "MONTH" -> 30L;
			// Date-only rental periods do not support hourly plans.
			case "HOUR" -> 0L;
			default -> throw new ValidationException(ErrorCode.RENTAL_DURATION_UNIT_UNSUPPORTED);
		};
		return Math.multiplyExact(plan.durationValue(), multiplier);
	}

	private static void calculateBestPrices(int limit, List<RentalPlan> plans, BigDecimal[] bestPrice, Step[] steps) {
		for (int days = 1; days <= limit; days++) {
			for (int index = 0; index < plans.size(); index++) {
				RentalPlan plan = plans.get(index);
				int planDays = Math.toIntExact(toDays(plan));
				int previousDays = days - planDays;
				if (previousDays < 0 || bestPrice[previousDays] == null) continue;

				BigDecimal candidate = bestPrice[previousDays].add(plan.rentalFee());
				if (bestPrice[days] == null || candidate.compareTo(bestPrice[days]) < 0) {
					bestPrice[days] = candidate;
					steps[days] = new Step(previousDays, index);
				}
			}
		}
	}

	private static boolean isBetter(long days, BigDecimal cost, long currentDays, BigDecimal currentCost) {
		if (currentCost == null) return true;
		int priceComparison = cost.compareTo(currentCost);
		return priceComparison < 0 || (priceComparison == 0 && days < currentDays);
	}

	private static List<PlanSelection> buildSelections(int days, List<RentalPlan> plans, Step[] steps) {
		int[] quantities = new int[plans.size()];
		int current = days;
		while (current > 0) {
			Step step = steps[current];
			if (step == null) throw new IllegalStateException(ErrorCode.RENTAL_PLAN_CALCULATION_FAILED);
			quantities[step.planIndex()]++;
			current = step.previousDays();
		}

		List<PlanSelection> selections = new ArrayList<>();
		for (int index = 0; index < quantities.length; index++) {
			if (quantities[index] > 0) {
				RentalPlan plan = plans.get(index);
				selections.add(new PlanSelection(plan.id(), quantities[index], toDays(plan), plan.rentalFee()));
			}
		}
		return selections;
	}
}
