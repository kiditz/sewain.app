package app.sewain.catalogs;

import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;

import java.util.UUID;

interface InventoryQueryService {

	PageResponse<InventoryResponse> search(PageQuery query);

	InventoryResponse findById(UUID id);
}