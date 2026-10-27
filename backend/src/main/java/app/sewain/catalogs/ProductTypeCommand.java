package app.sewain.catalogs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record ProductTypeCommand(
		@NotBlank
		@Size(max = 100)
		String name
) {
}