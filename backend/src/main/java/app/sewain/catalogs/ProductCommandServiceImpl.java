
package app.sewain.catalogs;

import app.sewain.exception.ConflictException;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import app.sewain.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class ProductCommandServiceImpl implements ProductCommandService {

	private final ProductRepository productRepository;
	private final AttributeValueRepository attributeValueRepository;
	private final InventoryUnitRepository inventoryUnitRepository;
	private final ProductMapper productMapper;
	private final ProductVariantMapper variantMapper;
	private final VariantRentalPlanMapper variantRentalPlanMapper;
	private final ProductTypeRepository productTypeRepository;
    private final TagRepository tagRepository;
	private final InventoryUnitMapper inventoryUnitMapper;

	@Override
	@Transactional
	public ProductResponse create(ProductCommand command) {
		validateSlug(command.slug());

		var product = productMapper.toEntity(command);
		product.setType(
				resolveProductType(command.productTypeId())
		);

        syncTags(product, command.tagIds());

		// Persist the parent before creating variants.
		product = productRepository.saveAndFlush(product);

		syncVariants(product, false, command.variants());

		productRepository.flush();

		return productMapper.toResponse(product);
	}

	@Override
	@Transactional
	public ProductResponse update(UUID id, ProductCommand command) {
		var product = productRepository.findById(id)
				.orElseThrow(() ->
						new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND)
				);

		validateSlug(id, command.slug());

		// ProductMapper must ignore variants and productType.
		productMapper.update(command, product);

		product.setType(
				resolveProductType(command.productTypeId())
		);

        syncTags(product, command.tagIds());

		syncVariants(product, true, command.variants());

		productRepository.flush();

		return productMapper.toResponse(product);
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		var product = productRepository.findById(id)
				.orElseThrow(() ->
						new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND)
				);

		productRepository.delete(product);
	}

    private void syncTags(Product product, List<UUID> tagIds) {
        // An omitted field preserves tags for existing API clients; [] clears them.
        if (tagIds == null) return;
        var ids = new HashSet<>(tagIds);
        var tags = tagRepository.findAllById(ids);
        if (tags.size() != ids.size()) throw new ResourceNotFoundException(ErrorCode.TAG_NOT_FOUND);
        product.getTags().clear();
        product.getTags().addAll(tags);
    }

	private void syncVariants(
			Product product,
			boolean isEdit,
			List<ProductVariantCommand> commands
	) {
		if (commands == null) {
			return;
		}

		var existingVariants = product.getVariants();
		var existingById = mapExistingVariants(existingVariants);
		var retainedIds = new HashSet<UUID>();

		for (var command : commands) {
			var variant = resolveOrCreateVariant(
					product,
					command,
					existingById,
					retainedIds
			);

			setupVariantValues(
					variant,
					command.attributeValueIds()
			);

			setupRentalPlans(
					variant,
					command.rentalPlans()
			);

			syncInventoryUnits(
					variant,
					command.inventoryUnits()
			);
		}

		if (!isEdit) {
			return;
		}

		removeUnretainedVariants(existingVariants, retainedIds);
	}

	private Map<UUID, ProductVariant> mapExistingVariants(
			List<ProductVariant> existingVariants
	) {
		return existingVariants.stream()
				.filter(variant -> variant.getId() != null)
				.collect(Collectors.toMap(
						ProductVariant::getId,
						Function.identity()
				));
	}

	private ProductVariant resolveOrCreateVariant(
			Product product,
			ProductVariantCommand command,
			Map<UUID, ProductVariant> existingById,
			HashSet<UUID> retainedIds
	) {
		if (command.id() == null) {
			var variant = variantMapper.toEntity(command);

			// Always associate the new variant with the managed parent.
			variant.setProduct(product);
			product.getVariants().add(variant);

			// Obtain the generated ID before creating dependent entities.
			productRepository.flush();

			// IMPORTANT: prevent the new variant from being removed
			// by removeUnretainedVariants().
			if (variant.getId() == null) {
				throw new IllegalStateException(
						ErrorCode.MESSAGE_PRODUCT_VARIANT_ID_WAS_NOT_GENERATED
				);
			}

			if (!retainedIds.add(variant.getId())) {
				throw new IllegalStateException(
						ErrorCode.MESSAGE_DUPLICATE_PRODUCT_VARIANT_ID
				);
			}

			return variant;
		}

		var variant = existingById.get(command.id());

		if (variant == null) {
			throw new ResourceNotFoundException(ErrorCode.VARIANT_NOT_FOUND);
		}

		if (!retainedIds.add(command.id())) {
			throw new ValidationException(
					ErrorCode.MESSAGE_DUPLICATE_PRODUCT_VARIANT_ID
			);
		}

		variantMapper.update(command, variant);

		return variant;
	}

	private void removeUnretainedVariants(
			List<ProductVariant> existingVariants,
			HashSet<UUID> retainedIds
	) {
		// Use a snapshot because the managed collection is modified below.
		var variantsToRemove = existingVariants.stream()
				.filter(variant ->
						variant.getId() != null
								&& !retainedIds.contains(variant.getId())
				)
				.toList();

		for (var variant : variantsToRemove) {
			unlinkInventoryUnits(variant);
			existingVariants.remove(variant);
		}
	}

	private void unlinkInventoryUnits(ProductVariant variant) {
		if (variant.getInventoryUnits() == null) {
			return;
		}

		for (var unit : variant.getInventoryUnits()) {
			unit.setVariant(null);
			inventoryUnitRepository.save(unit);
		}

		variant.getInventoryUnits().clear();
	}

	private void setupVariantValues(
			ProductVariant variant,
			List<UUID> attributeValueIds
	) {
		if (attributeValueIds == null) {
			return;
		}

		if (new HashSet<>(attributeValueIds).size()
				!= attributeValueIds.size()) {
			throw new ValidationException(
					ErrorCode.MESSAGE_DUPLICATE_ATTRIBUTE_VALUE_IDS_IN_VARIANT
			);
		}

		Map<UUID, AttributeValue> valuesById =
				attributeValueRepository.findAllById(attributeValueIds)
						.stream()
						.collect(Collectors.toMap(
								AttributeValue::getId,
								Function.identity()
						));

		if (valuesById.size() != attributeValueIds.size()) {
			throw new ValidationException(
					ErrorCode.MESSAGE_ONE_OR_MORE_ATTRIBUTE_VALUES_DO_NOT_EXIST
			);
		}

		List<UUID> attributeIds = attributeValueIds.stream()
				.map(valuesById::get)
				.map(value -> value.getAttribute().getId())
				.toList();

		if (new HashSet<>(attributeIds).size() != attributeIds.size()) {
			throw new ValidationException(
					ErrorCode.MESSAGE_A_VARIANT_CANNOT_HAVE_MULTIPLE_VALUES_FOR_THE_SAME_ATTRIBUTE
			);
		}

		// Delete old children before inserting replacements.
		// This is necessary when the child ID uses a composite key.
		variant.getValues().clear();
		productRepository.flush();

		for (UUID attributeValueId : attributeValueIds) {
			var attributeValue = valuesById.get(attributeValueId);

			var value = new ProductVariantValue();
			value.setVariant(variant);
			value.setAttribute(attributeValue.getAttribute());
			value.setAttributeValue(attributeValue);

			value.getId().setVariantId(variant.getId());
			value.getId().setAttributeId(
					attributeValue.getAttribute().getId()
			);

			variant.getValues().add(value);
		}
	}

	private void setupRentalPlans(
			ProductVariant variant,
			List<VariantRentalPlanCommand> commands
	) {
		if (commands == null) {
			return;
		}

		variant.getRentalPlans().clear();
		productRepository.flush();

		if (commands.isEmpty()) {
			return;
		}

		var plans = variantRentalPlanMapper.toEntities(commands);

		for (var plan : plans) {
			// Always create new rental-plan entities.
			plan.setVariant(variant);
			variant.getRentalPlans().add(plan);
		}
	}

	private void syncInventoryUnits(
			ProductVariant variant,
			List<InventoryUnitCommand> commands
	) {
		if (commands == null) {
			return;
		}

		for (var command : commands) {
			if (command.id() == null) {
				var unit = inventoryUnitMapper.toEntity(command);

				// Let persistence generate the ID.
				unit.setId(null);

				unit.setSerialNumber(
						"SN-" + UUID.randomUUID()
								.toString()
								.replace("-", "")
								.toUpperCase()
				);

				unit.setVariant(variant);
				variant.addInventoryUnit(unit);

				continue;
			}

			if (variant.getId() == null) {
				throw new ResourceNotFoundException(
						ErrorCode.INVENTORY_UNIT_NOT_FOUND
				);
			}

			var unit = inventoryUnitRepository
					.findByIdAndVariantId(
							command.id(),
							variant.getId()
					)
					.orElseThrow(() ->
							new ResourceNotFoundException(
									ErrorCode.INVENTORY_UNIT_NOT_FOUND
							)
					);

			inventoryUnitMapper.update(command, unit);

			if (command.status() != null) {
				unit.setStatus(command.status());
			}
		}

		// Inventory units omitted from the request are retained.
	}

	private void validateSlug(String slug) {
		if (productRepository.existsBySlug(slug)) {
			throw new ConflictException(
					ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS
			);
		}
	}

	private void validateSlug(UUID id, String slug) {
		if (productRepository.existsBySlugAndIdNot(slug, id)) {
			throw new ConflictException(
					ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS
			);
		}
	}

	private ProductType resolveProductType(UUID productTypeId) {
		if (productTypeId == null) {
			return null;
		}

		return productTypeRepository.findById(productTypeId)
				.orElseThrow(() ->
						new ResourceNotFoundException(
								ErrorCode.PRODUCT_TYPE_NOT_FOUND
						)
				);
	}
}
