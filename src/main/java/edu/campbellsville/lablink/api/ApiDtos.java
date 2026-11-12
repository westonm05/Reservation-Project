package edu.campbellsville.lablink.api;

import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.model.Reservation;
import edu.campbellsville.lablink.model.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The JSON shapes the REST API returns. They are separate from the JPA entities on purpose:
 * the API only exposes what outsiders need, and never who made a booking.
 */
public final class ApiDtos {

    private ApiDtos() {
    }

    /** One lab in GET /api/labs. */
    public record LabSummary(Long id, String name, String building, String roomNumber, int capacity,
                             String department) {
        public static LabSummary from(Lab lab) {
            return new LabSummary(lab.getId(), lab.getName(), lab.getBuilding(), lab.getRoomNumber(),
                    lab.getCapacity(), lab.getDepartment() == null ? null : lab.getDepartment().getName());
        }
    }

    /** A booked time slot: when, and whether it's still pending or approved. No names. */
    public record BusySlot(LocalDateTime start, LocalDateTime end, ReservationStatus status) {
        public static BusySlot from(Reservation r) {
            return new BusySlot(r.getStartTime(), r.getEndTime(), r.getStatus());
        }
    }

    /** Body of GET /api/labs/{id}/availability. free is true when busy is empty. */
    public record LabAvailability(Long labId, String labName, LocalDateTime from, LocalDateTime to,
                                  boolean free, List<BusySlot> busy) {
    }

    /** Body returned with a 400 or 404. */
    public record ApiError(int status, String error) {
    }
}
