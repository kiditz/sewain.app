package app.sewain.catalogs;

import java.util.List;
import java.util.UUID;

record CollectionRuleResponse(
		UUID id,
		CollectionRuleAttribute attribute,
		CollectionRuleOperator operator,
		List<String> values
) {
}
