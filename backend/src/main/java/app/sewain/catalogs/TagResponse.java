package app.sewain.catalogs;

import java.util.UUID;

record TagResponse(
		UUID id,
		String name,
		String slug
) {
}