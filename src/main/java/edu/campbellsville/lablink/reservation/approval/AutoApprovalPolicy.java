package edu.campbellsville.lablink.reservation.approval;

import edu.campbellsville.lablink.auth.User;
import edu.campbellsville.lablink.inventory.ApprovalRule;
import edu.campbellsville.lablink.inventory.Resource;
import org.springframework.stereotype.Component;

/** Concrete strategy: open resources are approved instantly for everyone. */
@Component
public class AutoApprovalPolicy implements ApprovalPolicy {

    @Override
    public ApprovalRule handles() {
        return ApprovalRule.AUTO;
    }

    @Override
    public ApprovalDecision evaluate(User requester, Resource resource) {
        return ApprovalDecision.approved();
    }
}
