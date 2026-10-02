package com.pressing.pressing.api.service;
import com.pressing.pressing.api.common.exception.ConflitDonneeException;
import com.pressing.pressing.api.common.exception.DonneeDejaExistanteException;
import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import com.pressing.pressing.api.dto.response.ClientDTO;
import com.pressing.pressing.api.entite.Client;
import com.pressing.pressing.api.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



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

    // Suppression definitive reservee aux clients sans historique : un client ayant
    // des commandes ne doit pas disparaitre (contrainte FK + litiges de restitution).
    @Transactional
    public void supprimer(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Client introuvable avec l'id : " + id));
        if (clientRepository.existsByIdclientAndCommandesIsNotEmpty(id)) {
            throw new ConflitDonneeException(
                    "Impossible de supprimer ce client : il possède déjà des commandes. "
                            + "Supprimez ou archivez ses commandes d'abord.");
        }
        clientRepository.delete(client);
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
