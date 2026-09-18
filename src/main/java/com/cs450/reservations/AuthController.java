package com.cs450.reservations;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller // handles web requests and returns page names
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login") // someone visits /login
    public String showLoginPage() {
        return "login"; // means: render templates/login.html
    }

    @GetMapping("/register") // someone visits /register
    public String showRegisterPage() {
        return "register";
    }

    @PostMapping("/register") // someone submits the register form
    public String register(@RequestParam String email, // pulls each field out of the form
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           @RequestParam String fullName,
                           Model model) { // Model carries data back to the page

        if (userService.emailTaken(email)) {
            model.addAttribute("error", "An account with this email already exists.");
            return "register"; // back to the form with the message
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "register";
        }

        if (password.length() < 8) {
            model.addAttribute("error", "Password must be at least 8 characters.");
            return "register";
        }

        userService.register(email, password, fullName);
        return "redirect:/login?registered"; // US-1: send them to login after signing up
    }
}
