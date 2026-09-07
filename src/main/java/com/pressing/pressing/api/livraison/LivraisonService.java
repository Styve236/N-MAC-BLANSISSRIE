package com.pressing.pressing.api.livraison;

import com.pressing.pressing.api.Users.Users;
import com.pressing.pressing.api.Users.UserRepository;
import com.pressing.pressing.api.commande.Commande;
import com.pressing.pressing.api.commande.CommandeRepository;
import com.pressing.pressing.api.commande.StatutCommande;
import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import com.pressing.pressing.api.sms.NotificationSmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LivraisonService {

    private final CommandeRepository commandeRepository;
    private final UserRepository userRepository;
    private final NotificationSmsService notificationSmsService;

    @Transactional
    public LivraisonDTO assignerLivraison(Long commandeId, LivraisonRequestDTO requete) {
        if (requete.getAdresseLivraison() == null || requete.getAdresseLivraison().isBlank()) {
            throw new IllegalArgumentException("L'adresse de livraison est obligatoire");
        }
        if (requete.getLivreurId() == null) {
            throw new IllegalArgumentException("Le livreur est obligatoire");
        }

        Commande commande = getCommande(commandeId);
        Users livreur = getLivreur(requete.getLivreurId());

        commande.setAdresseLivraison(requete.getAdresseLivraison().trim());
        commande.setLivreur(livreur);
        commande.setDateLivraisonPrevue(requete.getDateLivraisonPrevue());
        commande.setFraisLivraison(requete.getFraisLivraison());
        commande.setStatutLivraison(StatutLivraison.A_LIVRER);

        if (commande.getMontantTotal() != null && requete.getFraisLivraison() != null) {
            commande.setMontantTotal(commande.getMontantTotal().add(requete.getFraisLivraison()));
        }

        Commande sauvegardee = commandeRepository.save(commande);
        notificationSmsService.envoyerLivreCommande(sauvegardee);
        return toDTO(sauvegardee);
    }

    @Transactional
    public LivraisonDTO changerStatutLivraison(Long commandeId, StatutLivraison nouveauStatut) {
        if (nouveauStatut == null) {
            throw new IllegalArgumentException("Le statut est obligatoire");
        }
        Commande commande = getCommande(commandeId);
        commande.setStatutLivraison(nouveauStatut);

        if (nouveauStatut == StatutLivraison.LIVREE) {
            commande.setDateLivraisonReelle(LocalDateTime.now());
            commande.setStatut(StatutCommande.LIVRE);
        } else if (nouveauStatut == StatutLivraison.EN_COURS) {
            commande.setStatut(StatutCommande.PRET);
        }

        return toDTO(commandeRepository.save(commande));
    }

    @Transactional(readOnly = true)
    public List<LivraisonDTO> listerLivraisons(StatutLivraison statut) {
        return commandeRepository.findAll().stream()
                .filter(c -> c.getAdresseLivraison() != null)
                .filter(c -> statut == null || c.getStatutLivraison() == statut)
                .sorted((a, b) -> {
                    int sa = priorite(a.getStatutLivraison());
                    int sb = priorite(b.getStatutLivraison());
                    return Integer.compare(sa, sb);
                })
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LivraisonDTO> mesLivraisons(Long livreurId, StatutLivraison statut) {
        return commandeRepository.findAll().stream()
                .filter(c -> c.getLivreur() != null && c.getLivreur().getIdusers().equals(livreurId))
                .filter(c -> c.getAdresseLivraison() != null)
                .filter(c -> statut == null || c.getStatutLivraison() == statut)
                .sorted((a, b) -> {
                    int sa = priorite(a.getStatutLivraison());
                    int sb = priorite(b.getStatutLivraison());
                    return Integer.compare(sa, sb);
                })
                .map(this::toDTO)
                .toList();
    }

    private int priorite(StatutLivraison s) {
        if (s == null) return 0;
        return switch (s) {
            case EN_COURS -> 1;
            case A_LIVRER -> 2;
            case ECHEC -> 3;
            case LIVREE -> 4;
        };
    }

    private Commande getCommande(Long id) {
        return commandeRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Commande introuvable (id " + id + ")"));
    }

    private Users getLivreur(Long id) {
        Users u = userRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Utilisateur introuvable (id " + id + ")"));
        if (u.getRole() != com.pressing.pressing.api.Users.Role.LIVREUR) {
            throw new IllegalArgumentException("L'utilisateur " + u.getNom() + " n'est pas un livreur (rôle " + u.getRole() + ")");
        }
        return u;
    }

    private LivraisonDTO toDTO(Commande c) {
        LivraisonDTO dto = new LivraisonDTO();
        dto.setIdcommande(c.getIdcommande());
        dto.setNumeroTicket(c.getNumeroTicket());
        dto.setClientNom(c.getClient().getNom());
        dto.setClientTelephone(c.getClient().getTelephone());
        dto.setAdresseLivraison(c.getAdresseLivraison());
        dto.setFraisLivraison(c.getFraisLivraison());
        dto.setDateLivraisonPrevue(c.getDateLivraisonPrevue());
        dto.setDateLivraisonReelle(c.getDateLivraisonReelle());
        dto.setLivreurId(c.getLivreur() != null ? c.getLivreur().getIdusers() : null);
        dto.setLivreurNom(c.getLivreur() != null ? c.getLivreur().getNom() : null);
        dto.setStatutLivraison(c.getStatutLivraison());
        dto.setMontantTotal(c.getMontantTotal());
        dto.setMontantPaye(c.getMontantPaye());
        dto.setPayee(c.isPayee());
        return dto;
    }
}