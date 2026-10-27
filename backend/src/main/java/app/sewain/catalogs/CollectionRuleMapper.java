package app.sewain.catalogs;

import app.sewain.shared.GlobalMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
interface CollectionRuleMapper {

	CollectionRuleResponse toResponse(CollectionRule rule);

	default String toValue(CollectionRuleValue value) {
		return value.getValue();
	}
}
