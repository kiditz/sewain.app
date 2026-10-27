package app.sewain.storefront;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

import java.util.Set;

@Service
@RequiredArgsConstructor
class StorefrontService {
    private final StorefrontRepository repository;
    private final ObjectMapper json;

    @Transactional(readOnly = true)
    StorefrontResponse get() {
        var configuration = json.createObjectNode();
        configuration.set("pages", list("PAGE:"));
        configuration.set("theme", getResource("THEME").configuration());
        configuration.set("MenuItems", list("MENU:"));
        var navigation = getResource("NAVIGATION").configuration();
        configuration.putNull("headerMenuId");
        if (navigation.has("headerMenuId")) configuration.set("headerMenuId", navigation.get("headerMenuId"));
        return new StorefrontResponse(configuration, null);
    }

    @Transactional(readOnly = true)
    StorefrontResponse getPublished() {
        var configuration = get().configuration().deepCopy().asObject();
        var published = json.createArrayNode();
        configuration.path("pages").asArray().elements().forEach(page -> {
            if ("PUBLISHED".equals(page.path("status").asString())) published.add(page);
        });
        configuration.set("pages", published);
        return new StorefrontResponse(configuration, null);
    }

    @Transactional(readOnly = true)
    StorefrontResponse getPages() { ensureMigrated(); return new StorefrontResponse(list("PAGE:"), null); }

    @Transactional(readOnly = true)
    StorefrontResponse getMenus() { ensureMigrated(); return new StorefrontResponse(list("MENU:"), null); }

    @Transactional(readOnly = true)
    StorefrontResponse getResource(String key) {
        ensureMigrated();
        return repository.findByKey(key)
                .map(entity -> new StorefrontResponse(json.readTree(entity.getConfiguration()), entity.getUpdatedAt()))
                .orElseGet(() -> new StorefrontResponse(json.createObjectNode(), null));
    }

    private ArrayNode list(String prefix) {
        var result = json.createArrayNode();
        repository.findAllByKeyStartingWithOrderByCreatedAtAsc(prefix)
                .forEach(entity -> result.add(json.readTree(entity.getConfiguration())));
        return result;
    }

    @Transactional
    StorefrontResponse saveResource(String key, StorefrontCommand command) {
        ensureMigrated();
        var configuration = command.configuration();
        require(configuration != null && configuration.isObject());
        if (key.startsWith("PAGE:")) validatePage(key.substring(5), configuration);
        if (key.startsWith("MENU:")) configuration = normalizeMenu(key.substring(5), configuration);
        if ("NAVIGATION".equals(key)) {
            var menuId = configuration.get("headerMenuId");
            require(menuId != null && (menuId.isNull() || (menuId.isString()
                    && repository.findByKey(resourceKey("MENU:", menuId.asString())).isPresent())));
        }
        var entity = repository.findByKey(key).orElseGet(StorefrontConfiguration::new);
        entity.setKey(key);
        entity.setConfiguration(configuration.toString());
        var saved = repository.save(entity);
        return new StorefrontResponse(configuration, saved.getUpdatedAt());
    }

    @Transactional
    void deletePage(String id) {
        var key = resourceKey("PAGE:", id);
        var entity = repository.findByKey(key).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        require("CUSTOM".equals(json.readTree(entity.getConfiguration()).path("kind").asString()));
        repository.delete(entity);
        for (var menu : repository.findAllByKeyStartingWithOrderByCreatedAtAsc("MENU:")) {
            var config = json.readTree(menu.getConfiguration()).asObject();
            var items = json.createArrayNode();
            config.path("items").asArray().elements().forEach(item -> {
                if (!id.equals(item.path("pageId").asString())) items.add(item);
            });
            config.set("items", items);
            menu.setConfiguration(config.toString());
            repository.save(menu);
        }
    }

    @Transactional
    void deleteMenu(String id) {
        var entity = repository.findByKey(resourceKey("MENU:", id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        repository.delete(entity);
        repository.findByKey("NAVIGATION").ifPresent(navigation -> {
            var config = json.readTree(navigation.getConfiguration()).asObject();
            if (id.equals(config.path("headerMenuId").asString())) {
                config.putNull("headerMenuId");
                navigation.setConfiguration(config.toString());
                repository.save(navigation);
            }
        });
    }

    static String resourceKey(String prefix, String id) {
        require(id != null && id.matches("[a-zA-Z0-9_-]{1,45}"));
        return prefix + id;
    }

    private void validatePage(String id, JsonNode page) {
        require(id.equals(page.path("id").asString()));
        require(!page.path("title").asString("").isBlank());
        require(!page.path("slug").asString("").isBlank());
        require(Set.of("HOME", "CUSTOM", "PRODUCT", "PRODUCT_GRID", "COLLECTIONS", "CART", "CHECKOUT").contains(page.path("kind").asString("")));
        require(Set.of("DRAFT", "PUBLISHED").contains(page.path("status").asString("")));
        require(page.path("sections").isArray());
        for (var existing : repository.findAllByKeyStartingWithOrderByCreatedAtAsc("PAGE:")) {
            var other = json.readTree(existing.getConfiguration());
            if (id.equals(other.path("id").asString())) continue;
            require(!page.path("slug").asString().equals(other.path("slug").asString()));
            require("CUSTOM".equals(page.path("kind").asString()) || !page.path("kind").asString().equals(other.path("kind").asString()));
        }
    }

    private JsonNode normalizeMenu(String id, JsonNode menu) {
        require(id.equals(menu.path("id").asString()));
        require(!menu.path("name").asString("").isBlank() && menu.path("items").isArray());
        var normalized = json.createObjectNode();
        normalized.put("id", id);
        normalized.put("name", menu.path("name").asString().trim());
        var items = json.createArrayNode();
        menu.path("items").asArray().elements().forEach(item -> {
            require(!item.path("id").asString("").isBlank());
            var reference = json.createObjectNode();
            reference.put("id", item.path("id").asString());
            if (item.hasNonNull("target")) {
                var target = item.path("target");
                require(target.isObject());
                require(Set.of("PRODUCT", "COLLECTION").contains(target.path("type").asString("")));
                for (var field : Set.of("id", "title", "slug")) {
                    require(target.path(field).isString() && !target.path(field).asString().isBlank());
                }
                require(target.path("slug").asString().matches("[a-zA-Z0-9_-]+"));
                var destination = json.createObjectNode();
                for (var field : Set.of("type", "id", "title", "slug")) destination.put(field, target.path(field).asString());
                reference.set("target", destination);
            } else {
                var pageId = item.path("pageId").asString("");
                var page = repository.findByKey(resourceKey("PAGE:", pageId))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Menu page does not exist"));
                require(!"PRODUCT".equals(json.readTree(page.getConfiguration()).path("kind").asString()));
                reference.put("pageId", pageId);
            }
            var label = item.get("label");
            if (label != null && !label.isNull()) {
                require(label.isString());
                if (!label.asString().isBlank()) reference.put("label", label.asString().trim());
            }
            items.add(reference);
        });
        normalized.set("items", items);
        return normalized;
    }

    private void ensureMigrated() {
        if (repository.findByKey("STOREFRONT").isPresent() && repository.findByKey("NAVIGATION").isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Run storefront tenant migration V29 before editing");
        }
    }

    private static void require(boolean condition) {
        if (!condition) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storefront configuration");
    }
}
