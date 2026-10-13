package edu.campbellsville.lablink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for LabLink. Running this class starts an embedded web server
 * on http://localhost:8080 and wires together every @Controller, @Service and
 * @Repository it finds in this package and below.
 */
@SpringBootApplication
public class LabLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabLinkApplication.class, args);
    }
}
