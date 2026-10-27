package app.sewain.catalogs;

import app.sewain.shared.PageQuery;
import app.sewain.shared.SearchUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.data.domain.Sort.Direction.fromString;

final class CollectionSpecifications {

	private CollectionSpecifications() {}

	private static final Map<String, SearchUtils.FieldRule> RULES = Map.of(
			"name", SearchUtils.FieldRule.of(true, SearchUtils.Operator.CONTAINS),
			"slug", SearchUtils.FieldRule.of(true, SearchUtils.Operator.CONTAINS),
			"parent.name", SearchUtils.FieldRule.of(false, SearchUtils.Operator.CONTAINS)
	);

	public static Specification<Collection> search(
			Map<String, String> searchValues
	) {
		if (searchValues == null || searchValues.isEmpty()) {
			return Specification.unrestricted();
		}

		var filters = new ArrayList<SearchUtils.Filter>();

		searchValues.forEach((field, value) -> {
			var rule = RULES.get(field);

			if (rule != null && value != null && !value.isBlank()) {
				filters.add(new SearchUtils.Condition(
						field,
						rule.operators().iterator().next(),
						value
				));
			}
		});

		return filters.isEmpty()
				? Specification.unrestricted()
				: SearchUtils.from(
				Collection.class,
				new SearchUtils.Group(SearchUtils.Logic.AND, filters),
				RULES
		);
	}

	public static Sort toSort(PageQuery query) {
		var sortFields = query.getSort().stream()
				.map(item -> new SearchUtils.SortField(
						item.get("key"),
						fromString(item.get("order"))
				))
				.toList();

		return SearchUtils.toSort(Collection.class, sortFields, RULES);
	}
}