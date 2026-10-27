package app.sewain.media;

import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class MediaQueryServiceImpl implements MediaQueryService {
	private final MediaRepository mediaRepository;
	private final MediaMapper mapper;
	@Override
	public PageResponse<MediaResponse> findAll(PageQuery q) {
		var query = q == null ? PageQuery.defaultQuery() : q;
		Pageable pageable = PageRequest.of(
				query.pageIndex(),
				query.getSize(),
				Sort.by("createdAt").descending()
		);
		Page<Media> page = mediaRepository.findAll(
				MediaSpecifications.search(query.getSearch()),
				pageable
		);
		return new PageResponse<>(
				page.getContent()
						.stream()
						.map(mapper::toResponse)
						.toList(),
				page.getTotalElements()
		);
	}
}
