package app.sewain.customers;

import app.sewain.shared.PageQuery;
import app.sewain.shared.SearchUtils;
import app.sewain.shared.SearchUtils.Condition;
import app.sewain.shared.SearchUtils.Group;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static app.sewain.shared.SearchUtils.FieldRule.of;
import static app.sewain.shared.SearchUtils.Logic.AND;
import static app.sewain.shared.SearchUtils.Logic.OR;
import static app.sewain.shared.SearchUtils.Operator.CONTAINS;
import static app.sewain.shared.SearchUtils.Operator.EQUAL;

final class CustomerSpecifications {
	private static final Map<String, SearchUtils.FieldRule> RULES = Map.of(
			"fullName", of(true, CONTAINS),
			"email", of(true, CONTAINS),
			"phone", of(true, CONTAINS),
			"enabled", of(true, EQUAL),
			"createdAt", of(true, EQUAL)
	);

	private CustomerSpecifications() {}

	static Specification<Customer> search(Map<String, String> values) {
		if (values == null || values.isEmpty()) return Specification.unrestricted();
		var filters = new ArrayList<SearchUtils.Filter>();
		var keyword = values.get("keyword");
		if (keyword != null && !keyword.isBlank()) {
			filters.add(new Group(OR, List.of(
					new Condition("fullName", CONTAINS, keyword),
					new Condition("email", CONTAINS, keyword),
					new Condition("phone", CONTAINS, keyword)
			)));
		}
		var enabled = values.get("enabled");
		if (enabled != null && !enabled.isBlank()) {
			String value = enabled.equalsIgnoreCase("ACTIVE") ? "true"
					: enabled.equalsIgnoreCase("INACTIVE") ? "false" : enabled;
			filters.add(new Condition("enabled", EQUAL, value));
		}
		return filters.isEmpty() ? Specification.unrestricted()
				: SearchUtils.from(Customer.class, new Group(AND, filters), RULES);
	}

	static Sort toSort(PageQuery query) {
		if (query.getSort().isEmpty() || "name".equals(query.getSort().getFirst().get("key"))) {
			return Sort.by(Direction.DESC, "createdAt");
		}
		var fields = query.getSort().stream()
				.map(item -> new SearchUtils.SortField(item.get("key"), Direction.fromString(item.get("order"))))
				.toList();
		return SearchUtils.toSort(Customer.class, fields, RULES);
	}
}
