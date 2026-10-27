package app.sewain.orders;

import java.time.OffsetDateTime;
import java.util.UUID;

record MerchantOrderCalendarEntry(UUID orderId, String orderNumber, String customerName, OrderStatus status,
                                  FulfillmentType fulfillmentType, OffsetDateTime fulfillmentAt,
                                  OffsetDateTime returnAt, String productName, String variantName, int quantity,
                                  UUID inventoryUnitId, OffsetDateTime allocatedStartAt, OffsetDateTime allocatedEndAt,
                                  RentalAllocationStatus allocationStatus
) {
}
