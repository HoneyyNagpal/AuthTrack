package com.authtrack.controller;

import com.authtrack.dto.ApiResponse;
import com.authtrack.dto.AuthRequest;
import com.authtrack.dto.JwtResponse;
import com.authtrack.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody AuthRequest.Login request) {
        JwtResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody AuthRequest.Register request) {
        ApiResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }
}
