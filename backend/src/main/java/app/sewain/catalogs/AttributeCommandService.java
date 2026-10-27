package app.sewain.catalogs;

import java.util.UUID;

interface AttributeCommandService {

    AttributeResponse create(AttributeCommand command);

    AttributeResponse update(
            UUID id,
            AttributeCommand command
    );

    void delete(UUID id);
}