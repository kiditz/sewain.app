package app.sewain.catalogs;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class AttributeQueryServiceImpl
		implements AttributeQueryService {

	private final AttributeRepository attributeRepository;
	private final AttributeMapper mapper;

	@Override
	@Transactional(readOnly = true)
	public List<AttributeResponse> findAll() {

		return attributeRepository.findAll()
				.stream()
				.map(mapper::toResponse)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<AttributeOptionResponse> findOptions() {
		return attributeRepository.findAll()
				.stream()
				.map(mapper::toOptionResponse)
				.toList();

	}

	@Override
	public PageResponse<AttributeResponse> findAll(PageQuery q) {
		var query = q == null ? PageQuery.defaultQuery() : q;

		Pageable pageable = PageRequest.of(
				query.pageIndex(),
				query.getSize(),
				AttributeSpecifications.toSort(query)
		);

		Page<Attribute> page = attributeRepository.findAll(
				AttributeSpecifications.search(query.getSearch()),
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

	@Override
	@Transactional(readOnly = true)
	public AttributeResponse findById(UUID id) {

		var attribute = findAttribute(id);

		return mapper.toResponse(attribute);
	}

	private Attribute findAttribute(UUID id) {

		return attributeRepository.findById(id)
				.orElseThrow(() ->
						new ResourceNotFoundException(
								ErrorCode.ATTRIBUTE_NOT_FOUND
						)
				);
	}
}