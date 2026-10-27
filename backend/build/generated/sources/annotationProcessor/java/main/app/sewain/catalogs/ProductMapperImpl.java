package app.sewain.catalogs;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-27T20:08:38+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-java-compiler-worker-9.7.1.jar, environment: Java 21.0.12 (Amazon.com Inc.)"
)
@Component
class ProductMapperImpl implements ProductMapper {

    @Autowired
    private ProductVariantMapper productVariantMapper;
    @Autowired
    private ProductTypeMapper productTypeMapper;
    @Autowired
    private TagMapper tagMapper;

    @Override
    public List<ProductResponse> toResponses(Iterable<Product> entity) {
        if ( entity == null ) {
            return null;
        }

        List<ProductResponse> list = new ArrayList<ProductResponse>();
        for ( Product product : entity ) {
            list.add( toResponse( product ) );
        }

        return list;
    }

    @Override
    public List<ProductResponse> toResponses(List<Product> entity) {
        if ( entity == null ) {
            return null;
        }

        List<ProductResponse> list = new ArrayList<ProductResponse>( entity.size() );
        for ( Product product : entity ) {
            list.add( toResponse( product ) );
        }

        return list;
    }

    @Override
    public List<Product> toEntities(List<ProductCommand> entity) {
        if ( entity == null ) {
            return null;
        }

        List<Product> list = new ArrayList<Product>( entity.size() );
        for ( ProductCommand productCommand : entity ) {
            list.add( toEntity( productCommand ) );
        }

        return list;
    }

    @Override
    public Product toEntity(ProductCommand command) {
        if ( command == null ) {
            return null;
        }

        Product product = new Product();

        product.setName( command.name() );
        product.setMedia( command.media() );
        product.setSlug( command.slug() );
        product.setDescription( command.description() );
        product.setStatus( command.status() );

        return product;
    }

    @Override
    public void update(ProductCommand command, Product entity) {
        if ( command == null ) {
            return;
        }

        if ( command.name() != null ) {
            entity.setName( command.name() );
        }
        if ( command.media() != null ) {
            entity.setMedia( command.media() );
        }
        if ( command.slug() != null ) {
            entity.setSlug( command.slug() );
        }
        if ( command.description() != null ) {
            entity.setDescription( command.description() );
        }
        if ( command.status() != null ) {
            entity.setStatus( command.status() );
        }
    }

    @Override
    public ProductResponse toResponse(Product entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        ProductTypeResponse type = null;
        List<TagResponse> tags = null;
        String name = null;
        String slug = null;
        String media = null;
        String description = null;
        ProductStatus status = null;
        List<ProductVariantResponse> variants = null;
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;

        id = entity.getId();
        type = productTypeMapper.toResponse( entity.getType() );
        tags = tagMapper.toResponses( entity.getTags() );
        name = entity.getName();
        slug = entity.getSlug();
        media = entity.getMedia();
        description = entity.getDescription();
        status = entity.getStatus();
        variants = productVariantMapper.toResponses( entity.getVariants() );
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();

        ProductResponse productResponse = new ProductResponse( id, type, tags, name, slug, media, description, status, variants, createdAt, updatedAt );

        return productResponse;
    }
}
