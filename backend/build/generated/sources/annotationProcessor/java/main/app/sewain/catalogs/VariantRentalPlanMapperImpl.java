package app.sewain.catalogs;

import app.sewain.shared.DurationUnit;
import java.math.BigDecimal;
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
class VariantRentalPlanMapperImpl implements VariantRentalPlanMapper {

    @Override
    public VariantRentalPlanResponse toResponse(VariantRentalPlan entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        DurationUnit durationUnit = null;
        int durationValue = 0;
        BigDecimal rentalFee = null;
        boolean active = false;
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;

        id = entity.getId();
        durationUnit = entity.getDurationUnit();
        durationValue = entity.getDurationValue();
        rentalFee = entity.getRentalFee();
        active = entity.isActive();
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();

        VariantRentalPlanResponse variantRentalPlanResponse = new VariantRentalPlanResponse( id, durationUnit, durationValue, rentalFee, active, createdAt, updatedAt );

        return variantRentalPlanResponse;
    }

    @Override
    public List<VariantRentalPlanResponse> toResponses(Iterable<VariantRentalPlan> entity) {
        if ( entity == null ) {
            return null;
        }

        List<VariantRentalPlanResponse> list = new ArrayList<VariantRentalPlanResponse>();
        for ( VariantRentalPlan variantRentalPlan : entity ) {
            list.add( toResponse( variantRentalPlan ) );
        }

        return list;
    }

    @Override
    public List<VariantRentalPlanResponse> toResponses(List<VariantRentalPlan> entity) {
        if ( entity == null ) {
            return null;
        }

        List<VariantRentalPlanResponse> list = new ArrayList<VariantRentalPlanResponse>( entity.size() );
        for ( VariantRentalPlan variantRentalPlan : entity ) {
            list.add( toResponse( variantRentalPlan ) );
        }

        return list;
    }

    @Override
    public List<VariantRentalPlan> toEntities(List<VariantRentalPlanCommand> entity) {
        if ( entity == null ) {
            return null;
        }

        List<VariantRentalPlan> list = new ArrayList<VariantRentalPlan>( entity.size() );
        for ( VariantRentalPlanCommand variantRentalPlanCommand : entity ) {
            list.add( toEntity( variantRentalPlanCommand ) );
        }

        return list;
    }

    @Override
    public VariantRentalPlan toEntity(VariantRentalPlanCommand command) {
        if ( command == null ) {
            return null;
        }

        VariantRentalPlan variantRentalPlan = new VariantRentalPlan();

        if ( command.active() != null ) {
            variantRentalPlan.setActive( command.active() );
        }
        else {
            variantRentalPlan.setActive( true );
        }
        variantRentalPlan.setDurationUnit( command.durationUnit() );
        variantRentalPlan.setDurationValue( command.durationValue() );
        variantRentalPlan.setRentalFee( command.rentalFee() );

        return variantRentalPlan;
    }

    @Override
    public void update(VariantRentalPlanCommand domain, VariantRentalPlan entity) {
        if ( domain == null ) {
            return;
        }

        if ( domain.active() != null ) {
            entity.setActive( domain.active() );
        }
        else {
            entity.setActive( true );
        }
        if ( domain.durationUnit() != null ) {
            entity.setDurationUnit( domain.durationUnit() );
        }
        entity.setDurationValue( domain.durationValue() );
        if ( domain.rentalFee() != null ) {
            entity.setRentalFee( domain.rentalFee() );
        }
    }
}
