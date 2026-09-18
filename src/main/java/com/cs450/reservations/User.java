package com.cs450.reservations;

import jakarta.persistence.*;

@Entity //this class is the database table
@Table(name = "users")//names the table users. This is needed as user is a reserved word
public class User {

    @Id// this is the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)//database assigns the id automatically
    private Long id;

    @Column(nullable = false, unique = true)//cannot be empty
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String role;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
