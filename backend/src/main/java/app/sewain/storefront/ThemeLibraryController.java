package app.sewain.storefront;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/storefront/themes")
@RequiredArgsConstructor
class ThemeLibraryController {
    private final ThemeLibraryService service;
    @GetMapping JsonNode list() { return service.list(); }
    @PostMapping JsonNode install(@RequestBody JsonNode request) { return service.install(request); }
    @GetMapping("/{id}") JsonNode draft(@PathVariable String id) { return service.draft(id); }
    @PostMapping("/{id}/duplicate") JsonNode duplicate(@PathVariable String id) { return service.duplicate(id); }
    record Rename(String name) {}
    @PatchMapping("/{id}") void rename(@PathVariable String id, @RequestBody Rename request) { service.rename(id, request.name() == null ? "" : request.name()); }
    @DeleteMapping("/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    void delete(@PathVariable String id) { service.delete(id); }
    @PostMapping("/{id}/publish") void publish(@PathVariable String id) { service.publish(id); }
    @PostMapping("/{id}/rollback/{revision}") void rollback(@PathVariable String id, @PathVariable int revision) { service.rollback(id, revision); }
}
