package com.agri.supplytracker.graphql;

import com.agri.supplytracker.model.User;
import com.agri.supplytracker.repository.UserRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class UserGraphQLController {

    private final UserRepository userRepository;

    public UserGraphQLController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @QueryMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<User> users() {
        return userRepository.findAll();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public User user(@Argument String id) {
        return userRepository.findById(id).orElse(null);
    }
}
