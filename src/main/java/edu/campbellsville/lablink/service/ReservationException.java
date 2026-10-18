package edu.campbellsville.lablink.service;

/** Thrown when a reservation request breaks a business rule; the message is shown to the user. */
public class ReservationException extends RuntimeException {

    public ReservationException(String message) {
        super(message);
    }
}
