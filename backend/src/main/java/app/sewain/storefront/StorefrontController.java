package app.sewain.storefront;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/storefront")
@RequiredArgsConstructor
class StorefrontController {
    private final StorefrontService service;
    private final ThemeLibraryService themes;

    @GetMapping
    StorefrontResponse get(@RequestParam(required = false) String themeId) { return document(themes.draft(themeId)); }

    @GetMapping("/published")
    StorefrontResponse getPublished() { return document(themes.live()); }

    @GetMapping("/pages")
    StorefrontResponse pages(@RequestParam(required = false) String themeId) { return themes.resource(themeId, "pages"); }

    @PutMapping("/pages/{id}")
    StorefrontResponse savePage(@PathVariable String id, @Valid @RequestBody StorefrontCommand command, @RequestParam(required = false) String themeId) {
        StorefrontService.resourceKey("PAGE:", id);
        return themes.saveResource(themeId, "page:" + id, command.configuration());
    }

    @DeleteMapping("/pages/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    void deletePage(@PathVariable String id, @RequestParam(required = false) String themeId) { themes.deletePage(themeId, id); }

    @GetMapping("/theme")
    StorefrontResponse theme(@RequestParam(required = false) String themeId) { return themes.resource(themeId, "theme"); }

    @PutMapping("/theme")
    StorefrontResponse saveTheme(@Valid @RequestBody StorefrontCommand command, @RequestParam(required = false) String themeId) { return themes.saveResource(themeId, "theme", command.configuration()); }

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

    private StorefrontResponse document(tools.jackson.databind.JsonNode document) {
        var result = document.deepCopy().asObject();
        result.set("MenuItems", service.getMenus().configuration());
        result.set("headerMenuId", service.getResource("NAVIGATION").configuration().path("headerMenuId"));
        return new StorefrontResponse(result, null);
    }
}
