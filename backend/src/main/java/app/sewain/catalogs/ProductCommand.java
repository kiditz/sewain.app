package app.sewain.catalogs;

import app.sewain.shared.Slug;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

record ProductCommand(

		@NotBlank
		@Size(max = 255)
		String name,

		@NotBlank
		@Size(max = 255)
		@Slug
		String slug,
		@Size(max = 500)
		String media,
		String description,

		ProductStatus status,

		UUID productTypeId,
        List<@NotNull UUID> tagIds,
		List<@Valid ProductVariantCommand> variants

) {
	public ProductCommand {
		variants = variants == null ? List.of() : variants;
	}
}