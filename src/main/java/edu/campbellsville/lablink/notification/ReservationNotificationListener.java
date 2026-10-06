package edu.campbellsville.lablink.notification;

import edu.campbellsville.lablink.reservation.Reservation;
import edu.campbellsville.lablink.reservation.ReservationStatusChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * OBSERVER PATTERN: a concrete observer.
 *
 * Spring calls onStatusChanged() every time ReservationService publishes a
 * ReservationStatusChangedEvent. ReservationService has no reference to this
 * class at all, so we can add more observers (audit log, availability
 * dashboard) without touching the service.
 *
 * @TransactionalEventListener waits until the reservation is actually saved
 * (the database transaction commits) before notifying. That way nobody gets
 * an "approved" email for a reservation that failed to save.
 */
@Component
public class ReservationNotificationListener {

    private final NotificationSender sender;

    public ReservationNotificationListener(NotificationSender sender) {
        this.sender = sender;
    }

    @TransactionalEventListener
    public void onStatusChanged(ReservationStatusChangedEvent event) {
        Reservation r = event.reservation();
        String subject = "LabLink: reservation " + r.getStatus().name().toLowerCase();
        String body = "Your reservation for " + r.getResource().getName()
                + " (" + r.getStartTime() + " to " + r.getEndTime() + ") is now " + r.getStatus() + "."
                + (r.getStatusReason() != null ? " " + r.getStatusReason() : "");
        sender.send(r.getRequester().getEmail(), subject, body);
    }
}
