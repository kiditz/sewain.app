package app.sewain.catalogs;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Catalogs")
class ProductCommandController {

    private final ProductCommandService productCommandService;

    @PostMapping
    ProductResponse create(
            @Valid @RequestBody ProductCommand command
    ) {
        return productCommandService.create(command);
    }

    @PutMapping("/{id}")
    ProductResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductCommand command
    ) {
        return productCommandService.update(id, command);
    }

    @DeleteMapping("/{id}")
    void delete(@PathVariable UUID id) {
        productCommandService.delete(id);
    }
}