package app.sewain.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

interface FinanceCashFlowProjection {
	LocalDate getDate();
	BigDecimal getIncome();
	BigDecimal getExpense();
}
