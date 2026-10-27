
package app.sewain.catalogs;

import app.sewain.shared.GlobalMapper;
import app.sewain.shared.GlobalMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class)
interface ProductVariantValueMapper extends
		GlobalMapper<
				ProductVariantValue,
				ProductVariantValueResponse,
				ProductVariantValueCommand> {

	@Override
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "variant", ignore = true)
	@Mapping(target = "attribute", ignore = true)
	@Mapping(target = "attributeValue", ignore = true)
	ProductVariantValue toEntity(ProductVariantValueCommand command);

	@Override
	@Mapping(target = "attributeId", source = "attribute.id")
	@Mapping(target = "attributeName", source = "attribute.name")
	@Mapping(target = "attributeSlug", source = "attribute.slug")
	@Mapping(target = "attributeValueId", source = "attributeValue.id")
	@Mapping(target = "value", source = "attributeValue.value")
	@Mapping(target = "slug", source = "attributeValue.slug")
	@Mapping(target = "position", source = "attributeValue.position")
	ProductVariantValueResponse toResponse(ProductVariantValue entity);

	@Override
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "variant", ignore = true)
	@Mapping(target = "attribute", ignore = true)
	@Mapping(target = "attributeValue", ignore = true)
	void update(
			ProductVariantValueCommand command,
			@MappingTarget ProductVariantValue entity
	);
}
