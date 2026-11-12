package edu.campbellsville.lablink.web;

import edu.campbellsville.lablink.service.AvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/** Week view of when a lab is booked (backlog 3.2), e.g. /labs/3/availability?start=2026-11-16 */
@Controller
public class AvailabilityController {

    private final AvailabilityService availability;

    public AvailabilityController(AvailabilityService availability) {
        this.availability = availability;
    }

    @GetMapping("/labs/{id}/availability")
    public String week(@PathVariable Long id,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                       Model model) {
        LocalDate firstDay = start != null ? start : LocalDate.now();
        model.addAttribute("lab", availability.getLab(id));
        model.addAttribute("days", availability.week(id, firstDay));
        model.addAttribute("start", firstDay);
        model.addAttribute("previousWeek", firstDay.minusDays(7));
        model.addAttribute("nextWeek", firstDay.plusDays(7));
        return "labs/availability";
    }
}
