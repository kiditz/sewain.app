package app.sewain.storefront;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
class ThemeLibraryService {
    private final StorefrontRepository repository;
    private final ObjectMapper json;

    JsonNode list() {
        var active = state(false).path("activeId").asString();
        var items = json.createArrayNode();
        var content = repository.findAllByKeyStartingWithOrderByCreatedAtAsc("CONTENT_PAGE:");
        repository.findAllByKeyStartingWithOrderByCreatedAtAsc("THM:").forEach(row -> {
            var theme = parse(row);
            var item = json.createObjectNode();
            for (var field : Set.of("id", "name", "source", "version", "schemaVersion", "revision")) item.set(field, theme.path(field));
            item.put("active", active.equals(theme.path("id").asString()));
            var draft = effectiveDraft(theme, content);
            item.put("hasChanges", !published(draft).equals(theme.path("published")));
            item.put("homeSlug", draft.path("pages").asArray().valueStream().filter(page -> "HOME".equals(page.path("kind").asString())).findFirst().map(page -> page.path("slug").asString()).orElse("home"));
            item.set("colors", theme.path("draft").path("theme").path("colors"));
            var history = json.createArrayNode();
            theme.path("history").asArray().valueStream().forEach(revision -> {
                var summary = json.createObjectNode();
                summary.set("revision", revision.path("revision"));
                summary.set("createdAt", revision.path("createdAt"));
                history.add(summary);
            });
            item.set("history", history);
            items.add(item);
        });
        return items;
    }

    JsonNode draft(String id) {
        return effectiveDraft(instance(resolve(id)), repository.findAllByKeyStartingWithOrderByCreatedAtAsc("CONTENT_PAGE:"));
    }
    private JsonNode effectiveDraft(JsonNode instance, java.util.List<StorefrontConfiguration> content) {
        var document = instance.path("draft").deepCopy().asObject();
        var pages = json.createArrayNode();
        document.path("pages").asArray().valueStream().filter(page -> !"CUSTOM".equals(page.path("kind").asString())).forEach(pages::add);
        content.forEach(row -> pages.add(json.readTree(row.getConfiguration())));
        document.set("pages", pages);
        normalizeGlobals(pages, pages.valueStream().filter(page -> "HOME".equals(page.path("kind").asString())).findFirst().orElse(null));
        return document;
    }
    JsonNode live() {
        var theme = instance(resolve(null));
        return theme.path("published").isObject() ? theme.path("published") : json.createObjectNode();
    }
    StorefrontResponse resource(String id, String key) {
        var resolved = resolve(id);
        var document = draft(resolved);
        return new StorefrontResponse(document.path(key), null, resolved);
    }

    @Transactional
    JsonNode install(JsonNode request) {
        state(true);
        require(repository.findAllByKeyStartingWithOrderByCreatedAtAsc("THM:").size() < 50, "theme.limit");
        require(request.path("schemaVersion").asInt(1) == 1, "theme.unsupportedVersion");
        var document = request.path("configuration");
        validateDocument(document);
        var id = UUID.randomUUID().toString();
        var instance = json.createObjectNode();
        instance.put("id", id);
        instance.put("name", name(request.path("name").asString("")));
        instance.put("source", request.path("source").asString("custom"));
        instance.put("version", "1.0.0");
        instance.put("schemaVersion", 1);
        instance.put("revision", 0);
        instance.set("draft", document.deepCopy());
        instance.set("history", json.createArrayNode());
        write("THM:" + id, instance);
        return instance;
    }

    @Transactional
    JsonNode duplicate(String id) {
        var original = instance(id);
        var request = json.createObjectNode();
        request.put("name", original.path("name").asString().substring(0, Math.min(95, original.path("name").asString().length())) + " copy");
        request.put("source", original.path("source").asString());
        request.set("configuration", draft(id));
        return install(request);
    }

    @Transactional
    void rename(String id, String name) {
        state(true);
        var instance = instance(id);
        instance.put("name", name(name));
        write(key(id), instance);
    }

    @Transactional
    void delete(String id) {
        require(!state(true).path("activeId").asString().equals(id), "theme.activeCannotDelete");
        repository.delete(row(id));
    }

    @Transactional
    void publish(String id) {
        state(true);
        var instance = instance(id);
        var document = draft(id);
        validateDocument(document);
        require(document.path("pages").asArray().valueStream().anyMatch(page -> "HOME".equals(page.path("kind").asString()) && "PUBLISHED".equals(page.path("status").asString())), "theme.homeRequired");
        publishSnapshot(id, instance, published(document));
    }

