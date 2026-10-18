package edu.campbellsville.lablink.model;

import java.util.List;

/** Life cycle of a reservation request. */
public enum ReservationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED;

    /** Statuses that still hold the time slot (used for double-booking checks). */
    public static final List<ReservationStatus> ACTIVE = List.of(PENDING, APPROVED);
}
