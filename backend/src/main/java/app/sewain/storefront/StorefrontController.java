package app.sewain.storefront;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/storefront")
@RequiredArgsConstructor
class StorefrontController {
    private final StorefrontService service;

    @GetMapping
    StorefrontResponse get() { return service.get(); }

    @GetMapping("/published")
    StorefrontResponse getPublished() { return service.getPublished(); }

    @GetMapping("/pages")
    StorefrontResponse pages() { return service.getPages(); }

    @PutMapping("/pages/{id}")
    StorefrontResponse savePage(@PathVariable String id, @Valid @RequestBody StorefrontCommand command) {
        return service.saveResource(StorefrontService.resourceKey("PAGE:", id), command);
    }

    @DeleteMapping("/pages/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    void deletePage(@PathVariable String id) { service.deletePage(id); }

    @GetMapping("/theme")
    StorefrontResponse theme() { return service.getResource("THEME"); }

    @PutMapping("/theme")
    StorefrontResponse saveTheme(@Valid @RequestBody StorefrontCommand command) { return service.saveResource("THEME", command); }

    @GetMapping("/menus")
    StorefrontResponse menus() { return service.getMenus(); }

    @PutMapping("/menus/{id}")
    StorefrontResponse saveMenu(@PathVariable String id, @Valid @RequestBody StorefrontCommand command) {
        return service.saveResource(StorefrontService.resourceKey("MENU:", id), command);
    }

    @DeleteMapping("/menus/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    void deleteMenu(@PathVariable String id) { service.deleteMenu(id); }

    @GetMapping("/navigation")
    StorefrontResponse navigation() { return service.getResource("NAVIGATION"); }

    @PutMapping("/navigation")
    StorefrontResponse saveNavigation(@Valid @RequestBody StorefrontCommand command) { return service.saveResource("NAVIGATION", command); }
}
