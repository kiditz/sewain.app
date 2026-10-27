package app.sewain.merchants;

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
class MerchantMapperImpl implements MerchantMapper {

    @Override
    public MerchantResponse toResponse(Merchant entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        String name = null;
        String schema = null;
        String slug = null;
        StoreInformationResponse storeInformation = null;
        BusinessInformationResponse businessInformation = null;
        String customDomain = null;
        boolean published = false;

        id = entity.getId();
        name = entity.getName();
        schema = entity.getSchema();
        slug = entity.getSlug();
        storeInformation = storeInformationToStoreInformationResponse( entity.getStoreInformation() );
        businessInformation = businessInformationToBusinessInformationResponse( entity.getBusinessInformation() );
        customDomain = entity.getCustomDomain();
        published = entity.isPublished();

        MerchantResponse merchantResponse = new MerchantResponse( id, name, schema, slug, storeInformation, businessInformation, customDomain, published );

        return merchantResponse;
    }

    @Override
    public List<MerchantResponse> toResponses(Iterable<Merchant> entity) {
        if ( entity == null ) {
            return null;
        }

        List<MerchantResponse> list = new ArrayList<MerchantResponse>();
        for ( Merchant merchant : entity ) {
            list.add( toResponse( merchant ) );
        }

        return list;
    }

    @Override
    public List<MerchantResponse> toResponses(List<Merchant> entity) {
        if ( entity == null ) {
            return null;
        }

        List<MerchantResponse> list = new ArrayList<MerchantResponse>( entity.size() );
        for ( Merchant merchant : entity ) {
            list.add( toResponse( merchant ) );
        }

        return list;
    }

    @Override
    public Merchant toEntity(MerchantCommand entity) {
        if ( entity == null ) {
            return null;
        }

        Merchant merchant = new Merchant();

        merchant.setName( entity.name() );
        merchant.setSlug( entity.slug() );
        merchant.setCustomDomain( entity.customDomain() );
        if ( entity.published() != null ) {
            merchant.setPublished( entity.published() );
        }
        merchant.setStoreInformation( storeInformationCommandToStoreInformation( entity.storeInformation() ) );
        merchant.setBusinessInformation( businessInformationCommandToBusinessInformation( entity.businessInformation() ) );

        return merchant;
    }

    @Override
    public List<Merchant> toEntities(List<MerchantCommand> entity) {
        if ( entity == null ) {
            return null;
        }

        List<Merchant> list = new ArrayList<Merchant>( entity.size() );
        for ( MerchantCommand merchantCommand : entity ) {
            list.add( toEntity( merchantCommand ) );
        }

        return list;
    }

    @Override
    public void update(MerchantCommand command, Merchant entity) {
        if ( command == null ) {
            return;
        }

        if ( command.name() != null ) {
            entity.setName( command.name() );
        }
        if ( command.slug() != null ) {
            entity.setSlug( command.slug() );
        }
        if ( command.customDomain() != null ) {
            entity.setCustomDomain( command.customDomain() );
        }
        if ( command.published() != null ) {
            entity.setPublished( command.published() );
        }
        if ( command.storeInformation() != null ) {
            if ( entity.getStoreInformation() == null ) {
                entity.setStoreInformation( new StoreInformation() );
            }
            storeInformationCommandToStoreInformation1( command.storeInformation(), entity.getStoreInformation() );
        }
        if ( command.businessInformation() != null ) {
            if ( entity.getBusinessInformation() == null ) {
                entity.setBusinessInformation( new BusinessInformation() );
            }
            businessInformationCommandToBusinessInformation1( command.businessInformation(), entity.getBusinessInformation() );
        }
    }

    protected StoreInformationResponse storeInformationToStoreInformationResponse(StoreInformation storeInformation) {
        if ( storeInformation == null ) {
            return null;
        }

        String description = null;
        String logoUrl = null;
        String faviconUrl = null;
        String contactEmail = null;
        String contactPhone = null;

        description = storeInformation.getDescription();
        logoUrl = storeInformation.getLogoUrl();
        faviconUrl = storeInformation.getFaviconUrl();
        contactEmail = storeInformation.getContactEmail();
        contactPhone = storeInformation.getContactPhone();

        StoreInformationResponse storeInformationResponse = new StoreInformationResponse( description, logoUrl, faviconUrl, contactEmail, contactPhone );

        return storeInformationResponse;
    }

