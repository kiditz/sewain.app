package app.sewain.config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProvisioningWebSocketConfigurationTest {
    private final JwtDecoder decoder = mock(JwtDecoder.class);
    private final ProvisioningWebSocketConfiguration configuration = new ProvisioningWebSocketConfiguration(decoder, "http://localhost:3000");
    private Message<byte[]> message(StompHeaderAccessor header) {
        header.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], header.getMessageHeaders());
    }
    private Jwt token() {
        return Jwt.withTokenValue("token").header("alg", "RS256").claim("user", UUID.randomUUID().toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    }
    @Test void connectRequiresBearerAndUsesVerifiedUserClaim() {
        var missing = StompHeaderAccessor.create(StompCommand.CONNECT);
        assertThrows(AccessDeniedException.class, () -> configuration.provisioningInterceptor().preSend(message(missing), null));
        var jwt = token(); when(decoder.decode("token")).thenReturn(jwt);
        var authenticated = StompHeaderAccessor.create(StompCommand.CONNECT);
        authenticated.setNativeHeader("Authorization", "Bearer token");
        configuration.provisioningInterceptor().preSend(message(authenticated), null);
        assertEquals(jwt.getClaimAsString("user"), authenticated.getUser().getName());
    }
    @Test void cannotSubscribeToAnotherUsersOrBroadcastDestination() {
        var jwt = token();
        for (var destination : List.of("/queue/provisioning", "/topic/provisioning", "/user/another-user/queue/provisioning")) {
            var header = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            header.setUser(new JwtAuthenticationToken(jwt, List.of(), jwt.getClaimAsString("user")));
            header.setDestination(destination);
            assertThrows(AccessDeniedException.class, () -> configuration.provisioningInterceptor().preSend(message(header), null));
        }
    }
}
