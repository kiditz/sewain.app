package app.sewain.catalogs;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProductAttributeCommand(
        @NotNull
        UUID attributeValueId
) {
}