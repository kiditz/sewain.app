package app.sewain.catalogs;

import app.sewain.shared.Slug;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

record AttributeValueCommand(

		UUID id,

		@NotBlank
		@Size(max = 100)
		String value,

		@NotBlank
		@Size(max = 100)
		@Slug
		String slug
) {
}