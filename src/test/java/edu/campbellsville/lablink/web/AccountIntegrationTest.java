package edu.campbellsville.lablink.web;

import edu.campbellsville.lablink.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** INTEGRATION tests for the account page, run against the seeded demo student (password student123). */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountIntegrationTest {

    private static final String STUDENT = "student@lablink.edu";

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void anonymousUserCannotSeeAccountPage() throws Exception {
        mvc.perform(get("/account"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void accountPageShowsLoggedInUser() throws Exception {
        mvc.perform(get("/account"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(STUDENT)));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void correctCurrentPasswordChangesPassword() throws Exception {
        mvc.perform(post("/account/password").with(csrf())
                        .param("currentPassword", "student123")
                        .param("newPassword", "brandnew456")
                        .param("confirmPassword", "brandnew456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"));

        String hash = userRepository.findByEmail(STUDENT).orElseThrow().getPasswordHash();
        assertThat(passwordEncoder.matches("brandnew456", hash)).isTrue();
        assertThat(passwordEncoder.matches("student123", hash)).isFalse();
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void wrongCurrentPasswordShowsErrorAndKeepsOldPassword() throws Exception {
        mvc.perform(post("/account/password").with(csrf())
                        .param("currentPassword", "not-my-password")
                        .param("newPassword", "brandnew456")
                        .param("confirmPassword", "brandnew456"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Current password is incorrect")));

        String hash = userRepository.findByEmail(STUDENT).orElseThrow().getPasswordHash();
        assertThat(passwordEncoder.matches("student123", hash)).isTrue();
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void mismatchedConfirmationShowsError() throws Exception {
        mvc.perform(post("/account/password").with(csrf())
                        .param("currentPassword", "student123")
                        .param("newPassword", "brandnew456")
                        .param("confirmPassword", "typo456789"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Passwords do not match")));
    }
}