    @Transactional
    void rollback(String id, int revision) {
        state(true);
        var instance = instance(id);
        var snapshot = instance.path("history").asArray().valueStream().filter(entry -> entry.path("revision").asInt() == revision)
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "theme.revisionNotFound"));
        publishSnapshot(id, instance, snapshot.path("configuration").deepCopy());
    }

    private void publishSnapshot(String id, ObjectNode instance, JsonNode document) {
        var history = instance.path("history").deepCopy().asArray();
        // Capture the previous live snapshot once, including the migrated storefront.
        if (history.isEmpty() && instance.path("published").isObject()) {
            var previous = json.createObjectNode();
            previous.set("revision", instance.path("revision"));
            previous.put("createdAt", Instant.now().toString());
            previous.set("configuration", instance.path("published").deepCopy());
            history.add(previous);
        }
        int revision = instance.path("revision").asInt(0) + 1;
        var entry = json.createObjectNode();
        entry.put("revision", revision);
        entry.put("createdAt", Instant.now().toString());
        entry.set("configuration", document.deepCopy());
        history.add(entry);
        while (history.size() > 10) history.remove(0);
        instance.set("history", history);
        instance.set("published", document.deepCopy());
        instance.put("revision", revision);
        write(key(id), instance);
        var state = state(false);
        state.put("activeId", id);
        write("THEME_LIBRARY", state);
    }

    @Transactional
    StorefrontResponse saveResource(String id, String resource, JsonNode configuration) {
        state(true);
        id = resolve(id);
        require(configuration.isObject(), "theme.invalidConfiguration");
        var instance = instance(id);
        var document = draft(id).deepCopy().asObject();
        if (resource.equals("theme")) {
            validateTheme(configuration);
            document.set("theme", configuration.deepCopy());
        } else {
            var pageId = resource.substring("page:".length());
            require(pageId.equals(configuration.path("id").asString()), "theme.invalidPage");
            var pages = document.path("pages").deepCopy().asArray();
            int index = -1;
            for (int i = 0; i < pages.size(); i++) if (pageId.equals(pages.get(i).path("id").asString())) index = i;
            if (index < 0) pages.add(configuration.deepCopy()); else pages.set(index, configuration.deepCopy());
            document.set("pages", pages);
            validatePages(pages);
            var source = index >= 0 ? configuration : pages.valueStream().filter(page -> "HOME".equals(page.path("kind").asString())).findFirst().orElse(null);
            normalizeGlobals(pages, source);
            if ("CUSTOM".equals(configuration.path("kind").asString())) write("CONTENT_PAGE:" + pageId, configuration);
        }
        require(document.toString().length() <= 4_000_000, "theme.configurationTooLarge");
        instance.set("draft", document);
        write(key(id), instance);
        return new StorefrontResponse(configuration, null);
    }

    @Transactional
    void deletePage(String id, String pageId) {
        state(true);
        id = resolve(id);
        var instance = instance(id);
        var document = draft(id).deepCopy().asObject();
        var pages = json.createArrayNode();
        boolean found = false;
        for (var page : document.path("pages")) {
            if (pageId.equals(page.path("id").asString())) {
                require("CUSTOM".equals(page.path("kind").asString()), "theme.requiredPage");
                found = true;
            } else pages.add(page);
        }
        if (!found) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "theme.pageNotFound");
        document.set("pages", pages);
        repository.findByKey("CONTENT_PAGE:" + pageId).ifPresent(repository::delete);
        require(document.toString().length() <= 4_000_000, "theme.configurationTooLarge");
        instance.set("draft", document);
        write(key(id), instance);
    }


    private void normalizeGlobals(ArrayNode pages, JsonNode source) {
        if (source == null) return;
        for (var page : pages) {
            if (page == source) continue;
            var sections = json.createArrayNode();
            source.path("sections").asArray().valueStream().filter(section -> "NAVIGATION".equals(section.path("type").asString())).forEach(section -> sections.add(section.deepCopy()));
            page.path("sections").asArray().valueStream().filter(section -> !Set.of("NAVIGATION", "FOOTER").contains(section.path("type").asString())).forEach(sections::add);
            source.path("sections").asArray().valueStream().filter(section -> "FOOTER".equals(section.path("type").asString())).forEach(section -> sections.add(section.deepCopy()));
            page.asObject().set("sections", sections);
        }
    }

    private JsonNode published(JsonNode document) {
        var snapshot = document.deepCopy().asObject();
        var pages = json.createArrayNode();
        document.path("pages").asArray().valueStream().filter(page -> "PUBLISHED".equals(page.path("status").asString())).forEach(pages::add);
        snapshot.set("pages", pages);
        return snapshot;
    }
    private void validateDocument(JsonNode document) {
        require(document.isObject(), "theme.invalidConfiguration");
        validateTheme(document.path("theme"));
        validatePages(document.path("pages"));
        require(document.path("pages").asArray().valueStream().anyMatch(page -> "HOME".equals(page.path("kind").asString())), "theme.homeRequired");
        require(document.toString().length() <= 4_000_000, "theme.configurationTooLarge");
    }
    private void validatePages(JsonNode pages) {
        require(pages.isArray() && pages.size() <= 200, "theme.invalidPages");
        var ids = new HashSet<String>(); var slugs = new HashSet<String>(); var kinds = new HashSet<String>();
        int[] count = { 0 };
        for (var page : pages) {
            var kind = page.path("kind").asString("");
            require(ids.add(page.path("id").asString()) && page.path("id").asString("").matches("[a-zA-Z0-9_-]{1,45}"), "theme.invalidPage");
            require(!page.path("title").asString("").isBlank() && !page.path("slug").asString("").isBlank() && slugs.add(page.path("slug").asString()), "theme.invalidPage");
            require(Set.of("HOME", "CUSTOM", "PRODUCT", "PRODUCT_GRID", "COLLECTIONS", "CART", "CHECKOUT").contains(kind), "theme.invalidPage");
            require(kind.equals("CUSTOM") || kinds.add(kind), "theme.duplicateTemplate");
            require(Set.of("DRAFT", "PUBLISHED").contains(page.path("status").asString("")) && page.path("sections").isArray(), "theme.invalidPage");
            var nodeIds = new HashSet<String>();
            for (var section : page.path("sections")) validateNode(section, true, 0, count, nodeIds);
        }
    }
    private static final Set<String> SECTION_TYPES = Set.of("NAVIGATION", "FOOTER", "HERO", "RICH_TEXT", "IMAGE_BANNER", "SLIDESHOW", "FEATURED_PRODUCTS", "COLLECTION_LIST", "COLLECTION_PRODUCTS", "PRODUCT_GRID", "PRODUCT_INFORMATION", "PRODUCT_DESCRIPTION", "CART", "CHECKOUT");
    private static final Set<String> BLOCK_TYPES = Set.of("SLIDE", "SOCIAL_LINK", "CHECKOUT_CONTACT", "CHECKOUT_FULFILLMENT", "CHECKOUT_NOTES", "CHECKOUT_PAYMENT", "CHECKOUT_SUMMARY", "CHECKOUT_SUBMIT", "LOGO", "MENU", "HEADER", "EYEBROW", "HEADING", "TEXT", "BUTTON", "PRODUCT_CARD", "COLLECTION_LIST", "COLLECTION_PRODUCTS", "IMAGE", "PRODUCT_TITLE", "PRODUCT_PRICE", "VARIANT_SELECTOR", "RENTAL_PLAN_SELECTOR", "DEPOSIT_INFO", "QUANTITY_SELECTOR", "ADD_TO_CART", "PRICE_SUMMARY", "CART_ITEMS", "CART_SUMMARY", "CART_CHECKOUT");
    private void validateNode(JsonNode node, boolean section, int depth, int[] count, Set<String> ids) {
        require(depth <= 16 && ++count[0] <= 3000, "theme.invalidConfiguration");
        require(node.isObject() && (section ? SECTION_TYPES : BLOCK_TYPES).contains(node.path("type").asString("")), "theme.invalidConfiguration");
        require(!node.path("id").asString("").isBlank() && ids.add(node.path("id").asString()) && node.path("enabled").isBoolean() && node.path("settings").isObject(), "theme.invalidConfiguration");
        if (section || node.has("blocks")) {
            require(node.path("blocks").isArray(), "theme.invalidConfiguration");
            for (var child : node.path("blocks")) validateNode(child, false, depth + 1, count, ids);
        }
    }
    private void validateTheme(JsonNode theme) {
        require(theme.isObject() && theme.path("colors").isObject() && theme.path("layout").isObject() && theme.path("typography").isObject(), "theme.invalidSettings");
    }
    private String resolve(String id) { return id == null ? state(false).path("activeId").asString() : id; }
    private ObjectNode state(boolean lock) {
        return (lock ? repository.lockByKey("THEME_LIBRARY") : repository.findByKey("THEME_LIBRARY"))
                .map(this::parse).orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Run tenant migration V31"));
    }
    private StorefrontConfiguration row(String id) { return repository.findByKey(key(id)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "theme.notFound")); }
    private ObjectNode instance(String id) { return parse(row(id)); }
    private String key(String id) { return StorefrontService.resourceKey("THM:", id); }
    private ObjectNode parse(StorefrontConfiguration row) { return json.readTree(row.getConfiguration()).deepCopy().asObject(); }
    private void write(String key, JsonNode configuration) {
        var row = repository.findByKey(key).orElseGet(StorefrontConfiguration::new);
        row.setKey(key); row.setConfiguration(configuration.toString()); repository.save(row);
    }
    private String name(String value) { require(!value.isBlank() && value.trim().length() <= 100, "theme.invalidName"); return value.trim(); }
    private static void require(boolean condition, String reason) { if (!condition) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, reason); }
}
