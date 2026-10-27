package app.sewain.catalogs;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class ProductQueryServiceImpl implements ProductQueryService {

	private final ProductRepository productRepository;
	private final ProductMapper mapper;
    private final ProductTypeMapper typeMapper;


    @Override
    @Transactional(readOnly = true)
    public java.util.List<ProductTypeResponse> findTypes() {
        return typeMapper.toResponses(productRepository.findTypesByStatus(ProductStatus.ACTIVE));
    }

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ProductResponse> search(PageQuery q) {
		var query = q == null ? PageQuery.defaultQuery() : q;
		var sort = ProductSpecifications.toSort(query);

		var pageable = PageRequest.of(
				query.pageIndex(),
				query.getSize(),
				sort
		);

		var page = productRepository.findAll(
				ProductSpecifications.search(query.getSearch(), query.getSort()),
				pageable
		);

		return new PageResponse<>(
				mapper.toResponses(page.getContent()),
				page.getTotalElements()
		);
	}

	@Override
	@Transactional(readOnly = true)
	public ProductResponse findById(UUID id) {

		var product = productRepository.findById(id)
				.orElseThrow(() ->
						new ResourceNotFoundException(
								ErrorCode.PRODUCT_NOT_FOUND
						)
				);

		return mapper.toResponse(product);
	}

	@Override
	@Transactional(readOnly = true)
	public ProductResponse findBySlug(String slug) {

		var product = productRepository.findBySlug(slug)
				.orElseThrow(() ->
						new ResourceNotFoundException(
								ErrorCode.PRODUCT_NOT_FOUND
						)
				);

		return mapper.toResponse(product);
	}
}
