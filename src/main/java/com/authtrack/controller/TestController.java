package com.authtrack.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/public/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("message", "AuthTrack is running"));
    }

    @GetMapping("/user/dashboard")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> userAccess() {
        return ResponseEntity.ok(Map.of("message", "User dashboard - access granted"));
    }

    @GetMapping("/moderator/panel")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> moderatorAccess() {
        return ResponseEntity.ok(Map.of("message", "Moderator panel - access granted"));
    }

    @GetMapping("/admin/panel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminAccess() {
        return ResponseEntity.ok(Map.of("message", "Admin panel - access granted"));
    }
}
