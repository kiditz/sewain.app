package app.sewain.catalogs;

import app.sewain.exception.ResourceNotFoundException;
import app.sewain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TagServicesTest {
    private TagRepository repository;
    private TagCommandService commands;
    private TagQueryService queries;

    @BeforeEach
    void setup() {
        repository = mock(TagRepository.class);
        var mapper = Mappers.getMapper(TagMapper.class);
        commands = new TagCommandServiceImpl(repository, mapper);
        queries = new TagQueryServiceImpl(repository, mapper);
        when(repository.save(any(Tag.class))).thenAnswer(call -> {
            Tag entity = call.getArgument(0);
            if (entity.getId() == null) entity.setId(UUID.randomUUID());
            return entity;
        });
    }

    @Test
    void createsTrimmedTagAndBoundsSlugWhenThereIsACollision() {
        var name = "a".repeat(100);
        when(repository.existsBySlug(name)).thenReturn(true);
        var result = commands.create(new TagCommand("  " + name + "  "));
        assertEquals(name, result.name());
        assertEquals(100, result.slug().length());
        assertTrue(result.slug().endsWith("-2"));
        assertNotNull(result.id());
        assertThrows(ValidationException.class, () -> commands.create(new TagCommand("!!!")));
    }

    @Test
    void updatesTagInPlaceAndDeletesItWithoutDeletingProducts() {
        var tag = new Tag();
        var id = UUID.randomUUID();
        tag.setId(id);
        tag.setName("Old name");
        tag.setSlug("old-name");
        when(repository.findById(id)).thenReturn(Optional.of(tag));
        var result = commands.update(id, new TagCommand("  New name  "));
        assertEquals(id, result.id());
        assertEquals("New name", result.name());
        assertEquals("new-name", result.slug());
        verify(repository).existsBySlugAndIdNot("new-name", id);
        assertEquals(result, queries.findById(id));
        commands.delete(id);
        verify(repository).delete(tag);
    }

    @Test
    void missingTagsAreReportedByBothQueryAndCommandServices() {
        var id = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class, () -> queries.findById(id));
        assertThrows(ResourceNotFoundException.class, () -> commands.update(id, new TagCommand("Name")));
        assertThrows(ResourceNotFoundException.class, () -> commands.delete(id));
    }

    @Test
    void searchUsesBoundedAndDeterministicallySortedResults() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        assertTrue(queries.find("camp").isEmpty());
        verify(repository).findAll(any(Specification.class), argThat((Pageable page) ->
                page.getPageSize() == 50 && page.getSort().getOrderFor("name") != null && page.getSort().getOrderFor("id") != null));
    }

    @Test
    void httpCrudContractsAndNameValidation() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new TagQueryController(queries), new TagCommandController(commands)).build();
        var id = UUID.randomUUID();
        var tag = new Tag(); tag.setId(id); tag.setName("Camping"); tag.setSlug("camping");
        when(repository.findById(id)).thenReturn(Optional.of(tag));
        mvc.perform(post("/api/tags").contentType("application/json").content("{\"name\":\" Camping \"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Camping"));
        mvc.perform(get("/api/tags/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()));
        mvc.perform(put("/api/tags/{id}", id).contentType("application/json").content("{\"name\":\"Hiking\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.slug").value("hiking"));
        mvc.perform(post("/api/tags").contentType("application/json").content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/tags").contentType("application/json").content("{\"name\":\"" + "x".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/tags/{id}", id)).andExpect(status().isNoContent());
    }
}
