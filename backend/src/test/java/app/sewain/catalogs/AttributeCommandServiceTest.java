package app.sewain.catalogs;

import app.sewain.exception.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AttributeCommandServiceTest {
    final AttributeRepository repository = mock(AttributeRepository.class);
    final AttributeCommandServiceImpl service = new AttributeCommandServiceImpl(repository, Mappers.getMapper(AttributeMapper.class));

    AttributeCommand command(List<AttributeValueCommand> values) { return new AttributeCommand("Capacity", "capacity", "Capacity options", values); }
    AttributeValueCommand value(UUID id, String slug) { return new AttributeValueCommand(id, slug.toUpperCase(), slug); }

    @Test void createAttachesValuesToParentAndMapsResponse() {
        when(repository.save(any())).thenAnswer(call -> {
            Attribute parent = call.getArgument(0);
            assertEquals(2, parent.getValues().size());
            parent.getValues().forEach(v -> assertSame(parent, v.getAttribute()));
            return parent;
        });
        var response = service.create(command(List.of(value(null,"small"), value(null,"large"))));
        assertEquals("capacity", response.slug());
        assertEquals(2, response.values().size());
    }

    @Test void duplicateSlugIsRejectedBeforeSaving() {
        when(repository.existsBySlug("capacity")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create(command(List.of())));
        verify(repository, never()).save(any());
    }

    @Test void duplicateValueSlugsAreRejected() {
        assertThrows(ConflictException.class, () -> service.create(command(List.of(value(null,"small"), value(null,"small")))));
        verify(repository, never()).save(any());
    }

    @Test void updatePreservesExistingValueIdentityRemovesOmittedAndAddsNew() {
        var parent = new Attribute(); var id = UUID.randomUUID(); parent.setId(id);
        var kept = new AttributeValue(); kept.setId(UUID.randomUUID()); kept.setSlug("old"); kept.setAttribute(parent);
        var removed = new AttributeValue(); removed.setId(UUID.randomUUID()); removed.setAttribute(parent);
        parent.getValues().addAll(List.of(kept, removed));
        when(repository.findById(id)).thenReturn(Optional.of(parent));
        when(repository.save(parent)).thenReturn(parent);
        service.update(id, command(List.of(value(kept.getId(), "renamed"), value(null,"new"))));
        assertTrue(parent.getValues().contains(kept)); assertFalse(parent.getValues().contains(removed));
        assertEquals("renamed", kept.getSlug()); assertEquals(2, parent.getValues().size());
    }

    @Test void foreignValueIdIsRejected() {
        var id = UUID.randomUUID(); when(repository.findById(id)).thenReturn(Optional.of(new Attribute()));
        assertThrows(ResourceNotFoundException.class, () -> service.update(id, command(List.of(value(UUID.randomUUID(),"foreign")))));
    }

    @Test void updateRejectsAnotherAttributesSlug() {
        var id = UUID.randomUUID(); when(repository.findById(id)).thenReturn(Optional.of(new Attribute()));
        when(repository.existsBySlugAndIdNot("capacity",id)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.update(id, command(List.of())));
    }

    @Test void deleteRequiresExistingAttribute() {
        var id = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class, () -> service.delete(id));
        var entity = new Attribute(); when(repository.findById(id)).thenReturn(Optional.of(entity));
        service.delete(id); verify(repository).delete(entity);
    }

    @Test void omittedValuesNormalizeToEmptyList() {
        assertTrue(command(null).values().isEmpty());
    }
}
