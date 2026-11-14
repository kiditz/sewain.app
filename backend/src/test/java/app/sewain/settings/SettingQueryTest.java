package app.sewain.settings;
import app.sewain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SettingQueryTest {
 @Test void queryAndFacadeReturnMappedJsonAndRejectUnknownKey(){var repository=mock(SettingRepository.class);var mapper=Mappers.getMapper(SettingMapper.class);var json=new ObjectMapper();ReflectionTestUtils.setField(mapper,"jsonNodeMapper",new JsonNodeMapper(json));var service=new SettingQueryServiceImpl(repository,mapper);var setting=new Setting();setting.setKey(SettingKey.REGIONAL);setting.setValue(json.readTree("{\"currency\":\"IDR\"}"));when(repository.findAll()).thenReturn(List.of(setting));when(repository.findByKey(SettingKey.REGIONAL)).thenReturn(Optional.of(setting));assertEquals(1,service.findAll().size());assertEquals("IDR",new SettingFacade(service).getValue(SettingKey.REGIONAL).path("currency").asString());assertThrows(ResourceNotFoundException.class,()->service.findByKey(SettingKey.RENTAL));}
}
