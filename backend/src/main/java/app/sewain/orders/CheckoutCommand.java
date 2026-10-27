package app.sewain.orders;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record CheckoutCommand(
        @NotBlank @Size(max = 150) String customerName,
        @NotBlank @Email @Size(max = 255) String customerEmail,
        @Size(max = 30) String customerPhone,
        @NotNull FulfillmentType fulfillmentType,
        @Size(max = 1000) String deliveryAddress,
        @NotNull PaymentMethod paymentMethod,
        @NotNull InitialPaymentChoice initialPaymentChoice,
        BalanceDueAt balanceDueAt,
        @Size(max = 2000) String notes
) { }
