package edu.campbellsville.lablink.service;

import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.model.Reservation;
import edu.campbellsville.lablink.model.ReservationStatus;
import edu.campbellsville.lablink.model.Role;
import edu.campbellsville.lablink.model.User;
import edu.campbellsville.lablink.repository.LabRepository;
import edu.campbellsville.lablink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the reservation business rules against the real H2 database.
 * @Transactional rolls back after each test, so tests don't affect each other.
 */
@SpringBootTest
@Transactional
class ReservationServiceTest {

    @Autowired ReservationService reservationService;
    @Autowired UserRepository userRepository;
    @Autowired LabRepository labRepository;

    User student;
    User faculty;
    User manager;
    Lab lab;
    LocalDateTime tomorrow9am;

    @BeforeEach
    void setUp() {
        student = userRepository.save(new User("test.student@example.edu", "Test Student", "x", Role.STUDENT));
        faculty = userRepository.save(new User("test.faculty@example.edu", "Test Faculty", "x", Role.FACULTY));
        manager = userRepository.save(new User("test.manager@example.edu", "Test Manager", "x", Role.LAB_MANAGER));
        lab = labRepository.save(new Lab("Test Lab", "Test Hall", "T1", 10, null));
        tomorrow9am = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    void studentRequestStartsPending() {
        Reservation r = reservationService.requestLab(student, lab.getId(), tomorrow9am, tomorrow9am.plusHours(2), "Study group");
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void facultyRequestIsApprovedAutomatically() {
        Reservation r = reservationService.requestLab(faculty, lab.getId(), tomorrow9am, tomorrow9am.plusHours(1), "Lecture");
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.APPROVED);
    }

    @Test
    void overlappingRequestIsRejected() {
        reservationService.requestLab(student, lab.getId(), tomorrow9am, tomorrow9am.plusHours(2), "First");
        // 10:00-11:00 overlaps 9:00-11:00
        assertThatThrownBy(() -> reservationService.requestLab(
                faculty, lab.getId(), tomorrow9am.plusHours(1), tomorrow9am.plusHours(3), "Second"))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    void backToBackRequestsAreAllowed() {
        reservationService.requestLab(student, lab.getId(), tomorrow9am, tomorrow9am.plusHours(1), "First");
        Reservation second = reservationService.requestLab(
                faculty, lab.getId(), tomorrow9am.plusHours(1), tomorrow9am.plusHours(2), "Second");
        assertThat(second.getId()).isNotNull();
    }

    @Test
    void cancelledReservationFreesTheSlot() {
        Reservation first = reservationService.requestLab(student, lab.getId(), tomorrow9am, tomorrow9am.plusHours(1), "First");
        reservationService.cancel(first.getId(), student);
        Reservation second = reservationService.requestLab(faculty, lab.getId(), tomorrow9am, tomorrow9am.plusHours(1), "Second");
        assertThat(second.getStatus()).isEqualTo(ReservationStatus.APPROVED);
    }

    @Test
    void endMustBeAfterStart() {
        assertThatThrownBy(() -> reservationService.requestLab(
                student, lab.getId(), tomorrow9am, tomorrow9am.minusHours(1), "Backwards"))
                .isInstanceOf(ReservationException.class);
    }

    @Test
    void pastReservationsAreRejected() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        assertThatThrownBy(() -> reservationService.requestLab(
                student, lab.getId(), yesterday, yesterday.plusHours(1), "Too late"))
                .isInstanceOf(ReservationException.class);
    }

    @Test
    void managerCanApprovePendingRequest() {
        Reservation r = reservationService.requestLab(student, lab.getId(), tomorrow9am, tomorrow9am.plusHours(1), "Study");
        reservationService.approve(r.getId(), manager);
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.APPROVED);
        assertThat(r.getReviewedBy()).isEqualTo(manager);
    }

    @Test
    void studentCannotApprove() {
        Reservation r = reservationService.requestLab(student, lab.getId(), tomorrow9am, tomorrow9am.plusHours(1), "Study");
        assertThatThrownBy(() -> reservationService.approve(r.getId(), student))
                .isInstanceOf(ReservationException.class);
    }

    @Test
    void onlyOwnerCanCancel() {
        Reservation r = reservationService.requestLab(student, lab.getId(), tomorrow9am, tomorrow9am.plusHours(1), "Study");
        assertThatThrownBy(() -> reservationService.cancel(r.getId(), faculty))
                .isInstanceOf(ReservationException.class);
    }
}
