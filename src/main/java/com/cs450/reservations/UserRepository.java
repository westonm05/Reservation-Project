package com.cs450.reservations;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {//this gives us a lot of different functions such as save(),findById(), findAll(),delete(),count()

    Optional<User> findByEmail(String email);//select from users where email

    boolean existsByEmail(String email);//tells them they cannot register with an already used email
}
