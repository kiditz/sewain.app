package app.sewain.shared;

import java.util.List;
import java.util.UUID;

public record InventoryUnitStatusChangedEvent(List<UUID> inventoryUnitIds, String status, String tenantSchema) {}
