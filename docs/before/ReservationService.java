// =====================================================================
// BEFORE: a first-draft ReservationService that violates SOLID
// (kept in docs/ for the SOLID analysis; not compiled into the app)
//
// This is the "do everything in one place" class teams usually write first.
// It works, but it's hard to change and hard to test. Each problem is marked
// with the SOLID letter it breaks. The refactored version is in
//   src/main/java/edu/campbellsville/lablink/reservation/ReservationService.java
// =====================================================================
package edu.campbellsville.lablink.reservation;

import edu.campbellsville.lablink.auth.Role;
import edu.campbellsville.lablink.auth.User;
import edu.campbellsville.lablink.auth.UserRepository;
import edu.campbellsville.lablink.inventory.Resource;
import edu.campbellsville.lablink.inventory.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationService {

    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private UserRepository userRepository;

    // [D] Dependency Inversion violation: we build a specific SMTP client
    // ourselves with "new". This class is welded to campus email. Tests can't
    // swap in a fake, so every test run would try to send real email.
    private final SmtpEmailClient emailClient = new SmtpEmailClient("smtp.campbellsville.edu", 587);

    // [S] Single Responsibility violation: this one method validates input,
    // checks for conflicts, decides approval, saves, sends email AND writes an
    // audit log. That's at least five reasons this method might need to change.
    public Reservation createReservation(Long userId, Long resourceId,
                                         LocalDateTime start, LocalDateTime end) {
        // --- responsibility 1: input validation ---
        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot reserve in the past");
        }

        User user = userRepository.findById(userId).orElseThrow();
        Resource resource = resourceRepository.findById(resourceId).orElseThrow();

        // --- responsibility 2: conflict detection ---
        // (also loads EVERY reservation into memory just to check one resource)
        for (Reservation existing : reservationRepository.findAll()) {
            if (existing.getResource().getId().equals(resourceId)
                    && existing.getStatus() != ReservationStatus.CANCELLED
                    && existing.getStatus() != ReservationStatus.DENIED
                    && existing.getStartTime().isBefore(end)
                    && existing.getEndTime().isAfter(start)) {
                throw new IllegalStateException("That time slot is already taken");
            }
        }

        Reservation reservation = new Reservation(user, resource, start, end);

        // --- responsibility 3: approval rules ---
        // [O] Open/Closed violation: every new approval rule means editing
        // this if/else chain inside the service (same problem as
        // ReservationApprover.java in the Strategy pattern section).
        switch (resource.getApprovalRule()) {
            case AUTO -> reservation.changeStatus(ReservationStatus.APPROVED, null);
            case MANAGER_REVIEW -> {
                if (user.getRole() == Role.STUDENT) {
                    reservation.changeStatus(ReservationStatus.PENDING, "Awaiting lab manager");
                } else {
                    reservation.changeStatus(ReservationStatus.APPROVED, null);
                }
            }
            case FACULTY_ONLY -> {
                if (user.getRole() == Role.STUDENT) {
                    reservation.changeStatus(ReservationStatus.DENIED, "Faculty only");
                } else {
                    reservation.changeStatus(ReservationStatus.APPROVED, null);
                }
            }
            default -> reservation.changeStatus(ReservationStatus.PENDING, null);
        }

        // --- responsibility 4: persistence ---
        Reservation saved = reservationRepository.save(reservation);

        // --- responsibility 5: notifications ---
        emailClient.send(user.getEmail(),
                "LabLink reservation " + saved.getStatus(),
                "Your reservation for " + resource.getName() + " is " + saved.getStatus());

        // --- responsibility 6: audit logging to a local file ---
        try (FileWriter log = new FileWriter("audit.log", true)) {
            log.write(LocalDateTime.now() + " CREATED " + saved.getId() + "\n");
        } catch (IOException e) {
            // swallowed: a logging failure is silently ignored
        }

        return saved;
    }

    public void cancelReservation(Long reservationId) {
        Reservation r = reservationRepository.findById(reservationId).orElseThrow();
        r.changeStatus(ReservationStatus.CANCELLED, null);
        reservationRepository.save(r);
        // Same email + audit code copy-pasted again.
        emailClient.send(r.getRequester().getEmail(), "LabLink reservation CANCELLED",
                "Your reservation for " + r.getResource().getName() + " was cancelled");
        try (FileWriter log = new FileWriter("audit.log", true)) {
            log.write(LocalDateTime.now() + " CANCELLED " + r.getId() + "\n");
        } catch (IOException e) {
            // swallowed
        }
    }

    // [S] + [I] A reporting feature for lab managers also lives here.
    // The web controller and the reporting page both depend on this one big
    // class, so each is forced to depend on methods it never calls
    // (Interface Segregation).
    public String exportReservationsCsv() {
        StringBuilder csv = new StringBuilder("id,user,resource,start,end,status\n");
        List<Reservation> all = reservationRepository.findAll();
        for (Reservation r : all) {
            csv.append(r.getId()).append(',')
               .append(r.getRequester().getEmail()).append(',')
               .append(r.getResource().getName()).append(',')
               .append(r.getStartTime()).append(',')
               .append(r.getEndTime()).append(',')
               .append(r.getStatus()).append('\n');
        }
        return csv.toString();
    }
}

// The concrete email client the service creates with "new".
class SmtpEmailClient {
    private final String host;
    private final int port;

    SmtpEmailClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    void send(String to, String subject, String body) {
        // opens a real SMTP connection to host:port ...
    }
}
