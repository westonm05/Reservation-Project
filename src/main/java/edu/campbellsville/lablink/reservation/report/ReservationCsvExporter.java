package edu.campbellsville.lablink.reservation.report;

import edu.campbellsville.lablink.reservation.Reservation;
import edu.campbellsville.lablink.reservation.ReservationRepository;
import org.springframework.stereotype.Component;

/**
 * SINGLE RESPONSIBILITY / INTERFACE SEGREGATION: lab-manager reporting lives
 * in its own class. The reporting page depends on this small class, and the
 * reservation API no longer depends on reporting code it never uses.
 */
@Component
public class ReservationCsvExporter {

    private final ReservationRepository reservationRepository;

    public ReservationCsvExporter(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public String exportAll() {
        StringBuilder csv = new StringBuilder("id,user,resource,start,end,status\n");
        for (Reservation r : reservationRepository.findAll()) {
            csv.append(r.getId()).append(',')
               .append(escape(r.getRequester().getEmail())).append(',')
               .append(escape(r.getResource().getName())).append(',')
               .append(r.getStartTime()).append(',')
               .append(r.getEndTime()).append(',')
               .append(r.getStatus()).append('\n');
        }
        return csv.toString();
    }

    /** Quote values containing commas or quotes so the CSV opens correctly in Excel. */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
