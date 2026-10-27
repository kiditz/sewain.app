package app.sewain.orders;

import app.sewain.shared.GlobalMapperConfig;
import app.sewain.catalogs.OrderCatalogService;
import app.sewain.customers.Customer;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
interface MerchantOrderMapper {
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "orderNumber", source = "data.orderNumber")
	@Mapping(target = "customer", ignore = true)
	@Mapping(target = "cart", ignore = true)
	@Mapping(target = "status", source = "data.status")
	@Mapping(target = "customerEmail", source = "data.customerEmail")
	@Mapping(target = "balanceDueAt", source = "data.balanceDueAt")
	@Mapping(target = "paymentTermsSnapshot", source = "data.paymentTermsSnapshot")
	@Mapping(target = "subtotal", source = "data.subtotal")
	@Mapping(target = "depositTotal", source = "data.depositTotal")
	@Mapping(target = "depositWaived", source = "data.depositWaived")
	@Mapping(target = "depositWaiverReason", source = "data.depositWaiverReason")
	@Mapping(target = "discountTotal", ignore = true)
	@Mapping(target = "taxTotal", ignore = true)
	@Mapping(target = "deliveryFee", source = "data.deliveryFee")
	@Mapping(target = "totalAmount", source = "data.totalAmount")
	@Mapping(target = "currency", source = "data.currency")
	@Mapping(target = "placedAt", ignore = true)
	@Mapping(target = "cancelledAt", ignore = true)
	@Mapping(target = "completedAt", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	RentalOrder toEntity(MerchantOrderCommand command, MerchantOrderData data);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "order", source = "order")
	@Mapping(target = "productId", source = "pricing.productId")
	@Mapping(target = "variantId", source = "pricing.variantId")
	@Mapping(target = "productName", source = "pricing.productName")
	@Mapping(target = "variantName", source = "pricing.variantName")
	@Mapping(target = "sku", source = "pricing.sku")
	@Mapping(target = "rentalDurationValue", source = "durationValue")
	@Mapping(target = "rentalDurationUnit", source = "durationUnit")
	@Mapping(target = "rentalUnitPrice", source = "pricing.rentalPrice")
	@Mapping(target = "depositUnitAmount", source = "pricing.depositUnitAmount")
	@Mapping(target = "discountAmount", ignore = true)
	@Mapping(target = "taxAmount", ignore = true)
	@Mapping(target = "lineTotal", source = "lineTotal")
	@Mapping(target = "rentalPriceSnapshot", source = "pricing.rentalPriceSnapshot")
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	OrderItem toEntity(MerchantOrderItemCommand command, RentalOrder order,
			OrderCatalogService.VariantPricing pricing,
			int durationValue, RentalDurationUnit durationUnit, java.math.BigDecimal lineTotal);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "order", source = "order")
	@Mapping(target = "paymentType", source = "type")
	@Mapping(target = "paymentStage", source = "stage")
	@Mapping(target = "provider", constant = "MANUAL")
	@Mapping(target = "method", source = "method")
	@Mapping(target = "amount", source = "amount")
	@Mapping(target = "currency", source = "order.currency")
	@Mapping(target = "status", constant = "PENDING")
	@Mapping(target = "providerPaymentId", ignore = true)
	@Mapping(target = "idempotencyKey", ignore = true)
	@Mapping(target = "paymentUrl", ignore = true)
	@Mapping(target = "expiresAt", ignore = true)
	@Mapping(target = "paidAt", ignore = true)
	@Mapping(target = "failedAt", ignore = true)
	@Mapping(target = "providerResponse", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	Payment toEntity(RentalOrder order, PaymentStage stage, PaymentType type, java.math.BigDecimal amount, PaymentMethod method);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "order", source = "order")
	@Mapping(target = "paymentType", source = "type")
	@Mapping(target = "paymentStage", source = "stage")
	@Mapping(target = "provider", constant = "MANUAL")
	@Mapping(target = "method", source = "method")
	@Mapping(target = "amount", source = "amount")
	@Mapping(target = "currency", source = "order.currency")
	@Mapping(target = "status", constant = "SUCCEEDED")
	@Mapping(target = "providerPaymentId", source = "reference")
	@Mapping(target = "idempotencyKey", ignore = true)
	@Mapping(target = "paymentUrl", ignore = true)
	@Mapping(target = "expiresAt", ignore = true)
	@Mapping(target = "paidAt", source = "paidAt")
	@Mapping(target = "failedAt", ignore = true)
	@Mapping(target = "providerResponse", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	Payment toReceipt(RentalOrder order, PaymentStage stage, PaymentType type,
			java.math.BigDecimal amount, PaymentMethod method,
			java.time.OffsetDateTime paidAt, String reference);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "payment", source = "payment")
	@Mapping(target = "providerRefundId", ignore = true)
	@Mapping(target = "amount", source = "amount")
	@Mapping(target = "rentalAmount", source = "rentalAmount")
	@Mapping(target = "depositAmount", source = "depositAmount")
	@Mapping(target = "status", constant = "SUCCEEDED")
	@Mapping(target = "reason", source = "reason")
	@Mapping(target = "processedAt", source = "processedAt")
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	PaymentRefund toSuccessfulRefund(Payment payment, java.math.BigDecimal amount,
			java.math.BigDecimal rentalAmount, java.math.BigDecimal depositAmount,
			String reason, java.time.OffsetDateTime processedAt);

	@Mapping(target = "status", constant = "SUCCEEDED")
	@Mapping(target = "paidAt", source = "paidAt")
	@Mapping(target = "failedAt", ignore = true)
	@Mapping(target = "method", source = "command.method")
	@Mapping(target = "amount", source = "command.amount")
	@Mapping(target = "providerPaymentId", source = "command.reference")
	void confirmReceipt(RecordOrderPaymentCommand command, @MappingTarget Payment payment,
			java.time.OffsetDateTime paidAt);

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "status", source = "status")
	void updatePaymentStatus(PaymentStatus status, @MappingTarget Payment payment);

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "customer", source = "customer")
	void linkCustomer(Customer customer, @MappingTarget RentalOrder order);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "inventoryUnitId", source = "unitId")
	@Mapping(target = "orderItem", source = "item")
	@Mapping(target = "rentalStartAt", source = "start")
	@Mapping(target = "rentalEndAt", source = "end")
	@Mapping(target = "status", constant = "HELD")
	@Mapping(target = "holdExpiresAt", source = "expiresAt")
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	RentalAllocation toAllocation(java.util.UUID unitId, OrderItem item,
			java.time.OffsetDateTime start, java.time.OffsetDateTime end,
			java.time.OffsetDateTime expiresAt);

