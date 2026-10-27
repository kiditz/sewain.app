package app.sewain.auth;

import app.sewain.exception.ConflictException;

import app.sewain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Service
@RequiredArgsConstructor
class AuthServiceImpl implements AuthService {
	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final RegisterMapper registerMapper;
	private final JwtEncoder jwtEncoder;

	@Override
	@Transactional
	public UserResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new ConflictException(ErrorCode.EMAIL_EXISTS);
		}

		User user = registerMapper.toEntity(request);
		user.setPassword(passwordEncoder.encode(request.password()));
		return registerMapper.toResponse(userRepository.save(user));
	}

	@Override
	public LoginResponse login(LoginRequest request) {
		Authentication authentication = authenticationManager.
				authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
		CustomUserDetails userDetails =
				(CustomUserDetails) authentication.getPrincipal();

		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.claim("user", Objects.requireNonNull(userDetails).getId().toString())
				.subject(authentication.getName())
				.issuedAt(now)
				.expiresAt(now.plus(1, ChronoUnit.DAYS))
				.build();
		String token = jwtEncoder
				.encode(JwtEncoderParameters.from(claims))
				.getTokenValue();
		return new LoginResponse(token);
	}
}
