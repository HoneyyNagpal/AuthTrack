package com.authtrack.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.authtrack.dto.ApiResponse;
import com.authtrack.dto.AuthRequest;
import com.authtrack.dto.JwtResponse;
import com.authtrack.entity.Role;
import com.authtrack.entity.User;
import com.authtrack.exception.BadRequestException;
import com.authtrack.exception.RoleNotFoundException;
import com.authtrack.repository.RoleRepository;
import com.authtrack.repository.UserRepository;
import com.authtrack.security.JwtUtils;
import com.authtrack.security.UserDetailsImpl;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    public JwtResponse login(AuthRequest.Login request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            log.warn("Failed login attempt for username={}", request.getUsername());
            throw ex;
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        log.info("User logged in: username={}, roles={}", userDetails.getUsername(), roles);

        return new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(),
                userDetails.getEmail(), roles);
    }

    @Transactional
    public ApiResponse register(AuthRequest.Register request) {
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            log.warn("Ignoring requested roles during self-registration: username={}", request.getUsername());
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration rejected, username already taken: username={}", request.getUsername());
            throw new BadRequestException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration rejected, email already registered: username={}", request.getUsername());
            throw new BadRequestException("Email is already registered");
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );

        // Self-registration always gets ROLE_USER. Elevated roles are granted by an admin only.
        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new RoleNotFoundException("Default role ROLE_USER not found. Check DB seeding."));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);
        userRepository.save(user);

        log.info("User registered: username={}", request.getUsername());

        return new ApiResponse(true, "User registered successfully");
    }
}