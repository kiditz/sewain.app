package app.sewain.orders;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

record RecordDepositRefundCommand(@NotNull @DecimalMin(value = "0.01") BigDecimal amount,
		@Size(max = 1000) String reason) {}
