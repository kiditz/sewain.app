package app.sewain.orders;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

record RecordPaymentRefundCommand(@DecimalMin(value = "0.01") BigDecimal amount,
		@Size(max = 1000) String reason) {}
