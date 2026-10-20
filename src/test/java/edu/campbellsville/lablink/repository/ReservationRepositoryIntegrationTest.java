package edu.campbellsville.lablink.repository;

import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.model.Reservation;
import edu.campbellsville.lablink.model.ReservationStatus;
import edu.campbellsville.lablink.model.Role;
import edu.campbellsville.lablink.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * INTEGRATION tests for the data access layer: the repository's generated SQL running
 * against the real H2 database built from db/schema.sql.
 *
 * @DataJpaTest starts only the JPA part of the app (no web layer, no services), and rolls
 * back each test. replace = NONE keeps our configured H2 database (MySQL mode) instead of
 * swapping in a generic one, so the same DDL that production uses is what's tested.
 */
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReservationRepositoryIntegrationTest {

    @Autowired ReservationRepository reservations;
    @Autowired UserRepository users;
    @Autowired LabRepository labs;

    User student;
    Lab lab;
    Lab otherLab;
    LocalDateTime nine;

    @BeforeEach
    void setUp() {
        student = users.save(new User("repo.student@example.edu", "Repo Student", "x", Role.STUDENT));
        lab = labs.save(new Lab("Repo Test Lab", "Repo Hall", "R1", 10, null));
        otherLab = labs.save(new Lab("Repo Other Lab", "Repo Hall", "R2", 10, null));
        nine = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    void overlapQueryDetectsPartialOverlap() {
        save(lab, nine, nine.plusHours(2), ReservationStatus.APPROVED); // 9:00-11:00

        // asking for 10:00-12:00
        boolean taken = reservations.existsByLabAndStatusInAndStartTimeBeforeAndEndTimeAfter(
                lab, ReservationStatus.ACTIVE, nine.plusHours(3), nine.plusHours(1));

        assertThat(taken).isTrue();
    }

    @Test
    void overlapQueryAllowsBackToBackSlots() {
        save(lab, nine, nine.plusHours(2), ReservationStatus.APPROVED); // 9:00-11:00

        // asking for 11:00-12:00: touches but doesn't overlap
        boolean taken = reservations.existsByLabAndStatusInAndStartTimeBeforeAndEndTimeAfter(
                lab, ReservationStatus.ACTIVE, nine.plusHours(3), nine.plusHours(2));

        assertThat(taken).isFalse();
    }

    @Test
    void overlapQueryIgnoresCancelledAndRejectedReservations() {
        save(lab, nine, nine.plusHours(2), ReservationStatus.CANCELLED);
        save(lab, nine, nine.plusHours(2), ReservationStatus.REJECTED);

        boolean taken = reservations.existsByLabAndStatusInAndStartTimeBeforeAndEndTimeAfter(
                lab, ReservationStatus.ACTIVE, nine.plusHours(2), nine);

        assertThat(taken).isFalse();
    }

    @Test
    void overlapQueryOnlyLooksAtTheRequestedLab() {
        save(otherLab, nine, nine.plusHours(2), ReservationStatus.APPROVED);

        boolean taken = reservations.existsByLabAndStatusInAndStartTimeBeforeAndEndTimeAfter(
                lab, ReservationStatus.ACTIVE, nine.plusHours(2), nine);

        assertThat(taken).isFalse();
    }

    @Test
    void overlapQueryCanSkipTheReservationBeingRescheduled() {
        Reservation mine = save(lab, nine, nine.plusHours(2), ReservationStatus.PENDING);

        boolean takenBySomeoneElse = reservations.existsByLabAndStatusInAndStartTimeBeforeAndEndTimeAfterAndIdNot(
                lab, ReservationStatus.ACTIVE, nine.plusHours(3), nine.plusHours(1), mine.getId());

        assertThat(takenBySomeoneElse).isFalse();
    }

    @Test
    void userReservationsAreListedNewestFirst() {
        Reservation early = save(lab, nine, nine.plusHours(1), ReservationStatus.PENDING);
        Reservation late = save(lab, nine.plusDays(1), nine.plusDays(1).plusHours(1), ReservationStatus.PENDING);

        assertThat(reservations.findByUserOrderByStartTimeDesc(student)).containsExactly(late, early);
    }

    @Test
    void databaseRejectsReservationWithNeitherLabNorEquipment() {
        Reservation orphan = new Reservation();
        orphan.setUser(student);
        orphan.setStartTime(nine);
        orphan.setEndTime(nine.plusHours(1));
        orphan.setPurpose("Nothing reserved");

        // The CHECK constraint in schema.sql is a second line of defense behind the service layer
        assertThatThrownBy(() -> reservations.saveAndFlush(orphan))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Reservation save(Lab target, LocalDateTime start, LocalDateTime end, ReservationStatus status) {
        Reservation r = new Reservation();
        r.setUser(student);
        r.setLab(target);
        r.setStartTime(start);
        r.setEndTime(end);
        r.setPurpose("Repository test");
        r.setStatus(status);
        return reservations.save(r);
    }
}
