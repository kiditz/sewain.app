package app.sewain.catalogs;

import app.sewain.exception.ConflictException;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderCatalogService {
	private final ProductVariantRepository variants;
	private final VariantRentalPlanRepository plans;
	private final InventoryUnitLockRepository inventory;

	@Transactional(readOnly = true)
	public VariantPricing getPricing(UUID variantId, OffsetDateTime start, OffsetDateTime end, ZoneId timezone) {
		var variant = variants.findById(variantId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VARIANT_NOT_FOUND));
		if (!variant.isActive() || variant.getProduct().getStatus() != ProductStatus.ACTIVE) throw new ConflictException(ErrorCode.VARIANT_NOT_AVAILABLE);
		var activePlans = plans.findAll().stream()
				.filter(plan -> plan.isActive() && plan.getVariant().getId().equals(variantId))
				.toList();
		var rentalPlans = activePlans.stream()
				.map(plan -> new RentalPriceCalculator.RentalPlan(plan.getId(), plan.getDurationUnit().name(), plan.getDurationValue(), plan.getRentalFee()))
				.toList();
		var quote = RentalPriceCalculator.calculate(start, end, timezone, rentalPlans);
		var planById = activePlans.stream().collect(java.util.stream.Collectors.toMap(VariantRentalPlan::getId, plan -> plan));
		var selections = quote.selections().stream().map(selection -> {
			var plan = planById.get(selection.rentalPlanId());
			var snapshot = new LinkedHashMap<String, Object>();
			snapshot.put("rentalPlanId", selection.rentalPlanId());
			snapshot.put("durationUnit", plan.getDurationUnit().name());
			snapshot.put("durationValue", plan.getDurationValue());
			snapshot.put("durationDays", selection.durationDays());
			snapshot.put("quantity", selection.quantity());
			snapshot.put("unitPrice", selection.unitPrice());
			return snapshot;
		}).toList();
		var priceSnapshot = new LinkedHashMap<String, Object>();
		priceSnapshot.put("requestedDurationDays", quote.requestedDurationDays());
		priceSnapshot.put("coveredDurationDays", quote.coveredDurationDays());
		priceSnapshot.put("selections", selections);
		priceSnapshot.put("totalPrice", quote.totalPrice());
		return new VariantPricing(variant.getProduct().getId(), variant.getId(), variant.getProduct().getName(), variant.getName(), variant.getSku(), variant.getDepositAmount(), quote.totalPrice(), quote.requestedDurationDays(), priceSnapshot);
	}

	@Transactional
	public List<UUID> reserveUnits(UUID variantId, OffsetDateTime start, OffsetDateTime end, int quantity, int bufferHours) {
		OffsetDateTime bufferedEnd = end.plusHours(bufferHours);
		var unitIds = inventory.lockAvailableUnits(variantId, start, bufferedEnd, quantity);
		if (unitIds.size() != quantity) throw new ConflictException(ErrorCode.ORDER_INVENTORY_UNAVAILABLE);
		return unitIds;
	}

	public record VariantPricing(UUID productId, UUID variantId, String productName, String variantName, String sku, BigDecimal depositUnitAmount, BigDecimal rentalPrice, long durationDays, Map<String, Object> rentalPriceSnapshot) {}
}
