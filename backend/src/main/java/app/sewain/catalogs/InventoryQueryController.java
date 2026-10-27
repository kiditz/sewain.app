package app.sewain.catalogs;

import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory")
class InventoryQueryController {

	private final InventoryQueryService queryService;

	@GetMapping
	PageResponse<InventoryResponse> search(
			@RequestParam Map<String, String> params
	) {
		var query = PageQuery.from(params);

		// PageQuery.from() belum membawa sortBy/direction ke field sort.
		var sortBy = params.get("sortBy");

		if (StringUtils.hasText(sortBy)) {
			var direction = StringUtils.hasText(params.get("direction"))
					? params.get("direction")
					: params.getOrDefault("sortDirection", "asc");

			query.setSort(List.of(Map.of(
					"key", sortBy,
					"order", direction
			)));
		}

		return queryService.search(query);
	}

	@GetMapping("/{id}")
	InventoryResponse findById(@PathVariable UUID id) {
		return queryService.findById(id);
	}
}
