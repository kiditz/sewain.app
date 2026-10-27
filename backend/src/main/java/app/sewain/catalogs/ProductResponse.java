package app.sewain.catalogs;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

record ProductResponse(
		UUID id,
		ProductTypeResponse type,
        List<TagResponse> tags,
		String name,
		String slug,
		String media,
		String description,
		ProductStatus status,
		@JsonInclude(JsonInclude.Include.NON_EMPTY)
		List<ProductVariantResponse> variants,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}