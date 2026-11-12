package edu.campbellsville.lablink.web;

import edu.campbellsville.lablink.service.UserService;
import edu.campbellsville.lablink.web.form.PasswordChangeForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** "My account" page: shows the logged-in user's details and lets them change their password. */
@Controller
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/account")
    public String account(Principal principal, Model model) {
        model.addAttribute("user", userService.getByEmail(principal.getName()));
        model.addAttribute("form", new PasswordChangeForm());
        return "account";
    }

    @PostMapping("/account/password")
    public String changePassword(@Valid @ModelAttribute("form") PasswordChangeForm form,
                                 BindingResult errors,
                                 Principal principal,
                                 Model model,
                                 RedirectAttributes redirect) {
        if (form.getNewPassword() != null && !form.getNewPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (!errors.hasErrors()) {
            try {
                userService.changePassword(principal.getName(), form.getCurrentPassword(), form.getNewPassword());
                redirect.addFlashAttribute("message", "Password changed.");
                return "redirect:/account";
            } catch (IllegalArgumentException e) {
                // wrong current password, or new one is the same as the old one
                String field = e.getMessage().startsWith("Current") ? "currentPassword" : "newPassword";
                errors.rejectValue(field, "invalid", e.getMessage());
            }
        }
        model.addAttribute("user", userService.getByEmail(principal.getName()));
        return "account"; // show the page again with error messages
    }
}
