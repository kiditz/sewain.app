package app.sewain.catalogs;

enum CollectionRuleOperator {
	EQUALS,
	NOT_EQUALS,
	CONTAINS,
	NOT_CONTAINS,
	STARTS_WITH,
	ENDS_WITH;

	boolean isNegative() {
		return this == NOT_EQUALS || this == NOT_CONTAINS;
	}

	/**
	 * Operator positif untuk operator negatif (NOT_EQUALS -> EQUALS).
	 * Rule negatif dievaluasi sebagai NOT(salah satu value cocok).
	 */
	CollectionRuleOperator positive() {
		return switch (this) {
			case NOT_EQUALS -> EQUALS;
			case NOT_CONTAINS -> CONTAINS;
			default -> this;
		};
	}
}
