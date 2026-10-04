package com.authtrack.config;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.authtrack.entity.Role;
import com.authtrack.entity.User;
import com.authtrack.exception.RoleNotFoundException;
import com.authtrack.repository.RoleRepository;
import com.authtrack.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final int MIN_ADMIN_PASSWORD_LENGTH = 8;

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.bootstrap.admin.username:}") String adminUsername,
                           @Value("${app.bootstrap.admin.email:}") String adminEmail,
                           @Value("${app.bootstrap.admin.password:}") String adminPassword) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        seedRole(Role.RoleName.ROLE_USER);
        seedRole(Role.RoleName.ROLE_MODERATOR);
        seedRole(Role.RoleName.ROLE_ADMIN);
        seedBootstrapAdmin();
    }

    private void seedRole(Role.RoleName roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            roleRepository.save(new Role(roleName));
            log.info("Seeded role: {}", roleName);
        }
    }

    private void seedBootstrapAdmin() {
        if (adminUsername.isBlank() || adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("Bootstrap admin not configured, skipping");
            return;
        }
        if (adminPassword.length() < MIN_ADMIN_PASSWORD_LENGTH) {
            log.warn("Bootstrap admin password is shorter than {} characters, skipping", MIN_ADMIN_PASSWORD_LENGTH);
            return;
        }
        if (userRepository.existsByUsername(adminUsername)) {
            log.info("Bootstrap admin already exists: username={}", adminUsername);
            return;
        }

        Role adminRole = roleRepository.findByName(Role.RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new RoleNotFoundException("ROLE_ADMIN not found. Check DB seeding."));
        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new RoleNotFoundException("ROLE_USER not found. Check DB seeding."));

        User admin = new User(adminUsername, adminEmail, passwordEncoder.encode(adminPassword));
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        roles.add(userRole);
        admin.setRoles(roles);
        userRepository.save(admin);

        log.info("Bootstrap admin created: username={}", adminUsername);
    }
}