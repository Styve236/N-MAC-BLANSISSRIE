package com.pressing.pressing.api.service;
import com.pressing.pressing.api.common.exception.DonneeDejaExistanteException;
import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import com.pressing.pressing.api.dto.request.UserRequestDTO;
import com.pressing.pressing.api.dto.response.UserDTO;
import com.pressing.pressing.api.entite.Users;
import com.pressing.pressing.api.repository.UserRepository;
import com.pressing.pressing.api.entite.Role;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;




@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDTO creer(UserRequestDTO dto) {
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
        return toDTO(userRepository.save(utilisateur));
    }

    public List<UserDTO> lister() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "dateCreation"))
                .stream().map(this::toDTO).toList();
    }

    public UserDTO consulter(Long id) {
        Users utilisateur = userRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Employé introuvable avec l'id : " + id));
        return toDTO(utilisateur);
    }

    public List<UserDTO> listerLivreurs() {
        return userRepository.findByRoleAndActifTrue(Role.LIVREUR)
                .stream().map(this::toDTO).toList();
    }

    public UserDTO modifier(Long id, UserRequestDTO dto) {
        Users utilisateur = userRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Employé introuvable avec l'id : " + id));

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
        return toDTO(userRepository.save(utilisateur));
    }

    public UserDTO changerStatut(Long id, boolean actif) {
        Users utilisateur = userRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Employé introuvable avec l'id : " + id));
        utilisateur.setActif(actif);
        return toDTO(userRepository.save(utilisateur));
    }

    public UserDTO reinitialiserMotDePasse(Long id, String nouveauMotDePasse) {
        if (nouveauMotDePasse == null || nouveauMotDePasse.isBlank()) {
            throw new IllegalArgumentException("Le nouveau mot de passe est obligatoire");
        }
        Users utilisateur = userRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Employé introuvable avec l'id : " + id));
        utilisateur.setPassword(passwordEncoder.encode(nouveauMotDePasse));
        return toDTO(userRepository.save(utilisateur));
    }

    private UserDTO toDTO(Users u) {
        UserDTO dto = new UserDTO();
        dto.setIdusers(u.getIdusers());
        dto.setNom(u.getNom());
        dto.setTels(u.getTels());
        dto.setEmail(u.getEmail());
        dto.setRole(u.getRole());
        dto.setActif(u.isActif());
        dto.setDateCreation(u.getDateCreation());
        return dto;
    }
}
