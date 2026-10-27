
package app.sewain.shared;

import app.sewain.exception.ErrorCode;
import app.sewain.exception.ValidationException;

import jakarta.persistence.Transient;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.function.Function;

public final class SearchUtils {

	private static final char LIKE_ESCAPE = '!';

	public enum Operator {
		LIKE,
		CONTAINS,
		STARTS_WITH,
		ENDS_WITH,
		EQUAL,
		NOT_EQUAL,
		IN,
		NOT_IN,
		GREATER_THAN,
		GREATER_THAN_OR_EQUAL,
		LESS_THAN,
		LESS_THAN_OR_EQUAL,
		IS_NULL,
		IS_NOT_NULL
	}

	public enum Logic {
		AND,
		OR
	}

	public sealed interface Filter
			permits Condition, Group {
	}

	public record Condition(
			String field,
			Operator operator,
			Object value
	) implements Filter {
	}

	public record Group(
			Logic logic,
			List<Filter> filters
	) implements Filter {
		public Group {
			if (logic == null) {
				throw new ValidationException(
						ErrorCode.MESSAGE_FILTER_LOGIC_MUST_NOT_BE_NULL
				);
			}

			filters = filters == null
					? List.of()
					: List.copyOf(filters);
		}
	}

	public record FieldRule(
			Set<Operator> operators,
			boolean sortable
	) {
		public FieldRule {
			if (operators == null || operators.isEmpty()) {
				throw new ValidationException(
						ErrorCode.MESSAGE_AT_LEAST_ONE_OPERATOR_IS_REQUIRED
				);
			}

			operators = Set.copyOf(operators);
		}

		public static FieldRule of(
				boolean sortable,
				Operator... operators
		) {
			return new FieldRule(
					Set.of(operators),
					sortable
			);
		}

		public static FieldRule searchable() {
			return new FieldRule(
					EnumSet.allOf(Operator.class),
					false
			);
		}
	}

	public record SortField(
			String field,
			Sort.Direction direction
	) {
		public SortField {
			if (field == null || field.isBlank()) {
				throw new ValidationException(
						ErrorCode.MESSAGE_SORT_FIELD_MUST_NOT_BE_BLANK
				);
			}

			direction = direction == null
					? Sort.Direction.ASC
					: direction;
		}
	}

	private SearchUtils() {
	}

	public static <T> Specification<T> search(
			Class<T> entityClass,
			Map<String, String> searchValues,
			Map<String, Operator> fields
	) {
		return search(entityClass, searchValues, fields, false);
	}

	public static <T> Specification<T> search(
			Class<T> entityClass,
			Map<String, String> searchValues,
			Map<String, Operator> fields,
			boolean useOr
	) {
		if (searchValues == null || searchValues.isEmpty()) {
			return (root, query, cb) -> cb.conjunction();
		}

		Map<String, FieldRule> rules = fields.entrySet().stream()
				.collect(java.util.stream.Collectors.toMap(
						Map.Entry::getKey,
						entry -> FieldRule.of(
								false,
								entry.getValue()
						)
				));

		List<Filter> filters = searchValues.entrySet().stream()
				.filter(entry -> {
					Operator operator = fields.get(entry.getKey());

					return operator != null
							&& (operator == Operator.IS_NULL
							|| operator == Operator.IS_NOT_NULL
							|| entry.getValue() != null
							&& !entry.getValue().isBlank());
				})
				.map(entry -> (Filter) new Condition(
						entry.getKey(),
						fields.get(entry.getKey()),
						entry.getValue()
				))
				.toList();

		Filter filter = new Group(
				useOr ? Logic.OR : Logic.AND,
				filters
		);

		return from(entityClass, filter, rules);
	}

	public static <T> Specification<T> from(
			Class<T> entityClass,
			Filter filter,
			Map<String, FieldRule> rules
	) {
		if (filter == null) {
			return (root, query, cb) -> cb.conjunction();
		}

		validateConfiguration(entityClass, rules);

		return (root, query, cb) ->
				buildPredicate(
						entityClass,
						filter,
						root,
						query,
						cb,
						rules
				);
	}

