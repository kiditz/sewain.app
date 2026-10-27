package app.sewain.orders;

import app.sewain.shared.PageQuery;
import app.sewain.shared.SearchUtils;
import app.sewain.shared.SearchUtils.Condition;
import app.sewain.shared.SearchUtils.Group;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static app.sewain.shared.SearchUtils.FieldRule.of;
import static app.sewain.shared.SearchUtils.Logic.AND;
import static app.sewain.shared.SearchUtils.Logic.OR;
import static app.sewain.shared.SearchUtils.Operator.CONTAINS;
import static app.sewain.shared.SearchUtils.Operator.EQUAL;

final class MerchantOrderSpecifications {
	private MerchantOrderSpecifications() {}

	private static final Map<String, SearchUtils.FieldRule> RULES = Map.of(
			"orderNumber", of(true, CONTAINS),
			"customerName", of(true, CONTAINS),
			"customerEmail", of(false, CONTAINS),
			"status", of(true, EQUAL),
			"fulfillmentType", of(true, EQUAL),
			"paymentMethod", of(true, EQUAL),
			"createdAt", of(true, EQUAL),
			"totalAmount", of(true, EQUAL)
	);

	static Specification<RentalOrder> search(Map<String, String> values) {
		if (values == null || values.isEmpty()) return Specification.unrestricted();
		var filters = new ArrayList<SearchUtils.Filter>();
		var keyword = values.get("keyword");
		if (keyword != null && !keyword.isBlank()) {
			filters.add(new Group(OR, List.of(
					new Condition("orderNumber", CONTAINS, keyword),
					new Condition("customerName", CONTAINS, keyword),
					new Condition("customerEmail", CONTAINS, keyword)
			)));
		}
		values.forEach((field, value) -> {
			var rule = RULES.get(field);
			if (rule != null && value != null && !value.isBlank()) {
				filters.add(new Condition(field, rule.operators().iterator().next(), value));
			}
		});
		return filters.isEmpty() ? Specification.unrestricted()
				: SearchUtils.from(RentalOrder.class, new Group(AND, filters), RULES);
	}

	static Sort toSort(PageQuery query) {
		if (query.getSort() == null || query.getSort().isEmpty()
				|| (query.getSort().size() == 1 && "name".equals(query.getSort().getFirst().get("key")))) {
			return Sort.by(Sort.Direction.DESC, "createdAt");
		}
		var sortFields = query.getSort().stream()
				.map(item -> new SearchUtils.SortField(item.get("key"), Sort.Direction.fromString(item.get("order"))))
				.toList();
		return SearchUtils.toSort(RentalOrder.class, sortFields, RULES);
	}
}
