package edu.campbellsville.lablink.web;

import edu.campbellsville.lablink.model.Role;
import edu.campbellsville.lablink.model.User;
import edu.campbellsville.lablink.service.ReservationService;
import edu.campbellsville.lablink.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

/** Dashboard shown after login. */
@Controller
public class HomeController {

    private final UserService userService;
    private final ReservationService reservationService;

    public HomeController(UserService userService, ReservationService reservationService) {
        this.userService = userService;
        this.reservationService = reservationService;
    }

    @GetMapping("/")
    public String home(Principal principal, Model model) {
        User user = userService.getByEmail(principal.getName());
        model.addAttribute("user", user);
        model.addAttribute("myReservations", reservationService.forUser(user));
        if (user.getRole() == Role.LAB_MANAGER) {
            model.addAttribute("pendingCount", reservationService.pendingCount());
        }
        return "index";
    }
}
