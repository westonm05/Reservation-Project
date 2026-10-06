package edu.campbellsville.lablink.reservation.approval;

/**
 * The result of running an ApprovalPolicy.
 *
 * IMPROVEMENT over the old String result ("DENIED: ..."): the outcome is a
 * real type, so callers use a switch on Outcome instead of parsing text,
 * and a typo is a compile error instead of a silent bug.
 */
public record ApprovalDecision(Outcome outcome, String reason) {

    public enum Outcome { APPROVED, NEEDS_REVIEW, DENIED }

    public static ApprovalDecision approved() {
        return new ApprovalDecision(Outcome.APPROVED, null);
    }

    public static ApprovalDecision needsReview(String reason) {
        return new ApprovalDecision(Outcome.NEEDS_REVIEW, reason);
    }

    public static ApprovalDecision denied(String reason) {
        return new ApprovalDecision(Outcome.DENIED, reason);
    }
}
