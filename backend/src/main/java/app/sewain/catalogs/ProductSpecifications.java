package app.sewain.catalogs;

import app.sewain.shared.PageQuery;
import app.sewain.shared.SearchUtils;
import app.sewain.shared.SearchUtils.Condition;
import app.sewain.shared.SearchUtils.Group;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static app.sewain.shared.SearchUtils.FieldRule.*;
import static app.sewain.shared.SearchUtils.Logic.*;
import static app.sewain.shared.SearchUtils.Operator.*;

final class ProductSpecifications {

	private ProductSpecifications() {
	}

	private static final Map<String, SearchUtils.FieldRule> RULES = Map.of(
			"name", of(true, CONTAINS),
            "id", of(false, EQUAL, IN),
			"slug", of(true, CONTAINS),
			"status", of(false, EQUAL),
			"type.id", of(false, EQUAL, IN),
			"createdAt", of(true, EQUAL)
	);

	static Specification<Product> search(Map<String, String> values) {
		return search(values, List.of());
	}

	static Specification<Product> search(
			Map<String, String> values,
			List<Map<String, String>> sort
	) {
		if (values == null || values.isEmpty()) {
			values = Map.of();
		}

		var filters = new ArrayList<SearchUtils.Filter>();

		var keyword = values.get("keyword");
		if (keyword != null && !keyword.isBlank()) {
			filters.add(new Group(
					OR,
					List.of(
							new Condition("name", CONTAINS, keyword),
							new Condition("slug", CONTAINS, keyword)
					)
			));
		}

		values.forEach((field, value) -> {
			var rule = RULES.get(field);

			if (rule != null && value != null && !value.isBlank()
					&& !"createdAt".equals(field)) {
				var operator = ("type.id".equals(field) || "id".equals(field)) && value.contains(",") ? IN
						: rule.operators().contains(EQUAL) ? EQUAL : rule.operators().iterator().next();
				filters.add(new Condition(
						field,
						operator,
						value
				));
			}
		});

		Specification<Product> specification = filters.isEmpty()
				? Specification.unrestricted()
				: SearchUtils.from(
				Product.class,
				new Group(AND, filters),
				RULES
		);

		return specification
				.and(availableProducts(values.get("available")))
				.and(rentalPrice(values, sort));
	}

	static Sort toSort(PageQuery query) {
		var sortFields = query.getSort().stream()
				.map(item -> new SearchUtils.SortField(
						item.get("key"),
						Direction.fromString(item.get("order"))
				))
				.toList();
		if (sortFields.stream().anyMatch(field -> "rentalPrice".equals(field.field()))) {
			return Sort.unsorted();
		}

		return SearchUtils.toSort(Product.class, sortFields, RULES).and(Sort.by("id"));
	}

	private static Specification<Product> availableProducts(String available) {
		if (!Boolean.parseBoolean(available)) return Specification.unrestricted();
		return (root, query, cb) -> {
			var subquery = query.subquery(java.util.UUID.class);
			var variant = subquery.from(ProductVariant.class);
			subquery.select(variant.get("id"))
					.where(cb.equal(variant.get("product"), root), cb.isTrue(variant.get("active")));
			return cb.exists(subquery);
		};
	}

	private static Specification<Product> rentalPrice(
			Map<String, String> values,
			List<Map<String, String>> sort
	) {
		var minimum = decimal(values.get("minimumRentalPrice"));
		var maximum = decimal(values.get("maximumRentalPrice"));
		var priceSort = sort.stream()
				.filter(item -> "rentalPrice".equals(item.get("key")))
				.findFirst()
				.orElse(null);
		if (minimum == null && maximum == null && priceSort == null) {
			return Specification.unrestricted();
		}

		return (root, query, cb) -> {
			Subquery<BigDecimal> minimumPrice = query.subquery(BigDecimal.class);
			var correlatedProduct = minimumPrice.correlate(root);
			Join<Product, ProductVariant> variant = correlatedProduct.join("variants");
			Join<ProductVariant, VariantRentalPlan> plan = variant.join("rentalPlans");
			minimumPrice.select(cb.min(plan.get("rentalFee")))
					.where(cb.isTrue(variant.get("active")), cb.isTrue(plan.get("active")));

			var predicates = new ArrayList<Predicate>();
			if (minimum != null) predicates.add(cb.greaterThanOrEqualTo(minimumPrice, minimum));
			if (maximum != null) predicates.add(cb.lessThanOrEqualTo(minimumPrice, maximum));

			if (priceSort != null && query.getResultType() != Long.class
					&& query.getResultType() != long.class) {
				var direction = Direction.fromString(priceSort.getOrDefault("order", "asc"));
				query.orderBy(direction.isAscending()
						? cb.asc(minimumPrice)
						: cb.desc(minimumPrice), cb.asc(root.get("name")), cb.asc(root.get("id")));
			}
			return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static BigDecimal decimal(String value) {
		return value == null || value.isBlank() ? null : new BigDecimal(value);
	}
}
