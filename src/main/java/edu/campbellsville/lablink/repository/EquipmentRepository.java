package edu.campbellsville.lablink.repository;

import edu.campbellsville.lablink.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findAllByOrderByNameAsc();
}
