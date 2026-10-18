package edu.campbellsville.lablink.model;

/** What a user is allowed to do. Stored in the users.role column as text. */
public enum Role {
    /** Can browse labs/equipment and request reservations (requests need approval). */
    STUDENT,
    /** Same as a student, but reservations are approved automatically. */
    FACULTY,
    /** Manages labs and equipment and approves or rejects reservation requests. */
    LAB_MANAGER
}
