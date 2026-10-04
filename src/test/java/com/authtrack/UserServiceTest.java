package com.authtrack;

import com.authtrack.dto.UserResponse;
import com.authtrack.entity.Role;
import com.authtrack.entity.User;
import com.authtrack.exception.BadRequestException;
import com.authtrack.exception.ResourceNotFoundException;
import com.authtrack.repository.RoleRepository;
import com.authtrack.repository.UserRepository;
import com.authtrack.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private User buildUser(String username) {
        User user = new User(username, username + "@example.com", "hashed");
        user.setRoles(Set.of(new Role(Role.RoleName.ROLE_USER)));
        return user;
    }

    @Test
    void updateUserRoles_shouldAssignRequestedRoles() {
        User target = buildUser("bob");
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(roleRepository.findByName(Role.RoleName.ROLE_MODERATOR))
                .thenReturn(Optional.of(new Role(Role.RoleName.ROLE_MODERATOR)));

        UserResponse response = userService.updateUserRoles(2L, Set.of("moderator"), "alice");

        assertEquals(Set.of("ROLE_MODERATOR"), response.getRoles());
        verify(userRepository, times(1)).save(target);
    }

    @Test
    void updateUserRoles_shouldFail_whenAdminChangesOwnRoles() {
        User self = buildUser("alice");
        when(userRepository.findById(1L)).thenReturn(Optional.of(self));

        assertThrows(BadRequestException.class,
                () -> userService.updateUserRoles(1L, Set.of("user"), "alice"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRoles_shouldFail_whenRoleNameIsUnknown() {
        User target = buildUser("bob");
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThrows(BadRequestException.class,
                () -> userService.updateUserRoles(2L, Set.of("superuser"), "alice"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRoles_shouldFail_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUserRoles(99L, Set.of("user"), "alice"));
        verify(userRepository, never()).save(any());
    }
}