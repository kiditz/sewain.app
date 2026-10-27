package app.sewain.catalogs;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ConflictException;
import app.sewain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class InventoryCommandServiceImpl implements InventoryCommandService {

	private final InventoryUnitRepository inventoryRepository;
	private final ProductVariantRepository variantRepository;
	private final InventoryMapper inventoryMapper;

	@Override
	@Transactional
	public InventoryResponse create(InventoryCommand command) {
		var skuBarcode = command.skuBarcode().trim();

		if (inventoryRepository.existsBySkuBarcode(skuBarcode)) {
			throw new ConflictException(
					ErrorCode.INVENTORY_UNIT_SKU_ALREADY_EXISTS
			);
		}

		var unit = inventoryMapper.toEntity(command);
		unit.setSkuBarcode(skuBarcode);
		unit.setVariant(resolveVariant(command.variantId()));
		ensureSerialNumber(unit);

		unit = inventoryRepository.save(unit);
		return inventoryMapper.toResponse(unit, Set.of());
	}

	@Override
	@Transactional
	public InventoryResponse update(UUID id, InventoryCommand command) {
		var unit = getUnit(id);
		var skuBarcode = command.skuBarcode().trim();

		if (inventoryRepository.existsBySkuBarcodeAndIdNot(skuBarcode, id)) {
			throw new ConflictException(
					ErrorCode.INVENTORY_UNIT_SKU_ALREADY_EXISTS
			);
		}

		inventoryMapper.update(command, unit);
		unit.setSkuBarcode(skuBarcode);
		unit.setVariant(resolveVariant(command.variantId()));
		ensureSerialNumber(unit);

		inventoryRepository.flush();

		var allocated = inventoryRepository.hasRentalAllocation(id)
				? Set.of(id)
				: Set.<UUID>of();

		return inventoryMapper.toResponse(unit, allocated);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		var unit = getUnit(id);

		if (unit.getVariant() != null) {
			throw new ConflictException(
					ErrorCode.INVENTORY_UNIT_DELETE_HAS_VARIANT
			);
		}

		if (inventoryRepository.hasRentalAllocation(id)) {
			throw new ConflictException(
					ErrorCode.INVENTORY_UNIT_DELETE_ALLOCATED
			);
		}

		inventoryRepository.delete(unit);
	}

	private InventoryUnit getUnit(UUID id) {
		return inventoryRepository.findById(id)
				.orElseThrow(() ->
						new ResourceNotFoundException(
								ErrorCode.INVENTORY_UNIT_NOT_FOUND
						)
				);
	}

	private ProductVariant resolveVariant(UUID variantId) {
		if (variantId == null) {
			return null;
		}

		return variantRepository.findById(variantId)
				.orElseThrow(() ->
						new ResourceNotFoundException(
								ErrorCode.VARIANT_NOT_FOUND
						)
				);
	}

	private void ensureSerialNumber(InventoryUnit unit) {
		if (StringUtils.hasText(unit.getSerialNumber())) {
			unit.setSerialNumber(unit.getSerialNumber().trim());
			return;
		}

		unit.setSerialNumber(
				"SN-" + UUID.randomUUID()
						.toString()
						.replace("-", "")
						.toUpperCase()
		);
	}
}
