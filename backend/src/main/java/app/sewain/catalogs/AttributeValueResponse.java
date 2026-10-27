package app.sewain.catalogs;

import java.util.UUID;

record AttributeValueResponse(
		UUID id,
		String value,
		String slug
) {
}