//	@Mapping(target = "items", ignore = true)
	@Mapping(target = "payments", ignore = true)
	@Mapping(target = "totalPaidAmount", ignore = true)
	@Mapping(target = "remainingAmount", ignore = true)
	@Mapping(target = "remainingRentalAmount", ignore = true)
	@Mapping(target = "remainingDepositAmount", ignore = true)
	@Mapping(target = "initialAmountDue", ignore = true)
	@Mapping(target = "lateFee", ignore = true)
	MerchantOrderResponse toResponse(RentalOrder entity);

	@Mapping(target = "items", source = "items")
	@Mapping(target = "payments", source = "summary.payments")
	@Mapping(target = "totalPaidAmount", source = "summary.totalPaidAmount")
	@Mapping(target = "remainingAmount", source = "summary.remainingAmount")
	@Mapping(target = "remainingRentalAmount", source = "summary.remainingRentalAmount")
	@Mapping(target = "remainingDepositAmount", source = "summary.remainingDepositAmount")
	@Mapping(target = "initialAmountDue", source = "summary.initialAmountDue")
	@Mapping(target = "lateFee", source = "lateFee")
	MerchantOrderResponse toDetailResponse(RentalOrder entity, List<OrderItem> items,
			MerchantOrderPaymentSummary summary, MerchantOrderLateFeeResponse lateFee);

	MerchantOrderItemResponse toItemResponse(OrderItem entity);
	List<MerchantOrderItemResponse> toItemResponses(List<OrderItem> entities);

	@Mapping(target = "stage", source = "payment.paymentStage")
	@Mapping(target = "type", source = "payment.paymentType")
	@Mapping(target = "reference", source = "payment.providerPaymentId")
	@Mapping(target = "refundedAmount", source = "refundedAmount")
	@Mapping(target = "receivedAmount", expression = "java(payment.getStatus() == PaymentStatus.REFUNDED ? java.math.BigDecimal.ZERO : payment.getAmount().subtract(refundedAmount).max(java.math.BigDecimal.ZERO))")
	@Mapping(target = "rentalRefundedAmount", source = "rentalRefundedAmount")
	@Mapping(target = "depositRefundedAmount", source = "depositRefundedAmount")
	@Mapping(target = "rentalReceivedAmount", expression = "java(payment.getStatus() == PaymentStatus.REFUNDED ? java.math.BigDecimal.ZERO : payment.getRentalAmount().subtract(rentalRefundedAmount).max(java.math.BigDecimal.ZERO))")
	@Mapping(target = "depositReceivedAmount", expression = "java(payment.getStatus() == PaymentStatus.REFUNDED ? java.math.BigDecimal.ZERO : payment.getDepositAmount().subtract(depositRefundedAmount).max(java.math.BigDecimal.ZERO))")
	MerchantOrderPaymentResponse toPaymentResponse(Payment payment, java.math.BigDecimal refundedAmount,
			java.math.BigDecimal rentalRefundedAmount, java.math.BigDecimal depositRefundedAmount);

	List<MerchantOrderResponse> toResponses(List<RentalOrder> entities);
}
