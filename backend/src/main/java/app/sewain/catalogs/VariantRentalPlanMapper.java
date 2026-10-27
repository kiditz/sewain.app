
package app.sewain.catalogs;

import app.sewain.shared.GlobalMapper;
import app.sewain.shared.GlobalMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class)
interface VariantRentalPlanMapper extends
		GlobalMapper<
				VariantRentalPlan,
				VariantRentalPlanResponse,
				VariantRentalPlanCommand> {

	@Override
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "variant", ignore = true)
	@Mapping(target = "active", defaultValue = "true")
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	VariantRentalPlan toEntity(VariantRentalPlanCommand command);


	@Override
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "variant", ignore = true)
	@Mapping(target = "active", defaultValue = "true")
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	void update(VariantRentalPlanCommand domain, @MappingTarget VariantRentalPlan entity);
}
