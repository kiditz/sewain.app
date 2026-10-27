package app.sewain.orders;

import app.sewain.catalogs.OrderCatalogService;
import app.sewain.exception.ErrorCode;
import app.sewain.exception.ValidationException;
import app.sewain.settings.SettingFacade;
import app.sewain.settings.SettingKey;
import app.sewain.shared.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
class MerchantOrderCreationService {
    private static final String TIMEZONE_SETTING_KEY = "timezone";
    private static final String DEFAULT_TIMEZONE = "Asia/Jakarta";

    private final RentalOrderRepository orderRepository;
    private final MerchantOrderMapper mapper;
    private final OrderItemRepository itemRepository;
    private final OrderCatalogService catalogService;
    private final SettingFacade settings;
    private final ObjectMapper objectMapper;
    private final RentalAllocationRepository allocationRepository;
    private final ApplicationEventPublisher eventPublisher;
	private final PaymentRepository paymentRepository;

	@Transactional
	public MerchantOrderResponse create(MerchantOrderCommand command) {
		JsonNode payment = settings.getValue(SettingKey.PAYMENT);
		JsonNode accounts = settings.getValue(SettingKey.PAYMENT_BANK_ACCOUNTS);
		JsonNode rental = settings.getValue(SettingKey.RENTAL);
		JsonNode regional = settings.getValue(SettingKey.REGIONAL);
		JsonNode rentalPlanSetting = settings.getValue(SettingKey.RENTAL_PLAN);
		int bufferHours = rental.path("bufferHours").asInt(0);
		validateCommand(command, payment, accounts, rental, regional);
		var zoneId = ZoneId.of(regional.path(TIMEZONE_SETTING_KEY).asString(DEFAULT_TIMEZONE));
		var pricing = priceItems(command, zoneId);
		var totals = calculateOrderTotals(command, payment, pricing);
		var orderData = createOrderData(command, payment, accounts, rental, rentalPlanSetting, regional, totals);
		var order = mapper.toEntity(command, orderData);
		order = orderRepository.saveAndFlush(order);
		eventPublisher.publishEvent(new CustomerOrderCreatedEvent(order.getId(), command.customerName().trim(), command.customerEmail().trim().toLowerCase(Locale.ROOT), command.customerPhone()));
		createItems(command, order, pricing.itemPrices(), bufferHours);
		createPayment(order, PaymentStage.INITIAL, PaymentType.ORDER, totals.initialAmount(), command.paymentMethod());
		if (totals.separateDeposit() && totals.chargedDepositTotal().signum() > 0)
			createPayment(order, PaymentStage.DEPOSIT, PaymentType.DEPOSIT, totals.chargedDepositTotal(), null);
		publishActivity(order, order.getStatus(), null, order.getTotalAmount());
		return mapper.toResponse(order);
	}

	private PricingSummary priceItems(MerchantOrderCommand command, ZoneId zoneId) {
		var itemPrices = new ArrayList<OrderCatalogService.VariantPricing>();
		BigDecimal rentalTotal = BigDecimal.ZERO;
		BigDecimal depositTotal = BigDecimal.ZERO;
		for (var item : command.items()) {
			var itemPricing = catalogService.getPricing(item.variantId(), item.rentalStartAt(), item.rentalEndAt(), zoneId);
			if (!itemPricing.productId().equals(item.productId()))
				throw new ValidationException(ErrorCode.ORDER_PRODUCT_VARIANT_MISMATCH);
			itemPrices.add(itemPricing);
			var quantity = BigDecimal.valueOf(item.quantity());
			rentalTotal = rentalTotal.add(itemPricing.rentalPrice().multiply(quantity));
			depositTotal = depositTotal.add(itemPricing.depositUnitAmount().multiply(quantity));
		}
		return new PricingSummary(itemPrices, rentalTotal, depositTotal);
	}

	private OrderTotals calculateOrderTotals(MerchantOrderCommand command, JsonNode payment, PricingSummary pricing) {
		String waiverReason = validateDepositWaiver(command);
		BigDecimal deliveryFee = command.deliveryFee() == null ? BigDecimal.ZERO : command.deliveryFee();
		boolean separateDeposit = payment.path("separateDeposit").asBoolean(false);
		BigDecimal chargedDepositTotal = command.depositWaived() ? BigDecimal.ZERO : pricing.depositTotal();
		BigDecimal total = pricing.rentalTotal().add(chargedDepositTotal).add(deliveryFee);
		BigDecimal initialBase = pricing.rentalTotal().add(deliveryFee)
				.add(separateDeposit ? BigDecimal.ZERO : chargedDepositTotal);
		BigDecimal initialAmount = command.initialPaymentChoice() == InitialPaymentChoice.FULL_PAYMENT
				? initialBase : calculateDownPayment(initialBase, payment);
		return new OrderTotals(pricing.rentalTotal(), chargedDepositTotal, deliveryFee, total, initialAmount,
				separateDeposit, command.depositWaived(), waiverReason);
	}

