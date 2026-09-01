package com.pressing.pressing.api.Users;

import com.pressing.pressing.api.common.exception.DonneeDejaExistanteException;
import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Users creer(UserRequestDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DonneeDejaExistanteException("Un compte existe déjà avec cet email : " + dto.getEmail());
        }

        Users utilisateur = Users.builder()
                .nom(dto.getNom())
                .tels(dto.getTels())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole())
                .actif(true)
                .build();
        return userRepository.save(utilisateur);
    }

    public List<Users> lister() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "dateCreation"));
    }

    public Users consulter(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Employé introuvable avec l'id : " + id));
    }

    public Users modifier(Long id, UserRequestDTO dto) {
        Users utilisateur = consulter(id);

        if (!utilisateur.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new DonneeDejaExistanteException("Un compte existe déjà avec cet email : " + dto.getEmail());
        }

        utilisateur.setNom(dto.getNom());
        utilisateur.setTels(dto.getTels());
        utilisateur.setEmail(dto.getEmail());
        if (dto.getRole() != null) {
            utilisateur.setRole(dto.getRole());
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            utilisateur.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        return userRepository.save(utilisateur);
    }

    public Users changerStatut(Long id, boolean actif) {
        Users utilisateur = consulter(id);
        utilisateur.setActif(actif);
        return userRepository.save(utilisateur);
    }

    public Users reinitialiserMotDePasse(Long id, String nouveauMotDePasse) {
        if (nouveauMotDePasse == null || nouveauMotDePasse.isBlank()) {
            throw new IllegalArgumentException("Le nouveau mot de passe est obligatoire");
        }
        Users utilisateur = consulter(id);
        utilisateur.setPassword(passwordEncoder.encode(nouveauMotDePasse));
        return userRepository.save(utilisateur);
    }
}