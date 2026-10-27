package app.sewain.catalogs;

import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
@Tag(name = "Collections")
public class CollectionQueryController {
	private final CollectionQueryService service;

	@GetMapping
	List<CollectionsResponse> findCollections() {
		return service.findCategories();
	}

	@GetMapping("/{slug}")
	CollectionDetailResponse findBySlug(@PathVariable String slug) {
		return service.findBySlug(slug);
	}

	@GetMapping("/id/{id}")
	CollectionDetailResponse findById(@PathVariable UUID id) {
		return service.findById(id);
	}

	/**
	 * Produk di dalam collection (MANUAL: sesuai urutan, AUTOMATED: hasil rules).
	 * Query: page, size, keyword.
	 */
	@GetMapping("/{id}/products")
	PageResponse<ProductResponse> findProducts(
			@PathVariable UUID id,
			@RequestParam Map<String, String> params
	) {
		return service.findProducts(id, PageQuery.from(params));
	}

	@GetMapping("/search")
	PageResponse<CollectionsResponse> search(PageQuery query) {
		return service.findAll(query);
	}
}
