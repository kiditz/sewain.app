package app.sewain.orders;

import java.time.OffsetDateTime;
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
class CartMapperImpl implements CartMapper {

    @Autowired
    private CartItemMapper cartItemMapper;

    @Override
    public CartResponse toResponse(Cart cart) {
        if ( cart == null ) {
            return null;
        }

        UUID id = null;
        UUID customerId = null;
        UUID guestToken = null;
        String status = null;
        OffsetDateTime expiresAt = null;
        List<CartItemResponse> items = null;
        OffsetDateTime createdAt = null;
        OffsetDateTime updatedAt = null;

        id = cart.getId();
        customerId = cart.getCustomerId();
        guestToken = cart.getGuestToken();
        if ( cart.getStatus() != null ) {
            status = cart.getStatus().name();
        }
        expiresAt = cart.getExpiresAt();
        items = cartItemListToCartItemResponseList( cart.getItems() );
        createdAt = cart.getCreatedAt();
        updatedAt = cart.getUpdatedAt();

        CartResponse cartResponse = new CartResponse( id, customerId, guestToken, status, expiresAt, items, createdAt, updatedAt );

        return cartResponse;
    }

    @Override
    public CartResponse toResponse(Cart cart, List<CartItemResponse> items) {
        if ( cart == null && items == null ) {
            return null;
        }

        UUID id = null;
        UUID customerId = null;
        UUID guestToken = null;
        String status = null;
        OffsetDateTime expiresAt = null;
        OffsetDateTime createdAt = null;
        OffsetDateTime updatedAt = null;
        if ( cart != null ) {
            id = cart.getId();
            customerId = cart.getCustomerId();
            guestToken = cart.getGuestToken();
            if ( cart.getStatus() != null ) {
                status = cart.getStatus().name();
            }
            expiresAt = cart.getExpiresAt();
            createdAt = cart.getCreatedAt();
            updatedAt = cart.getUpdatedAt();
        }
        List<CartItemResponse> items1 = null;
        List<CartItemResponse> list = items;
        if ( list != null ) {
            items1 = new ArrayList<CartItemResponse>( list );
        }

        CartResponse cartResponse = new CartResponse( id, customerId, guestToken, status, expiresAt, items1, createdAt, updatedAt );

        return cartResponse;
    }

    protected List<CartItemResponse> cartItemListToCartItemResponseList(List<CartItem> list) {
        if ( list == null ) {
            return null;
        }

        List<CartItemResponse> list1 = new ArrayList<CartItemResponse>( list.size() );
        for ( CartItem cartItem : list ) {
            list1.add( cartItemMapper.toResponse( cartItem ) );
        }

        return list1;
    }
}
