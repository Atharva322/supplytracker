package com.agri.supplytracker.config;

import com.agri.supplytracker.model.User;
import com.agri.supplytracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.enabled:false}")
    private boolean bootstrapAdminEnabled;

    @Value("${app.bootstrap-admin.username:admin}")
    private String bootstrapAdminUsername;

    @Value("${app.bootstrap-admin.email:admin@supplytracker.local}")
    private String bootstrapAdminEmail;

    @Value("${app.bootstrap-admin.password:}")
    private String bootstrapAdminPassword;

    @Override
    public void run(String... args) {
        if (bootstrapAdminEnabled) {
            if (bootstrapAdminPassword == null || bootstrapAdminPassword.length() < 12) {
                throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be at least 12 characters when admin bootstrap is enabled");
            }
            createAdminIfAbsent();
        }
    }

    private void createAdminIfAbsent() {
        userRepository.findByUsername(bootstrapAdminUsername).ifPresentOrElse(existing -> {
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
            admin.setUsername(bootstrapAdminUsername);
            admin.setEmail(bootstrapAdminEmail);
            admin.setPassword(passwordEncoder.encode(bootstrapAdminPassword));
            admin.setRoles(Set.of("ROLE_ADMIN", "ROLE_USER"));
            admin.setStageProfile("ADMIN");
            admin.setLocation("HQ");
            userRepository.save(admin);
            logger.info("Bootstrap admin user created: username={}", bootstrapAdminUsername);
        });
    }
}
