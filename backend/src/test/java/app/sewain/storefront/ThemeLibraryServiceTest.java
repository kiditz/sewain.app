package app.sewain.storefront;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ThemeLibraryServiceTest {
    final Map<String, StorefrontConfiguration> records = new LinkedHashMap<>();
    final JsonMapper json = JsonMapper.builder().build();
    ThemeLibraryService service;
    @BeforeEach void setup() {
        var repository = mock(StorefrontRepository.class);
        when(repository.findByKey(anyString())).thenAnswer(call -> Optional.ofNullable(records.get(call.getArgument(0))));
        when(repository.lockByKey(anyString())).thenAnswer(call -> Optional.ofNullable(records.get(call.getArgument(0))));
        when(repository.findAllByKeyStartingWithOrderByCreatedAtAsc(anyString())).thenAnswer(call -> records.values().stream().filter(row -> row.getKey().startsWith(call.getArgument(0))).toList());
        when(repository.save(any(StorefrontConfiguration.class))).thenAnswer(call -> { var row = (StorefrontConfiguration) call.getArgument(0); records.put(row.getKey(), row); return row; });
        doAnswer(call -> { records.remove(((StorefrontConfiguration) call.getArgument(0)).getKey()); return null; }).when(repository).delete(any());
        var state = new StorefrontConfiguration(); state.setKey("THEME_LIBRARY"); state.setConfiguration("{\"activeId\":\"original\"}"); records.put(state.getKey(), state);
        service = new ThemeLibraryService(repository, json);
        var content = new StorefrontConfiguration(); content.setKey("CONTENT_PAGE:about");
        content.setConfiguration(request("Original").path("configuration").path("pages").get(1).toString()); records.put(content.getKey(), content);
        var original = service.install(request("Original"));
        var row = records.remove("THM:" + original.path("id").asString());
        var document = json.readTree(row.getConfiguration()).asObject(); document.put("id", "original");
        row.setKey("THM:original"); row.setConfiguration(document.toString()); records.put(row.getKey(), row);
        service.publish("original");
    }
    JsonNode request(String name) {
        return json.readTree("""
          {"name":"%s","configuration":{"theme":{"name":"Everyday","colors":{"primary":"#000000"},"layout":{},"typography":{}},"pages":[
           {"id":"home","title":"Home","slug":"home","kind":"HOME","status":"PUBLISHED","sections":[]},
           {"id":"about","title":"About","slug":"about","kind":"CUSTOM","status":"DRAFT","sections":[]}
          ]}}
          """.formatted(name));
    }
    @Test void draftEditsStayPrivateUntilPublishAndCustomDraftPagesRemainPrivate() {
        var settings = json.readTree("{\"colors\":{\"primary\":\"#ffffff\"},\"layout\":{},\"typography\":{}}");
        service.saveResource("original", "theme", settings);
        assertEquals("#000000", service.live().path("theme").path("colors").path("primary").asString());
        assertEquals("#ffffff", service.draft("original").path("theme").path("colors").path("primary").asString());
        service.publish("original");
        assertEquals("#ffffff", service.live().path("theme").path("colors").path("primary").asString());
        assertEquals(1, service.live().path("pages").size());
        assertEquals(2, service.draft("original").path("pages").size());
    }
    @Test void duplicationAndThemeSwitchingKeepDraftsIndependent() {
        var duplicate = service.duplicate("original"); var id = duplicate.path("id").asString();
        assertEquals(service.draft("original"), service.draft(id));
        var page = service.draft(id).path("pages").get(0).deepCopy().asObject(); page.put("title", "New home");
        service.saveResource(id, "page:home", page);
        assertEquals("Home", service.draft("original").path("pages").get(0).path("title").asString());
        assertEquals("Home", service.live().path("pages").get(0).path("title").asString());
        service.publish(id);
        assertEquals("New home", service.live().path("pages").get(0).path("title").asString());
        assertEquals(id, service.resource(null, "pages").themeId());
        service.delete("original");
        assertThrows(ResponseStatusException.class, () -> service.delete(id));
    }
    @Test void rollbackPublishesEarlierSnapshotAndPreservesTheEditingDraft() {
        var revision = json.readTree(records.get("THM:original").getConfiguration()).path("revision").asInt();
        var page = service.draft("original").path("pages").get(0).deepCopy().asObject(); page.put("title", "Updated");
        service.saveResource("original", "page:home", page); service.publish("original");
        service.rollback("original", revision);
        assertEquals("Home", service.live().path("pages").get(0).path("title").asString());
        assertEquals("Updated", service.draft("original").path("pages").get(0).path("title").asString());
        assertTrue(service.list().get(0).path("hasChanges").asBoolean());
    }
    @Test void validationRejectsDuplicateSlugsAndProtectsRequiredTemplates() {
        var bad = request("Bad").deepCopy().asObject();
        bad.path("configuration").path("pages").get(1).asObject().put("slug", "home");
        assertThrows(ResponseStatusException.class, () -> service.install(bad));
        var incompatible = request("New").deepCopy().asObject(); incompatible.put("schemaVersion", 99);
        assertThrows(ResponseStatusException.class, () -> service.install(incompatible));
        assertThrows(ResponseStatusException.class, () -> service.deletePage("original", "home"));
        assertThrows(ResponseStatusException.class, () -> service.rename("original", " "));
        assertThrows(ResponseStatusException.class, () -> service.rollback("original", 999));
    }

    @Test void customContentCreatedAfterInstallationSurvivesSwitchingThemesWithoutLeakingDrafts() {
        var id = service.duplicate("original").path("id").asString();
        var page = request("New").path("configuration").path("pages").get(1).deepCopy().asObject();
        page.put("id", "new-page"); page.put("slug", "new-page"); page.put("status", "PUBLISHED");
        service.saveResource("original", "page:new-page", page);
        assertEquals(3, service.draft(id).path("pages").size());
        assertEquals(1, service.live().path("pages").size());
        service.publish(id);
        assertEquals(2, service.live().path("pages").size());
        service.deletePage("original", "new-page");
        assertEquals(2, service.draft(id).path("pages").size());
        assertEquals(2, service.live().path("pages").size());
    }
    @Test void globalSectionsPropagateInsideOneThemeAndHistoryIsBounded() {
        var second = service.duplicate("original").path("id").asString();
        var page = service.draft("original").path("pages").get(0).deepCopy().asObject();
        page.set("sections", json.readTree("[{\"id\":\"nav\",\"type\":\"NAVIGATION\",\"enabled\":true,\"settings\":{\"sticky\":true},\"blocks\":[]}]"));
        service.saveResource("original", "page:home", page);
        assertTrue(service.draft("original").path("pages").get(1).path("sections").get(0).path("settings").path("sticky").asBoolean());
        assertEquals(0, service.draft(second).path("pages").get(0).path("sections").size());
        for (int i = 0; i < 12; i++) service.publish("original");
        assertEquals(10, service.list().get(0).path("history").size());
    }

    @Test void storefrontValidationErrorsKeepTheirStatusAndTranslationCode() throws Exception {
        org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new ThemeLibraryController(service))
                .setControllerAdvice(new StorefrontExceptionHandler()).build()
                .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/storefront/themes/original"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value("theme.activeCannotDelete"));
    }
}
