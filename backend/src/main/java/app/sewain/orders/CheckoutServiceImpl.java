package app.sewain.orders;

import app.sewain.exception.ErrorCode;
import app.sewain.exception.ValidationException;
import app.sewain.settings.SettingFacade;
import app.sewain.settings.SettingKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class CheckoutServiceImpl implements CheckoutService {
    private final CartService cartService;
    private final MerchantOrderCommandService orderCommandService;
    private final SettingFacade settings;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public CheckoutPaymentOptionsResponse paymentOptions() {
        JsonNode payment = settings.getValue(SettingKey.PAYMENT);
        JsonNode accounts = settings.getValue(SettingKey.PAYMENT_BANK_ACCOUNTS).path("accounts");
        JsonNode regional = settings.getValue(SettingKey.REGIONAL);
        var bankAccounts = objectMapper.convertValue(accounts, new TypeReference<List<Map<String, Object>>>() { });
        return new CheckoutPaymentOptionsResponse(
                payment.path("bankTransfer").asBoolean(false) && !accounts.isEmpty(),
                payment.path("cashAtPickup").asBoolean(false),
                payment.path("cashAtReturn").asBoolean(false),
                payment.path("downPaymentEnabled").asBoolean(false),
                payment.path("downPaymentType").asString("percentage"),
                payment.path("downPaymentValue").decimalValue(),
                payment.path("fullPaymentEnabled").asBoolean(false),
                payment.path("separateDeposit").asBoolean(false),
                payment.path("instructions").asString(""),
                bankAccounts == null ? List.of() : bankAccounts,
                regional.path("currency").asString("IDR"));
    }

    @Override
    @Transactional
    public MerchantOrderResponse checkout(UUID guestToken, CheckoutCommand command) {
        if (guestToken == null) throw new ValidationException(ErrorCode.CART_OWNER_INVALID);
        var cartCommand = new CartCommand(null, guestToken);
        var cart = cartService.getCart(cartCommand);
        var cartItems = cart.items();
        if (cartItems.isEmpty()) throw new ValidationException(ErrorCode.CHECKOUT_ITEMS_INVALID);
        if (command.fulfillmentType() == FulfillmentType.DELIVERY
                && (command.deliveryAddress() == null || command.deliveryAddress().isBlank()))
            throw new ValidationException(ErrorCode.ORDER_DELIVERY_DETAILS_REQUIRED);

        OffsetDateTime fulfillmentAt = cartItems.stream().map(CartItemResponse::rentalStartAt)
                .min(Comparator.naturalOrder()).orElseThrow(() -> new ValidationException(ErrorCode.CHECKOUT_ITEMS_INVALID));
        OffsetDateTime returnAt = cartItems.stream().map(CartItemResponse::rentalEndAt)
                .max(Comparator.naturalOrder()).orElseThrow(() -> new ValidationException(ErrorCode.CHECKOUT_ITEMS_INVALID));
        var orderItems = cartItems.stream().map(item -> new MerchantOrderItemCommand(
                item.productId(), item.variantId(), item.quantity(), item.rentalStartAt(), item.rentalEndAt())).toList();
        Map<String, Object> address = command.fulfillmentType() == FulfillmentType.DELIVERY
                ? Map.of("address", command.deliveryAddress().trim()) : null;
        var orderCommand = new MerchantOrderCommand(
                command.customerName(), command.customerEmail(), command.customerPhone(), command.fulfillmentType(),
                address,
                command.fulfillmentType() == FulfillmentType.PICKUP ? fulfillmentAt : null,
                command.fulfillmentType() == FulfillmentType.DELIVERY ? fulfillmentAt : null,
                returnAt, command.paymentMethod(), command.initialPaymentChoice(), command.balanceDueAt(),
                null, false, null, command.notes(), orderItems);
        var order = orderCommandService.create(orderCommand);
        cartService.clearItems(cartCommand);
        return order;
    }
}
