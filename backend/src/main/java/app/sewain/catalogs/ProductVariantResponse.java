package app.sewain.catalogs;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

record ProductVariantResponse(
		UUID id,
		String name,
		String sku,
		List<String> mediaList,
		BigDecimal buyPrice,
		BigDecimal depositAmount,
		boolean active,
		List<ProductVariantValueResponse> values,
		List<VariantRentalPlanResponse> rentalPlans,
		List<InventoryUnitResponse> inventoryUnits,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}