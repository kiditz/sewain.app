package app.sewain.catalogs;

import java.time.LocalDateTime;
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
class InventoryUnitMapperImpl implements InventoryUnitMapper {

    @Override
    public List<InventoryUnitResponse> toResponses(Iterable<InventoryUnit> entity) {
        if ( entity == null ) {
            return null;
        }

        List<InventoryUnitResponse> list = new ArrayList<InventoryUnitResponse>();
        for ( InventoryUnit inventoryUnit : entity ) {
            list.add( toResponse( inventoryUnit ) );
        }

        return list;
    }

    @Override
    public List<InventoryUnitResponse> toResponses(List<InventoryUnit> entity) {
        if ( entity == null ) {
            return null;
        }

        List<InventoryUnitResponse> list = new ArrayList<InventoryUnitResponse>( entity.size() );
        for ( InventoryUnit inventoryUnit : entity ) {
            list.add( toResponse( inventoryUnit ) );
        }

        return list;
    }

    @Override
    public List<InventoryUnit> toEntities(List<InventoryUnitCommand> entity) {
        if ( entity == null ) {
            return null;
        }

        List<InventoryUnit> list = new ArrayList<InventoryUnit>( entity.size() );
        for ( InventoryUnitCommand inventoryUnitCommand : entity ) {
            list.add( toEntity( inventoryUnitCommand ) );
        }

        return list;
    }

    @Override
    public InventoryUnit toEntity(InventoryUnitCommand command) {
        if ( command == null ) {
            return null;
        }

        InventoryUnit inventoryUnit = new InventoryUnit();

        inventoryUnit.setId( command.id() );
        inventoryUnit.setSkuBarcode( command.skuBarcode() );
        inventoryUnit.setSerialNumber( command.serialNumber() );
        inventoryUnit.setStatus( command.status() );
        inventoryUnit.setConditionNotes( command.conditionNotes() );

        return inventoryUnit;
    }

    @Override
    public InventoryUnitResponse toResponse(InventoryUnit inventoryUnit) {
        if ( inventoryUnit == null ) {
            return null;
        }

        UUID id = null;
        String skuBarcode = null;
        String serialNumber = null;
        InventoryUnitStatus status = null;
        String conditionNotes = null;
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;

        id = inventoryUnit.getId();
        skuBarcode = inventoryUnit.getSkuBarcode();
        serialNumber = inventoryUnit.getSerialNumber();
        status = inventoryUnit.getStatus();
        conditionNotes = inventoryUnit.getConditionNotes();
        createdAt = inventoryUnit.getCreatedAt();
        updatedAt = inventoryUnit.getUpdatedAt();

        InventoryUnitResponse inventoryUnitResponse = new InventoryUnitResponse( id, skuBarcode, serialNumber, status, conditionNotes, createdAt, updatedAt );

        return inventoryUnitResponse;
    }

    @Override
    public void update(InventoryUnitCommand command, InventoryUnit inventoryUnit) {
        if ( command == null ) {
            return;
        }

        if ( command.skuBarcode() != null ) {
            inventoryUnit.setSkuBarcode( command.skuBarcode() );
        }
        if ( command.serialNumber() != null ) {
            inventoryUnit.setSerialNumber( command.serialNumber() );
        }
        if ( command.conditionNotes() != null ) {
            inventoryUnit.setConditionNotes( command.conditionNotes() );
        }
    }
}
