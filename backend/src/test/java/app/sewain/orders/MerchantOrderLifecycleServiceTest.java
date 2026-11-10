package app.sewain.orders;

import app.sewain.exception.*;
import app.sewain.shared.InventoryUnitStatusChangedEvent;
import app.sewain.tenant.TenantContext;
import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationEventPublisher;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MerchantOrderLifecycleServiceTest {
    private final RentalOrderRepository orders=mock(RentalOrderRepository.class);
    private final MerchantOrderMapper mapper=mock(MerchantOrderMapper.class);
    private final OrderItemRepository items=mock(OrderItemRepository.class);
    private final PaymentRepository payments=mock(PaymentRepository.class);
    private final MerchantOrderLateFeeService lateFees=mock(MerchantOrderLateFeeService.class);
    private final RentalAllocationRepository allocations=mock(RentalAllocationRepository.class);
    private final ApplicationEventPublisher events=mock(ApplicationEventPublisher.class);
    private final MerchantOrderQueryService queries=mock(MerchantOrderQueryService.class);
    private final MerchantOrderPaymentService receipts=mock(MerchantOrderPaymentService.class);
    private final MerchantOrderLifecycleService service=new MerchantOrderLifecycleService(orders,mapper,items,payments,lateFees,allocations,events,queries,receipts);
    private RentalOrder order;
    @BeforeEach void setup() {
        TenantContext.set("outdoor");
        order=new RentalOrder(); order.setId(UUID.randomUUID());
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
    }
    @AfterEach void clear() { TenantContext.clear(); }
    private RentalAllocation allocation(RentalAllocationStatus status,UUID inventory) {
        var allocation=new RentalAllocation(); allocation.setStatus(status); allocation.setInventoryUnitId(inventory); allocation.setHoldExpiresAt(OffsetDateTime.now().plusHours(1)); return allocation;
    }
    @Test void rejectsSkippingPickupAndChangesToTerminalOrdersWithoutWrites() {
        for (var status:List.of(OrderStatus.PENDING_PAYMENT,OrderStatus.CONFIRMED,OrderStatus.PROCESSING,OrderStatus.COMPLETED,OrderStatus.CANCELLED,OrderStatus.EXPIRED)) {
            order.setStatus(status);
            assertThrows(ConflictException.class,()->service.updateStatus(order.getId(),OrderStatus.ACTIVE_RENTAL),status.name());
            assertEquals(status,order.getStatus());
        }
        verify(orders,never()).save(any()); verifyNoInteractions(allocations,receipts,events);
    }
    @Test void cancellationReleasesReservedStockButDoesNotCancelSettledPayment() {
        order.setStatus(OrderStatus.READY);
        var held=allocation(RentalAllocationStatus.HELD,UUID.randomUUID()); var confirmed=allocation(RentalAllocationStatus.CONFIRMED,UUID.randomUUID());
        when(allocations.findAllByOrderItem_Order_Id(order.getId())).thenReturn(List.of(held,confirmed));
        var pending=new Payment(); pending.setStatus(PaymentStatus.PENDING);
        var settled=new Payment(); settled.setStatus(PaymentStatus.SUCCEEDED);
        when(payments.findAllByOrder_Id(order.getId())).thenReturn(List.of(pending,settled));
        service.updateStatus(order.getId(),OrderStatus.CANCELLED);
        assertEquals(OrderStatus.CANCELLED,order.getStatus()); assertNotNull(order.getCancelledAt());
        assertEquals(RentalAllocationStatus.RELEASED,held.getStatus()); assertEquals(RentalAllocationStatus.RELEASED,confirmed.getStatus());
        verify(mapper).updatePaymentStatus(PaymentStatus.CANCELLED,pending);
        verify(mapper,never()).updatePaymentStatus(any(),eq(settled));
    }
    @Test void pickupPublishesDistinctConfirmedInventoryIdsWithTenantAndClearsHolds() {
        order.setStatus(OrderStatus.READY); var inventory=UUID.randomUUID();
        var first=allocation(RentalAllocationStatus.CONFIRMED,inventory); var second=allocation(RentalAllocationStatus.CONFIRMED,inventory);
        var released=allocation(RentalAllocationStatus.RELEASED,UUID.randomUUID());
        when(allocations.findAllByOrderItem_Order_Id(order.getId())).thenReturn(List.of(first,second,released));
        service.updateStatus(order.getId(),OrderStatus.ACTIVE_RENTAL);
        assertEquals(OrderStatus.ACTIVE_RENTAL,order.getStatus()); assertNull(first.getHoldExpiresAt()); assertNull(second.getHoldExpiresAt());
        assertNotNull(released.getHoldExpiresAt());
        verify(events).publishEvent(new InventoryUnitStatusChangedEvent(List.of(inventory),"RENTED","outdoor"));
    }
    @Test void unpaidPickupDoesNotChangeOrderOrStock() {
        order.setStatus(OrderStatus.READY); order.setBalanceDueAt(BalanceDueAt.PICKUP);
        var allocation=allocation(RentalAllocationStatus.CONFIRMED,UUID.randomUUID());
        when(allocations.findAllByOrderItem_Order_Id(order.getId())).thenReturn(List.of(allocation));
        doThrow(new ValidationException("balance required")).when(receipts).validateRentalAmountPaid(eq(order),anyList());
        assertThrows(ValidationException.class,()->service.updateStatus(order.getId(),OrderStatus.ACTIVE_RENTAL));
        assertEquals(OrderStatus.READY,order.getStatus()); assertNotNull(allocation.getHoldExpiresAt());
        verify(orders,never()).save(any()); verify(allocations,never()).saveAll(any()); verifyNoInteractions(events);
    }
    @Test void completedReturnMakesConfirmedInventoryAvailable() {
        order.setStatus(OrderStatus.ACTIVE_RENTAL); order.setReturnedAt(OffsetDateTime.parse("2026-01-05T12:00:00Z"));
        var inventory=UUID.randomUUID(); var allocation=allocation(RentalAllocationStatus.CONFIRMED,inventory);
        when(allocations.findAllByOrderItem_Order_Id(order.getId())).thenReturn(List.of(allocation));
        service.updateStatus(order.getId(),OrderStatus.COMPLETED);
        assertEquals(OrderStatus.COMPLETED,order.getStatus()); assertNotNull(order.getCompletedAt());
        assertEquals(RentalAllocationStatus.COMPLETED,allocation.getStatus());
        verify(events).publishEvent(new InventoryUnitStatusChangedEvent(List.of(inventory),"AVAILABLE","outdoor"));
    }
}
