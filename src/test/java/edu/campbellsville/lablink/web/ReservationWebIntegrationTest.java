package edu.campbellsville.lablink.web;

import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.model.Reservation;
import edu.campbellsville.lablink.model.ReservationStatus;
import edu.campbellsville.lablink.model.User;
import edu.campbellsville.lablink.repository.LabRepository;
import edu.campbellsville.lablink.repository.ReservationRepository;
import edu.campbellsville.lablink.repository.UserRepository;
import edu.campbellsville.lablink.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * INTEGRATION tests for the reservation workflow from the HTTP request all the way to the
 * database: Spring Security -> ReservationController -> ReservationService -> repositories -> H2.
 *
 * The users come from DevDataSeeder (student@, faculty@, manager@lablink.edu). @WithMockUser
 * logs a request in as one of them without going through the login page. @Transactional undoes
 * every database change after each test.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationWebIntegrationTest {

    private static final DateTimeFormatter HTML_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Autowired MockMvc mvc;
    @Autowired ReservationService reservationService;
    @Autowired ReservationRepository reservationRepository;
    @Autowired UserRepository userRepository;
    @Autowired LabRepository labRepository;

    User student;
    User faculty;
    User manager;
    Lab lab;
    LocalDateTime start;

    @BeforeEach
    void setUp() {
        student = userRepository.findByEmail("student@lablink.edu").orElseThrow();
        faculty = userRepository.findByEmail("faculty@lablink.edu").orElseThrow();
        manager = userRepository.findByEmail("manager@lablink.edu").orElseThrow();
        lab = labRepository.save(new Lab("Web Test Lab", "Web Hall", "W1", 10, null));
        start = LocalDateTime.now().plusDays(3).withHour(13).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    @WithMockUser(username = "student@lablink.edu", roles = "STUDENT")
    void submittingTheFormSavesAPendingReservation() throws Exception {
        mvc.perform(post("/reservations").with(csrf())
                        .param("labId", lab.getId().toString())
                        .param("startTime", start.format(HTML_DATETIME))
                        .param("endTime", start.plusHours(2).format(HTML_DATETIME))
                        .param("purpose", "Capstone meeting"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations"))
                .andExpect(flash().attribute("message", "Reservation submitted."));

        List<Reservation> saved = reservationRepository.findByUserOrderByStartTimeDesc(student).stream()
                .filter(r -> r.getLab() != null && r.getLab().getId().equals(lab.getId()))
                .toList();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(saved.get(0).getStartTime()).isEqualTo(start);
    }

    @Test
    @WithMockUser(username = "student@lablink.edu", roles = "STUDENT")
    void doubleBookingShowsTheErrorOnTheFormAndSavesNothing() throws Exception {
        reservationService.requestLab(faculty, lab.getId(), start, start.plusHours(2), "Lecture");
        long before = reservationRepository.count();

        mvc.perform(post("/reservations").with(csrf())
                        .param("labId", lab.getId().toString())
                        .param("startTime", start.plusHours(1).format(HTML_DATETIME))
                        .param("endTime", start.plusHours(3).format(HTML_DATETIME))
                        .param("purpose", "Conflicting request"))
                .andExpect(status().isOk()) // form shown again, not a redirect
                .andExpect(model().attribute("error", containsString("already booked")));

        assertThat(reservationRepository.count()).isEqualTo(before);
    }

    @Test
    @WithMockUser(username = "manager@lablink.edu", roles = "LAB_MANAGER")
    void managerApprovalIsStoredWithReviewer() throws Exception {
        Reservation pending = reservationService.requestLab(student, lab.getId(), start, start.plusHours(1), "Study");

        mvc.perform(post("/reservations/{id}/approve", pending.getId()).with(csrf()))
                .andExpect(redirectedUrl("/reservations/manage"))
                .andExpect(flash().attribute("message", "Reservation approved."));

        Reservation reloaded = reservationRepository.findById(pending.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ReservationStatus.APPROVED);
        assertThat(reloaded.getReviewedBy().getId()).isEqualTo(manager.getId());
    }

    @Test
    @WithMockUser(username = "student@lablink.edu", roles = "STUDENT")
    void studentIsForbiddenFromApprovingAndNothingChanges() throws Exception {
        Reservation pending = reservationService.requestLab(student, lab.getId(), start, start.plusHours(1), "Study");

        mvc.perform(post("/reservations/{id}/approve", pending.getId()).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(reservationRepository.findById(pending.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    @WithMockUser(username = "student@lablink.edu", roles = "STUDENT")
    void ownerCanCancelThroughTheWebAndTheSlotOpensUp() throws Exception {
        Reservation mine = reservationService.requestLab(student, lab.getId(), start, start.plusHours(1), "Study");

        mvc.perform(post("/reservations/{id}/cancel", mine.getId()).with(csrf()))
                .andExpect(redirectedUrl("/reservations"))
                .andExpect(flash().attribute("message", "Reservation cancelled."));

        assertThat(reservationRepository.findById(mine.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservationRepository.existsByLabAndStatusInAndStartTimeBeforeAndEndTimeAfter(
                lab, ReservationStatus.ACTIVE, start.plusHours(1), start)).isFalse();
    }

    @Test
    @WithMockUser(username = "faculty@lablink.edu", roles = "FACULTY")
    void userCannotCancelSomeoneElsesReservation() throws Exception {
        Reservation studentsBooking = reservationService.requestLab(student, lab.getId(), start, start.plusHours(1), "Study");

        mvc.perform(post("/reservations/{id}/cancel", studentsBooking.getId()).with(csrf()))
                .andExpect(redirectedUrl("/reservations"))
                .andExpect(flash().attribute("error", containsString("your own")));

        assertThat(reservationRepository.findById(studentsBooking.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.PENDING);
    }
}
