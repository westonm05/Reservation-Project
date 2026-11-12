package edu.campbellsville.lablink.service;

import edu.campbellsville.lablink.model.Department;
import edu.campbellsville.lablink.model.Equipment;
import edu.campbellsville.lablink.model.EquipmentCategory;
import edu.campbellsville.lablink.model.EquipmentStatus;
import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.repository.DepartmentRepository;
import edu.campbellsville.lablink.repository.EquipmentCategoryRepository;
import edu.campbellsville.lablink.repository.EquipmentRepository;
import edu.campbellsville.lablink.repository.LabRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** UNIT tests for lab and equipment search/filter (backlog 3.8). Repositories are mocks. */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // setUp stubs both lists; each test uses one of them
class InventorySearchTest {

    @Mock LabRepository labRepository;
    @Mock EquipmentRepository equipmentRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock EquipmentCategoryRepository categoryRepository;

    @InjectMocks InventoryService service;

    private Lab csLab;
    private Lab robotics;
    private Lab closedLab;
    private Equipment laptop;
    private Equipment brokenLaptop;
    private Equipment printer;

    @BeforeEach
    void setUp() {
        Department cs = department(1L, "CS", "Computer Science");
        Department art = department(2L, "ART", "Art and Design");

        csLab = new Lab("Computer Science Lab", "Science Building", "210", 30, "Java workstations");
        csLab.setDepartment(cs);
        robotics = new Lab("Robotics Lab", "Science Building", "118", 12, "Soldering stations");
        robotics.setDepartment(cs);
        closedLab = new Lab("Old Art Studio", "Fine Arts", "B2", 10, null);
        closedLab.setDepartment(art);
        closedLab.setActive(false);
        when(labRepository.findAllByOrderByNameAsc()).thenReturn(List.of(csLab, closedLab, robotics));

        EquipmentCategory laptops = category(5L, "Laptop");
        EquipmentCategory printers = category(6L, "3D Printer");

        laptop = new Equipment("Loaner Laptop 1", "LAP-0001", "Dell Latitude", null);
        laptop.setCategory(laptops);
        brokenLaptop = new Equipment("Loaner Laptop 4", "LAP-0004", "MacBook Air", null);
        brokenLaptop.setCategory(laptops);
        brokenLaptop.setStatus(EquipmentStatus.MAINTENANCE);
        printer = new Equipment("3D Printer", "PRN-0001", "Prusa MK4", null);
        printer.setCategory(printers);
        when(equipmentRepository.findAllByOrderByNameAsc()).thenReturn(List.of(printer, laptop, brokenLaptop));
    }

    @Test
    void noFiltersReturnsEveryLab() {
        assertThat(service.searchLabs(null, null, false)).containsExactly(csLab, closedLab, robotics);
    }

    @Test
    void labTextSearchIgnoresCaseAndMatchesBuilding() {
        assertThat(service.searchLabs("  SCIENCE building ", null, false)).containsExactly(csLab, robotics);
    }

    @Test
    void labTextSearchMatchesDescription() {
        assertThat(service.searchLabs("solder", null, false)).containsExactly(robotics);
    }

    @Test
    void labsCanBeFilteredByDepartment() {
        assertThat(service.searchLabs("", 2L, false)).containsExactly(closedLab);
    }

    @Test
    void activeOnlyHidesInactiveLabs() {
        assertThat(service.searchLabs(null, null, true)).containsExactly(csLab, robotics);
    }

    @Test
    void labFiltersCombine() {
        assertThat(service.searchLabs("lab", 1L, true)).containsExactly(csLab, robotics);
        assertThat(service.searchLabs("nothing like this", null, false)).isEmpty();
    }

    @Test
    void equipmentTextSearchMatchesSerialNumber() {
        assertThat(service.searchEquipment("prn-", null, null)).containsExactly(printer);
    }

    @Test
    void equipmentCanBeFilteredByCategory() {
        assertThat(service.searchEquipment(null, 5L, null)).containsExactly(laptop, brokenLaptop);
    }

    @Test
    void equipmentCanBeFilteredByStatus() {
        assertThat(service.searchEquipment("laptop", null, EquipmentStatus.AVAILABLE)).containsExactly(laptop);
        assertThat(service.searchEquipment(null, null, EquipmentStatus.MAINTENANCE)).containsExactly(brokenLaptop);
    }

    // Department and EquipmentCategory get their ids from the database, so tests set them directly.
    private static Department department(Long id, String code, String name) {
        Department d = new Department(code, name);
        ReflectionTestUtils.setField(d, "id", id);
        return d;
    }

    private static EquipmentCategory category(Long id, String name) {
        EquipmentCategory c = new EquipmentCategory(name, null, 24);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
