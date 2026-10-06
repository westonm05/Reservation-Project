package edu.campbellsville.lablink.reservation;

import edu.campbellsville.lablink.auth.User;
import edu.campbellsville.lablink.inventory.Resource;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

/** One person's booking of one resource for a block of time. */
@Entity
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User requester;

    @ManyToOne(optional = false)
    private Resource resource;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    /** Why it was denied or sent for review, shown to the requester. */
    private String statusReason;

    protected Reservation() {
        // required by JPA
    }

    public Reservation(User requester, Resource resource, LocalDateTime startTime, LocalDateTime endTime) {
        this.requester = requester;
        this.resource = resource;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = ReservationStatus.PENDING;
    }

    public void changeStatus(ReservationStatus newStatus, String reason) {
        this.status = newStatus;
        this.statusReason = reason;
    }

    public Long getId() { return id; }
    public User getRequester() { return requester; }
    public Resource getResource() { return resource; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public ReservationStatus getStatus() { return status; }
    public String getStatusReason() { return statusReason; }
}
