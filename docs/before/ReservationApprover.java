// =====================================================================
// BEFORE: approval logic WITHOUT the Strategy pattern
// (kept in docs/ for comparison; not compiled into the app)
//
// Compare with the "after" version in
//   src/main/java/edu/campbellsville/lablink/reservation/approval/
// =====================================================================
package edu.campbellsville.lablink.reservation;

import edu.campbellsville.lablink.auth.Role;
import edu.campbellsville.lablink.auth.User;
import edu.campbellsville.lablink.inventory.Resource;
import org.springframework.stereotype.Component;

@Component
public class ReservationApprover {

    /**
     * Decides what happens to a new request.
     * Returns "APPROVED", "PENDING" or "DENIED: <reason>".
     *
     * PROBLEMS with this design:
     *  1. Every approval rule lives in ONE method. Each new rule the lab
     *     managers ask for means editing this method again, and every edit
     *     risks breaking the rules that already work.
     *  2. You can't test one rule by itself. A test for TRAINING_REQUIRED
     *     still runs through all the other branches.
     *  3. Two teammates adding two rules at the same time both edit the same
     *     lines, so merge conflicts are guaranteed.
     *  4. The result is a String, so callers have to parse text like
     *     "DENIED: ..." to find out what happened. One typo breaks it silently.
     */
    public String decide(User requester, Resource resource) {
        String rule = resource.getApprovalRule().name();

        if (rule.equals("AUTO")) {
            return "APPROVED";

        } else if (rule.equals("MANAGER_REVIEW")) {
            if (requester.getRole() == Role.FACULTY || requester.getRole() == Role.LAB_MANAGER) {
                return "APPROVED";
            }
            return "PENDING";

        } else if (rule.equals("FACULTY_ONLY")) {
            if (requester.getRole() == Role.STUDENT) {
                return "DENIED: Only faculty can reserve " + resource.getName();
            }
            return "APPROVED";

        } else if (rule.equals("TRAINING_REQUIRED")) {
            if (requester.hasCertification(resource.getRequiredCertification())) {
                return "APPROVED";
            }
            return "DENIED: Requires " + resource.getRequiredCertification() + " training";
        }

        // A new rule added to the enum but forgotten here falls through to
        // this line, and nothing warns us at compile time.
        return "PENDING";
    }
}
