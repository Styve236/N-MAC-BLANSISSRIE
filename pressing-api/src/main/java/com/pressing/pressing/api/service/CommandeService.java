package com.pressing.pressing.api.service;
import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import com.pressing.pressing.api.dto.request.CommandeRequestDTO;
import com.pressing.pressing.api.dto.request.LigneCommandeDTO;
import com.pressing.pressing.api.dto.request.PaiementDTO;
import com.pressing.pressing.api.dto.response.CommandeDTO;
import com.pressing.pressing.api.entite.Client;
import com.pressing.pressing.api.entite.Commande;
import com.pressing.pressing.api.entite.LigneCommande;
import com.pressing.pressing.api.entite.MoyenPaiement;
import com.pressing.pressing.api.entite.StatutCommande;
import com.pressing.pressing.api.entite.Tarif;
import com.pressing.pressing.api.repository.ClientRepository;
import com.pressing.pressing.api.repository.CommandeRepository;
import com.pressing.pressing.api.repository.TarifRepository;
import com.pressing.pressing.api.service.NotificationSmsService;
import com.pressing.pressing.api.service.PaiementService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;




@Service
@RequiredArgsConstructor
public class CommandeService
{
    private final CommandeRepository commandeRepository;
    private final ClientRepository clientRepository;
    private final TarifRepository tarifRepository;
    private final NotificationSmsService notificationSmsService;
    private final NotificationService notificationService;
    private final LivraisonService livraisonService;
    private final PaiementService paiementService;

    public Commande creerCommande(CommandeRequestDTO dto) {
        if (dto.getClientId() == null) {
            throw new IllegalArgumentException("Le client est obligatoire");
        }
        if (dto.getLignes() == null || dto.getLignes().isEmpty()) {
            throw new IllegalArgumentException("La commande doit contenir au moins une ligne");
        }

        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable avec l'id: " + dto.getClientId()));

        Commande commande = Commande.builder()
                .client(client)
                .numeroTicket(genererNumeroTicket())
                .dateCreation(LocalDateTime.now())
                .dateRecuperationPrevue(dto.getDateRetraitPrevue() != null
                        ? dto.getDateRetraitPrevue().atStartOfDay()
                        : null)
                .statut(StatutCommande.RECU)
                .montantPaye(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (LigneCommandeDTO ligneDto : dto.getLignes()) {
            LigneCommande ligne = creerLigneCommande(commande, ligneDto);
            commande.getLignes().add(ligne);
            total = total.add(ligne.getMontant());
        }

        commande.setMontantTotal(total);
        commande.setPoidsTotal(calculerPoidsTotal(commande.getLignes()));

        Commande commandeEnregistree = commandeRepository.save(commande);

        // Acompte versé à la prise de la commande (optionnel)
        if (dto.getAcompte() != null && dto.getAcompte().compareTo(BigDecimal.ZERO) > 0) {
            PaiementDTO acompte = new PaiementDTO();
            acompte.setMontant(dto.getAcompte());
            acompte.setMoyenPaiement(dto.getMoyenAcompte() != null ? dto.getMoyenAcompte() : MoyenPaiement.ESPECES);
            paiementService.ajouter(commandeEnregistree.getIdcommande(), acompte);
            commandeEnregistree = commandeRepository.findById(commandeEnregistree.getIdcommande()).orElse(commandeEnregistree);
        }

        // Notification interne : prévient l'agent de production (message personnalisable).
        // Une erreur de notification ne doit jamais bloquer la création de la commande.
        try {
            notificationService.notifierNouvelleCommande(commandeEnregistree, dto.getMessageAgent());
        } catch (Exception ex) {
            // la commande reste enregistrée
        }

        notificationSmsService.envoyerConfirmationCommande(commandeEnregistree);
        return commandeEnregistree;
    }

    private LigneCommande creerLigneCommande(Commande commande, LigneCommandeDTO dto) {
        Tarif tarif = tarifRepository.findById(dto.getTarifId())
                .orElseThrow(() -> new IllegalArgumentException("Tarif introuvable avec l'id: " + dto.getTarifId()));

        boolean poidsFourni = dto.getPoids() != null && dto.getPoids().compareTo(BigDecimal.ZERO) > 0;
        boolean quantiteFournie = dto.getQuantite() != null && dto.getQuantite() > 0;

        if (poidsFourni && quantiteFournie) {
            throw new IllegalArgumentException("Choisir uniquement le poids (au kilo) OU la quantité (à la pièce), pas les deux");
        }
        if (!poidsFourni && !quantiteFournie) {
            throw new IllegalArgumentException("Préciser le poids (en kilogramme) ou la quantité (à la pièce)");
        }

        // Sous-total de la ligne : prix au kilo × poids  OU  prix unitaire × quantité
        BigDecimal montantLigne;
        if (poidsFourni) {
            if (tarif.getPrixauklo() == null) {
                throw new IllegalArgumentException("Le tarif « " + (tarif.getNom() != null ? tarif.getNom() : tarif.getTypevetement()) + " » n'a pas de prix au kilo");
            }
            montantLigne = tarif.getPrixauklo().multiply(dto.getPoids());
        } else {
            if (tarif.getPrixunitaire() == null) {
                throw new IllegalArgumentException("Le tarif « " + (tarif.getNom() != null ? tarif.getNom() : tarif.getTypevetement()) + " » n'a pas de prix à la pièce");
            }
            montantLigne = tarif.getPrixunitaire().multiply(BigDecimal.valueOf(dto.getQuantite()));
        }

        return LigneCommande.builder()
                .commande(commande)
                .tarif(tarif)
                .typeNettoyage(dto.getTypeNettoyage() != null ? dto.getTypeNettoyage() : tarif.getTypeNettoyage())
                .description(dto.getDescription())
                .poids(poidsFourni ? dto.getPoids() : null)
                .quantite(poidsFourni ? null : dto.getQuantite())
                .montant(montantLigne)
                .build();
    }

    private BigDecimal calculerPoidsTotal(List<LigneCommande> lignes) {
        return lignes.stream()
                .map(LigneCommande::getPoids)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String genererNumeroTicket() {
        return "TK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public BigDecimal getResteAPayer(Long commandeId){
        Commande cmd = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new IllegalArgumentException("Commande introuvable avec l'id: " + commandeId));
        return cmd.getResteAPayer();
    }

    // HISTORIQUE des commandes d'un client, trié de la plus récente à la plus ancienne
    public List<CommandeDTO> getHistoriqueParClient(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new RessourceNotFoundException("Client introuvable avec l'id : " + clientId);
        }

        return commandeRepository.findByClientIdclient(clientId).stream()
                .sorted(Comparator.comparing(Commande::getDateCreation).reversed())
                .map(this::toHistoriqueDTO)
                .toList();
    }

