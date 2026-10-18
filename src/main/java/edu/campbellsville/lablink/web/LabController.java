package edu.campbellsville.lablink.web;

import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Lab pages. Viewing is open to all logged-in users; changes are limited to lab managers (see SecurityConfig). */
@Controller
@RequestMapping("/labs")
public class LabController {

    private final InventoryService inventory;

    public LabController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("labs", inventory.allLabs());
        return "labs/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("lab", new Lab());
        return "labs/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("lab", inventory.getLab(id));
        return "labs/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("lab") Lab lab, BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return "labs/form";
        }
        try {
            inventory.saveLab(lab);
        } catch (DataIntegrityViolationException e) {
            errors.rejectValue("name", "duplicate", "A lab with that name already exists");
            return "labs/form";
        }
        redirect.addFlashAttribute("message", "Lab saved.");
        return "redirect:/labs";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            inventory.deleteLab(id);
            redirect.addFlashAttribute("message", "Lab deleted.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/labs";
    }
}
