package app.sewain.catalogs;

import java.util.UUID;

interface InventoryCommandService {

	InventoryResponse create(InventoryCommand command);

	InventoryResponse update(UUID id, InventoryCommand command);

	/**
	 * Hanya unit yang tidak punya varian dan belum pernah masuk
	 * rental allocation yang boleh dihapus.
	 */
	void delete(UUID id);
}
