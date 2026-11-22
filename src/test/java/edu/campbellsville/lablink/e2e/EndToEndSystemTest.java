package edu.campbellsville.lablink.e2e;

import edu.campbellsville.lablink.model.Equipment;
import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.model.Reservation;
import edu.campbellsville.lablink.model.ReservationStatus;
import edu.campbellsville.lablink.model.Role;
import edu.campbellsville.lablink.model.User;
import edu.campbellsville.lablink.repository.EquipmentRepository;
import edu.campbellsville.lablink.repository.LabRepository;
import edu.campbellsville.lablink.repository.ReservationRepository;
import edu.campbellsville.lablink.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SYSTEM (end-to-end) tests for Week 15. Unlike the integration tests, nothing here is mocked:
 * the whole application starts on a real port (embedded Tomcat + Spring Security + Flyway + H2),
 * and each scenario drives it over HTTP the way a browser does. It loads the login page, sends
 * the form with its CSRF token, keeps the session cookie, and follows redirects. The repositories
 * are only used to look up ids and to double-check what ended up in the database.
 *
 * The app gets its own in-memory database (lablink-e2e) so these tests can't disturb, or be
 * disturbed by, the other test classes. Every scenario creates its own lab or user and uses its
 * own dates, so the scenarios are independent and can run in any order.
 *
 * Each scenario prints an "E2E-RESULT" line so the results can be read straight from the CI log.
 */
@Tag("system")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:lablink-e2e;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.DisplayName.class)
@ExtendWith(EndToEndSystemTest.ResultLogger.class)
class EndToEndSystemTest {

    private static final DateTimeFormatter FORM_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final String STUDENT = "student@lablink.edu";
    private static final String FACULTY = "faculty@lablink.edu";
    private static final String MANAGER = "manager@lablink.edu";
    /** Building + room number must be unique, so every lab the tests create gets its own room. */
    private static final AtomicInteger LAB_ROOMS = new AtomicInteger();

    @LocalServerPort int port;
    @Autowired LabRepository labRepository;
    @Autowired EquipmentRepository equipmentRepository;
    @Autowired ReservationRepository reservationRepository;
    @Autowired UserRepository userRepository;

    // ------------------------------------------------------------------ scenarios

    @Test
    @DisplayName("E2E-01 New student registers, logs in and sees their dashboard")
    void studentRegistersAndLogsIn() throws Exception {
        Browser browser = new Browser();
        Page afterRegister = browser.submit("/register", "/register", Map.of(
                "fullName", "Erin Example", "email", "erin.e2e@lablink.edu",
                "password", "Welcome2026", "confirmPassword", "Welcome2026"));
        assertThat(afterRegister.path()).isEqualTo("/login");
        assertThat(afterRegister.body()).contains("Account created. Please log in.");

        Page home = browser.login("erin.e2e@lablink.edu", "Welcome2026");
        assertThat(home.path()).isEqualTo("/");
        assertThat(home.body()).contains("Welcome, Erin Example").contains("(STUDENT)");
    }

    @Test
    @DisplayName("E2E-02 Wrong password is refused and protected pages require login")
    void loginIsEnforced() throws Exception {
        Browser browser = new Browser();
        Page bounced = browser.get("/reservations");
        assertThat(bounced.path()).isEqualTo("/login");

        Page refused = browser.login(STUDENT, "not-the-password");
        assertThat(refused.path()).isEqualTo("/login");
        assertThat(refused.query()).contains("error");
        assertThat(browser.get("/reservations").path()).isEqualTo("/login");
    }

