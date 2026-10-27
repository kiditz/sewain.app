package app.sewain.orders;

import java.math.BigDecimal;
import java.util.Map;

record MerchantOrderData(String orderNumber, OrderStatus status, String customerEmail,
		BalanceDueAt balanceDueAt, Map<String, Object> paymentTermsSnapshot,
		BigDecimal subtotal, BigDecimal depositTotal, boolean depositWaived, String depositWaiverReason, BigDecimal deliveryFee,
		BigDecimal totalAmount, String currency) {}
