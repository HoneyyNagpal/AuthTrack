package com.authtrack.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.authtrack.dto.ApiResponse;
import com.authtrack.dto.RoleUpdateRequest;
import com.authtrack.dto.UserResponse;
import com.authtrack.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // admin only: view all users
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // admin or moderator: view a specific user
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // any authenticated user: view own profile by username
    @GetMapping("/profile/{username}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getProfile(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    // admin only: enable or disable a user account
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserStatus(id));
    }

    // admin only: replace a user's roles
    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateRoles(@PathVariable Long id,
                                                    @Valid @RequestBody RoleUpdateRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(userService.updateUserRoles(id, request.getRoles(), authentication.getName()));
    }

    // admin only: permanently delete a user
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new ApiResponse(true, "User deleted successfully"));
    }
}