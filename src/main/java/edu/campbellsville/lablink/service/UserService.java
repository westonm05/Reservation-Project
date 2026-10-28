package edu.campbellsville.lablink.service;

import edu.campbellsville.lablink.model.Role;
import edu.campbellsville.lablink.model.User;
import edu.campbellsville.lablink.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account with that email already exists");
        }
        User user = new User(normalizedEmail, fullName.trim(), passwordEncoder.encode(rawPassword), Role.STUDENT);
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
