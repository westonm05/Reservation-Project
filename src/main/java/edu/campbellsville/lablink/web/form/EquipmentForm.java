package edu.campbellsville.lablink.web.form;

import edu.campbellsville.lablink.model.Equipment;
import edu.campbellsville.lablink.model.EquipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Equipment form: holds the chosen lab as an id (from a dropdown) instead of a Lab object. */
public class EquipmentForm {

    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Serial number is required")
    @Size(max = 50)
    private String serialNumber;

    @Size(max = 500)
    private String description;

    @NotNull
    private EquipmentStatus status = EquipmentStatus.AVAILABLE;

    private Long labId; // optional

    public static EquipmentForm from(Equipment e) {
        EquipmentForm form = new EquipmentForm();
        form.id = e.getId();
        form.name = e.getName();
        form.serialNumber = e.getSerialNumber();
        form.description = e.getDescription();
        form.status = e.getStatus();
        form.labId = e.getLab() != null ? e.getLab().getId() : null;
        return form;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public EquipmentStatus getStatus() { return status; }
    public void setStatus(EquipmentStatus status) { this.status = status; }
    public Long getLabId() { return labId; }
    public void setLabId(Long labId) { this.labId = labId; }
}
