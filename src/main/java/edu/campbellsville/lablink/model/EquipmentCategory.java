package edu.campbellsville.lablink.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A kind of equipment (Laptop, 3D Printer, Oscilloscope...). The loan limit is a fact about
 * the category, not about each item, so it lives here once instead of on every equipment row.
 */
@Entity
@Table(name = "equipment_categories")
public class EquipmentCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    private String description;

    /** Longest single reservation allowed for items in this category. */
    @Column(name = "max_loan_hours", nullable = false)
    private int maxLoanHours;

    protected EquipmentCategory() {
    }

    public EquipmentCategory(String name, String description, int maxLoanHours) {
        this.name = name;
        this.description = description;
        this.maxLoanHours = maxLoanHours;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getMaxLoanHours() { return maxLoanHours; }
    public void setMaxLoanHours(int maxLoanHours) { this.maxLoanHours = maxLoanHours; }
}
