
package app.sewain.catalogs;

import app.sewain.shared.GlobalMapper;
import app.sewain.shared.GlobalMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
interface InventoryUnitMapper extends GlobalMapper<InventoryUnit, InventoryUnitResponse, InventoryUnitCommand> {

	@Mapping(target = "variant", ignore = true)
	InventoryUnit toEntity(InventoryUnitCommand command);

	InventoryUnitResponse toResponse(InventoryUnit inventoryUnit);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "variant", ignore = true)
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	void update(
			InventoryUnitCommand command,
			@org.mapstruct.MappingTarget InventoryUnit inventoryUnit
	);
}
