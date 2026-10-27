package app.sewain.merchants;

import app.sewain.shared.UserAuthentication;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
@Tag(name = "Merchant")
class MerchantQueryController {
	private final MerchantQueryService merchantQueryService;

	@GetMapping
	List<MerchantResponse> findMerchants(Authentication authentication) {
		var userId = UserAuthentication.getUserIdFromAuthentication(authentication);
		return merchantQueryService.findByUser(userId);
	}
}
