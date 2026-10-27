package app.sewain.countries;

import app.sewain.settings.JsonNodeMapper;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-20T12:27:41+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-java-compiler-worker-9.7.1.jar, environment: Java 21.0.12 (Amazon.com Inc.)"
)
@Component
class CountryMapperImpl implements CountryMapper {

    @Autowired
    private JsonNodeMapper jsonNodeMapper;

    @Override
    public CountryResponse toResponse(Country country) {
        if ( country == null ) {
            return null;
        }

        String iso2 = null;
        String iso3 = null;
        String phonecode = null;
        String currencyName = null;
        String currencySymbol = null;
        Long id = null;
        String name = null;
        String currency = null;
        String emoji = null;
        JsonNode timezones = null;

        iso2 = country.getIso2();
        iso3 = country.getIso3();
        phonecode = country.getPhonecode();
        currencyName = country.getCurrencyName();
        currencySymbol = country.getCurrencySymbol();
        id = country.getId();
        name = country.getName();
        currency = country.getCurrency();
        emoji = country.getEmoji();
        timezones = jsonNodeMapper.toJsonNode( country.getTimezones() );

        CountryResponse countryResponse = new CountryResponse( id, name, iso2, iso3, phonecode, currency, currencyName, currencySymbol, emoji, timezones );

        return countryResponse;
    }

    @Override
    public List<CountryResponse> toResponses(List<Country> countries) {
        if ( countries == null ) {
            return null;
        }

        List<CountryResponse> list = new ArrayList<CountryResponse>( countries.size() );
        for ( Country country : countries ) {
            list.add( toResponse( country ) );
        }

        return list;
    }
}
