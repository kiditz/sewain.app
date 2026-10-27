package app.sewain.orders;

enum PaymentStatus {
	PENDING,
	SUCCEEDED,
	FAILED,
	CANCELLED,
	EXPIRED,
	PARTIALLY_REFUNDED,
	REFUNDED
}