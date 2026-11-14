package app.sewain.settings;

import app.sewain.exception.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SettingCommandServiceTest {
    final SettingRepository repository=mock(SettingRepository.class);
    final ObjectMapper json=new ObjectMapper();
    final SettingCommandServiceImpl service;
    SettingCommandServiceTest() {
        var mapper=Mappers.getMapper(SettingMapper.class);
        ReflectionTestUtils.setField(mapper,"jsonNodeMapper",new JsonNodeMapper(json));
        service=new SettingCommandServiceImpl(repository,mapper);
    }
    SettingCommand command() { return new SettingCommand(SettingKey.REGIONAL,json.readTree("{\"currency\":\"JPY\"}")); }
    @Test void createPersistsAndMapsJsonValue() {
        when(repository.save(any())).thenAnswer(c->c.getArgument(0));
        var result=service.create(command()); assertEquals(SettingKey.REGIONAL,result.key());
        assertEquals("JPY",result.value().path("currency").asString());
    }
    @Test void duplicateKeyCannotOverwriteExistingSetting() {
        when(repository.existsByKey(SettingKey.REGIONAL)).thenReturn(true);
        assertThrows(ConflictException.class,()->service.create(command())); verify(repository,never()).save(any());
    }
    @Test void updateKeepsIdentityAndMapsNewValue() {
        var id=UUID.randomUUID(); var entity=new Setting(); entity.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        var result=service.update(id,command()); assertEquals(id,result.id());
        assertEquals("JPY",result.value().path("currency").asString());
    }
    @Test void missingSettingIsRejectedForUpdateAndDelete() {
        var id=UUID.randomUUID(); assertThrows(ResourceNotFoundException.class,()->service.update(id,command()));
        assertThrows(ResourceNotFoundException.class,()->service.delete(id)); verify(repository,never()).delete(any());
    }
    @Test void deleteUsesExistingEntity() {
        var id=UUID.randomUUID(); var entity=new Setting(); when(repository.findById(id)).thenReturn(Optional.of(entity));
        service.delete(id); verify(repository).delete(entity);
    }
}
