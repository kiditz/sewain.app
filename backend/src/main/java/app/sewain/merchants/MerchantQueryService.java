package app.sewain.merchants;

import java.util.List;
import java.util.UUID;

interface MerchantQueryService {
	List<MerchantResponse> findByUser(UUID userId);
	void validateAccess(UUID userId, String tenant);
}
