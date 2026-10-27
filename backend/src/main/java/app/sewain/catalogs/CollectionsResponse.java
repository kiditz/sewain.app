package app.sewain.catalogs;

import java.util.UUID;

/**
 * Ringkasan collection (daftar / tree). Tidak memuat rules & items
 * supaya query daftar tetap ringan.
 */
record CollectionsResponse(
		UUID id,
		String name,
		String slug,
		String imageUrl,
		UUID parentId,
		String description,
		CollectionType type,
		CollectionMatchCondition matchCondition,
		/* Jumlah produk manual. Untuk AUTOMATED selalu 0 (dihitung dari rule). */
		Long productCount
) {
}
