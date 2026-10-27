package app.sewain.orders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.Map;

record MerchantOrderItemResponse(UUID productId, UUID variantId, String productName, String variantName, String sku,
                                 OffsetDateTime rentalStartAt, OffsetDateTime rentalEndAt, int rentalDurationValue,
                                 RentalDurationUnit rentalDurationUnit, int quantity, BigDecimal rentalUnitPrice,
                                 BigDecimal depositUnitAmount, BigDecimal discountAmount, BigDecimal taxAmount,
                                 BigDecimal lineTotal, Map<String, Object> rentalPriceSnapshot) {
}