	private String validateDepositWaiver(MerchantOrderCommand command) {
		if (command.depositWaived() && (command.depositWaiverReason() == null || command.depositWaiverReason().isBlank()))
			throw new ValidationException(ErrorCode.ORDER_DEPOSIT_WAIVER_REASON_REQUIRED);
		if (!command.depositWaived() && command.depositWaiverReason() != null && !command.depositWaiverReason().isBlank())
			throw new ValidationException(ErrorCode.ORDER_DEPOSIT_WAIVER_INVALID);
		return command.depositWaived() ? command.depositWaiverReason().trim() : null;
	}

	private MerchantOrderData createOrderData(MerchantOrderCommand command, JsonNode payment, JsonNode accounts,
			JsonNode rental, JsonNode rentalPlanSetting, JsonNode regional, OrderTotals totals) {
		var balanceDueAt = command.initialPaymentChoice() == InitialPaymentChoice.DOWN_PAYMENT
				? command.balanceDueAt() : null;
		var paymentSnapshot = snapshot(payment, accounts, rental, rentalPlanSetting, regional,
				command.initialPaymentChoice(), totals.initialAmount());
		return new MerchantOrderData("SW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT),
				OrderStatus.PENDING_PAYMENT, command.customerEmail().trim().toLowerCase(Locale.ROOT), balanceDueAt,
				paymentSnapshot, totals.rentalTotal(), totals.chargedDepositTotal(), totals.depositWaived(),
				totals.depositWaiverReason(), totals.deliveryFee(), totals.total(),
				regional.path("currency").asString("IDR"));
	}

	private void createItems(MerchantOrderCommand command, RentalOrder order,
			List<OrderCatalogService.VariantPricing> itemPrices, int bufferHours) {
		for (int i = 0; i < command.items().size(); i++) {
			var input = command.items().get(i);
			var pricing = itemPrices.get(i);
			var lineTotal = pricing.rentalPrice().multiply(BigDecimal.valueOf(input.quantity()));
			var item = mapper.toEntity(input, order, pricing, Math.toIntExact(pricing.durationDays()), RentalDurationUnit.DAY, lineTotal);
			item = itemRepository.saveAndFlush(item);
			var unitIds = catalogService.reserveUnits(input.variantId(), input.rentalStartAt(), input.rentalEndAt(), input.quantity(), bufferHours);
			for (UUID unitId : unitIds) {
				var expiresAt = OffsetDateTime.now(ZoneId.systemDefault()).plusHours(2);
				allocationRepository.save(mapper.toAllocation(unitId, item, input.rentalStartAt(),
						input.rentalEndAt().plusHours(bufferHours), expiresAt));
			}
		}
	}

	private record PricingSummary(List<OrderCatalogService.VariantPricing> itemPrices, BigDecimal rentalTotal,
			BigDecimal depositTotal) { }

	private record OrderTotals(BigDecimal rentalTotal, BigDecimal chargedDepositTotal, BigDecimal deliveryFee,
			BigDecimal total, BigDecimal initialAmount, boolean separateDeposit, boolean depositWaived,
			String depositWaiverReason) { }

	private void validateCommand(MerchantOrderCommand command, JsonNode payment, JsonNode accounts, JsonNode rental, JsonNode regional) {
		validateFulfillmentDetails(command);
		validatePaymentSettings(command, payment, accounts);
		validateRentalWindow(command, rental, regional);
		validateOperatingHours(command, rental, regional);
	}

	private void validateFulfillmentDetails(MerchantOrderCommand command) {
		if (command.fulfillmentType() == FulfillmentType.PICKUP && command.pickupAt() == null)
			throw new ValidationException(ErrorCode.ORDER_PICKUP_TIME_REQUIRED);
		if (command.fulfillmentType() == FulfillmentType.DELIVERY
				&& (command.deliveryAt() == null || command.fulfillmentAddress() == null || command.fulfillmentAddress().isEmpty()))
			throw new ValidationException(ErrorCode.ORDER_DELIVERY_DETAILS_REQUIRED);
	}

	private void validatePaymentSettings(MerchantOrderCommand command, JsonNode payment, JsonNode accounts) {
		if (command.initialPaymentChoice() == InitialPaymentChoice.DOWN_PAYMENT
				&& (!payment.path("downPaymentEnabled").asBoolean(false) || command.balanceDueAt() == null))
			throw new ValidationException(ErrorCode.ORDER_DOWN_PAYMENT_INVALID);
		if (command.initialPaymentChoice() == InitialPaymentChoice.FULL_PAYMENT && !payment.path("fullPaymentEnabled").asBoolean(false))
			throw new ValidationException(ErrorCode.ORDER_FULL_PAYMENT_DISABLED);
		if (command.paymentMethod() == PaymentMethod.BANK_TRANSFER
				&& (!payment.path("bankTransfer").asBoolean(false) || accounts.path("accounts").isEmpty()))
			throw new ValidationException(ErrorCode.ORDER_BANK_TRANSFER_UNAVAILABLE);
		var cashDueAt = command.balanceDueAt();
		if (cashDueAt == null) {
			cashDueAt = command.fulfillmentType() == FulfillmentType.PICKUP ? BalanceDueAt.PICKUP : BalanceDueAt.RETURN;
		}
		if (command.paymentMethod() == PaymentMethod.CASH && cashDueAt == BalanceDueAt.PICKUP
				&& !payment.path("cashAtPickup").asBoolean(false))
			throw new ValidationException(ErrorCode.ORDER_CASH_AT_PICKUP_DISABLED);
		if (command.paymentMethod() == PaymentMethod.CASH && cashDueAt == BalanceDueAt.RETURN
				&& !payment.path("cashAtReturn").asBoolean(false))
			throw new ValidationException(ErrorCode.ORDER_CASH_AT_RETURN_DISABLED);
	}

