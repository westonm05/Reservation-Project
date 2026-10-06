package edu.campbellsville.lablink.reservation.approval;

import edu.campbellsville.lablink.inventory.ApprovalRule;
import edu.campbellsville.lablink.inventory.Resource;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * STRATEGY PATTERN: picks the right strategy for a resource.
 *
 * How it works: Spring finds every class marked @Component that implements
 * ApprovalPolicy and passes them all into this constructor as a List.
 * We index them by the rule each one handles.
 *
 * So adding a new policy class is enough. Spring discovers it, and this
 * class never needs editing.
 */
@Component
public class ApprovalPolicyResolver {

    private final Map<ApprovalRule, ApprovalPolicy> policiesByRule = new EnumMap<>(ApprovalRule.class);

    public ApprovalPolicyResolver(List<ApprovalPolicy> policies) {
        for (ApprovalPolicy policy : policies) {
            ApprovalPolicy previous = policiesByRule.put(policy.handles(), policy);
            if (previous != null) {
                throw new IllegalStateException("Two policies handle " + policy.handles() + ": "
                        + previous.getClass().getSimpleName() + " and " + policy.getClass().getSimpleName());
            }
        }
        // Fail at startup, not in front of a user, if a rule has no policy.
        // (The old if/else chain silently returned "PENDING" in that case.)
        for (ApprovalRule rule : ApprovalRule.values()) {
            if (!policiesByRule.containsKey(rule)) {
                throw new IllegalStateException("No ApprovalPolicy handles " + rule);
            }
        }
    }

    public ApprovalPolicy policyFor(Resource resource) {
        return policiesByRule.get(resource.getApprovalRule());
    }
}
