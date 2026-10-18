package edu.campbellsville.lablink.web;

import edu.campbellsville.lablink.model.Equipment;
import edu.campbellsville.lablink.model.EquipmentStatus;
import edu.campbellsville.lablink.service.InventoryService;
import edu.campbellsville.lablink.web.form.EquipmentForm;
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

@Controller
@RequestMapping("/equipment")
public class EquipmentController {

    private final InventoryService inventory;

    public EquipmentController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("equipmentList", inventory.allEquipment());
        return "equipment/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        return showForm(new EquipmentForm(), model);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return showForm(EquipmentForm.from(inventory.getEquipment(id)), model);
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") EquipmentForm form, BindingResult errors,
                       Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return showForm(form, model);
        }
        Equipment equipment = form.getId() == null ? new Equipment() : inventory.getEquipment(form.getId());
        equipment.setName(form.getName());
        equipment.setSerialNumber(form.getSerialNumber());
        equipment.setDescription(form.getDescription());
        equipment.setStatus(form.getStatus());
        equipment.setLab(form.getLabId() == null ? null : inventory.getLab(form.getLabId()));
        try {
            inventory.saveEquipment(equipment);
        } catch (DataIntegrityViolationException e) {
            errors.rejectValue("serialNumber", "duplicate", "That serial number is already registered");
            return showForm(form, model);
        }
        redirect.addFlashAttribute("message", "Equipment saved.");
        return "redirect:/equipment";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            inventory.deleteEquipment(id);
            redirect.addFlashAttribute("message", "Equipment deleted.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/equipment";
    }

    private String showForm(EquipmentForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("labs", inventory.allLabs());
        model.addAttribute("statuses", EquipmentStatus.values());
        return "equipment/form";
    }
}
