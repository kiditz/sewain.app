package app.sewain.storefront;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StorefrontServiceTest {
    private final Map<String, StorefrontConfiguration> records = new LinkedHashMap<>();
    private final JsonMapper json = JsonMapper.builder().build();
    private StorefrontService service;

    @BeforeEach
    void setup() {
        var repository = mock(StorefrontRepository.class);
        when(repository.findByKey(anyString())).thenAnswer(call -> Optional.ofNullable(records.get(call.getArgument(0))));
        when(repository.findAllByKeyStartingWithOrderByCreatedAtAsc(anyString())).thenAnswer(call ->
                records.values().stream().filter(record -> record.getKey().startsWith(call.getArgument(0))).toList());
        when(repository.save(any(StorefrontConfiguration.class))).thenAnswer(call -> {
            StorefrontConfiguration record = call.getArgument(0);
            records.put(record.getKey(), record);
            return record;
        });
        doAnswer(call -> { records.remove(((StorefrontConfiguration) call.getArgument(0)).getKey()); return null; })
                .when(repository).delete(any(StorefrontConfiguration.class));
        service = new StorefrontService(repository, json);
    }

    private JsonNode page(String id, String kind, String status) {
        return json.readTree("""
                {"id":"%s","title":"%s","slug":"%s","kind":"%s","status":"%s","targetUrl":"/pages/%s","sections":[]}
                """.formatted(id, id, id, kind, status, id));
    }
    private void save(String key, String body) { service.saveResource(key, new StorefrontCommand(json.readTree(body))); }
    private void menu() {
        save("MENU:main", """
                {"id":"main","name":"Main Menu","items":[{"id":"one","pageId":"home","label":"Old label","url":"/stale"},{"id":"two","pageId":"about"}]}
                """);
    }
    private void pages() {
        service.saveResource("PAGE:home", new StorefrontCommand(page("home", "HOME", "PUBLISHED")));
        service.saveResource("PAGE:about", new StorefrontCommand(page("about", "CUSTOM", "DRAFT")));
    }

    @Test
    void writesOnlyTheSelectedResourceAndPublishedResponseExcludesDrafts() {
        pages();
        save("THEME", "{\"name\":\"Original theme\"}");
        menu();
        save("NAVIGATION", "{\"headerMenuId\":\"main\"}");
        var originalPage = records.get("PAGE:home").getConfiguration();
        var originalTheme = records.get("THEME").getConfiguration();
        save("MENU:main", "{\"id\":\"main\",\"name\":\"Renamed menu\",\"items\":[]}");
        assertEquals(originalPage, records.get("PAGE:home").getConfiguration());
        assertEquals(originalTheme, records.get("THEME").getConfiguration());
        assertEquals(2, service.getPages().configuration().size());
        var published = service.getPublished().configuration();
        assertEquals(1, published.path("pages").size());
        assertEquals("home", published.path("pages").get(0).path("id").asString());
        assertEquals("Renamed menu", published.path("MenuItems").get(0).path("name").asString());
        assertEquals("main", published.path("headerMenuId").asString());
    }

    @Test
    void menuStoresPageReferencesAndCustomLabelsWhileDeletionCleansReferences() {
        pages(); menu();
        var items = service.getMenus().configuration().get(0).path("items");
        assertEquals(json.readTree("{\"id\":\"one\",\"pageId\":\"home\",\"label\":\"Old label\"}"), items.get(0));
        service.deletePage("about");
        assertEquals(1, service.getMenus().configuration().get(0).path("items").size());
        assertFalse(records.containsKey("PAGE:about"));
        save("NAVIGATION", "{\"headerMenuId\":\"main\"}");
        service.deleteMenu("main");
        assertTrue(service.getResource("NAVIGATION").configuration().path("headerMenuId").isNull());
    }

    @Test
    void customMenuLabelsSurviveReloadAndBlankLabelsUsePageTitles() {
        pages();
        save("MENU:main", """
                {"id":"main","name":"Main Menu","items":[{"id":"one","pageId":"home","label":"  Beranda  "},{"id":"two","pageId":"about","label":"  "}]}
                """);
        var items = service.getMenus().configuration().get(0).path("items");
        assertEquals("Beranda", items.get(0).path("label").asString());
        assertFalse(items.get(1).has("label"));
        assertEquals("Beranda", service.getPublished().configuration().path("MenuItems").get(0).path("items").get(0).path("label").asString());
    }

    @Test
    void catalogDestinationsSurviveSavingAlongsidePageLinksInOrder() {
        pages();
        save("MENU:main", """
                {"id":"main","name":"Main","items":[
                  {"id":"product","target":{"type":"PRODUCT","id":"p1","title":"Camera","slug":"camera","url":"javascript:bad"}},
                  {"id":"home","pageId":"home"},
                  {"id":"collection","label":"Pilihan","target":{"type":"COLLECTION","id":"c1","title":"Cameras","slug":"cameras"}}
                ]}
                """);
        var items = service.getMenus().configuration().get(0).path("items");
        assertEquals("product", items.get(0).path("id").asString());
        assertEquals("home", items.get(1).path("pageId").asString());
        assertEquals("cameras", items.get(2).path("target").path("slug").asString());
        assertFalse(items.get(0).path("target").has("url"));
        assertEquals(items, service.getPublished().configuration().path("MenuItems").get(0).path("items"));
        assertThrows(ResponseStatusException.class, () -> save("MENU:bad", """
                {"id":"bad","name":"Bad","items":[{"id":"one","target":{"type":"EXTERNAL","id":"x","title":"X","slug":"x"}}]}
                """));
        assertThrows(ResponseStatusException.class, () -> save("MENU:bad", """
                {"id":"bad","name":"Bad","items":[{"id":"one","target":{"type":"PRODUCT","id":"x","title":"X","slug":"../checkout"}}]}
                """));
    }

    @Test
    void rejectsInvalidReferencesAndProtectsBuiltInPages() {
        pages();
        assertThrows(ResponseStatusException.class, () -> save("MENU:main", "{\"id\":\"main\",\"name\":\"Menu\",\"items\":[{\"id\":\"one\",\"pageId\":\"missing\"}]}"));
        assertThrows(ResponseStatusException.class, () -> save("NAVIGATION", "{\"headerMenuId\":\"missing\"}"));
        assertThrows(ResponseStatusException.class, () -> service.deletePage("home"));
        assertThrows(ResponseStatusException.class, () -> service.saveResource("PAGE:wrong-id", new StorefrontCommand(page("about", "CUSTOM", "DRAFT"))));
        assertThrows(ResponseStatusException.class, () -> service.saveResource("PAGE:another-home", new StorefrontCommand(page("another-home", "HOME", "DRAFT"))));
    }

    @Test
    void doesNotReplaceLegacyDataWithDefaultsBeforeMigration() {
        var legacy = new StorefrontConfiguration();
        legacy.setKey("STOREFRONT");
        legacy.setConfiguration("{\"pages\":[{\"id\":\"original-page\"}]}");
        records.put("STOREFRONT", legacy);
        assertThrows(ResponseStatusException.class, () -> service.getPages());
        assertThrows(ResponseStatusException.class, () -> save("THEME", "{}"));
        assertEquals(1, records.size());
        assertTrue(records.get("STOREFRONT").getConfiguration().contains("original-page"));
    }

    @Test
    void removingLastMenuStaysEmptyAndLegacyWholeDocumentWriteIsDisabled() throws Exception {
        pages(); menu();
        save("NAVIGATION", "{\"headerMenuId\":\"main\"}");
        service.deleteMenu("main");
        assertEquals(0, service.getMenus().configuration().size());
        assertTrue(service.getPublished().configuration().path("headerMenuId").isNull());
        MockMvcBuilders.standaloneSetup(new StorefrontController(service)).build()
                .perform(put("/api/storefront").contentType("application/json").content("{\"configuration\":{}}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
