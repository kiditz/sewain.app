package app.sewain.catalogs;

import app.sewain.shared.DurationUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

record VariantRentalPlanCommand(

		@NotNull(message = "rentalPlan.durationUnit.notnull")
		DurationUnit durationUnit,

		@Positive(message = "rentalPlan.durationValue.positive")
		int durationValue,

		@NotNull(message = "rentalPlan.rentalFee.notnull")
		@DecimalMin(
				value = "0.00",
				message = "rentalPlan.rentalFee.min"
		)
		BigDecimal rentalFee,

		Boolean active

) {
}