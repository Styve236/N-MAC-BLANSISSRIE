package com.pressing.pressing.api.commande;

import com.pressing.pressing.api.client.Client;
import com.pressing.pressing.api.client.ClientRepository;
import com.pressing.pressing.api.common.dto.CommandeDTO;
import com.pressing.pressing.api.common.dto.CommandeRequestDTO;
import com.pressing.pressing.api.common.dto.LigneCommandeDTO;
import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import com.pressing.pressing.api.sms.NotificationSmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommandeService
{
    private final CommandeRepository commandeRepository;
    private final ClientRepository clientRepository;
    private final TarifRepository tarifRepository;
    private final NotificationSmsService notificationSmsService;

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
                .dateRecuperationPrevue(dto.getDateRetraitPrevue())
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
            montantLigne = tarif.getPrixauklo().multiply(dto.getPoids());
        } else {
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
        return cmd.getMontantTotal().subtract(cmd.getMontantPaye());
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

    public List<Commande> getCommandedujours(){
        LocalDateTime debut = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(23, 59);

        return commandeRepository.findByDateCreationBetween(debut, fin);
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
        return commandeRepository.save(commande);
    }

    public Commande marquerPret(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new IllegalArgumentException("Commande introuvable avec l'id: " + commandeId));
        commande.setStatut(StatutCommande.PRET);
        Commande commandeMaj = commandeRepository.save(commande);
        notificationSmsService.envoyerLingePret(commandeMaj);
        return commandeMaj;
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
        BigDecimal paye = c.getMontantPaye() != null ? c.getMontantPaye() : BigDecimal.ZERO;
        BigDecimal total = c.getMontantTotal() != null ? c.getMontantTotal() : BigDecimal.ZERO;
        dto.setResteAPayer(total.subtract(paye));
        dto.setPayee(paye.compareTo(total) >= 0);
        return dto;
    }
}
