package com.authtrack;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.authtrack.dto.ApiResponse;
import com.authtrack.dto.AuthRequest;
import com.authtrack.entity.Role;
import com.authtrack.entity.User;
import com.authtrack.exception.BadRequestException;
import com.authtrack.repository.RoleRepository;
import com.authtrack.repository.UserRepository;
import com.authtrack.security.JwtUtils;
import com.authtrack.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldSucceed_whenUsernameAndEmailAreUnique() {
        AuthRequest.Register request = new AuthRequest.Register();
        request.setUsername("johndoe");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(roleRepository.findByName(Role.RoleName.ROLE_USER))
                .thenReturn(Optional.of(new Role(Role.RoleName.ROLE_USER)));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse response = authService.register(request);

        assertTrue(response.isSuccess());
        assertEquals("User registered successfully", response.getMessage());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_shouldIgnoreRequestedAdminRole_andAssignUserRoleOnly() {
        AuthRequest.Register request = new AuthRequest.Register();
        request.setUsername("sneaky");
        request.setEmail("sneaky@example.com");
        request.setPassword("password123");
        request.setRoles(Set.of("admin"));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(roleRepository.findByName(Role.RoleName.ROLE_USER))
                .thenReturn(Optional.of(new Role(Role.RoleName.ROLE_USER)));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse response = authService.register(request);

        assertTrue(response.isSuccess());
        verify(roleRepository, never()).findByName(Role.RoleName.ROLE_ADMIN);
        verify(roleRepository, never()).findByName(Role.RoleName.ROLE_MODERATOR);
    }

    @Test
    void register_shouldFail_whenUsernameAlreadyExists() {
        AuthRequest.Register request = new AuthRequest.Register();
        request.setUsername("existinguser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldFail_whenEmailAlreadyExists() {
        AuthRequest.Register request = new AuthRequest.Register();
        request.setUsername("newuser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldThrow_whenCredentialsAreInvalid() {
        AuthRequest.Login request = new AuthRequest.Login();
        request.setUsername("johndoe");
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(jwtUtils, never()).generateToken(any());
    }
}