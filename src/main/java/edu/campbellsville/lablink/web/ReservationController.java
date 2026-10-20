package edu.campbellsville.lablink.web;

import edu.campbellsville.lablink.model.Reservation;
import edu.campbellsville.lablink.model.ReservationStatus;
import edu.campbellsville.lablink.model.User;
import edu.campbellsville.lablink.service.InventoryService;
import edu.campbellsville.lablink.service.ReservationException;
import edu.campbellsville.lablink.service.ReservationService;
import edu.campbellsville.lablink.service.UserService;
import edu.campbellsville.lablink.web.form.ReservationForm;
import edu.campbellsville.lablink.web.form.RescheduleForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservations;
    private final InventoryService inventory;
    private final UserService users;

    public ReservationController(ReservationService reservations, InventoryService inventory, UserService users) {
        this.reservations = reservations;
        this.inventory = inventory;
        this.users = users;
    }

    /** "My reservations" */
    @GetMapping
    public String mine(Principal principal, Model model) {
        model.addAttribute("reservations", reservations.forUser(currentUser(principal)));
        return "reservations/list";
    }

    /** Opened from a "Reserve" button: /reservations/new?labId=1 or ?equipmentId=4 */
    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long labId,
                          @RequestParam(required = false) Long equipmentId,
                          Model model) {
        ReservationForm form = new ReservationForm();
        form.setLabId(labId);
        form.setEquipmentId(equipmentId);
        return showForm(form, model);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ReservationForm form, BindingResult errors,
                         Principal principal, Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return showForm(form, model);
        }
        User user = currentUser(principal);
        try {
            if (form.getLabId() != null) {
                reservations.requestLab(user, form.getLabId(), form.getStartTime(), form.getEndTime(), form.getPurpose());
            } else if (form.getEquipmentId() != null) {
                reservations.requestEquipment(user, form.getEquipmentId(), form.getStartTime(), form.getEndTime(), form.getPurpose());
            } else {
                throw new ReservationException("Choose a lab or equipment to reserve");
            }
        } catch (ReservationException e) {
            model.addAttribute("error", e.getMessage());
            return showForm(form, model);
        }
        redirect.addFlashAttribute("message", "Reservation submitted.");
        return "redirect:/reservations";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        return act(redirect, "/reservations", "Reservation cancelled.",
                () -> reservations.cancel(id, currentUser(principal)));
    }

    // ---- reschedule (owner, pending reservations only) ----

    @GetMapping("/{id}/reschedule")
    public String rescheduleForm(@PathVariable Long id, Principal principal, Model model, RedirectAttributes redirect) {
        Reservation reservation = ownPending(id, principal);
        if (reservation == null) {
            redirect.addFlashAttribute("error", "Only your own pending reservations can be rescheduled");
            return "redirect:/reservations";
        }
        RescheduleForm form = new RescheduleForm();
        form.setStartTime(reservation.getStartTime());
        form.setEndTime(reservation.getEndTime());
        model.addAttribute("form", form);
        model.addAttribute("reservation", reservation);
        return "reservations/reschedule";
    }

    @PostMapping("/{id}/reschedule")
    public String reschedule(@PathVariable Long id, @Valid @ModelAttribute("form") RescheduleForm form, BindingResult errors,
                             Principal principal, Model model, RedirectAttributes redirect) {
        Reservation reservation = ownPending(id, principal);
        if (reservation == null) {
            redirect.addFlashAttribute("error", "Only your own pending reservations can be rescheduled");
            return "redirect:/reservations";
        }
        if (!errors.hasErrors()) {
            try {
                reservations.reschedule(id, currentUser(principal), form.getStartTime(), form.getEndTime());
                redirect.addFlashAttribute("message", "Reservation rescheduled.");
                return "redirect:/reservations";
            } catch (ReservationException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        model.addAttribute("reservation", reservation);
        return "reservations/reschedule";
    }

    // ---- lab manager pages ----

    @GetMapping("/manage")
    public String manage(Model model) {
        model.addAttribute("pending", reservations.pending());
        model.addAttribute("all", reservations.all());
        return "reservations/manage";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        return act(redirect, "/reservations/manage", "Reservation approved.",
                () -> reservations.approve(id, currentUser(principal)));
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        return act(redirect, "/reservations/manage", "Reservation rejected.",
                () -> reservations.reject(id, currentUser(principal)));
    }

    // ---- helpers ----

    private String act(RedirectAttributes redirect, String target, String success, Runnable action) {
        try {
            action.run();
            redirect.addFlashAttribute("message", success);
        } catch (ReservationException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + target;
    }

    private String showForm(ReservationForm form, Model model) {
        String resourceName = form.getLabId() != null
                ? inventory.getLab(form.getLabId()).getName()
                : form.getEquipmentId() != null ? inventory.getEquipment(form.getEquipmentId()).getName() : null;
        model.addAttribute("form", form);
        model.addAttribute("resourceName", resourceName);
        return "reservations/form";
    }

    /** The current user's reservation with this id if it is still PENDING, otherwise null. */
    private Reservation ownPending(Long id, Principal principal) {
        return reservations.forUser(currentUser(principal)).stream()
                .filter(r -> r.getId().equals(id) && r.getStatus() == ReservationStatus.PENDING)
                .findFirst()
                .orElse(null);
    }

    private User currentUser(Principal principal) {
        return users.getByEmail(principal.getName());
    }
}
