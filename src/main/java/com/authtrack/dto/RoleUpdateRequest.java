package com.authtrack.dto;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

public class RoleUpdateRequest {

    @NotEmpty(message = "At least one role is required")
    private Set<String> roles;

    public RoleUpdateRequest() {}

    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
}