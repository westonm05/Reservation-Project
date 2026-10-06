package edu.campbellsville.lablink.auth;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * A LabLink account. Login details (password hash, etc.) are added on the
 * authentication branch; this class only holds what reservations need.
 */
@Entity
@Table(name = "users") // "user" is a reserved word in some databases
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    /** Safety trainings this user has completed, e.g. "LASER_SAFETY". */
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> certifications = new HashSet<>();

    protected User() {
        // required by JPA
    }

    public User(String fullName, String email, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    public boolean hasCertification(String certification) {
        return certifications.contains(certification);
    }

    public void addCertification(String certification) {
        certifications.add(certification);
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
}
