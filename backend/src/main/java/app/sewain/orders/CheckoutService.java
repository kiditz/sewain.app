package app.sewain.orders;

import java.util.UUID;

interface CheckoutService {
    CheckoutPaymentOptionsResponse paymentOptions();
    MerchantOrderResponse checkout(UUID guestToken, CheckoutCommand command);
}
