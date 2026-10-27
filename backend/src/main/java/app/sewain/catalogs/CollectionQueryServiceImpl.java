package app.sewain.catalogs;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class CollectionQueryServiceImpl implements CollectionQueryService {
	private final CollectionRepository collectionRepository;
	private final CollectionItemRepository collectionItemRepository;
	private final ProductRepository productRepository;
	private final CollectionMapper mapper;
	private final ProductMapper productMapper;

	@Override
	@Transactional(readOnly = true)
	public List<CollectionsResponse> findCategories() {
		Sort sort = Sort.by(
				Sort.Order.asc("parent.position").nullsFirst(),
				Sort.Order.asc("position")
		);
		return mapper.toResponses(collectionRepository.findAll(sort));
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<CollectionsResponse> findAll(PageQuery q) {
		var query = q == null ? PageQuery.defaultQuery() : q;


		var pageable = PageRequest.of(
				query.pageIndex(),
				query.getSize(),
				CollectionSpecifications.toSort(query)
		);

		var page = collectionRepository.findAll(
				CollectionSpecifications.search(null),
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
	public CollectionDetailResponse findBySlug(String slug) {
		var category = collectionRepository.findBySlug(slug).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COLLECTION_NOT_FOUND));
		return mapper.toDetail(category);
	}

	@Override
	@Transactional(readOnly = true)
	public CollectionDetailResponse findById(UUID id) {
		return mapper.toDetail(getCollection(id));
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ProductResponse> findProducts(UUID id, PageQuery q) {
		var query = q == null ? PageQuery.defaultQuery() : q;
		var collection = getCollection(id);
		var keyword = query.getSearch().get("keyword");
        var statusValue = query.getSearch().get("status");
        ProductStatus status;
        java.util.List<UUID> ids;
        try {
            status = statusValue == null || statusValue.isBlank() ? null : ProductStatus.valueOf(statusValue);
            var idValue = query.getSearch().get("id");
            ids = idValue == null || idValue.isBlank() ? java.util.List.of() :
                    java.util.Arrays.stream(idValue.split(",")).map(String::trim).map(UUID::fromString).toList();
        } catch (IllegalArgumentException ex) {
            throw new app.sewain.exception.ValidationException("Invalid collection product filter");
        }

		Page<Product> page;

		if (collection.getType() == CollectionType.MANUAL) {
			page = collectionItemRepository.findProducts(
					id,
					toLikePattern(keyword),
                    status, !ids.isEmpty(), ids.isEmpty() ? java.util.List.of(new UUID(0, 0)) : ids,
                    Boolean.parseBoolean(query.getSearch().get("available")),
					PageRequest.of(query.pageIndex(), query.getSize())
			);
		} else {
			var specification = CollectionRuleSpecifications
					.matching(collection)
					.and(ProductSpecifications.search(
							query.getSearch()
					));

			page = productRepository.findAll(
					specification,
					PageRequest.of(
							query.pageIndex(),
							query.getSize(),
							Sort.by("name").ascending().and(Sort.by("id"))
					)
			);
		}

		return new PageResponse<>(
				productMapper.toResponses(page.getContent()),
				page.getTotalElements()
		);
	}

	private Collection getCollection(UUID id) {
		return collectionRepository.findById(id)
				.orElseThrow(() ->
						new ResourceNotFoundException(ErrorCode.COLLECTION_NOT_FOUND)
				);
	}

	/** Pola LIKE huruf kecil dengan escape '!' ("%" jika keyword kosong). */
	private String toLikePattern(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return "%";
		}

		var escaped = keyword.trim()
				.toLowerCase(Locale.ROOT)
				.replace("!", "!!")
				.replace("%", "!%")
				.replace("_", "!_");

		return "%" + escaped + "%";
	}
}
