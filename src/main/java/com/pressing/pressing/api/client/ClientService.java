package com.pressing.pressing.api.client;

import com.pressing.pressing.api.common.dto.ClientDTO;
import com.pressing.pressing.api.common.exception.DonneeDejaExistanteException;
import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {
    private final ClientRepository clientRepository;

    public ClientDTO creerClient(ClientDTO dto) {
        if (dto.getNom() == null || dto.getNom().isBlank()) {
            throw new IllegalArgumentException("Le nom du client est obligatoire");
        }
        if (dto.getTelephone() == null || dto.getTelephone().isBlank()) {
            throw new IllegalArgumentException("Le téléphone du client est obligatoire");
        }
        if (clientRepository.existsByTelephone(dto.getTelephone())) {
            throw new DonneeDejaExistanteException("Un client avec le numéro " + dto.getTelephone() + " existe déjà");
        }

        Client client = Client.builder()
                .nom(dto.getNom())
                .telephone(dto.getTelephone())
                .ville(dto.getVille())
                .quartier(dto.getQuartier())
                .points_fidelites(0)
                .build();

        return toDTO(clientRepository.save(client));
    }

    public List<ClientDTO> listerTous() {
        return clientRepository.findAll().stream().map(this::toDTO).toList();
    }

    public Page<ClientDTO> listerTousPaginer(Pageable pageable) {
        return clientRepository.findAll(pageable).map(this::toDTO);
    }

    public Page<ClientDTO> rechercherParNomPaginer(String nom, Pageable pageable) {
        return clientRepository.findByNomContainsIgnoreCase(nom, pageable).map(this::toDTO);
    }

    public ClientDTO consulterParId(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Client introuvable avec l'id : " + id));
        return toDTO(client);
    }

    public ClientDTO rechercherParTelephone(String telephone) {
        Client client = clientRepository.findByTelephone(telephone)
                .orElseThrow(() -> new RessourceNotFoundException("Aucun client trouvé avec le téléphone : " + telephone));
        return toDTO(client);
    }

    public List<ClientDTO> rechercherParNom(String nom) {
        return clientRepository.findByNomContainsIgnoreCase(nom).stream().map(this::toDTO).toList();
    }

    private ClientDTO toDTO(Client client) {
        ClientDTO dto = new ClientDTO();
        dto.setId(client.getIdclient());
        dto.setNom(client.getNom());
        dto.setTelephone(client.getTelephone());
        dto.setVille(client.getVille());
        dto.setQuartier(client.getQuartier());
        dto.setPoints_fidelites(client.getPoints_fidelites());
        return dto;
    }
}
