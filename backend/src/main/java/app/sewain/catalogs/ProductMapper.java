package app.sewain.catalogs;

import app.sewain.shared.GlobalMapper;
import app.sewain.shared.GlobalMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class, uses = {
		ProductVariantMapper.class,
		ProductTypeMapper.class,
        TagMapper.class,
})
interface ProductMapper extends GlobalMapper<Product, ProductResponse, ProductCommand> {

	@Override
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "type", ignore = true)
    @Mapping(target = "tags", ignore = true)
	@Mapping(target = "variants", ignore = true)
	Product toEntity(ProductCommand command);

	@Override
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "variants", ignore = true)
	@Mapping(target = "type", ignore = true)
    @Mapping(target = "tags", ignore = true)
	void update(ProductCommand command, @MappingTarget Product entity);

	@Override
	ProductResponse toResponse(Product entity);
}