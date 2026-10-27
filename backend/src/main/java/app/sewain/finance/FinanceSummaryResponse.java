package app.sewain.finance;

import java.math.BigDecimal;

record FinanceSummaryResponse(BigDecimal cashIn, BigDecimal cashOut, BigDecimal rentalIncome,
		BigDecimal depositHeld) {}
