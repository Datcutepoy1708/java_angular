package com.store;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
})
@ActiveProfiles("prod")
@AutoConfigureMockMvc
class ProductionProfileSmokeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Production profile: Swagger UI is disabled and returns 404")
    void swaggerUi_IsDisabled_Returns404() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Production profile: OpenAPI JSON docs are disabled and returns 404")
    void openApiDocs_AreDisabled_Returns404() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Production profile: Actuator metrics endpoint is blocked for anonymous users (returns 401)")
    void actuatorMetrics_Anonymous_Returns401() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Production profile: Actuator metrics is not exposed in web endpoints even for admin (returns 404)")
    void actuatorMetrics_Admin_IsNotExposed_Returns404() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Production profile: Actuator health does not leak component details (show-details: never)")
    void actuatorHealth_NeverShowsDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }
}
