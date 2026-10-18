package edu.campbellsville.lablink.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** What the "request a reservation" page submits. Exactly one of labId / equipmentId is filled. */
public class ReservationForm {

    private Long labId;
    private Long equipmentId;

    // Matches the format an HTML <input type="datetime-local"> sends, e.g. 2026-10-20T14:00
    @NotNull(message = "Start time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    @NotBlank(message = "Please describe the purpose")
    @Size(max = 255)
    private String purpose;

    public Long getLabId() { return labId; }
    public void setLabId(Long labId) { this.labId = labId; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}
