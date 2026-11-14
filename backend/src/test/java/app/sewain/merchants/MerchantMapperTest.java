package app.sewain.merchants;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MerchantMapperTest {
    final MerchantMapper mapper=Mappers.getMapper(MerchantMapper.class);
    MerchantCommand command(String name) {
        return new MerchantCommand(name,"outdoor",new StoreInformationCommand("Rental equipment","/logo.svg","/icon.png","store@example.invalid","08123"),
                new BusinessInformationCommand("Outdoor Ltd","Main Street","billing@example.invalid","08456","TAX-123"),"outdoor.example.invalid",true);
    }
    @Test void mapsStoreAndBusinessInformationWithoutMixingContactDetails() {
        var entity=mapper.toEntity(command("Outdoor")); var id=UUID.randomUUID(); entity.setId(id); entity.setSchema("tenant_outdoor");
        entity.setProvisioningStatus("READY"); entity.setProvisioningVersion(3);
        var response=mapper.toResponse(entity);
        assertEquals(id,response.id()); assertEquals("tenant_outdoor",response.schema()); assertEquals("READY",response.provisioningStatus());
        assertEquals(3,response.provisioningVersion()); assertTrue(response.published());
        assertEquals("store@example.invalid",response.storeInformation().contactEmail());
        assertEquals("/logo.svg",response.storeInformation().logoUrl()); assertEquals("/icon.png",response.storeInformation().faviconUrl());
        assertEquals("Rental equipment",response.storeInformation().description()); assertEquals("08123",response.storeInformation().contactPhone());
        assertEquals("Outdoor Ltd",response.businessInformation().name()); assertEquals("Main Street",response.businessInformation().address());
        assertEquals("billing@example.invalid",response.businessInformation().email()); assertEquals("08456",response.businessInformation().phone());
        assertEquals("TAX-123",response.businessInformation().taxId()); assertEquals("outdoor.example.invalid",response.customDomain());
    }
    @Test void partialUpdatePreservesPersistentIdentityAndOmittedInformation() {
        var entity=mapper.toEntity(command("Old")); var id=UUID.randomUUID(); entity.setId(id); entity.setSchema("tenant");
        mapper.update(new MerchantCommand("New",null,null,null,null,null),entity);
        assertEquals(id,entity.getId()); assertEquals("tenant",entity.getSchema()); assertEquals("New",entity.getName());
        assertEquals("outdoor",entity.getSlug()); assertEquals("TAX-123",entity.getBusinessInformation().getTaxId()); assertTrue(entity.isPublished());
    }
    @Test void updatePopulatesPreviouslyMissingEmbeddedObjects() {
        var entity=new Merchant(); entity.setStoreInformation(null); entity.setBusinessInformation(null);
        mapper.update(command("Updated"),entity);
        assertEquals("store@example.invalid",entity.getStoreInformation().getContactEmail());
        assertEquals("TAX-123",entity.getBusinessInformation().getTaxId());
        mapper.update(command("Again"),entity); assertEquals("Again",entity.getName());
    }
    @Test void mapsListsAndHandlesAbsentOptionalEmbeddables() {
        var bare=new Merchant(); bare.setStoreInformation(null); bare.setBusinessInformation(null);
        var result=mapper.toResponse(bare); assertNull(result.storeInformation()); assertNull(result.businessInformation());
        assertEquals(2,mapper.toEntities(List.of(command("A"),command("B"))).size());
        assertEquals(2,mapper.toResponses(List.of(bare,bare)).size());
        assertEquals(1,mapper.toResponses((Iterable<Merchant>)Set.of(bare)).size());
    }
}