	private static <T> Predicate buildPredicate(
			Class<T> entityClass,
			Filter filter,
			Root<T> root,
			jakarta.persistence.criteria.CriteriaQuery<?> query,
			CriteriaBuilder cb,
			Map<String, FieldRule> rules
	) {
		if (filter instanceof Condition condition) {
			return buildCondition(
					entityClass,
					condition,
					root,
					query,
					cb,
					rules
			);
		}

		if (filter instanceof Group(Logic logic, List<Filter> filters)) {
			Predicate[] predicates = filters
					.stream()
					.map(child -> buildPredicate(
							entityClass,
							child,
							root,
							query,
							cb,
							rules
					))
					.toArray(Predicate[]::new);

			return switch (logic) {
				case AND -> cb.and(predicates);
				case OR -> cb.or(predicates);
			};
		}

		throw new ValidationException(
				ErrorCode.MESSAGE_UNSUPPORTED_FILTER_TYPE
		);
	}

	private static <T> Predicate buildCondition(
			Class<T> entityClass,
			Condition condition,
			Root<T> root,
			jakarta.persistence.criteria.CriteriaQuery<?> query,
			CriteriaBuilder cb,
			Map<String, FieldRule> rules
	) {
		if (condition.field() == null
				|| condition.field().isBlank()
				|| condition.operator() == null) {
			throw new ValidationException(
					ErrorCode.MESSAGE_FILTER_FIELD_AND_OPERATOR_ARE_REQUIRED
			);
		}

		FieldRule rule = rules.get(condition.field());

		if (rule == null) {
			throw new ValidationException(
					ErrorCode.MESSAGE_UNSUPPORTED_SEARCH_FIELD + condition.field()
			);
		}

		if (!rule.operators().contains(condition.operator())) {
			throw new ValidationException(
					ErrorCode.MESSAGE_UNSUPPORTED_OPERATOR_FOR_FIELD.formatted(
							condition.operator(), condition.field())
			);
		}

		Operator operator = condition.operator();

		Class<?> fieldType = validatePath(
				entityClass,
				condition.field()
		);

		Object value = condition.value();

		if (operator != Operator.IS_NULL
				&& operator != Operator.IS_NOT_NULL
				&& value == null) {
			throw new ValidationException(
					ErrorCode.MESSAGE_VALUE_IS_REQUIRED_FOR + operator
			);
		}

		Path<?> path = resolvePath(
				root,
				condition.field(),
				query
		);

		return buildValuePredicate(
				cb,
				path,
				fieldType,
				operator,
				value
		);
	}

	private static Predicate buildValuePredicate(
			CriteriaBuilder cb,
			Path<?> path,
			Class<?> fieldType,
			Operator operator,
			Object value
	) {
		return switch (operator) {
			case LIKE, CONTAINS, STARTS_WITH, ENDS_WITH -> buildLikePredicate(
					cb, path, fieldType, operator, value
			);

			case EQUAL -> cb.equal(path, convertValue(value, fieldType));

			case NOT_EQUAL -> cb.notEqual(path, convertValue(value, fieldType));

			case IN -> path.in(convertValues(value, fieldType));

			case NOT_IN -> cb.not(path.in(convertValues(value, fieldType)));

			case GREATER_THAN -> compare(cb, path, fieldType, value, Comparison.GT);

			case GREATER_THAN_OR_EQUAL -> compare(cb, path, fieldType, value, Comparison.GE);

			case LESS_THAN -> compare(cb, path, fieldType, value, Comparison.LT);

			case LESS_THAN_OR_EQUAL -> compare(cb, path, fieldType, value, Comparison.LE);

			case IS_NULL -> cb.isNull(path);

			case IS_NOT_NULL -> cb.isNotNull(path);
		};
	}

