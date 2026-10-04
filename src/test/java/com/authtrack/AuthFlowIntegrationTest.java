package com.authtrack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    private static final String PASSWORD = "secret123";
    private static final String ADMIN_USERNAME = "testadmin";
    private static final String ADMIN_PASSWORD = "AdminPass123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void selfRegistration_shouldIgnoreRequestedAdminRole() throws Exception {
        String username = uniqueName("sneaky");
        register(username, "admin");

        JsonNode login = login(username, PASSWORD);

        assertEquals(1, login.get("roles").size());
        assertEquals("ROLE_USER", login.get("roles").get(0).asText());
    }

    @Test
    void publicEndpoint_shouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/public/ping"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_shouldReturn401_withoutValidToken() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void regularUser_shouldBeForbiddenFromPrivilegedEndpoints() throws Exception {
        String username = uniqueName("regular");
        register(username);
        String token = bearer(login(username, PASSWORD));

        mockMvc.perform(get("/api/user/dashboard").header("Authorization", token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/moderator/panel").header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/panel").header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_shouldAccessAdminEndpoints() throws Exception {
        String token = bearer(login(ADMIN_USERNAME, ADMIN_PASSWORD));

        mockMvc.perform(get("/api/admin/panel").header("Authorization", token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users").header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    void admin_shouldPromoteUserToModerator() throws Exception {
        String username = uniqueName("promote");
        register(username);
        JsonNode userLogin = login(username, PASSWORD);
        String userToken = bearer(userLogin);
        long userId = userLogin.get("id").asLong();

        String adminToken = bearer(login(ADMIN_USERNAME, ADMIN_PASSWORD));

        MvcResult result = mockMvc.perform(patch("/api/users/" + userId + "/roles")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"moderator\"]}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode updated = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("[\"ROLE_MODERATOR\"]", updated.get("roles").toString());

        mockMvc.perform(get("/api/moderator/panel").header("Authorization", userToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/panel").header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_shouldNotBeAbleToChangeOwnRoles() throws Exception {
        JsonNode adminLogin = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        String adminToken = bearer(adminLogin);
        long adminId = adminLogin.get("id").asLong();

        mockMvc.perform(patch("/api/users/" + adminId + "/roles")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"user\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void disabledUser_shouldNotBeAbleToLogin() throws Exception {
        String username = uniqueName("disabled_login");
        register(username);
        long userId = login(username, PASSWORD).get("id").asLong();

        String adminToken = bearer(login(ADMIN_USERNAME, ADMIN_PASSWORD));
        mockMvc.perform(patch("/api/users/" + userId + "/toggle-status")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isForbidden());
    }

    @Test
    void disabledUser_existingToken_shouldBeRejected() throws Exception {
        String username = uniqueName("disabled_token");
        register(username);
        JsonNode userLogin = login(username, PASSWORD);
        String userToken = bearer(userLogin);
        long userId = userLogin.get("id").asLong();

        mockMvc.perform(get("/api/user/dashboard").header("Authorization", userToken))
                .andExpect(status().isOk());

        String adminToken = bearer(login(ADMIN_USERNAME, ADMIN_PASSWORD));
        mockMvc.perform(patch("/api/users/" + userId + "/toggle-status")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/dashboard").header("Authorization", userToken))
                .andExpect(status().isUnauthorized());
    }

    private String uniqueName(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void register(String username, String... roles) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("email", username + "@example.com");
        body.put("password", PASSWORD);
        if (roles.length > 0) {
            body.put("roles", List.of(roles));
        }

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    private JsonNode login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearer(JsonNode loginResponse) {
        return "Bearer " + loginResponse.get("token").asText();
    }
}