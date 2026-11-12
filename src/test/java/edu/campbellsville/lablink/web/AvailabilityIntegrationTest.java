package edu.campbellsville.lablink.web;

import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.repository.LabRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * INTEGRATION tests for the lab availability week view (backlog 3.2). The seed data has an APPROVED
 * Cybersecurity Lab booking on Sep 22, 2026, 1:00-3:00 PM.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "student@lablink.edu", roles = "STUDENT")
class AvailabilityIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired LabRepository labRepository;

    @Test
    void weekViewShowsSeededBooking() throws Exception {
        mvc.perform(get("/labs/{id}/availability", labId("Cybersecurity Lab")).param("start", "2026-09-21"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cybersecurity Lab availability")))
                .andExpect(content().string(containsString("Sep 22 1:00 PM")))
                .andExpect(content().string(containsString("Free all day")));
    }

    @Test
    void weekViewLinksToNeighbouringWeeks() throws Exception {
        mvc.perform(get("/labs/{id}/availability", labId("Cybersecurity Lab")).param("start", "2026-09-21"))
                .andExpect(content().string(containsString("start=2026-09-14")))
                .andExpect(content().string(containsString("start=2026-09-28")));
    }

    @Test
    void labListLinksToAvailability() throws Exception {
        mvc.perform(get("/labs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/availability")));
    }

    private Long labId(String name) {
        return labRepository.findAll().stream()
                .filter(l -> l.getName().equals(name))
                .map(Lab::getId)
                .findFirst()
                .orElseThrow();
    }
}
