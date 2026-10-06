package edu.campbellsville.lablink.reservation;

import edu.campbellsville.lablink.auth.Role;
import edu.campbellsville.lablink.auth.User;
import edu.campbellsville.lablink.auth.UserRepository;
import edu.campbellsville.lablink.inventory.ApprovalRule;
import edu.campbellsville.lablink.inventory.Resource;
import edu.campbellsville.lablink.inventory.ResourceRepository;
import edu.campbellsville.lablink.inventory.ResourceType;
import edu.campbellsville.lablink.reservation.approval.ApprovalPolicyResolver;
import edu.campbellsville.lablink.reservation.approval.AutoApprovalPolicy;
import edu.campbellsville.lablink.reservation.approval.FacultyOnlyPolicy;
import edu.campbellsville.lablink.reservation.approval.ManagerReviewPolicy;
import edu.campbellsville.lablink.reservation.approval.TrainingRequiredPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Shows the payoff of Dependency Inversion: because ReservationService gets
 * everything through its constructor, the test hands it fakes ("mocks").
 * No database, no email server, no Spring startup. Runs in milliseconds.
 *
 * The old version (docs/before/ReservationService.java) couldn't be tested
 * like this: it created its own SmtpEmailClient with "new" and wrote to
 * audit.log on disk.
 */
class ReservationServiceTest {

    private ReservationRepository reservations;
    private UserRepository users;
    private ResourceRepository resources;
    private ReservationConflictChecker conflictChecker;
    private ApplicationEventPublisher events;
    private ReservationService service;

    private final User student = new User("Sam Student", "sam@example.edu", Role.STUDENT);
    private final LocalDateTime start = LocalDateTime.of(2030, 1, 15, 9, 0);
    private final LocalDateTime end = start.plusHours(2);

    @BeforeEach
    void setUp() {
        reservations = mock(ReservationRepository.class);
        users = mock(UserRepository.class);
        resources = mock(ResourceRepository.class);
        conflictChecker = mock(ReservationConflictChecker.class);
        events = mock(ApplicationEventPublisher.class);

        ApprovalPolicyResolver resolver = new ApprovalPolicyResolver(List.of(
                new AutoApprovalPolicy(), new ManagerReviewPolicy(),
                new FacultyOnlyPolicy(), new TrainingRequiredPolicy()));

        service = new ReservationService(reservations, users, resources, conflictChecker, resolver, events);

        when(users.findById(1L)).thenReturn(Optional.of(student));
        // save() just hands back whatever it was given
        when(reservations.save(any(Reservation.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void create_autoApprovedResource_savesAndPublishesEvent() {
        Resource openLab = new Resource("Open Computer Lab", ResourceType.LAB_SPACE, ApprovalRule.AUTO);
        when(resources.findById(2L)).thenReturn(Optional.of(openLab));

        Reservation result = service.create(new ReservationRequest(1L, 2L, start, end));

        assertEquals(ReservationStatus.APPROVED, result.getStatus());
        verify(events).publishEvent(any(ReservationStatusChangedEvent.class));
    }

    @Test
    void create_studentOnManagerReviewResource_isPending() {
        Resource chemLab = new Resource("Chem Lab 2", ResourceType.LAB_SPACE, ApprovalRule.MANAGER_REVIEW);
        when(resources.findById(2L)).thenReturn(Optional.of(chemLab));

        Reservation result = service.create(new ReservationRequest(1L, 2L, start, end));

        assertEquals(ReservationStatus.PENDING, result.getStatus());
    }

    @Test
    void create_conflictingTime_savesNothingAndNotifiesNobody() {
        Resource openLab = new Resource("Open Computer Lab", ResourceType.LAB_SPACE, ApprovalRule.AUTO);
        when(resources.findById(2L)).thenReturn(Optional.of(openLab));
        doThrow(new ReservationConflictException("taken"))
                .when(conflictChecker).check(any(), any(), any());

        assertThrows(ReservationConflictException.class,
                () -> service.create(new ReservationRequest(1L, 2L, start, end)));

        verify(reservations, never()).save(any());
        verifyNoInteractions(events);
    }
}
