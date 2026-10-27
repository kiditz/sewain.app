package app.sewain.reports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

record MerchantReportResponse(MerchantReportSummary summary, List<MerchantReportTrendPoint> revenueTrend,
		List<MerchantReportStatus> orderStatuses, List<MerchantReportProduct> topProducts,
		List<MerchantReportDailyPoint> dailyReports) {}

record MerchantReportSummary(long totalOrders, BigDecimal rentalRevenue, BigDecimal averageOrderValue,
		BigDecimal cancellationRate, BigDecimal ordersChangePercent, BigDecimal revenueChangePercent,
		BigDecimal averageOrderValueChangePercent, BigDecimal cancellationRateChangePoints,
		long productsRented, BigDecimal averageRentalDays, long uniqueCustomers) {}

record MerchantReportTrendPoint(LocalDate date, BigDecimal revenue, long orders) {}

record MerchantReportStatus(String status, long count) {}

record MerchantReportProduct(UUID id, String name, String category, long rentals, BigDecimal revenue,
		BigDecimal utilization) {}

record MerchantReportDailyPoint(LocalDate date, long orders, long completed, long cancelled,
		BigDecimal revenue) {}
