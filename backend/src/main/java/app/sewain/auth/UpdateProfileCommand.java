package app.sewain.auth;

import app.sewain.shared.ValidPhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record UpdateProfileCommand(
		@NotBlank
		String fullName,

		@NotBlank
		@Email
		String email,

		@NotBlank
		@ValidPhoneNumber
		String phone
) {
}