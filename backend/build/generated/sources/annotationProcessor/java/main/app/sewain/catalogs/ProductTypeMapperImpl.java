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
class ProductTypeMapperImpl implements ProductTypeMapper {

    @Override
    public List<ProductTypeResponse> toResponses(Iterable<ProductType> entity) {
        if ( entity == null ) {
            return null;
        }

        List<ProductTypeResponse> list = new ArrayList<ProductTypeResponse>();
        for ( ProductType productType : entity ) {
            list.add( toResponse( productType ) );
        }

        return list;
    }

    @Override
    public List<ProductTypeResponse> toResponses(List<ProductType> entity) {
        if ( entity == null ) {
            return null;
        }

        List<ProductTypeResponse> list = new ArrayList<ProductTypeResponse>( entity.size() );
        for ( ProductType productType : entity ) {
            list.add( toResponse( productType ) );
        }

        return list;
    }

    @Override
    public List<ProductType> toEntities(List<ProductTypeCommand> entity) {
        if ( entity == null ) {
            return null;
        }

        List<ProductType> list = new ArrayList<ProductType>( entity.size() );
        for ( ProductTypeCommand productTypeCommand : entity ) {
            list.add( toEntity( productTypeCommand ) );
        }

        return list;
    }

    @Override
    public ProductTypeResponse toResponse(ProductType entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        String name = null;
        String slug = null;

        id = entity.getId();
        name = entity.getName();
        slug = entity.getSlug();

        ProductTypeResponse productTypeResponse = new ProductTypeResponse( id, name, slug );

        return productTypeResponse;
    }

    @Override
    public ProductType toEntity(ProductTypeCommand command) {
        if ( command == null ) {
            return null;
        }

        ProductType productType = new ProductType();

        productType.setName( command.name() );

        return productType;
    }

    @Override
    public void update(ProductTypeCommand command, ProductType entity) {
        if ( command == null ) {
            return;
        }

        if ( command.name() != null ) {
            entity.setName( command.name() );
        }
    }
}
