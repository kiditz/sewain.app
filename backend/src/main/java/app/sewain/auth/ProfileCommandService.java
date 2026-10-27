package app.sewain.auth;


import java.util.UUID;

interface ProfileCommandService {
	void changePassword(
			UUID userId,
			ChangePasswordCommand command
	);
	UserResponse updateProfile(
			UUID userId,
			UpdateProfileCommand command
	);
}
