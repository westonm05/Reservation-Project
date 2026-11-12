package edu.campbellsville.lablink.web;

import edu.campbellsville.lablink.model.Role;
import edu.campbellsville.lablink.service.UserService;
import edu.campbellsville.lablink.web.form.NewAccountForm;
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

/**
 * User administration for lab managers (backlog 3.4): list accounts, add faculty and lab-manager
 * accounts, and change roles. SecurityConfig limits /admin/** to the LAB_MANAGER role.
 */
@Controller
@RequestMapping("/admin/users")
public class AdminController {

    private static final String LIST_REDIRECT = "redirect:/admin/users";
    private static final String NEW_VIEW = "admin/new-user";

    private final UserService users;

    public AdminController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", users.allUsers());
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam Role role, Principal principal,
                             RedirectAttributes redirect) {
        try {
            users.changeRole(id, role, principal.getName());
            redirect.addFlashAttribute("message", "Role updated.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return LIST_REDIRECT;
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new NewAccountForm());
        model.addAttribute("roles", Role.values());
        return NEW_VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") NewAccountForm form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                users.createAccount(form.getFullName(), form.getEmail(), form.getPassword(), form.getRole());
                redirect.addFlashAttribute("message", "Account created for " + form.getEmail().trim().toLowerCase() + ".");
                return LIST_REDIRECT;
            } catch (IllegalArgumentException e) {
                errors.rejectValue("email", "duplicate", e.getMessage());
            }
        }
        model.addAttribute("roles", Role.values());
        return NEW_VIEW;
    }
}
