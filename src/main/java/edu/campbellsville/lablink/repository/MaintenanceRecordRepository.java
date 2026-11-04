package edu.campbellsville.lablink.repository;

import edu.campbellsville.lablink.model.Equipment;
import edu.campbellsville.lablink.model.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    /** Full maintenance log for one item, newest first. */
    List<MaintenanceRecord> findByEquipmentOrderByReportedAtDesc(Equipment equipment);

    /** Open tickets (not yet resolved) for one item. Uses idx_maintenance_equipment_open. */
    List<MaintenanceRecord> findByEquipmentAndResolvedAtIsNull(Equipment equipment);
}
