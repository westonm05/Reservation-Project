package edu.campbellsville.lablink.repository;

import edu.campbellsville.lablink.model.Equipment;
import edu.campbellsville.lablink.model.EquipmentStatus;
import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.model.MaintenanceRecord;
import edu.campbellsville.lablink.model.Reservation;
import edu.campbellsville.lablink.model.ReservationStatus;
import edu.campbellsville.lablink.model.ReservationStatusChange;
import edu.campbellsville.lablink.model.User;
import edu.campbellsville.lablink.service.ReservationService;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the Week 11 database work end to end: Flyway built the schema from the migration
 * scripts, the seed script filled it, and the ORM classes read and write the new tables.
 * (Startup itself also proves the entities match the tables: ddl-auto=validate would fail.)
 */
@Tag("integration")
@SpringBootTest
@Transactional
class DatabaseMigrationIntegrationTest {

    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired EquipmentRepository equipment;
    @Autowired MaintenanceRecordRepository maintenance;
    @Autowired ReservationStatusChangeRepository history;
    @Autowired LabRepository labs;
    @Autowired ReservationService reservationService;

    @Test
    void allVersionedMigrationsWereApplied() {
        List<String> versions = Arrays.stream(flyway.info().applied())
                .filter(m -> m.getVersion() != null)
                .map(MigrationInfo::getVersion)
                .map(Object::toString)
                .toList();
        assertThat(versions).containsExactly("1", "2", "3", "4");
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void seedScriptLoadsAtLeastTwentyRowsIntoEachMajorTable() {
        for (String table : List.of("users", "labs", "equipment", "reservations",
                "reservation_status_history", "maintenance_records")) {
            Integer rows = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
            assertThat(rows).as(table).isGreaterThanOrEqualTo(20);
        }
    }

    @Test
    void seededForeignKeysResolveThroughTheOrm() {
        Equipment printer = equipment.findAll().stream()
                .filter(e -> e.getSerialNumber().equals("PRN-0001")).findFirst().orElseThrow();
        assertThat(printer.getLab().getName()).isEqualTo("Robotics Lab");
        assertThat(printer.getCategory().getName()).isEqualTo("3D Printer");
        assertThat(printer.getCategory().getMaxLoanHours()).isEqualTo(8);

        User manager = users.findByEmail("manager@lablink.edu").orElseThrow();
        assertThat(manager.getDepartment().getCode()).isEqualTo("CS");
    }

    @Test
    void equipmentInMaintenanceHasAnOpenMaintenanceRecord() {
        for (Equipment e : equipment.findAll()) {
            if (e.getStatus() == EquipmentStatus.MAINTENANCE) {
                List<MaintenanceRecord> open = maintenance.findByEquipmentAndResolvedAtIsNull(e);
                assertThat(open).as(e.getName()).isNotEmpty();
            }
        }
    }

    @Test
    void reservationServiceWritesStatusHistoryRows() {
        User student = users.findByEmail("student@lablink.edu").orElseThrow();
        User manager = users.findByEmail("manager@lablink.edu").orElseThrow();
        Lab lab = labs.save(new Lab("History Test Lab", "History Hall", "H1", 5, null));
        LocalDateTime start = LocalDateTime.now().plusDays(5).withHour(10).withMinute(0).withSecond(0).withNano(0);

        Reservation r = reservationService.requestLab(student, lab.getId(), start, start.plusHours(1), "Study");
        reservationService.approve(r.getId(), manager);

        List<ReservationStatusChange> trail = history.findByReservationOrderByChangedAtAscIdAsc(r);
        assertThat(trail).extracting(ReservationStatusChange::getNewStatus)
                .containsExactly(ReservationStatus.PENDING, ReservationStatus.APPROVED);
        assertThat(trail.get(0).getOldStatus()).isNull();
        assertThat(trail.get(1).getChangedBy().getId()).isEqualTo(manager.getId());
    }
}
