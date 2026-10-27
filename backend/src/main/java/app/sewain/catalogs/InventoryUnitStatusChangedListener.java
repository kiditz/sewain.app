package app.sewain.catalogs;

import app.sewain.exception.ErrorCode;

import app.sewain.shared.InventoryUnitStatusChangedEvent;
import app.sewain.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class InventoryUnitStatusChangedListener {
	private final InventoryUnitRepository inventoryUnitRepository;

	@ApplicationModuleListener
	void onStatusChanged(InventoryUnitStatusChangedEvent event) {
		if (event.tenantSchema() == null || event.tenantSchema().isBlank())
			throw new IllegalStateException(ErrorCode.INVENTORY_TENANT_SCHEMA_REQUIRED);

		TenantContext.set(event.tenantSchema());
		try {
			var nextStatus = InventoryUnitStatus.valueOf(event.status());
			var units = inventoryUnitRepository.findAllById(event.inventoryUnitIds());
			for (var unit : units) {
				if (nextStatus == InventoryUnitStatus.RENTED && unit.getStatus() == InventoryUnitStatus.AVAILABLE)
					unit.setStatus(InventoryUnitStatus.RENTED);
				else if (nextStatus == InventoryUnitStatus.AVAILABLE && unit.getStatus() == InventoryUnitStatus.RENTED)
					unit.setStatus(InventoryUnitStatus.AVAILABLE);
			}
			inventoryUnitRepository.saveAll(units);
		} finally {
			TenantContext.clear();
		}
	}
}
