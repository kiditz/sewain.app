package app.sewain.catalogs;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import app.sewain.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
class CollectionCommandServiceImpl implements CollectionCommandService {
	private final CollectionRepository repository;
	private final ProductRepository productRepository;
	private final CollectionMapper mapper;

	@Override
	@Transactional
	public CollectionDetailResponse create(CollectionCommand command) {
		var collection = mapper.toEntity(command);

		// Command lama tidak mengirim type / matchCondition.
		if (collection.getType() == null) {
			collection.setType(CollectionType.MANUAL);
		}
		if (collection.getMatchCondition() == null) {
			collection.setMatchCondition(CollectionMatchCondition.ALL);
		}

		if (command.parentId() != null) {
			var parent = repository.findById(command.parentId())
					.orElseThrow(() ->
							new ResourceNotFoundException(
									ErrorCode.COLLECTION_NOT_FOUND
							)
					);
			collection.setParent(parent);
			var lastOrder = repository.findMaxSortOrderByParentId(parent.getId());
			collection.setPosition(lastOrder + 1);
		} else {
			var lastOrder = repository.findMaxSortOrderForRoot();
			collection.setPosition(lastOrder + 1);
		}

		applyMembership(collection, command);

		return mapper.toDetail(repository.save(collection));
	}

	@Override
	@Transactional
	public CollectionDetailResponse update(UUID id, CollectionCommand command) {
		var collection = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COLLECTION_NOT_FOUND));
		mapper.update(command, collection);
		if (command.parentId() != null) {
			var parent = repository.findById(command.parentId()).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COLLECTION_NOT_FOUND));
			collection.setParent(parent);
		}

		applyMembership(collection, command);

		return mapper.toDetail(collection);
	}

	@Override
	@Transactional
	public void reorder(CollectionReorderCommand command) {
		var collectionIds = command.collectionIds();

		if (collectionIds.isEmpty()) {
			return;
		}

		var collections = repository.findAllById(collectionIds);

		if (collections.size() != collectionIds.size()) {
			throw new ResourceNotFoundException(
					ErrorCode.COLLECTION_NOT_FOUND
			);
		}

		var parentId = getParentId(collections.getFirst());

		if (collections.stream()
				.anyMatch(collection ->
						!Objects.equals(getParentId(collection), parentId))) {
			throw new ValidationException(
					ErrorCode.CATEGORY_REORDER_INVALID
			);
		}

		var siblings = parentId == null
				? repository.findRootCategories()
				: repository.findSiblings(parentId);

		var positions = new HashMap<UUID, Integer>();

		for (int i = 0; i < siblings.size(); i++) {
			positions.put(siblings.get(i).getId(), i);
		}

		var targetPositions = collectionIds.stream()
				.map(positions::get)
				.sorted()
				.toList();

		var collectionsById = collections.stream()
				.collect(Collectors.toMap(
						Collection::getId,
						collection -> collection
				));

		for (int i = 0; i < collectionIds.size(); i++) {
			collectionsById
					.get(collectionIds.get(i))
					.setPosition(targetPositions.get(i));
		}
	}

	private UUID getParentId(Collection collection) {
		return collection.getParent() == null
				? null
				: collection.getParent().getId();
	}

	@Transactional
	@Override
	public void delete(UUID id) {
		var collection = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COLLECTION_NOT_FOUND));
		repository.delete(collection);
	}

	/**
	 * MANUAL    : simpan produk pilihan (rules dibuang).
	 * AUTOMATED : simpan rules (produk pilihan dibuang), minimal 1 rule.
	 * <p>
	 * rules / productIds bernilai null berarti "jangan disentuh".
	 */
	private void applyMembership(Collection collection, CollectionCommand command) {
		if (collection.getType() == CollectionType.AUTOMATED) {
			if (command.rules() != null) {
				replaceRules(collection, command.rules());
			}

			if (collection.getRules().isEmpty()) {
				throw new ValidationException(
						ErrorCode.COLLECTION_RULES_REQUIRED
				);
			}

			collection.getItems().clear();
			return;
		}

		collection.getRules().clear();

		if (command.productIds() != null) {
			syncItems(collection, command.productIds());
		}
	}

	private void replaceRules(
			Collection collection,
			List<CollectionRuleCommand> commands
	) {
		collection.getRules().clear();

		var position = 0;

		for (var command : commands) {
			var values = command.values().stream()
					.map(String::trim)
					.filter(value -> !value.isEmpty())
					.distinct()
					.toList();

			if (values.isEmpty()) {
				continue;
			}

			var rule = new CollectionRule();
			rule.setCollection(collection);
			rule.setAttribute(command.attribute());
			rule.setOperator(command.operator());
			rule.setPosition(position++);
			values.forEach(rule::addValue);

			collection.getRules().add(rule);
		}
	}

	/**
	 * Sinkronkan item dengan urutan productIds. Item yang masih ada dipakai
	 * ulang (hanya position yang berubah) supaya tidak melanggar unique
	 * (collection_id, product_id) saat insert dan delete di satu flush.
	 */
	private void syncItems(Collection collection, List<UUID> productIds) {
		var ids = new ArrayList<>(new LinkedHashSet<>(productIds));

		var products = productRepository.findAllById(ids).stream()
				.collect(Collectors.toMap(Product::getId, product -> product));

		if (products.size() != ids.size()) {
			throw new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND);
		}

		var existing = collection.getItems().stream()
				.collect(Collectors.toMap(
						item -> item.getProduct().getId(),
						item -> item
				));

		var ordered = new ArrayList<CollectionItem>(ids.size());

		for (var i = 0; i < ids.size(); i++) {
			var item = existing.get(ids.get(i));

			if (item == null) {
				item = new CollectionItem();
				item.setCollection(collection);
				item.setProduct(products.get(ids.get(i)));
			}

			item.setPosition(i);
			ordered.add(item);
		}

		// Item yang tidak ada di `ordered` menjadi orphan dan dihapus.
		collection.getItems().clear();
		collection.getItems().addAll(ordered);
	}

}
