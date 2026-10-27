package app.sewain.merchants;

import java.util.UUID;

interface MerchantCommandService {
	MerchantResponse addMerchant(UUID userId, MerchantCommand command);

	MerchantResponse updateMerchant(UUID userId, UUID merchantId, MerchantCommand command);

}
