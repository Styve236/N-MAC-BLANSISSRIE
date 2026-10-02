package com.pressing.pressing.api.service;
import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import com.pressing.pressing.api.dto.request.CommandeRequestDTO;
import com.pressing.pressing.api.dto.request.LigneCommandeDTO;
import com.pressing.pressing.api.dto.request.PaiementDTO;
import com.pressing.pressing.api.dto.response.CommandeDTO;
import com.pressing.pressing.api.dto.response.CommandeDetailDTO;
import com.pressing.pressing.api.entite.Client;
import com.pressing.pressing.api.entite.Commande;
import com.pressing.pressing.api.entite.LigneCommande;
import com.pressing.pressing.api.entite.MoyenPaiement;
import com.pressing.pressing.api.entite.StatutCommande;
import com.pressing.pressing.api.entite.Tarif;
import com.pressing.pressing.api.mapper.CommandeMapper;
import com.pressing.pressing.api.repository.ClientRepository;
import com.pressing.pressing.api.repository.CommandeRepository;
import com.pressing.pressing.api.repository.CommandeSpecification;
import com.pressing.pressing.api.repository.TarifRepository;
import com.pressing.pressing.api.service.NotificationSmsService;
import com.pressing.pressing.api.service.PaiementService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;




@Service
@RequiredArgsConstructor
public class CommandeService
{
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CommandeService.class);

    private final CommandeRepository commandeRepository;
    private final ClientRepository clientRepository;
    private final TarifRepository tarifRepository;
    private final NotificationSmsService notificationSmsService;
    private final NotificationService notificationService;
    private final LivraisonService livraisonService;
    private final PaiementService paiementService;
    private final CommandeMapper commandeMapper;

    // Transaction unique : commande + lignes + acompte sont enregistres ensemble.
    // Avant, un echec de l'acompte laissait une commande sans paiement partiel.
    @Transactional
    public CommandeDetailDTO creerCommande(CommandeRequestDTO dto) {
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
            // C'est ici que la commande disparaissait : la notification tourne dans la
            // meme transaction, son échec la marque rollback-only, et le catch ne
            // l'empêche pas. Le commit levait alors UnexpectedRollbackException (500)
            // et la commande était perdue. La notification a son propre transaction.
            log.warn("Notification interne non creee pour la commande {} : {}",
                    commandeEnregistree.getIdcommande(), ex.getMessage(), ex);
        }

        // SMS de confirmation : un échec de l'envoi ne doit pas annuler la commande
        // (la transaction annulerait tout si l'exception remontait).
        try {
            notificationSmsService.envoyerConfirmationCommande(commandeEnregistree);
        } catch (Exception ex) {
            log.warn("SMS de confirmation non envoyé pour la commande {} : {}",
                    commandeEnregistree.getIdcommande(), ex.getMessage());
        }
        return commandeMapper.toDTO(commandeEnregistree);
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

    /**
     * Le numero de ticket sert d'identifiant dans un lien de recu public
     * (/recu/{numeroTicket}) : il doit donc etre devinable le moins possible.
     * 24 caracteres hexadecimaux aleatoires = 96 bits d'entropie, soit ~10^29
     * combinaisons. SecureRandom plutot que UUID.randomUUID(), qui s'appuie sur un
     * generateur partage et n'est pas destine a un usage securitaire.
     */
    private String genererNumeroTicket() {
        byte[] octets = new byte[12];
        new SecureRandom().nextBytes(octets);
        StringBuilder ticket = new StringBuilder("TK-");
        for (byte octet : octets) {
            ticket.append(Character.forDigit((octet >> 4) & 0xF, 16));
            ticket.append(Character.forDigit(octet & 0xF, 16));
        }
        return ticket.toString().toUpperCase();
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
        return commandeRepository.findAll(
                CommandeSpecification.filtre(clientId, statut, dateDebut, dateFin),
                Sort.by(Sort.Direction.DESC, "dateCreation"));
    }

    @Transactional(readOnly = true)
    public Page<CommandeDetailDTO> listerPaginer(Long clientId, StatutCommande statut, LocalDate dateDebut, LocalDate dateFin, Pageable pageable) {
        // Tous les filtres sont appliques en base : plus de chargement de toutes les
        // commandes pour les trier en Java (les filtres partiels sont desormais geres aussi).
        return commandeRepository
                .findAll(CommandeSpecification.filtre(clientId, statut, dateDebut, dateFin), pageable)
                .map(commandeMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public CommandeDetailDTO consulter(Long commandeId) {
        return commandeMapper.toDTO(consulterEntite(commandeId));
    }

    @Transactional(readOnly = true)
    public Commande consulterEntite(Long commandeId) {
        return commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Commande introuvable avec l'id : " + commandeId));
    }

    @Transactional
    public CommandeDetailDTO changerStatut(Long commandeId, StatutCommande nouveauStatut) {
        if (nouveauStatut == null) {
            throw new IllegalArgumentException("Le statut est obligatoire");
        }
        Commande commande = consulterEntite(commandeId);
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
        return commandeMapper.toDTO(commandeRepository.findById(commandeId).orElse(commande));
    }

    @Transactional
    public CommandeDetailDTO marquerPret(Long commandeId) {
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
        return commandeMapper.toDTO(finale);
    }

    public int envoyerRappelImpayes() {
        List<Commande> impayes = getImpayes();
        impayes.forEach(notificationSmsService::envoyerRappelImpaye);
        return impayes.size();
    }

    private CommandeDTO toHistoriqueDTO(Commande c) {
        CommandeDTO dto = new CommandeDTO();
        dto.setIdcommande(c.getIdcommande());
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