	private static Predicate buildLikePredicate(
			CriteriaBuilder cb,
			Path<?> path,
			Class<?> fieldType,
			Operator operator,
			Object value
	) {
		if (fieldType != String.class) {
			throw new IllegalArgumentException(
					operator + " requires a String field"
			);
		}

		String escaped = escapeLike(String.valueOf(value))
				.toLowerCase(Locale.ROOT);

		String pattern = switch (operator) {
			case LIKE, CONTAINS -> "%" + escaped + "%";
			case STARTS_WITH -> escaped + "%";
			case ENDS_WITH -> "%" + escaped;
			default -> throw new ValidationException(
					ErrorCode.MESSAGE_UNSUPPORTED_LIKE_OPERATOR + operator
			);
		};

		return cb.like(
				cb.lower(path.as(String.class)),
				pattern,
				LIKE_ESCAPE
		);
	}

	private enum Comparison {
		GT, GE, LT, LE
	}

	@SuppressWarnings({"rawtypes"})
	private static Predicate compare(
			CriteriaBuilder cb,
			Path<?> path,
			Class<?> fieldType,
			Object value,
			Comparison comparison
	) {
		if (!Comparable.class.isAssignableFrom(fieldType)) {
			throw new IllegalArgumentException(
					ErrorCode.MESSAGE_FIELD_IS_NOT_COMPARABLE
							+ fieldType.getSimpleName()
			);
		}

		Comparable converted = (Comparable) convertValue(
				value,
				fieldType
		);

		Expression expression = path;

		return switch (comparison) {
			case GT -> cb.greaterThan(expression, converted);
			case GE -> cb.greaterThanOrEqualTo(expression, converted);
			case LT -> cb.lessThan(expression, converted);
			case LE -> cb.lessThanOrEqualTo(expression, converted);
		};
	}

	private static String escapeLike(String value) {
		return value
				.replace("!", "!!")
				.replace("%", "!%")
				.replace("_", "!_");
	}

	private static List<Object> convertValues(
			Object value,
			Class<?> fieldType
	) {
		List<?> values;

		if (value instanceof Collection<?> collection) {
			values = List.copyOf(collection);
		} else if (value.getClass().isArray()) {
			throw new ValidationException(
					ErrorCode.MESSAGE_USE_A_COLLECTION_FOR_IN_NOT_IN_VALUES
			);
		} else if (value instanceof String string) {
			values = List.of(string.split(",", -1));
		} else {
			values = List.of(value);
		}

		List<Object> converted = values.stream()
				.map(item -> convertValue(item, fieldType))
				.toList();

		if (converted.isEmpty()) {
			throw new ValidationException(
					ErrorCode.MESSAGE_IN_NOT_IN_REQUIRES_AT_LEAST_ONE_VALUE
			);
		}

		if (converted.stream().anyMatch(java.util.Objects::isNull)) {
			throw new ValidationException(
					ErrorCode.MESSAGE_IN_NOT_IN_DOES_NOT_ACCEPT_NULL_VALUES
			);
		}

		return converted;
	}

	private static final Map<Class<?>, Function<String, ?>> VALUE_CONVERTERS =
			Map.<Class<?>, Function<String, ?>>ofEntries(
					Map.entry(String.class, Function.identity()),
					Map.entry(UUID.class, UUID::fromString),
					Map.entry(Long.class, Long::valueOf),
					Map.entry(Integer.class, Integer::valueOf),
					Map.entry(Short.class, Short::valueOf),
					Map.entry(Byte.class, Byte::valueOf),
					Map.entry(Double.class, Double::valueOf),
					Map.entry(Float.class, Float::valueOf),
					Map.entry(BigDecimal.class, BigDecimal::new),
					Map.entry(BigInteger.class, BigInteger::new),
					Map.entry(Boolean.class, SearchUtils::parseBoolean),
					Map.entry(Character.class, SearchUtils::parseCharacter),
					Map.entry(LocalDate.class, LocalDate::parse),
					Map.entry(LocalTime.class, LocalTime::parse),
					Map.entry(LocalDateTime.class, LocalDateTime::parse),
					Map.entry(Instant.class, Instant::parse),
					Map.entry(OffsetDateTime.class, OffsetDateTime::parse),
					Map.entry(OffsetTime.class, OffsetTime::parse),
					Map.entry(ZonedDateTime.class, ZonedDateTime::parse),
					Map.entry(Date.class, text -> Date.from(Instant.parse(text)))
			);

