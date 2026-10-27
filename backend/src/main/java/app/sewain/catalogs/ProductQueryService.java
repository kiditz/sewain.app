package app.sewain.catalogs;

import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;

import java.util.UUID;

interface ProductQueryService {
	PageResponse<ProductResponse> search(PageQuery q);

	ProductResponse findById(UUID id);

	ProductResponse findBySlug(String slug);
}