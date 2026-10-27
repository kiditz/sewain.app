package app.sewain.orders;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

record MerchantOrderCommand(
	@NotBlank @Size(max = 150) String customerName,
	@NotBlank @Email @Size(max = 255) String customerEmail,
	@Size(max = 30) String customerPhone,
	@NotNull FulfillmentType fulfillmentType,
	Map<String, Object> fulfillmentAddress,
	OffsetDateTime pickupAt,
	OffsetDateTime deliveryAt,
	@NotNull OffsetDateTime returnAt,
	@NotNull PaymentMethod paymentMethod,
	@NotNull InitialPaymentChoice initialPaymentChoice,
	BalanceDueAt balanceDueAt,
	@DecimalMin("0.00") BigDecimal deliveryFee,
	boolean depositWaived,
	@Size(max = 200) String depositWaiverReason,
	String notes,
	@NotEmpty List<@Valid MerchantOrderItemCommand> items
) {}

record MerchantOrderItemCommand(
	@NotNull UUID productId,
	@NotNull UUID variantId,
	@Min(1) int quantity,
	@NotNull OffsetDateTime rentalStartAt,
	@NotNull OffsetDateTime rentalEndAt
) {}
