package app.sewain.customers;

record CustomerStatisticsResponse(
		long total,
		long active,
		long newThisMonth,
		long repeatCustomers
) {}
