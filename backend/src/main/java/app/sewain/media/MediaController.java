package app.sewain.media;

import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.core.io.Resource;
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

	@GetMapping("/{id}/file")
	ResponseEntity<Resource> findFile(@PathVariable UUID id) {
		var file = queryService.findFile(id);
		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
				.contentType(MediaType.parseMediaType(file.contentType()))
				.contentLength(file.size()).body(file.resource());
	}

	@PutMapping(value = "/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	MediaResponse replaceFile(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
		return commandService.replaceFile(id, file);
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
