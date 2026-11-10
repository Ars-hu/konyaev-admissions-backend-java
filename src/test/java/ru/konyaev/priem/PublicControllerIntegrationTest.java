package ru.konyaev.priem;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.konyaev.priem.dto.RegisterRequest;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PublicControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/allowed-months returns list of summer months")
    void testGetAllowedMonths() throws Exception {
        mockMvc.perform(get("/api/allowed-months"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    @DisplayName("GET /api/dates-status returns valid structure")
    void testGetDatesStatus() throws Exception {
        mockMvc.perform(get("/api/dates-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.server_date", notNullValue()))
                .andExpect(jsonPath("$.advance_hours", is(24)));
    }

    @Test
    @DisplayName("POST /api/register validates required fields and returns 400")
    void testRegisterValidationFailure() throws Exception {
        RegisterRequest emptyReq = new RegisterRequest();

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()))
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("Malformed JSON returns 400 Bad Request, never 500")
    void testMalformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ broken json here"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("Out of range date string returns 400, never 500")
    void testInvalidDateReturns400() throws Exception {
        mockMvc.perform(get("/api/slots-status/99999-99-99-invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }
}
