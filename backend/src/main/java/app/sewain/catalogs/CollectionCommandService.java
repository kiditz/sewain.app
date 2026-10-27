package app.sewain.catalogs;

import java.util.UUID;

interface CollectionCommandService {
	CollectionDetailResponse create(CollectionCommand command);

	CollectionDetailResponse update(UUID id, CollectionCommand command);

	void reorder(CollectionReorderCommand command);

	void delete(UUID id);
}
