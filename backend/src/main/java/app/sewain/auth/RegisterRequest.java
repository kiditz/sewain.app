package app.sewain.auth;

import jakarta.validation.constraints.NotBlank;

record RegisterRequest(
		@NotBlank
		String email,
		@NotBlank
		String fullName,
		@NotBlank
		String password
) {
}