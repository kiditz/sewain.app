package app.sewain.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.config.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Configuration
@EnableWebSocketMessageBroker
public class ProvisioningWebSocketConfiguration implements WebSocketMessageBrokerConfigurer {
    private final JwtDecoder decoder;
    private final String[] origins;
    public ProvisioningWebSocketConfiguration(JwtDecoder decoder,
            @Value("${app.websocket.allowed-origin-patterns}") String origins) {
        this.decoder = decoder; this.origins = java.util.Arrays.stream(origins.split(",")).map(String::trim).filter(origin -> !origin.isEmpty()).toArray(String[]::new);
    }
    @Bean public ThreadPoolTaskScheduler provisioningHeartbeatScheduler() {
        var scheduler = new ThreadPoolTaskScheduler(); scheduler.setPoolSize(1); scheduler.setThreadNamePrefix("stomp-heartbeat-"); return scheduler;
    }
    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.setPreserveReceiveOrder(true);
        registry.addEndpoint("/ws/provisioning").setAllowedOriginPatterns(origins);
    }
    @Override public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app"); registry.setPreservePublishOrder(true);
        registry.enableSimpleBroker("/queue").setHeartbeatValue(new long[]{10000,10000}).setTaskScheduler(provisioningHeartbeatScheduler());
    }
    @Override public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(8192).setSendBufferSizeLimit(65536).setSendTimeLimit(15000).setTimeToFirstMessage(15000);
    }
    @Override public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(provisioningInterceptor());
    }
    ChannelInterceptor provisioningInterceptor() {
        return new ChannelInterceptor() {
            @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
                var header = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (header == null) throw new AccessDeniedException("Invalid STOMP frame");
                if (header.getCommand() == StompCommand.CONNECT) {
                    var authorization = header.getFirstNativeHeader("Authorization");
                    if (authorization == null || !authorization.startsWith("Bearer ")) throw new AccessDeniedException("Authentication required");
                    var jwt = decoder.decode(authorization.substring(7));
                    var userId = UUID.fromString(jwt.getClaimAsString("user"));
                    header.setUser(new JwtAuthenticationToken(jwt, List.of(), userId.toString()));
                    return message;
                }
                if (!(header.getUser() instanceof JwtAuthenticationToken user) || !user.isAuthenticated() ||
                        user.getToken().getExpiresAt() == null || !user.getToken().getExpiresAt().isAfter(Instant.now()))
                    throw new AccessDeniedException("Authentication expired");
                if (header.getCommand() == StompCommand.SUBSCRIBE && !Set.of("/user/queue/provisioning", "/user/queue/product-imports").contains(header.getDestination()))
                    throw new AccessDeniedException("Subscription denied");
                if (header.getCommand() == StompCommand.SEND && (header.getDestination() == null || !Set.of("/app/provisioning/status", "/app/provisioning/retry", "/app/product-imports/status").contains(header.getDestination())))
                    throw new AccessDeniedException("Destination denied");
                return message;
            }
        };
    }
}
