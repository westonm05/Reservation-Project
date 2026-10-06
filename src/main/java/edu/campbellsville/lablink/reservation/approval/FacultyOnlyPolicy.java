package edu.campbellsville.lablink.reservation.approval;

import edu.campbellsville.lablink.auth.Role;
import edu.campbellsville.lablink.auth.User;
import edu.campbellsville.lablink.inventory.ApprovalRule;
import edu.campbellsville.lablink.inventory.Resource;
import org.springframework.stereotype.Component;

/** Concrete strategy: students may not book this resource at all. */
@Component
public class FacultyOnlyPolicy implements ApprovalPolicy {

    @Override
    public ApprovalRule handles() {
        return ApprovalRule.FACULTY_ONLY;
    }

    @Override
    public ApprovalDecision evaluate(User requester, Resource resource) {
        if (requester.getRole() == Role.STUDENT) {
            return ApprovalDecision.denied("Only faculty can reserve " + resource.getName() + ".");
        }
        return ApprovalDecision.approved();
    }
}
