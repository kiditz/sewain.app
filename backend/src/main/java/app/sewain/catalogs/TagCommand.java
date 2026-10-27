package app.sewain.catalogs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record TagCommand(
		@NotBlank
		@Size(max = 100)
		String name
) {
    public TagCommand { name = name == null ? null : name.trim(); }
}