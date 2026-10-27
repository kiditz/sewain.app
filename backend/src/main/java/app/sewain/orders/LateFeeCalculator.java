package app.sewain.orders;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Perhitungan denda keterlambatan pengembalian.
 *
 * <p>Aturan:
 * <ul>
 *   <li>Denda dihitung dari selisih jadwal pengembalian ({@code dueAt}) dan waktu
 *   pengembalian aktual ({@code returnedAt}).</li>
 *   <li>Keterlambatan sampai batas toleransi (grace) tidak dikenai denda.</li>
	 *   <li>Jika melewati toleransi, seluruh keterlambatan ditagih (bukan hanya
	 *   kelebihannya) dan dibulatkan ke atas per hari.</li>
	 *   <li>Denda per hari mengikuti harga sewa efektif harian item pada order.</li>
 * </ul>
 */
final class LateFeeCalculator {

	private LateFeeCalculator() {
	}

	enum Unit {
		DAY(24 * 60);

		private final long minutes;

		Unit(long minutes) {
			this.minutes = minutes;
		}

		long minutes() {
			return minutes;
		}
	}

	record Policy(boolean enabled, long graceMinutes) {

		static Policy disabled() {
			return new Policy(false, 0);
		}

		/**
		 * True jika konfigurasi rental memuat pengaturan denda. Order lama yang
		 * snapshot-nya dibuat sebelum fitur ini tidak memilikinya.
		 */
		static boolean isConfigured(Map<?, ?> rentalConfig) {
			return rentalConfig != null && rentalConfig.containsKey("lateReturnEnabled");
		}

		/**
		 * Membaca pengaturan RENTAL (lateReturnEnabled, lateReturnGraceHours).
		 * Harga denda berasal dari harga sewa order.
		 */
		static Policy fromRentalConfig(Map<?, ?> rentalConfig) {
			if (rentalConfig == null || !Boolean.parseBoolean(String.valueOf(rentalConfig.get("lateReturnEnabled")))) {
				return disabled();
			}

			var graceHours = decimal(rentalConfig.get("lateReturnGraceHours"));

			var graceMinutes = graceHours.signum() <= 0
					? 0L
					: graceHours.multiply(BigDecimal.valueOf(60)).setScale(0, RoundingMode.HALF_UP).longValue();

			return new Policy(true, graceMinutes);
		}

		private static BigDecimal decimal(Object value) {
			if (value == null) {
				return BigDecimal.ZERO;
			}

			try {
				return new BigDecimal(String.valueOf(value));
			} catch (NumberFormatException ex) {
				return BigDecimal.ZERO;
			}
		}
	}

	record Quote(
			boolean enabled,
			long lateMinutes,
			long graceMinutes,
			Unit unit,
			BigDecimal unitAmount,
			long billedUnits,
			BigDecimal amount
	) {
	}

	static Quote calculate(Policy policy, OffsetDateTime dueAt, OffsetDateTime returnedAt, List<OrderItem> items) {
		var unitAmount = dailyRentalAmount(items);
		long lateMinutes = dueAt == null || returnedAt == null
				? 0
				: Math.max(0, Duration.between(dueAt, returnedAt).toMinutes());

		if (!policy.enabled() || lateMinutes <= policy.graceMinutes()) {
			return new Quote(
					policy.enabled(),
					lateMinutes,
					policy.graceMinutes(),
					Unit.DAY,
					unitAmount,
					0,
					BigDecimal.ZERO.setScale(2)
			);
		}

		// Pembulatan ke atas: 25 jam dengan satuan hari = 2 hari.
		long unitMinutes = Unit.DAY.minutes();
		long billedUnits = (lateMinutes + unitMinutes - 1) / unitMinutes;
		var amount = unitAmount
				.multiply(BigDecimal.valueOf(billedUnits))
				.setScale(2, RoundingMode.HALF_UP);

		return new Quote(
				true,
				lateMinutes,
				policy.graceMinutes(),
				Unit.DAY,
				unitAmount,
				billedUnits,
				amount
		);
	}

	private static BigDecimal dailyRentalAmount(List<OrderItem> items) {
		return items.stream().map(item -> {
			long durationDays = switch (item.getRentalDurationUnit()) {
				case HOUR -> 1;
				case DAY -> item.getRentalDurationValue();
				case WEEK -> (long) item.getRentalDurationValue() * 7;
				case MONTH -> (long) item.getRentalDurationValue() * 30;
			};
			if (durationDays <= 0) return BigDecimal.ZERO;
			return item.getRentalUnitPrice()
					.multiply(BigDecimal.valueOf(item.getQuantity()))
					.divide(BigDecimal.valueOf(durationDays), 8, RoundingMode.HALF_UP);
		}).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
	}
}
