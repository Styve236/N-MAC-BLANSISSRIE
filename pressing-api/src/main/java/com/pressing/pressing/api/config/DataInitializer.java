package com.pressing.pressing.api.config;
import com.pressing.pressing.api.entite.Role;
import com.pressing.pressing.api.entite.Users;
import com.pressing.pressing.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;



@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        creerCompteSiAbsent("admin@press.com", "admin123", Role.ADMIN, "Administrateur", "000000000");
        creerCompteSiAbsent("receptionniste@press.com", "recep123", Role.RECEPTIONNISTE, "Receptionniste", "691000001");
        creerCompteSiAbsent("agent@press.com", "agent123", Role.AGENT_PRODUCTION, "Agent Production", "691000002");
        creerCompteSiAbsent("livreur@press.com", "livreur123", Role.LIVREUR, "Livreur", "691000003");
    }

    private void creerCompteSiAbsent(String email, String motDePasse, Role role, String nom, String tels) {
        if (!userRepository.existsByEmail(email)) {
            Users utilisateur = Users.builder()
                    .nom(nom)
                    .tels(tels)
                    .email(email)
                    .password(passwordEncoder.encode(motDePasse))
                    .role(role)
                    .actif(true)
                    .build();
            userRepository.save(utilisateur);
            System.out.println(">>> Compte " + role + " cree par defaut : " + email + " / " + motDePasse);
        }
    }
}