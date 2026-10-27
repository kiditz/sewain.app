package app.sewain.orders;

import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Mencatat pengembalian barang. Denda keterlambatan dihitung dari
 * {@code returnedAt} (default: sekarang) terhadap jadwal pengembalian order.
 * Mencatat ulang selama order masih ACTIVE_RENTAL akan menghitung ulang denda.
 */
record RecordOrderReturnCommand(
		OffsetDateTime returnedAt,
		boolean waiveLateFee,
		@Size(max = 200) String waiverReason
) {
}
