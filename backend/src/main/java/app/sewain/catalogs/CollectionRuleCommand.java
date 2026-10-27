package app.sewain.catalogs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

record CollectionRuleCommand(
		@NotNull CollectionRuleAttribute attribute,
		@NotNull CollectionRuleOperator operator,
		@NotEmpty
		List<@NotBlank @Size(max = 255) String> values
) {
}
