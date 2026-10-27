package app.sewain.catalogs;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface AttributeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "values", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Attribute toEntity(AttributeCommand command);

    AttributeOptionResponse toOptionResponse(Attribute attribute);

    AttributeResponse toResponse(Attribute attribute);

    AttributeValueResponse toResponse(AttributeValue value);

}