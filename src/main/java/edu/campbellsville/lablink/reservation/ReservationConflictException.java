package edu.campbellsville.lablink.reservation;

/** Thrown when the requested time overlaps an existing reservation. */
public class ReservationConflictException extends RuntimeException {

    public ReservationConflictException(String message) {
        super(message);
    }
}
