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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final com.pressing.pressing.api.fidelite.FideliteService fideliteService;

    public Page<PaiementResponseDTO> listerTous(Long clientId, Pageable pageable) {
        Page<Paiement> page;
        if (clientId != null) {
            page = paiementRepository.findByClientIdclient(clientId, pageable);
        } else {
            page = paiementRepository.findAllByOrderByDatePaiementDesc(pageable);
        }
        return page.map(this::toDTO);
    }

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

        // Classification automatique du paiement : ACOMPTE / SOLDE / INTEGRAL
        BigDecimal avant = commande.getMontantPaye();
        BigDecimal apres = commande.getMontantPaye().add(dto.getMontant());
        TypePaiement typePaiement;
        if (avant.compareTo(BigDecimal.ZERO) == 0 && apres.compareTo(commande.getMontantTotal()) >= 0) {
            typePaiement = TypePaiement.INTEGRAL;
        } else if (apres.compareTo(commande.getMontantTotal()) >= 0) {
            typePaiement = TypePaiement.SOLDE;
        } else {
            typePaiement = TypePaiement.ACOMPTE;
        }

        Paiement paiement = Paiement.builder()
                .montant(dto.getMontant())
                .moyenPaiement(dto.getMoyenPaiement())
                .typePaiement(typePaiement)
                .referenceTransaction(dto.getReferenceTransaction())
                .datePaiement(LocalDateTime.now())
                .build();

        commande.addpaiement(paiement);
        Commande commandeMaj = commandeRepository.save(commande);

        notificationSmsService.envoyerPaiementRecu(commandeMaj, paiement.getMontant());

        fideliteService.crediterPoints(
                commande.getClient().getIdclient(),
                commande.getIdcommande(),
                paiement.getMontant());

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

        BigDecimal remise = commande.getRemise() != null ? commande.getRemise() : BigDecimal.ZERO;
        StatutPaiementDTO dto = new StatutPaiementDTO();
        dto.setIdcommande(commande.getIdcommande());
        dto.setNumeroTicket(commande.getNumeroTicket());
        dto.setMontantTotal(commande.getMontantTotal());
        dto.setMontantPaye(commande.getMontantPaye());
        dto.setResteAPayer(commande.getMontantTotal().subtract(remise).subtract(commande.getMontantPaye()));
        dto.setPayee(commande.getMontantPaye().compareTo(commande.getMontantTotal().subtract(remise)) >= 0);
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
        dto.setTypePaiement(paiement.getTypePaiement() != null ? paiement.getTypePaiement().name() : null);
        dto.setReferenceTransaction(paiement.getReferenceTransaction());
        dto.setDatePaiement(paiement.getDatePaiement());
        if (paiement.getCommande() != null) {
            dto.setIdcommande(paiement.getCommande().getIdcommande());
            dto.setNumeroTicket(paiement.getCommande().getNumeroTicket());
        }
        return dto;
    }
}