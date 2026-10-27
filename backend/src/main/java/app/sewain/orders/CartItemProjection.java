package app.sewain.orders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

interface CartItemProjection {
    UUID getId();
    UUID getProductId();
    String getProductName();
    String getProductSlug();
    List<String> getMediaList();
    UUID getVariantId();
    String getVariantName();
    int getQuantity();
    OffsetDateTime getRentalStartAt();
    OffsetDateTime getRentalEndAt();
    Object getRentalPlans();
    BigDecimal getRentalSubtotal();
    BigDecimal getDepositSubtotal();
    BigDecimal getTotalAmount();
    OffsetDateTime getCreatedAt();
    OffsetDateTime getUpdatedAt();
}