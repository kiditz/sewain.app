package app.sewain.finance;

import app.sewain.shared.PageResponse;

import java.util.List;

record FinanceDashboardResponse(FinanceSummaryResponse summary, List<FinanceCashFlowPoint> cashFlow,
		PageResponse<FinanceTransactionResponse> transactions) {}
