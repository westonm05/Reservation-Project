package edu.campbellsville.lablink.reservation;

/**
 * OBSERVER PATTERN: the message ReservationService "announces" whenever a
 * reservation is created or changes status.
 *
 * The service publishes this event and moves on. It doesn't know or care who
 * is listening (email, audit log, dashboards...). Listeners subscribe with
 * Spring's @EventListener annotation.
 */
public record ReservationStatusChangedEvent(Reservation reservation,
                                            ReservationStatus previousStatus) {
}
