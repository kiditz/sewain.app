package app.sewain.catalogs;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class InventoryQueryServiceImpl implements InventoryQueryService {

	private final InventoryUnitRepository inventoryRepository;
	private final InventoryMapper inventoryMapper;

	@Override
	public PageResponse<InventoryResponse> search(PageQuery q) {
		var query = q == null ? PageQuery.defaultQuery() : q;

		var page = inventoryRepository.findAll(
				InventorySpecifications.search(query.getSearch()),
				PageRequest.of(
						query.pageIndex(),
						query.getSize(),
						InventorySpecifications.toSort(query)
				)
		);

		var units = page.getContent();
		var allocatedUnitIds = findAllocatedUnitIds(units);

		return new PageResponse<>(
				units.stream()
						.map(unit -> inventoryMapper.toResponse(
								unit,
								allocatedUnitIds
						))
						.toList(),
				page.getTotalElements()
		);
	}

	@Override
	public InventoryResponse findById(UUID id) {
		var unit = inventoryRepository.findById(id)
				.orElseThrow(() ->
						new ResourceNotFoundException(
								ErrorCode.INVENTORY_UNIT_NOT_FOUND
						)
				);

		return inventoryMapper.toResponse(
				unit,
				findAllocatedUnitIds(List.of(unit))
		);
	}

	/**
	 * Satu query untuk seluruh unit di halaman (bukan per unit).
	 * Unit yang sudah punya varian tidak mungkin dihapus, jadi tidak perlu dicek.
	 */
	private Set<UUID> findAllocatedUnitIds(List<InventoryUnit> units) {
		var candidateIds = units.stream()
				.filter(unit -> unit.getVariant() == null)
				.map(InventoryUnit::getId)
				.toList();

		return candidateIds.isEmpty()
				? Set.of()
				: inventoryRepository.findAllocatedUnitIds(candidateIds);
	}
}
