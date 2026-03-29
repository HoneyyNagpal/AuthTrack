package com.authtrack.config;

import com.authtrack.entity.Role;
import com.authtrack.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        seedRole(Role.RoleName.ROLE_USER);
        seedRole(Role.RoleName.ROLE_MODERATOR);
        seedRole(Role.RoleName.ROLE_ADMIN);
    }

    private void seedRole(Role.RoleName roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            roleRepository.save(new Role(roleName));
        }
    }
}
