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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** UNIT tests for account registration and lookup. The repository and password encoder are mocks. */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserService service;

    @Test
    void registerStudentNormalizesInputAndHashesPassword() {
        when(userRepository.existsByEmail("new.student@example.edu")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User saved = service.registerStudent("  New Student ", "  New.Student@Example.EDU ", "password123");

        assertThat(saved.getEmail()).isEqualTo("new.student@example.edu");
        assertThat(saved.getFullName()).isEqualTo("New Student");
        assertThat(saved.getPasswordHash()).isEqualTo("HASHED").isNotEqualTo("password123");
        assertThat(saved.getRole()).isEqualTo(Role.STUDENT); // self-registration can never create a manager
    }

    @Test
    void registerStudentRejectsDuplicateEmailRegardlessOfCase() {
        when(userRepository.existsByEmail("taken@example.edu")).thenReturn(true);

        assertThatThrownBy(() -> service.registerStudent("Someone", "TAKEN@example.edu", "password123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void emailTakenIgnoresCaseAndSurroundingSpaces() {
        when(userRepository.existsByEmail("a@b.edu")).thenReturn(true);

        assertThat(service.emailTaken("  A@B.edu ")).isTrue();
    }

    @Test
    void getByEmailReturnsMatchingUser() {
        User u = new User("faculty@lablink.edu", "Demo Faculty", "hash", Role.FACULTY);
        when(userRepository.findByEmail("faculty@lablink.edu")).thenReturn(Optional.of(u));

        assertThat(service.getByEmail("faculty@lablink.edu")).isSameAs(u);
    }

    @Test
    void getByEmailThrowsForUnknownEmail() {
        when(userRepository.findByEmail("ghost@lablink.edu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByEmail("ghost@lablink.edu"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ghost@lablink.edu");
    }

    @Test
    void changePasswordStoresNewHashWhenCurrentPasswordIsCorrect() {
        User u = new User("student@lablink.edu", "Demo Student", "OLD_HASH", Role.STUDENT);
        when(userRepository.findByEmail("student@lablink.edu")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("student123", "OLD_HASH")).thenReturn(true);
        when(passwordEncoder.matches("newpassword1", "OLD_HASH")).thenReturn(false);
        when(passwordEncoder.encode("newpassword1")).thenReturn("NEW_HASH");

        service.changePassword("student@lablink.edu", "student123", "newpassword1");

        assertThat(u.getPasswordHash()).isEqualTo("NEW_HASH");
        verify(userRepository).save(u);
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        User u = new User("student@lablink.edu", "Demo Student", "OLD_HASH", Role.STUDENT);
        when(userRepository.findByEmail("student@lablink.edu")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("wrong-guess", "OLD_HASH")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword("student@lablink.edu", "wrong-guess", "newpassword1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Current password is incorrect");
        assertThat(u.getPasswordHash()).isEqualTo("OLD_HASH");
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordRejectsReusingTheSamePassword() {
        User u = new User("student@lablink.edu", "Demo Student", "OLD_HASH", Role.STUDENT);
        when(userRepository.findByEmail("student@lablink.edu")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("student123", "OLD_HASH")).thenReturn(true);

        assertThatThrownBy(() -> service.changePassword("student@lablink.edu", "student123", "student123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be different");
        verify(userRepository, never()).save(any());
    }
}
