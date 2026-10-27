package app.sewain.shared;

import org.mapstruct.*;

@MapperConfig(
		componentModel = MappingConstants.ComponentModel.SPRING,
		unmappedTargetPolicy = ReportingPolicy.IGNORE,
		nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface GlobalMapperConfig {
	// Left intentionally blank. Acts purely as a global configuration anchor.
}
