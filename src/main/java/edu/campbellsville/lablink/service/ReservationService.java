package edu.campbellsville.lablink.service;

import edu.campbellsville.lablink.model.Equipment;
import edu.campbellsville.lablink.model.EquipmentStatus;
import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.model.Reservation;
import edu.campbellsville.lablink.model.ReservationStatus;
import edu.campbellsville.lablink.model.Role;
import edu.campbellsville.lablink.model.User;
import edu.campbellsville.lablink.repository.EquipmentRepository;
import edu.campbellsville.lablink.repository.LabRepository;
import edu.campbellsville.lablink.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The core business rules of LabLink:
 *  - a reservation must be for the future and end after it starts
 *  - a lab or piece of equipment can't be double-booked
 *  - faculty and lab managers are approved automatically; students wait for a lab manager
 *  - only the owner can cancel, only a lab manager can approve/reject
 */
@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final LabRepository labRepository;
    private final EquipmentRepository equipmentRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              LabRepository labRepository,
                              EquipmentRepository equipmentRepository) {
        this.reservationRepository = reservationRepository;
        this.labRepository = labRepository;
        this.equipmentRepository = equipmentRepository;
    }

    @Transactional
    public Reservation requestLab(User requester, Long labId, LocalDateTime start, LocalDateTime end, String purpose) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new ReservationException("That lab does not exist"));
        if (!lab.isActive()) {
            throw new ReservationException("That lab is not currently available for reservations");
        }
        validateTimes(start, end);
        if (reservationRepository.existsByLabAndStatusInAndStartTimeBeforeAndEndTimeAfter(
                lab, ReservationStatus.ACTIVE, end, start)) {
            throw new ReservationException(lab.getName() + " is already booked during that time");
        }
        Reservation reservation = newReservation(requester, start, end, purpose);
        reservation.setLab(lab);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation requestEquipment(User requester, Long equipmentId, LocalDateTime start, LocalDateTime end, String purpose) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ReservationException("That equipment does not exist"));
        if (equipment.getStatus() != EquipmentStatus.AVAILABLE) {
            throw new ReservationException(equipment.getName() + " is not available (" + equipment.getStatus() + ")");
        }
        validateTimes(start, end);
        if (reservationRepository.existsByEquipmentAndStatusInAndStartTimeBeforeAndEndTimeAfter(
                equipment, ReservationStatus.ACTIVE, end, start)) {
            throw new ReservationException(equipment.getName() + " is already booked during that time");
        }
        Reservation reservation = newReservation(requester, start, end, purpose);
        reservation.setEquipment(equipment);
        return reservationRepository.save(reservation);
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
        if (!reservation.getUser().getId().equals(requester.getId())) {
            throw new ReservationException("You can only cancel your own reservations");
        }
        if (!ReservationStatus.ACTIVE.contains(reservation.getStatus())) {
            throw new ReservationException("Only pending or approved reservations can be cancelled");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
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

    private void review(Long reservationId, User manager, ReservationStatus decision) {
        if (manager.getRole() != Role.LAB_MANAGER) {
            throw new ReservationException("Only lab managers can review reservations");
        }
        Reservation reservation = get(reservationId);
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ReservationException("This reservation has already been reviewed");
        }
        reservation.setStatus(decision);
        reservation.setReviewedBy(manager);
        reservation.setReviewedAt(LocalDateTime.now());
        // No save() call needed: inside a @Transactional method, JPA writes changes automatically.
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
