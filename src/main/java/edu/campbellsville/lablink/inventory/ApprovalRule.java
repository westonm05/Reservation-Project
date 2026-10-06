package edu.campbellsville.lablink.inventory;

/**
 * How requests for a resource get approved. A lab manager picks one of these
 * for each lab or piece of equipment.
 *
 * Each value is handled by exactly one ApprovalPolicy class
 * (see reservation.approval). Adding a new rule = add a value here and a new
 * policy class; nothing else changes.
 */
public enum ApprovalRule {
    /** Anyone can book it; approved instantly (e.g. an open computer lab). */
    AUTO,
    /** Students need a lab manager to approve; faculty and managers don't. */
    MANAGER_REVIEW,
    /** Only faculty and lab managers may book it. */
    FACULTY_ONLY,
    /** The requester must hold the resource's required certification. */
    TRAINING_REQUIRED
}
