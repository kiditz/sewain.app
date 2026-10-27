package app.sewain.catalogs;

enum RentalAllocationStatus {
	HELD,       // Ya	Menunggu pembayaran, belum kedaluwarsa
	CONFIRMED,  // Ya	Booking sudah terkonfirmasi
	RELEASED,   // No	Booking batal, gagal, atau hold dilepas
	COMPLETED   // No   untuk periode lampau Penyewaan selesai; riwayat tetap disimpan
}
