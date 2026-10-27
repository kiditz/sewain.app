package app.sewain.catalogs;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record CollectionReorderCommand(
        @NotEmpty
        List<UUID> collectionIds
) {}