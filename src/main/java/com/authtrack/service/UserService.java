package com.authtrack.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.authtrack.dto.UserResponse;
import com.authtrack.entity.Role;
import com.authtrack.entity.User;
import com.authtrack.exception.BadRequestException;
import com.authtrack.exception.ResourceNotFoundException;
import com.authtrack.exception.RoleNotFoundException;
import com.authtrack.repository.RoleRepository;
import com.authtrack.repository.UserRepository;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToResponse(user);
    }

    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setActive(!user.isActive());
        userRepository.save(user);
        log.info("User status toggled: userId={}, active={}", id, user.isActive());
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateUserRoles(Long id, Set<String> requestedRoles, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (user.getUsername().equals(currentUsername)) {
            log.warn("Admin tried to change own roles: username={}", currentUsername);
            throw new BadRequestException("Admins cannot change their own roles");
        }

        Set<Role> newRoles = new HashSet<>();
        for (String requested : requestedRoles) {
            newRoles.add(findRole(toRoleName(requested)));
        }

        user.setRoles(newRoles);
        userRepository.save(user);

        log.info("Roles updated: targetUserId={}, newRoles={}, changedBy={}",
                id, newRoles.stream().map(r -> r.getName().name()).collect(Collectors.toSet()), currentUsername);

        return mapToResponse(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        userRepository.delete(user);
        log.info("User deleted: userId={}", id);
    }

    private Role.RoleName toRoleName(String raw) {
        return switch (raw.trim().toLowerCase()) {
            case "user" -> Role.RoleName.ROLE_USER;
            case "moderator", "mod" -> Role.RoleName.ROLE_MODERATOR;
            case "admin" -> Role.RoleName.ROLE_ADMIN;
            default -> throw new BadRequestException("Unknown role: " + raw);
        };
    }

    private Role findRole(Role.RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new RoleNotFoundException(roleName + " not found. Check DB seeding."));
    }

    private UserResponse mapToResponse(User user) {
        var roleNames = user.getRoles()
                .stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isActive(),
                roleNames,
                user.getCreatedAt()
        );
    }
}