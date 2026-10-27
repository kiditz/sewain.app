package app.sewain.media;

import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;

interface MediaQueryService {

	PageResponse<MediaResponse> findAll(PageQuery query);
}