	private static Object convertValue(Object value, Class<?> targetType) {
		if (value == null) {
			return null;
		}

		Class<?> type = wrapPrimitive(targetType);
		if (type.isInstance(value)) {
			return value;
		}

		String text = String.valueOf(value).trim();

		try {
			return convertText(text, type);
		} catch (RuntimeException ex) {
			throw new ValidationException(
					ErrorCode.MESSAGE_CANNOT_CONVERT_VALUE.formatted(
							text, type.getSimpleName()
					),
					ex
			);
		}
	}

	private static Object convertText(String text, Class<?> type) {
		Function<String, ?> converter = VALUE_CONVERTERS.get(type);
		if (converter != null) {
			return converter.apply(text);
		}

		if (type.isEnum()) {
			return parseEnum(text, type);
		}

		throw new IllegalArgumentException(
				ErrorCode.MESSAGE_UNSUPPORTED_SEARCH_VALUE_TYPE + type.getSimpleName()
		);
	}

	@SuppressWarnings({"rawtypes"})
	private static Object parseEnum(String text, Class<?> type) {
		return Enum.valueOf((Class<? extends Enum>) type, text);
	}

	private static Boolean parseBoolean(String text) {
		if (!text.equalsIgnoreCase("true")
				&& !text.equalsIgnoreCase("false")) {
			throw new ValidationException(
					ErrorCode.MESSAGE_INVALID_BOOLEAN_VALUE + text
			);
		}

		return Boolean.valueOf(text);
	}

	private static Character parseCharacter(String text) {
		if (text.length() != 1) {
			throw new ValidationException(
					ErrorCode.MESSAGE_EXPECTED_A_SINGLE_CHARACTER
			);
		}

		return text.charAt(0);
	}

	private static Class<?> wrapPrimitive(Class<?> type) {
		if (!type.isPrimitive()) return type;
		if (type == long.class) return Long.class;
		if (type == int.class) return Integer.class;
		if (type == short.class) return Short.class;
		if (type == byte.class) return Byte.class;
		if (type == double.class) return Double.class;
		if (type == float.class) return Float.class;
		if (type == boolean.class) return Boolean.class;
		if (type == char.class) return Character.class;
		return type;
	}

	private static Path<?> resolvePath(
			Root<?> root,
			String fieldPath,
			jakarta.persistence.criteria.CriteriaQuery<?> query
	) {
		String[] segments = fieldPath.split("\\.");
		Path<?> current = root;

		for (String segment : segments) {
			Field field = ReflectionUtils.findField(
					current.getJavaType(),
					segment
			);

			if (field != null
					&& Collection.class.isAssignableFrom(field.getType())) {
				if (!(current instanceof From<?, ?> from)) {
					throw new IllegalArgumentException(
							ErrorCode.MESSAGE_CANNOT_JOIN_COLLECTION_AT + segment
					);
				}

				current = from.join(
						segment,
						JoinType.LEFT
				);
				query.distinct(true);
			} else {
				current = current.get(segment);
			}
		}

		return current;
	}

	public static <T> Sort toSort(
			Class<T> entityClass,
			List<SortField> sortFields,
			Map<String, FieldRule> rules
	) {
		if (sortFields == null || sortFields.isEmpty()) {
			return Sort.unsorted();
		}

		List<Sort.Order> orders = new ArrayList<>();

		for (SortField sortField : sortFields) {
			FieldRule rule = rules.get(sortField.field());

			if (rule == null || !rule.sortable()) {
				throw new ValidationException(
						ErrorCode.MESSAGE_UNSUPPORTED_SORT_FIELD + sortField.field()
				);
			}

			validatePath(entityClass, sortField.field());

			if (containsCollection(entityClass, sortField.field())) {
				throw new ValidationException(
						ErrorCode.MESSAGE_SORTING_BY_COLLECTION_PATH_IS_NOT_SUPPORTED
								+ sortField.field()
				);
			}

			orders.add(new Sort.Order(
					sortField.direction(),
					sortField.field()
			));
		}

		return Sort.by(orders);
	}

