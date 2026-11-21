package edu.campbellsville.lablink.service;

import edu.campbellsville.lablink.model.Role;
import edu.campbellsville.lablink.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** UNIT tests for creating the first lab manager on an empty production database. */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock UserRepository userRepository;
    @Mock UserService userService;

    private AdminBootstrap bootstrap(String email, String password) {
        return new AdminBootstrap(userRepository, userService, email, password, "Ops Admin");
    }

    @Test
    void doesNothingWhenVariablesAreNotSet() {
        assertThat(bootstrap("", "").createFirstAdminIfNeeded()).isFalse();
        verify(userService, never()).createAccount(anyString(), anyString(), anyString(), any());
    }

    @Test
    void createsLabManagerWhenNoneExists() {
        when(userRepository.existsByRole(Role.LAB_MANAGER)).thenReturn(false);

        assertThat(bootstrap("admin@cu.edu", "a-long-password").createFirstAdminIfNeeded()).isTrue();
        verify(userService).createAccount("Ops Admin", "admin@cu.edu", "a-long-password", Role.LAB_MANAGER);
    }

    @Test
    void skipsWhenALabManagerAlreadyExists() {
        when(userRepository.existsByRole(Role.LAB_MANAGER)).thenReturn(true);

        assertThat(bootstrap("admin@cu.edu", "a-long-password").createFirstAdminIfNeeded()).isFalse();
        verify(userService, never()).createAccount(anyString(), anyString(), anyString(), any());
    }

    @Test
    void refusesAShortPassword() {
        when(userRepository.existsByRole(Role.LAB_MANAGER)).thenReturn(false);

        assertThatThrownBy(() -> bootstrap("admin@cu.edu", "short").createFirstAdminIfNeeded())
                .isInstanceOf(IllegalStateException.class);
    }
}
