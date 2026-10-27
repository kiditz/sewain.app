package app.sewain.catalogs;

import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Catalogs")
class ProductQueryController {

	private final ProductQueryService productQueryService;

	@GetMapping
	PageResponse<ProductResponse> search(@RequestParam Map<String, String> params) {
		return productQueryService.search(PageQuery.from(params));
	}

	@GetMapping("/{id}")
	ProductResponse findById(@PathVariable UUID id) {
		return productQueryService.findById(id);
	}

	@GetMapping("/slug/{slug}")
	ProductResponse findBySlug(@PathVariable String slug) {
		return productQueryService.findBySlug(slug);
	}
}