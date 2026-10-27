package app.sewain.catalogs;

import app.sewain.shared.PageQuery;
import app.sewain.shared.SearchUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static org.springframework.data.domain.Sort.Direction.fromOptionalString;

final class InventorySpecifications {

	private InventorySpecifications() {
	}

	private static final Map<String, SearchUtils.FieldRule> RULES = Map.of(
			"skuBarcode", SearchUtils.FieldRule.of(true, SearchUtils.Operator.CONTAINS),
			"serialNumber", SearchUtils.FieldRule.of(true, SearchUtils.Operator.CONTAINS),
			"status", SearchUtils.FieldRule.of(true, SearchUtils.Operator.EQUAL),
			"variant.name", SearchUtils.FieldRule.of(true, SearchUtils.Operator.CONTAINS),
			"variant.sku", SearchUtils.FieldRule.of(true, SearchUtils.Operator.CONTAINS),
			"variant.product.name", SearchUtils.FieldRule.of(true, SearchUtils.Operator.CONTAINS),
			"createdAt", SearchUtils.FieldRule.of(true, SearchUtils.Operator.EQUAL),
			"updatedAt", SearchUtils.FieldRule.of(true, SearchUtils.Operator.EQUAL)
	);

	private static final Set<String> SORT_ONLY_FIELDS = Set.of("createdAt", "updatedAt");

	private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt")
			.and(Sort.by(Sort.Direction.ASC, "id"));

	static Specification<InventoryUnit> search(Map<String, String> searchValues) {
		if (searchValues == null || searchValues.isEmpty()) {
			return Specification.unrestricted();
		}

		var filters = searchValues.entrySet().stream()
				.filter(entry -> RULES.containsKey(entry.getKey()))
				// createdAt / updatedAt hanya untuk sorting, bukan filter.
				.filter(entry -> !SORT_ONLY_FIELDS.contains(entry.getKey()))
				.filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
				.toList();

		var conditions = filters.stream()
				.filter(entry -> !"status".equals(entry.getKey()))
				.map(entry -> (SearchUtils.Filter) new SearchUtils.Condition(
						entry.getKey(),
						RULES.get(entry.getKey()).operators().iterator().next(),
						entry.getValue()
				))
				.toList();

		var statusFilters = filters.stream()
				.filter(entry -> "status".equals(entry.getKey()))
				.map(entry -> (SearchUtils.Filter) new SearchUtils.Condition(
						entry.getKey(),
						SearchUtils.Operator.EQUAL,
						entry.getValue()
				))
				.toList();

		var groups = new ArrayList<SearchUtils.Filter>();

		if (!statusFilters.isEmpty()) {
			groups.addAll(statusFilters);
		}

		if (!conditions.isEmpty()) {
			groups.add(new SearchUtils.Group(SearchUtils.Logic.OR, conditions));
		}

		return groups.isEmpty()
				? Specification.unrestricted()
				: SearchUtils.from(
				InventoryUnit.class,
				new SearchUtils.Group(SearchUtils.Logic.AND, groups),
				RULES
		);
	}

	/**
	 * Key sort yang tidak dikenal diabaikan (bukan error), dan jika tidak ada
	 * yang valid dipakai default: createdAt DESC.
	 */
	public static Sort toSort(PageQuery query) {
		var sortFields = query.getSort().stream()
				.filter(item -> {
					var rule = RULES.get(item.get("key"));
					return rule != null && rule.sortable();
				})
				.map(item -> new SearchUtils.SortField(
						item.get("key"),
						fromOptionalString(item.get("order")).orElse(Sort.Direction.ASC)
				))
				.toList();

		if (sortFields.isEmpty()) {
			return DEFAULT_SORT;
		}

		// Tie-breaker supaya pagination stabil.
		return SearchUtils.toSort(InventoryUnit.class, sortFields, RULES)
				.and(Sort.by(Sort.Direction.ASC, "id"));
	}
}