package com.agri.supplytracker.security;

import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthenticationInterceptor implements ChannelInterceptor {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService users;
    public WebSocketAuthenticationInterceptor(JwtUtil jwtUtil, CustomUserDetailsService users) { this.jwtUtil=jwtUtil; this.users=users; }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor=StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token=accessor.getFirstNativeHeader("Authorization");
            if(token==null) token=accessor.getFirstNativeHeader("token");
            if(token!=null && token.startsWith("Bearer ")) token=token.substring(7);
            if(token==null || token.isBlank()) throw new MessagingException("Authentication token required");
            String username=jwtUtil.extractUsername(token);
            UserDetails details=users.loadUserByUsername(username);
            if(!jwtUtil.validateToken(token,details)) throw new MessagingException("Invalid authentication token");
            accessor.setUser(new UsernamePasswordAuthenticationToken(details,null,details.getAuthorities()));
        }
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) && !"/user/queue/notifications".equals(accessor.getDestination())) {
            throw new MessagingException("Subscription destination is not allowed");
        }
        return message;
    }
}
