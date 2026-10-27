package app.sewain.auth;

import app.sewain.shared.UserAuthentication;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile")
class ProfileCommandController {

	private final ProfileCommandService profileCommandService;

	@PutMapping("/password")
	void changePassword(
			Authentication authentication,
			@Valid @RequestBody ChangePasswordCommand command
	) {
		UUID userId = UserAuthentication.getUserIdFromAuthentication(authentication);
		profileCommandService.changePassword(userId, command);
	}

	@PutMapping
	UserResponse updateProfile(
			Authentication authentication,
			@Valid @RequestBody UpdateProfileCommand command
	) {
		UUID userId = UserAuthentication.getUserIdFromAuthentication(authentication);
		return profileCommandService.updateProfile(userId, command);
	}
}