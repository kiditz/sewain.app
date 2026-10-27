package app.sewain.catalogs;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
@Tag(name = "Collections")
class CollectionsCommandController {
	private final CollectionCommandService service;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	CollectionDetailResponse create(@Valid @RequestBody CollectionCommand command) {
		return service.create(command);
	}

	@PutMapping("/{id}")
	CollectionDetailResponse update(@PathVariable UUID id, @Valid @RequestBody CollectionCommand command) {
		return service.update(id, command);
	}

	@DeleteMapping("/{id}")
	void delete(@PathVariable UUID id) {
		service.delete(id);
	}

	@PutMapping("/reorder")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void reorder(
			@Valid @RequestBody CollectionReorderCommand command
	) {
		service.reorder(command);
	}

}
