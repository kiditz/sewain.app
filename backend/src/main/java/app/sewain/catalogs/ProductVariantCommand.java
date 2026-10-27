package app.sewain.catalogs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

record ProductVariantCommand(
        UUID id,
        @NotBlank(message = "product.variant.name.notblank")
        @Size(max = 255, message = "product.variant.name.size")
        String name,

        @NotBlank(message = "product.variant.sku.notblank")
        @Size(max = 100, message = "product.variant.sku.size")
        String sku,

        List<@NotBlank String> mediaList,

        @DecimalMin(value = "0.00", message = "product.variant.buyPrice.min")
        BigDecimal buyPrice,

        @NotNull(message = "product.variant.depositAmount.notnull")
        @DecimalMin(value = "0.00", message = "product.variant.depositAmount.min")
        BigDecimal depositAmount,

        Boolean active,

        List<@NotNull UUID> attributeValueIds,

        List<@Valid VariantRentalPlanCommand> rentalPlans,
        List<@Valid InventoryUnitCommand> inventoryUnits


) {
}