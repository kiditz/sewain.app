package app.sewain.catalogs;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
class TagQueryController {
    private final TagQueryService queryService;

    @GetMapping
    List<TagResponse> find(@RequestParam(required = false) String search) { return queryService.find(search); }

    @GetMapping("/{id}")
    TagResponse findById(@PathVariable UUID id) { return queryService.findById(id); }
}
