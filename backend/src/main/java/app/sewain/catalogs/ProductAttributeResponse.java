package app.sewain.catalogs;

import java.util.UUID;

public record ProductAttributeResponse(
        UUID attributeId,
        String attributeName,
        UUID attributeValueId,
        String attributeValue
) {
}