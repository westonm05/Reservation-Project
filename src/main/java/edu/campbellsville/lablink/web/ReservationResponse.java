package edu.campbellsville.lablink.web;

import edu.campbellsville.lablink.reservation.Reservation;

import java.time.LocalDateTime;

/**
 * What the API sends back. Using a separate class instead of returning the
 * Reservation entity keeps database details (and the whole User object)
 * out of the JSON.
 */
public record ReservationResponse(Long id,
                                  String resourceName,
                                  LocalDateTime startTime,
                                  LocalDateTime endTime,
                                  String status,
                                  String statusReason) {

    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(r.getId(), r.getResource().getName(),
                r.getStartTime(), r.getEndTime(), r.getStatus().name(), r.getStatusReason());
    }
}
