package app.sewain.orders;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class MerchantOrderCommandServiceImpl implements MerchantOrderCommandService {
    private final MerchantOrderCreationService creationService;
    private final MerchantOrderPaymentService paymentService;
    private final MerchantOrderLifecycleService lifecycleService;

    @Override
    public MerchantOrderResponse create(MerchantOrderCommand command) {
        return creationService.create(command);
    }

    @Override
    public MerchantOrderResponse recordPayment(UUID orderId, RecordOrderPaymentCommand command) {
        return paymentService.recordPayment(orderId, command);
    }

    @Override
    public MerchantOrderResponse recordRefund(UUID orderId, UUID paymentId, RecordPaymentRefundCommand command) {
        return paymentService.recordRefund(orderId, paymentId, command);
    }

    @Override
    public MerchantOrderResponse recordDepositRefund(UUID orderId, RecordDepositRefundCommand command) {
        return paymentService.recordDepositRefund(orderId, command);
    }

    @Override
    public MerchantOrderResponse recordReturn(UUID orderId, RecordOrderReturnCommand command) {
        return lifecycleService.recordReturn(orderId, command);
    }

    @Override
    public MerchantOrderResponse updateStatus(UUID orderId, OrderStatus next) {
        return lifecycleService.updateStatus(orderId, next);
    }
}
