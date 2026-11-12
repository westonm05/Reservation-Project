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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Lab pages. Viewing is open to all logged-in users; changes are limited to lab managers (see SecurityConfig). */
@Controller
@RequestMapping("/labs")
public class LabController {

    private static final String FORM_VIEW = "labs/form";

    private final InventoryService inventory;

    public LabController(InventoryService inventory) {
        this.inventory = inventory;
    }

    /** Lab list with optional search filters (backlog 3.8), e.g. /labs?q=science&departmentId=1&activeOnly=true */
    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Long departmentId,
                       @RequestParam(defaultValue = "false") boolean activeOnly,
                       Model model) {
        model.addAttribute("labs", inventory.searchLabs(q, departmentId, activeOnly));
        model.addAttribute("departments", inventory.allDepartments());
        model.addAttribute("q", q);
        model.addAttribute("departmentId", departmentId);
        model.addAttribute("activeOnly", activeOnly);
        return "labs/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("lab", new Lab());
        model.addAttribute("departments", inventory.allDepartments());
        return FORM_VIEW;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("lab", inventory.getLab(id));
        model.addAttribute("departments", inventory.allDepartments());
        return FORM_VIEW;
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("lab") Lab lab, BindingResult errors,
                       @RequestParam(required = false) Long departmentId, Model model, RedirectAttributes redirect) {
        // The dropdown sends the department's id; look up the Department it refers to.
        lab.setDepartment(inventory.getDepartment(departmentId));
        model.addAttribute("departments", inventory.allDepartments());
        if (errors.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            inventory.saveLab(lab);
        } catch (DataIntegrityViolationException e) {
            errors.rejectValue("name", "duplicate", "A lab with that name already exists");
            return FORM_VIEW;
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
