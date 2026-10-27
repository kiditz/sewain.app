package app.sewain.catalogs;


import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProductVariantValueCommand(
        @NotNull(message = "product.variant.value.attributeValueId.notnull")
        UUID attributeValueId
) {
}
