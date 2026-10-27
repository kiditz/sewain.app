package app.sewain.catalogs;

import java.util.UUID;

interface TagCommandService {

    TagResponse create(TagCommand command);

    TagResponse update(UUID id, TagCommand command);

    void delete(UUID id);
}