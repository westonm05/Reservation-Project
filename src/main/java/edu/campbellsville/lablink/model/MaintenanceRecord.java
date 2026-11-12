package edu.campbellsville.lablink.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** A problem reported on a piece of equipment. Open while resolvedAt is null. */
@Entity
@Table(name = "maintenance_records")
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many records can belong to one item. LAZY: the equipment row is only loaded if we use it.
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_id", nullable = false)
    private User reportedBy;

    @Column(nullable = false, length = 500)
    private String issue;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;

    protected MaintenanceRecord() {
    }

    public MaintenanceRecord(Equipment equipment, User reportedBy, String issue) {
        this.equipment = equipment;
        this.reportedBy = reportedBy;
        this.issue = issue;
    }

    /** Closes the record. */
    public void resolve(String notes) {
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = notes;
    }

    public boolean isOpen() { return resolvedAt == null; }

    public Long getId() { return id; }
    public Equipment getEquipment() { return equipment; }
    public User getReportedBy() { return reportedBy; }
    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public String getResolutionNotes() { return resolutionNotes; }
}
