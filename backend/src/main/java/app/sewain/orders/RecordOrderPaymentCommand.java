package app.sewain.orders;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

record RecordOrderPaymentCommand(@NotNull PaymentStage stage, @NotNull @DecimalMin(value = "0.01") BigDecimal amount, @NotNull PaymentMethod method, String reference) {}
