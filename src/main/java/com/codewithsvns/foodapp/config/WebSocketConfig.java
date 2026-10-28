
package com.codewithsvns.foodapp.config;

import com.codewithsvns.foodapp.model.AccountStatus;
import com.codewithsvns.foodapp.model.UserProfile;
import com.codewithsvns.foodapp.model.UserRole;
import com.codewithsvns.foodapp.repository.UserProfileRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.UUID;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;
    private final UserProfileRepository userProfileRepository;

    public WebSocketConfig(
            JwtDecoder jwtDecoder,
            UserProfileRepository userProfileRepository) {
        this.jwtDecoder = jwtDecoder;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(
                "http://localhost:5173",
                "http://localhost:5174",
                "https://hungry-hour-food-order-and-delivery.vercel.app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(
                    Message<?> message,
                    MessageChannel channel) {

                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(
                                message, StompHeaderAccessor.class);

                if (accessor == null || accessor.getCommand() == null) {
                    throw new AccessDeniedException("Invalid STOMP message");
                }

                StompCommand command = accessor.getCommand();

                if (command == StompCommand.CONNECT) {
                    authenticateConnection(accessor);
                } else if (command == StompCommand.SUBSCRIBE) {
                    requireAuthenticated(accessor);

                    if (!"/user/queue/notifications".equals(
                            accessor.getDestination())) {
                        throw new AccessDeniedException(
                                "Subscription destination is not allowed");
                    }
                } else if (command == StompCommand.SEND) {
                    throw new AccessDeniedException(
                            "Client messages are not supported");
                } else if (command != StompCommand.DISCONNECT
                        && command != StompCommand.UNSUBSCRIBE) {
                    requireAuthenticated(accessor);
                }

                return message;
            }
        });
    }

    private void authenticateConnection(StompHeaderAccessor accessor) {
        String authorization =
                accessor.getFirstNativeHeader("Authorization");

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            throw new AccessDeniedException(
                    "A Bearer access token is required");
        }

        String token = authorization.substring(7).trim();

        if (token.isEmpty()) {
            throw new AccessDeniedException("Access token is empty");
        }

        final Jwt jwt;
        final UUID userId;

        try {
            jwt = jwtDecoder.decode(token);
            userId = UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new AccessDeniedException(
                    "Invalid or expired access token", exception);
        }

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "User profile not found"));

        if (profile.getRole() != UserRole.DELIVERY_PARTNER
                || profile.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "An active delivery partner account is required");
        }

        accessor.setUser(new JwtAuthenticationToken(jwt));
    }

    private void requireAuthenticated(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null
                || accessor.getUser().getName() == null) {
            throw new AccessDeniedException(
                    "Authentication is required");
        }
    }
}
