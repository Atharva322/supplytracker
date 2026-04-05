package com.agri.supplytracker.config;

import com.agri.supplytracker.model.User;
import com.agri.supplytracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createAdminIfAbsent();
    }

    private void createAdminIfAbsent() {
        userRepository.findByUsername("admin").ifPresentOrElse(existing -> {
            if (!existing.getRoles().contains("ROLE_ADMIN")) {
                existing.getRoles().addAll(Set.of("ROLE_ADMIN", "ROLE_USER"));
                existing.setStageProfile("ADMIN");
                existing.setLocation("HQ");
                userRepository.save(existing);
                logger.info("✅ Admin user updated with ROLE_ADMIN.");
            } else {
                logger.info("Admin user already exists with correct roles — skipping seed.");
            }
        }, () -> {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@supplytracker.local");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRoles(Set.of("ROLE_ADMIN", "ROLE_USER"));
            admin.setStageProfile("ADMIN");
            admin.setLocation("HQ");
            userRepository.save(admin);
            logger.info("✅ Permanent admin user created: username=admin");
        });
    }
}
