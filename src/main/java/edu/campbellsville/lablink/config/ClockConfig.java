package edu.campbellsville.lablink.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides the app's clock. Classes ask for a Clock instead of calling
 * LocalDateTime.now() directly, so tests can freeze time at a known moment.
 * (A small example of Dependency Inversion.)
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
