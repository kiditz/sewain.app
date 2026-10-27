package app.sewain.catalogs;

import java.util.List;
import java.util.UUID;

public record AttributeOptionResponse(
        UUID id,
        String name,
        List<AttributeValueOptionResponse> values
) {
}