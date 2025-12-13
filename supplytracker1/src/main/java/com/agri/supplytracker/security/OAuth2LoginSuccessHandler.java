package com.agri.supplytracker.security;

import com.agri.supplytracker.model.User;
import com.agri.supplytracker.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        
        // Find or create user
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername(email.split("@")[0]); // Use email prefix as username
            newUser.setEmail(email);
            newUser.setPassword(""); // OAuth users don't have password
            newUser.setRoles(Set.of("ROLE_USER")); // Default role
            return userRepository.save(newUser);
        });

        // Generate JWT token
        String jwt = jwtUtil.generateToken(user.getUsername());
        
        // Redirect to frontend main page with token and roles
        String roles = String.join(",", user.getRoles());
        String redirectUrl = String.format("http://localhost:5173/?token=%s&username=%s&roles=%s",
                jwt, user.getUsername(), roles);
        
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
