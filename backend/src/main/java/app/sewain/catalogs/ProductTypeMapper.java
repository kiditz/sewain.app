package app.sewain.catalogs;

import app.sewain.shared.GlobalMapper;
import app.sewain.shared.GlobalMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class)
interface ProductTypeMapper extends GlobalMapper<ProductType, ProductTypeResponse, ProductTypeCommand> {

	ProductTypeResponse toResponse(ProductType entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "slug", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	ProductType toEntity(ProductTypeCommand command);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "slug", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	void update(
			ProductTypeCommand command,
			@MappingTarget ProductType entity
	);
}