package edu.campbellsville.lablink.service;

import edu.campbellsville.lablink.model.Department;
import edu.campbellsville.lablink.model.Equipment;
import edu.campbellsville.lablink.model.EquipmentCategory;
import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.repository.DepartmentRepository;
import edu.campbellsville.lablink.repository.EquipmentCategoryRepository;
import edu.campbellsville.lablink.repository.EquipmentRepository;
import edu.campbellsville.lablink.repository.LabRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Create / read / update / delete for labs and equipment. */
@Service
public class InventoryService {

    private final LabRepository labRepository;
    private final EquipmentRepository equipmentRepository;
    private final DepartmentRepository departmentRepository;
    private final EquipmentCategoryRepository categoryRepository;

    public InventoryService(LabRepository labRepository, EquipmentRepository equipmentRepository,
                            DepartmentRepository departmentRepository, EquipmentCategoryRepository categoryRepository) {
        this.labRepository = labRepository;
        this.equipmentRepository = equipmentRepository;
        this.departmentRepository = departmentRepository;
        this.categoryRepository = categoryRepository;
    }

    // ---- lookups for form dropdowns ----

    public List<Department> allDepartments() {
        return departmentRepository.findAllByOrderByNameAsc();
    }

    /** The department with this id, or null when id is null (the "none" choice in a dropdown). */
    public Department getDepartment(Long id) {
        return id == null ? null : departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));
    }

    public List<EquipmentCategory> allCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    /** The category with this id, or null when id is null. */
    public EquipmentCategory getCategory(Long id) {
        return id == null ? null : categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    // ---- labs ----

    public List<Lab> allLabs() {
        return labRepository.findAllByOrderByNameAsc();
    }

    public Lab getLab(Long id) {
        return labRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Lab not found"));
    }

    @Transactional
    public Lab saveLab(Lab lab) {
        return labRepository.save(lab); // insert if id is null, update otherwise
    }

    /** Deletes a lab. Fails with a friendly message if equipment or reservations still point at it. */
    @Transactional
    public void deleteLab(Long id) {
        try {
            labRepository.deleteById(id);
            labRepository.flush(); // force the DELETE now so a foreign-key error surfaces here
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("This lab still has equipment or reservations. Mark it inactive instead.");
        }
    }

    // ---- equipment ----

    public List<Equipment> allEquipment() {
        return equipmentRepository.findAllByOrderByNameAsc();
    }

    public Equipment getEquipment(Long id) {
        return equipmentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Equipment not found"));
    }

    @Transactional
    public Equipment saveEquipment(Equipment equipment) {
        return equipmentRepository.save(equipment);
    }

    @Transactional
    public void deleteEquipment(Long id) {
        try {
            equipmentRepository.deleteById(id);
            equipmentRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("This equipment has reservations or maintenance records. Set its status to RETIRED instead.");
        }
    }
}
