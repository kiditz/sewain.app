package app.sewain.merchants;

import app.sewain.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class MerchantQueryServiceImpl implements MerchantQueryService {

	private final MerchantRepository merchantRepository;
	private final MerchantUserRepository merchantUserRepository;
	private final MerchantMapper mapper;

	@Override
	@Transactional(readOnly = true)
	@Cacheable(
			cacheNames = "userMerchants",
			key = "#userId"
	)
	public List<MerchantResponse> findByUser(UUID userId) {
		return mapper.toResponses(merchantRepository.findByUserId(userId));
	}

	@Override
	@Cacheable(
			cacheNames = "merchantAccess",
			key = "#userId + ':' + #tenant"
	)
	public void validateAccess(UUID userId, String tenant) {
		if ("public".equals(tenant)) {
			return;
		}
		boolean allowed = merchantUserRepository.existsBySchemaAndUserId(tenant, userId);

		if (!allowed) {
			throw new AccessDeniedException(ErrorCode.MERCHANT_ACCESS_DENIED);
		}

	}
}
