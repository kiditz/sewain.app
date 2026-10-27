package app.sewain.orders;

import app.sewain.exception.ConflictException;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import app.sewain.exception.ValidationException;
import app.sewain.shared.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
class MerchantOrderLifecycleService {
    private final RentalOrderRepository orderRepository;
    private final MerchantOrderMapper mapper;
    private final OrderItemRepository itemRepository;
    private final PaymentRepository paymentRepository;
    private final MerchantOrderLateFeeService lateFeeService;
    private final RentalAllocationRepository allocationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MerchantOrderQueryService queryService;
    private final MerchantOrderPaymentService paymentService;

    @Transactional
    MerchantOrderResponse recordReturn(UUID orderId, RecordOrderReturnCommand command) {
        var order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.ACTIVE_RENTAL)
            throw new ConflictException(ErrorCode.ORDER_RETURN_NOT_ALLOWED);

        var now = OffsetDateTime.now(ZoneId.systemDefault());
        var returnedAt = command.returnedAt() == null ? now : command.returnedAt();
        var items = itemRepository.findAllByOrder_IdOrderByCreatedAtAsc(orderId);
        var earliestStart = items.stream().map(OrderItem::getRentalStartAt).min(Comparator.naturalOrder());
        if (returnedAt.isAfter(now.plusMinutes(5)) || earliestStart.map(returnedAt::isBefore).orElse(false))
            throw new ValidationException(ErrorCode.ORDER_RETURN_TIME_INVALID);

        var quote = lateFeeService.calculate(order, items, returnedAt);
        var waive = command.waiveLateFee();
        var reason = command.waiverReason() == null ? "" : command.waiverReason().trim();
        if (waive && reason.isEmpty())
            throw new ValidationException(ErrorCode.ORDER_LATE_FEE_WAIVER_REASON_REQUIRED);
        if ((!waive && !reason.isEmpty()) || (waive && quote.amount().signum() == 0))
            throw new ValidationException(ErrorCode.ORDER_LATE_FEE_WAIVER_INVALID);

