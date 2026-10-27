package app.sewain.catalogs;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class ProductTypeQueryServiceImpl implements ProductTypeQueryService {

	private final ProductTypeRepository repository;
	private final ProductTypeMapper mapper;

	@Override
	@Transactional(readOnly = true)
	public List<ProductTypeResponse> find(String search) {
		return repository.findAll(ProductTypeSpecification.search(search), Pageable.ofSize(50))
				.stream()
				.map(mapper::toResponse)
				.toList();

	}

	@Override
	@Transactional(readOnly = true)
	public ProductTypeResponse findById(UUID id) {
		ProductType entity = repository.findById(id)
				.orElseThrow(() ->
						new ResourceNotFoundException(
								ErrorCode.PRODUCT_TYPE_NOT_FOUND
						)
				);

		return mapper.toResponse(entity);
	}
}