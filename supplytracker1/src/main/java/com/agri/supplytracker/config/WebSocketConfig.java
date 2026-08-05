package com.agri.supplytracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;
import com.agri.supplytracker.security.WebSocketAuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketAuthenticationInterceptor authenticationInterceptor;
    @Value("${cors.allowed.origins:http://localhost:5173,http://localhost:5174,http://localhost:3000}") private String allowedOrigins;
    public WebSocketConfig(WebSocketAuthenticationInterceptor authenticationInterceptor) { this.authenticationInterceptor=authenticationInterceptor; }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/queue", "/topic");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) { registration.interceptors(authenticationInterceptor); }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications")
                .setAllowedOrigins(java.util.Arrays.stream(allowedOrigins.split(",")).map(String::trim).toArray(String[]::new))
                .withSockJS();
    }
}
