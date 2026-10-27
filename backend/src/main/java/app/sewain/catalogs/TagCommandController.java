package app.sewain.catalogs;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
class TagCommandController {
    private final TagCommandService commandService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TagResponse create(@Valid @RequestBody TagCommand command) { return commandService.create(command); }

    @PutMapping("/{id}")
    TagResponse update(@PathVariable UUID id, @Valid @RequestBody TagCommand command) { return commandService.update(id, command); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) { commandService.delete(id); }
}