	private void validateRentalWindow(MerchantOrderCommand command, JsonNode rental, JsonNode regional) {
		var zoneId = ZoneId.of(regional.path(TIMEZONE_SETTING_KEY).asString(DEFAULT_TIMEZONE));
		var latestRentalDate = OffsetDateTime.now(zoneId).toLocalDate().plusDays(rental.path("maximumAdvanceDays").asInt(90));
		if (command.items().stream().anyMatch(item -> item.rentalStartAt().atZoneSameInstant(zoneId).toLocalDate().isAfter(latestRentalDate)))
			throw new ValidationException(ErrorCode.ORDER_RENTAL_TOO_FAR);
		var fulfillmentAt = command.fulfillmentType() == FulfillmentType.PICKUP ? command.pickupAt() : command.deliveryAt();
		if (fulfillmentAt.isBefore(OffsetDateTime.now(zoneId).plusHours(rental.path("minimumNoticeHours").asInt(0))))
			throw new ValidationException(ErrorCode.ORDER_RENTAL_NOTICE_TOO_SHORT);
		if (command.items().stream().anyMatch(item -> !item.rentalEndAt().isAfter(item.rentalStartAt()) || command.returnAt().isBefore(item.rentalEndAt())))
			throw new ValidationException(ErrorCode.ORDER_RENTAL_PERIOD_INVALID);
	}

	private void validateOperatingHours(MerchantOrderCommand command, JsonNode rental, JsonNode regional) {
		var zoneId = ZoneId.of(regional.path(TIMEZONE_SETTING_KEY).asString(DEFAULT_TIMEZONE));
		var fulfillmentAt = command.fulfillmentType() == FulfillmentType.PICKUP ? command.pickupAt() : command.deliveryAt();
		var localFulfillment = fulfillmentAt.atZoneSameInstant(zoneId);
		var day = localFulfillment.getDayOfWeek().name();
		boolean operating = false;
		for (var configuredDay : rental.path("operatingDays")) {
			if (configuredDay.asString().equalsIgnoreCase(day)) operating = true;
		}
		if (!operating) throw new ValidationException(ErrorCode.ORDER_RENTAL_DAY_CLOSED);
		var time = localFulfillment.toLocalTime();
		if (time.isBefore(java.time.LocalTime.parse(rental.path("openingTime").asString("00:00")))
				|| time.isAfter(java.time.LocalTime.parse(rental.path("closingTime").asString("23:59"))))
			throw new ValidationException(ErrorCode.ORDER_RENTAL_OUTSIDE_OPERATING_HOURS);
	}

	private BigDecimal calculateDownPayment(BigDecimal base, JsonNode settings) {
		BigDecimal value = new BigDecimal(settings.path("downPaymentValue").asString("0"));
		if ("percentage".equalsIgnoreCase(settings.path("downPaymentType").asString()))
			return base.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		return value.min(base);
	}

	private Map<String, Object> snapshot(JsonNode payment, JsonNode accounts, JsonNode rental, JsonNode rentalPlan, JsonNode regional, InitialPaymentChoice choice, BigDecimal initial) {
		var result = new LinkedHashMap<String, Object>();
		result.put("payment", objectMapper.convertValue(payment, new TypeReference<Map<String, Object>>() {
		}));
		result.put("bankAccounts", objectMapper.convertValue(accounts.path("accounts"), new TypeReference<List<Object>>() {
		}));
		result.put("rental", objectMapper.convertValue(rental, new TypeReference<Map<String, Object>>() {
		}));
		result.put("rentalPlan", objectMapper.convertValue(rentalPlan, new TypeReference<List<Object>>() {
		}));
		result.put("regional", objectMapper.convertValue(regional, new TypeReference<Map<String, Object>>() {
		}));
		result.put("initialPaymentChoice", choice.name());
		result.put("initialAmount", initial);
		return result;
	}

	private void createPayment(RentalOrder order, PaymentStage stage, PaymentType type, BigDecimal amount, PaymentMethod method) {
		if (amount.signum() <= 0) return;
		paymentRepository.save(mapper.toEntity(order, stage, type, amount, method));
	}
	private void publishActivity(RentalOrder order, OrderStatus currentStatus, PaymentStage paymentStage, BigDecimal amount) {
		eventPublisher.publishEvent(new MerchantOrderActivityEvent(order.getId(), order.getOrderNumber(), order.getCustomerName(), "ORDER_CREATED", null, currentStatus == null ? null : currentStatus.name(), paymentStage == null ? null : paymentStage.name(), amount, app.sewain.tenant.TenantContext.get()));
	}

}
