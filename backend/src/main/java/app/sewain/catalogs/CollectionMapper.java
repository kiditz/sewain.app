package app.sewain.catalogs;

import app.sewain.shared.GlobalMapper;
import app.sewain.shared.GlobalMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.UUID;

@Mapper(config = GlobalMapperConfig.class, uses = CollectionRuleMapper.class)
interface CollectionMapper extends GlobalMapper<Collection, CollectionsResponse, CollectionCommand> {

	/*
	 * rules & items tidak dipetakan otomatis: keduanya disinkronkan
	 * secara eksplisit oleh CollectionCommandServiceImpl.
	 */
	@Override
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "position", ignore = true)
	@Mapping(target = "parent", ignore = true)
	@Mapping(target = "children", ignore = true)
	@Mapping(target = "rules", ignore = true)
	@Mapping(target = "items", ignore = true)
	@Mapping(target = "productCount", ignore = true)
	Collection toEntity(CollectionCommand command);

	@Override
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "position", ignore = true)
	@Mapping(target = "parent", ignore = true)
	@Mapping(target = "children", ignore = true)
	@Mapping(target = "rules", ignore = true)
	@Mapping(target = "items", ignore = true)
	@Mapping(target = "productCount", ignore = true)
	void update(CollectionCommand command, @MappingTarget Collection entity);

	@Override
	@Mapping(target = "parentId", source = "parent.id")
	CollectionsResponse toResponse(Collection entity);

	@Mapping(target = "parentId", source = "parent.id")
	@Mapping(target = "productIds", source = "items")
	CollectionDetailResponse toDetail(Collection entity);

	default UUID toProductId(CollectionItem item) {
		return item.getProduct().getId();
	}
}
