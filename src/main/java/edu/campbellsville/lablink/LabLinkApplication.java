package edu.campbellsville.lablink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. Running this class starts an embedded web server on http://localhost:8080.
 * Spring scans this package and everything under it for controllers, services,
 * repositories and configuration classes.
 */
@SpringBootApplication
public class LabLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabLinkApplication.class, args);
    }
}
