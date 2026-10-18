package edu.campbellsville.lablink.repository;

import edu.campbellsville.lablink.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data writes the SQL for us: save(), findById(), findAll(), delete() come from
 * JpaRepository, and methods named like findByEmail are turned into queries automatically.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
