package app.sewain.catalogs;

import app.sewain.shared.Slug;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

record AttributeCommand(
		@NotBlank
		@Size(max = 100)
		String name,

		@NotBlank
		@Size(max = 100)
		@Slug
		String slug,

		String description,

		List<@Valid AttributeValueCommand> values
) {
	public AttributeCommand {
		values = values == null ? List.of() : values;
	}
}