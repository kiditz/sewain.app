package app.sewain.merchants;

import java.util.UUID;

record MerchantResponse(
		UUID id,
		String name,
		String schema,
		String slug,
		StoreInformationResponse storeInformation,
		BusinessInformationResponse businessInformation,
		String customDomain,
		String provisioningStatus,
		long provisioningVersion,
		boolean published
) {
}

record StoreInformationResponse(
		String description,
		String logoUrl,
		String faviconUrl,
		String contactEmail,
		String contactPhone
) {
}

record BusinessInformationResponse(
		String name,
		String address,
		String email,
		String phone,
		String taxId
) {
}