    public List<Commande> getImpayes(){
        return commandeRepository.findByMontantPayeLessThanMontantTotal();
    }

    @Transactional(readOnly = true)
    public List<Commande> lister(Long clientId, StatutCommande statut, LocalDate dateDebut, LocalDate dateFin) {
        return commandeRepository.findAll().stream()
                .filter(c -> clientId == null || c.getClient().getIdclient().equals(clientId))
                .filter(c -> statut == null || c.getStatut() == statut)
                .filter(c -> dateDebut == null || (c.getDateCreation() != null && !c.getDateCreation().toLocalDate().isBefore(dateDebut)))
                .filter(c -> dateFin == null || (c.getDateCreation() != null && !c.getDateCreation().toLocalDate().isAfter(dateFin)))
                .sorted(Comparator.comparing(Commande::getDateCreation).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<Commande> listerPaginer(Long clientId, StatutCommande statut, LocalDate dateDebut, LocalDate dateFin, Pageable pageable) {
        if (clientId != null && statut != null) {
            return commandeRepository.findByClientIdclientAndStatut(clientId, statut, pageable);
        }
        if (clientId != null) {
            return commandeRepository.findByClientIdclient(clientId, pageable);
        }
        if (statut != null) {
            return commandeRepository.findByStatut(statut, pageable);
        }
        if (dateDebut != null && dateFin != null) {
            return commandeRepository.findByDateCreationBetween(dateDebut.atStartOfDay(), dateFin.atTime(23, 59, 59), pageable);
        }
        if (clientId == null && statut == null && dateDebut == null && dateFin == null) {
            return commandeRepository.findAll(pageable);
        }
        // Filtres partiels (une seule date, ou client + dates...) : on filtre en mémoire puis on pagine
        List<Commande> filtrées = lister(clientId, statut, dateDebut, dateFin);
        long total = filtrées.size();
        int debut = (int) Math.min(pageable.getOffset(), total);
        List<Commande> contenu = filtrées.subList(debut, (int) Math.min(debut + pageable.getPageSize(), total));
        return new PageImpl<>(contenu, pageable, total);
    }

    @Transactional(readOnly = true)
    public Commande consulter(Long commandeId) {
        return commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Commande introuvable avec l'id : " + commandeId));
    }

    public Commande changerStatut(Long commandeId, StatutCommande nouveauStatut) {
        if (nouveauStatut == null) {
            throw new IllegalArgumentException("Le statut est obligatoire");
        }
        Commande commande = consulter(commandeId);
        commande.setStatut(nouveauStatut);
        if (nouveauStatut == StatutCommande.RECUPERE && commande.getDateRetraitReelle() == null) {
            commande.setDateRetraitReelle(LocalDateTime.now());
        }
        commandeRepository.save(commande);
        if (nouveauStatut == StatutCommande.PRET) {
            try {
                livraisonService.assurerLivraisonPret(commandeId);
            } catch (Exception ex) {
                // la commande reste PRET même si la création de la livraison échoue
            }
        }
        return commandeRepository.findById(commandeId).orElse(commande);
    }

    public Commande marquerPret(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new IllegalArgumentException("Commande introuvable avec l'id: " + commandeId));
        commande.setStatut(StatutCommande.PRET);
        Commande commandeMaj = commandeRepository.save(commande);
        // La livraison est automatiquement créée vers le premier livreur actif si elle n'existe pas.
        try {
            livraisonService.assurerLivraisonPret(commandeId);
        } catch (Exception ex) {
            // la commande reste PRET même si la création de la livraison échoue
        }
        Commande finale = commandeRepository.findById(commandeId).orElse(commandeMaj);
        notificationSmsService.envoyerLingePret(finale);
        return finale;
    }

    public int envoyerRappelImpayes() {
        List<Commande> impayes = getImpayes();
        impayes.forEach(notificationSmsService::envoyerRappelImpaye);
        return impayes.size();
    }

    private CommandeDTO toHistoriqueDTO(Commande c) {
        CommandeDTO dto = new CommandeDTO();
        dto.setId(c.getIdcommande());
        dto.setNumeroTicket(c.getNumeroTicket());
        dto.setDateCreation(c.getDateCreation());
        dto.setDateRecuperationPrevue(c.getDateRecuperationPrevue());
        dto.setDateRetraitReelle(c.getDateRetraitReelle());
        dto.setStatut(c.getStatut());
        dto.setPoidsTotal(c.getPoidsTotal());
        dto.setMontantTotal(c.getMontantTotal());
        dto.setMontantPaye(c.getMontantPaye());
        dto.setRemise(c.getRemise());
        dto.setResteAPayer(c.getResteAPayer());
        dto.setPayee(c.isPayee());
        return dto;
    }
}
