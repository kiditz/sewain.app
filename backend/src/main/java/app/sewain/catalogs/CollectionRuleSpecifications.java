package app.sewain.catalogs;

import app.sewain.exception.ValidationException;

import app.sewain.exception.ErrorCode;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Mengubah rule collection AUTOMATED menjadi Specification<Product>.
 *
 * <h3>Semantik</h3>
 * <ul>
 *   <li>Beberapa value dalam satu rule digabung dengan OR
 *       (cocok dengan salah satu value).</li>
 *   <li>Operator negatif (NOT_EQUALS / NOT_CONTAINS) = NOT(cocok dengan
 *       salah satu value), jadi "tag NOT_EQUALS [a, b]" berarti produk tidak
 *       punya tag a maupun b.</li>
 *   <li>Antar rule digabung sesuai match condition: ALL = AND, ANY = OR.</li>
 *   <li>Collection tanpa rule tidak cocok dengan produk apa pun.</li>
 *   <li>Perbandingan tidak peka huruf besar/kecil.</li>
 * </ul>
 */
final class CollectionRuleSpecifications {

	private static final char LIKE_ESCAPE = '!';

	private CollectionRuleSpecifications() {
	}

	/** Snapshot rule agar Specification tidak menyentuh entity lazy di luar transaksi. */
	private record RuleSpec(
			CollectionRuleAttribute attribute,
			CollectionRuleOperator operator,
			List<String> values
	) {
	}

	static Specification<Product> matching(Collection collection) {
		var rules = collection.getRules().stream()
				.map(rule -> new RuleSpec(
						rule.getAttribute(),
						rule.getOperator(),
						rule.getValues().stream()
								.map(CollectionRuleValue::getValue)
								.toList()
				))
				.filter(rule -> !rule.values().isEmpty())
				.toList();

		if (rules.isEmpty()) {
			return (root, query, cb) -> cb.disjunction();
		}

		var matchAny = collection.getMatchCondition() == CollectionMatchCondition.ANY;

		return (root, query, cb) -> {
			var predicates = rules.stream()
					.map(rule -> toPredicate(rule, root, query, cb))
					.toArray(Predicate[]::new);

			return matchAny ? cb.or(predicates) : cb.and(predicates);
		};
	}

	private static Predicate toPredicate(
			RuleSpec rule,
			Root<Product> root,
			CriteriaQuery<?> query,
			CriteriaBuilder cb
	) {
		Predicate positive = switch (rule.attribute()) {
			case TITLE -> anyValue(cb, root.get("name"), rule);

			case TYPE -> {
				// Produk tanpa type dianggap bernilai "" (agar NOT_* tetap cocok).
				Join<Product, ProductType> type = root.join("type", JoinType.LEFT);
				yield anyValue(cb, cb.coalesce(type.get("name"), ""), rule);
			}

			case TAG -> existsIn(query, cb, root, sub -> {
				Join<Product, Tag> tag = sub.join("tags");
				return tag.get("name");
			}, rule);

			case VARIANT_TITLE -> existsIn(query, cb, root, sub -> {
				Join<Product, ProductVariant> variant = sub.join("variants");
				return variant.get("name");
			}, rule);

			case ATTRIBUTE_VALUE -> existsIn(query, cb, root, sub -> {
				Join<Product, ProductVariant> variant = sub.join("variants");
				Join<ProductVariant, ProductVariantValue> variantValue = variant.join("values");
				Join<ProductVariantValue, AttributeValue> attributeValue =
						variantValue.join("attributeValue");
				return attributeValue.get("value");
			}, rule);
		};

		return rule.operator().isNegative() ? cb.not(positive) : positive;
	}

	/**
	 * Untuk atribut multi-value (tag, varian, ...): produk cocok jika ADA
	 * salah satu elemen yang cocok. EXISTS dipakai agar tidak menggandakan
	 * baris produk dan agar NOT_* benar ("tidak ada elemen yang cocok").
	 */
	private static Predicate existsIn(
			CriteriaQuery<?> query,
			CriteriaBuilder cb,
			Root<Product> root,
			Function<Root<Product>, Expression<String>> path,
			RuleSpec rule
	) {
		Subquery<Integer> subquery = query.subquery(Integer.class);
		Root<Product> sub = subquery.correlate(root);

		subquery.select(cb.literal(1))
				.where(anyValue(cb, path.apply(sub), rule));

		return cb.exists(subquery);
	}

	private static Predicate anyValue(
			CriteriaBuilder cb,
			Expression<String> expression,
			RuleSpec rule
	) {
		var operator = rule.operator().positive();

		return cb.or(
				rule.values().stream()
						.map(value -> match(cb, expression, operator, value))
						.toArray(Predicate[]::new)
		);
	}

	private static Predicate match(
			CriteriaBuilder cb,
			Expression<String> expression,
			CollectionRuleOperator operator,
			String value
	) {
		var lowered = cb.lower(expression);
		var needle = value.trim().toLowerCase(Locale.ROOT);

		return switch (operator) {
			case EQUALS -> cb.equal(lowered, needle);
			case CONTAINS -> cb.like(lowered, "%" + escape(needle) + "%", LIKE_ESCAPE);
			case STARTS_WITH -> cb.like(lowered, escape(needle) + "%", LIKE_ESCAPE);
			case ENDS_WITH -> cb.like(lowered, "%" + escape(needle), LIKE_ESCAPE);
			default -> throw new ValidationException(
					ErrorCode.MESSAGE_UNSUPPORTED_OPERATOR + operator
			);
		};
	}

	private static String escape(String value) {
		return value
				.replace("!", "!!")
				.replace("%", "!%")
				.replace("_", "!_");
	}
}
