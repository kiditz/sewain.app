package app.sewain.catalogs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-20T12:27:41+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-java-compiler-worker-9.7.1.jar, environment: Java 21.0.12 (Amazon.com Inc.)"
)
@Component
class ProductVariantValueMapperImpl implements ProductVariantValueMapper {

    @Override
    public List<ProductVariantValueResponse> toResponses(Iterable<ProductVariantValue> entity) {
        if ( entity == null ) {
            return null;
        }

        List<ProductVariantValueResponse> list = new ArrayList<ProductVariantValueResponse>();
        for ( ProductVariantValue productVariantValue : entity ) {
            list.add( toResponse( productVariantValue ) );
        }

        return list;
    }

    @Override
    public List<ProductVariantValueResponse> toResponses(List<ProductVariantValue> entity) {
        if ( entity == null ) {
            return null;
        }

        List<ProductVariantValueResponse> list = new ArrayList<ProductVariantValueResponse>( entity.size() );
        for ( ProductVariantValue productVariantValue : entity ) {
            list.add( toResponse( productVariantValue ) );
        }

        return list;
    }

    @Override
    public List<ProductVariantValue> toEntities(List<ProductVariantValueCommand> entity) {
        if ( entity == null ) {
            return null;
        }

        List<ProductVariantValue> list = new ArrayList<ProductVariantValue>( entity.size() );
        for ( ProductVariantValueCommand productVariantValueCommand : entity ) {
            list.add( toEntity( productVariantValueCommand ) );
        }

        return list;
    }

    @Override
    public ProductVariantValue toEntity(ProductVariantValueCommand command) {
        if ( command == null ) {
            return null;
        }

        ProductVariantValue productVariantValue = new ProductVariantValue();

        return productVariantValue;
    }

    @Override
    public ProductVariantValueResponse toResponse(ProductVariantValue entity) {
        if ( entity == null ) {
            return null;
        }

        UUID attributeId = null;
        String attributeName = null;
        String attributeSlug = null;
        UUID attributeValueId = null;
        String value = null;
        String slug = null;
        int position = 0;

        attributeId = entityAttributeId( entity );
        attributeName = entityAttributeName( entity );
        attributeSlug = entityAttributeSlug( entity );
        attributeValueId = entityAttributeValueId( entity );
        value = entityAttributeValueValue( entity );
        slug = entityAttributeValueSlug( entity );
        position = entityAttributeValuePosition( entity );

        ProductVariantValueResponse productVariantValueResponse = new ProductVariantValueResponse( attributeId, attributeName, attributeSlug, attributeValueId, value, slug, position );

        return productVariantValueResponse;
    }

    @Override
    public void update(ProductVariantValueCommand command, ProductVariantValue entity) {
        if ( command == null ) {
            return;
        }
    }

    private UUID entityAttributeId(ProductVariantValue productVariantValue) {
        Attribute attribute = productVariantValue.getAttribute();
        if ( attribute == null ) {
            return null;
        }
        return attribute.getId();
    }

    private String entityAttributeName(ProductVariantValue productVariantValue) {
        Attribute attribute = productVariantValue.getAttribute();
        if ( attribute == null ) {
            return null;
        }
        return attribute.getName();
    }

    private String entityAttributeSlug(ProductVariantValue productVariantValue) {
        Attribute attribute = productVariantValue.getAttribute();
        if ( attribute == null ) {
            return null;
        }
        return attribute.getSlug();
    }

    private UUID entityAttributeValueId(ProductVariantValue productVariantValue) {
        AttributeValue attributeValue = productVariantValue.getAttributeValue();
        if ( attributeValue == null ) {
            return null;
        }
        return attributeValue.getId();
    }

    private String entityAttributeValueValue(ProductVariantValue productVariantValue) {
        AttributeValue attributeValue = productVariantValue.getAttributeValue();
        if ( attributeValue == null ) {
            return null;
        }
        return attributeValue.getValue();
    }

    private String entityAttributeValueSlug(ProductVariantValue productVariantValue) {
        AttributeValue attributeValue = productVariantValue.getAttributeValue();
        if ( attributeValue == null ) {
            return null;
        }
        return attributeValue.getSlug();
    }

    private int entityAttributeValuePosition(ProductVariantValue productVariantValue) {
        AttributeValue attributeValue = productVariantValue.getAttributeValue();
        if ( attributeValue == null ) {
            return 0;
        }
        return attributeValue.getPosition();
    }
}
