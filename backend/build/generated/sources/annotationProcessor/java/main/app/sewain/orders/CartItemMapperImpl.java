package app.sewain.orders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
class CartItemMapperImpl implements CartItemMapper {

    @Autowired
    private RentalPlansMapper rentalPlansMapper;

    @Override
    public CartItemResponse toResponse(CartItemProjection projection) {
        if ( projection == null ) {
            return null;
        }

        List<Map<String, Object>> rentalPlans = null;
        UUID id = null;
        UUID productId = null;
        String productName = null;
        String productSlug = null;
        List<String> mediaList = null;
        UUID variantId = null;
        String variantName = null;
        int quantity = 0;
        OffsetDateTime rentalStartAt = null;
        OffsetDateTime rentalEndAt = null;
        BigDecimal rentalSubtotal = null;
        BigDecimal depositSubtotal = null;
        BigDecimal totalAmount = null;
        OffsetDateTime createdAt = null;
        OffsetDateTime updatedAt = null;

        rentalPlans = rentalPlansMapper.map( projection.getRentalPlans() );
        id = projection.getId();
        productId = projection.getProductId();
        productName = projection.getProductName();
        productSlug = projection.getProductSlug();
        List<String> list1 = projection.getMediaList();
        if ( list1 != null ) {
            mediaList = new ArrayList<String>( list1 );
        }
        variantId = projection.getVariantId();
        variantName = projection.getVariantName();
        quantity = projection.getQuantity();
        rentalStartAt = projection.getRentalStartAt();
        rentalEndAt = projection.getRentalEndAt();
        rentalSubtotal = projection.getRentalSubtotal();
        depositSubtotal = projection.getDepositSubtotal();
        totalAmount = projection.getTotalAmount();
        createdAt = projection.getCreatedAt();
        updatedAt = projection.getUpdatedAt();

        CartItemResponse cartItemResponse = new CartItemResponse( id, productId, productName, productSlug, mediaList, variantId, variantName, quantity, rentalStartAt, rentalEndAt, rentalPlans, rentalSubtotal, depositSubtotal, totalAmount, createdAt, updatedAt );

        return cartItemResponse;
    }

    @Override
    public List<CartItemResponse> toResponses(List<CartItemProjection> projections) {
        if ( projections == null ) {
            return null;
        }

        List<CartItemResponse> list = new ArrayList<CartItemResponse>( projections.size() );
        for ( CartItemProjection cartItemProjection : projections ) {
            list.add( toResponse( cartItemProjection ) );
        }

        return list;
    }

    @Override
    public CartItemResponse toResponse(CartItem item) {
        if ( item == null ) {
            return null;
        }

        UUID id = null;
        UUID productId = null;
        UUID variantId = null;
        int quantity = 0;
        OffsetDateTime rentalStartAt = null;
        OffsetDateTime rentalEndAt = null;
        List<Map<String, Object>> rentalPlans = null;
        BigDecimal rentalSubtotal = null;
        BigDecimal depositSubtotal = null;
        BigDecimal totalAmount = null;
        OffsetDateTime createdAt = null;
        OffsetDateTime updatedAt = null;

        id = item.getId();
        productId = item.getProductId();
        variantId = item.getVariantId();
        quantity = item.getQuantity();
        rentalStartAt = item.getRentalStartAt();
        rentalEndAt = item.getRentalEndAt();
        List<Map<String, Object>> list = item.getRentalPlans();
        if ( list != null ) {
            rentalPlans = new ArrayList<Map<String, Object>>( list );
        }
        rentalSubtotal = item.getRentalSubtotal();
        depositSubtotal = item.getDepositSubtotal();
        totalAmount = item.getTotalAmount();
        createdAt = item.getCreatedAt();
        updatedAt = item.getUpdatedAt();

        String productName = null;
        String productSlug = null;
        List<String> mediaList = null;
        String variantName = null;

        CartItemResponse cartItemResponse = new CartItemResponse( id, productId, productName, productSlug, mediaList, variantId, variantName, quantity, rentalStartAt, rentalEndAt, rentalPlans, rentalSubtotal, depositSubtotal, totalAmount, createdAt, updatedAt );

        return cartItemResponse;
    }

    @Override
    public CartItem toEntity(CartItemCommand command) {
        if ( command == null ) {
            return null;
        }

        CartItem cartItem = new CartItem();

        cartItem.setProductId( command.productId() );
        cartItem.setVariantId( command.variantId() );
        cartItem.setQuantity( command.quantity() );
        cartItem.setRentalStartAt( command.rentalStartAt() );
        cartItem.setRentalEndAt( command.rentalEndAt() );
        List<Map<String, Object>> list = command.rentalPlans();
        if ( list != null ) {
            cartItem.setRentalPlans( new ArrayList<Map<String, Object>>( list ) );
        }
        cartItem.setRentalSubtotal( command.rentalSubtotal() );
        cartItem.setDepositSubtotal( command.depositSubtotal() );
        cartItem.setTotalAmount( command.totalAmount() );

        return cartItem;
    }

    @Override
    public void update(CartItemCommand command, CartItem item) {
        if ( command == null ) {
            return;
        }

        if ( command.productId() != null ) {
            item.setProductId( command.productId() );
        }
        if ( command.variantId() != null ) {
            item.setVariantId( command.variantId() );
        }
        item.setQuantity( command.quantity() );
        if ( command.rentalStartAt() != null ) {
            item.setRentalStartAt( command.rentalStartAt() );
        }
        if ( command.rentalEndAt() != null ) {
            item.setRentalEndAt( command.rentalEndAt() );
        }
        if ( item.getRentalPlans() != null ) {
            List<Map<String, Object>> list = command.rentalPlans();
            if ( list != null ) {
                item.getRentalPlans().clear();
                item.getRentalPlans().addAll( list );
            }
        }
        else {
            List<Map<String, Object>> list = command.rentalPlans();
            if ( list != null ) {
                item.setRentalPlans( new ArrayList<Map<String, Object>>( list ) );
            }
        }
        if ( command.rentalSubtotal() != null ) {
            item.setRentalSubtotal( command.rentalSubtotal() );
        }
        if ( command.depositSubtotal() != null ) {
            item.setDepositSubtotal( command.depositSubtotal() );
        }
        if ( command.totalAmount() != null ) {
            item.setTotalAmount( command.totalAmount() );
        }
    }
}
