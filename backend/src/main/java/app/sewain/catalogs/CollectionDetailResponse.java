package app.sewain.catalogs;

import java.util.List;
import java.util.UUID;

/**
 * Detail collection: ringkasan + rules (AUTOMATED) + id produk manual (MANUAL).
 */
record CollectionDetailResponse(
		UUID id,
		String name,
		String slug,
		String imageUrl,
		UUID parentId,
		String description,
		CollectionType type,
		CollectionMatchCondition matchCondition,
		List<CollectionRuleResponse> rules,
		List<UUID> productIds
) {
}
