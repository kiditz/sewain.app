package app.sewain.shared;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class UserAuthentication {
	private UserAuthentication() {
	}
	public static UUID getUserIdFromAuthentication(Authentication authentication) {
		Jwt jwt = (Jwt) authentication.getPrincipal();
		return UUID.fromString(
				requireNonNull(requireNonNull(jwt).getClaimAsString("user"))
		);
	}
}
