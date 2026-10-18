package edu.campbellsville.lablink.web;

import edu.campbellsville.lablink.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sends fake HTTP requests through the whole app (security, controllers, templates, database). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityAndRegistrationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;

    @Test
    void anonymousUserIsSentToLogin() throws Exception {
        mvc.perform(get("/labs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void loginAndRegisterPagesArePublic() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    void registrationCreatesStudentAccount() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "New Student")
                        .param("email", "New.Student@Example.edu")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        assertThat(userRepository.findByEmail("new.student@example.edu")).isPresent();
    }

    @Test
    void mismatchedPasswordsShowFormAgain() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "Typo Person")
                        .param("email", "typo@example.edu")
                        .param("password", "password123")
                        .param("confirmPassword", "different123"))
                .andExpect(status().isOk());
        assertThat(userRepository.findByEmail("typo@example.edu")).isEmpty();
    }

    @Test
    void seededManagerCanLogIn() throws Exception {
        mvc.perform(formLogin("/login").userParameter("email").user("manager@lablink.edu").password("manager123"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @WithMockUser(username = "student@lablink.edu", roles = "STUDENT")
    void studentCanViewLabsButNotAddThem() throws Exception {
        mvc.perform(get("/labs")).andExpect(status().isOk());
        mvc.perform(get("/labs/new")).andExpect(status().isForbidden());
        mvc.perform(get("/reservations/manage")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@lablink.edu", roles = "LAB_MANAGER")
    void managerCanReachManagementPages() throws Exception {
        mvc.perform(get("/labs/new")).andExpect(status().isOk());
        mvc.perform(get("/reservations/manage")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "student@lablink.edu", roles = "STUDENT")
    void dashboardRendersForSeededStudent() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
    }
}
