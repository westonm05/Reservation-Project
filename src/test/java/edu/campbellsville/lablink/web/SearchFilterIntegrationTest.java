package edu.campbellsville.lablink.web;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** INTEGRATION tests for search/filter on the lab and equipment pages (backlog 3.8), using the seed data. */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "student@lablink.edu", roles = "STUDENT")
class SearchFilterIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void labSearchShowsOnlyMatchingLabs() throws Exception {
        mvc.perform(get("/labs").param("q", "robotics"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Robotics Lab")))
                .andExpect(content().string(not(containsString("Media Lab"))));
    }

    @Test
    void labSearchWithNoMatchesSaysSo() throws Exception {
        mvc.perform(get("/labs").param("q", "zzz-no-such-lab"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No labs match your search.")));
    }

    @Test
    void equipmentStatusFilterHidesOtherStatuses() throws Exception {
        mvc.perform(get("/equipment").param("q", "laptop").param("status", "MAINTENANCE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Loaner Laptop 4")))
                .andExpect(content().string(not(containsString("Loaner Laptop 1"))));
    }

    @Test
    void unfilteredPagesStillListEverything() throws Exception {
        mvc.perform(get("/equipment"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Loaner Laptop 1")))
                .andExpect(content().string(containsString("Loaner Laptop 4")));
    }
}