	private static boolean containsCollection(
			Class<?> entityClass,
			String fieldPath
	) {
		Class<?> currentType = entityClass;

		for (String segment : fieldPath.split("\\.")) {
			Field field = ReflectionUtils.findField(
					currentType,
					segment
			);

			if (field == null) {
				throw new ValidationException(
						ErrorCode.MESSAGE_UNSUPPORTED_SEARCH_FIELD + fieldPath
				);
			}

			if (Collection.class.isAssignableFrom(field.getType())) {
				return true;
			}

			currentType = field.getType();
		}

		return false;
	}

	private static void validateConfiguration(
			Class<?> entityClass,
			Map<String, FieldRule> rules
	) {
		if (rules == null || rules.isEmpty()) {
			throw new IllegalArgumentException(
					ErrorCode.MESSAGE_SEARCH_FIELD_RULES_MUST_NOT_BE_EMPTY
			);
		}

		rules.forEach((path, rule) -> {
			if (path == null || path.isBlank() || rule == null) {
				throw new IllegalArgumentException(
						ErrorCode.MESSAGE_INVALID_SEARCH_FIELD_CONFIGURATION
				);
			}

			validatePath(entityClass, path);
		});
	}

	private static Class<?> validatePath(
			Class<?> entityClass,
			String fieldPath
	) {
		String[] segments = fieldPath.split("\\.");
		Class<?> currentType = entityClass;

		for (int i = 0; i < segments.length; i++) {
			String segment = segments[i];
			validateSegmentName(segment, fieldPath);

			Field field = findSupportedField(currentType, segment, fieldPath);
			currentType = resolveFieldType(field, fieldPath);

			validateNavigation(currentType, i, segments.length, fieldPath);
		}

		return currentType;
	}

	private static void validateSegmentName(String segment, String fieldPath) {
		if (segment.isBlank()) {
			throw new IllegalArgumentException(
					ErrorCode.MESSAGE_INVALID_FIELD_PATH + fieldPath
			);
		}
	}

	private static Field findSupportedField(
			Class<?> currentType,
			String segment,
			String fieldPath
	) {
		Field field = ReflectionUtils.findField(currentType, segment);

		if (field == null || field.isAnnotationPresent(Transient.class)) {
			throw new ValidationException(
					ErrorCode.MESSAGE_UNSUPPORTED_SEARCH_FIELD + fieldPath
			);
		}

		return field;
	}

	private static Class<?> resolveFieldType(Field field, String fieldPath) {
		if (!Collection.class.isAssignableFrom(field.getType())) {
			return field.getType();
		}

		Type genericType = field.getGenericType();
		if (!(genericType instanceof ParameterizedType parameterized)) {
			throw new IllegalArgumentException(
					ErrorCode.MESSAGE_CANNOT_RESOLVE_COLLECTION + fieldPath
			);
		}

		return toClass(parameterized.getActualTypeArguments()[0]);
	}

	private static void validateNavigation(
			Class<?> currentType,
			int index,
			int segmentCount,
			String fieldPath
	) {
		boolean hasNextSegment = index < segmentCount - 1;
		if (hasNextSegment && isScalar(currentType)) {
			throw new IllegalArgumentException(
					ErrorCode.MESSAGE_CANNOT_NAVIGATE_THROUGH_SCALAR_FIELD + fieldPath
			);
		}
	}

	private static Class<?> toClass(Type type) {
		if (type instanceof Class<?> clazz) {
			return clazz;
		}

		if (type instanceof ParameterizedType parameterized
				&& parameterized.getRawType() instanceof Class<?> clazz) {
			return clazz;
		}

		throw new IllegalArgumentException(
				ErrorCode.MESSAGE_UNSUPPORTED_GENERIC_FIELD_TYPE + type
		);
	}

	private static boolean isScalar(Class<?> type) {
		return type.isPrimitive()
				|| type.isEnum()
				|| type == String.class
				|| Number.class.isAssignableFrom(type)
				|| type == Boolean.class
				|| type == Character.class
				|| type == UUID.class
				|| type == LocalDate.class
				|| type == LocalTime.class
				|| type == LocalDateTime.class
				|| type == Instant.class
				|| type == OffsetDateTime.class
				|| type == OffsetTime.class
				|| type == ZonedDateTime.class
				|| Date.class.isAssignableFrom(type);
	}


}

