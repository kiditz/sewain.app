package app.sewain.catalogs;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-20T12:27:41+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-java-compiler-worker-9.7.1.jar, environment: Java 21.0.12 (Amazon.com Inc.)"
)
@Component
class ProductVariantMapperImpl implements ProductVariantMapper {

    @Autowired
    private ProductVariantValueMapper productVariantValueMapper;
    @Autowired
    private VariantRentalPlanMapper variantRentalPlanMapper;
    @Autowired
    private InventoryUnitMapper inventoryUnitMapper;

    @Override
    public ProductVariantResponse toResponse(ProductVariant entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        String name = null;
        String sku = null;
        List<String> mediaList = null;
        BigDecimal buyPrice = null;
        BigDecimal depositAmount = null;
        boolean active = false;
        List<ProductVariantValueResponse> values = null;
        List<VariantRentalPlanResponse> rentalPlans = null;
        List<InventoryUnitResponse> inventoryUnits = null;
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;

        id = entity.getId();
        name = entity.getName();
        sku = entity.getSku();
        List<String> list = entity.getMediaList();
        if ( list != null ) {
            mediaList = new ArrayList<String>( list );
        }
        buyPrice = entity.getBuyPrice();
        depositAmount = entity.getDepositAmount();
        active = entity.isActive();
        values = productVariantValueMapper.toResponses( entity.getValues() );
        rentalPlans = variantRentalPlanMapper.toResponses( entity.getRentalPlans() );
        inventoryUnits = inventoryUnitMapper.toResponses( entity.getInventoryUnits() );
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();

        ProductVariantResponse productVariantResponse = new ProductVariantResponse( id, name, sku, mediaList, buyPrice, depositAmount, active, values, rentalPlans, inventoryUnits, createdAt, updatedAt );

        return productVariantResponse;
    }

    @Override
    public List<ProductVariantResponse> toResponses(Iterable<ProductVariant> entity) {
        if ( entity == null ) {
            return null;
        }

        List<ProductVariantResponse> list = new ArrayList<ProductVariantResponse>();
        for ( ProductVariant productVariant : entity ) {
            list.add( toResponse( productVariant ) );
        }

        return list;
    }

    @Override
    public List<ProductVariantResponse> toResponses(List<ProductVariant> entity) {
        if ( entity == null ) {
            return null;
        }

        List<ProductVariantResponse> list = new ArrayList<ProductVariantResponse>( entity.size() );
        for ( ProductVariant productVariant : entity ) {
            list.add( toResponse( productVariant ) );
        }

        return list;
    }

    @Override
    public List<ProductVariant> toEntities(List<ProductVariantCommand> entity) {
        if ( entity == null ) {
            return null;
        }

        List<ProductVariant> list = new ArrayList<ProductVariant>( entity.size() );
        for ( ProductVariantCommand productVariantCommand : entity ) {
            list.add( toEntity( productVariantCommand ) );
        }

        return list;
    }

    @Override
    public ProductVariant toEntity(ProductVariantCommand command) {
        if ( command == null ) {
            return null;
        }

        ProductVariant productVariant = new ProductVariant();

        List<String> list = command.mediaList();
        if ( list != null ) {
            productVariant.setMediaList( new ArrayList<String>( list ) );
        }
        if ( command.active() != null ) {
            productVariant.setActive( command.active() );
        }
        else {
            productVariant.setActive( true );
        }
        productVariant.setName( command.name() );
        productVariant.setSku( command.sku() );
        productVariant.setBuyPrice( command.buyPrice() );
        productVariant.setDepositAmount( command.depositAmount() );

        return productVariant;
    }

    @Override
    public void update(ProductVariantCommand command, ProductVariant entity) {
        if ( command == null ) {
            return;
        }

        if ( command.name() != null ) {
            entity.setName( command.name() );
        }
        if ( command.sku() != null ) {
            entity.setSku( command.sku() );
        }
        if ( entity.getMediaList() != null ) {
            List<String> list = command.mediaList();
            if ( list != null ) {
                entity.getMediaList().clear();
                entity.getMediaList().addAll( list );
            }
        }
        else {
            List<String> list = command.mediaList();
            if ( list != null ) {
                entity.setMediaList( new ArrayList<String>( list ) );
            }
        }
        if ( command.buyPrice() != null ) {
            entity.setBuyPrice( command.buyPrice() );
        }
        if ( command.depositAmount() != null ) {
            entity.setDepositAmount( command.depositAmount() );
        }
        if ( command.active() != null ) {
            entity.setActive( command.active() );
        }
    }
}
