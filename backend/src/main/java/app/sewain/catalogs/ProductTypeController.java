package app.sewain.catalogs;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/product-types")
@RequiredArgsConstructor
class ProductTypeController {

	private final ProductTypeCommandService commandService;
	private final ProductTypeQueryService queryService;

	@GetMapping
	public List<ProductTypeResponse> find(
			@RequestParam(required = false) String search
	) {
		return queryService.find(search);
	}

	@GetMapping("/{id}")
	public ProductTypeResponse findById(@PathVariable UUID id) {
		return queryService.findById(id);
	}

	@PostMapping
	public ProductTypeResponse create(
			@Valid @RequestBody ProductTypeCommand command
	) {
		return commandService.create(command);
	}

	@PutMapping("/{id}")
	public ProductTypeResponse update(
			@PathVariable UUID id,
			@Valid @RequestBody ProductTypeCommand command
	) {
		return commandService.update(id, command);
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable UUID id) {
		commandService.delete(id);
	}
}