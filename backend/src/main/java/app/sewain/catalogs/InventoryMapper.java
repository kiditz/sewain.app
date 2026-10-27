package app.sewain.catalogs;

import app.sewain.shared.GlobalMapperConfig;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Set;
import java.util.UUID;

@Mapper(config = GlobalMapperConfig.class)
interface InventoryMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "variant", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	InventoryUnit toEntity(InventoryCommand command);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "variant", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	void update(
			InventoryCommand command,
			@MappingTarget InventoryUnit entity
	);

	/**
	 * @param allocatedUnitIds id unit yang pernah masuk rental allocation
	 */
	@Mapping(target = "variantId", source = "variant.id")
	@Mapping(target = "productId", source = "variant.product.id")
	@Mapping(target = "productName", source = "variant.product.name")
	@Mapping(target = "variantName", source = "variant.name")
	@Mapping(target = "variantSku", source = "variant.sku")
	@Mapping(
			target = "deletable",
			expression = "java(entity.getVariant() == null "
					+ "&& !allocatedUnitIds.contains(entity.getId()))"
	)
	InventoryResponse toResponse(
			InventoryUnit entity,
			@Context Set<UUID> allocatedUnitIds
	);
}
