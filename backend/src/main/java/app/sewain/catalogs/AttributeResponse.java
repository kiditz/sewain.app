package app.sewain.catalogs;

import java.util.List;
import java.util.UUID;

record AttributeResponse(
		UUID id,
		String name,
		String slug,
		String description,
		List<AttributeValueResponse> values
) {
}