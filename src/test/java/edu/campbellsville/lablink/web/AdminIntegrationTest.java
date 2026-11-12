package edu.campbellsville.lablink.web;

import edu.campbellsville.lablink.model.Role;
import edu.campbellsville.lablink.model.User;
import edu.campbellsville.lablink.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** INTEGRATION tests for the admin Users pages (backlog 3.4), using the seed data. */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminIntegrationTest {

    private static final String MANAGER = "manager@lablink.edu";

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;

    @Test
    @WithMockUser(username = "student@lablink.edu", roles = "STUDENT")
    void studentsCannotOpenAdminPages() throws Exception {
        mvc.perform(get("/admin/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = MANAGER, roles = "LAB_MANAGER")
    void managerSeesEveryAccount() throws Exception {
        mvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("student@lablink.edu")))
                .andExpect(content().string(containsString("faculty@lablink.edu")));
    }

    @Test
    @WithMockUser(username = MANAGER, roles = "LAB_MANAGER")
    void managerCreatesFacultyAccount() throws Exception {
        mvc.perform(post("/admin/users").with(csrf())
                        .param("fullName", "Dr. New Hire")
                        .param("email", "new.hire@lablink.edu")
                        .param("password", "welcome123")
                        .param("role", "FACULTY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        User created = userRepository.findByEmail("new.hire@lablink.edu").orElseThrow();
        assertThat(created.getRole()).isEqualTo(Role.FACULTY);
    }

    @Test
    @WithMockUser(username = MANAGER, roles = "LAB_MANAGER")
    void duplicateEmailShowsErrorOnForm() throws Exception {
        mvc.perform(post("/admin/users").with(csrf())
                        .param("fullName", "Someone")
                        .param("email", "faculty@lablink.edu")
                        .param("password", "welcome123")
                        .param("role", "FACULTY"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("already exists")));
    }

    @Test
    @WithMockUser(username = MANAGER, roles = "LAB_MANAGER")
    void managerPromotesStudentToFaculty() throws Exception {
        Long studentId = userRepository.findByEmail("student@lablink.edu").orElseThrow().getId();

        mvc.perform(post("/admin/users/{id}/role", studentId).with(csrf()).param("role", "FACULTY"))
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("message", "Role updated."));

        assertThat(userRepository.findById(studentId).orElseThrow().getRole()).isEqualTo(Role.FACULTY);
    }

    @Test
    @WithMockUser(username = MANAGER, roles = "LAB_MANAGER")
    void managerCannotDemoteThemselves() throws Exception {
        Long managerId = userRepository.findByEmail(MANAGER).orElseThrow().getId();

        mvc.perform(post("/admin/users/{id}/role", managerId).with(csrf()).param("role", "STUDENT"))
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("error", "You can't change your own role"));

        assertThat(userRepository.findById(managerId).orElseThrow().getRole()).isEqualTo(Role.LAB_MANAGER);
    }
}
