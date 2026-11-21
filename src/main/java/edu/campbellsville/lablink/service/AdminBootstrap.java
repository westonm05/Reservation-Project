package edu.campbellsville.lablink.service;

import edu.campbellsville.lablink.model.Role;
import edu.campbellsville.lablink.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the first lab-manager account on a brand-new production database.
 *
 * Production runs without the demo seed data, and self-registration only creates students, so
 * without this nobody could ever reach the admin page. When LABLINK_ADMIN_EMAIL and
 * LABLINK_ADMIN_PASSWORD are set AND no lab manager exists yet, this runs once at startup.
 * After that it does nothing, so the variables can (and should) be removed.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminBootstrap.class);
    static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final UserService userService;
    private final String email;
    private final String password;
    private final String fullName;

    public AdminBootstrap(UserRepository userRepository, UserService userService,
                          @Value("${lablink.bootstrap-admin.email:}") String email,
                          @Value("${lablink.bootstrap-admin.password:}") String password,
                          @Value("${lablink.bootstrap-admin.name:LabLink Administrator}") String fullName) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    @Override
    public void run(ApplicationArguments args) {
        createFirstAdminIfNeeded();
    }

    /** Returns true if an account was created. */
    boolean createFirstAdminIfNeeded() {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return false;
        }
        if (userRepository.existsByRole(Role.LAB_MANAGER)) {
            LOG.info("bootstrap.admin.skipped reason=lab-manager-exists");
            return false;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "LABLINK_ADMIN_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        userService.createAccount(fullName, email, password, Role.LAB_MANAGER);
        LOG.warn("bootstrap.admin.created email={} - remove LABLINK_ADMIN_PASSWORD from the environment now",
                email.trim().toLowerCase());
        return true;
    }
}
