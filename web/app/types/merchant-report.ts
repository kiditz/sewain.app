export interface MerchantReportSummary {
  totalOrders: number;
  rentalRevenue: number;
  averageOrderValue: number;
  cancellationRate: number;
  ordersChangePercent: number;
  revenueChangePercent: number;
  averageOrderValueChangePercent: number;
  cancellationRateChangePoints: number;
  productsRented: number;
  averageRentalDays: number;
  uniqueCustomers: number;
}

export interface MerchantReportTrendPoint {
  date: string;
  revenue: number;
  orders: number;
}

export interface MerchantReportStatus {
  status: string;
  count: number;
}

export interface MerchantReportProduct {
  id: string;
  name: string;
  category: string;
  rentals: number;
  revenue: number;
  utilization: number;
}

export interface MerchantReportDailyPoint {
  date: string;
  orders: number;
  completed: number;
  cancelled: number;
  revenue: number;
}

export interface MerchantReportResponse {
  summary: MerchantReportSummary;
  revenueTrend: MerchantReportTrendPoint[];
  orderStatuses: MerchantReportStatus[];
  topProducts: MerchantReportProduct[];
  dailyReports: MerchantReportDailyPoint[];
}
