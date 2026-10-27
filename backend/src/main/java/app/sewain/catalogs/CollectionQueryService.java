package app.sewain.catalogs;

import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;

import java.util.List;
import java.util.UUID;

interface CollectionQueryService {
	List<CollectionsResponse> findCategories();

	PageResponse<CollectionsResponse> findAll(PageQuery query);

	CollectionDetailResponse findBySlug(String slug);

	CollectionDetailResponse findById(UUID id);

	/**
	 * Produk yang tergabung di collection:
	 * MANUAL sesuai urutan pilihan, AUTOMATED hasil evaluasi rules.
	 */
	PageResponse<ProductResponse> findProducts(UUID id, PageQuery query);
}
