package edu.campbellsville.lablink.repository;

import edu.campbellsville.lablink.model.Equipment;
import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.model.Reservation;
import edu.campbellsville.lablink.model.ReservationStatus;
import edu.campbellsville.lablink.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserOrderByStartTimeDesc(User user);

    List<Reservation> findByStatusOrderByStartTimeAsc(ReservationStatus status);

    List<Reservation> findAllByOrderByStartTimeDesc();

    long countByStatus(ReservationStatus status);

    /*
     * Double-booking check. Two time windows overlap when
     *   existing.start < requested.end  AND  existing.end > requested.start
     * The method name spells that out: StartTimeBefore(end) and EndTimeAfter(start).
     */
    boolean existsByLabAndStatusInAndStartTimeBeforeAndEndTimeAfter(
            Lab lab, Collection<ReservationStatus> statuses, LocalDateTime end, LocalDateTime start);

    boolean existsByEquipmentAndStatusInAndStartTimeBeforeAndEndTimeAfter(
            Equipment equipment, Collection<ReservationStatus> statuses, LocalDateTime end, LocalDateTime start);
}
