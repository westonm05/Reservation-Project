package com.cs450.reservations;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service // Spring creates one of these at startup and hands it out where needed
public class CustomUserDetailsService implements UserDetailsService { // Security requires this interface

    private final UserRepository userRepository; // our door into the users table

    public CustomUserDetailsService(UserRepository userRepository) { // Spring passes the repository in for us
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException { // Security calls this during login

        User user = userRepository.findByEmail(email) // find the account by the email that was typed
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email)); // no match means login fails

        return org.springframework.security.core.userdetails.User // full path because Security has its own User class
                .withUsername(user.getEmail()) // email is what people log in with
                .password(user.getPassword()) // stays hashed, Security compares the hashes itself
                .roles(user.getRole()) // "STUDENT" becomes ROLE_STUDENT internally
                .build();
    }
}
