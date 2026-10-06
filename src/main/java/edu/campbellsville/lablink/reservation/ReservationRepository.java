package edu.campbellsville.lablink.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Reservations for this resource whose time range overlaps [start, end).
     * Two ranges overlap when each one starts before the other one ends.
     */
    @Query("""
            select r from Reservation r
            where r.resource.id = :resourceId
              and r.status in :activeStatuses
              and r.startTime < :end
              and r.endTime > :start
            """)
    List<Reservation> findOverlapping(@Param("resourceId") Long resourceId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end,
                                      @Param("activeStatuses") Collection<ReservationStatus> activeStatuses);

    List<Reservation> findByRequesterIdOrderByStartTimeDesc(Long requesterId);
}
