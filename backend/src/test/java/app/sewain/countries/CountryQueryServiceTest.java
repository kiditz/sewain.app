package app.sewain.countries;

import app.sewain.settings.JsonNodeMapper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CountryQueryServiceTest {
    private final CountryRepository repository = mock(CountryRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CountryQueryService service = new CountryQueryService(repository,
            new CountryMapperImpl(new JsonNodeMapper(objectMapper)));

    @Test
    void returnsAnEmptyListWhenNoCountriesExist() {
        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();

        verify(repository).findAllByOrderByNameAsc();
    }

    @Test
    void mapsCountryDetailsAndPreservesRepositoryOrder() {
        var indonesia = new Country();
        indonesia.setId(1L);
        indonesia.setName("Indonesia");
        indonesia.setIso2("ID");
        indonesia.setIso3("IDN");
        indonesia.setPhonecode("62");
        indonesia.setCurrency("IDR");
        indonesia.setCurrencyName("Indonesian rupiah");
        indonesia.setCurrencySymbol("Rp");
        indonesia.setEmoji("🇮🇩");
        indonesia.setTimezones(objectMapper.readTree("[{\"zoneName\":\"Asia/Jakarta\",\"gmtOffset\":25200}]"));
        var singapore = new Country();
        singapore.setId(2L);
        singapore.setName("Singapore");
        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(indonesia, singapore));

        var result = service.findAll();

        assertThat(result).extracting(CountryResponse::name).containsExactly("Indonesia", "Singapore");
        assertThat(result.getFirst()).isEqualTo(new CountryResponse(1L, "Indonesia", "ID", "IDN",
                "62", "IDR", "Indonesian rupiah", "Rp", "🇮🇩", indonesia.getTimezones()));
        assertThat(result.get(1).timezones().isNull()).isTrue();
        verify(repository).findAllByOrderByNameAsc();
    }
}
