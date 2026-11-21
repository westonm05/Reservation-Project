package edu.campbellsville.lablink.web;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The health check Docker uses must be public; the metrics must not be. */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void healthIsPublicAndUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void readinessProbeIsPublic() throws Exception {
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    @Test
    void metricsNeedALogin() throws Exception {
        mvc.perform(get("/actuator/metrics")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentsCannotSeeMetrics() throws Exception {
        mvc.perform(get("/actuator/metrics")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LAB_MANAGER")
    void labManagersCanSeeMetrics() throws Exception {
        mvc.perform(get("/actuator/metrics/jvm.memory.used")).andExpect(status().isOk());
    }
}
