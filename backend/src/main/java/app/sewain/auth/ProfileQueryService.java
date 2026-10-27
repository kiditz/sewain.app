package app.sewain.auth;

interface ProfileQueryService {
	UserResponse findProfile(String email);
}
