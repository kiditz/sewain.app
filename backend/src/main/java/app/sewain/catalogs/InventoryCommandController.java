package app.sewain.catalogs;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory")
class InventoryCommandController {

	private final InventoryCommandService inventoryCommandService;

	@PostMapping
	InventoryResponse create(
			@Valid @RequestBody InventoryCommand command
	) {
		return inventoryCommandService.create(command);
	}

	@PutMapping("/{id}")
	InventoryResponse update(
			@PathVariable UUID id,
			@Valid @RequestBody InventoryCommand command
	) {
		return inventoryCommandService.update(id, command);
	}

	@DeleteMapping("/{id}")
	void delete(@PathVariable UUID id) {
		inventoryCommandService.delete(id);
	}
}
