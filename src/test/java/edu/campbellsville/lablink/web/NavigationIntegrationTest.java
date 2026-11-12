package edu.campbellsville.lablink.web;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** INTEGRATION tests for the refreshed layout and navigation (backlog 3.7). */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NavigationIntegrationTest {

    /** The Equipment link carries both class="active" and aria-current="page", in either order. */
    private static final Pattern ACTIVE_EQUIPMENT_LINK = Pattern.compile(
            "(?s).*<a href=\"/equipment\"(?=[^>]*class=\"active\")(?=[^>]*aria-current=\"page\")[^>]*>Equipment</a>.*");

    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username = "student@lablink.edu", roles = "STUDENT")
    void currentSectionIsMarkedInTheNavBar() throws Exception {
        mvc.perform(get("/equipment"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern(ACTIVE_EQUIPMENT_LINK)));
    }

    @Test
    @WithMockUser(username = "student@lablink.edu", roles = "STUDENT")
    void everyPageHasASkipLinkToMainContent() throws Exception {
        mvc.perform(get("/labs"))
                .andExpect(content().string(containsString("href=\"#main\"")))
                .andExpect(content().string(containsString("<main id=\"main\">")));
    }

    @Test
    @WithMockUser(username = "student@lablink.edu", roles = "STUDENT")
    void studentsDoNotSeeManagerLinks() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Find a lab")))
                .andExpect(content().string(not(containsString("Manage users"))));
    }

    @Test
    @WithMockUser(username = "manager@lablink.edu", roles = "LAB_MANAGER")
    void managersSeeAdminShortcut() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Manage users")));
    }
}
