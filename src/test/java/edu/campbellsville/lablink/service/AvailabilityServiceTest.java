package edu.campbellsville.lablink.service;

import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.model.Reservation;
import edu.campbellsville.lablink.model.ReservationStatus;
import edu.campbellsville.lablink.repository.LabRepository;
import edu.campbellsville.lablink.repository.ReservationRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** UNIT tests for lab availability (backlog 3.2 / 3.6). Repositories are mocks. */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 16);

    @Mock LabRepository labRepository;
    @Mock ReservationRepository reservationRepository;

    @InjectMocks AvailabilityService service;

    private final Lab lab = new Lab("Robotics Lab", "Science Building", "118", 12, null);

    @Test
    void bookingsAsksRepositoryForActiveReservationsInTheWindow() {
        LocalDateTime from = MONDAY.atTime(8, 0);
        LocalDateTime to = MONDAY.atTime(17, 0);
        Reservation r = booking(MONDAY.atTime(9, 0), MONDAY.atTime(10, 0));
        when(labRepository.findById(2L)).thenReturn(Optional.of(lab));
        when(reservationRepository.findByLabAndStatusInAndStartTimeBeforeAndEndTimeAfterOrderByStartTimeAsc(
                lab, ReservationStatus.ACTIVE, to, from)).thenReturn(List.of(r));

        assertThat(service.bookings(2L, from, to)).containsExactly(r);
    }

    @Test
    void windowMustEndAfterItStarts() {
        LocalDateTime t = MONDAY.atTime(9, 0);

        assertThatThrownBy(() -> service.bookings(2L, t, t))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("after the start");
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void windowLongerThan31DaysIsRefused() {
        assertThatThrownBy(() -> service.bookings(2L, MONDAY.atStartOfDay(), MONDAY.plusDays(32).atStartOfDay()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("31 days");
    }

    @Test
    void unknownLabIsReported() {
        when(labRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.bookings(99L, MONDAY.atStartOfDay(), MONDAY.plusDays(1).atStartOfDay()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Lab not found");
    }

    @Test
    void isFreeIsTrueOnlyWhenNothingIsBooked() {
        when(labRepository.findById(2L)).thenReturn(Optional.of(lab));
        when(reservationRepository.findByLabAndStatusInAndStartTimeBeforeAndEndTimeAfterOrderByStartTimeAsc(
                eq(lab), any(), any(), any()))
                .thenReturn(List.of())
                .thenReturn(List.of(booking(MONDAY.atTime(9, 0), MONDAY.atTime(10, 0))));

        assertThat(service.isFree(2L, MONDAY.atTime(11, 0), MONDAY.atTime(12, 0))).isTrue();
        assertThat(service.isFree(2L, MONDAY.atTime(9, 30), MONDAY.atTime(10, 30))).isFalse();
    }

    @Test
    void weekHasSevenDaysAndPutsEachBookingOnItsDay() {
        Reservation tuesday = booking(MONDAY.plusDays(1).atTime(9, 0), MONDAY.plusDays(1).atTime(11, 0));
        Reservation overnight = booking(MONDAY.plusDays(3).atTime(22, 0), MONDAY.plusDays(4).atTime(2, 0));
        when(labRepository.findById(2L)).thenReturn(Optional.of(lab));
        when(reservationRepository.findByLabAndStatusInAndStartTimeBeforeAndEndTimeAfterOrderByStartTimeAsc(
                lab, ReservationStatus.ACTIVE, MONDAY.plusDays(7).atStartOfDay(), MONDAY.atStartOfDay()))
                .thenReturn(List.of(tuesday, overnight));

        List<AvailabilityService.DaySchedule> week = service.week(2L, MONDAY);

        assertThat(week).hasSize(7);
        assertThat(week.get(0).date()).isEqualTo(MONDAY);
        assertThat(week.get(0).isFree()).isTrue();
        assertThat(week.get(1).bookings()).containsExactly(tuesday);
        // the overnight booking appears on both Thursday and Friday
        assertThat(week.get(3).bookings()).containsExactly(overnight);
        assertThat(week.get(4).bookings()).containsExactly(overnight);
        assertThat(week.get(6).isFree()).isTrue();
    }

    private Reservation booking(LocalDateTime start, LocalDateTime end) {
        Reservation r = new Reservation();
        r.setLab(lab);
        r.setStartTime(start);
        r.setEndTime(end);
        r.setStatus(ReservationStatus.APPROVED);
        return r;
    }
}
