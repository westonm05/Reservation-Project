package edu.campbellsville.lablink.service;

import edu.campbellsville.lablink.model.Equipment;
import edu.campbellsville.lablink.model.EquipmentStatus;
import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.model.Reservation;
import edu.campbellsville.lablink.model.ReservationStatusChange;
import edu.campbellsville.lablink.model.ReservationStatus;
import edu.campbellsville.lablink.model.Role;
import edu.campbellsville.lablink.model.User;
import edu.campbellsville.lablink.repository.EquipmentRepository;
import edu.campbellsville.lablink.repository.LabRepository;
import edu.campbellsville.lablink.repository.ReservationRepository;
import edu.campbellsville.lablink.repository.ReservationStatusChangeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The core business rules of LabLink:
 *  - a reservation must be for the future and end after it starts
 *  - a lab or piece of equipment can't be double-booked
 *  - faculty and lab managers are approved automatically; students wait for a lab manager
 *  - only the owner can cancel or reschedule, only a lab manager can approve/reject
 *  - only pending reservations can be rescheduled
 *  - equipment can't be reserved for longer than its category's loan limit
 *  - every status change is recorded in reservation_status_history (who, when, from, to)
 */
@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final LabRepository labRepository;
    private final EquipmentRepository equipmentRepository;
    private final ReservationStatusChangeRepository historyRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              LabRepository labRepository,
                              EquipmentRepository equipmentRepository,
                              ReservationStatusChangeRepository historyRepository) {
        this.reservationRepository = reservationRepository;
        this.labRepository = labRepository;
        this.equipmentRepository = equipmentRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public Reservation requestLab(User requester, Long labId, LocalDateTime start, LocalDateTime end, String purpose) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new ReservationException("That lab does not exist"));
        if (!lab.isActive()) {
            throw new ReservationException("That lab is not currently available for reservations");
        }
        validateTimes(start, end);
        rejectIfBooked(lab.getName(), reservationRepository.existsByLabAndStatusInAndStartTimeBeforeAndEndTimeAfter(
                lab, ReservationStatus.ACTIVE, end, start));
        Reservation reservation = newReservation(requester, start, end, purpose);
        reservation.setLab(lab);
        return saveNew(reservation);
    }

    @Transactional
    public Reservation requestEquipment(User requester, Long equipmentId, LocalDateTime start, LocalDateTime end, String purpose) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ReservationException("That equipment does not exist"));
        if (equipment.getStatus() != EquipmentStatus.AVAILABLE) {
            throw new ReservationException(equipment.getName() + " is not available (" + equipment.getStatus() + ")");
        }
        validateTimes(start, end);
        checkLoanLimit(equipment, start, end);
        rejectIfBooked(equipment.getName(), reservationRepository.existsByEquipmentAndStatusInAndStartTimeBeforeAndEndTimeAfter(
                equipment, ReservationStatus.ACTIVE, end, start));
        Reservation reservation = newReservation(requester, start, end, purpose);
        reservation.setEquipment(equipment);
        return saveNew(reservation);
    }

    @Transactional
    public void approve(Long reservationId, User manager) {
        review(reservationId, manager, ReservationStatus.APPROVED);
    }

    @Transactional
    public void reject(Long reservationId, User manager) {
        review(reservationId, manager, ReservationStatus.REJECTED);
    }

    @Transactional
    public void cancel(Long reservationId, User requester) {
        Reservation reservation = get(reservationId);
        requireOwner(reservation, requester, "cancel");
        if (!ReservationStatus.ACTIVE.contains(reservation.getStatus())) {
            throw new ReservationException("Only pending or approved reservations can be cancelled");
        }
        changeStatus(reservation, ReservationStatus.CANCELLED, requester, "Cancelled by requester");
    }

    /** Moves a PENDING reservation to a new time window (backlog item 3.3). */
    @Transactional
    public Reservation reschedule(Long reservationId, User requester, LocalDateTime newStart, LocalDateTime newEnd) {
        Reservation reservation = get(reservationId);
        requireOwner(reservation, requester, "reschedule");
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ReservationException("Only pending reservations can be rescheduled");
        }
        validateTimes(newStart, newEnd);
        if (reservation.getEquipment() != null) {
            checkLoanLimit(reservation.getEquipment(), newStart, newEnd);
        }
        rejectIfBooked(reservation.getResourceName(), overlapsOtherBookings(reservation, newStart, newEnd));
        reservation.setStartTime(newStart);
        reservation.setEndTime(newEnd);
        return reservation;
    }

    public List<Reservation> forUser(User user) {
        return reservationRepository.findByUserOrderByStartTimeDesc(user);
    }

    public List<Reservation> pending() {
        return reservationRepository.findByStatusOrderByStartTimeAsc(ReservationStatus.PENDING);
    }

    public List<Reservation> all() {
        return reservationRepository.findAllByOrderByStartTimeDesc();
    }

    public long pendingCount() {
        return reservationRepository.countByStatus(ReservationStatus.PENDING);
    }

    // ---- helpers ----

    private Reservation get(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationException("Reservation not found"));
    }

    private void requireOwner(Reservation reservation, User user, String action) {
        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new ReservationException("You can only " + action + " your own reservations");
        }
    }

    private void rejectIfBooked(String resourceName, boolean alreadyBooked) {
        if (alreadyBooked) {
            throw new ReservationException(resourceName + " is already booked during that time");
        }
    }

    /** Would the reservation, moved to start..end, overlap any OTHER active booking of the same lab/equipment? */
    private boolean overlapsOtherBookings(Reservation reservation, LocalDateTime start, LocalDateTime end) {
        if (reservation.getLab() != null) {
            return reservationRepository.existsByLabAndStatusInAndStartTimeBeforeAndEndTimeAfterAndIdNot(
                    reservation.getLab(), ReservationStatus.ACTIVE, end, start, reservation.getId());
        }
        return reservationRepository.existsByEquipmentAndStatusInAndStartTimeBeforeAndEndTimeAfterAndIdNot(
                reservation.getEquipment(), ReservationStatus.ACTIVE, end, start, reservation.getId());
    }

    private void review(Long reservationId, User manager, ReservationStatus decision) {
        if (manager.getRole() != Role.LAB_MANAGER) {
            throw new ReservationException("Only lab managers can review reservations");
        }
        Reservation reservation = get(reservationId);
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ReservationException("This reservation has already been reviewed");
        }
        changeStatus(reservation, decision, manager,
                decision == ReservationStatus.APPROVED ? "Approved by lab manager" : "Rejected by lab manager");
        reservation.setReviewedBy(manager);
        reservation.setReviewedAt(LocalDateTime.now());
        // No save() call needed: inside a @Transactional method, JPA writes changes automatically.
    }

    /** Saves a new reservation and writes the first row of its history. */
    private Reservation saveNew(Reservation reservation) {
        Reservation saved = reservationRepository.save(reservation);
        String note = saved.getStatus() == ReservationStatus.APPROVED
                ? "Submitted and approved automatically" : "Submitted for approval";
        historyRepository.save(new ReservationStatusChange(saved, null, saved.getStatus(), saved.getUser(), note));
        return saved;
    }

    /** Sets the new status and records the change in reservation_status_history. */
    private void changeStatus(Reservation reservation, ReservationStatus newStatus, User changedBy, String note) {
        ReservationStatus oldStatus = reservation.getStatus();
        reservation.setStatus(newStatus);
        historyRepository.save(new ReservationStatusChange(reservation, oldStatus, newStatus, changedBy, note));
    }

    /** Rejects equipment reservations longer than the item's category allows (no category = no limit). */
    private void checkLoanLimit(Equipment equipment, LocalDateTime start, LocalDateTime end) {
        if (equipment.getCategory() == null) {
            return;
        }
        int maxHours = equipment.getCategory().getMaxLoanHours();
        if (Duration.between(start, end).compareTo(Duration.ofHours(maxHours)) > 0) {
            throw new ReservationException(equipment.getCategory().getName()
                    + " items can be reserved for at most " + maxHours + " hours at a time");
        }
    }

    private void validateTimes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new ReservationException("Start and end times are required");
        }
        if (!end.isAfter(start)) {
            throw new ReservationException("End time must be after start time");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new ReservationException("Reservations must start in the future");
        }
    }

    private Reservation newReservation(User requester, LocalDateTime start, LocalDateTime end, String purpose) {
        Reservation reservation = new Reservation();
        reservation.setUser(requester);
        reservation.setStartTime(start);
        reservation.setEndTime(end);
        reservation.setPurpose(purpose);
        boolean autoApprove = requester.getRole() == Role.FACULTY || requester.getRole() == Role.LAB_MANAGER;
        reservation.setStatus(autoApprove ? ReservationStatus.APPROVED : ReservationStatus.PENDING);
        return reservation;
    }
}
