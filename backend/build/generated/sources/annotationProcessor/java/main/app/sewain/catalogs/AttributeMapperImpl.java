package app.sewain.catalogs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-20T15:51:39+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-java-compiler-worker-9.7.1.jar, environment: Java 21.0.12 (Amazon.com Inc.)"
)
@Component
class AttributeMapperImpl implements AttributeMapper {

    @Override
    public Attribute toEntity(AttributeCommand command) {
        if ( command == null ) {
            return null;
        }

        Attribute attribute = new Attribute();

        attribute.setName( command.name() );
        attribute.setSlug( command.slug() );
        attribute.setDescription( command.description() );

        return attribute;
    }

    @Override
    public AttributeOptionResponse toOptionResponse(Attribute attribute) {
        if ( attribute == null ) {
            return null;
        }

        UUID id = null;
        String name = null;
        List<AttributeValueOptionResponse> values = null;

        id = attribute.getId();
        name = attribute.getName();
        values = attributeValueListToAttributeValueOptionResponseList( attribute.getValues() );

        AttributeOptionResponse attributeOptionResponse = new AttributeOptionResponse( id, name, values );

        return attributeOptionResponse;
    }

    @Override
    public AttributeResponse toResponse(Attribute attribute) {
        if ( attribute == null ) {
            return null;
        }

        UUID id = null;
        String name = null;
        String slug = null;
        String description = null;
        List<AttributeValueResponse> values = null;

        id = attribute.getId();
        name = attribute.getName();
        slug = attribute.getSlug();
        description = attribute.getDescription();
        values = attributeValueListToAttributeValueResponseList( attribute.getValues() );

        AttributeResponse attributeResponse = new AttributeResponse( id, name, slug, description, values );

        return attributeResponse;
    }

    @Override
    public AttributeValueResponse toResponse(AttributeValue value) {
        if ( value == null ) {
            return null;
        }

        UUID id = null;
        String value1 = null;
        String slug = null;

        id = value.getId();
        value1 = value.getValue();
        slug = value.getSlug();

        AttributeValueResponse attributeValueResponse = new AttributeValueResponse( id, value1, slug );

        return attributeValueResponse;
    }

    protected AttributeValueOptionResponse attributeValueToAttributeValueOptionResponse(AttributeValue attributeValue) {
        if ( attributeValue == null ) {
            return null;
        }

        UUID id = null;
        String value = null;

        id = attributeValue.getId();
        value = attributeValue.getValue();

        AttributeValueOptionResponse attributeValueOptionResponse = new AttributeValueOptionResponse( id, value );

        return attributeValueOptionResponse;
    }

    protected List<AttributeValueOptionResponse> attributeValueListToAttributeValueOptionResponseList(List<AttributeValue> list) {
        if ( list == null ) {
            return null;
        }

        List<AttributeValueOptionResponse> list1 = new ArrayList<AttributeValueOptionResponse>( list.size() );
        for ( AttributeValue attributeValue : list ) {
            list1.add( attributeValueToAttributeValueOptionResponse( attributeValue ) );
        }

        return list1;
    }

    protected List<AttributeValueResponse> attributeValueListToAttributeValueResponseList(List<AttributeValue> list) {
        if ( list == null ) {
            return null;
        }

        List<AttributeValueResponse> list1 = new ArrayList<AttributeValueResponse>( list.size() );
        for ( AttributeValue attributeValue : list ) {
            list1.add( toResponse( attributeValue ) );
        }

        return list1;
    }
}
