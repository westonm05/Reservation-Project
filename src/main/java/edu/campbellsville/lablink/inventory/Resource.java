package edu.campbellsville.lablink.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** A lab space or piece of equipment that people can reserve. */
@Entity
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private ResourceType type;

    @Enumerated(EnumType.STRING)
    private ApprovalRule approvalRule;

    /** Only used when approvalRule is TRAINING_REQUIRED, e.g. "LASER_SAFETY". */
    private String requiredCertification;

    protected Resource() {
        // required by JPA
    }

    public Resource(String name, ResourceType type, ApprovalRule approvalRule) {
        this.name = name;
        this.type = type;
        this.approvalRule = approvalRule;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ResourceType getType() { return type; }
    public ApprovalRule getApprovalRule() { return approvalRule; }
    public String getRequiredCertification() { return requiredCertification; }

    public void setRequiredCertification(String requiredCertification) {
        this.requiredCertification = requiredCertification;
    }
}
