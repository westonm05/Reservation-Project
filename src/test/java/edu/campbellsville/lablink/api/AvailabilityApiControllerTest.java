package edu.campbellsville.lablink.api;

import edu.campbellsville.lablink.api.ApiDtos.ApiError;
import edu.campbellsville.lablink.api.ApiDtos.LabAvailability;
import edu.campbellsville.lablink.api.ApiDtos.LabSummary;
import edu.campbellsville.lablink.model.Department;
import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.model.Reservation;
import edu.campbellsville.lablink.model.ReservationStatus;
import edu.campbellsville.lablink.service.AvailabilityService;
import edu.campbellsville.lablink.service.InventoryService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** UNIT tests for the REST API controller (backlog 3.6), called as plain Java with mocked services. */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AvailabilityApiControllerTest {

    private static final LocalDateTime NINE = LocalDateTime.of(2026, 11, 16, 9, 0);
    private static final LocalDateTime FIVE = LocalDateTime.of(2026, 11, 16, 17, 0);

    @Mock InventoryService inventory;
    @Mock AvailabilityService availability;

    @InjectMocks AvailabilityApiController controller;

    private final Lab lab = new Lab("Robotics Lab", "Science Building", "118", 12, null);

    @Test
    void labsListsActiveLabsWithDepartmentName() {
        lab.setDepartment(new Department("CS", "Computer Science"));
        when(inventory.searchLabs(null, null, true)).thenReturn(List.of(lab));

        List<LabSummary> labs = controller.labs();

        assertThat(labs).hasSize(1);
        assertThat(labs.get(0).name()).isEqualTo("Robotics Lab");
        assertThat(labs.get(0).department()).isEqualTo("Computer Science");
    }

    @Test
    void availabilityReportsBusySlotsWithoutWhoBookedThem() {
        Reservation r = new Reservation();
        r.setStartTime(NINE.plusHours(1));
        r.setEndTime(NINE.plusHours(2));
        r.setStatus(ReservationStatus.PENDING);
        when(availability.getLab(2L)).thenReturn(lab);
        when(availability.bookings(2L, NINE, FIVE)).thenReturn(List.of(r));

        LabAvailability body = controller.labAvailability(2L, NINE, FIVE);

        assertThat(body.free()).isFalse();
        assertThat(body.busy()).hasSize(1);
        assertThat(body.busy().get(0).start()).isEqualTo(NINE.plusHours(1));
        assertThat(body.busy().get(0).status()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void availabilityDefaultsToTheNextSevenDays() {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        when(availability.getLab(2L)).thenReturn(lab);
        when(availability.bookings(2L, today, today.plusDays(7))).thenReturn(List.of());

        LabAvailability body = controller.labAvailability(2L, null, null);

        assertThat(body.from()).isEqualTo(today);
        assertThat(body.to()).isEqualTo(today.plusDays(7));
        assertThat(body.free()).isTrue();
    }

    @Test
    void notFoundMessagesBecome404AndOthers400() {
        ResponseEntity<ApiError> missing = controller.badRequest(new IllegalArgumentException("Lab not found"));
        ResponseEntity<ApiError> bad = controller.badRequest(new IllegalArgumentException("Ask for at most 31 days at a time"));

        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(bad.getBody().error()).contains("31 days");
    }
}
