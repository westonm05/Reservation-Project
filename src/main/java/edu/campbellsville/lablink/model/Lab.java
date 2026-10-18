package edu.campbellsville.lablink.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A room that can be reserved. Validation annotations are checked when the lab form is submitted. */
@Entity
@Table(name = "labs")
public class Lab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank(message = "Building is required")
    @Size(max = 100)
    @Column(nullable = false)
    private String building;

    @NotBlank(message = "Room number is required")
    @Size(max = 20)
    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Column(nullable = false)
    private int capacity = 1;

    @Size(max = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true; // inactive labs can't be reserved

    public Lab() {
    }

    public Lab(String name, String building, String roomNumber, int capacity, String description) {
        this.name = name;
        this.building = building;
        this.roomNumber = roomNumber;
        this.capacity = capacity;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
