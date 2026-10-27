package app.sewain.catalogs;

import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;

import java.util.UUID;
import java.util.List;

interface ProductQueryService {
    List<ProductTypeResponse> findTypes();
	PageResponse<ProductResponse> search(PageQuery q);

	ProductResponse findById(UUID id);

	ProductResponse findBySlug(String slug);
}