package com.agri.supplytracker.controller;

import com.agri.supplytracker.dto.AuthResponse;
import com.agri.supplytracker.dto.LoginRequest;
import com.agri.supplytracker.dto.RegisterRequest;
import com.agri.supplytracker.model.User;
import com.agri.supplytracker.repository.UserRepository;
import com.agri.supplytracker.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().body(new AuthResponse(null, null, null, "Invalid username or password"));
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);
        
        User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);
        Set<String> roles = user != null ? user.getRoles() : new HashSet<>();
        
        AuthResponse response = new AuthResponse(jwt, loginRequest.getUsername(), roles, "Login successful");
        if (user != null) {
            response.setStageProfile(user.getStageProfile());
            response.setLocation(user.getLocation());
            response.setAssociatedFarmId(user.getAssociatedFarmId());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        // Check if username exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new AuthResponse(null, null, null, "Username already exists"));
        }

        // Check if email exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new AuthResponse(null, null, null, "Email already exists"));
        }

        // Create new user
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        
        // Set roles - default to ROLE_USER unless specified
        Set<String> roles = new HashSet<>();
        if (registerRequest.getRoles() != null && !registerRequest.getRoles().isEmpty()) {
            roles.addAll(registerRequest.getRoles());
        } else {
            roles.add("ROLE_USER");
        }
        user.setRoles(roles);

        userRepository.save(user);

        // Auto-login after registration
        final UserDetails userDetails = userDetailsService.loadUserByUsername(registerRequest.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);
        
        AuthResponse response = new AuthResponse(jwt, registerRequest.getUsername(), roles, "Registration successful");
        response.setStageProfile(user.getStageProfile());
        response.setLocation(user.getLocation());
        response.setAssociatedFarmId(user.getAssociatedFarmId());

        return ResponseEntity.ok(response);
    }
}
