package ru.konyaev.priem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.konyaev.priem.dto.request.LoginRequest;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String getAdminToken() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .username("admin")
                .password("admin123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("token").asText();
    }

    @Test
    @DisplayName("Admin login with invalid credentials returns 401")
    void testAdminLoginFailure() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .username("wrong_admin")
                .password("wrong_password")
                .build();

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Неверный логин или пароль")));
    }

    @Test
    @DisplayName("Admin access to /api/admin/applications without token returns 401")
    void testProtectedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin access to /api/admin/applications with valid JWT token succeeds")
    void testProtectedWithToken() throws Exception {
        String token = getAdminToken();

        mockMvc.perform(get("/api/admin/applications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", notNullValue()))
                .andExpect(jsonPath("$.total", notNullValue()));
    }

    @Test
    @DisplayName("Admin export applications to Excel returns binary xlsx")
    void testExportExcel() throws Exception {
        String token = getAdminToken();

        mockMvc.perform(get("/api/admin/applications/export/excel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    @DisplayName("Invalid ID format or out-of-range number returns 400, never 500")
    void testInvalidIdReturns400() throws Exception {
        String token = getAdminToken();

        mockMvc.perform(patch("/api/admin/applications/not-a-number/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"confirmed\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }
}
