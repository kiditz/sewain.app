package app.sewain.orders;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
class CheckoutController {
    private final CheckoutService service;

    @GetMapping("/payment-options")
    CheckoutPaymentOptionsResponse paymentOptions() {
        return service.paymentOptions();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MerchantOrderResponse checkout(
            @RequestHeader(value = "X-Guest-Token", required = false) UUID guestToken,
            @Valid @RequestBody CheckoutCommand command) {
        return service.checkout(guestToken, command);
    }
}
