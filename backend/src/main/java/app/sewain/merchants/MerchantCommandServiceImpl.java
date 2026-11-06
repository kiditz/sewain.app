package app.sewain.merchants;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class MerchantCommandServiceImpl implements MerchantCommandService {
	private final MerchantRepository merchantRepository;
	private final MerchantUserRepository merchantUserRepository;
	private final MerchantMapper mapper;
	private final ApplicationEventPublisher publisher;

	@Transactional
	@Override
	@CacheEvict(
			cacheNames = "userMerchants",
			key = "#userId"
	)
	public MerchantResponse addMerchant(UUID userId, MerchantCommand command) {
		var merchant = mapper.toEntity(command);
		merchant.setActive(true);
		merchant.setUserId(userId);
		merchant.setSchema(command.slug().replace("-", "_"));
		merchantRepository.save(merchant);
		var merchantUser = new MerchantUser(new MerchantUserId(merchant.getId(), userId), MerchantRole.OWNER);
		merchantUserRepository.saveAndFlush(merchantUser);
		publisher.publishEvent(new MerchantCreatedEvent(merchant.getName(), merchant.getSchema()));
		return mapper.toResponse(merchant);
	}

	@Override
	@Transactional
	@CacheEvict(
			cacheNames = "userMerchants",
			key = "#userId"
	)
	public MerchantResponse updateMerchant(UUID userId, UUID merchantId, MerchantCommand command) {
		Merchant merchant = merchantRepository.findById(merchantId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MERCHANT_NOT_FOUND));
        if (!userId.equals(merchant.getUserId())) throw new org.springframework.security.access.AccessDeniedException(ErrorCode.MERCHANT_ACCESS_DENIED);
		mapper.update(command, merchant);
		return mapper.toResponse(merchant);
	}

}
