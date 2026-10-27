
package app.sewain.catalogs;

import java.time.LocalDateTime;
import java.util.UUID;

record InventoryUnitResponse(
		UUID id,
		String skuBarcode,
		String serialNumber,
		InventoryUnitStatus status,
		String conditionNotes,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
