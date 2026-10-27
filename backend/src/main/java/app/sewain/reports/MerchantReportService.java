package app.sewain.reports;

import app.sewain.exception.ValidationException;

import app.sewain.exception.ErrorCode;

import app.sewain.settings.SettingFacade;
import app.sewain.settings.SettingKey;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class MerchantReportService {
	private final NamedParameterJdbcTemplate jdbc;
	private final SettingFacade settings;

	@Transactional(readOnly = true)
	MerchantReportResponse get(int days) {
		if (days != 7 && days != 30 && days != 90)
			throw new ValidationException(ErrorCode.REPORTS_PERIOD_INVALID);

		JsonNode regional = settings.getValue(SettingKey.REGIONAL);
		ZoneId zone = ZoneId.of(regional.path("timezone").asString("Asia/Jakarta"));
		LocalDate to = LocalDate.now(zone);
		LocalDate from = to.minusDays(days - 1L);
		LocalDate previousFrom = from.minusDays(days);
		var params = new MapSqlParameterSource()
				.addValue("previousFromDate", previousFrom)
				.addValue("fromAt", from.atStartOfDay(zone).toOffsetDateTime())
				.addValue("toAt", to.plusDays(1).atStartOfDay(zone).toOffsetDateTime())
				.addValue("previousFromAt", previousFrom.atStartOfDay(zone).toOffsetDateTime())
				.addValue("timezone", zone.getId())
				.addValue("periodDays", days);

		var rawSummary = jdbc.queryForMap(SUMMARY_SQL, params);
		long orderCount = number(rawSummary, "order_count").longValue();
		long cancelledCount = number(rawSummary, "cancelled_count").longValue();
		long previousOrderCount = number(rawSummary, "previous_order_count").longValue();
		long previousCancelledCount = number(rawSummary, "previous_cancelled_count").longValue();
		BigDecimal averageOrderValue = decimal(rawSummary, "average_order_value");
		BigDecimal previousAverageOrderValue = decimal(rawSummary, "previous_average_order_value");
		BigDecimal revenue = decimal(rawSummary, "rental_revenue");
		BigDecimal previousRevenue = decimal(rawSummary, "previous_rental_revenue");
		BigDecimal cancellationRate = rate(cancelledCount, orderCount);
		BigDecimal previousCancellationRate = rate(previousCancelledCount, previousOrderCount);

		var summary = new MerchantReportSummary(orderCount, revenue, averageOrderValue, cancellationRate,
				percentChange(orderCount, previousOrderCount), percentChange(revenue, previousRevenue),
				percentChange(averageOrderValue, previousAverageOrderValue),
				cancellationRate.subtract(previousCancellationRate).setScale(2, RoundingMode.HALF_UP),
				number(rawSummary, "products_rented").longValue(), decimal(rawSummary, "average_rental_days"),
				number(rawSummary, "unique_customers").longValue());

		var ordersByDate = new HashMap<LocalDate, DailyOrderCounts>();
		jdbc.query(DAILY_ORDERS_SQL, params, (rs, row) -> new DailyOrderCounts(
				rs.getObject("report_date", LocalDate.class), rs.getLong("order_count"),
				rs.getLong("completed"), rs.getLong("cancelled")))
				.forEach(point -> ordersByDate.put(point.date(), point));
		var revenueByDate = new HashMap<LocalDate, BigDecimal>();
		jdbc.query(DAILY_REVENUE_SQL, params, (rs, row) -> new DailyRevenuePoint(
				rs.getObject("report_date", LocalDate.class), rs.getBigDecimal("net_revenue")))
				.forEach(point -> revenueByDate.put(point.date(), point.revenue()));
		var daily = new ArrayList<MerchantReportDailyPoint>();
		for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
			var orderPoint = ordersByDate.get(date);
			daily.add(new MerchantReportDailyPoint(date, orderPoint == null ? 0 : orderPoint.orders(),
					orderPoint == null ? 0 : orderPoint.completed(), orderPoint == null ? 0 : orderPoint.cancelled(),
					revenueByDate.getOrDefault(date, BigDecimal.ZERO)));
		}
		var trend = toTrend(daily, days);
		var statuses = jdbc.query(STATUS_SQL, params, (rs, row) ->
				new MerchantReportStatus(rs.getString("status"), rs.getLong("count")));
		var products = jdbc.query(PRODUCT_SQL, params, (rs, row) -> new MerchantReportProduct(
				rs.getObject("product_id", UUID.class), rs.getString("name"), rs.getString("category"),
				rs.getLong("rentals"), rs.getBigDecimal("revenue"), rs.getBigDecimal("utilization")));
		return new MerchantReportResponse(summary, trend, statuses, products, daily);
	}

	private List<MerchantReportTrendPoint> toTrend(List<MerchantReportDailyPoint> daily, int days) {
		int bucketCount = Math.min(7, days);
		var buckets = new ArrayList<MerchantReportTrendPoint>();
		for (int index = 0; index < bucketCount; index++) {
			int start = (int) ((long) index * daily.size() / bucketCount);
			int end = (int) ((long) (index + 1) * daily.size() / bucketCount);
			var points = daily.subList(start, end);
			BigDecimal revenue = points.stream().map(MerchantReportDailyPoint::revenue)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			long orders = points.stream().mapToLong(MerchantReportDailyPoint::orders).sum();
			if (!points.isEmpty()) buckets.add(new MerchantReportTrendPoint(points.getFirst().date(), revenue, orders));
		}
		return List.copyOf(buckets);
	}

	private BigDecimal number(java.util.Map<String, Object> row, String key) {
		Object value = row.get(key);
		return value == null ? BigDecimal.ZERO : value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
	}

	private BigDecimal decimal(java.util.Map<String, Object> row, String key) {
		return number(row, key);
	}

	private BigDecimal rate(long numerator, long denominator) {
		if (denominator == 0) return BigDecimal.ZERO;
		return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
				.divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
	}

	private BigDecimal percentChange(long current, long previous) {
		return percentChange(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
	}

	private BigDecimal percentChange(BigDecimal current, BigDecimal previous) {
		if (previous.signum() == 0) return current.signum() == 0 ? BigDecimal.ZERO : null;
		return current.subtract(previous).multiply(BigDecimal.valueOf(100))
				.divide(previous.abs(), 2, RoundingMode.HALF_UP);
	}

	private static final String SUMMARY_SQL = """
			WITH order_metrics AS (
			    SELECT
			        count(*) FILTER (WHERE created_at >= :fromAt AND created_at < :toAt) AS order_count,
			        count(*) FILTER (WHERE created_at >= :fromAt AND created_at < :toAt AND status = 'CANCELLED') AS cancelled_count,
			        count(*) FILTER (WHERE created_at >= :previousFromAt AND created_at < :fromAt) AS previous_order_count,
			        count(*) FILTER (WHERE created_at >= :previousFromAt AND created_at < :fromAt AND status = 'CANCELLED') AS previous_cancelled_count,
			        coalesce(avg(total_amount) FILTER (WHERE created_at >= :fromAt AND created_at < :toAt AND status NOT IN ('CANCELLED', 'EXPIRED')), 0) AS average_order_value,
			        coalesce(avg(total_amount) FILTER (WHERE created_at >= :previousFromAt AND created_at < :fromAt AND status NOT IN ('CANCELLED', 'EXPIRED')), 0) AS previous_average_order_value,
			        count(DISTINCT lower(customer_email)) FILTER (WHERE created_at >= :fromAt AND created_at < :toAt) AS unique_customers
			    FROM orders
			), item_metrics AS (
			    SELECT count(DISTINCT i.product_id) AS products_rented,
			           coalesce(sum(extract(epoch FROM (i.rental_end_at - i.rental_start_at)) / 86400 * i.quantity)
			                / nullif(sum(i.quantity), 0), 0) AS average_rental_days
			    FROM order_items i JOIN orders o ON o.id = i.order_id
			    WHERE o.created_at >= :fromAt AND o.created_at < :toAt AND o.status NOT IN ('CANCELLED', 'EXPIRED')
			), finance_metrics AS (
			    SELECT coalesce(sum(amount) FILTER (WHERE occurred_at >= :fromAt AND occurred_at < :toAt AND category = 'RENTAL_PAYMENT' AND direction = 'INCOME'), 0)
			              - coalesce(sum(amount) FILTER (WHERE occurred_at >= :fromAt AND occurred_at < :toAt AND category = 'RENTAL_REFUND' AND direction = 'EXPENSE'), 0) AS rental_revenue,
			           coalesce(sum(amount) FILTER (WHERE occurred_at >= :previousFromAt AND occurred_at < :fromAt AND category = 'RENTAL_PAYMENT' AND direction = 'INCOME'), 0)
			              - coalesce(sum(amount) FILTER (WHERE occurred_at >= :previousFromAt AND occurred_at < :fromAt AND category = 'RENTAL_REFUND' AND direction = 'EXPENSE'), 0) AS previous_rental_revenue
			    FROM finance_transactions
			)
			SELECT * FROM order_metrics CROSS JOIN item_metrics CROSS JOIN finance_metrics
			""";

	private record DailyOrderCounts(LocalDate date, long orders, long completed, long cancelled) {}
	private record DailyRevenuePoint(LocalDate date, BigDecimal revenue) {}

	private static final String DAILY_ORDERS_SQL = """
			SELECT (created_at AT TIME ZONE :timezone)::date AS report_date,
			       count(*) AS order_count,
			       count(*) FILTER (WHERE status = 'COMPLETED') AS completed,
			       count(*) FILTER (WHERE status = 'CANCELLED') AS cancelled
			FROM orders
			WHERE created_at >= :fromAt AND created_at < :toAt
			GROUP BY 1
			ORDER BY 1
			""";

	private static final String DAILY_REVENUE_SQL = """
			SELECT (occurred_at AT TIME ZONE :timezone)::date AS report_date,
			       coalesce(sum(amount) FILTER (WHERE category = 'RENTAL_PAYMENT' AND direction = 'INCOME'), 0)
			         - coalesce(sum(amount) FILTER (WHERE category = 'RENTAL_REFUND' AND direction = 'EXPENSE'), 0) AS net_revenue
			FROM finance_transactions
			WHERE occurred_at >= :fromAt AND occurred_at < :toAt
			GROUP BY 1
			ORDER BY 1
			""";

	private static final String STATUS_SQL = """
			SELECT status, count(*) AS count FROM orders
			WHERE created_at >= :fromAt AND created_at < :toAt
			GROUP BY status ORDER BY status
			""";

	private static final String PRODUCT_SQL = """
			WITH inventory AS (
			    SELECT pv.product_id, count(iu.id) AS unit_count
			    FROM product_variants pv LEFT JOIN inventory_units iu ON iu.variant_id = pv.id
			    GROUP BY pv.product_id
			)
			SELECT i.product_id, i.product_name AS name, coalesce(pt.name, '—') AS category,
			       sum(i.quantity) AS rentals, sum(i.line_total) AS revenue,
			       CASE WHEN coalesce(max(inventory.unit_count), 0) = 0 THEN 0
			            ELSE least(100, round(100.0 * coalesce(sum(usage.unit_days), 0)
			                 / (max(inventory.unit_count) * :periodDays), 1)) END AS utilization
			FROM order_items i
			JOIN orders o ON o.id = i.order_id
			LEFT JOIN products p ON p.id = i.product_id
			LEFT JOIN product_types pt ON pt.id = p.product_type_id
			LEFT JOIN inventory ON inventory.product_id = i.product_id
			LEFT JOIN LATERAL (
			    SELECT coalesce(sum(extract(epoch FROM (least(a.rental_end_at, :toAt) - greatest(a.rental_start_at, :fromAt))) / 86400), 0) AS unit_days
			    FROM rental_allocations a
			    WHERE a.order_item_id = i.id AND a.status IN ('CONFIRMED', 'COMPLETED')
			      AND a.rental_start_at < :toAt AND a.rental_end_at > :fromAt
			) usage ON true
			WHERE o.created_at >= :fromAt AND o.created_at < :toAt AND o.status NOT IN ('CANCELLED', 'EXPIRED')
			GROUP BY i.product_id, i.product_name, pt.name
			ORDER BY revenue DESC LIMIT 5
			""";
}
