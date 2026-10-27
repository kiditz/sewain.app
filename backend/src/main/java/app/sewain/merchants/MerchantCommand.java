package app.sewain.merchants;

import app.sewain.shared.Slug;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

record MerchantCommand(
		@NotBlank
		String name,
		@NotBlank
		@Slug
		String slug,
		@Valid
		StoreInformationCommand storeInformation,

		@Valid
		BusinessInformationCommand businessInformation,

		String customDomain,
		Boolean published
) {
}

record StoreInformationCommand(
		String description,
		String logoUrl,
		String faviconUrl,
		String contactEmail,
		String contactPhone
) {
}

record BusinessInformationCommand(
		String name,
		String address,
		String email,
		String phone,
		String taxId
) {
}