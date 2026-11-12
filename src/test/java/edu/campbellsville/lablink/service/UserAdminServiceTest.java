package edu.campbellsville.lablink.service;

import edu.campbellsville.lablink.model.Role;
import edu.campbellsville.lablink.model.User;
import edu.campbellsville.lablink.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** UNIT tests for the user-administration methods (backlog 3.4). The repository and encoder are mocks. */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserService service;

    @Test
    void createAccountUsesTheChosenRoleAndHashesPassword() {
        when(userRepository.existsByEmail("new.prof@lablink.edu")).thenReturn(false);
        when(passwordEncoder.encode("temporary1")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User saved = service.createAccount(" New Prof ", " New.Prof@LabLink.edu ", "temporary1", Role.FACULTY);

        assertThat(saved.getRole()).isEqualTo(Role.FACULTY);
        assertThat(saved.getEmail()).isEqualTo("new.prof@lablink.edu");
        assertThat(saved.getFullName()).isEqualTo("New Prof");
        assertThat(saved.getPasswordHash()).isEqualTo("HASHED");
    }

    @Test
    void createAccountRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("faculty@lablink.edu")).thenReturn(true);

        assertThatThrownBy(() -> service.createAccount("X", "faculty@lablink.edu", "temporary1", Role.LAB_MANAGER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    void allUsersComesBackSortedByName() {
        User a = new User("a@lablink.edu", "Alice", "h", Role.STUDENT);
        User b = new User("b@lablink.edu", "Bob", "h", Role.FACULTY);
        when(userRepository.findAllByOrderByFullNameAsc()).thenReturn(List.of(a, b));

        assertThat(service.allUsers()).containsExactly(a, b);
    }

    @Test
    void managerCanChangeAnotherUsersRole() {
        User student = new User("student@lablink.edu", "Demo Student", "h", Role.STUDENT);
        when(userRepository.findById(3L)).thenReturn(Optional.of(student));
        when(userRepository.save(student)).thenReturn(student);

        service.changeRole(3L, Role.FACULTY, "manager@lablink.edu");

        assertThat(student.getRole()).isEqualTo(Role.FACULTY);
        verify(userRepository).save(student);
    }

    @Test
    void managerCannotChangeOwnRole() {
        User manager = new User("manager@lablink.edu", "Demo Lab Manager", "h", Role.LAB_MANAGER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> service.changeRole(1L, Role.STUDENT, "MANAGER@lablink.edu"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You can't change your own role");
        assertThat(manager.getRole()).isEqualTo(Role.LAB_MANAGER);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeRoleOfMissingUserFails() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeRole(99L, Role.FACULTY, "manager@lablink.edu"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }
}
