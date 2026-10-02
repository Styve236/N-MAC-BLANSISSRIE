package com.pressing.pressing.api.config;
import com.pressing.pressing.api.entite.Role;
import com.pressing.pressing.api.entite.Users;
import com.pressing.pressing.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;



// Cree les comptes de demonstration. DESACTIVE par defaut : sans cela,
// admin/admin123 serait recree a chaque demarrage. En developpement local,
// l'activer explicitement avec APP_INITIALIZER_ACTIF=true.
@Component
@Order(1)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.initializer.actif", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

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
            log.warn("Compte de démonstration créé : {} ({}) — à supprimer ou à changer en production",
                    email, role);
        }
    }
}