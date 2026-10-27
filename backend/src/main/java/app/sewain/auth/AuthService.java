package app.sewain.auth;

interface AuthService {
	UserResponse register(RegisterRequest request);

	LoginResponse login(LoginRequest request);
}
