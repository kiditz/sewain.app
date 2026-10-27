package app.sewain.orders;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchant/orders")
@RequiredArgsConstructor
class MerchantOrderCommandController {
    private final MerchantOrderCommandService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MerchantOrderResponse create(@Valid @RequestBody MerchantOrderCommand command) { return service.create(command); }

    @PostMapping("/{id}/payments")
    MerchantOrderResponse recordPayment(@PathVariable UUID id, @Valid @RequestBody RecordOrderPaymentCommand command) { return service.recordPayment(id, command); }

    @PostMapping("/{id}/payments/{paymentId}/refunds")
    MerchantOrderResponse recordRefund(@PathVariable UUID id, @PathVariable UUID paymentId, @Valid @RequestBody RecordPaymentRefundCommand command) { return service.recordRefund(id, paymentId, command); }

    @PostMapping("/{id}/deposit-refunds")
    MerchantOrderResponse recordDepositRefund(@PathVariable UUID id, @Valid @RequestBody RecordDepositRefundCommand command) { return service.recordDepositRefund(id, command); }

    @PostMapping("/{id}/return")
    MerchantOrderResponse recordReturn(@PathVariable UUID id, @Valid @RequestBody RecordOrderReturnCommand command) { return service.recordReturn(id, command); }

    @PatchMapping("/{id}/status")
    MerchantOrderResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateMerchantOrderStatusCommand command) { return service.updateStatus(id, command.status()); }
}
