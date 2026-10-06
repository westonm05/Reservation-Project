package edu.campbellsville.lablink.reservation;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * SINGLE RESPONSIBILITY: decides whether a requested time slot is valid and
 * free. That's its only job, so it's the only class that changes if the
 * scheduling rules change (e.g. adding cleanup time between bookings).
 *
 * Before the refactor this logic sat in the middle of createReservation().
 */
@Component
public class ReservationConflictChecker {

    /** Reservations in these states still block the time slot. */
    static final Set<ReservationStatus> ACTIVE = EnumSet.of(ReservationStatus.PENDING, ReservationStatus.APPROVED);

    private final ReservationRepository reservationRepository;
    private final Clock clock;

    // Constructor injection: Spring passes these in. Because they're
    // parameters, a test can pass in a fake repository or a fixed clock.
    public ReservationConflictChecker(ReservationRepository reservationRepository, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.clock = clock;
    }

    public void check(Long resourceId, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        if (start.isBefore(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("Reservations can't start in the past.");
        }
        // The database does the overlap search instead of loading every row.
        if (!reservationRepository.findOverlapping(resourceId, start, end, ACTIVE).isEmpty()) {
            throw new ReservationConflictException("That time slot is already taken.");
        }
    }
}
