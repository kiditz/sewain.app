package app.sewain.catalogs;

import java.util.List;
import java.util.UUID;

interface TagQueryService {

    List<TagResponse> find(String search);

    TagResponse findById(UUID id);
}