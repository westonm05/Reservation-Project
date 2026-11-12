package edu.campbellsville.lablink.api;

import edu.campbellsville.lablink.api.ApiDtos.ApiError;
import edu.campbellsville.lablink.api.ApiDtos.BusySlot;
import edu.campbellsville.lablink.api.ApiDtos.LabAvailability;
import edu.campbellsville.lablink.api.ApiDtos.LabSummary;
import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.service.AvailabilityService;
import edu.campbellsville.lablink.service.InventoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Public, read-only REST API for lab availability (backlog 3.6, the project's advanced feature).
 * Other campus systems, such as a department website or a room-display screen, can ask whether a
 * lab is free without logging in. @RestController means every method returns JSON instead of a page.
 * See docs/api.md for examples.
 */
@RestController
@RequestMapping("/api/labs")
public class AvailabilityApiController {

    private final InventoryService inventory;
    private final AvailabilityService availability;

    public AvailabilityApiController(InventoryService inventory, AvailabilityService availability) {
        this.inventory = inventory;
        this.availability = availability;
    }

    /** GET /api/labs: every lab that can currently be reserved. */
    @GetMapping
    public List<LabSummary> labs() {
        return inventory.searchLabs(null, null, true).stream().map(LabSummary::from).toList();
    }

    /**
     * GET /api/labs/{id}/availability?from=2026-11-16T08:00&amp;to=2026-11-16T17:00
     * Both parameters are optional; the default window is the next 7 days starting today.
     */
    @GetMapping("/{id}/availability")
    public LabAvailability labAvailability(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDateTime start = from != null ? from : LocalDate.now().atStartOfDay();
        LocalDateTime end = to != null ? to : start.plusDays(7);
        Lab lab = availability.getLab(id);
        List<BusySlot> busy = availability.bookings(id, start, end).stream().map(BusySlot::from).toList();
        return new LabAvailability(lab.getId(), lab.getName(), start, end, busy.isEmpty(), busy);
    }

    // ---- errors come back as JSON too, not as an HTML error page ----

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException e) {
        HttpStatus status = e.getMessage() != null && e.getMessage().endsWith("not found")
                ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new ApiError(status.value(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> badParameter(MethodArgumentTypeMismatchException e) {
        String message = "Invalid value for '" + e.getName() + "'. Use ISO format, e.g. 2026-11-16T08:00";
        return ResponseEntity.badRequest().body(new ApiError(HttpStatus.BAD_REQUEST.value(), message));
    }
}
