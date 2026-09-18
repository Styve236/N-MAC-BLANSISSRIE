package com.pressing.pressing.api.service;
import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import com.pressing.pressing.api.dto.request.LivraisonRequestDTO;
import com.pressing.pressing.api.dto.response.LivraisonDTO;
import com.pressing.pressing.api.entite.Commande;
import com.pressing.pressing.api.entite.Role;
import com.pressing.pressing.api.entite.StatutCommande;
import com.pressing.pressing.api.entite.StatutLivraison;
import com.pressing.pressing.api.entite.Users;
import com.pressing.pressing.api.repository.CommandeRepository;
import com.pressing.pressing.api.repository.UserRepository;
import com.pressing.pressing.api.service.NotificationSmsService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;




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
        if (requete.getFraisLivraison() != null && requete.getFraisLivraison().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Les frais de livraison ne peuvent pas être négatifs");
        }

        Commande commande = getCommande(commandeId);
        Users livreur = getLivreur(requete.getLivreurId());

        commande.setAdresseLivraison(requete.getAdresseLivraison().trim());
        commande.setLivreur(livreur);
        commande.setDateLivraisonPrevue(requete.getDateLivraisonPrevue());
        commande.setFraisLivraison(requete.getFraisLivraison());
        commande.setStatutLivraison(StatutLivraison.A_LIVRER);

        if (commande.getMontantTotal() != null && requete.getFraisLivraison() != null
                && requete.getFraisLivraison().compareTo(BigDecimal.ZERO) > 0) {
            commande.setMontantTotal(commande.getMontantTotal().add(requete.getFraisLivraison()));
            // Les frais ajoutent un reste à payer : la commande n'est plus forcément payée
            if (commande.getStatut() == StatutCommande.PAYEE && commande.aUnsolde()) {
                commande.setStatut(StatutCommande.RECUPERE);
            }
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
        if (nouveauStatut == StatutLivraison.LIVREE && commande.aUnsolde()) {
            throw new IllegalArgumentException(
                    "Impossible de livrer : la commande " + commande.getNumeroTicket()
                            + " n'est pas entièrement payée (reste " + commande.getResteAPayer()
                            + " FCFA).");
        }
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
                .filter(c -> !c.isMasqueeParLivreur())
                .filter(c -> statut == null || c.getStatutLivraison() == statut)
                .sorted((a, b) -> {
                    int sa = priorite(a.getStatutLivraison());
                    int sb = priorite(b.getStatutLivraison());
                    return Integer.compare(sa, sb);
                })
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public LivraisonDTO masquerLivraison(Long commandeId) {
        Commande commande = getCommande(commandeId);
        if (commande.getStatutLivraison() != StatutLivraison.LIVREE) {
            throw new IllegalArgumentException("Seules les livraisons déjà livrées peuvent être retirées du flux du livreur");
        }
        commande.setMasqueeParLivreur(true);
        return toDTO(commandeRepository.save(commande));
    }

    // Garantit qu'une commande PRET dispose d'une livraison : assignée au premier livreur actif (adresse par défaut).
    @Transactional
    public void assurerLivraisonPret(Long commandeId) {
        Commande commande = getCommande(commandeId);
        if (commande.getAdresseLivraison() != null && commande.getLivreur() != null) {
            return;
        }
        List<Users> livreursActifs = userRepository.findByRoleAndActifTrue(Role.LIVREUR);
        if (livreursActifs.isEmpty()) {
            return;
        }
        commande.setAdresseLivraison(commande.getAdresseLivraison() != null
                ? commande.getAdresseLivraison()
                : adresseDefaut(commande));
        commande.setLivreur(livreursActifs.get(0));
        commande.setStatutLivraison(StatutLivraison.A_LIVRER);
        commandeRepository.save(commande);
    }

    // Rattrapage : transforme les commandes PRET existantes sans livraison en livraison (livreur actif par défaut)
    @Transactional
    public int rattraperLivraisonsPret() {
        List<Commande> aTraiter = commandeRepository.findPretesSansLivraison(StatutCommande.PRET);
        List<Users> livreursActifs = userRepository.findByRoleAndActifTrue(Role.LIVREUR);
        Users premierLivreur = livreursActifs.isEmpty() ? null : livreursActifs.get(0);
        int compteur = 0;
        for (Commande commande : aTraiter) {
            commande.setAdresseLivraison(adresseDefaut(commande));
            if (premierLivreur != null) {
                commande.setLivreur(premierLivreur);
            }
            commande.setStatutLivraison(StatutLivraison.A_LIVRER);
            compteur++;
        }
        commandeRepository.saveAll(aTraiter);
        return compteur;
    }

    private String adresseDefaut(Commande commande) {
        if (commande.getClient() == null) {
            return "Adresse à confirmer";
        }
        String quartier = commande.getClient().getQuartier();
        String ville = commande.getClient().getVille();
        if (quartier != null && !quartier.isBlank() && ville != null && !ville.isBlank()) {
            return quartier.trim() + ", " + ville.trim();
        }
        if (quartier != null && !quartier.isBlank()) {
            return quartier.trim();
        }
        if (ville != null && !ville.isBlank()) {
            return ville.trim();
        }
        return "Adresse à confirmer";
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
        if (u.getRole() != Role.LIVREUR) {
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