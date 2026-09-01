package com.pressing.pressing.api.recu;

import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import com.pressing.pressing.api.commande.Commande;
import com.pressing.pressing.api.commande.CommandeRepository;
import com.pressing.pressing.api.commande.LigneCommande;
import com.pressing.pressing.api.commande.Tarif;
import com.pressing.pressing.api.sms.NotificationSmsService;
import com.pressing.pressing.api.sms.SmsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RecuService {

    private final CommandeRepository commandeRepository;
    private final NotificationSmsService notificationSmsService;
    private final SmsProperties smsProperties;

    @Transactional(readOnly = true)
    public RecuDTO getRecuParCommandeId(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Commande introuvable avec l'id : " + commandeId));
        return toDTO(commande);
    }

    @Transactional(readOnly = true)
    public RecuDTO getRecuParNumeroTicket(String numeroTicket) {
        Commande commande = commandeRepository.findByNumeroTicket(numeroTicket)
                .orElseThrow(() -> new RessourceNotFoundException("Aucune commande pour le ticket : " + numeroTicket));
        return toDTO(commande);
    }

    public RecuDTO envoyerRecuParSms(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Commande introuvable avec l'id : " + commandeId));
        notificationSmsService.envoyerRecuCommande(commande);
        return toDTO(commande);
    }

    String genererLien(Commande commande) {
        return smsProperties.getRecuBaseUrl() + "/recu/" + commande.getNumeroTicket();
    }

    private RecuDTO toDTO(Commande commande) {
        RecuDTO dto = new RecuDTO();
        dto.setIdcommande(commande.getIdcommande());
        dto.setNumeroTicket(commande.getNumeroTicket());
        dto.setLien(genererLien(commande));
        dto.setStatut(commande.getStatut() != null ? commande.getStatut().name() : null);
        dto.setNomClient(commande.getClient().getNom());
        dto.setTelephoneClient(commande.getClient().getTelephone());
        dto.setDateCreation(commande.getDateCreation());
        dto.setDateRecuperationPrevue(commande.getDateRecuperationPrevue());
        dto.setPoidsTotal(commande.getPoidsTotal());
        dto.setMontantTotal(commande.getMontantTotal());
        dto.setMontantPaye(commande.getMontantPaye());
        dto.setResteAPayer(commande.getMontantTotal().subtract(commande.getMontantPaye()));
        dto.setLignes(commande.getLignes().stream().map(this::ligneToDTO).toList());
        dto.setPaiements(commande.getPaiements().stream().map(p -> {
            PaiementRecuDTO paiement = new PaiementRecuDTO();
            paiement.setMontant(p.getMontant());
            paiement.setMoyenPaiement(p.getMoyenPaiement() != null ? p.getMoyenPaiement().name() : null);
            paiement.setDatePaiement(p.getDatePaiement());
            paiement.setReferenceTransaction(p.getReferenceTransaction());
            return paiement;
        }).toList());
        return dto;
    }

    private LigneRecuDTO ligneToDTO(LigneCommande ligne) {
        LigneRecuDTO dto = new LigneRecuDTO();
        Tarif tarif = ligne.getTarif();
        if (tarif != null) {
            dto.setDesignation(tarif.getNom() != null ? tarif.getNom() : tarif.getTypevetement());
            dto.setTypeNettoyage(tarif.getTypeNettoyage() != null ? tarif.getTypeNettoyage().name() : null);
        }
        if (ligne.getPoids() != null && ligne.getPoids().compareTo(BigDecimal.ZERO) > 0) {
            dto.setUnite("KILO");
        } else {
            dto.setUnite("PIECE");
        }
        dto.setQuantite(ligne.getQuantite());
        dto.setPoids(ligne.getPoids());
        dto.setMontant(ligne.getMontant());
        return dto;
    }
}