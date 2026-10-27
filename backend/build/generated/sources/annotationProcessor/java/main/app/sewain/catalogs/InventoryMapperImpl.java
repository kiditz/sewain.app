package app.sewain.catalogs;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-20T12:27:41+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-java-compiler-worker-9.7.1.jar, environment: Java 21.0.12 (Amazon.com Inc.)"
)
@Component
class InventoryMapperImpl implements InventoryMapper {

    @Override
    public InventoryUnit toEntity(InventoryCommand command) {
        if ( command == null ) {
            return null;
        }

        InventoryUnit inventoryUnit = new InventoryUnit();

        inventoryUnit.setSkuBarcode( command.skuBarcode() );
        inventoryUnit.setSerialNumber( command.serialNumber() );
        inventoryUnit.setStatus( command.status() );
        inventoryUnit.setConditionNotes( command.conditionNotes() );

        return inventoryUnit;
    }

    @Override
    public void update(InventoryCommand command, InventoryUnit entity) {
        if ( command == null ) {
            return;
        }

        if ( command.skuBarcode() != null ) {
            entity.setSkuBarcode( command.skuBarcode() );
        }
        if ( command.serialNumber() != null ) {
            entity.setSerialNumber( command.serialNumber() );
        }
        if ( command.status() != null ) {
            entity.setStatus( command.status() );
        }
        if ( command.conditionNotes() != null ) {
            entity.setConditionNotes( command.conditionNotes() );
        }
    }

    @Override
    public InventoryResponse toResponse(InventoryUnit entity, Set<UUID> allocatedUnitIds) {
        if ( entity == null ) {
            return null;
        }

        UUID variantId = null;
        UUID productId = null;
        String productName = null;
        String variantName = null;
        String variantSku = null;
        UUID id = null;
        String skuBarcode = null;
        String serialNumber = null;
        InventoryUnitStatus status = null;
        String conditionNotes = null;
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;

        variantId = entityVariantId( entity );
        productId = entityVariantProductId( entity );
        productName = entityVariantProductName( entity );
        variantName = entityVariantName( entity );
        variantSku = entityVariantSku( entity );
        id = entity.getId();
        skuBarcode = entity.getSkuBarcode();
        serialNumber = entity.getSerialNumber();
        status = entity.getStatus();
        conditionNotes = entity.getConditionNotes();
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();

        boolean deletable = entity.getVariant() == null && !allocatedUnitIds.contains(entity.getId());

        InventoryResponse inventoryResponse = new InventoryResponse( id, variantId, productId, productName, variantName, variantSku, skuBarcode, serialNumber, status, conditionNotes, deletable, createdAt, updatedAt );

        return inventoryResponse;
    }

    private UUID entityVariantId(InventoryUnit inventoryUnit) {
        ProductVariant variant = inventoryUnit.getVariant();
        if ( variant == null ) {
            return null;
        }
        return variant.getId();
    }

    private UUID entityVariantProductId(InventoryUnit inventoryUnit) {
        ProductVariant variant = inventoryUnit.getVariant();
        if ( variant == null ) {
            return null;
        }
        Product product = variant.getProduct();
        if ( product == null ) {
            return null;
        }
        return product.getId();
    }

    private String entityVariantProductName(InventoryUnit inventoryUnit) {
        ProductVariant variant = inventoryUnit.getVariant();
        if ( variant == null ) {
            return null;
        }
        Product product = variant.getProduct();
        if ( product == null ) {
            return null;
        }
        return product.getName();
    }

    private String entityVariantName(InventoryUnit inventoryUnit) {
        ProductVariant variant = inventoryUnit.getVariant();
        if ( variant == null ) {
            return null;
        }
        return variant.getName();
    }

    private String entityVariantSku(InventoryUnit inventoryUnit) {
        ProductVariant variant = inventoryUnit.getVariant();
        if ( variant == null ) {
            return null;
        }
        return variant.getSku();
    }
}
