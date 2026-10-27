package app.sewain.catalogs;

import app.sewain.shared.GlobalMapper;
import app.sewain.shared.GlobalMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class,
		uses = {
				ProductVariantValueMapper.class,
				VariantRentalPlanMapper.class,
				InventoryUnitMapper.class
		}
)
interface ProductVariantMapper extends GlobalMapper<ProductVariant, ProductVariantResponse, ProductVariantCommand> {

	@Override
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "product", ignore = true)
	@Mapping(target = "values", ignore = true)
	@Mapping(target = "rentalPlans", ignore = true)
	@Mapping(target = "inventoryUnits", ignore = true)
	@Mapping(target = "mediaList", source = "mediaList")
	@Mapping(target = "active", defaultValue = "true")
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	ProductVariant toEntity(ProductVariantCommand command);

	@Override
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "product", ignore = true)
	@Mapping(target = "values", ignore = true)
	@Mapping(target = "rentalPlans", ignore = true)
	@Mapping(target = "inventoryUnits", ignore = true)
	void update(ProductVariantCommand command, @MappingTarget ProductVariant entity);
}