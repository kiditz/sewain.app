package app.sewain.orders;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CartMappingTest {
    final CartItemMapper items=Mappers.getMapper(CartItemMapper.class);
    final CartMapper carts=Mappers.getMapper(CartMapper.class);
    CartMappingTest() {
        ReflectionTestUtils.setField(items,"rentalPlansMapper",new RentalPlansMapper(new ObjectMapper()));
        ReflectionTestUtils.setField(carts,"cartItemMapper",items);
    }
    CartItemCommand command(int quantity) {
        var start=OffsetDateTime.parse("2026-12-01T00:00:00Z");
        return new CartItemCommand(UUID.randomUUID(),UUID.randomUUID(),quantity,start,start.plusDays(2),
                List.of(Map.of("durationUnit","DAY","rentalFee",100)),new BigDecimal("200"),new BigDecimal("50"),new BigDecimal("250"));
    }
    @Test void itemRoundTripPreservesRentalWindowPlansAndMoney() {
        var command=command(2); var entity=items.toEntity(command); var response=items.toResponse(entity);
        assertEquals(command.productId(),response.productId()); assertEquals(command.variantId(),response.variantId());
        assertEquals(2,response.quantity()); assertEquals(command.rentalStartAt(),response.rentalStartAt()); assertEquals(command.rentalEndAt(),response.rentalEndAt());
        assertEquals(command.rentalPlans(),response.rentalPlans()); assertEquals(command.rentalSubtotal(),response.rentalSubtotal());
        assertEquals(command.depositSubtotal(),response.depositSubtotal()); assertEquals(command.totalAmount(),response.totalAmount());
    }
    @Test void itemUpdatePreservesIdentityAndCartOwnership() {
        var entity=items.toEntity(command(1)); var id=UUID.randomUUID(); entity.setId(id); var cart=new Cart(); entity.setCart(cart);
        var updated=command(3); items.update(updated,entity);
        assertEquals(id,entity.getId()); assertSame(cart,entity.getCart()); assertEquals(3,entity.getQuantity());
        assertEquals(updated.variantId(),entity.getVariantId()); assertEquals(updated.rentalPlans(),entity.getRentalPlans());
    }
    @Test void projectedItemsIncludePublicCatalogMetadataAndPlanSnapshot() {
        var projection=mock(CartItemProjection.class); var id=UUID.randomUUID(); var product=UUID.randomUUID(); var variant=UUID.randomUUID();
        when(projection.getId()).thenReturn(id); when(projection.getProductId()).thenReturn(product); when(projection.getVariantId()).thenReturn(variant);
        when(projection.getProductName()).thenReturn("Tent"); when(projection.getProductSlug()).thenReturn("tent");
        when(projection.getVariantName()).thenReturn("4 Person"); when(projection.getMediaList()).thenReturn(List.of("/tent.png"));
        when(projection.getQuantity()).thenReturn(2); when(projection.getTotalAmount()).thenReturn(new BigDecimal("250"));
        when(projection.getRentalPlans()).thenReturn(List.of(Map.of("durationUnit","DAY")));
        var response=items.toResponses(List.of(projection)).getFirst();
        assertEquals(id,response.id()); assertEquals(product,response.productId()); assertEquals(variant,response.variantId());
        assertEquals("Tent",response.productName()); assertEquals("tent",response.productSlug()); assertEquals("4 Person",response.variantName());
        assertEquals(List.of("/tent.png"),response.mediaList()); assertEquals(2,response.quantity());
        assertEquals("DAY",response.rentalPlans().getFirst().get("durationUnit"));
    }
    @Test void cartMapsOwnershipAndCanUseHydratedProjectionItems() {
        var cart=new Cart(); cart.setId(UUID.randomUUID()); cart.setGuestToken(UUID.randomUUID()); cart.setCustomerId(UUID.randomUUID());
        cart.addItem(items.toEntity(command(1)));
        var response=carts.toResponse(cart);
        assertEquals(cart.getId(),response.id()); assertEquals(cart.getGuestToken(),response.guestToken()); assertEquals(cart.getCustomerId(),response.customerId());
        assertEquals("ACTIVE",response.status()); assertEquals(1,response.items().size());
        var hydrated=items.toResponse(cart.getItems().getFirst());
        assertEquals(List.of(hydrated),carts.toResponse(cart,List.of(hydrated)).items());
    }
    @Test void absentPlanSnapshotsProduceEmptyPlanList() {
        var projection=mock(CartItemProjection.class);
        assertTrue(items.toResponse(projection).rentalPlans().isEmpty());
        when(projection.getRentalPlans()).thenReturn(Arrays.asList(Map.of("durationUnit","DAY"),"invalid",null));
        assertEquals(1,items.toResponse(projection).rentalPlans().size());
    }
}
