package app.sewain.orders;

import app.sewain.settings.SettingFacade;
import app.sewain.settings.SettingKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
class MerchantOrderLateFeeService {
    private final SettingFacade settings;
    private final ObjectMapper objectMapper;

    LateFeeCalculator.Quote calculate(RentalOrder order, List<OrderItem> items, OffsetDateTime returnedAt) {
        return LateFeeCalculator.calculate(policy(order), dueAt(order, items), returnedAt, items);
    }

    MerchantOrderLateFeeResponse response(RentalOrder order, List<OrderItem> items) {
        var policy = policy(order);
        var dueAt = dueAt(order, items);
        if (order.getReturnedAt() != null)
            return new MerchantOrderLateFeeResponse(order.getLateFeeUnit() != null, true, dueAt, order.getReturnedAt(),
                    order.getLateMinutes(), policy.graceMinutes(), order.getLateFeeUnit(), order.getLateFeeUnitAmount(),
                    order.getLateFeeUnits(), order.getLateFeeAssessed());
        if (order.getStatus() != OrderStatus.ACTIVE_RENTAL) return null;
        var now = OffsetDateTime.now(ZoneId.systemDefault());
        var quote = LateFeeCalculator.calculate(policy, dueAt, now, items);
        return new MerchantOrderLateFeeResponse(quote.enabled(), false, dueAt, now, quote.lateMinutes(),
                quote.graceMinutes(), quote.unit().name(), quote.unitAmount(), quote.billedUnits(), quote.amount());
    }

    private LateFeeCalculator.Policy policy(RentalOrder order) {
        if (order.getPaymentTermsSnapshot().get("rental") instanceof Map<?, ?> snapshot
                && LateFeeCalculator.Policy.isConfigured(snapshot))
            return LateFeeCalculator.Policy.fromRentalConfig(snapshot);
        try {
            var current = objectMapper.convertValue(settings.getValue(SettingKey.RENTAL), new TypeReference<Map<String, Object>>() { });
            return LateFeeCalculator.Policy.fromRentalConfig(current);
        } catch (RuntimeException ex) {
            return LateFeeCalculator.Policy.disabled();
        }
    }

    private OffsetDateTime dueAt(RentalOrder order, List<OrderItem> items) {
        if (order.getReturnAt() != null) return order.getReturnAt();
        return items.stream().map(OrderItem::getRentalEndAt).max(Comparator.naturalOrder()).orElse(null);
    }
}
