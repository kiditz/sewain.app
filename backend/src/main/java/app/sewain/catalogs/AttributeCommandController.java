package app.sewain.catalogs;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/attributes")
@RequiredArgsConstructor
@Tag(name = "Catalogs")
class AttributeCommandController {

    private final AttributeCommandService attributeCommandService;

    @PostMapping
    AttributeResponse create(
            @Valid @RequestBody AttributeCommand command
    ) {
        return attributeCommandService.create(command);
    }

    @PutMapping("/{id}")
    AttributeResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody AttributeCommand command
    ) {
        return attributeCommandService.update(id, command);
    }

    @DeleteMapping("/{id}")
    void delete(
            @PathVariable UUID id
    ) {
        attributeCommandService.delete(id);
    }
}