package app.sewain.catalogs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Command untuk membuat / mengubah satu unit inventaris.
 * <p>
 * {@code variantId} opsional: unit boleh belum dikaitkan ke varian produk.
 * {@code serialNumber} dibuat otomatis jika kosong.
 */
record InventoryCommand(
		@NotBlank(message = "inventory.unit.sku.notblank")
		@Size(max = 100, message = "inventory.unit.sku.size")
		String skuBarcode,

		@Size(max = 150, message = "inventory.unit.serial.size")
		String serialNumber,

		UUID variantId,

		@NotNull(message = "inventory.unit.status.notnull")
		InventoryUnitStatus status,

		String conditionNotes
) {
}
