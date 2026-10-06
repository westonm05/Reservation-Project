package edu.campbellsville.lablink.reservation.approval;

import edu.campbellsville.lablink.auth.Role;
import edu.campbellsville.lablink.auth.User;
import edu.campbellsville.lablink.inventory.ApprovalRule;
import edu.campbellsville.lablink.inventory.Resource;
import edu.campbellsville.lablink.inventory.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Plain unit tests: no Spring, no database. Each strategy is a small class,
 * so each one can be tested on its own. That wasn't possible with the old
 * if/else chain.
 */
class ApprovalPolicyTest {

    private final User student = new User("Sam Student", "sam@example.edu", Role.STUDENT);
    private final User faculty = new User("Dr. Fay", "fay@example.edu", Role.FACULTY);

    @Test
    void managerReview_studentNeedsReview_facultyApproved() {
        Resource lab = new Resource("Chem Lab 2", ResourceType.LAB_SPACE, ApprovalRule.MANAGER_REVIEW);
        ManagerReviewPolicy policy = new ManagerReviewPolicy();

        assertEquals(ApprovalDecision.Outcome.NEEDS_REVIEW, policy.evaluate(student, lab).outcome());
        assertEquals(ApprovalDecision.Outcome.APPROVED, policy.evaluate(faculty, lab).outcome());
    }

    @Test
    void facultyOnly_deniesStudents() {
        Resource scope = new Resource("Electron Microscope", ResourceType.EQUIPMENT, ApprovalRule.FACULTY_ONLY);

        assertEquals(ApprovalDecision.Outcome.DENIED, new FacultyOnlyPolicy().evaluate(student, scope).outcome());
    }

    @Test
    void trainingRequired_approvesOnlyCertifiedUsers() {
        Resource cutter = new Resource("Laser Cutter", ResourceType.EQUIPMENT, ApprovalRule.TRAINING_REQUIRED);
        cutter.setRequiredCertification("LASER_SAFETY");
        TrainingRequiredPolicy policy = new TrainingRequiredPolicy();

        assertEquals(ApprovalDecision.Outcome.DENIED, policy.evaluate(student, cutter).outcome());

        student.addCertification("LASER_SAFETY");
        assertEquals(ApprovalDecision.Outcome.APPROVED, policy.evaluate(student, cutter).outcome());
    }

    @Test
    void resolver_picksPolicyMatchingTheResourceRule() {
        ApprovalPolicyResolver resolver = new ApprovalPolicyResolver(List.of(
                new AutoApprovalPolicy(), new ManagerReviewPolicy(),
                new FacultyOnlyPolicy(), new TrainingRequiredPolicy()));

        Resource lab = new Resource("Open Lab", ResourceType.LAB_SPACE, ApprovalRule.AUTO);

        assertInstanceOf(AutoApprovalPolicy.class, resolver.policyFor(lab));
    }

    @Test
    void resolver_failsAtStartupWhenARuleHasNoPolicy() {
        assertThrows(IllegalStateException.class,
                () -> new ApprovalPolicyResolver(List.of(new AutoApprovalPolicy())));
    }
}
