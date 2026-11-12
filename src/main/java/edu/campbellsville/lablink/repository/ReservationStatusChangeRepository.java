package edu.campbellsville.lablink.repository;

import edu.campbellsville.lablink.model.Reservation;
import edu.campbellsville.lablink.model.ReservationStatusChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationStatusChangeRepository extends JpaRepository<ReservationStatusChange, Long> {

    /** A reservation's audit trail, oldest first (id breaks ties within the same second). */
    List<ReservationStatusChange> findByReservationOrderByChangedAtAscIdAsc(Reservation reservation);
}
