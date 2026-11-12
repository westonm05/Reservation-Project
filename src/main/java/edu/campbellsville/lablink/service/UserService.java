package edu.campbellsville.lablink.service;

import edu.campbellsville.lablink.model.Role;
import edu.campbellsville.lablink.model.User;
import edu.campbellsville.lablink.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Business logic for accounts. Controllers call services; services call repositories. */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Spring passes in ("injects") the repository and encoder automatically.
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Creates a new student account. Throws if the email is already registered. */
    @Transactional
    public User registerStudent(String fullName, String email, String rawPassword) {
        return createAccount(fullName, email, rawPassword, Role.STUDENT);
    }

    /**
     * Creates an account with any role. Used by lab managers on the admin page (backlog 3.4)
     * to add faculty and other lab managers, who can't sign themselves up.
     */
    @Transactional
    public User createAccount(String fullName, String email, String rawPassword, Role role) {
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account with that email already exists");
        }
        User user = new User(normalizedEmail, fullName.trim(), passwordEncoder.encode(rawPassword), role);
        return userRepository.save(user);
    }

    /** Every account, sorted by name, for the admin page. */
    public List<User> allUsers() {
        return userRepository.findAllByOrderByFullNameAsc();
    }

    /**
     * Changes another user's role (backlog 3.4). A lab manager can't change their own role,
     * so the system can never be left without anyone able to manage it by accident.
     */
    @Transactional
    public User changeRole(Long userId, Role newRole, String actingEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getEmail().equalsIgnoreCase(actingEmail)) {
            throw new IllegalArgumentException("You can't change your own role");
        }
        user.setRole(newRole);
        return userRepository.save(user);
    }

    public boolean emailTaken(String email) {
        return userRepository.existsByEmail(email.trim().toLowerCase());
    }

    /**
     * Changes a user's password after checking they know the current one.
     * Throws IllegalArgumentException with a message the page can show if the check fails.
     */
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = getByEmail(email);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from the current one");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No user with email " + email));
    }
}
