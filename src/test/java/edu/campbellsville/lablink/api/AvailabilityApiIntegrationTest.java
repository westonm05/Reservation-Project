package edu.campbellsville.lablink.api;

import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.repository.LabRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * INTEGRATION tests for the public REST API (backlog 3.6): the whole app runs, with no logged-in user.
 * The seed data has an APPROVED Cybersecurity Lab booking on Sep 22, 2026, 1:00-3:00 PM.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AvailabilityApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired LabRepository labRepository;

    @Test
    void labListIsPublicJson() throws Exception {
        mvc.perform(get("/api/labs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[*].name", hasItem("Cybersecurity Lab")));
    }

    @Test
    void busyWindowListsTheSeededBooking() throws Exception {
        mvc.perform(get("/api/labs/{id}/availability", labId("Cybersecurity Lab"))
                        .param("from", "2026-09-22T08:00").param("to", "2026-09-22T18:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labName").value("Cybersecurity Lab"))
                .andExpect(jsonPath("$.free").value(false))
                .andExpect(jsonPath("$.busy", hasSize(1)))
                .andExpect(jsonPath("$.busy[0].start", startsWith("2026-09-22T13:00")))
                .andExpect(jsonPath("$.busy[0].status").value("APPROVED"));
    }

    @Test
    void openWindowIsFree() throws Exception {
        mvc.perform(get("/api/labs/{id}/availability", labId("Cybersecurity Lab"))
                        .param("from", "2026-09-22T15:00").param("to", "2026-09-22T17:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.free").value(true))
                .andExpect(jsonPath("$.busy", hasSize(0)));
    }

    @Test
    void unknownLabIs404() throws Exception {
        mvc.perform(get("/api/labs/999999/availability"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Lab not found"));
    }

    @Test
    void badDateIs400() throws Exception {
        mvc.perform(get("/api/labs/{id}/availability", labId("Cybersecurity Lab")).param("from", "next tuesday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void apiIsReadOnly() throws Exception {
        // POST isn't public: without a login (and CSRF token) it is refused
        mvc.perform(post("/api/labs")).andExpect(status().isForbidden());
    }

    private Long labId(String name) {
        return labRepository.findAll().stream()
                .filter(l -> l.getName().equals(name))
                .map(Lab::getId)
                .findFirst()
                .orElseThrow();
    }
}
