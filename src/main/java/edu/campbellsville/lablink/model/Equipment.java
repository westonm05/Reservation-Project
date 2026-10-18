package edu.campbellsville.lablink.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A reservable item (laptop, oscilloscope, 3D printer...). It may live in a lab or be portable. */
@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentStatus status = EquipmentStatus.AVAILABLE;

    // Many pieces of equipment can belong to one lab. Null means portable / not assigned.
    @ManyToOne
    @JoinColumn(name = "lab_id")
    private Lab lab;

    public Equipment() {
    }

    public Equipment(String name, String serialNumber, String description, Lab lab) {
        this.name = name;
        this.serialNumber = serialNumber;
        this.description = description;
        this.lab = lab;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public EquipmentStatus getStatus() { return status; }
    public void setStatus(EquipmentStatus status) { this.status = status; }
    public Lab getLab() { return lab; }
    public void setLab(Lab lab) { this.lab = lab; }
}
