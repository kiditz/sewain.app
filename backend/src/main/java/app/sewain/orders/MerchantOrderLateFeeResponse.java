package app.sewain.orders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Penilaian denda keterlambatan sebuah order.
 *
 * @param applied     true jika pengembalian sudah dicatat (nilai final);
 *                    false jika masih estimasi per {@code evaluatedAt}
 * @param dueAt       jadwal pengembalian
 * @param evaluatedAt waktu pengembalian aktual (applied) atau waktu estimasi
 * @param amount      denda hasil perhitungan, sebelum waiver
 */
record MerchantOrderLateFeeResponse(
		boolean enabled,
		boolean applied,
		OffsetDateTime dueAt,
		OffsetDateTime evaluatedAt,
		long lateMinutes,
		long graceMinutes,
		String unit,
		BigDecimal unitAmount,
		long billedUnits,
		BigDecimal amount
) {
}
