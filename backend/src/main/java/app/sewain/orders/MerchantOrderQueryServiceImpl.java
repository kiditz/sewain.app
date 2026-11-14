package app.sewain.orders;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import app.sewain.exception.ValidationException;
import app.sewain.shared.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
class MerchantOrderQueryServiceImpl implements MerchantOrderQueryService {
    private final RentalOrderRepository orderRepository;
    private final MerchantOrderMapper mapper;
    private final OrderItemRepository itemRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository refundRepository;
    private final MerchantOrderLateFeeService lateFeeService;
    private final MerchantOrderActivityRepository activityRepository;

	@Transactional(readOnly = true)
	@Override
	public PageResponse<MerchantOrderResponse> search(PageQuery q) {
		var query = q == null ? PageQuery.defaultQuery() : q;
		var page = orderRepository.findAll(MerchantOrderSpecifications.search(query.getSearch()), PageRequest.of(query.pageIndex(), query.getSize(), MerchantOrderSpecifications.toSort(query)));
		return new PageResponse<>(mapper.toResponses(page.getContent()), page.getTotalElements());
	}

	@Transactional(readOnly = true)
	@Override
	public List<MerchantOrderActivityResponse> recentActivities() {
		return activityRepository.findTop20ByOrderByCreatedAtDesc().stream().map(activity -> new MerchantOrderActivityResponse(activity.getId(), activity.getOrderId(), activity.getOrderNumber(), activity.getCustomerName(), activity.getEventType(), activity.getPreviousStatus(), activity.getCurrentStatus(), activity.getPaymentStage(), activity.getAmount(), activity.getCreatedAt())).toList();
	}

	@Transactional(readOnly = true)
	@Override
	public List<MerchantOrderCalendarEntry> calendar(OffsetDateTime fromAt, OffsetDateTime toAt) {
		if (fromAt == null || toAt == null || !toAt.isAfter(fromAt))
			throw new ValidationException(ErrorCode.ORDER_CALENDAR_RANGE_INVALID);
		if (Duration.between(fromAt, toAt).toDays() > 93)
			throw new ValidationException(ErrorCode.ORDER_CALENDAR_RANGE_TOO_LARGE);
		return orderRepository.findCalendarEntries(fromAt, toAt, FulfillmentType.PICKUP, OrderStatus.CANCELLED, OrderStatus.EXPIRED, RentalAllocationStatus.RELEASED);
	}

	@Transactional(readOnly = true)
	@Override
	public MerchantOrderResponse get(UUID id) {
		var order = orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND));
		return detailResponse(order);
	}

	private MerchantOrderResponse detailResponse(RentalOrder order) {
		var payments = paymentRepository.findAllByOrder_Id(order.getId());
		var refundedAmounts = successfulRefundAmounts(order.getId());
		var refundedRentalAmounts = successfulRentalRefundAmounts(order.getId());
		var refundedDepositAmounts = successfulDepositRefundAmounts(order.getId());
		var paymentResponses = payments.stream().map(payment -> mapper.toPaymentResponse(payment, refundedAmounts.getOrDefault(payment.getId(), BigDecimal.ZERO), refundedRentalAmounts.getOrDefault(payment.getId(), BigDecimal.ZERO), refundedDepositAmounts.getOrDefault(payment.getId(), BigDecimal.ZERO))).sorted(Comparator.comparing(MerchantOrderPaymentResponse::createdAt)).toList();
		var paidTotals = paidTotals(payments, refundedAmounts, refundedRentalAmounts, refundedDepositAmounts);
		BigDecimal rentalDue = order.getTotalAmount().subtract(order.getDepositTotal());
		Object initialAmount = order.getPaymentTermsSnapshot().get("initialAmount");
		var summary = new MerchantOrderPaymentSummary(paymentResponses, paidTotals.total(), order.getTotalAmount().subtract(paidTotals.total()).max(BigDecimal.ZERO), rentalDue.subtract(paidTotals.rental()).max(BigDecimal.ZERO), order.getDepositTotal().subtract(paidTotals.deposit()).max(BigDecimal.ZERO), initialAmount == null ? BigDecimal.ZERO : new BigDecimal(initialAmount.toString()));
		var items = itemRepository.findAllByOrder_IdOrderByCreatedAtAsc(order.getId());
		return mapper.toDetailResponse(order, items, summary, lateFeeService.response(order, items));
	}

	private PaymentTotals paidTotals(List<Payment> payments, Map<UUID, BigDecimal> refunds,
			Map<UUID, BigDecimal> rentalRefunds, Map<UUID, BigDecimal> depositRefunds) {
		return payments.stream()
				.filter(this::isPaid)
				.map(payment -> payment.getStatus() == PaymentStatus.REFUNDED
						? new PaymentTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
						: new PaymentTotals(
								payment.getAmount().subtract(refunds.getOrDefault(payment.getId(), BigDecimal.ZERO)).max(BigDecimal.ZERO),
								payment.getRentalAmount().subtract(rentalRefunds.getOrDefault(payment.getId(), BigDecimal.ZERO)).max(BigDecimal.ZERO),
								payment.getDepositAmount().subtract(depositRefunds.getOrDefault(payment.getId(), BigDecimal.ZERO)).max(BigDecimal.ZERO)))
				.reduce(new PaymentTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), PaymentTotals::add);
	}

	private boolean isPaid(Payment payment) {
		return payment.getStatus() == PaymentStatus.SUCCEEDED
				|| payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED
				|| payment.getStatus() == PaymentStatus.REFUNDED;
	}

	private record PaymentTotals(BigDecimal total, BigDecimal rental, BigDecimal deposit) {
		private PaymentTotals add(PaymentTotals other) {
			return new PaymentTotals(total.add(other.total), rental.add(other.rental), deposit.add(other.deposit));
		}
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

	private Map<UUID, BigDecimal> successfulDepositRefundAmounts(UUID orderId) {
		var refundedAmounts = new HashMap<UUID, BigDecimal>();
		refundRepository.findAllByPayment_Order_Id(orderId).stream().filter(refund -> refund.getStatus() == PaymentRefundStatus.SUCCEEDED).forEach(refund -> refundedAmounts.merge(refund.getPayment().getId(), refund.getDepositAmount(), BigDecimal::add));
		return refundedAmounts;
	}
}
