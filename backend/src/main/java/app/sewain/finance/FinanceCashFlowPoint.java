package app.sewain.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

record FinanceCashFlowPoint(LocalDate date, BigDecimal income, BigDecimal expense) {}
