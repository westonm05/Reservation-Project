package edu.campbellsville.lablink.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * One row of a reservation's audit trail: "on [changedAt], [changedBy] moved it from
 * [oldStatus] to [newStatus]". Written by ReservationService and never changed afterwards.
 * oldStatus is null for the row written when the reservation is first created.
 */
@Entity
@Table(name = "reservation_status_history")
public class ReservationStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "old_status", length = 20)
    private ReservationStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "new_status", nullable = false, length = 20)
    private ReservationStatus newStatus;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private User changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    private String note;

    protected ReservationStatusChange() {
    }

    public ReservationStatusChange(Reservation reservation, ReservationStatus oldStatus,
                                   ReservationStatus newStatus, User changedBy, String note) {
        this.reservation = reservation;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.note = note;
    }

    public Long getId() { return id; }
    public Reservation getReservation() { return reservation; }
    public ReservationStatus getOldStatus() { return oldStatus; }
    public ReservationStatus getNewStatus() { return newStatus; }
    public User getChangedBy() { return changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public String getNote() { return note; }
}
