package com.pressing.pressing.api.config;

import com.pressing.pressing.api.Users.Role;
import com.pressing.pressing.api.Users.UserRepository;
import com.pressing.pressing.api.Users.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@press.com")) {
            Users admin = Users.builder()
                    .nom("Administrateur")
                    .tels("000000000")
                    .email("admin@press.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .actif(true)
                    .build();
            userRepository.save(admin);
            System.out.println(">>> Compte ADMIN cree par defaut : admin@press.com / admin123");
        }
    }
}