
package app.sewain.catalogs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

record InventoryUnitCommand(
		UUID id,
		@NotBlank
		@Size(max = 100)
		String skuBarcode,

		@Size(max = 150)
		String serialNumber,

		InventoryUnitStatus status,

		String conditionNotes
) {
}
