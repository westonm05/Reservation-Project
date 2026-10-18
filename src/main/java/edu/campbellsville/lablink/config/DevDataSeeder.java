package edu.campbellsville.lablink.config;

import edu.campbellsville.lablink.model.Equipment;
import edu.campbellsville.lablink.model.Lab;
import edu.campbellsville.lablink.model.Role;
import edu.campbellsville.lablink.model.User;
import edu.campbellsville.lablink.repository.EquipmentRepository;
import edu.campbellsville.lablink.repository.LabRepository;
import edu.campbellsville.lablink.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Fills the in-memory development database with demo accounts and inventory each time the
 * app starts, so there is something to click on. Does NOT run with the "mysql" profile.
 */
@Component
@Profile("!mysql")
public class DevDataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final LabRepository labs;
    private final EquipmentRepository equipment;
    private final PasswordEncoder encoder;

    public DevDataSeeder(UserRepository users, LabRepository labs, EquipmentRepository equipment, PasswordEncoder encoder) {
        this.users = users;
        this.labs = labs;
        this.equipment = equipment;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return; // already seeded
        }
        users.save(new User("manager@lablink.edu", "Demo Lab Manager", encoder.encode("manager123"), Role.LAB_MANAGER));
        users.save(new User("faculty@lablink.edu", "Demo Faculty", encoder.encode("faculty123"), Role.FACULTY));
        users.save(new User("student@lablink.edu", "Demo Student", encoder.encode("student123"), Role.STUDENT));

        Lab csLab = labs.save(new Lab("Computer Science Lab", "Science Building", "210", 30, "30 workstations with IntelliJ and Java 17"));
        Lab robotics = labs.save(new Lab("Robotics Lab", "Science Building", "118", 12, "Workbenches, soldering stations"));
        labs.save(new Lab("Media Lab", "Library", "B04", 8, "Video editing suites"));

        equipment.save(new Equipment("Raspberry Pi Kit", "RPI-0001", "Pi 5 with sensors", robotics));
        equipment.save(new Equipment("3D Printer", "PRN-0001", "Prusa MK4", robotics));
        equipment.save(new Equipment("Loaner Laptop", "LAP-0001", "Windows laptop", csLab));
        equipment.save(new Equipment("Portable Projector", "PRJ-0001", "Checked out from the front desk", null));
    }
}
