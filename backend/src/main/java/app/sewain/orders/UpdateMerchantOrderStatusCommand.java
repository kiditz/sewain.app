package app.sewain.orders;

import jakarta.validation.constraints.NotNull;

record UpdateMerchantOrderStatusCommand(@NotNull OrderStatus status) {}
