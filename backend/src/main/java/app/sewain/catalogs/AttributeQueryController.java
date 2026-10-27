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
@RequestMapping("/api/attributes")
@RequiredArgsConstructor
@Tag(name = "Catalogs")
class AttributeQueryController {

	private final AttributeQueryService attributeQueryService;

	@GetMapping
	List<AttributeResponse> findAll() {
		return attributeQueryService.findAll();
	}

	@GetMapping("search")
	PageResponse<AttributeResponse> search(@RequestParam Map<String, String> params) {
		return attributeQueryService.findAll(PageQuery.from(params));
	}

	@GetMapping("/{id}")
	AttributeResponse findById(
			@PathVariable UUID id
	) {
		return attributeQueryService.findById(id);
	}

	@GetMapping("/options")
	List<AttributeOptionResponse> findOptions() {
		return attributeQueryService.findOptions();
	}
}