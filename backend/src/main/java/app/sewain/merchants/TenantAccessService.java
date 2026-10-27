package app.sewain.merchants;

import app.sewain.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantAccessService {

	private final MerchantUserRepository merchantUserRepository;
	private final MerchantRepository merchantRepository;

	public void validateAccess(UUID userId, String tenant) {
		if ("public".equals(tenant)) {
			return;
		}

		boolean allowed =
				merchantUserRepository.existsBySchemaAndUserId(tenant, userId);

		if (!allowed) {
			throw new AccessDeniedException(ErrorCode.MERCHANT_ACCESS_DENIED);
		}
	}

	public String getSchema(String slug) {
		return merchantRepository.findBySlug(slug)
				.map(Merchant::getSchema).orElse("public");

	}
}
