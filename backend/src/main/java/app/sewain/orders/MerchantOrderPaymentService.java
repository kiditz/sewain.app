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
class MerchantOrderPaymentService {
    private final RentalOrderRepository orderRepository;
    private final MerchantOrderMapper mapper;
    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository refundRepository;
    private final RentalAllocationRepository allocationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MerchantOrderQueryService queryService;

	@Transactional
	MerchantOrderResponse recordPayment(UUID orderId, RecordOrderPaymentCommand command) {
		var normalizedCommand = new RecordOrderPaymentCommand(command.stage(), command.amount(), command.method(), normalizeReference(command.reference()));
		var order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND));
		var previousStatus = order.getStatus();
		if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.EXPIRED || order.getStatus() == OrderStatus.COMPLETED)
			throw new ConflictException(ErrorCode.ORDER_PAYMENT_NOT_ALLOWED);
		var existingPayments = paymentRepository.findAllByOrder_Id(orderId);
		if (normalizedCommand.stage() == PaymentStage.INITIAL && existingPayments.stream().anyMatch(p -> p.getPaymentStage() == PaymentStage.INITIAL && p.getStatus() == PaymentStatus.SUCCEEDED))
			throw new ConflictException(ErrorCode.ORDER_INITIAL_PAYMENT_ALREADY_PAID);
		validatePaymentAmount(order, normalizedCommand, existingPayments);
		BigDecimal paidSoFar = totalReceived(orderId, existingPayments);
		var existingPending = paymentRepository.findFirstByOrder_IdAndPaymentStageAndStatusOrderByCreatedAtAsc(orderId, normalizedCommand.stage(), PaymentStatus.PENDING);
		BigDecimal amountToAdd = existingPending.map(p -> normalizedCommand.amount().subtract(p.getAmount())).orElse(normalizedCommand.amount());
		if (amountToAdd.signum() < 0 || paidSoFar.add(amountToAdd).compareTo(order.getTotalAmount()) > 0)
			throw new ValidationException(ErrorCode.ORDER_PAYMENT_AMOUNT_INVALID);
		var paymentType = paymentTypeFor(normalizedCommand.stage());
		var pendingPayment = existingPending.orElse(null);
		var payment = createOrConfirmReceipt(order, normalizedCommand, paymentType, pendingPayment);
		var refundedRentalAmounts = successfulRentalRefundAmounts(orderId);
		var rentalPaidSoFar = existingPayments.stream().filter(existing -> existing.getStatus() == PaymentStatus.SUCCEEDED || existing.getStatus() == PaymentStatus.PARTIALLY_REFUNDED).map(existing -> existing.getRentalAmount().subtract(refundedRentalAmounts.getOrDefault(existing.getId(), BigDecimal.ZERO)).max(BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
		var paymentAllocation = allocateReceivedPayment(order, normalizedCommand.stage(), normalizedCommand.amount(), rentalPaidSoFar);
		payment.assignCashAllocation(paymentAllocation.rentalAmount(), paymentAllocation.depositAmount());
		payment = paymentRepository.saveAndFlush(payment);
		eventPublisher.publishEvent(new OrderPaymentReceivedEvent(payment.getId(), order.getId(), order.getOrderNumber(), paymentAllocation.rentalAmount(), paymentAllocation.depositAmount(), order.getCurrency(), normalizedCommand.method().name(), payment.getPaidAt(), app.sewain.tenant.TenantContext.get()));
		if (normalizedCommand.stage() == PaymentStage.INITIAL && order.getStatus() == OrderStatus.PENDING_PAYMENT) {
			existingPayments.add(payment);
			confirmOrder(order, existingPayments);
		}
		orderRepository.save(order);
		publishActivity(order, "PAYMENT_RECORDED", null, null, normalizedCommand.stage(), normalizedCommand.amount());
		if (previousStatus != order.getStatus())
			publishActivity(order, "STATUS_CHANGED", previousStatus, order.getStatus(), null, null);
		return queryService.get(order.getId());
	}

	@Transactional
	MerchantOrderResponse recordRefund(UUID orderId, UUID paymentId, RecordPaymentRefundCommand command) {
		var order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND));
		var payment = paymentRepository.findById(paymentId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_PAYMENT_NOT_FOUND));
		if (!payment.getOrder().getId().equals(orderId)) throw new ResourceNotFoundException(ErrorCode.ORDER_PAYMENT_NOT_FOUND);
		if (payment.getStatus() != PaymentStatus.SUCCEEDED && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED)
			throw new ConflictException(ErrorCode.ORDER_PAYMENT_REFUND_NOT_ALLOWED);
		var successfulRefunds = refundRepository.findAllByPayment_Order_Id(orderId).stream().filter(refund -> refund.getStatus() == PaymentRefundStatus.SUCCEEDED && refund.getPayment().getId().equals(paymentId)).toList();
		var amountRefunded = successfulRefunds.stream().map(PaymentRefund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		var rentalRefunded = successfulRefunds.stream().map(PaymentRefund::getRentalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		var depositRefunded = successfulRefunds.stream().map(PaymentRefund::getDepositAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		var amountRemaining = payment.getAmount().subtract(amountRefunded).max(BigDecimal.ZERO);
		if (command.amount() == null || command.amount().signum() <= 0 || command.amount().compareTo(amountRemaining) > 0)
			throw new ValidationException(ErrorCode.ORDER_PAYMENT_REFUND_AMOUNT_INVALID);
		var allocation = allocateRefund(command.amount(), payment.getRentalAmount().subtract(rentalRefunded).max(BigDecimal.ZERO), payment.getDepositAmount().subtract(depositRefunded).max(BigDecimal.ZERO), amountRemaining);
		var processedAt = OffsetDateTime.now(ZoneId.systemDefault());
		var refund = refundRepository.saveAndFlush(mapper.toSuccessfulRefund(payment, command.amount(), allocation.rentalAmount(), allocation.depositAmount(), command.reason(), processedAt));
		var refundedAfter = amountRefunded.add(command.amount());
		mapper.updatePaymentStatus(refundedAfter.compareTo(payment.getAmount()) >= 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED, payment);
		paymentRepository.save(payment);
		eventPublisher.publishEvent(new OrderPaymentRefundedEvent(refund.getId(), payment.getId(), order.getId(), order.getOrderNumber(), allocation.rentalAmount(), allocation.depositAmount(), payment.getCurrency(), payment.getMethod() == null ? "OTHER" : payment.getMethod().name(), processedAt, app.sewain.tenant.TenantContext.get()));
		publishActivity(order, "PAYMENT_REFUNDED", null, null, payment.getPaymentStage(), command.amount());
		return queryService.get(order.getId());
	}

	@Transactional
	MerchantOrderResponse recordDepositRefund(UUID orderId, RecordDepositRefundCommand command) {
		var order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND));
		if (order.getStatus() != OrderStatus.COMPLETED)
			throw new ValidationException(ErrorCode.ORDER_DEPOSIT_REFUND_RETURN_REQUIRED);

		var payments = paymentRepository.findAllByOrder_Id(orderId).stream().filter(payment -> payment.getStatus() == PaymentStatus.SUCCEEDED || payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED).filter(payment -> payment.getDepositAmount().signum() > 0).sorted(Comparator.comparing(Payment::getPaidAt, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
		var successfulRefunds = refundRepository.findAllByPayment_Order_Id(orderId).stream().filter(refund -> refund.getStatus() == PaymentRefundStatus.SUCCEEDED).toList();
		var refundedAmountByPayment = new HashMap<UUID, BigDecimal>();
		var refundedDepositByPayment = new HashMap<UUID, BigDecimal>();
		for (var refund : successfulRefunds) {
			var paymentId = refund.getPayment().getId();
			refundedAmountByPayment.merge(paymentId, refund.getAmount(), BigDecimal::add);
			refundedDepositByPayment.merge(paymentId, refund.getDepositAmount(), BigDecimal::add);
		}
		var depositAvailable = payments.stream().map(payment -> payment.getDepositAmount().subtract(refundedDepositByPayment.getOrDefault(payment.getId(), BigDecimal.ZERO)).max(BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
		if (command.amount() == null || command.amount().signum() <= 0 || command.amount().compareTo(depositAvailable) > 0)
			throw new ValidationException(ErrorCode.ORDER_DEPOSIT_REFUND_AMOUNT_INVALID);

		var toRefund = command.amount();
		var processedAt = OffsetDateTime.now(ZoneId.systemDefault());
		for (var payment : payments) {
			toRefund = toRefund.subtract(refundDepositFromPayment(order, payment, toRefund, command.reason(), processedAt,
					refundedAmountByPayment, refundedDepositByPayment));
		}
		if (toRefund.signum() > 0) throw new ValidationException(ErrorCode.ORDER_DEPOSIT_REFUND_AMOUNT_INVALID);
		return queryService.get(order.getId());
	}

	private BigDecimal refundDepositFromPayment(RentalOrder order, Payment payment, BigDecimal requested,
			String reason, OffsetDateTime processedAt, Map<UUID, BigDecimal> refundedAmountByPayment,
			Map<UUID, BigDecimal> refundedDepositByPayment) {
		if (requested.signum() <= 0) return BigDecimal.ZERO;
		var paymentId = payment.getId();
		var depositRemaining = payment.getDepositAmount().subtract(refundedDepositByPayment.getOrDefault(paymentId, BigDecimal.ZERO)).max(BigDecimal.ZERO);
		var paymentRemaining = payment.getAmount().subtract(refundedAmountByPayment.getOrDefault(paymentId, BigDecimal.ZERO)).max(BigDecimal.ZERO);
		var refundAmount = requested.min(depositRemaining).min(paymentRemaining);
		if (refundAmount.signum() <= 0) return BigDecimal.ZERO;

		var refund = refundRepository.saveAndFlush(mapper.toSuccessfulRefund(payment, refundAmount, BigDecimal.ZERO, refundAmount, reason, processedAt));
		var refundedAfter = refundedAmountByPayment.getOrDefault(paymentId, BigDecimal.ZERO).add(refundAmount);
		var refundStatus = refundedAfter.compareTo(payment.getAmount()) >= 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
		mapper.updatePaymentStatus(refundStatus, payment);
		paymentRepository.save(payment);
		refundedAmountByPayment.put(paymentId, refundedAfter);
		refundedDepositByPayment.merge(paymentId, refundAmount, BigDecimal::add);
		eventPublisher.publishEvent(new OrderPaymentRefundedEvent(refund.getId(), paymentId, order.getId(), order.getOrderNumber(), BigDecimal.ZERO, refundAmount, payment.getCurrency(), payment.getMethod() == null ? "OTHER" : payment.getMethod().name(), processedAt, app.sewain.tenant.TenantContext.get()));
		publishActivity(order, "DEPOSIT_REFUNDED", null, null, PaymentStage.DEPOSIT, refundAmount);
		return refundAmount;
	}

	private PaymentType paymentTypeFor(PaymentStage stage) {
		if (stage == PaymentStage.DEPOSIT) return PaymentType.DEPOSIT;
		if (stage == PaymentStage.BALANCE || stage == PaymentStage.LATE_FEE) return PaymentType.RENTAL;
		return PaymentType.ORDER;
	}

	private Payment createOrConfirmReceipt(RentalOrder order, RecordOrderPaymentCommand command,
			PaymentType type, Payment pendingPayment) {
		if (pendingPayment != null) return confirmReceipt(pendingPayment, command);
		return mapper.toReceipt(order, command.stage(), type, command.amount(), command.method(),
				OffsetDateTime.now(ZoneId.systemDefault()), command.reference());
	}

	private PaymentAllocation allocateReceivedPayment(RentalOrder order, PaymentStage stage, BigDecimal amount, BigDecimal paidSoFar) {
		if (stage == PaymentStage.DEPOSIT) return new PaymentAllocation(BigDecimal.ZERO, amount);
		boolean separateDeposit = order.getPaymentTermsSnapshot().get("payment") instanceof Map<?, ?> config && Boolean.TRUE.equals(config.get("separateDeposit"));
		if (separateDeposit) return new PaymentAllocation(amount, BigDecimal.ZERO);
		BigDecimal rentalDue = order.getTotalAmount().subtract(order.getDepositTotal()).max(BigDecimal.ZERO);
		BigDecimal rentalAmount = amount.min(rentalDue.subtract(paidSoFar).max(BigDecimal.ZERO));
		return new PaymentAllocation(rentalAmount, amount.subtract(rentalAmount));
	}

	private PaymentAllocation allocateRefund(BigDecimal amount, BigDecimal rentalRemaining, BigDecimal depositRemaining, BigDecimal totalRemaining) {
		if (amount.compareTo(totalRemaining) == 0) return new PaymentAllocation(rentalRemaining, depositRemaining);
		if (totalRemaining.signum() == 0) return new PaymentAllocation(BigDecimal.ZERO, BigDecimal.ZERO);
		BigDecimal rentalAmount = amount.multiply(rentalRemaining).divide(totalRemaining, 2, RoundingMode.HALF_UP).min(rentalRemaining);
		BigDecimal depositAmount = amount.subtract(rentalAmount).min(depositRemaining);
		if (rentalAmount.add(depositAmount).compareTo(amount) < 0)
			rentalAmount = amount.subtract(depositAmount).min(rentalRemaining);
		return new PaymentAllocation(rentalAmount, depositAmount);
	}

	private Map<UUID, BigDecimal> successfulRefundAmounts(UUID orderId) {
		var refundedAmounts = new HashMap<UUID, BigDecimal>();
		refundRepository.findAllByPayment_Order_Id(orderId).stream().filter(refund -> refund.getStatus() == PaymentRefundStatus.SUCCEEDED).forEach(refund -> refundedAmounts.merge(refund.getPayment().getId(), refund.getAmount(), BigDecimal::add));
		return refundedAmounts;
	}

	private Map<UUID, BigDecimal> successfulRentalRefundAmounts(UUID orderId) {
		var refundedAmounts = new HashMap<UUID, BigDecimal>();
		refundRepository.findAllByPayment_Order_Id(orderId).stream().filter(refund -> refund.getStatus() == PaymentRefundStatus.SUCCEEDED).forEach(refund -> refundedAmounts.merge(refund.getPayment().getId(), refund.getRentalAmount(), BigDecimal::add));
		return refundedAmounts;
	}

	BigDecimal totalReceived(UUID orderId, List<Payment> payments) {
		var refundedAmounts = successfulRefundAmounts(orderId);
		return payments.stream().map(payment -> netReceived(payment, refundedAmounts)).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal netReceived(Payment payment, Map<UUID, BigDecimal> refundedAmounts) {
		if (payment.getStatus() == PaymentStatus.REFUNDED) return BigDecimal.ZERO;
		if (payment.getStatus() != PaymentStatus.SUCCEEDED && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED)
			return BigDecimal.ZERO;
		return payment.getAmount().subtract(refundedAmounts.getOrDefault(payment.getId(), BigDecimal.ZERO)).max(BigDecimal.ZERO);
	}

	void confirmOrder(RentalOrder order, List<Payment> payments) {
		boolean initialPaid = payments.stream().anyMatch(p -> p.getPaymentStage() == PaymentStage.INITIAL && p.getStatus() == PaymentStatus.SUCCEEDED);
		if (!initialPaid) throw new ValidationException(ErrorCode.ORDER_INITIAL_PAYMENT_REQUIRED);
		order.setStatus(OrderStatus.CONFIRMED);
		allocationRepository.findAllByOrderItem_Order_Id(order.getId()).stream().filter(a -> a.getStatus() == RentalAllocationStatus.HELD).forEach(a -> {
			a.setStatus(RentalAllocationStatus.CONFIRMED);
			a.setHoldExpiresAt(null);
		});
	}

	void validateRentalAmountPaid(RentalOrder order, List<Payment> payments) {
		BigDecimal due = order.getTotalAmount().subtract(order.getDepositTotal());
		var refundedAmounts = successfulRentalRefundAmounts(order.getId());
		BigDecimal paid = payments.stream().map(payment -> payment.getRentalAmount().subtract(refundedAmounts.getOrDefault(payment.getId(), BigDecimal.ZERO)).max(BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
		if (paid.compareTo(due) < 0) throw new ValidationException(ErrorCode.ORDER_BALANCE_PAYMENT_REQUIRED);
	}

	private void validatePaymentAmount(RentalOrder order, RecordOrderPaymentCommand command, List<Payment> payments) {
		if (command.stage() == PaymentStage.INITIAL) {
			Object amount = order.getPaymentTermsSnapshot().get("initialAmount");
			if (amount == null || command.amount().compareTo(new BigDecimal(amount.toString())) != 0)
				throw new ValidationException(ErrorCode.ORDER_INITIAL_PAYMENT_AMOUNT_INVALID);
		}
		if (command.stage() == PaymentStage.DEPOSIT && command.amount().compareTo(order.getDepositTotal()) != 0)
			throw new ValidationException(ErrorCode.ORDER_DEPOSIT_PAYMENT_AMOUNT_INVALID);
		if (command.stage() == PaymentStage.LATE_FEE
				&& command.amount().compareTo(remainingLateFeeAmount(order, payments)) > 0)
			throw new ValidationException(ErrorCode.ORDER_LATE_FEE_PAYMENT_AMOUNT_INVALID);
		if (command.stage() == PaymentStage.BALANCE) {
			BigDecimal due = order.getTotalAmount().subtract(order.getDepositTotal()).subtract(order.getLateFeeTotal()).max(BigDecimal.ZERO);
			var refundedAmounts = successfulRentalRefundAmounts(order.getId());
			BigDecimal paid = payments.stream()
					.filter(p -> p.getPaymentStage() != PaymentStage.LATE_FEE)
					.map(p -> p.getRentalAmount().subtract(refundedAmounts.getOrDefault(p.getId(), BigDecimal.ZERO)).max(BigDecimal.ZERO))
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			if (command.amount().compareTo(due.subtract(paid)) > 0)
				throw new ValidationException(ErrorCode.ORDER_BALANCE_PAYMENT_AMOUNT_INVALID);
		}
	}

	private BigDecimal remainingLateFeeAmount(RentalOrder order, List<Payment> payments) {
		var refundedAmounts = successfulRefundAmounts(order.getId());
		var paid = payments.stream()
				.filter(payment -> payment.getPaymentStage() == PaymentStage.LATE_FEE)
				.filter(payment -> payment.getStatus() == PaymentStatus.SUCCEEDED || payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED)
				.map(payment -> payment.getAmount().subtract(refundedAmounts.getOrDefault(payment.getId(), BigDecimal.ZERO)).max(BigDecimal.ZERO))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return order.getLateFeeTotal().subtract(paid).max(BigDecimal.ZERO);
	}

	private Payment confirmReceipt(Payment payment, RecordOrderPaymentCommand command) {
		mapper.confirmReceipt(command, payment, OffsetDateTime.now(ZoneId.systemDefault()));
		return payment;
	}

	private String normalizeReference(String reference) {
		return reference == null || reference.isBlank() ? null : reference.trim();
	}
	private void publishActivity(RentalOrder order, String eventType, OrderStatus previousStatus, OrderStatus currentStatus, PaymentStage paymentStage, BigDecimal amount) {
		eventPublisher.publishEvent(new MerchantOrderActivityEvent(order.getId(), order.getOrderNumber(), order.getCustomerName(), eventType, previousStatus == null ? null : previousStatus.name(), currentStatus == null ? null : currentStatus.name(), paymentStage == null ? null : paymentStage.name(), amount, app.sewain.tenant.TenantContext.get()));
	}

	private record PaymentAllocation(BigDecimal rentalAmount, BigDecimal depositAmount) { }
}
