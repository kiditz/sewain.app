package app.sewain.catalogs;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class TagQueryServiceImpl implements TagQueryService {

	private final TagRepository repository;
	private final TagMapper mapper;

	@Override
	@Transactional(readOnly = true)
	public List<TagResponse> find(String search) {
		return repository.findAll(TagSpecification.search(search), PageRequest.of(0, 50, Sort.by("name").ascending().and(Sort.by("id"))))
				.stream()
				.map(mapper::toResponse)
				.toList();

	}

	@Override
	@Transactional(readOnly = true)
	public TagResponse findById(UUID id) {
		Tag entity = repository.findById(id)
				.orElseThrow(() ->
						new ResourceNotFoundException(
								ErrorCode.TAG_NOT_FOUND
						)
				);

		return mapper.toResponse(entity);
	}
}