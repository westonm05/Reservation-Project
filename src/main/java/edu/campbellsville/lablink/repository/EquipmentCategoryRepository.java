package edu.campbellsville.lablink.repository;

import edu.campbellsville.lablink.model.EquipmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentCategoryRepository extends JpaRepository<EquipmentCategory, Long> {

    List<EquipmentCategory> findAllByOrderByNameAsc();
}