    protected BusinessInformationResponse businessInformationToBusinessInformationResponse(BusinessInformation businessInformation) {
        if ( businessInformation == null ) {
            return null;
        }

        String name = null;
        String address = null;
        String email = null;
        String phone = null;
        String taxId = null;

        name = businessInformation.getName();
        address = businessInformation.getAddress();
        email = businessInformation.getEmail();
        phone = businessInformation.getPhone();
        taxId = businessInformation.getTaxId();

        BusinessInformationResponse businessInformationResponse = new BusinessInformationResponse( name, address, email, phone, taxId );

        return businessInformationResponse;
    }

    protected StoreInformation storeInformationCommandToStoreInformation(StoreInformationCommand storeInformationCommand) {
        if ( storeInformationCommand == null ) {
            return null;
        }

        StoreInformation storeInformation = new StoreInformation();

        storeInformation.setDescription( storeInformationCommand.description() );
        storeInformation.setLogoUrl( storeInformationCommand.logoUrl() );
        storeInformation.setFaviconUrl( storeInformationCommand.faviconUrl() );
        storeInformation.setContactEmail( storeInformationCommand.contactEmail() );
        storeInformation.setContactPhone( storeInformationCommand.contactPhone() );

        return storeInformation;
    }

    protected BusinessInformation businessInformationCommandToBusinessInformation(BusinessInformationCommand businessInformationCommand) {
        if ( businessInformationCommand == null ) {
            return null;
        }

        BusinessInformation businessInformation = new BusinessInformation();

        businessInformation.setName( businessInformationCommand.name() );
        businessInformation.setAddress( businessInformationCommand.address() );
        businessInformation.setEmail( businessInformationCommand.email() );
        businessInformation.setPhone( businessInformationCommand.phone() );
        businessInformation.setTaxId( businessInformationCommand.taxId() );

        return businessInformation;
    }

    protected void storeInformationCommandToStoreInformation1(StoreInformationCommand storeInformationCommand, StoreInformation mappingTarget) {
        if ( storeInformationCommand == null ) {
            return;
        }

        if ( storeInformationCommand.description() != null ) {
            mappingTarget.setDescription( storeInformationCommand.description() );
        }
        if ( storeInformationCommand.logoUrl() != null ) {
            mappingTarget.setLogoUrl( storeInformationCommand.logoUrl() );
        }
        if ( storeInformationCommand.faviconUrl() != null ) {
            mappingTarget.setFaviconUrl( storeInformationCommand.faviconUrl() );
        }
        if ( storeInformationCommand.contactEmail() != null ) {
            mappingTarget.setContactEmail( storeInformationCommand.contactEmail() );
        }
        if ( storeInformationCommand.contactPhone() != null ) {
            mappingTarget.setContactPhone( storeInformationCommand.contactPhone() );
        }
    }

    protected void businessInformationCommandToBusinessInformation1(BusinessInformationCommand businessInformationCommand, BusinessInformation mappingTarget) {
        if ( businessInformationCommand == null ) {
            return;
        }

        if ( businessInformationCommand.name() != null ) {
            mappingTarget.setName( businessInformationCommand.name() );
        }
        if ( businessInformationCommand.address() != null ) {
            mappingTarget.setAddress( businessInformationCommand.address() );
        }
        if ( businessInformationCommand.email() != null ) {
            mappingTarget.setEmail( businessInformationCommand.email() );
        }
        if ( businessInformationCommand.phone() != null ) {
            mappingTarget.setPhone( businessInformationCommand.phone() );
        }
        if ( businessInformationCommand.taxId() != null ) {
            mappingTarget.setTaxId( businessInformationCommand.taxId() );
        }
    }
}
