package app.sewain.auth;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
@Tag(name = "Profile")
class ProfileQueryController {
	private final ProfileQueryService userQueryService;

	@GetMapping
	UserResponse findAuthenticatedUser(Authentication authentication) {
		return userQueryService.findProfile(authentication.getName());
	}
}
