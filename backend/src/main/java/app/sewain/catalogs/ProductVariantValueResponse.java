package app.sewain.catalogs;

import java.util.UUID;

record ProductVariantValueResponse(
        UUID attributeId,
        String attributeName,
        String attributeSlug,
        UUID attributeValueId,
        String value,
        String slug,
        int position
) {
}