package app.sewain.media;

import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Tag(name = "Media")
class MediaController {

	private final MediaCommandService commandService;
	private final MediaQueryService queryService;

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ResponseEntity<List<MediaResponse>> upload(@RequestPart("files") List<MultipartFile> files) {
		return ResponseEntity.ok(commandService.upload(files));
	}

	@GetMapping
	PageResponse<MediaResponse> findAll(@RequestParam Map<String, String> params) {
		return queryService.findAll(PageQuery.from(params));
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@PathVariable UUID id) {
		commandService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}")
	MediaResponse updateFilename(@PathVariable UUID id, @Valid @RequestBody UpdateMediaFilenameCommand command) {
		return commandService.updateFilename(id, command);
	}
}
