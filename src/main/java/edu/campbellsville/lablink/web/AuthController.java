package edu.campbellsville.lablink.web;

import edu.campbellsville.lablink.service.UserService;
import edu.campbellsville.lablink.web.form.RegistrationForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Login page and student self-registration. (The login POST itself is handled by Spring Security.) */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "login"; // renders templates/login.html
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form,
                           BindingResult errors,
                           RedirectAttributes redirect) {
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (form.getEmail() != null && userService.emailTaken(form.getEmail())) {
            errors.rejectValue("email", "taken", "An account with that email already exists");
        }
        if (errors.hasErrors()) {
            return "register"; // show the form again with error messages
        }
        userService.registerStudent(form.getFullName(), form.getEmail(), form.getPassword());
        redirect.addFlashAttribute("message", "Account created. Please log in.");
        return "redirect:/login";
    }
}
