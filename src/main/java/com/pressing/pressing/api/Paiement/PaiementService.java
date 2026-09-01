package com.pressing.pressing.api.Paiement;

import com.pressing.pressing.api.client.Client;
import com.pressing.pressing.api.client.ClientRepository;
import com.pressing.pressing.api.common.dto.PaiementDTO;
import com.pressing.pressing.api.common.dto.PaiementResponseDTO;
import com.pressing.pressing.api.common.dto.StatutPaiementDTO;
import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import com.pressing.pressing.api.commande.Commande;
import com.pressing.pressing.api.commande.CommandeRepository;
import com.pressing.pressing.api.sms.NotificationSmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaiementService {
    private final PaiementRepository paiementRepository;
    private final CommandeRepository commandeRepository;
    private final ClientRepository clientRepository;
    private final NotificationSmsService notificationSmsService;

    public BigDecimal getCAduJour(LocalDate date){
        return paiementRepository.sumMontantByDate(date);
    }

    @Transactional
    public PaiementResponseDTO ajouter(Long commandeId, PaiementDTO dto) {
        if (dto.getMontant() == null || dto.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du paiement doit être supérieur à zéro");
        }
        if (dto.getMoyenPaiement() == null) {
            throw new IllegalArgumentException("Le moyen de paiement est obligatoire");
        }

        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Commande introuvable avec l'id : " + commandeId));

        BigDecimal reste = commande.getMontantTotal().subtract(commande.getMontantPaye());
        if (dto.getMontant().compareTo(reste) > 0) {
            throw new IllegalArgumentException("Le montant dépasse le reste à payer (" + reste + " FCFA)");
        }

        Paiement paiement = Paiement.builder()
                .montant(dto.getMontant())
                .moyenPaiement(dto.getMoyenPaiement())
                .referenceTransaction(dto.getReferenceTransaction())
                .datePaiement(LocalDateTime.now())
                .build();

        commande.addpaiement(paiement);
        Commande commandeMaj = commandeRepository.save(commande);

        notificationSmsService.envoyerPaiementRecu(commandeMaj, paiement.getMontant());
        return toDTO(paiement);
    }

    @Transactional(readOnly = true)
    public List<PaiementResponseDTO> listerParCommande(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Commande introuvable avec l'id : " + commandeId));

        return commande.getPaiements().stream()
                .sorted(Comparator.comparing(Paiement::getDatePaiement).reversed())
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaiementResponseDTO> listerParClient(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new RessourceNotFoundException("Client introuvable avec l'id : " + clientId);
        }

        return commandeRepository.findByClientIdclient(clientId).stream()
                .flatMap(c -> c.getPaiements().stream())
                .sorted(Comparator.comparing(Paiement::getDatePaiement).reversed())
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public StatutPaiementDTO statutPaiement(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RessourceNotFoundException("Commande introuvable avec l'id : " + commandeId));

        StatutPaiementDTO dto = new StatutPaiementDTO();
        dto.setIdcommande(commande.getIdcommande());
        dto.setNumeroTicket(commande.getNumeroTicket());
        dto.setMontantTotal(commande.getMontantTotal());
        dto.setMontantPaye(commande.getMontantPaye());
        dto.setResteAPayer(commande.getMontantTotal().subtract(commande.getMontantPaye()));
        dto.setPayee(commande.isPayee());
        dto.setPaiements(commande.getPaiements().stream()
                .sorted(Comparator.comparing(Paiement::getDatePaiement))
                .map(this::toDTO)
                .toList());
        return dto;
    }

    private PaiementResponseDTO toDTO(Paiement paiement) {
        PaiementResponseDTO dto = new PaiementResponseDTO();
        dto.setIdpaiement(paiement.getIdpaiement());
        dto.setMontant(paiement.getMontant());
        dto.setMoyenPaiement(paiement.getMoyenPaiement() != null ? paiement.getMoyenPaiement().name() : null);
        dto.setReferenceTransaction(paiement.getReferenceTransaction());
        dto.setDatePaiement(paiement.getDatePaiement());
        if (paiement.getCommande() != null) {
            dto.setIdcommande(paiement.getCommande().getIdcommande());
            dto.setNumeroTicket(paiement.getCommande().getNumeroTicket());
        }
        return dto;
    }
}