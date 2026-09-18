package com.cs450.reservations;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service // business logic lives here, not in the controller
public class UserService {

    private final UserRepository userRepository; // saves and loads users
    private final PasswordEncoder passwordEncoder; // the BCrypt bean from SecurityConfig

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) { // Spring supplies both
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean emailTaken(String email) { // US-1: no duplicate emails
        return userRepository.existsByEmail(email);
    }

    public void register(String email, String rawPassword, String fullName) {
        User user = new User(); // plain Java object for now, not in the database yet
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword)); // hash it before it ever touches the database
        user.setFullName(fullName);
        user.setRole("STUDENT"); // US-1: new accounts default to student
        userRepository.save(user); // Hibernate turns this into an INSERT
    }
}
