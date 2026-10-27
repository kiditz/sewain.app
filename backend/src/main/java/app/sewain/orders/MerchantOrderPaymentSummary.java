package app.sewain.orders;

import java.math.BigDecimal;
import java.util.List;

record MerchantOrderPaymentSummary(List<MerchantOrderPaymentResponse> payments,
		BigDecimal totalPaidAmount, BigDecimal remainingAmount,
		BigDecimal remainingRentalAmount, BigDecimal remainingDepositAmount,
		BigDecimal initialAmountDue) {}
