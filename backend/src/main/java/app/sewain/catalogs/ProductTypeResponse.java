package app.sewain.catalogs;

import java.util.UUID;

record ProductTypeResponse(
		UUID id,
		String name,
		String slug
) {
}