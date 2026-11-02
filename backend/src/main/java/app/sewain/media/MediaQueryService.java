package app.sewain.media;

import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;
import java.util.UUID;

interface MediaQueryService {

	PageResponse<MediaResponse> findAll(PageQuery query);

	MediaFileResponse findFile(UUID id);
}
