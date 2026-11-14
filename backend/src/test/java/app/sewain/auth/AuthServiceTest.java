package app.sewain.auth;

import app.sewain.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AuthServiceTest {
    final AuthenticationManager authentication = mock(AuthenticationManager.class);
    final UserRepository users = mock(UserRepository.class);
    final PasswordEncoder passwords = mock(PasswordEncoder.class);
    final JwtEncoder encoder = mock(JwtEncoder.class);
    final AuthServiceImpl service = new AuthServiceImpl(authentication, users, passwords,
            Mappers.getMapper(RegisterMapper.class), encoder);

    @Test void registerStoresEncodedPasswordAndReturnsPublicIdentity() {
        when(passwords.encode("secret")).thenReturn("encoded");
        var id = UUID.randomUUID();
        when(users.save(any(User.class))).thenAnswer(call -> {
            User user = call.getArgument(0);
            assertEquals("encoded", user.getPassword());
            user.setId(id); return user;
        });
        var result = service.register(new RegisterRequest("owner@example.invalid", "Owner", "secret"));
        assertEquals(id, result.id());
        assertEquals("owner@example.invalid", result.email());
        assertEquals("Owner", result.fullName());
    }

    @Test void duplicateEmailDoesNotEncodeOrPersist() {
        when(users.existsByEmail("taken@example.invalid")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.register(new RegisterRequest("taken@example.invalid", "Owner", "secret")));
        verifyNoInteractions(passwords, encoder);
        verify(users, never()).save(any());
    }

    @Test void successfulLoginSignsUserIdSubjectAndOneDayExpiry() {
        var user = new User(); user.setId(UUID.randomUUID()); user.setEmail("owner@example.invalid"); user.setEnabled(true);
        var principal = new CustomUserDetails(user);
        when(authentication.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        when(encoder.encode(any())).thenAnswer(call -> {
            JwtEncoderParameters parameters = call.getArgument(0);
            var claims = parameters.getClaims();
            assertEquals(user.getId().toString(), claims.getClaim("user"));
            assertEquals(user.getEmail(), claims.getSubject());
            assertEquals(Duration.ofDays(1), Duration.between(claims.getIssuedAt(), claims.getExpiresAt()));
            return Jwt.withTokenValue("signed-token").header("alg", "RS256").claims(c -> c.putAll(claims.getClaims())).build();
        });
        assertEquals("signed-token", service.login(new LoginRequest(user.getEmail(), "secret")).accessToken());
    }

    @Test void invalidCredentialsNeverIssueToken() {
        when(authentication.authenticate(any())).thenThrow(new BadCredentialsException("Invalid"));
        assertThrows(BadCredentialsException.class, () -> service.login(new LoginRequest("owner@example.invalid", "wrong")));
        verifyNoInteractions(encoder);
    }

    @Test void disabledAccountNeverIssuesToken() {
        when(authentication.authenticate(any())).thenThrow(new DisabledException("Disabled"));
        assertThrows(DisabledException.class, () -> service.login(new LoginRequest("owner@example.invalid", "secret")));
        verifyNoInteractions(encoder);
    }

    @Test void principalExposesStoredIdentityAndEnabledFlag() {
        var user = new User(); user.setId(UUID.randomUUID()); user.setEmail("user@example.invalid"); user.setPassword("hash"); user.setEnabled(false);
        var details = new CustomUserDetails(user);
        assertEquals(user.getId(), details.getId()); assertEquals(user.getEmail(), details.getUsername());
        assertEquals("hash", details.getPassword()); assertFalse(details.isEnabled());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        user.setEnabled(true); assertTrue(details.isEnabled());
    }
}
