package edu.campbellsville.lablink.reservation;

import java.time.LocalDateTime;

/**
 * What a user submits to create a reservation.
 *
 * requesterId is temporary: once Spring Security is wired in (auth branch),
 * the requester comes from the logged-in user instead of the request body.
 */
public record ReservationRequest(Long requesterId,
                                 Long resourceId,
                                 LocalDateTime startTime,
                                 LocalDateTime endTime) {
}
