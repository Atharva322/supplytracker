package com.agri.supplytracker.controller;

import com.agri.supplytracker.dto.RegisterRequest;
import com.agri.supplytracker.model.User;
import com.agri.supplytracker.repository.UserRepository;
import com.agri.supplytracker.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerSecurityTest {
    @Test
    void publicRegistrationAlwaysCreatesLowPrivilegeUser() {
        UserRepository users=mock(UserRepository.class); PasswordEncoder encoder=mock(PasswordEncoder.class);
        UserDetailsService details=mock(UserDetailsService.class); JwtUtil jwt=mock(JwtUtil.class);
        AuthController controller=new AuthController();
        ReflectionTestUtils.setField(controller,"userRepository",users); ReflectionTestUtils.setField(controller,"passwordEncoder",encoder);
        ReflectionTestUtils.setField(controller,"userDetailsService",details); ReflectionTestUtils.setField(controller,"jwtUtil",jwt);
        when(encoder.encode(any())).thenReturn("encoded"); when(users.save(any())).thenAnswer(i->i.getArgument(0));
        var principal=org.springframework.security.core.userdetails.User.withUsername("alice").password("encoded").roles("USER").build();
        when(details.loadUserByUsername("alice")).thenReturn(principal); when(jwt.generateToken(principal)).thenReturn("token");
        RegisterRequest request=new RegisterRequest(); request.setUsername("alice"); request.setEmail("alice@example.com"); request.setPassword("long-password-123");
        controller.register(request);
        ArgumentCaptor<User> saved=ArgumentCaptor.forClass(User.class); verify(users).save(saved.capture());
        assertEquals(Set.of("ROLE_USER"),saved.getValue().getRoles());
    }
}
