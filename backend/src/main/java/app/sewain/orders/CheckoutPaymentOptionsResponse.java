package app.sewain.orders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

record CheckoutPaymentOptionsResponse(
        boolean bankTransferEnabled,
        boolean cashAtPickupEnabled,
        boolean cashAtReturnEnabled,
        boolean downPaymentEnabled,
        String downPaymentType,
        BigDecimal downPaymentValue,
        boolean fullPaymentEnabled,
        boolean separateDeposit,
        String instructions,
        List<Map<String, Object>> bankAccounts,
        String currency
) { }
