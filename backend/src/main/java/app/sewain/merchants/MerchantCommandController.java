package app.sewain.merchants;

import app.sewain.shared.UserAuthentication;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
@Tag(name = "Merchant")
class MerchantCommandController {
	private final MerchantCommandService commandService;


	@PostMapping
	MerchantResponse addMerchant(Authentication authentication, @Valid @RequestBody MerchantCommand command) {
		UUID userId = UserAuthentication.getUserIdFromAuthentication(authentication);
		return commandService.addMerchant(userId, command);
	}

	@PutMapping("{id}")
	MerchantResponse updateMerchant(@PathVariable UUID id, Authentication authentication, @Valid @RequestBody MerchantCommand command) {
		UUID userId = UserAuthentication.getUserIdFromAuthentication(authentication);
		return commandService.updateMerchant(userId, id, command);
	}


}
