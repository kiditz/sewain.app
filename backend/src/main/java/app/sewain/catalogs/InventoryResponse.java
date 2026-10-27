package app.sewain.catalogs;

import java.time.LocalDateTime;
import java.util.UUID;

record InventoryResponse(
		UUID id,
		UUID variantId,
		UUID productId,
		String productName,
		String variantName,
		String variantSku,
		String skuBarcode,
		String serialNumber,
		InventoryUnitStatus status,
		String conditionNotes,
		/*
		 * true jika unit boleh dihapus:
		 * tidak punya varian dan belum pernah masuk rental allocation.
		 */
		boolean deletable,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
