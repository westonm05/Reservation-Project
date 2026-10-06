package edu.campbellsville.lablink.reservation.approval;

import edu.campbellsville.lablink.auth.User;
import edu.campbellsville.lablink.inventory.ApprovalRule;
import edu.campbellsville.lablink.inventory.Resource;

/**
 * STRATEGY PATTERN: the "Strategy" interface.
 *
 * Each approval rule is its own class that implements this interface.
 * ReservationService only knows about this interface, never about the
 * individual rules, so it doesn't change when rules are added or edited.
 *
 * In the old version (docs/before/ReservationApprover.java) all of these
 * rules were branches of one long if/else chain.
 */
public interface ApprovalPolicy {

    /** Which ApprovalRule this class handles. Used to pick the right strategy. */
    ApprovalRule handles();

    /** Decide what should happen to this user's request for this resource. */
    ApprovalDecision evaluate(User requester, Resource resource);
}
