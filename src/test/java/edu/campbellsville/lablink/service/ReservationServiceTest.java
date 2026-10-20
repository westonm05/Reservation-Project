package edu.campbellsville.lablink.service;

import edu.campbellsville.lablink.model.Equipment;
import edu.campbellsville.lablink.model.EquipmentStatus;
import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.model.Reservation;
import edu.campbellsville.lablink.model.ReservationStatus;
import edu.campbellsville.lablink.model.Role;
import edu.campbellsville.lablink.model.User;
import edu.campbellsville.lablink.repository.EquipmentRepository;
import edu.campbellsville.lablink.repository.LabRepository;
import edu.campbellsville.lablink.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UNIT tests for ReservationService.
 *
 * Unlike ReservationServiceIntegrationTest, these don't start Spring or touch a database.
 * The three repositories are replaced with Mockito "mocks": fake objects whose answers we
 * script with when(...).thenReturn(...). That isolates the business rules in the service,
 * so a failure here means the rule itself is wrong, and each test runs in milliseconds.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock ReservationRepository reservationRepository;
    @Mock LabRepository labRepository;
    @Mock EquipmentRepository equipmentRepository;

    @InjectMocks ReservationService service; // built with the three mocks above

    User student;
    User faculty;
    User manager;
    Lab lab;
    Equipment printer;
    LocalDateTime start;
    LocalDateTime end;

    @BeforeEach
    void setUp() {
        student = user(1L, Role.STUDENT);
        faculty = user(2L, Role.FACULTY);
        manager = user(3L, Role.LAB_MANAGER);

        lab = new Lab("Robotics Lab", "Science Building", "118", 12, null);
        lab.setId(10L);

        printer = new Equipment("3D Printer", "PRN-0001", "Prusa MK4", lab);
        ReflectionTestUtils.setField(printer, "id", 20L);

        start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        end = start.plusHours(2);
    }

    // ------------------------------------------------------------------ lab requests

    @Nested
    class RequestLab {

        @Test
        void studentRequestIsSavedAsPending() {
            labExists();
            saveReturnsArgument();

            Reservation r = service.requestLab(student, 10L, start, end, "Study group");

            assertThat(r.getStatus()).isEqualTo(ReservationStatus.PENDING);
            assertThat(r.getUser()).isEqualTo(student);
            assertThat(r.getLab()).isEqualTo(lab);
            assertThat(r.getEquipment()).isNull();
            assertThat(r.getPurpose()).isEqualTo("Study group");
            verify(reservationRepository).save(r);
        }

        @Test
        void facultyRequestIsApprovedAutomatically() {
            labExists();
            saveReturnsArgument();

            Reservation r = service.requestLab(faculty, 10L, start, end, "Lecture");

            assertThat(r.getStatus()).isEqualTo(ReservationStatus.APPROVED);
        }

        @Test
        void labManagerRequestIsApprovedAutomatically() {
            labExists();
            saveReturnsArgument();

            Reservation r = service.requestLab(manager, 10L, start, end, "Maintenance");

            assertThat(r.getStatus()).isEqualTo(ReservationStatus.APPROVED);
        }

        @Test
        void doubleBookingCheckOnlyCountsPendingAndApprovedReservations() {
            labExists();
            saveReturnsArgument();

            service.requestLab(student, 10L, start, end, "Study");

            // The overlap query receives (requested END, requested START) in that order:
            // existing.start < requested.end AND existing.end > requested.start
            verify(reservationRepository).existsByLabAndStatusInAndStartTimeBeforeAndEndTimeAfter(
                    lab, ReservationStatus.ACTIVE, end, start);
        }

        @Test
        void unknownLabIsRejected() {
            when(labRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.requestLab(student, 99L, start, end, "Study"))
                    .isInstanceOf(ReservationException.class)
                    .hasMessageContaining("does not exist");
            verify(reservationRepository, never()).save(any());
        }

        @Test
        void inactiveLabIsRejected() {
            lab.setActive(false);
            labExists();

            assertThatThrownBy(() -> service.requestLab(student, 10L, start, end, "Study"))
                    .isInstanceOf(ReservationException.class)
                    .hasMessageContaining("not currently available");
            verify(reservationRepository, never()).save(any());
        }

        @Test
        void missingStartTimeIsRejected() {
            labExists();

            assertThatThrownBy(() -> service.requestLab(student, 10L, null, end, "Study"))
                    .isInstanceOf(ReservationException.class)
                    .hasMessageContaining("required");
        }

        @Test
        void zeroLengthReservationIsRejected() {
            labExists();

            // Edge case: end == start is not "after" start
            assertThatThrownBy(() -> service.requestLab(student, 10L, start, start, "Study"))
                    .isInstanceOf(ReservationException.class)
                    .hasMessageContaining("End time must be after start time");
        }

        @Test
        void reservationInThePastIsRejected() {
            labExists();
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

            assertThatThrownBy(() -> service.requestLab(student, 10L, yesterday, yesterday.plusHours(1), "Study"))
                    .isInstanceOf(ReservationException.class)
                    .hasMessageContaining("future");
        }

        @Test
        void overlappingReservationIsRejected() {
            labExists();
            when(reservationRepository.existsByLabAndStatusInAndStartTimeBeforeAndEndTimeAfter(
                    lab, ReservationStatus.ACTIVE, end, start)).thenReturn(true);

            assertThatThrownBy(() -> service.requestLab(student, 10L, start, end, "Study"))
                    .isInstanceOf(ReservationException.class)
                    .hasMessage("Robotics Lab is already booked during that time");
            verify(reservationRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------ equipment requests

    @Nested
    class RequestEquipment {

        @Test
        void availableEquipmentIsReserved() {
            when(equipmentRepository.findById(20L)).thenReturn(Optional.of(printer));
            saveReturnsArgument();

            Reservation r = service.requestEquipment(student, 20L, start, end, "Prototype");

            assertThat(r.getEquipment()).isEqualTo(printer);
            assertThat(r.getLab()).isNull();
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.PENDING);
        }

        @Test
        void equipmentUnderMaintenanceIsRejected() {
            printer.setStatus(EquipmentStatus.MAINTENANCE);
            when(equipmentRepository.findById(20L)).thenReturn(Optional.of(printer));

            assertThatThrownBy(() -> service.requestEquipment(student, 20L, start, end, "Prototype"))
                    .isInstanceOf(ReservationException.class)
                    .hasMessageContaining("MAINTENANCE");
        }

        @Test
        void unknownEquipmentIsRejected() {
            when(equipmentRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.requestEquipment(student, 404L, start, end, "Prototype"))
                    .isInstanceOf(ReservationException.class)
                    .hasMessageContaining("does not exist");
        }

        @Test
        void overlappingEquipmentReservationIsRejected() {
            when(equipmentRepository.findById(20L)).thenReturn(Optional.of(printer));
            when(reservationRepository.existsByEquipmentAndStatusInAndStartTimeBeforeAndEndTimeAfter(
                    printer, ReservationStatus.ACTIVE, end, start)).thenReturn(true);

            assertThatThrownBy(() -> service.requestEquipment(student, 20L, start, end, "Prototype"))
                    .isInstanceOf(ReservationException.class)
                    .hasMessageContaining("already booked");
            verify(reservationRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------ approve / reject

    @Nested
    class Review {

        @Test
        void managerApprovesPendingRequest() {
            Reservation r = existingReservation(ReservationStatus.PENDING, student);

            service.approve(50L, manager);

            assertThat(r.getStatus()).isEqualTo(ReservationStatus.APPROVED);
            assertThat(r.getReviewedBy()).isEqualTo(manager);
            assertThat(r.getReviewedAt()).isNotNull();
        }

        @Test
        void managerRejectsPendingRequest() {
            Reservation r = existingReservation(ReservationStatus.PENDING, student);

            service.reject(50L, manager);

            assertThat(r.getStatus()).isEqualTo(ReservationStatus.REJECTED);
            assertThat(r.getReviewedBy()).isEqualTo(manager);
        }

        @Test
        void facultyCannotApprove() {
            assertThatThrownBy(() -> service.approve(50L, faculty))
                    .isInstanceOf(ReservationException.class)
                    .hasMessageContaining("Only lab managers");
            // The role check happens before the database is even asked
            verify(reservationRepository, never()).findById(anyLong());
        }

        @Test
        void alreadyReviewedRequestCannotBeReviewedAgain() {
            existingReservation(ReservationStatus.APPROVED, student);

            assertThatThrownBy(() -> service.reject(50L, manager))
                    .isInstanceOf(ReservationException.class)
                    .hasMessageContaining("already been reviewed");
        }

        @Test
        void missingReservationIsReported() {
            when(reservationRepository.findById(50L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.approve(50L, manager))
                    .isInstanceOf(ReservationException.class)
                    .hasMessage("Reservation not found");
        }
    }

    // ------------------------------------------------------------------ cancel

    @Nested
    class Cancel {

        @Test
        void ownerCancelsPendingReservation() {
            Reservation r = existingReservation(ReservationStatus.PENDING, student);

            service.cancel(50L, student);

            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        void ownerCancelsApprovedReservation() {
            Reservation r = existingReservation(ReservationStatus.APPROVED, student);

            service.cancel(50L, student);

            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        void ownerIsMatchedByIdNotByObject() {
            // Edge case: a different User object loaded for the same account must still count as the owner
            Reservation r = existingReservation(ReservationStatus.PENDING, student);
            User sameStudentReloaded = user(1L, Role.STUDENT);

            service.cancel(50L, sameStudentReloaded);

            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        void someoneElsesReservationCannotBeCancelled() {
            Reservation r = existingReservation(ReservationStatus.PENDING, student);

            assertThatThrownBy(() -> service.cancel(50L, faculty))
                    .isInstanceOf(ReservationException.class)
                    .hasMessageContaining("your own");
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.PENDING);
        }

        @Test
        void rejectedReservationCannotBeCancelled() {
            existingReservation(ReservationStatus.REJECTED, student);

            assertThatThrownBy(() -> service.cancel(50L, student))
                    .isInstanceOf(ReservationException.class)
                    .hasMessageContaining("Only pending or approved");
        }
    }

    // ------------------------------------------------------------------ reschedule (written test-first)

    @Nested
    class Reschedule {

        @Test
        void ownerMovesPendingReservationToANewTime() {
            Reservation r = existingReservation(ReservationStatus.PENDING, student);
            LocalDateTime newStart = start.plusDays(1);
            LocalDateTime newEnd = newStart.plusHours(3);

            Reservation moved = service.reschedule(50L, student, newStart, newEnd);

            assertThat(moved).isSameAs(r);
            assertThat(r.getStartTime()).isEqualTo(newStart);
            assertThat(r.getEndTime()).isEqualTo(newEnd);
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.PENDING); // still needs approval
        }

        @Test
        void reservationDoesNotConflictWithItsOwnCurrentSlot() {
            // Moving 9-11 to 10-12 overlaps the reservation's own old slot; that must be allowed
            existingReservation(ReservationStatus.PENDING, student);

            service.reschedule(50L, student, start.plusHours(1), end.plusHours(1));

            verify(reservationRepository).existsByLabAndStatusInAndStartTimeBeforeAndEndTimeAfterAndIdNot(
                    lab, ReservationStatus.ACTIVE, end.plusHours(1), start.plusHours(1), 50L);
        }

        @Test
        void conflictWithAnotherReservationIsRejectedAndTimesStayTheSame() {
            Reservation r = existingReservation(ReservationStatus.PENDING, student);
            LocalDateTime newStart = start.plusHours(4);
            LocalDateTime newEnd = newStart.plusHours(1);
            when(reservationRepository.existsByLabAndStatusInAndStartTimeBeforeAndEndTimeAfterAndIdNot(
                    lab, ReservationStatus.ACTIVE, newEnd, newStart, 50L)).thenReturn(true);

            assertThatThrownBy(() -> service.reschedule(50L, student, newStart, newEnd))
                    .isInstanceOf(ReservationException.class)
                    .hasMessage("Robotics Lab is already booked during that time");
            assertThat(r.getStartTime()).isEqualTo(start);
            assertThat(r.getEndTime()).isEqualTo(end);
        }

        @Test
        void equipmentReservationChecksEquipmentAvailability() {
            Reservation r = existingReservation(ReservationStatus.PENDING, student);
            r.setLab(null);
            r.setEquipment(printer);
            LocalDateTime newStart = start.plusDays(2);

            service.reschedule(50L, student, newStart, newStart.plusHours(1));

            verify(reservationRepository).existsByEquipmentAndStatusInAndStartTimeBeforeAndEndTimeAfterAndIdNot(
                    printer, ReservationStatus.ACTIVE, newStart.plusHours(1), newStart, 50L);
            assertThat(r.getStartTime()).isEqualTo(newStart);
        }

        @Test
        void onlyTheOwnerCanReschedule() {
            existingReservation(ReservationStatus.PENDING, student);

            assertThatThrownBy(() -> service.reschedule(50L, faculty, start.plusDays(1), end.plusDays(1)))
                    .isInstanceOf(ReservationException.class)
                    .hasMessage("You can only reschedule your own reservations");
        }

        @Test
        void approvedReservationCannotBeRescheduled() {
            existingReservation(ReservationStatus.APPROVED, student);

            assertThatThrownBy(() -> service.reschedule(50L, student, start.plusDays(1), end.plusDays(1)))
                    .isInstanceOf(ReservationException.class)
                    .hasMessage("Only pending reservations can be rescheduled");
        }

        @Test
        void newTimesFollowTheSameRulesAsANewRequest() {
            Reservation r = existingReservation(ReservationStatus.PENDING, student);

            assertThatThrownBy(() -> service.reschedule(50L, student, end, start)) // backwards
                    .isInstanceOf(ReservationException.class)
                    .hasMessageContaining("End time must be after start time");
            assertThat(r.getStartTime()).isEqualTo(start);
        }

        @Test
        void missingReservationIsReported() {
            when(reservationRepository.findById(50L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.reschedule(50L, student, start, end))
                    .isInstanceOf(ReservationException.class)
                    .hasMessage("Reservation not found");
        }
    }

    // ------------------------------------------------------------------ queries

    @Test
    void pendingCountComesFromRepository() {
        when(reservationRepository.countByStatus(ReservationStatus.PENDING)).thenReturn(4L);

        assertThat(service.pendingCount()).isEqualTo(4L);
    }

    // ------------------------------------------------------------------ helpers

    private static User user(Long id, Role role) {
        User u = new User(role.name().toLowerCase() + "@example.edu", role.name(), "hash", role);
        ReflectionTestUtils.setField(u, "id", id); // normally the database assigns the id
        return u;
    }

    private void labExists() {
        when(labRepository.findById(10L)).thenReturn(Optional.of(lab));
    }

    private void saveReturnsArgument() {
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(call -> call.getArgument(0));
    }

    private Reservation existingReservation(ReservationStatus status, User owner) {
        Reservation r = new Reservation();
        ReflectionTestUtils.setField(r, "id", 50L);
        r.setUser(owner);
        r.setLab(lab);
        r.setStartTime(start);
        r.setEndTime(end);
        r.setPurpose("Existing");
        r.setStatus(status);
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(r));
        return r;
    }
}
