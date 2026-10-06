package edu.campbellsville.lablink.reservation;

import edu.campbellsville.lablink.auth.User;
import edu.campbellsville.lablink.auth.UserRepository;
import edu.campbellsville.lablink.inventory.Resource;
import edu.campbellsville.lablink.inventory.ResourceRepository;
import edu.campbellsville.lablink.reservation.approval.ApprovalDecision;
import edu.campbellsville.lablink.reservation.approval.ApprovalPolicyResolver;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * AFTER the SOLID refactor (compare docs/before/ReservationService.java).
 *
 * This class now only coordinates the steps of a reservation. Each step is
 * handed to a class whose job is exactly that step:
 *
 *   - time-slot rules      -> ReservationConflictChecker   (S)
 *   - approval rules       -> ApprovalPolicy strategies     (O)
 *   - email / audit / etc. -> listeners of ReservationStatusChangedEvent (S, D)
 *   - CSV reports          -> report.ReservationCsvExporter (S, I)
 *
 * Every dependency arrives through the constructor as an interface or a
 * small focused class. Nothing is created with "new" in here (D).
 */
@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final ReservationConflictChecker conflictChecker;
    private final ApprovalPolicyResolver approvalPolicyResolver;
    private final ApplicationEventPublisher events;

    public ReservationService(ReservationRepository reservationRepository,
                              UserRepository userRepository,
                              ResourceRepository resourceRepository,
                              ReservationConflictChecker conflictChecker,
                              ApprovalPolicyResolver approvalPolicyResolver,
                              ApplicationEventPublisher events) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.conflictChecker = conflictChecker;
        this.approvalPolicyResolver = approvalPolicyResolver;
        this.events = events;
    }

    @Transactional
    public Reservation create(ReservationRequest request) {
        User requester = userRepository.findById(request.requesterId())
                .orElseThrow(() -> new NoSuchElementException("No user with id " + request.requesterId()));
        Resource resource = resourceRepository.findById(request.resourceId())
                .orElseThrow(() -> new NoSuchElementException("No resource with id " + request.resourceId()));

        conflictChecker.check(resource.getId(), request.startTime(), request.endTime());

        Reservation reservation = new Reservation(requester, resource, request.startTime(), request.endTime());

        // Strategy pattern: ask whichever policy belongs to this resource.
        ApprovalDecision decision = approvalPolicyResolver.policyFor(resource).evaluate(requester, resource);
        reservation.changeStatus(toStatus(decision.outcome()), decision.reason());

        Reservation saved = reservationRepository.save(reservation);

        // Observer pattern: announce it. Listeners handle email, audit, etc.
        events.publishEvent(new ReservationStatusChangedEvent(saved, null));
        return saved;
    }

    @Transactional
    public Reservation cancel(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("No reservation with id " + reservationId));

        ReservationStatus previous = reservation.getStatus();
        reservation.changeStatus(ReservationStatus.CANCELLED, null);
        Reservation saved = reservationRepository.save(reservation);

        events.publishEvent(new ReservationStatusChangedEvent(saved, previous));
        return saved;
    }

    private static ReservationStatus toStatus(ApprovalDecision.Outcome outcome) {
        // A switch over an enum: if someone adds a new Outcome, the compiler
        // flags this switch until it's handled.
        return switch (outcome) {
            case APPROVED -> ReservationStatus.APPROVED;
            case NEEDS_REVIEW -> ReservationStatus.PENDING;
            case DENIED -> ReservationStatus.DENIED;
        };
    }
}
