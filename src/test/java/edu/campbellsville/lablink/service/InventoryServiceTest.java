package edu.campbellsville.lablink.service;

import edu.campbellsville.lablink.model.Equipment;
import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.repository.EquipmentRepository;
import edu.campbellsville.lablink.repository.LabRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

/** UNIT tests for lab and equipment CRUD. Repositories are mocks. */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock LabRepository labRepository;
    @Mock EquipmentRepository equipmentRepository;

    @InjectMocks InventoryService service;

    @Test
    void allLabsAreReturnedInNameOrder() {
        Lab a = new Lab("A Lab", "B1", "1", 5, null);
        Lab b = new Lab("B Lab", "B1", "2", 5, null);
        when(labRepository.findAllByOrderByNameAsc()).thenReturn(List.of(a, b));

        assertThat(service.allLabs()).containsExactly(a, b);
    }

    @Test
    void getLabReturnsExistingLab() {
        Lab lab = new Lab("Media Lab", "Library", "B04", 8, null);
        when(labRepository.findById(3L)).thenReturn(Optional.of(lab));

        assertThat(service.getLab(3L)).isSameAs(lab);
    }

    @Test
    void getLabThrowsWhenMissing() {
        when(labRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLab(3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Lab not found");
    }

    @Test
    void saveLabPassesThroughToRepository() {
        Lab lab = new Lab("New Lab", "Hall", "101", 20, null);
        when(labRepository.save(lab)).thenReturn(lab);

        assertThat(service.saveLab(lab)).isSameAs(lab);
    }

    @Test
    void deleteLabDeletesThenFlushes() {
        service.deleteLab(3L);

        // flush() must come after deleteById() so a foreign-key error is raised inside deleteLab
        InOrder order = inOrder(labRepository);
        order.verify(labRepository).deleteById(3L);
        order.verify(labRepository).flush();
    }

    @Test
    void deleteLabInUseGivesFriendlyMessage() {
        doThrow(new DataIntegrityViolationException("FK violation")).when(labRepository).flush();

        assertThatThrownBy(() -> service.deleteLab(3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mark it inactive instead");
    }

    @Test
    void getEquipmentThrowsWhenMissing() {
        when(equipmentRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEquipment(7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Equipment not found");
    }

    @Test
    void deleteEquipmentWithReservationsGivesFriendlyMessage() {
        doThrow(new DataIntegrityViolationException("FK violation")).when(equipmentRepository).flush();

        assertThatThrownBy(() -> service.deleteEquipment(7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RETIRED");
    }

    @Test
    void saveEquipmentPassesThroughToRepository() {
        Equipment e = new Equipment("Loaner Laptop", "LAP-0002", null, null);
        when(equipmentRepository.save(e)).thenReturn(e);

        assertThat(service.saveEquipment(e)).isSameAs(e);
    }
}