    @Test
    @DisplayName("E2E-03 Lab manager adds a lab, finds it by search, edits it and deletes it")
    void managerManagesLabs() throws Exception {
        Browser manager = loggedIn(MANAGER, "manager123");
        Page saved = manager.submit("/labs/new", "/labs/save", labForm("E2E Chemistry Annex", "Annex Hall", "A1", "12"));
        assertThat(saved.path()).isEqualTo("/labs");
        assertThat(saved.body()).contains("Lab saved.");

        Page search = manager.get("/labs?q=Chemistry+Annex");
        assertThat(search.body()).contains("E2E Chemistry Annex");

        Long id = labId("E2E Chemistry Annex");
        Map<String, String> edit = labForm("E2E Chemistry Annex", "Annex Hall", "A1", "40");
        edit.put("id", id.toString());
        manager.submit("/labs/" + id + "/edit", "/labs/save", edit);
        assertThat(labRepository.findById(id).orElseThrow().getCapacity()).isEqualTo(40);

        Page deleted = manager.submit("/labs", "/labs/" + id + "/delete", Map.of());
        assertThat(deleted.body()).contains("Lab deleted.");
        assertThat(labRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("E2E-04 Student request goes PENDING, manager approves it, student sees APPROVED")
    void studentRequestIsApproved() throws Exception {
        Long lab = createLab("E2E Approval Lab");
        LocalDateTime start = futureSlot(10, 9);

        Browser student = loggedIn(STUDENT, "student123");
        Page submitted = student.submit("/reservations/new?labId=" + lab, "/reservations",
                reservationForm("labId", lab, start, start.plusHours(2), "E2E study group"));
        assertThat(submitted.path()).isEqualTo("/reservations");
        assertThat(submitted.body()).contains("Reservation submitted.").contains("PENDING");

        Reservation pending = onlyReservation(STUDENT, lab);
        Browser manager = loggedIn(MANAGER, "manager123");
        assertThat(manager.get("/reservations/manage").body()).contains("E2E study group");
        Page approved = manager.submit("/reservations/manage", "/reservations/" + pending.getId() + "/approve", Map.of());
        assertThat(approved.body()).contains("Reservation approved.");

        assertThat(student.get("/reservations").body()).contains("APPROVED");
        assertThat(onlyReservation(STUDENT, lab).getStatus()).isEqualTo(ReservationStatus.APPROVED);
    }

    @Test
    @DisplayName("E2E-05 Manager rejects a student request")
    void studentRequestIsRejected() throws Exception {
        Long lab = createLab("E2E Rejection Lab");
        LocalDateTime start = futureSlot(11, 9);
        Browser student = loggedIn(STUDENT, "student123");
        student.submit("/reservations/new?labId=" + lab, "/reservations",
                reservationForm("labId", lab, start, start.plusHours(1), "E2E rejected request"));

        Reservation pending = onlyReservation(STUDENT, lab);
        Browser manager = loggedIn(MANAGER, "manager123");
        Page rejected = manager.submit("/reservations/manage", "/reservations/" + pending.getId() + "/reject", Map.of());
        assertThat(rejected.body()).contains("Reservation rejected.");
        assertThat(student.get("/reservations").body()).contains("REJECTED");
    }

    @Test
    @DisplayName("E2E-06 Faculty booking is auto-approved and an overlapping request is blocked")
    void doubleBookingIsPrevented() throws Exception {
        Long lab = createLab("E2E Conflict Lab");
        LocalDateTime start = futureSlot(12, 13);

        Browser faculty = loggedIn(FACULTY, "faculty123");
        Page booked = faculty.submit("/reservations/new?labId=" + lab, "/reservations",
                reservationForm("labId", lab, start, start.plusHours(2), "E2E lecture"));
        assertThat(booked.body()).contains("APPROVED");

        long before = reservationRepository.count();
        Browser student = loggedIn(STUDENT, "student123");
        Page blocked = student.submit("/reservations/new?labId=" + lab, "/reservations",
                reservationForm("labId", lab, start.plusHours(1), start.plusHours(3), "E2E overlapping"));
        assertThat(blocked.status()).isEqualTo(200);
        assertThat(blocked.body()).contains("is already booked during that time");
        assertThat(reservationRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("E2E-07 Student reschedules a pending request, then cancels it")
    void rescheduleThenCancel() throws Exception {
        Long lab = createLab("E2E Reschedule Lab");
        LocalDateTime start = futureSlot(13, 10);
        Browser student = loggedIn(STUDENT, "student123");
        student.submit("/reservations/new?labId=" + lab, "/reservations",
                reservationForm("labId", lab, start, start.plusHours(1), "E2E reschedule me"));
        Long id = onlyReservation(STUDENT, lab).getId();

        LocalDateTime moved = start.plusDays(1).withHour(15);
        Map<String, String> times = new LinkedHashMap<>();
        times.put("startTime", moved.format(FORM_TIME));
        times.put("endTime", moved.plusHours(1).format(FORM_TIME));
        Page rescheduled = student.submit("/reservations/" + id + "/reschedule", "/reservations/" + id + "/reschedule", times);
        assertThat(rescheduled.body()).contains("Reservation rescheduled.");
        assertThat(reservationRepository.findById(id).orElseThrow().getStartTime()).isEqualTo(moved);

        Page cancelled = student.submit("/reservations", "/reservations/" + id + "/cancel", Map.of());
        assertThat(cancelled.body()).contains("Reservation cancelled.").contains("CANCELLED");
    }

    @Test
    @DisplayName("E2E-08 Equipment loan limit: 9 h on a 3D printer is refused, 4 h is accepted")
    void equipmentLoanLimit() throws Exception {
        Long printer = equipmentId("PRN-0002");
        LocalDateTime start = futureSlot(14, 8);
        Browser student = loggedIn(STUDENT, "student123");

        Page tooLong = student.submit("/reservations/new?equipmentId=" + printer, "/reservations",
                reservationForm("equipmentId", printer, start, start.plusHours(9), "E2E long print"));
        assertThat(tooLong.body()).contains("can be reserved for at most 8 hours at a time");

        Page ok = student.submit("/reservations/new?equipmentId=" + printer, "/reservations",
                reservationForm("equipmentId", printer, start, start.plusHours(4), "E2E short print"));
        assertThat(ok.body()).contains("Reservation submitted.").contains("3D Printer 2");
    }

    @Test
    @DisplayName("E2E-09 Approved booking shows on the week view and in the public REST API")
    void availabilityViews() throws Exception {
        Long lab = createLab("E2E Availability Lab");
        LocalDateTime start = futureSlot(3, 14);
        Browser faculty = loggedIn(FACULTY, "faculty123");
        faculty.submit("/reservations/new?labId=" + lab, "/reservations",
                reservationForm("labId", lab, start, start.plusHours(2), "E2E seminar"));

        Page week = faculty.get("/labs/" + lab + "/availability?start=" + start.toLocalDate());
        assertThat(week.body()).contains("E2E Availability Lab availability").contains("APPROVED");

        Browser anonymous = new Browser();
        Page api = anonymous.get("/api/labs/" + lab + "/availability?from=" + start.toLocalDate()
                + "T00:00&to=" + start.toLocalDate() + "T23:59");
        assertThat(api.status()).isEqualTo(200);
        assertThat(api.body()).contains("\"labName\":\"E2E Availability Lab\"")
                .contains("\"status\":\"APPROVED\"")
                .contains(start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        assertThat(anonymous.get("/api/labs").body()).contains("E2E Availability Lab");
    }

    @Test
    @DisplayName("E2E-10 Students are blocked from manager-only pages")
    void roleBasedAccess() throws Exception {
        Browser student = loggedIn(STUDENT, "student123");
        for (String path : List.of("/labs/new", "/equipment/new", "/reservations/manage", "/admin/users")) {
            assertThat(student.get(path).status()).as(path).isEqualTo(403);
        }
        assertThat(student.get("/labs").status()).isEqualTo(200);
    }

    @Test
    @DisplayName("E2E-11 Manager creates a faculty account, the user logs in, manager changes the role")
    void adminCreatesAccount() throws Exception {
        Browser manager = loggedIn(MANAGER, "manager123");
        Page created = manager.submit("/admin/users/new", "/admin/users", Map.of(
                "fullName", "Dr. E2E Faculty", "email", "dr.e2e@lablink.edu",
                "password", "TempPass2026", "role", "FACULTY"));
        assertThat(created.body()).contains("Account created for dr.e2e@lablink.edu.");

        Page home = loggedIn("dr.e2e@lablink.edu", "TempPass2026").current();
        assertThat(home.body()).contains("Welcome, Dr. E2E Faculty").contains("(FACULTY)");

        Long id = userRepository.findByEmail("dr.e2e@lablink.edu").orElseThrow().getId();
        Page changed = manager.submit("/admin/users", "/admin/users/" + id + "/role", Map.of("role", "LAB_MANAGER"));
        assertThat(changed.body()).contains("Role updated.");
        assertThat(userRepository.findById(id).orElseThrow().getRole()).isEqualTo(Role.LAB_MANAGER);
    }

    @Test
    @DisplayName("E2E-12 User changes password, old password stops working, then logs out")
    void passwordChangeAndLogout() throws Exception {
        Browser setup = new Browser();
        setup.submit("/register", "/register", Map.of("fullName", "Pat Password", "email", "pat.e2e@lablink.edu",
                "password", "FirstPass2026", "confirmPassword", "FirstPass2026"));

        Browser pat = loggedIn("pat.e2e@lablink.edu", "FirstPass2026");
        Page changed = pat.submit("/account", "/account/password", Map.of("currentPassword", "FirstPass2026",
                "newPassword", "SecondPass2026", "confirmPassword", "SecondPass2026"));
        assertThat(changed.body()).contains("Password changed.");

        Page loggedOut = pat.submit("/", "/logout", Map.of());
        assertThat(loggedOut.path()).isEqualTo("/login");
        assertThat(pat.get("/account").path()).isEqualTo("/login");

        assertThat(new Browser().login("pat.e2e@lablink.edu", "FirstPass2026").query()).contains("error");
        assertThat(new Browser().login("pat.e2e@lablink.edu", "SecondPass2026").path()).isEqualTo("/");
    }

    @Test
    @DisplayName("E2E-13 Health check is public and the API answers bad requests with JSON errors")
    void operationsAndApiErrors() throws Exception {
        Browser anonymous = new Browser();
        Page health = anonymous.get("/actuator/health");
        assertThat(health.status()).isEqualTo(200);
        assertThat(health.body()).contains("\"status\":\"UP\"");

        Page missing = anonymous.get("/api/labs/999999/availability");
        assertThat(missing.status()).isEqualTo(404);
        assertThat(missing.body()).contains("\"status\":404");

        Page badDate = anonymous.get("/api/labs/1/availability?from=next-tuesday");
        assertThat(badDate.status()).isEqualTo(400);
        assertThat(badDate.body()).contains("Use ISO format");
    }

    // ------------------------------------------------------------------ helpers

    private Browser loggedIn(String email, String password) throws Exception {
        Browser browser = new Browser();
        Page home = browser.login(email, password);
        assertThat(home.path()).as("login as " + email).isEqualTo("/");
        return browser;
    }

    private Long createLab(String name) throws Exception {
        Page saved = loggedIn(MANAGER, "manager123").submit("/labs/new", "/labs/save", labForm(name, "Test Hall", "T" + LAB_ROOMS.incrementAndGet(), "20"));
        assertThat(saved.body()).contains("Lab saved.");
        return labId(name);
    }

    private static Map<String, String> labForm(String name, String building, String room, String capacity) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("name", name);
        form.put("building", building);
        form.put("roomNumber", room);
        form.put("capacity", capacity);
        form.put("description", "Created by the end-to-end tests");
        form.put("active", "true");
        return form;
    }

    private static Map<String, String> reservationForm(String idField, Long id, LocalDateTime start, LocalDateTime end,
                                                       String purpose) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put(idField, id.toString());
        form.put("startTime", start.format(FORM_TIME));
        form.put("endTime", end.format(FORM_TIME));
        form.put("purpose", purpose);
        return form;
    }

    /** A whole-hour start time a given number of days from now. */
    private static LocalDateTime futureSlot(int daysAhead, int hour) {
        return LocalDateTime.now().plusDays(daysAhead).withHour(hour).withMinute(0).withSecond(0).withNano(0);
    }

    private Long labId(String name) {
        return labRepository.findAll().stream().filter(l -> name.equals(l.getName()))
                .map(Lab::getId).findFirst().orElseThrow();
    }

    private Long equipmentId(String serial) {
        return equipmentRepository.findAll().stream().filter(e -> serial.equals(e.getSerialNumber()))
                .map(Equipment::getId).findFirst().orElseThrow();
    }

    private Reservation onlyReservation(String email, Long labId) {
        User user = userRepository.findByEmail(email).orElseThrow();
        List<Reservation> mine = reservationRepository.findByUserOrderByStartTimeDesc(user).stream()
                .filter(r -> r.getLab() != null && r.getLab().getId().equals(labId))
                .toList();
        assertThat(mine).hasSize(1);
        return mine.get(0);
    }

    /** One HTTP response after all redirects were followed. */
    record Page(int status, URI uri, String body) {
        String path() {
            return uri.getPath();
        }

        String query() {
            return uri.getQuery() == null ? "" : uri.getQuery();
        }
    }

    /**
     * A tiny browser: one session cookie, follows redirects (turning POST into GET like a real
     * browser does after a 302), and reads the CSRF token from the form on the page it loaded.
     */
    final class Browser {
        private static final Pattern CSRF = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"");
        private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        private final Map<String, String> cookies = new LinkedHashMap<>();
        private Page current;

        Page current() {
            return current;
        }

        Page get(String path) throws IOException, InterruptedException {
            return send(HttpRequest.newBuilder(url(path)).GET(), 0);
        }

        /** Opens formPage (like clicking a link to it), then submits the form to action with its CSRF token. */
        Page submit(String formPage, String action, Map<String, String> fields) throws IOException, InterruptedException {
            Page page = get(formPage);
            Matcher token = CSRF.matcher(page.body());
            assertThat(token.find()).as("CSRF token on " + formPage).isTrue();
            StringJoiner body = new StringJoiner("&");
            body.add("_csrf=" + encode(token.group(1)));
            fields.forEach((k, v) -> body.add(encode(k) + "=" + encode(v)));
            HttpRequest.Builder post = HttpRequest.newBuilder(url(action))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
            return send(post, 0);
        }

        Page login(String email, String password) throws IOException, InterruptedException {
            return submit("/login", "/login", Map.of("email", email, "password", password));
        }

        private Page send(HttpRequest.Builder request, int hops) throws IOException, InterruptedException {
            if (!cookies.isEmpty()) {
                StringJoiner header = new StringJoiner("; ");
                cookies.forEach((k, v) -> header.add(k + "=" + v));
                request.header("Cookie", header.toString());
            }
            HttpRequest built = request.build();
            HttpResponse<String> response = http.send(built, HttpResponse.BodyHandlers.ofString());
            for (String setCookie : response.headers().allValues("Set-Cookie")) {
                String pair = setCookie.split(";", 2)[0];
                int eq = pair.indexOf('=');
                cookies.put(pair.substring(0, eq), pair.substring(eq + 1));
            }
            int status = response.statusCode();
            if (status >= 300 && status < 400 && hops < 10) {
                URI next = built.uri().resolve(response.headers().firstValue("Location").orElseThrow());
                return send(HttpRequest.newBuilder(next).GET(), hops + 1);
            }
            current = new Page(status, built.uri(), response.body());
            return current;
        }

        private URI url(String path) {
            return URI.create("http://localhost:" + port + path);
        }

        private String encode(String value) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }
    }

    /** Prints one line per scenario, e.g. "E2E-RESULT | PASS | E2E-01 ...", for the test report. */
    static class ResultLogger implements TestWatcher {
        @Override
        public void testSuccessful(ExtensionContext context) {
            System.out.println("E2E-RESULT | PASS | " + context.getDisplayName());
        }

        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            System.out.println("E2E-RESULT | FAIL | " + context.getDisplayName() + " | " + cause.getMessage());
        }
    }
}
