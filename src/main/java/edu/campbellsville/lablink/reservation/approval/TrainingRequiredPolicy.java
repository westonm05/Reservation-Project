package edu.campbellsville.lablink.reservation.approval;

import edu.campbellsville.lablink.auth.User;
import edu.campbellsville.lablink.inventory.ApprovalRule;
import edu.campbellsville.lablink.inventory.Resource;
import org.springframework.stereotype.Component;

/**
 * Concrete strategy: the requester must have finished the resource's
 * safety training (e.g. LASER_SAFETY for the laser cutter).
 *
 * This whole rule was added as ONE new file. No other approval class, and not
 * ReservationService, had to be opened. That's the Open/Closed Principle the
 * Strategy pattern gives us.
 */
@Component
public class TrainingRequiredPolicy implements ApprovalPolicy {

    @Override
    public ApprovalRule handles() {
        return ApprovalRule.TRAINING_REQUIRED;
    }

    @Override
    public ApprovalDecision evaluate(User requester, Resource resource) {
        String required = resource.getRequiredCertification();
        if (required == null || requester.hasCertification(required)) {
            return ApprovalDecision.approved();
        }
        return ApprovalDecision.denied(
                "You need " + required + " training before reserving " + resource.getName() + ".");
    }
}
