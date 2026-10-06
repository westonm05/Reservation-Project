package edu.campbellsville.lablink.reservation.approval;

import edu.campbellsville.lablink.auth.Role;
import edu.campbellsville.lablink.auth.User;
import edu.campbellsville.lablink.inventory.ApprovalRule;
import edu.campbellsville.lablink.inventory.Resource;
import org.springframework.stereotype.Component;

/** Concrete strategy: students wait for a lab manager; staff are approved. */
@Component
public class ManagerReviewPolicy implements ApprovalPolicy {

    @Override
    public ApprovalRule handles() {
        return ApprovalRule.MANAGER_REVIEW;
    }

    @Override
    public ApprovalDecision evaluate(User requester, Resource resource) {
        if (requester.getRole() == Role.STUDENT) {
            return ApprovalDecision.needsReview("A lab manager will review this request.");
        }
        return ApprovalDecision.approved();
    }
}
