package app.sewain.catalogs;

import app.sewain.shared.DurationUnit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VariantRentalPlanResponse(
        UUID id,
        DurationUnit durationUnit,
        int durationValue,
        BigDecimal rentalFee,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}