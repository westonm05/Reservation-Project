package edu.campbellsville.lablink.service;

import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.model.Reservation;
import edu.campbellsville.lablink.model.ReservationStatus;
import edu.campbellsville.lablink.repository.LabRepository;
import edu.campbellsville.lablink.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * When is a lab already booked? (backlog 3.2, and the REST API in 3.6)
 * "Booked" means a PENDING or APPROVED reservation, the same rule the double-booking check uses.
 */
@Service
@Transactional(readOnly = true)
public class AvailabilityService {

    /** The API refuses windows longer than this so one request can't ask for years of data. */
    static final Duration MAX_WINDOW = Duration.ofDays(31);

    private final LabRepository labRepository;
    private final ReservationRepository reservationRepository;

    public AvailabilityService(LabRepository labRepository, ReservationRepository reservationRepository) {
        this.labRepository = labRepository;
        this.reservationRepository = reservationRepository;
    }

    /** One day of a lab's schedule: the date and the bookings that touch it. */
    public record DaySchedule(LocalDate date, List<Reservation> bookings) {
        public boolean isFree() {
            return bookings.isEmpty();
        }
    }

    public Lab getLab(Long labId) {
        return labRepository.findById(labId).orElseThrow(() -> new IllegalArgumentException("Lab not found"));
    }

    /** Bookings for a lab that overlap [from, to), earliest first. */
    public List<Reservation> bookings(Long labId, LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new IllegalArgumentException("The end of the window must be after the start");
        }
        if (Duration.between(from, to).compareTo(MAX_WINDOW) > 0) {
            throw new IllegalArgumentException("Ask for at most 31 days at a time");
        }
        return reservationRepository.findByLabAndStatusInAndStartTimeBeforeAndEndTimeAfterOrderByStartTimeAsc(
                getLab(labId), ReservationStatus.ACTIVE, to, from);
    }

    /** True when nothing is booked in [start, end). */
    public boolean isFree(Long labId, LocalDateTime start, LocalDateTime end) {
        return bookings(labId, start, end).isEmpty();
    }

    /**
     * Seven days of a lab's schedule starting on firstDay. A booking that runs past midnight
     * shows up on every day it covers.
     */
    public List<DaySchedule> week(Long labId, LocalDate firstDay) {
        LocalDateTime weekStart = firstDay.atStartOfDay();
        List<Reservation> all = bookings(labId, weekStart, weekStart.plusDays(7));
        List<DaySchedule> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDateTime dayStart = weekStart.plusDays(i);
            LocalDateTime dayEnd = dayStart.plusDays(1);
            List<Reservation> today = all.stream()
                    .filter(r -> r.getStartTime().isBefore(dayEnd) && r.getEndTime().isAfter(dayStart))
                    .toList();
            days.add(new DaySchedule(dayStart.toLocalDate(), today));
        }
        return days;
    }
}
