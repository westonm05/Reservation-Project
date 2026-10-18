package edu.campbellsville.lablink.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Who can reach which URLs. Every request passes through these rules before it gets
 * to a controller. Anything not listed requires the user to be logged in.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // open to everyone
                .requestMatchers("/login", "/register", "/css/**", "/error").permitAll()
                // lab managers only: changing inventory and reviewing requests
                .requestMatchers("/labs/new", "/labs/*/edit", "/labs/*/delete", "/labs/save").hasRole("LAB_MANAGER")
                .requestMatchers("/equipment/new", "/equipment/*/edit", "/equipment/*/delete", "/equipment/save").hasRole("LAB_MANAGER")
                .requestMatchers("/reservations/manage", "/reservations/*/approve", "/reservations/*/reject").hasRole("LAB_MANAGER")
                // everything else: any logged-in user
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .defaultSuccessUrl("/", true)
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll());
        return http.build();
    }

    /** BCrypt turns a password into a one-way hash so the real password is never stored. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