        applyLateFee(order, quote, returnedAt, waive, waive ? reason : null);
        orderRepository.save(order);
        return queryService.get(order.getId());
    }

    @Transactional
    MerchantOrderResponse updateStatus(UUID orderId, OrderStatus next) {
        var order = findOrder(orderId);
        var current = order.getStatus();
        if (!isAllowedTransition(current, next))
            throw new ConflictException(ErrorCode.ORDER_STATUS_TRANSITION_INVALID);
        var allocations = allocationRepository.findAllByOrderItem_Order_Id(orderId);
        validateStatusPaymentRequirements(order, next);
        applyStatusEffects(order, next, allocations);
        order.setStatus(next);
        orderRepository.save(order);
        allocationRepository.saveAll(allocations);
        publishActivity(order, "STATUS_CHANGED", current, next, null, null);
        return queryService.get(order.getId());
    }

    private RentalOrder findOrder(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void validateStatusPaymentRequirements(RentalOrder order, OrderStatus next) {
        if (next == OrderStatus.COMPLETED) ensureReturnRecorded(order);
        boolean paymentDue = (next == OrderStatus.ACTIVE_RENTAL && order.getBalanceDueAt() == BalanceDueAt.PICKUP)
                || (next == OrderStatus.COMPLETED && order.getBalanceDueAt() == BalanceDueAt.RETURN);
        if (paymentDue) paymentService.validateRentalAmountPaid(order, paymentRepository.findAllByOrder_Id(order.getId()));
    }

    private void ensureReturnRecorded(RentalOrder order) {
        if (order.getReturnedAt() == null) {
            var now = OffsetDateTime.now(ZoneId.systemDefault());
            var items = itemRepository.findAllByOrder_IdOrderByCreatedAtAsc(order.getId());
            var quote = lateFeeService.calculate(order, items, now);
            if (quote.amount().signum() > 0)
                throw new ValidationException(ErrorCode.ORDER_LATE_FEE_RETURN_REQUIRED);
            applyLateFee(order, quote, now, false, null);
        }
        if (order.getLateFeeTotal().signum() > 0)
            paymentService.validateRentalAmountPaid(order, paymentRepository.findAllByOrder_Id(order.getId()));
    }

    private void applyLateFee(RentalOrder order, LateFeeCalculator.Quote quote, OffsetDateTime returnedAt,
            boolean waived, String waiverReason) {
        var charged = waived ? BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY) : quote.amount();
        var newTotal = order.getTotalAmount().subtract(order.getLateFeeTotal()).add(charged);
        if (newTotal.compareTo(paymentService.totalReceived(order.getId(), paymentRepository.findAllByOrder_Id(order.getId()))) < 0)
            throw new ValidationException(ErrorCode.ORDER_LATE_FEE_BELOW_RECEIVED);

        order.setReturnedAt(returnedAt);
        order.setLateMinutes(quote.lateMinutes());
        order.setLateFeeUnit(quote.enabled() ? quote.unit().name() : null);
        order.setLateFeeUnitAmount(quote.unitAmount());
        order.setLateFeeUnits(Math.toIntExact(quote.billedUnits()));
        order.setLateFeeAssessed(quote.amount());
        order.setLateFeeTotal(charged);
        order.setLateFeeWaived(waived);
        order.setLateFeeWaiverReason(waiverReason);
        order.setTotalAmount(newTotal);
    }

    private void applyStatusEffects(RentalOrder order, OrderStatus next, List<RentalAllocation> allocations) {
        switch (next) {
            case CANCELLED -> cancelOrder(order, allocations);
            case CONFIRMED -> paymentService.confirmOrder(order, paymentRepository.findAllByOrder_Id(order.getId()));
            case ACTIVE_RENTAL -> activateRental(allocations);
            case COMPLETED -> completeRental(order, allocations);
            default -> { }
        }
    }

    private void cancelOrder(RentalOrder order, List<RentalAllocation> allocations) {
        order.setCancelledAt(OffsetDateTime.now(ZoneId.systemDefault()));
        paymentRepository.findAllByOrder_Id(order.getId()).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                .forEach(payment -> mapper.updatePaymentStatus(PaymentStatus.CANCELLED, payment));
        allocations.stream()
                .filter(allocation -> allocation.getStatus() == RentalAllocationStatus.HELD || allocation.getStatus() == RentalAllocationStatus.CONFIRMED)
                .forEach(allocation -> allocation.setStatus(RentalAllocationStatus.RELEASED));
    }

    private void activateRental(List<RentalAllocation> allocations) {
        var activeAllocations = allocations.stream().filter(allocation -> allocation.getStatus() == RentalAllocationStatus.CONFIRMED).toList();
        activeAllocations.forEach(allocation -> allocation.setHoldExpiresAt(null));
        publishInventoryStatusChange(activeAllocations, "RENTED");
    }

    private void completeRental(RentalOrder order, List<RentalAllocation> allocations) {
        order.setCompletedAt(OffsetDateTime.now(ZoneId.systemDefault()));
        var completedAllocations = allocations.stream().filter(allocation -> allocation.getStatus() == RentalAllocationStatus.CONFIRMED).toList();
        allocations.forEach(allocation -> allocation.setStatus(RentalAllocationStatus.COMPLETED));
        publishInventoryStatusChange(completedAllocations, "AVAILABLE");
    }

    private void publishInventoryStatusChange(List<RentalAllocation> allocations, String status) {
        var inventoryUnitIds = allocations.stream().map(RentalAllocation::getInventoryUnitId).distinct().toList();
        if (!inventoryUnitIds.isEmpty())
            eventPublisher.publishEvent(new InventoryUnitStatusChangedEvent(inventoryUnitIds, status, app.sewain.tenant.TenantContext.get()));
    }

    private void publishActivity(RentalOrder order, String eventType, OrderStatus previousStatus,
            OrderStatus currentStatus, PaymentStage paymentStage, BigDecimal amount) {
        eventPublisher.publishEvent(new MerchantOrderActivityEvent(order.getId(), order.getOrderNumber(), order.getCustomerName(),
                eventType, previousStatus == null ? null : previousStatus.name(), currentStatus == null ? null : currentStatus.name(),
                paymentStage == null ? null : paymentStage.name(), amount, app.sewain.tenant.TenantContext.get()));
    }

    private boolean isAllowedTransition(OrderStatus current, OrderStatus next) {
        return switch (current) {
            case PENDING_PAYMENT -> next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED;
            case PROCESSING -> next == OrderStatus.READY || next == OrderStatus.CANCELLED;
            case READY -> next == OrderStatus.ACTIVE_RENTAL || next == OrderStatus.CANCELLED;
            case ACTIVE_RENTAL -> next == OrderStatus.COMPLETED;
            case COMPLETED, CANCELLED, EXPIRED -> false;
        };
    }
}
