package app.sewain.countries;

import app.sewain.settings.JsonNodeMapper;
import app.sewain.shared.GlobalMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class, uses = JsonNodeMapper.class)
interface CountryMapper {

	@Mapping(target = "iso2", source = "iso2")
	@Mapping(target = "iso3", source = "iso3")
	@Mapping(target = "phonecode", source = "phonecode")
	@Mapping(target = "currencyName", source = "currencyName")
	@Mapping(target = "currencySymbol", source = "currencySymbol")
	CountryResponse toResponse(Country country);

	List<CountryResponse> toResponses(List<Country> countries);
}
